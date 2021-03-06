package nl.b3p.gis.viewer.admindata;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.ViewerAction;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.wms.capabilities.ServiceProvider;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 *
 * @author Chris en Boy
 */
public class CollectAdmindata {

    private static final Log logger = LogFactory.getLog(CollectAdmindata.class);
    private static final FilterFactory2 filterFac = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
    protected static final double DEFAULTTOLERANCE = 5.0;
    public static final String SEARCH = "search";
    public static final String SEARCHID = "searchId";
    public static final String SEARCHCLUSTERID = "searchClusterId";
    public static final String DEFAULT_LAYOUT = "admindata";
    private static DecimalFormat threeDBFormat = new DecimalFormat("#.##");


    public GegevensBronBean fillGegevensBronBeanFromPoint( int themaId,
            String wkt, String jsonCQL, boolean collectGeom, String appCode, String scale, String tolerance) {
        
        double distance = getDistance(scale, tolerance);
        Geometry g = getGeometry(distance,wkt,null, "0");
        return fillGegevensBronBean(-1, themaId, g.toText(), jsonCQL, collectGeom, "-1", appCode);
    }

    /*je wilt juist wel die geom op kunnen halen met een AJAX verzoek!
     * en aangezien je voor DWR ajax unieke functie namen moet hebben deze verwijderd:
     */
    /*public GegevensBronBean fillGegevensBronBean(int gegevensBronId, int themaId, String wkt, String cql, String parentHtmlId) throws Exception {
     boolean collectGeom = false;
     return fillGegevensBronBean(gegevensBronId, themaId, wkt, cql, collectGeom, parentHtmlId);
     }*/
    public GegevensBronBean fillGegevensBronBean(int gegevensBronId, int themaId,
            String wkt, String jsonCQL, boolean collectGeom, String parentHtmlId, String appCode) {
        GegevensBronBean bean = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;

        try {
            JSONObject cqlFilters = new JSONObject(jsonCQL);
            tx = sess.beginTransaction();

            Gegevensbron gb = retrieveGegevensbron(gegevensBronId, themaId, sess);
            if (gb != null) {
                /* addChilds */
                List childBronnen = sess.createQuery("from Gegevensbron where parent = :parentId order by volgordenr, naam").setInteger("parentId", gb.getId()).list();

                bean = new GegevensBronBean();

                bean.setId(gb.getId());
                bean.setAdminPk(gb.getAdmin_pk());
                bean.setParentHtmlId(parentHtmlId);
                bean.setEditable(gb.isEditable());

                Themas thema = null;
                if (themaId > 0) {
                    thema = (Themas) sess.get(Themas.class, themaId);
                }

                if (thema == null || thema.getNaam() == null || thema.getNaam().equals("")) {
                    bean.setTitle(gb.getNaam());
                } else {
                    bean.setTitle(thema.getNaam());
                }
                if (thema != null) {
                    bean.setOrder(thema.getBelangnr());
                }

                WebContext ctx = WebContextFactory.get();
                HttpServletRequest request = null;

                if (ctx != null) {
                    request = ctx.getHttpServletRequest();
                }

                GisPrincipal user = null;

                if (request != null) {
                    user = GisPrincipal.getGisPrincipal(request);
                }

                String layout = null;
                layout = findDataAdminLayout(thema, user, appCode);

                if (layout == null) {
                    layout = DEFAULT_LAYOUT;
                }

                bean.setLayout(layout);

                // Per ThemaData een LabelBean toevoegen
                List objectdata_items = SpatialUtil.getThemaData(gb, true);
                Iterator iter = objectdata_items.iterator();
                while (iter.hasNext()) {
                    ThemaData td = (ThemaData) iter.next();

                    if (!td.isBasisregel()) {
                        continue;
                    }

                    LabelBean lb = new LabelBean();

                    if (td.getId() != null) {
                        lb.setId(td.getId());
                    }

                    lb.setLabel(td.getLabel());
                    lb.setKolomBreedte(td.getKolombreedte());
                    lb.setKolomNaam(td.getKolomnaam());
                    lb.setCommando(td.getCommando());

                    int typeId = td.getDataType().getId();
                    lb.setType(RecordValueBean.getStringType(typeId));

                    lb.setEenheid(td.getEenheid());

                    bean.addLabel(lb);
                }

                Bron b = gb.getBron(request);

                if (b != null) {
                    Geometry geom = null;
                    if (wkt != null) {
                        geom = DataStoreUtil.createGeomFromWKTString(wkt);
                    }

                    List<String> propnames = bean.getKolomNamenList();
                    Filter parentCqlFilter = null;
                    //haal het cql filter op voor de (parent)gegevens bron.
                    String cql = null;
                    if (cqlFilters.has("" + gb.getId())) {
                        cql = cqlFilters.getString("" + gb.getId());
                    }
                    if (cql != null && cql.length() > 0) {
                        parentCqlFilter = CQL.toFilter(cql);
                    }
                    
                    Integer maximum = ConfigKeeper.getMaxNumberOfFeatures(appCode);
                    List<Feature> features = null;
                    features = DataStoreUtil.getFeatures(b, gb, geom, parentCqlFilter, propnames, maximum, collectGeom);

                    /*
                     * TODO: Tijdelijke fix voor ophalen features cyclomedia wfs via
                     * geotools. De intersects filter niet te werken maar het bbox
                     * filter wel. Op r. 408 van de DataStoreUtil.createIntersectFilter
                     * zegt de wfs wel dat hij de intersects fully support maar er komen
                     * geen features terug met dat filter.
                     */
//                    if (features == null || features.size() < 1) {
//                        if (gb.getBron().getUrl().contains("wfs")) {
//                            features = DataStoreUtil.getWfsFeaturesWithGeotools(gb, geom);
//                        }
//                    }

                    //featuretype waarmee gekeken kan worden of er een geometry in de feature zit.
                    DataStore tempDatastore = b.toDatastore();
                    SimpleFeatureType featureType = null;
                    try {
                        featureType = DataStoreUtil.getSchema(tempDatastore, gb);
                    } finally {
                        tempDatastore.dispose();
                    }
                    if (features != null && !features.isEmpty()) {
                        Iterator featureIter = features.iterator();
                        while (featureIter.hasNext()) {
                            Feature f = (Feature) featureIter.next();

                            RecordBean record = null;
                            record = getRecordBean(f, gb, bean.getLabels(), wkt);

                            if (record == null) {
                                continue;
                            }

                            /* Controleer of de feature een geometry property heeft.
                             * Als er geen geometry is opgehaald (collectGeom==false)
                             * kijk dan of het feature type wel een geometryDescriptor heeft.
                             */
                            if (collectGeom) {
                                GeometryAttribute attrib = f.getDefaultGeometryProperty();
                                if (attrib != null) {
                                    record.setShowMagicWand(true);
                                } else if (attrib == null) {
                                    record.setShowMagicWand(false);
                                }
                            } else {
                                if (featureType != null && featureType.getGeometryDescriptor() != null) {
                                    record.setShowMagicWand(true);
                                } else {
                                    record.setShowMagicWand(false);
                                }
                            }

                            Iterator iter4 = childBronnen.iterator();

                            while (iter4.hasNext()) {
                                Gegevensbron child = (Gegevensbron) iter4.next();

                                String fkField = child.getAdmin_fk();
                                if(record.getId() == null){
                                    continue;
                                }
                                String recordId = record.getId().toString();

                                Filter childFilter = null;
                                ArrayList<Filter> filters = new ArrayList();
                                //Haal het stukje CQL op voor de child gegevensbron.
                                String childCql = null;
                                if (cqlFilters.has("" + child.getId())) {
                                    childCql = cqlFilters.getString("" + child.getId());
                                }
                                if (childCql != null && childCql.length() > 0) {
                                    Filter childCqlFilter = CQL.toFilter(childCql);
                                    filters.add(childCqlFilter);
                                }
                                Filter attrFilter = null;
                                if (fkField != null && recordId != null) {
                                    attrFilter = FilterBuilder.createEqualsFilter(fkField, recordId);
                                }

                                if (attrFilter != null) {
                                    filters.add(attrFilter);
                                }

                                if (filters.size() == 1) {
                                    childFilter = filters.get(0);
                                }

                                if (filters.size() > 1) {
                                    childFilter = filterFac.and(filters);
                                }

                                SimpleFeatureImpl feature = (SimpleFeatureImpl) f;
                                int count = 0;
                                Geometry featureGeom = null;
                                //geen count doen, is erg traag.
                                /*if (feature.getDefaultGeometry()!=null)
                                 featureGeom=(Geometry) feature.getDefaultGeometry();*/

                                //count = getAantalChildRecords(child, childFilter, featureGeom);
                                //altijd childs tonen.
                                count = 1;
                                JSONObject childCQL = new JSONObject(cqlFilters.toString());
                                //voeg het child filter toe aan het json object zodat het de volgende keer gebruikt kan worden.
                                if (childCQL != null && childFilter != null) {
                                    childCQL.put("" + child.getId(), CQL.toCQL(childFilter));
                                }

                                if (count > 0) {
                                    RecordChildBean childBean = new RecordChildBean();
                                    childBean.setId(child.getId().toString());
                                    childBean.setGegevensBronBeanId(bean.getId());
                                    childBean.setTitle(child.getNaam());
                                    childBean.setAantalRecords(count);
                                    childBean.setThemaId(new Integer(themaId).toString());
                                    //childBean.setCql(CQL.toCQL(attrFilter));
                                    childBean.setCql(childCQL.toString());
                                    childBean.setWkt(wkt);

                                    record.addChild(childBean);
                                }
                            }

                            bean.addRecord(record);
                        }

                        if (bean != null) {
                            bean.setCsvPksFromRecordBeans();
                        }
                    }
                }
            }

            tx.commit();

        } catch (CQLException cqlEx) {
            logger.error("Fout tijdens filter: ", cqlEx);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }

        } catch (NoSuchElementException nse) {
            logger.error("Verkeerd element in resultaat objectdata: ", nse);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception e) {
            logger.error("Fout tijdens ophalen objectdata: ", e);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }

