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
package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.CMSPagina;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Attribuut;
import nl.b3p.zoeker.configuratie.ResultaatAttribuut;
import nl.b3p.zoeker.configuratie.ZoekAttribuut;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import nl.b3p.zoeker.services.A11YResult;
import nl.b3p.zoeker.services.ZoekResultaat;
import nl.b3p.zoeker.services.ZoekResultaatAttribuut;
import nl.b3p.zoeker.services.Zoeker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class A11YViewerAction extends BaseGisAction {

    protected static final String LIST = "list";
    protected static final String SEARCH = "search";
    protected static final String START_LOCATIE = "startLocation";
    protected static final String RESULTS = "results";
    private Zoeker zoeker;
    private static int MAX_SEARCH_RESULTS = 1000;
    private static final int MAX_PAGE_LIMIT = 25;
    private static final Log logger = LogFactory.getLog(A11YViewerAction.class);

    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp;

        hibProp = new ExtendedMethodProperties(LIST);
        hibProp.setDefaultForwardName(LIST);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(LIST, hibProp);

        hibProp = new ExtendedMethodProperties(SEARCH);
        hibProp.setDefaultForwardName(SEARCH);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(SEARCH, hibProp);

        hibProp = new ExtendedMethodProperties(RESULTS);
        hibProp.setDefaultForwardName(RESULTS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(RESULTS, hibProp);

        hibProp = new ExtendedMethodProperties(START_LOCATIE);
        hibProp.setDefaultForwardName(START_LOCATIE);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(START_LOCATIE, hibProp);

        return map;
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        zoeker = new Zoeker();

        addCMSParams(request);

        createLists(dynaForm, request);

        return mapping.findForward(LIST);
    }

    private void setBreadCrumb(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        A11YResult a11yResult = (A11YResult) session.getAttribute("a11yResult");

        if (a11yResult != null) {
            request.setAttribute("a11yResultMap", a11yResult.getResultMap());
        }
    }

    public ActionForward search(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        addCMSParams(request);

        setBreadCrumb(request);

        showZoekVelden(dynaForm, request);

        return mapping.findForward(SEARCH);
    }

    public ActionForward results(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        addCMSParams(request);

        showZoekResults(dynaForm, request);

        return mapping.findForward(RESULTS);
    }

    public ActionForward startLocation(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String appCode = (String) request.getParameter("appCode");
        request.setAttribute("appCode", appCode);

        A11YResult a11yResult = createA11YResult(request);

        if (a11yResult != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute("a11yResult", a11yResult);
        }

        return mapping.findForward(START_LOCATIE);
    }

    /* This pojo holds the search results with the hidden form fields keys, values
     * in the Map and some other search attributes like appCode and searchConfigId.
     */
    private A11YResult createA11YResult(HttpServletRequest request) {
        A11YResult result = new A11YResult();

        Enumeration attrs = request.getParameterNames();
        while (attrs.hasMoreElements()) {
            String key = (String) attrs.nextElement();
            String value = (String) request.getParameter(key);

            if (key.equalsIgnoreCase("startGeom")) {
                result.setStartWkt(value);
            }
            if (key.equalsIgnoreCase("nextStep")) {
                result.setHasNextStep(true);
            }
            if (key.equalsIgnoreCase("appCode")) {
                result.setAppCode(value);
            }
            if (key.equalsIgnoreCase("searchConfigId") && !value.equals("")) {
                result.setSearchConfigId(new Integer(value));
            }

            if (!key.equalsIgnoreCase("startGeom") && !key.equalsIgnoreCase("nextStep") && !key.equalsIgnoreCase("appCode")
                    && !key.equalsIgnoreCase("searchConfigId") && !key.equalsIgnoreCase("startLocation")) {

                result.addResult(key, value);
            }

            logger.debug("REQUEST PARAM: " + key + "=" + value);
        }

        return result;
    }

    @Override
    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request)
            throws Exception {

        super.createLists(dynaForm, request);

        String appCode = (String) request.getParameter("appCode");

        /* Instellingen ophalen en appCode weer op request plaatsen */
        Applicatie app = null;
        if (appCode != null && !appCode.equals("")) {
            app = KaartSelectieUtil.getApplicatie(appCode);
        } else {
            app = KaartSelectieUtil.getDefaultApplicatie();

        }

        if (app != null) {
            appCode = app.getCode();
            request.setAttribute("appCode", appCode);
        } else {
            return;
        }

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(appCode);

        /* Indien niet aanwezig dan defaults laden */
        if ((map == null) || (map.size() < 1)) {
            map = configKeeper.getDefaultInstellingen();
        }

        /* Zoekers tonen voor deze Applicatie */
        String zoekConfigIds = (String) map.get("zoekConfigIds");

        if (zoekConfigIds != null && !zoekConfigIds.isEmpty()) {
            zoekConfigIds = zoekConfigIds.replace("\"", "");
            String[] ids = zoekConfigIds.split(",");

            List<ZoekConfiguratie> zcs = getZoekConfigs(ids);
            request.setAttribute("zoekConfigs", zcs);
        }

        Integer maxResults = (Integer) map.get("maxResults");
        if (maxResults != null && maxResults > 0) {
            MAX_SEARCH_RESULTS = new Integer(maxResults);
        }
    }

    private void addCMSParams(HttpServletRequest request) {
        String cmsPageId = (String) request.getParameter("cmsPageId"); 
        CMSPagina cmsPage = null;
        
        if (cmsPageId != null && !cmsPageId.isEmpty()) {
            request.setAttribute("cmsPageId", cmsPageId);
            
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            cmsPage = (CMSPagina) sess.get(CMSPagina.class, new Integer(cmsPageId));
        } 
        
        if (cmsPage != null) {
            if (cmsPage.getThema() != null) {
                request.setAttribute("theme", cmsPage.getThema());
            }
            
            if (cmsPage.getSefUrl() != null) {
                request.setAttribute("sefUrl", cmsPage.getSefUrl());
            }            
        }       
    }

    private List<ZoekConfiguratie> getZoekConfigs(String[] zoekerIds) {
        List<ZoekConfiguratie> zcs = new ArrayList<ZoekConfiguratie>();

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List<ZoekConfiguratie> results = sess.createQuery("from ZoekConfiguratie"
                + " order by naam").list();

        for (ZoekConfiguratie zc : results) {
            for (String id : zoekerIds) {
                if (id != null && !id.equals("") && zc.getId() == Integer.parseInt(id)) {
                    zcs.add(zc);
                }
            }
        }

        return zcs;
    }

    private void showZoekVelden(DynaValidatorForm dynaForm, HttpServletRequest request) {
        String appCode = (String) request.getParameter("appCode");
        String searchConfigId = (String) request.getParameter("searchConfigId");

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        ZoekConfiguratie zc = (ZoekConfiguratie) sess.get(ZoekConfiguratie.class, new Integer(searchConfigId));

        Set<ZoekAttribuut> zoekVelden = zc.getZoekVelden();
        request.setAttribute("zoekVelden", zoekVelden);

        Map results = new HashMap();
        for (ZoekAttribuut attr : zoekVelden) {
            if (attr.getInputtype() == ZoekAttribuut.SELECT_CONTROL) {
                List<ZoekResultaat> zr = zoeker.zoekMetConfiguratie(attr.getInputzoekconfiguratie(), new String[]{"*"}, MAX_SEARCH_RESULTS, new ArrayList());
                results.put(attr.getLabel(), zr);
            }
        }

        Map params = createInputParamsMap(zoekVelden, request);
        request.setAttribute("params", params);

        request.setAttribute("dropdownResults", results);
        request.setAttribute("appCode", appCode);
        request.setAttribute("searchConfigId", searchConfigId);
        request.setAttribute("searchName", zc.getNaam());
    }

    private void showZoekResults(DynaValidatorForm dynaForm, HttpServletRequest request) {
        String appCode = (String) request.getParameter("appCode");
        String searchConfigId = (String) request.getParameter("searchConfigId");

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        ZoekConfiguratie zc = (ZoekConfiguratie) sess.get(ZoekConfiguratie.class, new Integer(searchConfigId));

        String[] searchStrings = createZoekStringForZoeker(zc.getZoekVelden(), request);

        Integer startIndex;
        Integer limit;
        if (request.getParameter("startIndex") != null) {
            startIndex = new Integer(request.getParameter("startIndex"));
        } else {
            startIndex = 0;
        }
        if (request.getParameter("limit") != null) {
            limit = new Integer(request.getParameter("limit"));
        } else {
            limit = MAX_PAGE_LIMIT;
        }

        if (startIndex < 0) {
            startIndex = 0;
        }

        if (limit < 0) {
            limit = 0;
        }

        if (limit > MAX_PAGE_LIMIT) {
            limit = MAX_PAGE_LIMIT;
        }

        List<ZoekResultaat> results = new ArrayList<ZoekResultaat>();

        if (zoeker == null) {
            zoeker = new Zoeker();
        }

        HttpSession session = request.getSession(true);
        A11YResult a11yResult = (A11YResult) session.getAttribute("a11yResult");

        if (a11yResult != null) {
            results = zoeker.zoekMetConfiguratie(zc, searchStrings, MAX_SEARCH_RESULTS, new ArrayList(), true, startIndex, limit, a11yResult);
        } else {
            results = zoeker.zoekMetConfiguratie(zc, searchStrings, MAX_SEARCH_RESULTS, new ArrayList(), true, startIndex, limit);
        }

        if (a11yResult != null) {
            request.setAttribute("a11yResultMap", a11yResult.getResultMap());
        }

        request.setAttribute("searchConfigId", searchConfigId);

        if (zc.getParentZoekConfiguratie() != null) {
            request.setAttribute("nextStep", true);
            request.setAttribute("nextSearchConfigId", zc.getParentZoekConfiguratie().getId());
        } else {
            request.setAttribute("nextStep", false);
        }

        if (results != null && results.size() > 0) {
            ZoekResultaat r = (ZoekResultaat) results.get(0);
            request.setAttribute("count", r.getCount());

            if (r.getCount() < limit) {
                limit = r.getCount();
            }

            boolean startLocation = false;
            for (ZoekResultaatAttribuut attr : r.getAttributen()) {
                if (attr.getType() == Attribuut.START_GEOMETRY_TYPE) {
                    startLocation = true;
                }
            }

            request.setAttribute("startLocation", startLocation);
        } else {
            request.setAttribute("count", 0);
        }

        request.setAttribute("startIndex", startIndex);
        request.setAttribute("limit", limit);

        request.setAttribute("results", results);
        request.setAttribute("appCode", appCode);
        request.setAttribute("searchName", zc.getNaam());

        Map params = createResultParamsMap(zc.getResultaatVelden(), zc.getZoekVelden(), request);
        request.setAttribute("params", params);

        Map searchparams = createSearchStringMapForResult(zc.getZoekVelden(), request);
        request.setAttribute("searchparams", searchparams);
    }

    private Map createResultParamsMap(Set<ResultaatAttribuut> velden, Set<ZoekAttribuut> zoekVelden, HttpServletRequest request) {
        Map resultParams = new HashMap();

        Map params = request.getParameterMap();

        /* Controleren of een url param voorkomt in de resultaatvelden */
        for (ResultaatAttribuut attribuut : velden) {
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();

                String param = (String) pairs.getKey();
                if (param.equalsIgnoreCase(attribuut.getLabel()) || param.equalsIgnoreCase(attribuut.getNaam())
                        || param.equalsIgnoreCase(attribuut.getAttribuutnaam())) {

                    String[] waardes = (String[]) pairs.getValue();
                    String value = waardes[0];

                    resultParams.put(param, value);
                }
            }
        }

        /* Controleren of een url param voorkomt in de zoekvelden. Alleen param en waarde
         toevoegen als deze nog niet in Map zit */
        for (ZoekAttribuut attribuut : zoekVelden) {
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();

                String param = (String) pairs.getKey();
                if (param.equalsIgnoreCase(attribuut.getLabel()) || param.equalsIgnoreCase(attribuut.getNaam())
                        || param.equalsIgnoreCase(attribuut.getAttribuutnaam())) {

                    String[] waardes = (String[]) pairs.getValue();
                    String value = waardes[0];

                    if (!resultParams.containsKey(param)) {
                        resultParams.put(param, value);
                    }
                }
            }
        }

        return resultParams;
    }

    private Map createSearchStringMapForResult(Set<ZoekAttribuut> zoekVelden, HttpServletRequest request) {
        Map values = new HashMap();
        Map params = request.getParameterMap();

        for (ZoekAttribuut attribuut : zoekVelden) {
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();

                String param = (String) pairs.getKey();

                if (param.equalsIgnoreCase(attribuut.getLabel()) || param.equalsIgnoreCase(attribuut.getNaam())) {
                    String[] waardes = (String[]) pairs.getValue();
                    String value = waardes[0];

                    /* TODO: Deze check moet anders. Indien bijpassend resultaatveld ook van het type
                     * geom is dan niet in map stoppen */
                    if (!value.contains("POLYGON") && !value.contains("POINT")
                            && !value.contains("LINE")) {

                        values.put(attribuut.getLabel(), value);
                    }
                }
            }
        }

        return values;
    }

    private String[] createZoekStringForZoeker(Set<ZoekAttribuut> zoekVelden, HttpServletRequest request) {
        List<String> values = new ArrayList<String>();
        Map params = request.getParameterMap();

        for (ZoekAttribuut attribuut : zoekVelden) {
            Boolean lijktOp = false;

            if (attribuut.getType() == Attribuut.GEEN_TYPE) {
                lijktOp = true;
            }

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();

                String param = (String) pairs.getKey();

                if (param.equalsIgnoreCase(attribuut.getLabel()) || param.equalsIgnoreCase(attribuut.getNaam())) {
                    String[] waardes = (String[]) pairs.getValue();
                    String value = waardes[0];

                    Integer getal = null;

                    try {
                        getal = Integer.parseInt(value);
                    } catch (NumberFormatException nfex) {
                    }

                    if (lijktOp && !value.equals("") && getal == null) {
                        values.add("%" + value + "%");
                    } else {
                        values.add(value);
                    }
                }
            }
        }

        return values.toArray(new String[0]);
    }

    private Map createInputParamsMap(Set<ZoekAttribuut> zoekVelden, HttpServletRequest request) {
        Map searchParams = new HashMap();
        Map params = request.getParameterMap();

        for (ZoekAttribuut attribuut : zoekVelden) {
            Boolean lijktOp = false;

            if (attribuut.getType() == Attribuut.GEEN_TYPE) {
                lijktOp = true;
            }

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();

                String param = (String) pairs.getKey();

                if (param.equalsIgnoreCase(attribuut.getLabel()) || param.equalsIgnoreCase(attribuut.getNaam())) {
                    String[] waardes = (String[]) pairs.getValue();
                    String value = waardes[0];

                    Integer getal = null;

                    try {
                        getal = Integer.parseInt(value);
                    } catch (NumberFormatException nfex) {
                    }

                    searchParams.put(attribuut.getLabel(), value);
                }
            }
        }

        return searchParams;
    }
}
