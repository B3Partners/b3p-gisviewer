package nl.b3p.gis.viewer;

import nl.b3p.gis.geotools.DataStoreUtil;
import com.vividsolutions.jts.geom.Geometry;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.admindata.CollectAdmindata;
import nl.b3p.gis.viewer.admindata.RecordChildBean;
import nl.b3p.gis.viewer.db.CMSPagina;
import nl.b3p.gis.viewer.db.DataTypen;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;

public class GetViewerDataAction extends BaseGisAction {

    private static final Log logger = LogFactory.getLog(GetViewerDataAction.class);
    protected static final String ADMINDATA = "admindata";
    protected static final String AANVULLENDEINFO = "aanvullendeinfo";
    protected static final String METADATA = "metadata";
    protected static final String OBJECTDATA = "objectdata";
    protected static final String ADMINDATAFW = "admindata";
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
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema_items regels
     */
    public ActionForward admindata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.debug("Entering admindata");
        List themas = getThemas(mapping, dynaForm, request);
        List ggbBeans = new ArrayList();
        if (themas != null) {
            ggbBeans = CollectAdmindata.collectGegevensbronRecordChilds(request, themas, false);
        }

        request.setAttribute("beans", ggbBeans);
        boolean onlyFeaturesInGeom = true;
        if (FormUtils.nullIfEmpty(request.getParameter("onlyFeaturesInGeom")) != null) {
            try {
                onlyFeaturesInGeom = Boolean.parseBoolean(request.getParameter("onlyFeaturesInGeom"));
            } catch (Exception e) {
                logger.error("Param 'onlyFeaturesInGeom' is not a boolean", e);
            }
        }

        String bookmarkAppcode = null;
        if (FormUtils.nullIfEmpty(request.getParameter("bookmarkAppcode")) != null) {
            bookmarkAppcode = request.getParameter("bookmarkAppcode");
        }

        request.setAttribute("onlyFeaturesInGeom", onlyFeaturesInGeom);
        request.setAttribute("bookmarkAppcode", bookmarkAppcode);
        
        setCMSTheme(request);

        return mapping.findForward(ADMINDATAFW);
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * aanvullende info.
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema_items regels
     */
    public ActionForward aanvullendeinfo(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(mapping, dynaForm, request);

        if (gb == null) {
            return mapping.findForward("aanvullendeinfo");
        }

        String appCode = (String) request.getParameter("appCode"); //TODO CvL
        
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
            request.setAttribute("regels", getThemaObjectsWithId(gb, thema_items, request, appCode));
        }
        
        setCMSTheme(request);
        
