package nl.b3p.gis.viewer.admindata;

import com.vividsolutions.jts.geom.Geometry;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.hibernate.Session;
import org.opengis.feature.Feature;
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

    public GegevensBronBean fillGegevensBronBean(int gegevensBronId, int themaId, String wkt, String cql, String parentHtmlId) throws Exception {

        boolean collectGeom = false;

        return fillGegevensBronBean(gegevensBronId, themaId, wkt, cql, collectGeom, parentHtmlId);
    }

    private GegevensBronBean fillGegevensBronBean(int gegevensBronId, int themaId, String wkt, String cql, boolean collectGeom, String parentHtmlId) {
        GegevensBronBean bean = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        sess.beginTransaction();

        Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gegevensBronId);
        if (gb == null) {
            return null;
        }

        /* addChilds */
        List childBronnen = sess.createQuery("from Gegevensbron where parent = :parentId order by volgordenr, naam")
                .setInteger("parentId", gb.getId()).list();

        bean = new GegevensBronBean();

        bean.setId(gb.getId());
        bean.setAdminPk(gb.getAdmin_pk());
        bean.setParentHtmlId(parentHtmlId);

        Themas thema = null;
        if (themaId > 0) {
            thema = (Themas) sess.get(Themas.class, themaId);
        }
        if (thema == null || thema.getNaam() == null || thema.getNaam().equals("")) {
            bean.setTitle(gb.getNaam());
        } else {
            bean.setTitle(thema.getNaam());
        }

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

        /* het request kan via context worden opgehaald indien deze methode
         * via dwr wordt aangeroepen */
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = null;

        if (ctx != null) {
            request = ctx.getHttpServletRequest();
        }

        Bron b = gb.getBron(request);

        if (b == null) {
            return null;
        }

        Geometry geom = null;
        try {
            geom = DataStoreUtil.createGeomFromWKTString(wkt);
        } catch (Exception ex) {
            logger.error("", ex);
        }

        List<String> propnames = bean.getKolomNamenList();
        Filter cqlFilter = null;
            try {
            cqlFilter = CQL.toFilter(cql);
        } catch (CQLException ex) {
            logger.error("", ex);
        }
 
        List<Feature> features = null;
        try {
            features = DataStoreUtil.getFeatures(b, gb, geom, cqlFilter, propnames, null, collectGeom);
        } catch (Exception ex) {
            logger.error("", ex);
        }

        if (features == null || features.isEmpty()) {
            return null;
        }

        Iterator featureIter = features.iterator();
        while (featureIter.hasNext()) {
            Feature f = (Feature) featureIter.next();

            RecordBean record = null;
            try {
                record = getRecordBean(f, gb, bean.getLabels());
            } catch (Exception ex) {
                logger.error("", ex);
            }
            if (record == null) {
                continue;
            }

            Iterator iter4 = childBronnen.iterator();

            while (iter4.hasNext()) {
                Gegevensbron child = (Gegevensbron) iter4.next();

                String fkField = child.getAdmin_fk();
                String recordId = record.getId().toString();

                Filter childFilter = null;
                ArrayList<Filter> filters = new ArrayList();
                if (cqlFilter!=null) {
                    filters.add(cqlFilter);
                }
                Filter attrFilter = null;
                if (fkField != null && recordId != null) {
                    attrFilter = FilterBuilder.createEqualsFilter(fkField, recordId);
                }
                if (attrFilter!=null) {
                    filters.add(attrFilter);
                }
                if (filters.size() == 1) {
                    childFilter = filters.get(0);
                }
                if (filters.size() > 1) {
                    childFilter = filterFac.and(filters);
                }

                int count = 0;
                try {
                    count = getAantalChildRecords(child, childFilter, geom);
                } catch (Exception ex) {
                    logger.error("", ex);
                }

                if (count > 0) {
                    RecordChildBean childBean = new RecordChildBean();
                    childBean.setId(child.getId().toString());
                    childBean.setGegevensBronBeanId(bean.getId());
                    childBean.setTitle(child.getNaam());
                    childBean.setAantalRecords(count);
                    childBean.setThemaId(new Integer(themaId).toString());
                    childBean.setCql(CQL.toCQL(attrFilter));

                    record.addChild(childBean);
                }
            }

            bean.addRecord(record);
        }

        bean.setCsvPksFromRecordBeans();
        return bean;
    }

    protected RecordBean getRecordBean(Feature f, Gegevensbron gb, List<LabelBean> label_bean_items) throws SQLException, UnsupportedEncodingException, Exception {
        RecordBean rb = new RecordBean();

        String adminPk = DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart();
        if (adminPk != null) {
            rb.setId(f.getProperty(adminPk).getValue());
        }

        if (label_bean_items == null) {
            return null;
        }

        Iterator it = label_bean_items.iterator();
        while (it.hasNext()) {
            LabelBean lb = (LabelBean) it.next();
            RecordValueBean rvb = new RecordValueBean();

            rvb.setType(lb.getType());
            rvb.setEenheid(lb.getEenheid());
            rvb.setKolomBreedte(lb.getKolomBreedte());

            /*
             * Controleer of de kolomnaam van dit themadata object wel voorkomt in de feature.
             * zoniet kan het zijn dat er een prefix ns in staat. Die moet er dan van afgehaald worden.
             * Als het dan nog steeds niet bestaat: een lege toevoegen.
             */
            String kolomnaam = lb.getKolomNaam();
            if (kolomnaam != null && kolomnaam.length() > 0) {
                kolomnaam = DataStoreUtil.convertFullnameToQName(lb.getKolomNaam()).getLocalPart();
            }

            List valueList = null;
            Object value = null;
            if (kolomnaam != null && f.getProperty(kolomnaam) != null) {
                value = f.getProperty(kolomnaam).getValue();
                //Kijk of er in de waarde van de kolomnaam een komma zit. Zoja, splits het dan op.
                valueList = splitObject(value, ",");
            }
            Object pkValue = null;
            if (adminPk != null) {
                pkValue = f.getProperty(adminPk).getValue();
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
            if (lb.getType().equals(RecordValueBean.TYPE_DATA) && kolomnaam != null) {
                if (value != null) {
                    rvb.setValue(value.toString());
                }
                /*
                 * In het tweede geval dient de informatie in de thema data als link naar een andere
                 * informatiebron. Deze link zal enigszins aangepast moeten worden om tot 
                 * werkende link te komen.
                 */
            } else if (lb.getType().equals(RecordValueBean.TYPE_URL)) {
                StringBuffer url;
                if (lb.getCommando() != null) {
                    url = new StringBuffer(lb.getCommando());
                } else {
                    url = new StringBuffer();
                }

                /* TODO: BOY: Welk id moet hier geappend worden ? */
                url.append(Themas.THEMAID);
                url.append("=");
                url.append(gb.getId());

                if (adminPk != null && pkValue != null) {
                    pkValue = f.getProperty(adminPk).getValue();
                    url.append("&");
                    url.append(adminPk);
                    url.append("=");
                    url.append(URLEncoder.encode(pkValue.toString().trim(), "utf-8"));
                }

                if (value != null && !kolomnaam.equalsIgnoreCase(adminPk)) {
                    url.append("&");
                    url.append(kolomnaam);
                    url.append("=");
                    url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                }

                rvb.setValue(url.toString());

                /*
                 * De laatste mogelijkheid betreft een query. Vanuit de themadata wordt nu een
                 * een commando url opgehaald en deze wordt met de kolomnaam aangevuld.
                 */
            } else if (lb.getType().equals(RecordValueBean.TYPE_QUERY)) {
                if (lb.getCommando() != null) {
                    HashMap fhm = toHashMap(f);
                    List regelValues = new ArrayList();
                    for (int i = 0; i < valueList.size(); i++) {
                        String commando = lb.getCommando().toString();
                        Object localValue = valueList.get(i);
                        if (commando.contains("[") || commando.contains("]")) {
                            //vervang de eventuele csv in 1 waarde van die csv
                            if (kolomnaam != null) {
                                fhm.put(kolomnaam, localValue);
                            }
                            String newCommando = replaceValuesInString(commando, fhm);
                            regelValues.add(newCommando);
                        } else if (localValue != null) {
                            commando += localValue.toString().trim();
                            regelValues.add(commando);
                        } else {
                            regelValues.add("");
                        }
                    }
                    rvb.setValueList(regelValues);

                } else if (value != null) {
                    // enkele waarde in lijst voor eenduidigheid tbv front-end
                    rvb.setValueList(valueList);
                }

            } else if (lb.getType().equals(RecordValueBean.TYPE_FUNCTION)) {
                if (pkValue != null) {
                    String attributeName = kolomnaam;
                    Object attributeValue = null;
                    if (attributeName != null) {
                        attributeValue = f.getProperty(attributeName).getValue();
                    } else {
                        attributeName = adminPk;
                        attributeValue = pkValue;
                    }

                    // De attributeValue ook eerst vooraan erbij zetten om die te kunnen tonen op de admindata pagina - Drie hekjes als scheidingsteken
                    StringBuilder function = new StringBuilder("");
                    function.append(attributeValue);
                    function.append("###").append(lb.getCommando());
                    function.append("(this, ");
                    function.append("'").append(gb.getId()).append("'");
                    function.append(",");
                    function.append("'").append(adminPk).append("'");
                    function.append(",");
                    function.append("'").append(pkValue).append("'");
                    function.append(",");
                    function.append("'").append(attributeName).append("'");
                    function.append(",");
                    function.append("'").append(attributeValue).append("'");
                    function.append(",");
                    function.append("'").append(lb.getEenheid()).append("'");
                    function.append(")");

                    rvb.setValue(function.toString());

                } else if (value != null) {
                    rvb.setValue(value.toString());
                }
            }

            rb.addValue(rvb);
        }

        return rb;
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

    public GegevensBronBean createTestGegevensBronBean(int id, String parentHtmlId) {
        GegevensBronBean gbBean = new GegevensBronBean();
        gbBean.setId(id);
        gbBean.setTitle("Test GegevensBronBean");
        gbBean.setParentHtmlId(parentHtmlId);

        LabelBean labelId = new LabelBean();
        labelId.setId(1);
        labelId.setLabel("id");

        LabelBean labelNaam = new LabelBean();
        labelNaam.setId(2);
        labelNaam.setLabel("naam");

        LabelBean labelUrl = new LabelBean();
        labelUrl.setId(3);
        labelUrl.setLabel("url");

        LabelBean labelQuery = new LabelBean();
        labelQuery.setId(4);
        labelQuery.setLabel("query");

        LabelBean labelFunction = new LabelBean();
        labelFunction.setId(5);
        labelFunction.setLabel("function");

        gbBean.addLabel(labelId);
        gbBean.addLabel(labelNaam);
        gbBean.addLabel(labelUrl);
        gbBean.addLabel(labelQuery);
        gbBean.addLabel(labelFunction);

        RecordBean record1 = new RecordBean();
        record1.setId(1);
        record1.setWkt(null);

        RecordValueBean value1 = new RecordValueBean();
        value1.setValue("1");
        value1.setType(RecordValueBean.TYPE_DATA);

        RecordValueBean value2 = new RecordValueBean();
        value2.setValue("Boy de Wit");
        value2.setType(RecordValueBean.TYPE_DATA);

        RecordValueBean value3 = new RecordValueBean();
        value3.setValue("www.spellenenzo.nl");
        value3.setType(RecordValueBean.TYPE_URL);

        RecordValueBean value4 = new RecordValueBean();
        value4.setValue("/viewerdata.do?kolom=BOY");
        value4.setType(RecordValueBean.TYPE_QUERY);

        RecordValueBean value5 = new RecordValueBean();
        value5.setValue("jsDoStuff()");
        value5.setType(RecordValueBean.TYPE_FUNCTION);

        record1.addValue(value1);
        record1.addValue(value2);
        record1.addValue(value3);
        record1.addValue(value4);
        record1.addValue(value5);

        RecordBean record2 = new RecordBean();
        record2.setId(2);
        record2.setWkt(null);

        RecordValueBean value6 = new RecordValueBean();
        value6.setValue("2");
        value6.setType(RecordValueBean.TYPE_DATA);

        RecordValueBean value7 = new RecordValueBean();
        value7.setValue("Chris van Lith");
        value7.setType(RecordValueBean.TYPE_DATA);

        RecordValueBean value8 = new RecordValueBean();
        value8.setValue("www.b3p.nl");
        value8.setType(RecordValueBean.TYPE_URL);

        RecordValueBean value9 = new RecordValueBean();
        value9.setValue("/viewerdata.do?kolom=CHRIS");
        value9.setType(RecordValueBean.TYPE_QUERY);

        RecordValueBean value10 = new RecordValueBean();
        value10.setValue("jsDoMoreStuff()");
        value10.setType(RecordValueBean.TYPE_FUNCTION);

        record2.addValue(value6);
        record2.addValue(value7);
        record2.addValue(value8);
        record2.addValue(value9);
        record2.addValue(value10);

        RecordChildBean child1 = new RecordChildBean();
        child1.setGegevensBronBeanId(83);
        child1.setTitle("Test child bij record 2");
        child1.setAantalRecords(25);

        record2.addChild(child1);

        gbBean.addRecord(record1);
        gbBean.addRecord(record2);

        return gbBean;
    }

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
}