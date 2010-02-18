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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.wms.capabilities.Layer;
import nl.b3p.wms.capabilities.SrsBoundingBox;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
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

public class ViewerAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(ViewerAction.class);
    protected static final String LIST = "list";
    protected static final String LOGIN = "login";
    /*Mogelijke request waarden*/
    //De themaid's die zichtbaar moeten zijn in de kaart en aangevinkt moeten zijn. Komma gescheiden
    public static final String ID="id";
    //de clusterIds waarvan de themas zichtbaar moeten zijn in de kaart (aangevinkt). Komma gescheiden
    public static final String CLUSTERID="clusterId";
    //de extent waarde kaart naar moet zoomen
    public static final String EXTENT="extent";
    //de zoekConfiguratie id die moet worden gebruikt om te zoeken
    public static final String SEARCHCONFIGID ="searchConfigId";
    //het woord waarop gezocht moet worden in de zoekconfiguratie
    public static final String SEARCH ="search";
    //de actie die gedaan kan worden na het zoeken (filter,zoom,highlight)
    public static final String SEARCHACTION ="searchAction";
    //Het thema waarop een sld moet worden toegepast (alleen filter en highlight)
    public static final String SEARCHSLDTHEMAID ="searchSldThemaId";
    //Het cluster waarop een sld moet worden toegepast (alleen filter en highlight)
    public static final String SEARCHSLDCLUSTERID ="searchSldClusterId";
    //De waarde die moet worden gebruikt in het sld als value
    public static final String SEARCHSLDVISIBLEVALUE="searchSldVisibleValue";

    //Het tabblad dat actief moet zijn (moet wel enabled zijn)
    public static final String ACTIVETAB="activeTab";
    //De enabled tabs die mogelijk zijn. Komma gescheiden
    public static final String ENABLEDTAB="enabledTabs";
    /*Einde mogelijke request waarden*/

    public static final String ZOEKCONFIGURATIES="zoekconfiguraties";
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

        List themalist = getValidThemas(false, null, request);
        request.setAttribute("themalist", themalist);

        addDefaultMessage(mapping, request);
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
        //als er geen user principal is (ook geen anoniem) dan forwarden naar de login.
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        if (user == null) {
            log.info("Geen user beschikbaar, ook geen anoniem. Forward naar login om te proberen een user te maken met login gegevens.");
            return mapping.findForward(LOGIN);
        }
        createLists(dynaForm, request);
        return mapping.findForward(SUCCESS);
    }

    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        super.createLists(dynaForm, request);
        List ctl = SpatialUtil.getValidClusters();
        List themalist = getValidThemas(false, ctl, request);
        Map rootClusterMap = getClusterMap(themalist, ctl, null);
        List actieveThemas = null;
        if (FormUtils.nullIfEmpty(request.getParameter(ID))!=null){
            actieveThemas = new ArrayList();
            String[] ids=request.getParameter(ID).split(",");
            for (int i =0; i < ids.length; i++){
                try{
                    int id= Integer.parseInt(ids[i]);
                    actieveThemas.add(id);
                }catch (NumberFormatException nfe){
                    log.error("Id geen integer. ",nfe);
                }                
            }
            if (actieveThemas.size()==0){
                actieveThemas=null;
            }
        }
        String lastActiefThemaId = null;
        if (actieveThemas!=null)
            lastActiefThemaId=""+actieveThemas.get(actieveThemas.size()-1);
        Themas actiefThema = SpatialUtil.getThema(lastActiefThemaId);

        List actieveClusters=null;
        if (FormUtils.nullIfEmpty(request.getParameter(CLUSTERID))!=null){
            actieveClusters=new ArrayList();
            String[] ids=request.getParameter(CLUSTERID).split(",");
            for (int i =0; i < ids.length; i++){
                try{
                    int id= Integer.parseInt(ids[i]);
                    actieveClusters.add(id);
                }catch (NumberFormatException nfe){
                    log.error("ClusterId geen integer. ",nfe);
                }
            }
            if (actieveClusters.size()==0){
                actieveClusters=null;
            }
        }
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        request.setAttribute("tree", createJasonObject(rootClusterMap, actieveThemas,actieveClusters, user));

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 28992);
        Polygon extentBbox = null;
        Polygon fullExtentBbox = null;

        //stukje voor BBox toevoegen.
        Set bboxen=null;
        if (user.getSp().getTopLayer()!=null){
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
        if(FormUtils.nullIfEmpty(request.getParameter(SEARCHCONFIGID))!=null && FormUtils.nullIfEmpty(request.getParameter(SEARCH))!=null){
            try{
                request.setAttribute(SEARCHCONFIGID, new Integer(request.getParameter(SEARCHCONFIGID)));
                request.setAttribute(SEARCH, request.getParameter(SEARCH));
                //searchAction: Wat te doen met het gevonden initZoek resultaat.
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHACTION))!=null){
                    request.setAttribute(SEARCHACTION,request.getParameter(SEARCHACTION));
                }
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHSLDTHEMAID))!=null){
                    request.setAttribute(SEARCHSLDTHEMAID,request.getParameter(SEARCHSLDTHEMAID));
                }
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHSLDCLUSTERID))!=null){
                    request.setAttribute(SEARCHSLDCLUSTERID,request.getParameter(SEARCHSLDCLUSTERID));
                }
                if (FormUtils.nullIfEmpty(request.getParameter(SEARCHSLDVISIBLEVALUE))!=null){
                    request.setAttribute(SEARCHSLDVISIBLEVALUE,request.getParameter(SEARCHSLDVISIBLEVALUE));
                }
            }catch(NumberFormatException nfe){
                log.error(SEARCHCONFIGID+" = NAN: "+request.getParameter(SEARCHCONFIGID));
            }
        }
        //zoekconfiguraties inlezen.
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfiguraties = Zoeker.getZoekConfiguraties();
        List zoekconfiguratiesJson= new ArrayList();
        if (zoekconfiguraties!=null){
            for (int i=0; i < zoekconfiguraties.size(); i++){
                ZoekConfiguratie zc = (ZoekConfiguratie) zoekconfiguraties.get(i);
                zoekconfiguratiesJson.add(zc.toJSON());
            }
        }
        if (zoekconfiguraties!=null){
            request.setAttribute(ZOEKCONFIGURATIES, zoekconfiguratiesJson);
        }
        //set de actieve tabs en enabled tabs
        JSONArray enabledTabs=null;
        String[] enabledTokens=null;
        String enabledTab=FormUtils.nullIfEmpty(request.getParameter(ENABLEDTAB));
        String activeTab=FormUtils.nullIfEmpty(request.getParameter(ACTIVETAB));
        if (enabledTab!=null){
            //stop alle enabled tabs in een jsonarray en controleer of de activeTab er in zit.
            enabledTabs= new JSONArray();
            enabledTokens =enabledTab.split(",");
            boolean containsActiveTab=false;
            for (int i=0; i < enabledTokens.length; i++){
                enabledTabs.put(enabledTokens[i]);
                if (enabledTokens[i].equalsIgnoreCase(activeTab)){
                    containsActiveTab=true;
                }
            }
            if (!containsActiveTab && activeTab!=null){
                enabledTabs.put(activeTab);
            }
        }
        if (activeTab!=null)
            request.setAttribute(ACTIVETAB,activeTab);
        if (enabledTabs!=null)
            request.setAttribute(ENABLEDTAB, enabledTabs);
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

    private Map getClusterMap(List themalist, List clusterlist, Clusters rootCluster) throws JSONException, Exception {
        if (themalist == null || clusterlist == null) {
            return null;
        }
        List childrenList = getThemaList(themalist, rootCluster);

        List subclusters = null;
        Iterator it = clusterlist.iterator();
        while (it.hasNext()) {
            Clusters cluster = (Clusters) it.next();
            if (rootCluster == cluster.getParent()) {
                Map clusterMap = getClusterMap(themalist, clusterlist, cluster);
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
        if (children != null) {
            Collections.reverse(children);
        }
        return children;
    }

    protected JSONObject createJasonObject(Map rootClusterMap, List actieveThemas,List actieveClusters,GisPrincipal user) throws JSONException {
        JSONObject root = new JSONObject().put("id", "root").put("type", "root").put("title", "root");
        if (rootClusterMap == null || rootClusterMap.isEmpty()) {
            return root;
        }
        List clusterMaps = (List) rootClusterMap.get("subclusters");
        if (clusterMaps == null || clusterMaps.isEmpty()) {
            return root;
        }
        root.put("children", getSubClusters(clusterMaps, null, actieveThemas,actieveClusters,user));

        return root;
    }

    private JSONArray getSubClusters(List subclusterMaps, JSONArray clusterArray, List actieveThemas,List actieveClusters,GisPrincipal user) throws JSONException {
        if (subclusterMaps == null) {
            return clusterArray;
        }
        Iterator it = subclusterMaps.iterator();
        while (it.hasNext()) {
            Map clMap = (Map) it.next();

            Clusters cluster = (Clusters) clMap.get("cluster");
            JSONObject jsonCluster = new JSONObject();
            if (cluster.getId()!=null){
                jsonCluster.put("id", "c" + cluster.getId().intValue());
            }
            jsonCluster.put("type", "child");
            jsonCluster.put("title", cluster.getNaam());
            jsonCluster.put("cluster", true);
            setExtraClusterProperties(jsonCluster,cluster);
            if (actieveClusters!=null &&  actieveClusters.contains(cluster.getId())){
                jsonCluster.put("active",true);
                jsonCluster.put("visible",true);
            }else if (cluster.isDefault_visible()){
                jsonCluster.put("visible",true);
            }else{
                jsonCluster.put("visible",false);
            }
            if (cluster.getMetadatalink() != null) {
                String metadatalink = cluster.getMetadatalink();
                metadatalink = metadatalink.replaceAll("%id%", "" + cluster.getId());
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }
            List childrenList = (List) clMap.get("children");
            JSONArray childrenArray = getChildren(childrenList, actieveThemas,user);
            List subsubclusterMaps = (List) clMap.get("subclusters");
            childrenArray = getSubClusters(subsubclusterMaps, childrenArray, actieveThemas,actieveClusters,user);
            jsonCluster.put("children", childrenArray);

            if (clusterArray == null) {
                clusterArray = new JSONArray();
            }
            clusterArray.put(jsonCluster);

        }
        return clusterArray;
    }

    private JSONArray getChildren(List children, List actieveThemas,GisPrincipal user) throws JSONException {
        if (children == null) {
            return null;
        }
        JSONArray childrenArray = null;
        Iterator it = children.iterator();
        while (it.hasNext()) {
            Themas th = (Themas) it.next();
            Integer themaId = th.getId();
            String ttitel = th.getNaam();
            JSONObject jsonCluster = new JSONObject().put("id", themaId).put("type", "child").put("title", ttitel).put("cluster", false);

            if (th.getOrganizationcodekey() != null && th.getOrganizationcodekey().length() > 0) {
                jsonCluster.put("organizationcodekey", th.getOrganizationcodekey().toUpperCase());
            } else {
                jsonCluster.put("organizationcodekey", "");
            }

            if(th.getMaptipstring() != null){
                jsonCluster.put("maptipfield", th.getMaptipstring());
            }

            if (actieveThemas != null && themaId != null && actieveThemas.contains(themaId)) {
                jsonCluster.put("visible", "on");
                if (th.isAnalyse_thema()) {
                    jsonCluster.put("analyse", "active");
                } else {
                    jsonCluster.put("analyse", "off");
                }
            }
            else {
                if (th.isVisible()) {
                    jsonCluster.put("visible", "on");
                } else {
                    jsonCluster.put("visible", "off");
                }
                if (th.isAnalyse_thema()) {
                    jsonCluster.put("analyse", "on");
                } else {
                    jsonCluster.put("analyse", "off");
                }
            }
            /*Set some cluster properties that are used by the thema.*/
            Clusters cluster = th.getCluster();
            setExtraClusterProperties(jsonCluster,cluster);

            if (th.getWms_layers_real() != null) {
                jsonCluster.put("wmslayers", th.getWms_layers_real());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers_real()!=null && (th.getAdmin_tabel()==null || th.getAdmin_tabel().length()==0)){
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers_real());
                }
                if (th.getWms_legendlayer_real()!=null){
                    jsonCluster.put("legendurl",user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer_real())));
                }
            } else {
                jsonCluster.put("wmslayers", th.getWms_layers());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers()!=null && th.getAdmin_tabel()==null){
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers());
                }
                if (th.getWms_legendlayer()!=null){
                    jsonCluster.put("legendurl",user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer())));
                }
            }
            if (th.getMetadata_link() != null) {
                String metadatalink = th.getMetadata_link();
                metadatalink = metadatalink.replaceAll("%id%", "" + themaId);
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            if (childrenArray == null) {
                childrenArray = new JSONArray();
            }
            childrenArray.put(jsonCluster);
        }

        return childrenArray;
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
}
