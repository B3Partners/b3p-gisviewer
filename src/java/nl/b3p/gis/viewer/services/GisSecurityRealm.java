/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 * 
 * This file is part of B3P Gisviewer.
 * 
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import nl.b3p.commons.security.XmlSecurityDatabase;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.services.B3PCredentials;
import nl.b3p.commons.services.HttpClientConfigured;
import nl.b3p.wms.capabilities.ServiceProvider;
import nl.b3p.wms.capabilities.WMSCapabilitiesReader;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.ExternalAuthenticatedRealm;
import org.securityfilter.realm.FlexibleRealmInterface;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GisSecurityRealm implements FlexibleRealmInterface, ExternalAuthenticatedRealm {

    private static final Log log = LogFactory.getLog(GisSecurityRealm.class);
    private static final String FORM_USERNAME = "j_username";
    private static final String FORM_PASSWORD = "j_password";
    private static final String FORM_CODE = "j_code";
    private static final String CAPABILITIES_QUERYSTRING = "REQUEST=GetCapabilities&VERSION=1.1.1&SERVICE=WMS";
    static protected Map<String, ServiceProvider> perUserNameSPCache = new HashMap();

    public Principal authenticate(SecurityRequestWrapper request) {
        String username = FormUtils.nullIfEmpty(request.getParameter(FORM_USERNAME));
        String password = FormUtils.nullIfEmpty(request.getParameter(FORM_PASSWORD));
        String code = FormUtils.nullIfEmpty(request.getParameter(FORM_CODE));

        HttpSession session = request.getSession();
        session.setAttribute("loginForm", true);

        return authenticate(username, password, code, request);
    }

    public Principal getAuthenticatedPrincipal(String username, String password) {
        return authenticate(username, password);
    }

    public boolean isUserInRole(Principal principal, String rolename) {
        if (principal == null) {
            return false;
        }
        boolean inRole = ((GisPrincipal) principal).isInRole(rolename);
        if (!inRole) {
            inRole = XmlSecurityDatabase.isUserInRole(principal.getName(), rolename);
        }
        return inRole;
    }

    public static String createCapabilitiesURL(String code) {
        String url = HibernateUtil.createPersonalKbUrl(code);
        if (url.indexOf('?') == -1) {
            url += "?";
        }
        if (url.indexOf('?') == url.length() - 1) {
            url += CAPABILITIES_QUERYSTRING;
        } else if (url.lastIndexOf('&') == url.length() - 1) {
            url += CAPABILITIES_QUERYSTRING;
        } else {
            url += "&" + CAPABILITIES_QUERYSTRING;
        }

        log.debug("Using external kb url: " + url);

        return url;
    }

    public static String createInternalCapabilitiesURL(String code) {
        String url = HibernateUtil.createInternalKbUrl(code);

        if (url == null || url.equals("")) {
            url = HibernateUtil.createPersonalKbUrl(code);
        }

        if (url.indexOf('?') == -1) {
            url += "?";
        }
        if (url.indexOf('?') == url.length() - 1) {
            url += CAPABILITIES_QUERYSTRING;
        } else if (url.lastIndexOf('&') == url.length() - 1) {
            url += CAPABILITIES_QUERYSTRING;
        } else {
            url += "&" + CAPABILITIES_QUERYSTRING;
        }

        log.debug("Using internal kb url: " + url);

        return url;
    }

    protected static GisPrincipal authenticateFake(String username) {

        List roles = new ArrayList();
        roles.add(HibernateUtil.GEBRUIKERS_ROL);
        roles.add(HibernateUtil.THEMABEHEERDERS_ROL);

        return new GisPrincipal(username, roles);
    }

    public static GisPrincipal authenticateHttp(String location, String username, String password,
            String code, SecurityRequestWrapper request) {

        log.debug("Start authenticateHttp()");

        WMSCapabilitiesReader wmscr = new WMSCapabilitiesReader();
        ServiceProvider sp = null;

        /* TODO: Wat te doen als de Applicatie een gebruikerscode heeft die ongeldig is
         Bijvoorbeeld ABC ? Ik denk dat je dan gewoon niet kunt inloggen. Misschien aan gisviewerconfig
         kant controleren of ingevulde gebruikerscode bij opslaan van een Applicatie wel
         geldig is ?
         */

        //Indien location "_VIEWER_CONFIG=true" bevat dan onder aparte key opslaan
        //omdat dan alle kaarten worden opgevraagd tbv configuratie. Welke kaarten
        //feitelijk worden opgehaald wordt bepaald door rol bij username/pw.
        //Indien via code ingelogd cachen met code
        String key = "";
        if(username == null && code != null) {
            key = code;
        } else if (username != null && username.equals("anoniem")) {
            key = code;
        } else if (code == null && username != null) {
            key = username;
        } else if (username != null && password != null) {
            key = username;
        }

        boolean isConfig = 
                location==null?false:location.contains("_VIEWER_CONFIG=true");
        if (key == null || key.isEmpty()) {
            return null;
        } else if (isConfig) {
            key += "_VIEWER_CONFIG";
        }

        String ip = null;

        if (request != null) {
            ip = request.getRemoteAddr();

            if (ip == null) {
                ip = request.getLocalAddr();
            }
        }

        log.debug("Username: " + username + ", Password: ****, Code: " + code + ", Key: "
                + key + ", Ip: " + ip);

        /* Do user/password check at kaartenbalie. */
        if (username != null && code == null || code.isEmpty() || code.equals("")
                || code.equalsIgnoreCase("null")) {

            log.debug("Checking login with kaartenbalie!");

            boolean canLogin = GisSecurityRealm.canLoginKaartenbalie(username, password, ip);

            if (!canLogin) {
                log.error("Gebruiker " + username + " is ongeldig. IP-adres: " + ip);

                return null;
            }
        }

        try {
            /* 
             WMS getCapabilities (serviceprovider) cachen.
             Als 'cacheOnDisk' en 'cacheOnDiskPath' params niet
             in web.xml staan plaatst hij de ServiceProvider
             objecten in geheugen (HashMap)            
             */
            Boolean cacheOnDisk = HibernateUtil.cacheOnDisk;
            if (cacheOnDisk != null && cacheOnDisk && isCachedOnDisk(key)) {
                sp = readCacheFromDisk(key);

                log.debug("User from DISK cache " + sp.getUserName() + " using key " + key);
            }

            if (cacheOnDisk == null || !cacheOnDisk && isInSPCache(key)) {
                sp = getFromSPCache(key);

                log.debug("User from MEM cache " + sp.getUserName() + " using key " + key);

            } else if (sp == null) {
                sp = wmscr.getProvider(location, username, password, ip);

                if (username != null && password != null && password.length() > 0) {
                    sp.setPassword(password);
                }

                if (cacheOnDisk != null && cacheOnDisk) {
                    writeCacheToDisk(key, sp);

                    log.debug("Login new in DISK cache for user " + sp.getUserName() + " using key " + key);
                } else {
                    putInSPCache(key, sp);

                    log.debug("Login new in MEM for user " + sp.getUserName() + " using key " + key);
                }
            }

        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("Error reading GetCapabilities ", ex);
            } else {
                log.error("Error reading GetCapabilities: " + ex.getLocalizedMessage());
            }

            return null;
        }

        if (sp == null) {
            log.error("No ServiceProvider found, denying login!");
            return null;
        }

        /* code uit service provider gebruiken */
        if (sp.getPersonalCode() != null) {
            code = sp.getPersonalCode();
        }

        if (sp.getUserName() != null) {
            username = sp.getUserName();
        }

        if (username == null || username.length() < 1) {
            username = HibernateUtil.ANONYMOUS_USER;
        }

        log.debug("Login for user: " + username);

        return new GisPrincipal(username, password, code, sp);
    }

    /* Do user/password check at kaartenbalie */
    public static boolean canLoginKaartenbalie(String username,
            String password, String ip) {

        String loginKbUrl = HibernateUtil.getKbLoginUrl();
        String kbUrl = HibernateUtil.getKbUrl();

        /* For when loginKbUrl not in web.xml */
        if (loginKbUrl == null && kbUrl != null) {
            if (kbUrl.contains("/services")) {
                loginKbUrl = kbUrl;
                loginKbUrl = loginKbUrl.replaceAll("/services", "/login");
            }        
        }

        if (loginKbUrl == null || username == null
                || password == null || loginKbUrl.isEmpty()
                || username.isEmpty() || password.isEmpty()) {

            return false;
        }
        

        B3PCredentials cred = new B3PCredentials();
        cred.setUserName(username);
        cred.setPassword(password);
        cred.setUrl(loginKbUrl);
        cred.setPreemptive(true);

        HttpClientConfigured hcc = new HttpClientConfigured(cred);
        log.debug("method url: " + loginKbUrl);
        HttpGet request = new HttpGet(loginKbUrl);
        request.addHeader("X-Forwarded-For", ip);

        HttpResponse response = null;
        try {
            response = hcc.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            log.debug("Status code: " + statusCode);
            if (statusCode != 200) {
                return false;
            }
        } catch (IOException ex) {
            log.debug("Exception False: ", ex);
            return false;
        } finally {
            hcc.close(response);
            hcc.close();
        }

        return true;
    }

    private static boolean isExpired(Date expireDate) {
        Date now = new Date();

        if (expireDate != null && expireDate.before(now)) {
            return true;
        }

        return false;
    }

    public Principal authenticate(String username, String password) {
        return authenticate(username, password, null);
    }

    public static Principal authenticate(String username, String password, String code) {

        // Eventueel fake Principal aanmaken
        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            return authenticateFake(username);
        }
        String url = createInternalCapabilitiesURL(code);
        return authenticateHttp(url, username, password, code, null);
    }

    public static Principal authenticate(String username, String password, String code,
            SecurityRequestWrapper request) {

        // Eventueel fake Principal aanmaken
        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            return authenticateFake(username);
        }
        String url = createInternalCapabilitiesURL(code);
        return authenticateHttp(url, username, password, code, request);
    }

    public Principal getAuthenticatedPrincipal(String username) {
        return null;
    }

    public static synchronized boolean isInSPCache(String userName) {
        if (Bron.isCacheExpired()) {
            flushSPCache();

            return false;
        }

        if (perUserNameSPCache.containsKey(userName)) {
            return true;
        }

        return false;
    }

    public static synchronized void putInSPCache(String userName, ServiceProvider sp) {
        perUserNameSPCache.put(userName, sp);
    }

    public static synchronized ServiceProvider getFromSPCache(String userName) {

        return (ServiceProvider) perUserNameSPCache.get(userName);
    }

    private static boolean isCachedOnDisk(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        String cacheOnDiskPath = HibernateUtil.cacheOnDiskPath;

        String fileName = cacheOnDiskPath + key + ".xml";

        File f = new File(fileName);
        if (f.exists() && !f.isDirectory()) {
            return true;
        }

        return false;
    }
    
    private static boolean isCachePathValid() {
        String cacheOnDiskPath = HibernateUtil.cacheOnDiskPath;
        File f = new File(cacheOnDiskPath);
        if (f.isDirectory()) {
            return true;
        }
        return false;
    }

    private static void writeCacheToDisk(String key, ServiceProvider sp) {
        if (key == null || sp == null || key.isEmpty() || !isCachePathValid()) {
            return;
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;

        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
        }

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("WMT_MS_Capabilities");
        doc.appendChild(rootElement);

        Element spElem = sp.toElement(doc, rootElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;

        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException ex) {
        }

        String cacheOnDiskPath = HibernateUtil.cacheOnDiskPath;
        String fileName = cacheOnDiskPath + key + ".xml";

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(fileName));

        try {
            transformer.transform(source, result);
        } catch (TransformerException ex) {
            log.error("Error writing cache to disk: ", ex);
        }
    }

    private static ServiceProvider readCacheFromDisk(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String cacheOnDiskPath = HibernateUtil.cacheOnDiskPath;
        String fileName = cacheOnDiskPath + key + ".xml";
        File file = new File(fileName);

        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
        } catch (IOException ex) {
        }

        WMSCapabilitiesReader wmsReader = new WMSCapabilitiesReader();
        ServiceProvider serviceProvider = wmsReader.getProvider(in);

        return serviceProvider;
    }

    public static synchronized void flushSPCache() {
        boolean cacheOnDisk = HibernateUtil.cacheOnDisk;

        if (cacheOnDisk) {
            try {
                String dir = HibernateUtil.cacheOnDiskPath;
                File directory = new File(dir);

                FileUtils.cleanDirectory(directory);
            } catch (IOException ex) {
                log.error("Fout tijdens verwijderen disk cache.", ex);
            }

            log.debug("Cache on DISK WMS leeggemaakt.");
        } else {
            perUserNameSPCache.clear();
            //perUserNameSPCache = null;
            //perUserNameSPCache = new HashMap();

            log.debug("Cache in MEMORY WMS leeggemaakt.");
        }
    }
}
