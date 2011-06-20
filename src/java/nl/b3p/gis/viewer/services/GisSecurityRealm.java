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

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.b3p.commons.security.XmlSecurityDatabase;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.wms.capabilities.ServiceProvider;
import nl.b3p.wms.capabilities.WMSCapabilitiesReader;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.ExternalAuthenticatedRealm;
import org.securityfilter.realm.FlexibleRealmInterface;

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

        WMSCapabilitiesReader wmscr = new WMSCapabilitiesReader();
        ServiceProvider sp = null;

        /* Indien via code ingelogd cachen met code */
        String key = "";

        if (username == null && code != null) {
            key = code;
        } else if (username != null && username.equals("anoniem")) {
            key = code;
        } else if (code == null && username != null) {
            key = username;
        } else if (username != null && password != null) {
            key = username;
        }

        String ip = null;

        if (request != null) {
            ip = request.getRemoteAddr();

            if (ip == null) {
                ip = request.getLocalAddr();
            }
        }

        if (ip == null) {
            ip = "invalidIp";
        }

        key = key + "_" + ip;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date expDate = null;

        try {
            /* WMS getCapabilities (serviceprovider) cachen */
            if (isInSPCache(key)) {
                sp = getFromSPCache(key);

                /* Wachtwoord controleren. Anders kun je nadat de serviceprovider
                 * de eerste gecached is inloggen met de username zonder een geldig
                 * wachtwoord in te vullen. Dus bekijken we of het wachtwoord voor
                 * de gecachte serviceprovider is gezet en houden we die tegen het
                 * ingevulde wachtwoord aan.
                 */
                if (username != null && username.length() > 0 && sp.getPassword() != null ) {
                    if (!sp.getPassword().equals(password)) {
                        return null;
                    }
                }

                /* Controleren of provider niet over datum is */
                expDate = sp.getExpireDate();
                if (isExpired(expDate)) {
                    log.info("EXPIRED: Login for " + sp.getUserName() + " has expired on " + df.format(expDate));
                    return null;
                }
                
            } else {
                sp = wmscr.getProvider(location, username, password);

                /* Controleren of provider niet over datum is */
                expDate = sp.getExpireDate();

                if (isExpired(expDate)) {
                    log.info("EXPIRED: Login for " + sp.getUserName() + " has expired on " + df.format(expDate));
                    return null;

                } else {                    
                    if (username != null && password != null && password.length() > 0) {
                        sp.setPassword(password);
                    }

                    putInSPCache(key, sp);
                }
            }

        } catch (Exception ex) {
            log.error("Error reading GetCapabilities: " + ex.getLocalizedMessage());
            return null;
        }

        if (sp == null) {
            log.error("No ServiceProvider found, denying login!");
            return null;
        }

        if (sp.getAllRoles() == null || sp.getAllRoles().isEmpty()) {
            if (!XmlSecurityDatabase.booleanAuthenticate(username, password)) {
                log.info("ServiceProvider has no roles");
                //return null;
            }
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

        log.debug("login: " + username);
        return new GisPrincipal(username, password, code, sp);
    }

    private static boolean isExpired(Date expireDate) {
        Date now = new Date();

        if (expireDate.before(now)) {
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
        String url = createCapabilitiesURL(code);
        return authenticateHttp(url, username, password, code, null);
    }

    public static Principal authenticate(String username, String password, String code,
            SecurityRequestWrapper request) {

        // Eventueel fake Principal aanmaken
        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            return authenticateFake(username);
        }
        String url = createCapabilitiesURL(code);
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

        return (ServiceProvider)perUserNameSPCache.get(userName);
    }

    public static synchronized void flushSPCache() {
        perUserNameSPCache = new HashMap();

        log.info("Cache WMS leeggemaakt.");
    }
}
