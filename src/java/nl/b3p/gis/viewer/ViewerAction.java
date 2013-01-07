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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.UserKaartgroep;
import nl.b3p.gis.viewer.db.UserKaartlaag;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.gis.viewer.db.UserService;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.wms.capabilities.Layer;
import nl.b3p.wms.capabilities.ServiceProvider;
import nl.b3p.wms.capabilities.SrsBoundingBox;
import nl.b3p.wms.capabilities.TileSet;
import nl.b3p.zoeker.configuratie.ZoekAttribuut;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import nl.b3p.zoeker.services.A11YResult;
import nl.b3p.zoeker.services.Zoeker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securityfilter.filter.SecurityFilter;

public class ViewerAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(ViewerAction.class);
    protected static final String LIST = "list";
    protected static final String LOGIN = "login";
    protected static final String SIMPLE_VIEWER_FW = "simpleviewer";
    private static final String PAGE_GISVIEWER_TAB = "gisviewer_tab";

    /*Mogelijke request waarden*/
    //De themaid's die zichtbaar moeten zijn in de kaart en aangevinkt moeten zijn. Komma gescheiden
    public static final String ID = "id";
    //de clusterIds waarvan de themas zichtbaar moeten zijn in de kaart (aangevinkt). Komma gescheiden
    public static final String CLUSTERID = "clusterId";
    //de extent waarde kaart naar moet zoomen
    public static final String EXTENT = "extent";
    //de scale waar de kaart naar moet zoomen
    public static final String RESOLUTION = "resolution";
    //de zoekConfiguratie id die moet worden gebruikt om te zoeken
    public static final String SEARCHCONFIGID = "searchConfigId";
    // zoekconfig naam om op te zoeken
    public static final String SEARCHCONFIGNAME = "searchConfigName";
    //het woord waarop gezocht moet worden in de zoekconfiguratie
    public static final String SEARCH = "search";
    //de actie die gedaan kan worden na het zoeken (filter,zoom,highlight)
    public static final String SEARCHACTION = "searchAction";
    //Het thema waarop een sld moet worden toegepast (alleen filter en highlight)    
    public static final String SEARCHSLDTHEMAID = "searchSldThemaId";//deze niet meer gebruiken!
    public static final String SEARCHID = "searchId";
    //Het cluster waarop een sld moet worden toegepast (alleen filter en highlight)
    public static final String SEARCHSLDCLUSTERID = "searchSldClusterId";//deze niet meer gebruiken!
    public static final String SEARCHCLUSTERID = "searchClusterId";
    //De waarde die moet worden gebruikt in het sld als value
    public static final String SEARCHSLDVISIBLEVALUE = "searchSldVisibleValue";
    //Het tabblad dat actief moet zijn (moet wel enabled zijn)
    public static final String ACTIVETAB = "activeTab";
    //De enabled tabs die mogelijk zijn. Komma gescheiden
    public static final String ENABLEDTAB = "enabledTabs";
    //De mappen die je opengeklapt wil hebben
    public static final String EXPANDNODES = "expandNodes";
    /*Einde mogelijke request waarden*/
    public static final String ZOEKCONFIGURATIES = "zoekconfiguraties";
    public static final double squareRootOf2 = Math.sqrt(2);
    public static final String APPCODE = "appCode";
    public static final String A11Y_VIEWER_FW = "a11yViewer";
    public static final String A11Y = "accessibility";

    /**
     * Return een hashmap die een property koppelt aan een Action.
     *
     * @return Map hashmap met action properties.
     */
    // <editor-fold defaultstate="" desc="protected Map getActionMethodPropertiesMap()">
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(LIST);
        hibProp.setDefaultForwardName(LIST);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(LIST, hibProp);

        return map;
    }
    // </editor-fold>

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
    // <editor-fold defaultstate="" desc="public ActionForward knop(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response)">
    public ActionForward list(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        List themalist = getValidUserThemas(false, null, request);
        request.setAttribute("themalist", themalist);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }
    // </editor-fold>

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
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        /* User is null bij ongeldige inloggegevens, ip check of als de
         * Applicatie geen gebruikerscode heeft gekoppeld. */
        if (user == null) {
            SecurityFilter.saveRequestInformation(request);

            log.debug("Ongeldige gebruiker. Terug naar login form.");

            return mapping.findForward(LOGIN);
        }

        String a11yStr = request.getParameter(ViewerAction.A11Y);

        if (a11yStr != null && !a11yStr.equals("")) {
            Integer nr = new Integer(a11yStr);

            if (nr != null && nr == 1) {
                return mapping.findForward(A11Y_VIEWER_FW);
            }
        }

        createLists(dynaForm, request);

        Map configMap = (Map) request.getAttribute("configMap");
        if (configMap != null) {
            String viewerTemplate = (String) configMap.get("viewerTemplate");
            if (viewerTemplate != null && viewerTemplate.equals("embedded")) {
                return mapping.findForward(SIMPLE_VIEWER_FW);
            }
        }
        return mapping.findForward(SUCCESS);
    }

    @Override
    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        super.createLists(dynaForm, request);

        List ctl = SpatialUtil.getValidClusters();
        List themalist = getValidUserThemas(false, ctl, request);

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        String userCode = user.getCode();

        /* Applicatie instellingen ophalen */
        Applicatie app = null;
        String appCode = request.getParameter(ViewerAction.APPCODE);
        if (appCode != null && appCode.length() > 0) {
            app = KaartSelectieUtil.getApplicatie(appCode);
        }

        if (app == null) {
            Applicatie defaultApp = KaartSelectieUtil.getDefaultApplicatie();
            app = defaultApp;
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        /* Als app nog steeds null is dan is er geen default app en is er geen geldige
         appcode meegegeven. Dan een default app maken en opslaan */
        if (app == null) {
            app = KaartSelectieUtil.getNewApplicatie();
            app.setDefault_app(true);
            app.setNaam("standaard");

            sess.save(app);
            sess.flush();

            ConfigKeeper.writeDefaultApplicatie(app.getCode());
        }

        if (app != null && !app.getNaam().equals("")) {
            request.setAttribute("appName", app.getNaam());
        }

        if (app != null) {
            /* Appcode klaarzetten voor kaartselectie form */
            HttpSession session = request.getSession(true);
            session.setAttribute("appCode", app.getCode());
            request.setAttribute("bookmarkAppcode", app.getCode());

            appCode = app.getCode();
            app.setDatum_gebruikt(new Date());

            sess.save(app);
            sess.flush();
        }

        Map rootClusterMap = getClusterMap(themalist, ctl, null, userCode);
        List actieveThemas = null;
        if (FormUtils.nullIfEmpty(request.getParameter(ID)) != null) {
            actieveThemas = new ArrayList();
            String[] ids = request.getParameter(ID).split(",");
            for (int i = 0; i < ids.length; i++) {
                try {
                    int id = Integer.parseInt(ids[i]);
                    actieveThemas.add(id);
                } catch (NumberFormatException nfe) {
                    log.error("Id geen integer. ", nfe);
                }
            }
            if (actieveThemas.isEmpty()) {
                actieveThemas = null;
            }
        }
        String lastActiefThemaId = null;
        if (actieveThemas != null) {
            lastActiefThemaId = "" + actieveThemas.get(actieveThemas.size() - 1);
        }
        Themas actiefThema = SpatialUtil.getThema(lastActiefThemaId);

        List actieveClusters = null;
        if (FormUtils.nullIfEmpty(request.getParameter(CLUSTERID)) != null) {
            actieveClusters = new ArrayList();
            String[] ids = request.getParameter(CLUSTERID).split(",");
            for (int i = 0; i < ids.length; i++) {
                try {
                    int id = Integer.parseInt(ids[i]);
                    actieveClusters.add(id);
                } catch (NumberFormatException nfe) {
                    log.error("ClusterId geen integer. ", nfe);
                }
            }
            if (actieveClusters.isEmpty()) {
                actieveClusters = null;
            }
        }

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(appCode);

        /* Indien niet aanwezig dan defaults laden */
        if ((map == null) || (map.size() < 1)) {
            map = configKeeper.getDefaultInstellingen();
        }

        /* Kijken of er een dropdown gemaakt moet worden voor user wms lijst 
         * in kaartselectiescherm */
        Boolean useUserWmsDropdown = (Boolean) map.get("useUserWmsDropdown");

        if (useUserWmsDropdown != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute("useUserWmsDropdown", useUserWmsDropdown);
        }

        request.setAttribute("configMap", map);

        JSONObject treeObject = createJasonObject(rootClusterMap, actieveThemas, actieveClusters, user, appCode);
        request.setAttribute("tree", treeObject);

        /* tree op alfabet zetten voor bepaalde klanten */
        if (map != null) {
            String treeOrder = (String) map.get("treeOrder");
            if (treeOrder != null && treeOrder.equals("alphabet")) {
                convertTreeOrderPlim(treeObject);
            }
        }

        /* Klaarzetten UserLayers uit eigen toegevoegde WMS Services */
        setUserviceTrees(appCode, request);

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 28992);
        Polygon extentBbox = null;
        Polygon fullExtentBbox = null;


        //stukje voor BBox toevoegen.
        Set bboxen = null;
        if (user.getSp().getTopLayer() != null) {
            bboxen = user.getSp().getTopLayer().getSrsbb();
            Iterator it = bboxen.iterator();
            while (it.hasNext()) {
                SrsBoundingBox bbox = (SrsBoundingBox) it.next();
                if (FormUtils.nullIfEmpty(bbox.getMaxx()) != null && FormUtils.nullIfEmpty(bbox.getMaxy()) != null && FormUtils.nullIfEmpty(bbox.getMinx()) != null && FormUtils.nullIfEmpty(bbox.getMiny()) != null) {
                    if (bbox.getSrs() != null && bbox.getSrs().equalsIgnoreCase("epsg:28992")) {
                        request.setAttribute("fullExtent", bbox.getMinx() + "," + bbox.getMiny() + "," + bbox.getMaxx() + "," + bbox.getMaxy());
                        try {
                            Coordinate[] ca = getCoordinateArray(
                                    Double.parseDouble(bbox.getMinx()),
                                    Double.parseDouble(bbox.getMiny()),
                                    Double.parseDouble(bbox.getMaxx()),
                                    Double.parseDouble(bbox.getMaxy()));
                            LinearRing lr = geometryFactory.createLinearRing(ca);
                            fullExtentBbox = geometryFactory.createPolygon(lr, null);
                        } catch (NumberFormatException nfe) {
                            log.error("BBOX fullextent wrong format: " + request.getAttribute("fullExtent"));
                        }
                        break;
                    }
                }
            }
        }
        String extent = null;
        //Controleer of een extent is meegegeven en of de extent een bbox is van 4 getallen
        if (request.getParameter(EXTENT) != null && request.getParameter(EXTENT).split(",").length == 4) {
            try {
                String requestExtent = request.getParameter(EXTENT);
                int test = Integer.parseInt(requestExtent.split(",")[0]);
                test = Integer.parseInt(requestExtent.split(",")[1]);
                test = Integer.parseInt(requestExtent.split(",")[2]);
                test = Integer.parseInt(requestExtent.split(",")[3]);
                extent = requestExtent;
            } catch (NumberFormatException nfe) {
                log.error("1 of meer van de opgegeven extent coordinaten is geen getal.");
                extent = null;
            }
        }

        /* Als extent niet aan url is meegegeven kijken of er een is ingesteld voor
         * de Applicatie */
        if (map != null && extent == null) {
            String cfgExtent = (String) map.get("extent");
            if (cfgExtent != null && cfgExtent.split(",").length == 4) {
                extent = cfgExtent;
            }
        }

        if (FormUtils.nullIfEmpty(request.getParameter(RESOLUTION)) != null) {
            request.setAttribute(RESOLUTION, request.getParameter(RESOLUTION));
        }

        //als er geen juiste extent is gevonden en er is een actiefthemaid meegegeven gebruik de bbox van die layer
        if (extent == null && actiefThema != null) {
            Layer layer = user.getLayer(actiefThema.getWms_layers_real());
            if (layer != null) {
                bboxen = layer.getSrsbb();
                if (bboxen != null) {
                    Iterator i = bboxen.iterator();
                    while (i.hasNext()) {
                        SrsBoundingBox bbox = (SrsBoundingBox) i.next();
                        if (FormUtils.nullIfEmpty(bbox.getMaxx()) != null && FormUtils.nullIfEmpty(bbox.getMaxy()) != null && FormUtils.nullIfEmpty(bbox.getMinx()) != null && FormUtils.nullIfEmpty(bbox.getMiny()) != null) {
                            if (bbox.getSrs() != null && bbox.getSrs().equalsIgnoreCase("epsg:28992")) {
                                extent = "" + bbox.getMinx() + "," + bbox.getMiny() + "," + bbox.getMaxx() + "," + bbox.getMaxy();
                                try {
                                    Coordinate[] ca = getCoordinateArray(
                                            Double.parseDouble(bbox.getMinx()),
                                            Double.parseDouble(bbox.getMiny()),
                                            Double.parseDouble(bbox.getMaxx()),
                                            Double.parseDouble(bbox.getMaxy()));
                                    LinearRing lr = geometryFactory.createLinearRing(ca);
                                    extentBbox = geometryFactory.createPolygon(lr, null);
                                } catch (NumberFormatException nfe) {
                                    log.error("BBOX extent wrong format: " + extent);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        //Maak de extent niet groter dan de fullExtent.
        if (extent != null) {
            if (fullExtentBbox != null && extentBbox != null) {
                Polygon ip = (Polygon) fullExtentBbox.intersection(extentBbox);
                if (!ip.isEmpty()) {
                    Envelope env = ip.getEnvelopeInternal();
                    if (env != null) {
                        extent = "" + env.getMinX() + "," + env.getMinY() + "," + env.getMaxX() + "," + env.getMaxY();
                    }
                }
            }
            request.setAttribute(EXTENT, extent);
        }
        //set search params
        if (FormUtils.nullIfEmpty(request.getParameter(SEARCHCONFIGID)) != null && FormUtils.nullIfEmpty(request.getParameter(SEARCH)) != null) {
            try {
                request.setAttribute(SEARCHCONFIGID, new Integer(request.getParameter(SEARCHCONFIGID)));
                request.setAttribute(SEARCH, request.getParameter(SEARCH));
                //searchAction: Wat te doen met het gevonden initZoek resultaat.
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHACTION)) != null) {
                    request.setAttribute(SEARCHACTION, request.getParameter(SEARCHACTION));
                }
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHID)) != null) {
                    request.setAttribute(SEARCHID, request.getParameter(SEARCHID));
                    //om backwards compatible te houden
                } else if (FormUtils.nullIfEmpty(request.getParameter(SEARCHSLDTHEMAID)) != null) {
                    request.setAttribute(SEARCHID, request.getParameter(SEARCHSLDTHEMAID));
                    //als geen searchId is gebruikt gebruik dan voor de searchId's de id's.
                } else if (FormUtils.nullIfEmpty(request.getParameter(ID)) != null) {
                    request.setAttribute(SEARCHID, request.getParameter(ID));
                }
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHCLUSTERID)) != null) {
                    request.setAttribute(SEARCHCLUSTERID, request.getParameter(SEARCHCLUSTERID));
                    //om backwards compatible te houden
                } else if (FormUtils.nullIfEmpty(request.getParameter(SEARCHSLDCLUSTERID)) != null) {
                    request.setAttribute(SEARCHCLUSTERID, request.getParameter(SEARCHSLDCLUSTERID));
                    //als er geen searchClusterId is gebruikt dan gebruik de clusterId
                } else if (FormUtils.nullIfEmpty(request.getParameter(CLUSTERID)) != null) {
                    request.setAttribute(SEARCHCLUSTERID, request.getParameter(CLUSTERID));
                }
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHSLDVISIBLEVALUE)) != null) {
                    request.setAttribute(SEARCHSLDVISIBLEVALUE, request.getParameter(SEARCHSLDVISIBLEVALUE));
                }
            } catch (NumberFormatException nfe) {
                log.error(SEARCHCONFIGID + " = NAN: " + request.getParameter(SEARCHCONFIGID));
            }
        }

        /* search param klaarzetten voor zoeken via params */
        String temp = request.getParameter(SEARCHCONFIGID);
        String zoekIngangNaam = request.getParameter(SEARCHCONFIGNAME);

        Integer zoekConfigId = null;
        Set<ZoekAttribuut> velden = null;

        if (temp != null && !temp.equals("")) {
            zoekConfigId = new Integer(temp);
        }

        //zoekconfiguraties inlezen.        
        List zoekconfiguraties = Zoeker.getZoekConfiguraties();
        List zoekconfiguratiesJson = new ArrayList();
        if (zoekconfiguraties != null) {
            for (int i = 0; i < zoekconfiguraties.size(); i++) {
                ZoekConfiguratie zc = (ZoekConfiguratie) zoekconfiguraties.get(i);
                zoekconfiguratiesJson.add(zc.toJSON());

                Integer zcId = zc.getId();
                if (zoekIngangNaam == null && zoekConfigId != null && zcId.intValue() == zoekConfigId.intValue()) {
                    velden = zc.getZoekVelden();
                } else if (zoekIngangNaam != null && zc.getNaam() != null && zc.getNaam().equals(zoekIngangNaam)) {
                    velden = zc.getZoekVelden();
                    zoekConfigId = zc.getId();
                }
            }
        }
        if (zoekconfiguraties != null) {
            request.setAttribute(ZOEKCONFIGURATIES, zoekconfiguratiesJson);
        }

        if (zoekConfigId != null || zoekIngangNaam != null) {
            if (velden != null) {
                String params = "";

                int i = 0;
                for (ZoekAttribuut za : velden) {
                    String veldNaam = null;
                    String waarde = null;

                    if (za.getLabel() != null) {
                        veldNaam = za.getLabel();
                        waarde = request.getParameter(veldNaam);
                    }

                    if (waarde == null && za.getAttribuutLocalnaam() != null) {
                        veldNaam = za.getAttribuutLocalnaam();
                        waarde = request.getParameter(veldNaam);
                    }

                    if (waarde == null && za.getAttribuutnaam() != null) {
                        veldNaam = za.getAttribuutnaam();
                        waarde = request.getParameter(veldNaam);
                    }

                    if (waarde == null) {
                        String upper = veldNaam.toUpperCase();
                        waarde = request.getParameter(upper);
                    }

                    if (waarde == null) {
                        String lower = veldNaam.toLowerCase();
                        waarde = request.getParameter(lower);
                    }

                    if (waarde == null) {
                        waarde = "";
                    }

                    /* Bij het zoekveld type lijkt op moet er ook %% om de waarde heen
                     anders vind de back-end niets */
                    if (za.getType() == 0) {
                        //waarde = "%" + waarde.trim() + "%";
                    }

                    if (i < 1) {
                        params += waarde;
                    } else {
                        params += "," + waarde;
                    }

                    i++;
                }

                request.setAttribute(SEARCHCONFIGID, zoekConfigId);
                request.setAttribute(SEARCH, params);
            }
        }

        //set de actieve tabs en enabled tabs
        JSONArray enabledTabs = null;
        String[] enabledTokens = null;
        String enabledTab = FormUtils.nullIfEmpty(request.getParameter(ENABLEDTAB));
        String activeTab = FormUtils.nullIfEmpty(request.getParameter(ACTIVETAB));
        if (enabledTab != null) {
            //stop alle enabled tabs in een jsonarray en controleer of de activeTab er in zit.
            enabledTabs = new JSONArray();
            enabledTokens = enabledTab.split(",");
            boolean containsActiveTab = false;
            for (int i = 0; i < enabledTokens.length; i++) {
                enabledTabs.put(enabledTokens[i]);
                if (enabledTokens[i].equalsIgnoreCase(activeTab)) {
                    containsActiveTab = true;
                }
            }
            if (!containsActiveTab && activeTab != null) {
                enabledTabs.put(activeTab);
            }
        }
        if (activeTab != null) {
            request.setAttribute(ACTIVETAB, activeTab);
        }
        if (enabledTabs != null) {
            request.setAttribute(ENABLEDTAB, enabledTabs);
        }

        //kijk of er in de tree iets moet worden uitgeklapt
        if (FormUtils.nullIfEmpty(request.getParameter(EXPANDNODES)) != null) {
            String collapsedNodes = request.getParameter(EXPANDNODES);
            JSONArray nodes = new JSONArray();
            String[] nodeIds = collapsedNodes.split(",");
            for (int i = 0; i < nodeIds.length; i++) {
                nodes.put(nodeIds[i]);
            }
            request.setAttribute(EXPANDNODES, nodes);
        }

        //get tekstblokken
        List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_TAB);
        request.setAttribute("tekstBlokken", tekstBlokken);

        /* Set search result with startlocation on request. Used in search tab 
         * and placing a location marker on map */
        HttpSession session = request.getSession(true);
        A11YResult a11yResult = (A11YResult) session.getAttribute("a11yResult");

        if (a11yResult != null && a11yResult.getStartWkt() != null) {
            request.setAttribute("a11yResultMap", a11yResult.getResultMap());

            Geometry startGeom = createGeomFromWkt(a11yResult.getStartWkt());
            if (startGeom != null && startGeom.getCentroid() != null) {
                Double startLocationX = startGeom.getCentroid().getX();
                Double startLocationY = startGeom.getCentroid().getY();

                request.setAttribute("startLocationX", startLocationX);
                request.setAttribute("startLocationY", startLocationY);
            }
        }
    }

    private Coordinate[] getCoordinateArray(double minx, double miny, double maxx, double maxy) {
        Coordinate[] ca = new Coordinate[5];
        ca[0] = new Coordinate(minx, miny);
        ca[1] = new Coordinate(minx, maxy);
        ca[2] = new Coordinate(maxx, maxy);
        ca[3] = new Coordinate(maxx, miny);
        ca[4] = new Coordinate(minx, miny);
        return ca;
    }

    private Map getClusterMap(List themalist, List clusterlist, Clusters rootCluster, String userCode) throws JSONException, Exception {
        if (themalist == null || clusterlist == null) {
            return null;
        }
        List childrenList = getThemaList(themalist, rootCluster);

        List subclusters = null;
        Iterator it = clusterlist.iterator();
        while (it.hasNext()) {
            Clusters cluster = (Clusters) it.next();

            if (rootCluster == cluster.getParent()) {
                Map clusterMap = getClusterMap(themalist, clusterlist, cluster, userCode);
                if (clusterMap == null || clusterMap.isEmpty()) {
                    continue;
                }
                if (subclusters == null) {
                    subclusters = new ArrayList();
                }
                subclusters.add(clusterMap);
            }
        }

        if ((childrenList == null || childrenList.isEmpty()) && ((subclusters == null || subclusters.isEmpty()))) {
            return null;
        }
        Map clusterNode = new HashMap();
        clusterNode.put("subclusters", subclusters);
        clusterNode.put("children", childrenList);
        clusterNode.put("cluster", rootCluster);

        return clusterNode;
    }

    private List getThemaList(List themalist, Clusters rootCluster) throws JSONException, Exception {
        if (themalist == null) {
            return null;
        }
        ArrayList children = null;
        Iterator it = themalist.iterator();
        while (it.hasNext()) {
            Themas thema = (Themas) it.next();
            if (thema.getCluster() == rootCluster) {
                if (children == null) {
                    children = new ArrayList();
                }
                children.add(thema);
            }
        }
        return children;
    }

    private void convertTreeOrderPlim(JSONObject treeObject) throws JSONException {
        if (!treeObject.isNull("children")) {
            JSONArray childArray = treeObject.getJSONArray("children");
            TreeMap tm = new TreeMap();
            for (int i = 0; i < childArray.length(); i++) {
                JSONObject childObject = childArray.getJSONObject(i);
                convertTreeOrderPlim(childObject);
                String title = childObject.getString("title");
                tm.put(title, childObject);
            }
            Collection c = tm.values();
            treeObject.put("children", c);
        }
        return;
    }

    protected JSONObject createJasonObject(Map rootClusterMap, List actieveThemas, List actieveClusters, GisPrincipal user, String appCode) throws JSONException {
        JSONObject root = new JSONObject().put("id", "root").put("type", "root").put("title", "root");

        if (rootClusterMap == null || rootClusterMap.isEmpty()) {
            return root;
        }

        List clusterMaps = (List) rootClusterMap.get("subclusters");
        if (clusterMaps == null || clusterMaps.isEmpty()) {
            return root;
        }

        /* ophalen user kaartgroepen om eventueel cluster aan te zetten. */
        List<UserKaartgroep> groepen = SpatialUtil.getUserKaartGroepen(appCode);
        List<UserKaartlaag> userlagen = SpatialUtil.getUserKaartLagen(appCode);

        JSONArray children = getSubClusters(clusterMaps, null, actieveThemas, actieveClusters, user, 0, appCode, groepen, userlagen);
        root.put("children", children);

        return root;
    }

    private JSONArray getSubClusters(List subclusterMaps, JSONArray clusterArray,
            List actieveThemas, List actieveClusters, GisPrincipal user, int order,
            String appCode, List<UserKaartgroep> groepen, List<UserKaartlaag> userlagen)
            throws JSONException {

        if (subclusterMaps == null) {
            return clusterArray;
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Iterator it = subclusterMaps.iterator();
        while (it.hasNext()) {
            Map clMap = (Map) it.next();

            Clusters cluster = (Clusters) clMap.get("cluster");

            /* Cluster hoeft niet getoond te worden als er eigen kaartlagen
             aangezet zijn maar hier hoort het cluster niet bij */
            boolean showCluster = false;
            for (UserKaartlaag laag : userlagen) {

                Integer themaId = laag.getThemaid();

                Themas thema = (Themas) sess.get(Themas.class, themaId);

                if (isInCluster(thema, cluster)) {
                    showCluster = true;
                    break;
                }
            }

            /* controleren of cluster default aan staat in user kaartgroepen */
            boolean defaultOn = false;
            if (groepen != null && groepen.size() > 0) {
                for (UserKaartgroep groep : groepen) {
                    if (groep.getClusterid() == cluster.getId()) {
                        if (groep.getDefault_on()) {
                            defaultOn = true;
                        }
                    }
                }
            }

            JSONObject jsonCluster = new JSONObject();
            if (cluster.getId() != null) {
                jsonCluster.put("id", "c" + cluster.getId().intValue());
            }
            jsonCluster.put("type", "child");

            String titel = cluster.getNaam();
            if (titel == null || titel.equals("")) {
                titel = "(geen naam opgegeven)";
            }

            jsonCluster.put("title", titel);
            jsonCluster.put("cluster", true);

            if (cluster.isExclusive_childs()) {
                jsonCluster.put("exclusive_childs", true);
            } else {
                jsonCluster.put("exclusive_childs", false);
            }
            setExtraClusterProperties(jsonCluster, cluster);
            if (actieveClusters != null && actieveClusters.contains(cluster.getId())) {
                jsonCluster.put("active", true);
                jsonCluster.put("visible", true);
            } else if ((actieveClusters == null || actieveClusters.isEmpty()) && cluster.isDefault_visible()) {
                jsonCluster.put("visible", true);
            } else {
                jsonCluster.put("visible", false);
            }
            if (cluster.getMetadatalink() != null) {
                String metadatalink = cluster.getMetadatalink();
                metadatalink = metadatalink.replaceAll("%id%", "" + cluster.getId());
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            if (defaultOn) {
                jsonCluster.put("visible", true);
            }

            if (userlagen != null && userlagen.size() > 0 && !showCluster && !defaultOn) {
                jsonCluster.put("hide_tree", true);
                jsonCluster.put("callable", false);
                jsonCluster.put("background", false);
                jsonCluster.put("hide_legend", true);
                jsonCluster.put("visible", false);
                jsonCluster.put("active", false);
            }

            List childrenList = (List) clMap.get("children");

            JSONArray childrenArray = new JSONArray();
            order = getChildren(childrenArray, childrenList, actieveThemas, user, order, appCode, userlagen);

            List subsubclusterMaps = (List) clMap.get("subclusters");
            childrenArray = getSubClusters(subsubclusterMaps, childrenArray, actieveThemas, actieveClusters, user, order, appCode, groepen, userlagen);

            if (childrenArray.length() > 0) {
                jsonCluster.put("children", childrenArray);

                if (clusterArray == null) {
                    clusterArray = new JSONArray();
                }
                clusterArray.put(jsonCluster);
            }


        }

        return clusterArray;
    }

    private int getChildren(JSONArray childrenArray, List children, List actieveThemas,
            GisPrincipal user, int order, String appCode, List<UserKaartlaag> lagen)
            throws JSONException {

        if (children == null || childrenArray == null) {
            return order;
        }

        Iterator it = children.iterator();
        while (it.hasNext()) {
            Themas th = (Themas) it.next();

            /* controleren of thema in user kaartlagen voorkomt */
            boolean defaultOn = false;
            if (lagen != null && lagen.size() > 0) {
                boolean isInList = false;
                for (UserKaartlaag laag : lagen) {
                    if (laag.getThemaid().equals(th.getId())) {
                        isInList = true;

                        if (laag.getDefault_on()) {
                            defaultOn = true;
                        }
                        break;
                    }
                }
                if (!isInList) {
                    continue;
                }
            }

            /* TODO: validAdmindataSource ging eerst via th.hasValidAdmindataSource(user)
             * maar dit duurt soms erg lang, nu wordt er gekeken of er een gegevensbron is */
            boolean validAdmindataSource = false;
            Gegevensbron themaGb = th.getGegevensbron();

            if (themaGb != null && themaGb.getAdmin_pk() != null) {
                validAdmindataSource = true;
            }

            // Check of er een admin source is met rechten
            if (th.isAnalyse_thema() && !validAdmindataSource) {
                log.debug(th.getNaam() + "' is analyse kaartlaag maar"
                        + " hier is nog geen gegevensbron voor geconfigureerd.");
            }

            Integer themaId = th.getId();
            String ttitel = th.getNaam();

            if (ttitel == null || ttitel.equals("")) {
                ttitel = "(geen naam opgegeven)";
            }

            JSONObject jsonCluster = new JSONObject().put("id", themaId).put("type", "child").put("title", ttitel).put("cluster", false);

            order++;
            jsonCluster.put("order", order);

            if (th.getOrganizationcodekey() != null && th.getOrganizationcodekey().length() > 0) {
                jsonCluster.put("organizationcodekey", th.getOrganizationcodekey().toUpperCase());
            } else {
                jsonCluster.put("organizationcodekey", "");
            }

            if (th.getMaptipstring() != null) {
                jsonCluster.put("maptipfield", th.getMaptipstring());
            }

            if (actieveThemas != null && themaId != null && actieveThemas.contains(themaId)) {
                jsonCluster.put("visible", "on");
                if (th.isAnalyse_thema() && validAdmindataSource) {
                    jsonCluster.put("analyse", "active");
                } else {
                    jsonCluster.put("analyse", "off");
                }
            } else if (actieveThemas == null || actieveThemas.isEmpty()) {
                if (th.isVisible()) {
                    jsonCluster.put("visible", "on");
                } else {
                    jsonCluster.put("visible", "off");
                }

                if (th.isAnalyse_thema() && validAdmindataSource) {
                    jsonCluster.put("analyse", "on");
                } else {
                    jsonCluster.put("analyse", "off");
                }
            }

            /* user kaartlaag default on zetten */
            if (defaultOn && lagen.size() > 0) {
                jsonCluster.put("visible", "on");
            } else if (!defaultOn && lagen.size() > 0) {
                jsonCluster.put("visible", "off");

                /* Toch nog aanzetten als id is meegegeven in url */
                if (actieveThemas != null && actieveThemas.contains(themaId)) {
                    jsonCluster.put("visible", "on");
                }
            }

            /* Extra property die gebruikt wordt voor highLightThemaObject
             * deze gebruikte eerste analyse property maar de highlight knop
             * hoeft nu alleen maar te highlighten en geen info meer op te halen.
             * Dus analyse moet ook aanstaan als er geen valdAdmindatasource is.
             */
            if (th.isAnalyse_thema()) {
                jsonCluster.put("highlight", "on");
            } else {
                jsonCluster.put("highlight", "off");
            }

            /* Property die gebruikt kan worden bij het downloaden van Shape en GML.
             * Er is dan een gegevensbron nodig */
            if (validAdmindataSource) {
                jsonCluster.put("gegevensbronid", themaGb.getId());
            } else {
                jsonCluster.put("gegevensbronid", -1);
            }

            /*Set some cluster properties that are used by the thema.*/
            Clusters cluster = th.getCluster();
            if (cluster.getId() != null) {
                jsonCluster.put("clusterid", "c" + cluster.getId().intValue());
            }
            setExtraClusterProperties(jsonCluster, cluster);

            Layer layer = null;
            if (th.getWms_layers_real() != null) {
                jsonCluster.put("wmslayers", th.getWms_layers_real());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers_real() != null && !validAdmindataSource) {
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers_real());
                }
                if (th.getWms_legendlayer_real() != null) {
                    jsonCluster.put("legendurl", user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer_real())));
                }
                layer = user.getLayer(th.getWms_layers_real());
            } else {
                jsonCluster.put("wmslayers", th.getWms_layers());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers() != null && !validAdmindataSource) {
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers());
                }
                if (th.getWms_legendlayer() != null) {
                    jsonCluster.put("legendurl", user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer())));
                }
                layer = user.getLayer(th.getWms_layers());
            }
            //toevoegen scale hints
            if (layer != null) {
                NumberFormat formatter = new DecimalFormat("#.#####");
                /*LET OP!!!!!!
                 * in json wordt nu de scalehint(max/min) gevuld met de resolutie!
                 */
                double shmax = -1.0;
                try {
                    shmax = Double.parseDouble(layer.getScaleHintMax());
                    shmax /= squareRootOf2;
                } catch (NumberFormatException nfe) {
                    log.debug("max scale hint not valid: " + layer.getScaleHintMax());
                }
                if (shmax > 0) {
                    jsonCluster.put("scalehintmax", formatter.format(shmax));
                }
                double shmin = -1.0;
                try {
                    shmin = Double.parseDouble(layer.getScaleHintMin());
                    shmin /= squareRootOf2;
                } catch (NumberFormatException nfe) {
                    log.debug("min scale hint not valid: " + layer.getScaleHintMin());
                }
                if (shmin > 0) {
                    jsonCluster.put("scalehintmin", formatter.format(shmin));
                }
                //haal de eventuele tile gegevens op.
                ServiceProvider sp = user.getSp();
                if (sp != null) {
                    TileSet ts = sp.getTileSet(layer);
                    if (ts != null) {
                        jsonCluster.put("tiled", true);
                        jsonCluster.put("resolutions", ts.getResolutions());
                        jsonCluster.put("tileWidth", ts.getWidth());
                        jsonCluster.put("tileHeight", ts.getHeight());
                        jsonCluster.put("tileFormat", ts.getFormat());
                        jsonCluster.put("tileLayers", ts.getLayerString());

                        if (ts.getBoundingBox() != null) {
                            jsonCluster.put("tileSrs", ts.getBoundingBox().getSrs());
                        } else {
                            jsonCluster.put("tileSrs", "EPSG:28992");
                        }

                        jsonCluster.put("tileVersion", sp.getWmsVersion());
                        jsonCluster.put("tileStyles", ts.getStyles());

                        if (ts.getBoundingBox() != null) {
                            String bbox = "";
                            bbox += ts.getBoundingBox().getMinx() + ",";
                            bbox += ts.getBoundingBox().getMiny() + ",";
                            bbox += ts.getBoundingBox().getMaxx() + ",";
                            bbox += ts.getBoundingBox().getMaxy();

                            jsonCluster.put("tileBoundingBox", bbox);
                        } else {
                            jsonCluster.put("tileBoundingBox", "-285401.92,22598.08,595401.9199999999,903401.9199999999");

                        }
                    }
                }
            }

            if (th.getMetadata_link() != null) {
                String metadatalink = th.getMetadata_link();
                metadatalink = metadatalink.replaceAll("%id%", "" + themaId);
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            if (th.getStyle() != null) {
                jsonCluster.put("use_style", th.getStyle());
            } else {
                jsonCluster.put("use_style", "default");
            }

            childrenArray.put(jsonCluster);
        }

        return order;
    }

    private void setExtraClusterProperties(JSONObject jsonCluster, Clusters cluster) throws JSONException {
        if (cluster.isDefault_cluster()) {
            jsonCluster.put("default_cluster", true);
        } else {
            jsonCluster.put("default_cluster", false);
        }
        if (cluster.isHide_legend()) {
            jsonCluster.put("hide_legend", true);
        } else {
            jsonCluster.put("hide_legend", false);
        }
        if (cluster.isHide_tree()) {
            jsonCluster.put("hide_tree", true);
        } else {
            jsonCluster.put("hide_tree", false);
        }
        if (cluster.isBackground_cluster()) {
            jsonCluster.put("background", true);
        } else {
            jsonCluster.put("background", false);
        }
        if (cluster.isExtra_level()) {
            jsonCluster.put("extra_level", true);
        } else {
            jsonCluster.put("extra_level", false);
        }
        if (cluster.isCallable()) {
            jsonCluster.put("callable", true);
        } else {
            jsonCluster.put("callable", false);
        }
    }

    private void setUserviceTrees(String appCode, HttpServletRequest request) throws JSONException, Exception {
        List<JSONObject> servicesTrees = new ArrayList();

        List<UserService> services = SpatialUtil.getValidUserServices(appCode);

        /* per service een tree maken */
        for (UserService service : services) {
            JSONObject tree = createUserServiceTree(service);
            servicesTrees.add(tree);
        }

        /* lijst van tree's klaarzetten */
        request.setAttribute("servicesTrees", servicesTrees);
    }

    protected JSONObject createUserServiceTree(UserService service) throws JSONException, Exception {
        JSONObject root = new JSONObject();

        root.put("id", "0");
        root.put("title", service.getGroupname());
        root.put("name", service.getGroupname());

        List<UserLayer> ctl = SpatialUtil.getValidUserLayers(service);

        Map rootLayerMap = getUserLayersMap(ctl, null);
        List lMaps = (List) rootLayerMap.get("sublayers");

        root.put("children", getSubLayers(lMaps, null, service));

        return root;
    }

    private Map getUserLayersMap(List layerList, UserLayer root) throws JSONException, Exception {
        if (layerList == null) {
            return null;
        }

        List subLayers = null;
        Iterator it = layerList.iterator();
        while (it.hasNext()) {
            UserLayer la = (UserLayer) it.next();
            if (root == la.getParent()) {
                Map lMap = getUserLayersMap(layerList, la);
                if (lMap == null || lMap.isEmpty()) {
                    continue;
                }
                if (subLayers == null) {
                    subLayers = new ArrayList();
                }
                subLayers.add(lMap);
            }
        }

        Map lNode = new HashMap();
        lNode.put("sublayers", subLayers);
        lNode.put("userlayer", root);

        return lNode;
    }

    private JSONArray getSubLayers(List subLayers, JSONArray layersArray, UserService service)
            throws JSONException {

        if (subLayers == null) {
            return layersArray;
        }

        String serviceUrl = service.getUrl();
        String serviceSld = service.getSld_url();

        Iterator it = subLayers.iterator();
        while (it.hasNext()) {
            Map lMap = (Map) it.next();

            UserLayer layer = (UserLayer) lMap.get("userlayer");

            JSONObject jsonLayer = new JSONObject();

            jsonLayer.put("id", layer.getId().intValue());
            jsonLayer.put("serviceid", layer.getServiceid().getId().intValue());
            jsonLayer.put("title", layer.getTitle());
            jsonLayer.put("name", layer.getName());
            jsonLayer.put("queryable", layer.getQueryable());
            jsonLayer.put("scalehintmin", layer.getScalehint_min());
            jsonLayer.put("scalehintmax", layer.getScalehint_max());
            jsonLayer.put("use_style", layer.getUse_style());
            jsonLayer.put("sld_part", layer.getSld_part());
            jsonLayer.put("show", layer.getShow());
            jsonLayer.put("default_on", layer.getDefault_on());
            jsonLayer.put("service_url", serviceUrl);
            jsonLayer.put("service_sld", serviceSld);

            List subsubMaps = (List) lMap.get("sublayers");

            if (subsubMaps != null && !subsubMaps.isEmpty()) {
                JSONArray childrenArray = new JSONArray();

                childrenArray = getSubLayers(subsubMaps, childrenArray, service);
                jsonLayer.put("children", childrenArray);
            }

            if (layersArray == null) {
                layersArray = new JSONArray();
            }

            layersArray.put(jsonLayer);
        }

        return layersArray;
    }

    private boolean isInCluster(Themas thema, Clusters inCluster) {
        if (thema == null || thema.getCluster() == null) {
            return false;
        } else {
            return isInCluster(thema.getCluster(), inCluster);
        }
    }

    private boolean isInCluster(Clusters cluster, Clusters inCluster) {
        if (cluster.getId().equals(inCluster.getId())) {
            return true;
        }
        if (cluster.getParent() == null) {
            return false;
        } else {
            return isInCluster(cluster.getParent(), inCluster);
        }

    }

    private Geometry createGeomFromWkt(String wkt) {
        WKTReader wktreader = new WKTReader(new GeometryFactory(new PrecisionModel(), 28992));
        Geometry geom = null;
        try {
            geom = wktreader.read(wkt);
        } catch (Exception e) {
            log.error("Fout bij parsen wkt geometry", e);
        }

        return geom;
    }
}