        return bean;
    }

    protected RecordBean getRecordBean(Feature f, Gegevensbron gb, List<LabelBean> label_bean_items,
            String wkt) throws SQLException, UnsupportedEncodingException, Exception {

        RecordBean rb = new RecordBean();
        String adminPk = null;

        if (gb.getAdmin_pk() != null) {
            QName qName = DataStoreUtil.convertColumnNameToQName(gb.getAdmin_pk());

            if (qName != null) {
                adminPk = qName.getLocalPart();
            }
        }

        if (adminPk != null) {
            rb.setId(f.getProperty(adminPk).getValue());
        }

        if (label_bean_items == null) {
            return null;
        }

        String ggbId = Integer.toString(gb.getId());

        Iterator it = label_bean_items.iterator();
        while (it.hasNext()) {
            LabelBean lb = (LabelBean) it.next();

            RecordValueBean rvb = new RecordValueBean();

            rvb.setType(lb.getType());
            rvb.setEenheid(lb.getEenheid());
            rvb.setKolomBreedte(lb.getKolomBreedte());

            String commando = lb.getCommando();
            String eenheid = lb.getEenheid();

            String kolomnaam = lb.getKolomNaam();
            if (kolomnaam != null && kolomnaam.length() > 0) {
                kolomnaam = DataStoreUtil.convertColumnNameToQName(lb.getKolomNaam()).getLocalPart();
            }

            List attributeValueList = null;
            Object attributeValue = null;
            if (kolomnaam != null && f.getProperty(kolomnaam) != null) {
                attributeValue = f.getProperty(kolomnaam).getValue();
                //Kijk of er in de waarde van de kolomnaam een komma zit. Zoja, splits het dan op.
                if (commando != null && (commando.contains(kolomnaam) || lb.getType().equals(RecordValueBean.TYPE_URL))) {
                    attributeValueList = splitObject(attributeValue, ",");
                }
            }
            Object pkValue = null;
            if (adminPk != null) {
                pkValue = f.getProperty(adminPk).getValue();
            }

            /*
             * Controleer om welk datatype dit themadata object om draait.
             * Binnen het Datatype zijn er vier mogelijkheden, namelijk echt data,
             * een URL of een Query of een javascript function.
             * In alle vier de gevallen moeten er verschillende handelingen verricht
             * worden om deze informatie op het scherm te krijgen.
             * In alle gevallen wordt een enkele waarde berekend en een lijstwaarde
             * voor het geval er komma's in de waarde zitten.
             */
            List resultList = new ArrayList();
            String resultValue = null;
            if (lb.getType().equals(RecordValueBean.TYPE_GOOGLENAV)) {
                resultValue = "TYPE_GOOGLENAV";

                if (attributeValueList != null && attributeValueList.size() > 1) {
                    for (int i = 0; i < attributeValueList.size(); i++) {
                        Object localValue = attributeValueList.get(i);
                        String lData = "TYPE_GOOGLENAV";
                        resultList.add(lData);
                    }
                }
            } else if (lb.getType().equals(RecordValueBean.TYPE_DATA)) {
                resultValue = createData(attributeValue);

                if (attributeValueList != null && attributeValueList.size() > 1) {
                    for (int i = 0; i < attributeValueList.size(); i++) {
                        Object localValue = attributeValueList.get(i);
                        String lData = createData(localValue);
                        resultList.add(lData);
                    }
                }
            } else if (lb.getType().equals(RecordValueBean.TYPE_DATUM)) {
                resultValue = createDatum(attributeValue);

                if (attributeValueList != null && attributeValueList.size() > 1) {
                    for (int i = 0; i < attributeValueList.size(); i++) {
                        Object localValue = attributeValueList.get(i);
                        String lData = createDatum(localValue);
                        resultList.add(lData);
                    }
                }
            } else if (lb.getType().equals(RecordValueBean.TYPE_URL)) {
                resultValue = createUrl(kolomnaam, attributeValue, adminPk, pkValue,
                        /* TODO: BOY: Welk id moet hier geappend worden ? */ Themas.THEMAID, ggbId, commando);

                if (attributeValueList != null && attributeValueList.size() > 1) {
                    for (int i = 0; i < attributeValueList.size(); i++) {
                        Object localValue = attributeValueList.get(i);
                        String lUrl = createUrl(kolomnaam, localValue, adminPk, pkValue, Themas.THEMAID, ggbId, commando);
                        resultList.add(lUrl);
                    }
                }
            } else if (lb.getType().equals(RecordValueBean.TYPE_QUERY)) {
                HashMap fhm = toHashMap(f);
                if (attributeValue != null) {
                    resultValue = attributeValue.toString();
                }
                //resultValue = createQuery(kolomnaam, attributeValue, commando, fhm);

                if (attributeValueList != null && attributeValueList.size() > 1) {
                    for (int i = 0; i < attributeValueList.size(); i++) {
                        Object localValue = attributeValueList.get(i);
                        String lQuery = createQuery(kolomnaam, localValue, commando, fhm);
                        resultList.add(lQuery);
                    }
                } else {
                    // dit is nodig omdat kolomnaam leeg kan zijn, waarbij attribuutwaarden
                    // via [] vervangingen worden ingevuld.
                    String lQuery = createQuery(kolomnaam, null, commando, fhm);
                    resultList.add(lQuery);
                }

            } else if (lb.getType().equals(RecordValueBean.TYPE_FUNCTION)) {

                /* Indien commando surfaceArea dan verschil geom en getekende polygon berekenen */
                if (lb.getCommando() != null && lb.getCommando().contains(RecordValueBean.JAVASCRIPT_COMMAND_SURFACE_AREA)) {
                    if (kolomnaam != null && f.getProperty(kolomnaam) != null) {
                        rvb.setType(RecordValueBean.TYPE_DATA);

                        Geometry recordGeom = (Geometry) f.getProperty(kolomnaam).getValue();
                        String result = calcSurfaceAreaDifference(recordGeom, wkt);

                        if (result != null) {
                            resultValue = result;
                        } else {
                            resultValue = "-";
                        }
                    }
                } else {
                    resultValue = createFunction(kolomnaam, attributeValue, adminPk, pkValue, ggbId, commando, eenheid);

                    if (attributeValueList != null && attributeValueList.size() > 1) {
                        for (int i = 0; i < attributeValueList.size(); i++) {
                            Object localValue = attributeValueList.get(i);
                            String lFunction = createFunction(kolomnaam, localValue, adminPk, pkValue, ggbId, commando, eenheid);
                            resultList.add(lFunction);
                        }
                    }
                }
            }

            rvb.setValue(resultValue);
            rvb.setValueList(resultList);

            rb.addValue(rvb);
        }

        return rb;
    }

    private String calcSurfaceAreaDifference(Geometry geom, String wkt) {
        String result = null;

        try {
            Geometry mapPoly = DataStoreUtil.createGeomFromWKTString(wkt);
            Geometry intersection = mapPoly.intersection(geom);

            intersection.setSRID(28992);
            if (intersection != null) {
                if (intersection instanceof LineString) {
                    Double len = intersection.getLength();

                    if (len >= 1000) {
                        len /= 1000;
                        result = threeDBFormat.format(len) + " km";
                    } else {
                        result = threeDBFormat.format(len) + " m";
                    }

                } else {
                    Double area = intersection.getArea();

                    if (area >= 1000000) {
                        area /= 1000000;
                        result = threeDBFormat.format(area) + " km2";
                    } else {
                        result = threeDBFormat.format(area) + " m2";
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Fout tijden berekenen oppervlakte verschil:", ex);
        }

        return result;
    }

    /**
     * In het eerste geval, wanneer het gaat om data, betreft dit de kolomnaam.
     * Als deze kolomnaam ingevuld staat hoeft deze alleen opgehaald te worden
     * en aan de arraylist regel toegevoegd te worden.
     *
     */
    private String createData(Object attributeValue) {
        if (attributeValue == null) {
            return null;
        }
        return attributeValue.toString();
    }
    
    private String createDatum(Object attributeValue) {
        if (attributeValue == null) {
            return null;
        }
        if(attributeValue instanceof Date){
            Date datum = (Date)attributeValue;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY");
            String timestamp = dateFormat.format(datum);
            return timestamp;
        }else{
            return attributeValue.toString();
        }
    }

    /**
     * In het tweede geval dient de informatie in de thema data als link naar
     * een andere informatiebron. Deze link zal enigszins aangepast moeten
     * worden om tot werkende link te komen.
     */
    private String createUrl(String attributeName, Object attributeValue, String adminPk, Object pkValue, String ggbIdName, String ggbId, String commando) {
        StringBuffer url;
        if (commando != null) {
            url = new StringBuffer(commando);
        } else {
            url = new StringBuffer();
        }

        url.append(ggbIdName);
        url.append("=");
        url.append(ggbId);

        if (adminPk != null && pkValue != null) {
            url.append("&");
            url.append(adminPk);
            url.append("=");
            try {
                url.append(URLEncoder.encode(pkValue.toString().trim(), "utf-8"));
            } catch (UnsupportedEncodingException ex) {
                logger.error("", ex);
            }
        }

        if (attributeValue != null && attributeName != null && !attributeName.equalsIgnoreCase(adminPk)) {
            url.append("&");
            url.append(attributeName);
            url.append("=");
            try {
                url.append(URLEncoder.encode(attributeValue.toString().trim(), "utf-8"));
            } catch (UnsupportedEncodingException ex) {
                logger.error("", ex);
            }
        }

        return url.toString();

    }

    /**
     * De laatste mogelijkheid betreft een query. Vanuit de themadata wordt nu
     * een een commando url opgehaald en deze wordt met de kolomnaam aangevuld.
     */
    private String createQuery(String attributeName, Object attributeValue, String commando, HashMap fhm) {
        if (commando == null) {
            return null;
        }

        if (commando.contains("[") && commando.contains("]")) {
            //vervang de eventuele csv in 1 waarde van die csv
            if (attributeName != null && attributeValue != null) {
                fhm.put(attributeName, attributeValue);
            }
            String newCommando = null;
            try {
                newCommando = replaceValuesInString(commando, fhm);
            } catch (Exception ex) {
                logger.error("", ex);
            }
            return newCommando;
        }

        if (StringUtils.containsIgnoreCase(commando, "ReportServlet")) {
            return commando;
        }

        if (attributeValue != null) {
            commando += attributeValue.toString().trim();
            return commando;
        }

        return null;

    }

    private String createFunction(String attributeName, Object attributeValue, String adminPk, Object pkValue, String ggbId, String commando, String eenheid) {
        if (pkValue == null && attributeValue == null) {
            return null;
        }
        if (pkValue == null) {
            return attributeValue.toString();
        }

        // De attributeValue ook eerst vooraan erbij zetten om die te kunnen tonen op de admindata pagina - Drie hekjes als scheidingsteken
        StringBuilder function = new StringBuilder("");
        function.append(attributeValue);
        function.append("###").append(commando);
        function.append("(this, ");
        function.append("'").append(ggbId).append("'");
        function.append(",");
        function.append("'").append(adminPk).append("'");
        function.append(",");
        function.append("'").append(pkValue).append("'");
        function.append(",");
        function.append("'").append(attributeName).append("'");
        function.append(",");
        function.append("'").append(attributeValue).append("'");
        function.append(",");
        function.append("'").append(eenheid).append("'");
        function.append(")");

        return function.toString();
    }

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
     * Maak een count op de kinderen. Als er een geometry wordt meegegeven wordt
     * er ook gekeken of de kinderen in de geometry liggen (vaak van de parent).
     * Als je ze niet mee geeft dan worden de kinderen bepaalt aan de hand van
     * de foreign key.
     */
    protected int getAantalChildRecords(Gegevensbron childGb, Filter filter, Geometry geom) throws Exception {
        int count = -1;

        if (childGb == null) {
            return count;
        }

        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = null;

        if (ctx != null) {
            request = ctx.getHttpServletRequest();
        }

        Bron b = childGb.getBron(request);

        if (b == null) {
            return count;
        }

        /* Ophalen count van een RO Online WFS duurt best lang */
        if (b.getType().equals(Bron.TYPE_WFS)) {
            return 1;
        }

        DataStore ds = null;
        try {
            ds = b.toDatastore();

            Filter childGeomFilter = null;
            ArrayList<Filter> filters = new ArrayList();
            if (filter != null) {
                filters.add(filter);
            }
            /* geom filter toevoegen */
            Filter geomFilter = null;
            if (geom != null) {
                geomFilter = DataStoreUtil.createIntersectFilter(childGb, ds, geom);
            }
            if (geomFilter != null) {
                filters.add(geomFilter);
            }
            if (filters.size() == 1) {
                childGeomFilter = filters.get(0);
            }
            if (filters.size() > 1) {
                childGeomFilter = filterFac.and(filters);
            }

            List<String> propnames = new ArrayList<String>();

            if (childGb.getAdmin_fk() != null) {
                propnames.add(childGb.getAdmin_fk());
            }

            FeatureCollection fc = DataStoreUtil.getFeatureCollection(ds, childGb, childGeomFilter, propnames, null, false);
            count = fc.size();

        } catch (Exception ex) {
            logger.error("Fout tijdens maken DataStore voor child Gegevensbron", ex);
        } finally {
            ds.dispose();
        }

        return count;
    }
   
    private Gegevensbron retrieveGegevensbron(int gegevensbronId, int themaId, Session sess) {
        Gegevensbron gegevensbron = null;
        gegevensbron = (Gegevensbron) sess.get(Gegevensbron.class, gegevensbronId);
        if (gegevensbron == null && themaId > 0) {
            Themas t = (Themas) sess.get(Themas.class, themaId);
            if (t != null) {
                gegevensbron = t.getGegevensbron();
            }
        }
        return gegevensbron;
    }

    static private String findDataAdminLayout(Themas thema, GisPrincipal user, String appCode) throws Exception {
        /* Bepalen welke jsp (layout) voor admindata gebruikt moet worden
         * 1 = uitgebreide jsp
         * 2 = simpel naast elkaar
         * TODO: 3 = simpel onder elkaar
         * 4: multi_admin -> komt later in de plaats van uitgebreide jsp (1)
         */

        if (thema == null || user == null) {
            return null;
        }

        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = null;

        if (ctx != null) {
            request = ctx.getHttpServletRequest();
        }

        /* Applicatie instellingen ophalen */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(appCode, true);

        String layoutAdminData = (String) map.get("layoutAdminData");

        String themaLayout = thema.getLayoutadmindata();
        if ((themaLayout == null) || themaLayout.equals("")) {
            layoutAdminData = (String) map.get("layoutAdminData");
        } else {
            layoutAdminData = themaLayout;
        }

        return layoutAdminData;
    }

    static public List collectGegevensbronRecordChilds(HttpServletRequest request, List themas, boolean locatie) throws JSONException {
        Geometry geom = getGeometry(request);
        String wkt = null;
        if (geom != null) {
            wkt = geom.toText();
        }

        List beans = new ArrayList();

        /* Per thema een GegevensBronBean vullen */
        Iterator iter = themas.iterator();
        while (iter.hasNext()) {
            Themas thema = (Themas) iter.next();
            logger.debug("checking thema: " + thema == null ? "<null>" : thema.getNaam());
            Gegevensbron gb = thema.getGegevensbron();

            if (gb != null) {

                String gbId = thema.getGegevensbron().getId().toString();
                String themaId = thema.getId().toString();
                String themaNaam = thema.getNaam();

                /* Filter naar CQL */
                Filter filter = getExtraFilter(thema, request);
                String gbCQL = null;
                if (filter != null) {
                    gbCQL = CQL.toCQL(filter);
                }
                JSONObject cqlFilters = new JSONObject();
                //Haal het extra CQL filter op als die is meegegeven.
                if (FormUtils.nullIfEmpty(request.getParameter("extraCriteria")) != null) {
                    try {
                        cqlFilters = new JSONObject(request.getParameter("extraCriteria"));
                        String cql = null;
                        if (cqlFilters.has("" + gbId)) {
                            cql = cqlFilters.getString("" + gbId);
                        }
                        if (FormUtils.nullIfEmpty(cql) != null) {
                            if (gbCQL == null) {
                                gbCQL = "";
                            } else {
                                gbCQL += " && ";
                            }
                            gbCQL += cql;
                        }
                    } catch (JSONException je) {
                        cqlFilters = new JSONObject();
                        logger.error("Fout bij converteren ExtraCriteria(JSON) naar een JSONObject");
                    }
                }
                cqlFilters.put("" + gbId, gbCQL);

                RecordChildBean childBean = new RecordChildBean();
                childBean.setId(gbId);
                childBean.setGegevensBronBeanId(new Integer(0));
                childBean.setTitle(themaNaam);
                childBean.setAantalRecords(1);
                childBean.setThemaId(themaId);
                childBean.setCql(cqlFilters.toString());
                childBean.setWkt(wkt);

                beans.add(childBean);
            }
        }
        return beans;
    }

    static public Geometry getGeometry(HttpServletRequest request) {
        String geom = FormUtils.nullIfEmpty(request.getParameter("geom"));
        String withinObject = request.getParameter("withinObject");
        double distance = getDistance(request);
        double[] coords = getCoords(request);
        Geometry geometry = getGeometry(distance, geom, coords, withinObject);

        return geometry;
    }

    
    static public Geometry getGeometry(double distance, String geom, double[] coords, String withinObject) {
        Geometry geometry = null;
        if (geom != null) {
            geometry = SpatialUtil.geometrieFromText(geom, 28992);
        } else {
            GeometryFactory gf = new GeometryFactory();
            if (coords == null) {
                geometry = null;
            } else if (coords.length == 2) {
                geometry = gf.createPoint(new Coordinate(coords[0], coords[1]));
            } else if (coords.length == 10) {
                Coordinate[] coordinates = new Coordinate[5];
                for (int i = 0; i < coordinates.length; i++) {
                    coordinates[i] = new Coordinate(coords[i * 2], coords[i * 2 + 1]);
                }
                geometry = gf.createPolygon(gf.createLinearRing(coordinates), null);
            }
        }

        boolean selectWithinobject = false;

        if (withinObject != null && withinObject.equals("1")) {
            selectWithinobject = true;
        }

        /* Indien selecteren binnen kaartobject dan Line en Point
         * niet bufferen. Polygons een kleine negatieve buffer zodat
         * de objectdata voor omliggende objecten niet wordt opgehaald */
        if (geometry != null) {
            if (selectWithinobject) {
                if (geom.indexOf("POLY") != -1) {
                    geometry = geometry.buffer(-0.0001);
                }
            } else {
                geometry = geometry.buffer(distance);
            }
        }
        return geometry;
    }

    static private double[] getCoords(HttpServletRequest request) {
        double[] coords = null;
        if (request.getParameter("coords") != null && !request.getParameter("coords").equals("")) {
            String[] coordString = request.getParameter("coords").split(",");
            coords = new double[coordString.length];
            for (int i = 0; i < coordString.length; i++) {
                coords[i] = Double.parseDouble(coordString[i]);
            }
        }
        return coords;
    }

    static private double getDistance(HttpServletRequest request) {
        String s = request.getParameter("scale");
        String tolerance = request.getParameter("tolerance");
        return getDistance(s, tolerance);
    }

    static private double getDistance(String s,String tolerance) {
        double scale = 0.0;
        try {
            if (s != null) {
                scale = Double.parseDouble(s);
                //af ronden op 6 decimalen
                scale = Math.round((scale * 1000000));
                scale = scale / 1000000;
            }
        } catch (NumberFormatException nfe) {
            scale = 0.0;
            logger.debug("Scale is geen double dus wordt genegeerd");
        }
        double clickTolerance = DEFAULTTOLERANCE;
        try {
            if (tolerance != null) {
                clickTolerance = Double.parseDouble(tolerance);
            }
        } catch (NumberFormatException nfe) {
            clickTolerance = DEFAULTTOLERANCE;
            logger.debug("Tolerance is geen double dus de default wordt gebruikt: " + DEFAULTTOLERANCE + " pixels");
        }
        double distance = clickTolerance;
        if (scale > 0.0) {
            distance = scale * (clickTolerance);
        }
        return distance;
    }

    static public Filter getExtraFilter(Themas t, HttpServletRequest request) {
        ArrayList<Filter> extraFilters = new ArrayList<Filter>();
        //controleer of er een extra sld filter meegegeven is en of die op dit thema moet worden toegepast.
        Filter sldFilter = createSldFilter(t, request);
        if (sldFilter != null) {
            extraFilters.add(sldFilter);
        }
        //controleer of er een organization code is voor dit thema
        //TODO: Dit moet er echt uit. Dit doen zoals hierboven. In een CQL stoppen!
        String organizationcodekey = t.getOrganizationcodekey();
        String organizationcode = getOrganizationCode(request);
        if (FormUtils.nullIfEmpty(organizationcodekey) != null
                && FormUtils.nullIfEmpty(organizationcode) != null) {
            Filter organizationFilter = FilterBuilder.createEqualsFilter(organizationcodekey, organizationcode);
            extraFilters.add(organizationFilter);
        }
        if (extraFilters.size() == 0) {
            return null;
        }
        if (extraFilters.size() == 1) {
            return extraFilters.get(0);
        }
        //groter dan 1 dus 'and' doen:
        return FilterBuilder.getFactory().and(extraFilters);
    }

    static private Filter createSldFilter(Themas t, HttpServletRequest request) {
        if (doExtraSearchFilter(t, request)) {
            return FilterBuilder.createEqualsFilter(t.getSldattribuut(), request.getParameter(ViewerAction.SEARCH));
        }
        return null;
    }

    static private boolean doExtraSearchFilter(Themas t, HttpServletRequest request) {
        if (FormUtils.nullIfEmpty(t.getSldattribuut()) != null && FormUtils.nullIfEmpty(request.getParameter(ViewerAction.SEARCH)) != null) {
            String searchId = request.getParameter(ViewerAction.SEARCHID);
            String searchClusterId = request.getParameter(ViewerAction.SEARCHCLUSTERID);
            if (FormUtils.nullIfEmpty(searchId) != null) {
                String[] searchIds = searchId.split(",");
                for (int i = 0; i < searchIds.length; i++) {
                    try {
                        if (t.getId().intValue() == Integer.parseInt(searchIds[i])) {
                            return true;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                }
            }
            if (FormUtils.nullIfEmpty(searchClusterId) != null) {
                String[] clusterIds = searchClusterId.split(",");
                for (int i = 0; i < clusterIds.length; i++) {
                    try {
                        if (isInCluster(t.getCluster(), Integer.parseInt(clusterIds[i]))) {
                            return true;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                }
            }

        }
        return false;
    }

    /**
     * this cluster is or is in the cluster with id==clusterId
     */
    static private boolean isInCluster(Clusters themaCluster, int clusterId) {
        if (themaCluster == null) {
            return false;
        } else if (themaCluster.getId() == clusterId) {
            return true;
        } else {
            return isInCluster(themaCluster.getParent(), clusterId);
        }
    }

    static public String getOrganizationCode(HttpServletRequest request) {
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        if (gp != null) {
            ServiceProvider sp = gp.getSp();
            if (sp != null) {
                return sp.getOrganizationCode();
            } else {
                logger.error("Er is geen serviceprovider aanwezig bij GisPrincipal met naam: " + gp.getName());
                return null;
            }
        } else {
            logger.debug("Er is geen GisPrincipal aanwezig.");
            return null;
        }
    }
    
    public static void main(String [] args) throws CQLException {
        String cql = "aaa = null";
        Filter parentCqlFilter = CQL.toFilter(cql);
        System.out.println(parentCqlFilter);
    }
    
    
}
