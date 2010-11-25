package nl.b3p.gis.viewer;

import nl.b3p.gis.geotools.DataStoreUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.admindata.GegevensBronBean;
import nl.b3p.gis.viewer.admindata.LabelBean;
import nl.b3p.gis.viewer.admindata.RecordBean;
import nl.b3p.gis.viewer.admindata.RecordChildBean;
import nl.b3p.gis.viewer.admindata.RecordValueBean;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import nl.b3p.gis.viewer.admindata.CollectAdmindata;

public class GetViewerDataAction extends BaseGisAction {

    private static final Log logger = LogFactory.getLog(GetViewerDataAction.class);

    protected static final String ADMINDATA = "admindata";
    protected static final String MULTI_ADMINDATA = "multi_admindata";
    protected static final String AANVULLENDEINFO = "aanvullendeinfo";
    protected static final String METADATA = "metadata";
    protected static final String OBJECTDATA = "objectdata";
    protected static final String PK_FIELDNAME_PARAM = "pkFieldName";

    /**
     * Return een hashmap die een property koppelt aan een Action.
     *
     * @return Map hashmap met action properties.
     */
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(OBJECTDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.objectdata.failed");
        map.put(OBJECTDATA, hibProp);

        hibProp = new ExtendedMethodProperties(ADMINDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.admindata.failed");
        map.put(ADMINDATA, hibProp);

        hibProp = new ExtendedMethodProperties(MULTI_ADMINDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.admindata.failed");
        map.put(MULTI_ADMINDATA, hibProp);

        hibProp = new ExtendedMethodProperties(AANVULLENDEINFO);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.aanvullende.failed");
        map.put(AANVULLENDEINFO, hibProp);

        hibProp = new ExtendedMethodProperties(METADATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.metadata.failed");
        map.put(METADATA, hibProp);

        return map;
    }

    /**
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     */
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return mapping.findForward(SUCCESS);
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * administratieve data.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema_items
     * regels
     */
    public ActionForward admindata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayList themas = getThemas(mapping, dynaForm, request);
        ArrayList regels = new ArrayList();
        ArrayList ti = new ArrayList();
        if (themas == null) {
            request.setAttribute("regels_list", regels);
            request.setAttribute("thema_items_list", ti);
            return mapping.findForward("admindata");
        }

        collectThemaRegels(mapping, request, themas, regels, ti, false);
        request.setAttribute("regels_list", regels);
        request.setAttribute("thema_items_list", ti);

        /* Bepalen welke jsp (layout) voor admindata gebruikt moet worden
         * 1 = uitgebreide jsp
         * 2 = simpel naast elkaar
         * TODO: 3 = simpel onder elkaar
         */
        int aantalThemas = themas.size();

        /* Default ophalen uit configKeeper */
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Set roles = user.getRoles();

        /* Ophalen rollen in configuratie database */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = null;

        try {
            rollenPrio = configKeeper.getConfiguratie("rollenPrio", "rollen");
        } catch (Exception ex) {
            logger.debug("Fout bij ophalen configKeeper configuratie: " + ex);
        }

        /* alleen doen als configuratie tabel bestaat */
        if (rollenPrio != null && rollenPrio.getPropval() != null) {
            String[] configRollen = rollenPrio.getPropval().split(",");

            /* init loop vars */
            String rolnaam = "";
            String inlogRol = "";

            Map map = null;
            Boolean foundRole = false;

            /* Zoeken of gebruiker een rol heeft die in de rollen
             * configuratie voorkomt. Hoogste rol wordt geladen */
            for (int i = 0; i < configRollen.length; i++) {

                if (foundRole) {
                    break;
                }

                rolnaam = configRollen[i];

                /* per rol uit config database loopen door
                 * toegekende rollen */
                Iterator iter = roles.iterator();

                while (iter.hasNext()) {
                    inlogRol = iter.next().toString();

                    if (rolnaam.equals(inlogRol)) {
                        map = configKeeper.getConfigMap(rolnaam);
                        foundRole = true;

                        break;
                    }
                }
            }

            /* als gevonden rol geen configuratie records heeft dan defaults laden */
            if ((map == null) || (map.size() < 1)) {
                map = configKeeper.getConfigMap("default");
            }

            String layoutAdminData = "";

            /* Indien maar 1 thema pak dan de instelling van Thema object
            of als deze niet ingesteld is pak dan de global configuratie setting */
            if (aantalThemas == 1) {
                Themas t = (Themas) themas.get(0);
                String themaLayout = t.getLayoutadmindata();

                if ((themaLayout == null) || themaLayout.equals("")) {
                    layoutAdminData = (String) map.get("layoutAdminData");
                } else {
                    layoutAdminData = themaLayout;
                }
            } else {
                layoutAdminData = (String) map.get("layoutAdminData");
            }

            return mapping.findForward(layoutAdminData);
        }

        /* geen config gevonden of ingesteld pak de uitgebreide versie */
        return mapping.findForward("admindata1");
    }

    public ActionForward multi_admindata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        /* Ophalen aangevinkte themas (kaartlagen) */
        List themas = getThemas(mapping, dynaForm, request);

        if (themas == null) {
            return mapping.findForward(MULTI_ADMINDATA);
        }

        List beans = new ArrayList();

        /* Per thema een GegevensBronBean vullen */
        Iterator iter = themas.iterator();
        while(iter.hasNext()) {
            Themas thema = (Themas)iter.next();
            Gegevensbron gb = thema.getGegevensbron();

            if (gb != null) {
                int gbId = thema.getGegevensbron().getId().intValue();

                CollectAdmindata cad = new CollectAdmindata();
                GegevensBronBean bean = cad.createTestGegevensBronBean(99);

                if (bean != null) {
                    beans.add(bean);
                }
            }
        }

        /* Klaarzetten List van GegevensBronBeans */
        request.setAttribute("beans", beans);

        return mapping.findForward(MULTI_ADMINDATA);
    }

    protected void collectThemaRegels(ActionMapping mapping, HttpServletRequest request,
            ArrayList themas, ArrayList regels, ArrayList ti, boolean locatie) {
        for (int i = 0; i < themas.size(); i++) {
            Themas t = (Themas) themas.get(i);
            if (locatie && !t.isLocatie_thema()) {
                continue;
            }

            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            if (t.hasValidAdmindataSource(user)) {
                try {
                    Gegevensbron gb = t.getGegevensbron();
                    List thema_items = SpatialUtil.getThemaData(gb, true);
                    int themadatanummer = 0;
                    themadatanummer = ti.size();
                    for (int a = 0; a < ti.size(); a++) {
                        if (compareThemaDataLists((List) ti.get(a), thema_items)) {
                            themadatanummer = a;
                            break;
                        }
                    }

                    Bron b = gb.getBron(request);
                    List l = null;
                    if (b != null) {
                        if (themadatanummer == regels.size()) {
                            regels.add(new ArrayList());
                        }
                        l = getThemaObjectsWithGeom(t, thema_items, request);
                    }
                    if (l != null && l.size() > 0) {
                        ((ArrayList) regels.get(themadatanummer)).addAll(l);
                        if (themadatanummer == ti.size()) {
                            ti.add(thema_items);
                        }
                    }
                } catch (Exception e) {
                    String mapserver4Hack = "msQueryByRect(): Search returned no results. No matching record(s) found.";
                    if (mapserver4Hack.equalsIgnoreCase(e.getMessage())) {
                        // mapserver 4 returns service exception when no hits, this is not compliant.
                    } else {
                        String msg = e.getMessage();

                        if (msg != null) {
                            if (msg.contains("PropertyDescriptor is null - did you request a property that does not exist?")) {
                                msg = "U vraagt een attribuut op dat niet bestaat, waarschijnlijk is de configuratie niet in orde, raadpleeg de beheerder!";
                            }
                        } else {
                            msg = "Kon objectinfo niet ophalen.";
                        }

                        logger.error("Fout bij laden admindata voor thema: " + t.getNaam() + ":", e);
                        addAlternateMessage(mapping, request, "", "thema: " + t.getNaam() + ", " + msg);
                    }
                }
            }
        }

    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * aanvullende info.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema_items
     * regels
     */
    public ActionForward aanvullendeinfo(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(mapping, dynaForm, request);

        if (gb == null) {
            return mapping.findForward("aanvullendeinfo");
        }

        String gegevensbronId = (String) request.getParameter("gegevensbronid");

        if (gegevensbronId != null) {
            gb = SpatialUtil.getGegevensbron(gegevensbronId);

            String fkId = (String) request.getParameter("id");
            request.setAttribute("fkId", fkId);
        }

        List<ThemaData> thema_items = SpatialUtil.getThemaData(gb, false);
        request.setAttribute("thema_items", thema_items);

        Bron b = gb.getBron(request);

        if (b != null) {
            request.setAttribute("regels", getThemaObjectsWithId(gb, thema_items, request));
        }
        return mapping.findForward("aanvullendeinfo");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * metadata.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema
     */
    public ActionForward metadata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(mapping, dynaForm, request);
        request.setAttribute("themas", t);
        return mapping.findForward("metadata");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * "Gebieden" tab.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * object_data
     *
     */
    public ActionForward objectdata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayList themas = getThemas(mapping, dynaForm, request);
        ArrayList regels = new ArrayList();
        ArrayList ti = new ArrayList();
        if (themas == null) {
            request.setAttribute("regels_list", regels);
            return mapping.findForward("admindata");
        }

        collectThemaRegels(mapping, request, themas, regels, ti, true);
        request.setAttribute("regels_list", regels);
        request.setAttribute("thema_items_list", ti);

        return mapping.findForward("objectdata");
    }

    protected List<AdminDataRowBean> getThemaObjectsWithGeom(Themas t, List<ThemaData> thema_items, HttpServletRequest request) throws Exception {
        if (t == null) {
            return null;
        }
        if (thema_items == null || thema_items.isEmpty()) {
            //throw new Exception("Er is geen themadata geconfigureerd voor thema: " + t.getNaam() + " met id: " + t.getId());
            return null;
        }
        Geometry geom = getGeometry(request);
        Filter extraFilter = getExtraFilter(t, request);

        // filter op locatie thema's

        Gegevensbron gb = t.getGegevensbron();
        Bron b = gb.getBron(request);

        List<String> propnames = DataStoreUtil.basisRegelThemaData2PropertyNames(thema_items);
        List<Feature> features = DataStoreUtil.getFeatures(b, gb, geom, extraFilter, propnames, null, false);
        List<AdminDataRowBean> regels = new ArrayList();
        for (int i = 0; i < features.size(); i++) {
            Feature f = (Feature) features.get(i);
            regels.add(getRegel(f, gb, thema_items));
        }
        return regels;
    }

    protected List getThemaObjectsWithId(Gegevensbron gb, List thema_items, HttpServletRequest request) throws Exception {
        if (gb == null) {
            return null;
        }
        if (thema_items == null || thema_items.isEmpty()) {
            return null;
        }

        String adminPk = DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart();
        String id = null;
        Filter filter = null;
        if (adminPk != null) {

            id = request.getParameter(adminPk);

            /* Als er een foreign key is ingevuld dan een filter toevoegen op
             * dit veld */
            String fkField = gb.getAdmin_fk();
            String fkId = null;

            if (fkField != null) {
                fkId = (String) request.getAttribute("fkId");

                if (fkId != null) {
                    filter = FilterBuilder.createEqualsFilter(fkField, fkId);
                }
            }

            /* alleen adminpk als filter adden als foreign key leeg is */
            if (id != null && fkId == null) {
                filter = FilterBuilder.createEqualsFilter(adminPk, id);
            } else {
                // tbv data2info meerdere id's
                String primaryKeys = request.getParameter("primaryKeys");
                if (primaryKeys != null) {
                    String[] primaryKeysArray = primaryKeys.split(",");
                    filter = FilterBuilder.createOrEqualsFilter(adminPk, primaryKeysArray);
                }
            }
        }

        List regels = new ArrayList();

        boolean addKaart = false;
        if (FormUtils.nullIfEmpty(request.getParameter("addKaart")) != null) {
            addKaart = true;
        }

        List<ReferencedEnvelope> kaartEnvelopes = new ArrayList<ReferencedEnvelope>();
        Bron b = gb.getBron(request);
        List<String> propnames = DataStoreUtil.themaData2PropertyNames(thema_items);
        List<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propnames, null, true);
        for (int i = 0; i < features.size(); i++) {
            Feature f = (Feature) features.get(i);
            if (addKaart) {
                ReferencedEnvelope env = DataStoreUtil.convertFeature2Envelop(f);
                if (env != null) {
                    kaartEnvelopes.add(env);
                }
            }
            regels.add(getRegel(f, gb, thema_items));
        }
        if (addKaart) {
            request.setAttribute("envelops", kaartEnvelopes);
        }
        return regels;
    }

    private Geometry getGeometry(HttpServletRequest request) {
        String geom = request.getParameter("geom");
        double distance = getDistance(request);
        Geometry geometry = null;
        if (geom != null) {
            geometry = SpatialUtil.geometrieFromText(geom, 28992);
        } else {
            GeometryFactory gf = new GeometryFactory();
            double[] coords = getCoords(request);
            if (coords.length == 2) {
                geometry = gf.createPoint(new Coordinate(coords[0], coords[1]));
            } else if (coords.length == 10) {
                Coordinate[] coordinates = new Coordinate[5];
                for (int i = 0; i < coordinates.length; i++) {
                    coordinates[i] = new Coordinate(coords[i * 2], coords[i * 2 + 1]);
                }
                geometry = gf.createPolygon(gf.createLinearRing(coordinates), null);
            }
        }
        if (geometry != null) {
            geometry = geometry.buffer(distance);
        }
        return geometry;
    }
}