        return mapping.findForward("aanvullendeinfo");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * metadata.
     *
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
     *
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
    public ActionForward objectdata(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {        
        
        String appCode = request.getParameter("bookmarkAppcode");        
        request.setAttribute("appCode", appCode);     
        
        List<Themas> locatieThemas = new ArrayList();
        List<Themas> themas = getThemas(mapping, dynaForm, request);
        if (themas != null) {
            for (Themas t : themas) {
                if (t.isLocatie_thema()) {
                    locatieThemas.add(t);
                }
            }
        }

        List<RecordChildBean> ggbBeans = new ArrayList();
        if (locatieThemas.size() > 0) {
            ggbBeans = CollectAdmindata.collectGegevensbronRecordChilds(request, locatieThemas, false);
        }
        
        request.setAttribute("ggbBeans", ggbBeans);           

        setCMSTheme(request);
        
        return mapping.findForward("objectdata");
    }
    
    private void setCMSTheme(HttpServletRequest request) {
        String param = request.getParameter("cmsPageId");
        Integer cmsPageId = null;
        if (param != null && !param.isEmpty()) {
            cmsPageId = new Integer(param);
        }
        
        /* CMS Theme klaarzetten */
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        
        CMSPagina cmsPage = null;        
        if (cmsPageId != null && cmsPageId > 0) {
            cmsPage = (CMSPagina) sess.get(CMSPagina.class, cmsPageId);
        }
        
        if (cmsPage != null && cmsPage.getThema() != null
                && !cmsPage.getThema().equals("")) {
            request.setAttribute("theme", cmsPage.getThema());
        }
    }

    /**
     * Onderstaande code is deprecated en wordt alleen nog gebruikt voor de oude
     * versie van bovenstaande methodes. dit moet allemaal via CollectAdmindata
     * gaan lopen
     *
     * @deprecated
     */
    private void collectThemaRegels(ActionMapping mapping, HttpServletRequest request, List themas, List regels, List ti, boolean locatie, String appCode) {

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
                        l = getThemaObjectsWithGeom(t, thema_items, request, appCode);
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
     *
     * @param t
     * @param thema_items
     * @param request
     * @return
     * @throws Exception
     * @deprecated
     */
    private List<AdminDataRowBean> getThemaObjectsWithGeom(Themas t, List<ThemaData> thema_items, HttpServletRequest request, String appCode) throws Exception {
        if (t == null) {
            return null;
        }
        if (thema_items == null || thema_items.isEmpty()) {
            //throw new Exception("Er is geen themadata geconfigureerd voor thema: " + t.getNaam() + " met id: " + t.getId());
            return null;
        }
        Geometry geom = CollectAdmindata.getGeometry(request);
        Filter extraFilter = CollectAdmindata.getExtraFilter(t, request);

        // filter op locatie thema's

        Gegevensbron gb = t.getGegevensbron();
        Bron b = gb.getBron(request);

        List<String> propnames = DataStoreUtil.basisRegelThemaData2PropertyNames(thema_items);
        Integer maximum = ConfigKeeper.getMaxNumberOfFeatures(appCode);
        List<Feature> features = DataStoreUtil.getFeatures(b, gb, geom, extraFilter, propnames, maximum, true);
        List<AdminDataRowBean> regels = new ArrayList();
        for (int i = 0; i < features.size(); i++) {
            Feature f = (Feature) features.get(i);
            regels.add(getRegel(f, gb, thema_items));
        }
        return regels;
    }

    /**
     *
     * @param gb
     * @param thema_items
     * @param request
     * @return
     * @throws Exception
     * @deprecated
     */
    protected List getThemaObjectsWithId(Gegevensbron gb, List thema_items, HttpServletRequest request, String appCode) throws Exception {
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
        Integer maximum = ConfigKeeper.getMaxNumberOfFeatures(appCode);
        List<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propnames, maximum, true);
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

    /**
     *
     * @param f
     * @param gb
     * @param thema_items
     * @return
     * @throws SQLException
     * @throws UnsupportedEncodingException
     * @throws Exception
     * @deprecated
     */
    protected AdminDataRowBean getRegel(Feature f, Gegevensbron gb, List<ThemaData> thema_items) throws SQLException, UnsupportedEncodingException, Exception {
        AdminDataRowBean regel = new AdminDataRowBean();

        String adminPk = DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart();
        if (adminPk != null) {
            regel.setPrimaryKey(f.getProperty(adminPk).getValue());
        }
        Iterator it = thema_items.iterator();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            /*
             * Controleer of de kolomnaam van dit themadata object wel voorkomt in de feature.
             * zoniet kan het zijn dat er een prefix ns in staat. Die moet er dan van afgehaald worden.
             * Als het dan nog steeds niet bestaat: een lege toevoegen.
             */
            String kolomnaam = td.getKolomnaam();
            if (kolomnaam != null && kolomnaam.length() > 0) {
                kolomnaam = DataStoreUtil.convertFullnameToQName(td.getKolomnaam()).getLocalPart();
            }
            /*
             * Controleer om welk datatype dit themadata object om draait.
             * Binnen het Datatype zijn er drie mogelijkheden, namelijk echt data,
             * een URL of een Query.
             * In alle drie de gevallen moeten er verschillende handelingen verricht
             * worden om deze informatie op het scherm te krijgen.
             *
             * In het eerste geval, wanneer het gaat om data, betreft dit de kolomnaam.
             * Als deze kolomnaam ingevuld staat hoeft deze alleen opgehaald te worden
             * en aan de arraylist regel toegevoegd te worden.
             */
            if (td.getDataType().getId() == DataTypen.DATA && kolomnaam != null) {
                if (f.getProperty(kolomnaam).getValue() == null) {
                    regel.addValue(null);
                } else {
                    regel.addValue(f.getProperty(kolomnaam).getValue().toString());
                }
                /*
                 * In het tweede geval dient de informatie in de thema data als link naar een andere
                 * informatiebron. Deze link zal enigszins aangepast moeten worden om tot vollende
                 * werkende link te dienen.
                 */
            } else if (td.getDataType().getId() == DataTypen.URL) {
                StringBuffer url;
                if (td.getCommando() != null) {
                    url = new StringBuffer(td.getCommando());
                } else {
                    url = new StringBuffer();
                }

                /* BOY: Welk id moet hier geappend worden ? */
                url.append(Themas.THEMAID);
                url.append("=");
                url.append(gb.getId());

                Object value = null;
                if (adminPk != null) {
                    value = f.getProperty(adminPk).getValue();
                    if (value != null) {
                        url.append("&");
                        url.append(adminPk);
                        url.append("=");
                        url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                    }
                }

                if (kolomnaam != null && kolomnaam.length() > 0 && !kolomnaam.equalsIgnoreCase(adminPk)) {
                    value = f.getProperty(kolomnaam).getValue();
                    if (value != null) {
                        url.append("&");
                        url.append(kolomnaam);
                        url.append("=");
                        url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                    }
                }

                regel.addValue(url.toString());

                /*
                 * De laatste mogelijkheid betreft een query. Vanuit de themadata wordt nu een
                 * een commando url opgehaald en deze wordt met de kolomnaam aangevuld.
                 */
            } else if (td.getDataType().getId() == DataTypen.QUERY) {
                StringBuffer url;
                if (td.getCommando() != null) {
                    url = new StringBuffer(td.getCommando());
                    String commando = url.toString();
                    //Kijk of er in de waarde van de kolomnaam een komma zit. Zoja, splits het dan op.
                    Object valueToSplit = null;
                    if (kolomnaam != null && f.getProperty(kolomnaam) != null) {
                        valueToSplit = f.getProperty(kolomnaam).getValue();
                    }
                    HashMap fhm = toHashMap(f);
                    List values = splitObject(valueToSplit, ",");
                    List regelValues = new ArrayList();
                    for (int i = 0; i < values.size(); i++) {
                        Object value = values.get(i);
                        if (commando.contains("[") || commando.contains("]")) {
                            //vervang de eventuele csv in 1 waarde van die csv
                            if (kolomnaam != null) {
                                fhm.put(kolomnaam, value);
                            }
                            String newCommando = replaceValuesInString(commando, fhm);
                            regelValues.add(newCommando);
                        } else {
                            if (value != null) {
                                url.append(value.toString().trim());
                                regelValues.add(url.toString());
                            } else {
                                regelValues.add("");
                            }
                        }
                    }
                    regel.addValue(regelValues);
                } else {
                    if (f.getProperty(kolomnaam).getValue() == null) {
                        regel.addValue(null);
                    } else {
                        regel.addValue(f.getProperty(kolomnaam).getValue().toString());
                    }
                }
            } else if (td.getDataType().getId() == DataTypen.FUNCTION) {
                Object keyValue = null;
                if (adminPk != null) {
                    keyValue = f.getProperty(adminPk).getValue();
                }
                if (keyValue != null) {
                    String attributeName = kolomnaam;
                    Object attributeValue = null;
                    if (attributeName != null) {
                        attributeValue = f.getProperty(attributeName).getValue();
                    } else {
                        attributeName = adminPk;
                        attributeValue = keyValue;
                    }

                    // De attributeValue ook eerst vooraan erbij zetten om die te kunnen tonen op de admindata pagina - Drie hekjes als scheidingsteken
                    StringBuilder function = new StringBuilder("");
                    function.append(attributeValue);
                    function.append("###").append(td.getCommando());
                    function.append("(this, ");
                    function.append("'").append(td.getGegevensbron().getId()).append("'");
                    function.append(",");
                    function.append("'").append(adminPk).append("'");
                    function.append(",");
                    function.append("'").append(keyValue).append("'");
                    function.append(",");
                    function.append("'").append(attributeName).append("'");
                    function.append(",");
                    function.append("'").append(attributeValue).append("'");
                    function.append(",");
                    function.append("'").append(td.getEenheid()).append("'");
                    function.append(")");
                    regel.addValue(function.toString());
                } else {
                    regel.addValue("");
                }
            } else /*
             * Indien een datatype aan geen van de voorwaarden voldoet wordt er een
             * lege regel aan de regel arraylist toegevoegd.
             */ {
                regel.addValue("");
            }
        }
        return regel;
    }

    /**
     * DOCUMENT ME!!!
     *
     * @param params Map
     * @param key String
     *
     * @return String
     * @deprecated
     */
    protected String getStringFromParam(Map params, String key) {
        Object ob = params.get(key);
        String string = null;
        if (ob instanceof String) {
            string = (String) ob;
        }
        if (ob instanceof String[]) {
            string = ((String[]) ob)[0];
        }
        return string;
    }

    /**
     * Compare 2 thema datalists voor het tonen in de admindata. (dus niet
     * volledige vergelijking maar alleen op label en basisregel)
     *
     * @deprecated
     */
    protected boolean compareThemaDataLists(List list1, List list2) {
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        int basisRegelTeller1 = 0;
        int basisRegelTeller2 = 0;
        for (int i1 = 0; i1 < list1.size(); i1++) {
            ThemaData td1 = (ThemaData) list1.get(i1);
            if (td1.isBasisregel()) {
                basisRegelTeller1++;
            }
            if (td1.isBasisregel() && td1.getLabel() != null) {
                boolean bevatGelijke = false;
                for (int i2 = 0; i2 < list2.size(); i2++) {
                    ThemaData td2 = (ThemaData) list2.get(i2);
                    if (td2.isBasisregel()) {
                        basisRegelTeller2++;
                    }
                    if (td2.isBasisregel() && td1.getLabel().equalsIgnoreCase(td2.getLabel())) {
                        bevatGelijke = true;
                        break;
                    }
                }
                if (!bevatGelijke) {
                    return false;
                }
            }
        }
        if (basisRegelTeller1 == basisRegelTeller2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param f
     * @return
     * @throws Exception
     * @deprecated
     */
    private HashMap toHashMap(Feature f) throws Exception {
        HashMap result = new HashMap();
        FeatureType ft = f.getType();
        Iterator it = ft.getDescriptors().iterator();
        while (it.hasNext()) {
            PropertyDescriptor pd = (PropertyDescriptor) it.next();
            String key = pd.getName().getLocalPart();
            Object value = f.getProperty(pd.getName()).getValue();
            result.put(key, value);
        }
        return result;
    }

    /**
     * Alle [kolomnamen] in de url worden vervangen door de waarde in de kolom.
     * Bijvoorbeeld:
     * http://plannen.kaartenbalie.nl/[planeigenaar]/[plannaam]/[planidentificyaty].html
     * Kan dan worden: http://plannen.kaartenbalie.nl/gemeente/plansoen/p38.html
     *
     * @deprecated
     */
    private String replaceValuesInString(String string, HashMap values) throws Exception {
        if (!string.contains("[") && !string.contains("]")) {
            return string;
        }
        StringBuffer url;
        if (string != null) {
            url = new StringBuffer(string);
        } else {
            url = new StringBuffer();
        }

        int begin = -1;
        int eind = -1;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '[') {
                if (begin == -1) {
                    begin = i;
                } else {
                    logger.error("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
                    throw new Exception("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
                }
            } else if (c == ']') {
                eind = i;
                if (begin != -1 && eind != -1) {
                    String kolomnaam = url.substring(begin + 1, eind);
                    if (kolomnaam == null || kolomnaam.length() == 0) {
                        logger.error("Commando \"" + string + "\" is niet correct. Geen kolomnaam aanwezig tussen [ en ].");
                        throw new Exception("Commando \"" + string + "\" is niet correct. Geen kolomnaam aanwezig tussen [ en ].");
                    }
                    Object value = values.get(kolomnaam);
                    if (value == null) {
                        value = "";
                    }
                    url.replace(begin, eind + 1, value.toString().trim());
                    begin = -1;
                    eind = -1;
                    i = 0;
                } else {
                    logger.error("Commando \"" + string + "\" is niet correct. Er ontbreekt een [ .");
                    throw new Exception("Commando \"" + string + "\" is niet correct. Er ontbreekt een [ .");
                }
            } else if (i == url.length() - 1 && begin != -1) {
                logger.error("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
                throw new Exception("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
            }
        }
        return url.toString();
    }

    /**
     *
     * @param value
     * @param seperator
     * @return
     * @deprecated
     */
    private List splitObject(Object value, String seperator) {
        ArrayList values = new ArrayList();
        if (value == null) {
            values.add(value);
        } else if (value instanceof String) {
            String[] tokens = ((String) value).split(seperator);
            values.addAll(Arrays.asList(tokens));
        } else {
            values.add(value);
        }
        return values;
    }
}
