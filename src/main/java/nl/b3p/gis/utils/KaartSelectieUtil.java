package nl.b3p.gis.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.db.CyclomediaAccount;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.UserKaartgroep;
import nl.b3p.gis.viewer.db.UserKaartlaag;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.gis.viewer.db.UserLayerStyle;
import nl.b3p.gis.viewer.db.UserService;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.wms.capabilities.Layer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.ows.StyleImpl;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Boy de Wit
 */
public class KaartSelectieUtil {
    
    private static final Log log = LogFactory.getLog(KaartSelectieUtil.class);

    public static void populateKaartSelectieForm(String appCode, HttpServletRequest request)
            throws JSONException, Exception {

        setKaartlagenTree(request, appCode);
        setUserServiceTrees(request, appCode);
    }

    public static String[] addDefaultOnValues(String[] defaults, String[] current) {
        List col2 = new ArrayList();

        /* eerst huidige waardes erin stoppen */
        col2.addAll(Arrays.asList(current));

        /* daarna de nieuwe default waardes toevoegen */
        for (int j = 0; j < defaults.length; j++) {
            if (!col2.contains(defaults[j])) {
                col2.add(defaults[j]);
            }
        }

        return (String[]) col2.toArray(new String[0]);
    }

    public static void removeExistingUserKaartgroepAndUserKaartlagen(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartgroep> groepen = sess.createQuery("from UserKaartgroep where code = :code")
            .setParameter("code", code)
            .list();

        List<UserKaartlaag> lagen = sess.createQuery("from UserKaartlaag where code = :code")
            .setParameter("code", code)
            .list();

        for (UserKaartgroep groep : groepen) {
            sess.delete(groep);
        }

        for (UserKaartlaag laag : lagen) {
            sess.delete(laag);
        }
    }

    public static void removeExistingConfigKeeperSettings(String appCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<Configuratie> configItems = sess.createQuery("from Configuratie where setting = :code")
            .setParameter("code", appCode)
            .list();

        for (Configuratie item : configItems) {
            sess.delete(item);
        }
    }

    public static void resetExistingUserLayers(String code) {
        List<UserService> services = getUserServices(code);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        for (UserService service : services) {
            Set<UserLayer> userLayers = service.getUser_layers();

            for (UserLayer layer : userLayers) {
                layer.setDefault_on(null);
                layer.setShow(null);

                sess.merge(layer);
            }
        }
    }

