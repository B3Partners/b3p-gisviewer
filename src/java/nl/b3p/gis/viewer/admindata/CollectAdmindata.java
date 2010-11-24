package nl.b3p.gis.viewer.admindata;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
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
import org.hibernate.Session;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

/**
 *
 * @author Chris en Boy
 */
public class CollectAdmindata {

    private static final Log logger = LogFactory.getLog(CollectAdmindata.class);

    protected static final double DEFAULTTOLERANCE = 5.0;
    
    public GegevensBronBean fillGegevensBronBean(int gegevensBronId, String title, String wkt, String scale, String tolerance, String coordList) throws Exception {
        Session sess = null;
        
        try {
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            sess.beginTransaction();
        
            Gegevensbron gb = (Gegevensbron)sess.get(Gegevensbron.class, gegevensBronId);

            if (gb == null) {
                return null;
            }

            GegevensBronBean bean = new GegevensBronBean();

            if (title == null || title.equals("")) {
                bean.setTitle(gb.getNaam());
            } else {
                bean.setTitle(title);
            }

            /* Per ThemaData een LabelBean toevoegen */
            List objectdata_items = SpatialUtil.getThemaData(gb, true);
            Iterator iter = objectdata_items.iterator();
            while(iter.hasNext()) {
                ThemaData td = (ThemaData)iter.next();

                if (!td.isBasisregel())
                    continue;

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

            /* Records ophalen en toevoegen */
            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();

            Bron b = gb.getBron(request);

            if (b == null) {
                return null;
            }

            DataStore ds = b.toDatastore();
            
            try {
                
                Geometry geom = getGeometry(wkt, scale, tolerance, coordList);
                
                List<String> propnames = bean.getKolomNamenList();
                
                List<String> propnames = DataStoreUtil.basisRegelThemaData2PropertyNames(thema_items);
                List<Feature> features = DataStoreUtil.getFeatures(b, gb, geom, extraFilter, propnames, null, false);
                List<AdminDataRowBean> regels = new ArrayList();
                for (int i = 0; i < features.size(); i++) {
                    Feature f = (Feature) features.get(i);
                    regels.add(getRegel(f, gb, thema_items));
                }
                
            } finally {
                ds.dispose();
            }

        } catch (Exception ex) {
            log.error("", ex);
            return wkt;
        } finally {
            sess.close();
        }
        
        
        
        
        
        List list = new ArrayList();

        if (b != null) {
            list = getThemaObjectsWithGeom(thema, thema_items, request);
        }

        Iterator iter2 = list.iterator();

        int recordId = 0;
        while (iter2.hasNext()) {
            AdminDataRowBean rowBean = (AdminDataRowBean)iter2.next();

            RecordBean rb = new RecordBean();
            rb.setId(recordId);
            recordId++;

            rb.setWkt(rb.getWkt());

            List values = rowBean.getValues();
            Iterator iter3 = values.iterator();

            int i = 0;
            while(iter3.hasNext()) {
                ThemaData td = (ThemaData)thema_items.get(i);

                if (!td.isBasisregel())
                    continue;

                String waarde = (String)iter3.next();

                RecordValueBean valueBean = new RecordValueBean();
                valueBean.setValue(waarde);
                
                int typeId = td.getDataType().getId();

                valueBean.setType(RecordValueBean.getStringType(typeId));

                rb.addValue(valueBean);

                i++;
            }

            /* childs adden */
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            List childBronnen = sess.createQuery("from Gegevensbron where parent = :parentId")
                .setInteger("parentId", gb.getId()).list();

            Iterator iter4 = childBronnen.iterator();

            while(iter4.hasNext()) {
                Gegevensbron child = (Gegevensbron)iter4.next();

                bron childBron = child.getBron();

                DataStore ds

                RecordChildBean childBean = new RecordChildBean();
                childBean.setGegevensBronBeanId(child.getId());
                childBean.setTitle(child.getNaam());
                childBean.setAantalRecords(0);
                
                rb.addChild(childBean);
            }

            bean.addRecord(rb);
        }

        return bean;
    }

    protected RecordBean getRecordBean(Feature f, Gegevensbron gb, List<LabelBean> label_bean_items) throws SQLException, UnsupportedEncodingException, Exception {
        RecordBean rb = new RecordBean();

        String adminPk = DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart();
        if (adminPk != null) {
            rb.setId(f.getProperty(adminPk).getValue());
        }

        Iterator it = label_bean_items.iterator();
        while (it.hasNext()) {
            LabelBean lb = (LabelBean) it.next();
            RecordValueBean rvb = new RecordValueBean();
            /*
             * Controleer of de kolomnaam van dit themadata object wel voorkomt in de feature.
             * zoniet kan het zijn dat er een prefix ns in staat. Die moet er dan van afgehaald worden.
             * Als het dan nog steeds niet bestaat: een lege toevoegen.
             */
            String kolomnaam = lb.getKolomNaam();
            if (kolomnaam!=null && kolomnaam.length()>0) {
                    kolomnaam = DataStoreUtil.convertFullnameToQName(lb.getKolomNaam()).getLocalPart();
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
                if (f.getProperty(kolomnaam).getValue() != null) {
                    rvb.setValue(f.getProperty(kolomnaam).getValue().toString());
                }
                /*
                 * In het tweede geval dient de informatie in de thema data als link naar een andere
                 * informatiebron. Deze link zal enigszins aangepast moeten worden om tot vollende
                 * werkende link te dienen.
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

                rvb.setValue(url.toString());

                /*
                 * De laatste mogelijkheid betreft een query. Vanuit de themadata wordt nu een
                 * een commando url opgehaald en deze wordt met de kolomnaam aangevuld.
                 */
            } else if (lb.getType().equals(RecordValueBean.TYPE_QUERY)) {
                StringBuffer url;
                if (lb.getCommando() != null) {
                    url = new StringBuffer(lb.getCommando());
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

                    rvb.setValueList(regelValues);

                } else {

                    if (f.getProperty(kolomnaam).getValue() != null) {
                        rvb.setValue(f.getProperty(kolomnaam).getValue().toString());
                    }
                }

            } else if (lb.getType().equals(RecordValueBean.TYPE_FUNCTION)) {
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
                    function.append("###").append(lb.getCommando());
                    function.append("(this, ");
                    function.append("'").append(gb.getId()).append("'");
                    function.append(",");
                    function.append("'").append(adminPk).append("'");
                    function.append(",");
                    function.append("'").append(keyValue).append("'");
                    function.append(",");
                    function.append("'").append(attributeName).append("'");
                    function.append(",");
                    function.append("'").append(attributeValue).append("'");
                    function.append(",");
                    function.append("'").append(lb.getEenheid()).append("'");
                    function.append(")");

                    rvb.setValue(function.toString());
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

    private Geometry getGeometry(String wkt, String scale, String tolerance, String coordList) {

        double distance = getDistance(scale, tolerance);
        Geometry geometry = null;
        if (wkt != null) {
            geometry = SpatialUtil.geometrieFromText(wkt, 28992);
        } else {
            GeometryFactory gf = new GeometryFactory();
            double[] coords = getCoords(coordList);
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

    protected double getDistance(String s, String tolerance) {

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

    protected double[] getCoords(String coordList) {
        double[] coords = null;
        if (coordList != null && !coordList.equals("")) {
            String[] coordString = coordList.split(",");
            coords = new double[coordString.length];
            for (int i = 0; i < coordString.length; i++) {
                coords[i] = Double.parseDouble(coordString[i]);
            }
        }
        return coords;
    }

    /*
    protected int getAantalChildRecords(Themas t, List<ThemaData> thema_items, HttpServletRequest request) throws Exception {
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

   */
}
