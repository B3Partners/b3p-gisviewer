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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.BaseGisAction;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Themas;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.*;
import org.apache.struts.validator.DynaValidatorForm;
import org.securityfilter.filter.SecurityFilter;
import org.securityfilter.filter.SecurityRequestWrapper;

/**
 * Deze Action haalt alle analyse thema's op.
 */
public class IndexAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(IndexAction.class);
    protected static final String LOGIN = "login";
    protected static final String LOGINERROR = "loginError";
    protected static final String LOGOUT = "logout";
    protected static final String LIST = "list";

    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(LOGIN);
        hibProp.setDefaultForwardName(LOGIN);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(LOGIN, hibProp);

        hibProp = new ExtendedMethodProperties(LOGINERROR);
        hibProp.setDefaultMessageKey("error.inlog");
        hibProp.setDefaultForwardName(LOGINERROR);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(LOGINERROR, hibProp);

        hibProp = new ExtendedMethodProperties(LOGOUT);
        hibProp.setDefaultForwardName(LOGOUT);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(LOGOUT, hibProp);

        hibProp = new ExtendedMethodProperties(LIST);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(LIST, hibProp);

        return map;
    }

    /**
     *
     * @param mapping The ActionMapping used to select this instance.
     * @param dynaForm The DynaValidatorForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     *
     * @return an Actionforward object.
     *
     * @throws Exception
     */
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        createLists(dynaForm, request);
        return mapping.findForward(SUCCESS);
    }

    private String findCodeinUrl(String url) throws MalformedURLException {

        if (url == null) {
            return null;
        }
        String code = null;
        URL ourl = new URL(url);

        String qparams = ourl.getQuery();
        if (qparams != null && qparams.length() != 0) {
            int pos = qparams.indexOf(URL_AUTH);
            if (pos >= 0 && qparams.length() > pos + URL_AUTH.length() + 1) {
                code = qparams.substring(pos + URL_AUTH.length() + 1);
                pos = code.indexOf('&');
                if (pos >= 0) {
                    code = code.substring(0, pos);
                }
            }
        }
        return code;
    }

    public ActionForward login(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (request instanceof SecurityRequestWrapper) {

            SecurityRequestWrapper srw = (SecurityRequestWrapper) request;
            String savedURL = SecurityFilter.getContinueToURL(request);
            String code = findCodeinUrl(savedURL);

            Principal user = null;
            // Eventueel fake Principal aanmaken
            if (!HibernateUtil.isCheckLoginKaartenbalie()) {
                user = GisSecurityRealm.authenticateFake(HibernateUtil.ANONYMOUS_USER);
            } else {
                String url = GisSecurityRealm.createCapabilitiesURL(code);
                user = GisSecurityRealm.authenticateHttp(url, HibernateUtil.ANONYMOUS_USER, null, code);
            }

            if (user != null) {
                // invalidate old session if the user was already authenticated, and they logged in as a different user
                if (request.getUserPrincipal() != null && false == request.getUserPrincipal().equals(user)) {
                    request.getSession().invalidate();
                }
                srw.setUserPrincipal(user);
                log.debug("Automatic login for user: " + HibernateUtil.ANONYMOUS_USER);
                // This is the url that the user was initially accessing before being prompted for login.
                response.sendRedirect(response.encodeRedirectURL(savedURL));
                return null;
            }
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        createLists(dynaForm, request);
        return getDefaultForward(mapping, request);
    }

    public ActionForward loginError(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward logout(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        String sesId = session.getId();
        session.invalidate();
        log.info("Logged out from session: " + sesId);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    /**
     * De knop berekent een lijst van thema's en stuurt dan door.
     *
     * @param mapping The ActionMapping used to select this instance.
     * @param dynaForm The DynaValidatorForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     *
     * @return an Actionforward object.
     *
     * @throws Exception
     */
    public ActionForward list(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {        
        List validThemas = getValidThemas(false, null, request);
        List themalist=new ArrayList();
        List clusterlist=new ArrayList();
        if (validThemas!=null){
            for (int i=0; i < validThemas.size(); i++){
                Themas t= (Themas) validThemas.get(i);
                Clusters c = t.getCluster();
                clusterlist = findParentClusters(c, clusterlist);
                if (!c.isHide_tree() && !c.isBackground_cluster()){
                    themalist.add(t);
                }
            }
        }
        request.setAttribute("themalist", themalist);
        request.setAttribute("clusterlist", clusterlist);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        createLists(dynaForm, request);
        return getDefaultForward(mapping, request);
    }

    private List findParentClusters(Clusters c, List parents) {
        if (parents==null) {
            return null;
        }
        if (c==null || !c.isCallable() || c.isBackground_cluster()) {
            return parents;
        }
        if (!parents.contains(c)) {
            parents.add(c);
        }
        return findParentClusters(c.getParent(), parents);
    }
}