    public static void saveKaartGroepen(String appCode, String[] kaartgroepenAan, String[] kaartgroepenDefaultAan) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        for (int i = 0; i < kaartgroepenAan.length; i++) {
            Integer clusterId = new Integer(kaartgroepenAan[i]);
            boolean defaultOn = isKaartGroepDefaultOn(kaartgroepenDefaultAan, clusterId);

            UserKaartgroep groep = getUserKaartGroep(appCode, clusterId);

            if (groep != null) {
                groep.setDefault_on(defaultOn);
                sess.merge(groep);
            } else {
                UserKaartgroep newGroep = new UserKaartgroep(appCode, clusterId, defaultOn);
                sess.save(newGroep);
            }
        }
    }

    public static void saveKaartlagen(String appCode, String[] kaartlagenAan,
            String[] kaartlagenDefaultAan) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        for (int j = 0; j < kaartlagenAan.length; j++) {
            Integer themaId = new Integer(kaartlagenAan[j]);
            boolean defaultOn = isKaartlaagDefaultOn(kaartlagenDefaultAan, themaId);

            UserKaartlaag laag = getUserKaartlaag(appCode, themaId);

            if (laag != null) {
                laag.setDefault_on(defaultOn);
                sess.merge(laag);
            } else {
                UserKaartlaag newLaag = new UserKaartlaag(appCode, themaId, defaultOn);
                sess.save(newLaag);
            }
        }
    }

    public static void saveServiceLayers(String[] layersAan, String[] layersDefaultAan) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        for (int k = 0; k < layersAan.length; k++) {
            Integer id = new Integer(layersAan[k]);
            boolean defaultOn = isKaartlaagDefaultOn(layersDefaultAan, id);

            UserLayer userLayer = (UserLayer)sess.get(UserLayer.class, id);
            if (userLayer != null) {
                userLayer.setDefault_on(defaultOn);
                userLayer.setShow(true);

                sess.merge(userLayer);
            }
        }
    }

    public static void saveUserLayerStyles(String[] useLayerStyles) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        
        for (int m=0; m < useLayerStyles.length; m++) {
            String[] useStyle = useLayerStyles[m].split("@");

            Integer layerId = new Integer(useStyle[0]);
            String styleName = useStyle[1];

            if (layerId > 0 && styleName != null) {
                UserLayer ul = (UserLayer) sess.get(UserLayer.class, layerId);

                if (styleName.equals("default")) {
                    ul.setUse_style(null);
                } else {
                    ul.setUse_style(styleName);
                }

                sess.merge(ul);
            }
        }
    }

    public static void saveUserLayerSldParts(String[] userLayerIds, String[] useLayerSldParts) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        for (int n=0; n < useLayerSldParts.length; n++) {
            String sldPart = useLayerSldParts[n];
            Integer layerId = new Integer(userLayerIds[n]);

            if (sldPart != null &&  layerId > 0) {
                UserLayer ul = (UserLayer) sess.get(UserLayer.class, layerId);

                if (sldPart.equals("")) {
                    ul.setSld_part(null);
                } else {
                    ul.setSld_part(sldPart);
                }

                sess.merge(ul);
            }
        }
    }

    public static Applicatie getNewApplicatie() {
        Applicatie app = null;
        app = new Applicatie();

        String appCode = null;
        try {
            appCode = Applicatie.createApplicatieCode();
        } catch (Exception ex) {
            log.error("Fout tijdens maken Applicatie code:", ex);
        }

        app.setCode(appCode);

        /* Applicaties door de beheerder gemaakt zijn standaard read-only */
        app.setRead_only(true);
        app.setUser_copy(false);
        app.setDefault_app(false);
        app.setVersie(1);
        app.setDatum_gebruikt(new Date());

        return app;
    }

    public static Applicatie getApplicatie(String appCode){
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Applicatie app = null;

        List<Applicatie> applicaties = sess.createQuery("from Applicatie where code = :appcode")
                .setParameter("appcode", appCode)
                .setMaxResults(1).list();

        if (applicaties != null && applicaties.size() == 1) {
            return (Applicatie) applicaties.get(0);
        }else{ 
            log.error("Applicatie not found or more then 1 found. Appcode: "+appCode);
        }
        return app;
    }

    public static Applicatie getDefaultApplicatie() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Applicatie app = null;

        List<Applicatie> applicaties = sess.createQuery("from Applicatie"
                + " where default_app = :value")
                .setParameter("value", true)
                .setMaxResults(1).list();

        if (applicaties != null && applicaties.size() == 1) {
            return (Applicatie) applicaties.get(0);
        }

        return app;
    }

    private static void setKaartlagenTree(HttpServletRequest request, String appCode) 
            throws JSONException, Exception {        
        List ctl = SpatialUtil.getValidClusters();

        /* Nodig zodat ongeconfigureerde layers niet getoond worden */
        HibernateUtil.setUseKaartenbalieCluster(false);

        List themalist = getValidThemas(false, ctl, request);
        
        Map rootClusterMap = getClusterMap(themalist, ctl, null);

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        
        JSONObject treeObject = null;
        if (user != null) {
            treeObject = createJasonObject(rootClusterMap, user, appCode);
            
            if (treeObject != null) {
                request.setAttribute("tree", treeObject);
            }
        }
    }

    private static void setUserServiceTrees(HttpServletRequest request, String appCode) throws JSONException, Exception {
        List<JSONObject> servicesTrees = new ArrayList();

        /* user services ophalen */
        List<UserService> services = getUserServices(appCode);

        /* per service een tree maken */
        for (UserService service : services) {
            JSONObject tree = createUserServiceTree(service);
            servicesTrees.add(tree);
        }

        /* lijst van tree's klaarzetten */
        request.setAttribute("servicesTrees", servicesTrees);
    }

    private static JSONObject createUserServiceTree(UserService service) throws JSONException, Exception {
        JSONObject root = new JSONObject();

        root.put("id", "0");
        root.put("title", service.getGroupname());
        root.put("name", service.getGroupname());
        root.put("serviceid", service.getId());

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List ctl = sess.createQuery("from UserLayer where serviceid = :service"
                + " order by name, title, id")
                .setParameter("service", service)
                .list();

        Map rootLayerMap = getUserLayersMap(ctl, null);
        List lMaps = (List) rootLayerMap.get("sublayers");

        root.put("children", getSubLayers(lMaps, null));

        return root;
    }

    private static Map getUserLayersMap(List layerList, UserLayer root) throws JSONException, Exception {
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

    private static JSONArray getSubLayers(List subLayers, JSONArray layersArray) throws JSONException {
        if (subLayers == null) {
            return layersArray;
        }

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

            JSONArray styles = getLayerStyles(layer);
            if (styles != null && styles.length() > 0) {
                jsonLayer.put("styles", styles);
            }

            List subsubMaps = (List) lMap.get("sublayers");

            if (subsubMaps != null && !subsubMaps.isEmpty()) {
                JSONArray childrenArray = new JSONArray();

                childrenArray = getSubLayers(subsubMaps, childrenArray);
                jsonLayer.put("children", childrenArray);
            }

            if (layersArray == null) {
                layersArray = new JSONArray();
            }

            layersArray.put(jsonLayer);
        }

        return layersArray;
    }

    private static JSONArray getLayerStyles(UserLayer layer) throws JSONException {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List<UserLayerStyle> styles = sess.createQuery("from UserLayerStyle where"
                + " layerid = :layer")
                .setParameter("layer", layer)
                .list();

        JSONArray arr = new JSONArray();
        for (UserLayerStyle uls : styles) {
            arr.put(uls.getName());
        }

        return arr;
    }

    private static Map getClusterMap(List themalist, List clusterlist, Clusters rootCluster) throws JSONException, Exception {
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

    private static List getThemaList(List themalist, Clusters rootCluster) throws JSONException, Exception {
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

    private static JSONObject createJasonObject(Map rootClusterMap, GisPrincipal user, String appCode) throws JSONException {
        JSONObject root = new JSONObject().put("id", "root").put("type", "root").put("title", "root");
        if (rootClusterMap == null || rootClusterMap.isEmpty()) {
            return root;
        }
        List clusterMaps = (List) rootClusterMap.get("subclusters");
        if (clusterMaps == null || clusterMaps.isEmpty()) {
            return root;
        }

        /* ophalen user kaartgroepen en kaartlagen */
        List<UserKaartgroep> groepen = getUserKaartGroepen(appCode);
        List<UserKaartlaag> lagen = getUserKaartLagen(appCode);

        root.put("children", getSubClusters(clusterMaps, null, user, 0, groepen, lagen));

        return root;
    }

    private static JSONArray getSubClusters(List subclusterMaps, JSONArray clusterArray,
            GisPrincipal user, int order, List<UserKaartgroep> groepen, List<UserKaartlaag> lagen)
            throws JSONException {

        if (subclusterMaps == null) {
            return clusterArray;
        }

        Iterator it = subclusterMaps.iterator();
        while (it.hasNext()) {
            Map clMap = (Map) it.next();

            Clusters cluster = (Clusters) clMap.get("cluster");
            JSONObject jsonCluster = new JSONObject();
            if (cluster.getId() != null) {
                jsonCluster.put("id", cluster.getId().intValue());
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

            jsonCluster.put("visible", true);

            if (cluster.getMetadatalink() != null) {
                String metadatalink = cluster.getMetadatalink();
                metadatalink = metadatalink.replaceAll("%id%", "" + cluster.getId());
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            /* kijken of cluster voorkomt in user kaartgroepen */
            jsonCluster.put("groepSelected", false);
            jsonCluster.put("groupDefaultOn", false);

            for (UserKaartgroep kaartGroep : groepen) {
                if (cluster.getId().intValue() == kaartGroep.getClusterid().intValue()) {
                    jsonCluster.put("groepSelected", true);

                    if (kaartGroep.getDefault_on()) {
                        jsonCluster.put("groupDefaultOn", true);
                    }
                }
            }

            List childrenList = (List) clMap.get("children");

            JSONArray childrenArray = new JSONArray();
            order = getChildren(childrenArray, childrenList, user, order, lagen);
            List subsubclusterMaps = (List) clMap.get("subclusters");
            childrenArray = getSubClusters(subsubclusterMaps, childrenArray, user, order, groepen, lagen);
            jsonCluster.put("children", childrenArray);

            if (clusterArray == null) {
                clusterArray = new JSONArray();
            }
            clusterArray.put(jsonCluster);

        }
        return clusterArray;
    }

    private static int getChildren(JSONArray childrenArray, List children, GisPrincipal user,
            int order, List<UserKaartlaag> lagen)
            throws JSONException {

        if (children == null || childrenArray == null) {
            return order;
        }

        Iterator it = children.iterator();
        while (it.hasNext()) {
            Themas th = (Themas) it.next();

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

            jsonCluster.put("visible", "on");
            jsonCluster.put("analyse", "off");

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

            /* kijken of thema voorkomt in user kaartlagen */
            jsonCluster.put("kaartSelected", false);
            jsonCluster.put("kaartDefaultOn", false);

            for (UserKaartlaag kaartlaag : lagen) {
                if (th.getId().intValue() == kaartlaag.getThemaid().intValue()) {
                    jsonCluster.put("kaartSelected", true);

                    if (kaartlaag.getDefault_on()) {
                        jsonCluster.put("kaartDefaultOn", true);
                    }
                }
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
                if (th.getWms_querylayers_real() != null) {
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers_real());
                }
                if (th.getWms_legendlayer_real() != null) {
                    jsonCluster.put("legendurl", user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer_real()), th.getStyle()));
                }
                layer = user.getLayer(th.getWms_layers_real());
            } else {
                jsonCluster.put("wmslayers", th.getWms_layers());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers() != null) {
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers());
                }
                if (th.getWms_legendlayer() != null) {
                    jsonCluster.put("legendurl", user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer()), th.getStyle()));
                }
                layer = user.getLayer(th.getWms_layers());
            }
            //toevoegen scale hints
            if (layer != null) {
                NumberFormat formatter = new DecimalFormat("#.#####");

                double shmax = -1.0;
                try {
                    shmax = Double.parseDouble(layer.getScaleHintMax());
                } catch (NumberFormatException nfe) {
                    log.debug("max scale hint not valid: " + layer.getScaleHintMax());
                }
                if (shmax > 0) {
                    jsonCluster.put("scalehintmax", formatter.format(shmax));
                }
                double shmin = -1.0;
                try {
                    shmin = Double.parseDouble(layer.getScaleHintMin());
                } catch (NumberFormatException nfe) {
                    log.debug("min scale hint not valid: " + layer.getScaleHintMin());
                }
                if (shmin > 0) {
                    jsonCluster.put("scalehintmin", formatter.format(shmin));
                }
            }
            if (th.getMetadata_link() != null) {
                String metadatalink = th.getMetadata_link();
                metadatalink = metadatalink.replaceAll("%id%", "" + themaId);
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            childrenArray.put(jsonCluster);
        }

        return order;
    }

    private static void setExtraClusterProperties(JSONObject jsonCluster, Clusters cluster) throws JSONException {
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

    private static List getValidThemas(boolean locatie, List ctl, HttpServletRequest request) {
        List configuredThemasList = SpatialUtil.getValidThemas(locatie);

        List layersFromRoles = null;
        // Zoek layers die via principal binnen komen
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        if (user != null) {
            layersFromRoles = user.getLayerNames(false);
        }
        if (layersFromRoles == null) {
            return null;
        }

        // Voeg alle themas toe die layers hebben die volgens de rollen
        // acceptabel zijn (voldoende rechten dus).
        List layersFound = new ArrayList();
        List checkedThemaList = new ArrayList();
        if (configuredThemasList != null) {
            Iterator it2 = configuredThemasList.iterator();
            while (it2.hasNext()) {
                Themas t = (Themas) it2.next();

                // Als geen check via kaartenbalie dan alle layers doorgeven
                if (checkThemaLayers(t, layersFromRoles)
                        || !HibernateUtil.isCheckLoginKaartenbalie()) {
                    checkedThemaList.add(t);
                    layersFound.add(t.getWms_layers_real());
                }
            }
        }

        // als alleen configureerde layers getoond mogen worden,
        // dan hier stoppen
        if (!HibernateUtil.isUseKaartenbalieCluster()) {
            return checkedThemaList;
        }

        //zoek of maak een cluster aan voor als er kaarten worden gevonden die geen thema hebben.
        Clusters c = SpatialUtil.getDefaultCluster();
        if (c == null) {
            c = new Clusters();
            c.setNaam(HibernateUtil.getKaartenbalieCluster());
            c.setParent(null);
        }

        Iterator it = layersFromRoles.iterator();
        int tid = 100000;
        ArrayList extraThemaList = new ArrayList();
        // Kijk welke lagen uit de rollen nog niet zijn toegevoegd
        // en voeg deze alsnog toe via dummy thema en cluster.
        while (it.hasNext()) {
            String layer = (String) it.next();
            if (layersFound.contains(layer)) {
                continue;
            }

            // Layer bestaat nog niet dus aanmaken
            Layer l = user.getLayer(layer);
            if (l != null) {
                Themas t = new Themas();
                t.setNaam(l.getTitle());
                t.setId(new Integer(tid++));
                if (user.hasLegendGraphic(l)) {
                    t.setWms_legendlayer_real(layer);
                }
                if ("1".equalsIgnoreCase(l.getQueryable())) {
                    t.setWms_querylayers_real(layer);
                }
                t.setWms_layers_real(layer);
                t.setCluster(c);
                // voeg extra laag als nieuw thema toe
                extraThemaList.add(t);
            }
        }
        if (extraThemaList.size() > 0) {
            if (ctl == null) {
                ctl = new ArrayList();
            }
            ctl.add(c);
            for (int i = 0; i < extraThemaList.size(); i++) {
                checkedThemaList.add(extraThemaList.get(i));
            }
        }

        return checkedThemaList;
    }

    private static boolean checkThemaLayers(Themas t, List acceptableLayers) {
        if (t == null || acceptableLayers == null) {
            return false;
        }
        String wmsls = t.getWms_layers_real();
        if (wmsls == null || wmsls.length() == 0) {
            return false;
        }

        String[] wmsla = wmsls.split(",");
        for (int i = 0; i < wmsla.length; i++) {
            if (!acceptableLayers.contains(wmsla[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKaartGroepDefaultOn(String[] kaartgroepenDefaultAan, Integer clusterId) {

        boolean defaultOn = false;

        for (int i = 0; i < kaartgroepenDefaultAan.length; i++) {
            Integer defaultOnId = new Integer(kaartgroepenDefaultAan[i]);

            if (defaultOnId.intValue() == clusterId.intValue()) {
                defaultOn = true;
                break;
            }
        }

        return defaultOn;
    }

    private static boolean isKaartlaagDefaultOn(String[] kaartlagenDefaultAan, Integer themaId) {

        boolean defaultOn = false;

        for (int i = 0; i < kaartlagenDefaultAan.length; i++) {
            Integer defaultOnId = new Integer(kaartlagenDefaultAan[i]);

            if (defaultOnId.intValue() == themaId.intValue()) {
                defaultOn = true;
                break;
            }
        }

        return defaultOn;
    }

    private static UserKaartgroep getUserKaartGroep(String code, Integer clusterId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List groepen = sess.createQuery("from UserKaartgroep where code = :code and"
                + " clusterid = :clusterid").setParameter("code", code)
                .setParameter("clusterid", clusterId)
                .setMaxResults(1)
                .list();

        if (groepen != null && groepen.size() == 1) {
            return (UserKaartgroep) groepen.get(0);
        }

        return null;
    }

    private static UserKaartlaag getUserKaartlaag(String code, Integer themaId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List groepen = sess.createQuery("from UserKaartlaag where code = :code and"
                + " themaid = :themaid").setParameter("code", code)
                .setParameter("themaid", themaId)
                .setMaxResults(1)
                .list();

        if (groepen != null && groepen.size() == 1) {
            return (UserKaartlaag) groepen.get(0);
        }

        return null;
    }

    private static List<UserKaartlaag> getUserKaartLagen(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartlaag> lagen = sess.createQuery("from UserKaartlaag where code = :code")
                .setParameter("code", code)
                .list();

        return lagen;
    }

    private static List<UserKaartgroep> getUserKaartGroepen(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartgroep> groepen = sess.createQuery("from UserKaartgroep where code = :code")
                .setParameter("code", code)
                .list();

        return groepen;
    }

    private static List<UserService> getUserServices(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserService> services = sess.createQuery("from UserService where code = :code")
                .setParameter("code", code)
                .list();

        return services;
    }

    public static void removeExistingUserServices(String appCode) {
        List<UserService> services = getUserServices(appCode);

        for (UserService service : services) {
            removeService(service.getId());
        }
    }

    public static void removeService(Integer serviceId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        UserService service = (UserService) sess.get(UserService.class, serviceId);
        sess.delete(service);
    }
    
    public static void removeCyclomediaAccount(String appCode) {
        ConfigKeeper keeper = new ConfigKeeper();
        CyclomediaAccount cycloeAccount = keeper.getCyclomediaAccount(appCode);
        
        if (cycloeAccount != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            sess.delete(cycloeAccount);
        }
    }

    public static List<org.geotools.data.ows.Layer> getParentLayers(org.geotools.data.ows.Layer[] layers) {
        List<org.geotools.data.ows.Layer> parents = new ArrayList();

        for (int i = 0; i < layers.length; i++) {
            if (layers[i].getChildren().length > 0 || layers[i].getParent() == null) {
                parents.add(layers[i]);
            }
        }

        return parents;
    }

    public static UserLayer createUserLayers(UserService us, org.geotools.data.ows.Layer layer, UserLayer parent) {
        String layerTitle = layer.getTitle();
        String layerName = layer.getName();
        boolean queryable = layer.isQueryable();
        double scaleMin = layer.getScaleDenominatorMin();
        double scaleMax = layer.getScaleDenominatorMax();

        UserLayer ul = new UserLayer();

        ul.setServiceid(us);

        if (layerName != null && !layerName.equals("")) {
            ul.setName(layerName);
        }

        if (layerTitle != null && !layerTitle.equals("")) {
            ul.setTitle(layerTitle);
        }

        ul.setQueryable(queryable);

        if (scaleMin > 0) {
            ul.setScalehint_min(Double.toString(scaleMin));
        }

        if (scaleMax > 0) {
            ul.setScalehint_max(Double.toString(scaleMax));
        }

        List<StyleImpl> styles = layer.getStyles();

        for (StyleImpl style : styles) {
            String styleName = style.getName();

            if (styleName != null && !styleName.equals("")) {
                UserLayerStyle uls = new UserLayerStyle(ul, styleName);
                ul.addStyle(uls);
            }
        }

        if (parent != null) {
            ul.setParent(parent);
        }

        org.geotools.data.ows.Layer[] childs = layer.getChildren();
        for (int i = 0; i < childs.length; i++) {
            UserLayer child = createUserLayers(us, childs[i], ul);
            us.addLayer(child);
        }

        return ul;
    }

    public static boolean userAlreadyHasThisService(String code, String url) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserService> services = sess.createQuery("from UserService where"
                + " code = :code and url = :url")
                .setParameter("code", code)
                .setParameter("url", url)
                .setMaxResults(1)
                .list();

        if (services != null && services.size() == 1) {
            return true;
        }

        return false;
    }

    public static Applicatie copyApplicatie(Applicatie sourceApp, boolean readOnly, boolean userCopy) {
        /* Maak kopie Applicatie */
        Applicatie app = new Applicatie();

        String newCode = null;
        try {
            newCode = Applicatie.createApplicatieCode();
        } catch (Exception ex) {
            log.error("Fout tijdens maken Applicatie code:", ex);
        }

        Integer versie = null;        
        if (sourceApp.getVersie() != null && userCopy) {
            versie = sourceApp.getVersie() + 1;
        } else if (sourceApp.getVersie() != null && !userCopy) {
            versie = sourceApp.getVersie();
        } else {
            versie = 1;
        }

        app.setEmail(sourceApp.getEmail());
        app.setNaam(sourceApp.getNaam());
        app.setCode(newCode);
        app.setGebruikersCode(sourceApp.getGebruikersCode());
        app.setParent(null);
        app.setDatum_gebruikt(new Date());
        app.setRead_only(readOnly);
        app.setUser_copy(userCopy);
        app.setVersie(versie);
        app.setDefault_app(false);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        sess.save(app);
        sess.flush();

        /* Maak kopie configkeeper instellingen */
        copyConfigKeeperSettings(sourceApp.getCode(), newCode);

        /* Maak kopie User kaartgroepen */
        copyUserKaartgroepen(sourceApp.getCode(), newCode);

        /* Maak kopie User kaartlagen */
        copyUserKaartlagen(sourceApp.getCode(), newCode);
        
        /* Kopie CyclomediaAccount */
        copyCyclomediaAccount(sourceApp.getCode(), newCode);

        /* TODO: User services niet kopieeren ? */

        return app;
    }

    private static void copyConfigKeeperSettings(String oldCode, String newCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<Configuratie> configItems = sess.createQuery("from Configuratie where"
                + " setting = :appcode")
                .setParameter("appcode", oldCode)
                .list();

        for (Configuratie item : configItems) {
            Configuratie clone = (Configuratie) item.clone();
            clone.setSetting(newCode);

            sess.save(clone);
            sess.flush();
        }
    }

    private static void copyUserKaartgroepen(String oldCode, String newCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartgroep> groepen = sess.createQuery("from UserKaartgroep where"
                + " code = :appcode")
                .setParameter("appcode", oldCode)
                .list();

        for (UserKaartgroep groep : groepen) {
            UserKaartgroep clone = (UserKaartgroep) groep.clone();
            clone.setCode(newCode);

            sess.save(clone);
            sess.flush();
        }
    }
    
    private static void copyCyclomediaAccount(String oldCode, String newCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<CyclomediaAccount> groepen = sess.createQuery("from CyclomediaAccount where"
                + " app_code = :appcode")
                .setParameter("appcode", oldCode)
                .list();

        for (CyclomediaAccount groep : groepen) {
            CyclomediaAccount clone = (CyclomediaAccount) groep.clone();
            clone.setAppCode(newCode);

            sess.save(clone);
            sess.flush();
        }
    }

    private static void copyUserKaartlagen(String oldCode, String newCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartlaag> lagen = sess.createQuery("from UserKaartlaag where"
                + " code = :appcode")
                .setParameter("appcode", oldCode)
                .list();

        for (UserKaartlaag laag : lagen) {
            UserKaartlaag clone = (UserKaartlaag) laag.clone();
            clone.setCode(newCode);

            sess.save(clone);
            sess.flush();
        }
    }
}
