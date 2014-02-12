package nl.b3p.gis.viewer;

import nl.b3p.gis.geotools.DataStoreUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;
import java.sql.DriverManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.zoeker.services.A11YResult;
import org.geotools.data.DataStore;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public class GetLocationData {

    private static final Log log = LogFactory.getLog(GetLocationData.class);
    private static int maxFeatures = 10000;

    public GetLocationData() {
    }

    public String getWkt(String ggbId, String attributeName, String compareValue) throws SQLException {
        String wkt = "";

        Transaction tx = null;
        DataStore ds = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        try {
            tx = sess.beginTransaction();

            Integer id = -1;

            id = new Integer(ggbId);

            if (id != null) {
                WebContext ctx = WebContextFactory.get();
                HttpServletRequest request = ctx.getHttpServletRequest();

                Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, id);

                if (gb != null) {
                    Bron b = gb.getBron(request);

                    if (b != null) {
                        ds = b.toDatastore();

                        if (ds != null) {
                            GeometryDescriptor gDescr = DataStoreUtil.getSchema(ds, gb).getGeometryDescriptor();

                            if (gDescr != null) {
                                String geometryName = gDescr.getLocalName();
                                ArrayList<String> propertyNames = new ArrayList();
                                propertyNames.add(geometryName);
                                ArrayList<Feature> list = DataStoreUtil.getFeatures(ds, gb, FilterBuilder.createEqualsFilter(attributeName, compareValue), propertyNames, 1, true);

                                if (list.size() >= 1) {
                                    Feature f = list.get(0);
                                    wkt = DataStoreUtil.selecteerKaartObjectWkt(f);
                                }
                            }
                        }
                    }
                }
            }

            tx.commit();

        } catch (NumberFormatException ex) {
            log.error("Fout tijdens omzetten gegevensbronid: " + ex);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception ex) {
            log.error("Fout tijdens ophalen wkt: " + ex);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } finally {
            if (ds != null) {
                ds.dispose();
            }
        }

        return wkt;
    }

    public String[] getArea(String elementId, String themaId, String attributeName, String compareValue, String eenheid) throws SQLException {
        Session sess = null;
        double area = 0.0;
        String[] returnValue = new String[2];
        returnValue[0] = elementId;
        returnValue[1] = "Fout (zie log)";

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            sess.beginTransaction();

            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();

            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, new Integer(themaId));
            Bron b = gb.getBron(request);

            if (b == null) {
                return returnValue;
            }
            DataStore ds = b.toDatastore();
            try {
                //haal alleen de geometry op.
                String geometryName = "the_geom";
                ArrayList<String> propertyNames = new ArrayList();
                propertyNames.add(geometryName);
                ArrayList<Feature> list = DataStoreUtil.getFeatures(ds, gb, FilterBuilder.createEqualsFilter(attributeName, compareValue), propertyNames, 1, true);
                if (list.size() >= 1) {
                    Feature f = list.get(0);
                    area = ((Geometry) f.getDefaultGeometryProperty().getValue()).getArea();
                }
            } finally {
                ds.dispose();
            }

        } catch (Exception ex) {
            log.error("", ex);
            return returnValue;
        } finally {
            sess.close();
        }
        if (eenheid != null && eenheid.equals("null")) {
            eenheid = null;
        }
        int divide = 0;
        if (eenheid != null) {
            if (eenheid.equalsIgnoreCase("km")) {
                divide = 1000000;
            } else if (eenheid.equalsIgnoreCase("hm")) {
                divide = 10000;
            }
        }
        if (area > 0.0) {
            if (divide > 0) {
                area /= divide;
            }
            area *= 100;
            area = Math.round(area);
            area /= 100;
        }
        String value = "" + area;
        if (eenheid != null) {
            value += " " + eenheid;
        } else {
            value += " m";
        }
        returnValue[1] = value;

        return returnValue;
    }

    public String zendMelding(String wkt, String tekst) throws Exception {
        if (wkt == null || wkt.length() == 0 || tekst == null || tekst.length() == 0) {
            return null;
        }
        Geometry geom = DataStoreUtil.createGeomFromWKTString(wkt);
        Point p = geom.getCentroid();
        String message = "<p>Een bericht met de inhoud \"";
        message += tekst;
        message += "\" voor locatie met RD-coordinaten (";
        message += (int) p.getX();
        message += ",";
        message += (int) p.getY();
        message += ") is verzonden naar de beheerder.<p>";

        long now = (new Date()).getTime();
        message += "<p>Uw referentienummer is: \"" + Long.toString(now, 32) + "\".<p>";

        return message;
    }

    public Map getAnalyseData(String wkt, String activeThemaIds, String extraCriterium) throws Exception {
        if (wkt == null || wkt.length() == 0 || activeThemaIds == null || activeThemaIds.length() == 0) {
            return null;
        }
        Geometry geom = DataStoreUtil.createGeomFromWKTString(wkt);
        String[] themaIds = activeThemaIds.split(",");
        if (themaIds == null || themaIds.length == 0) {
            return null;
        }

        Map results = new HashMap();

        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;

        try {
            tx = sess.beginTransaction();

            for (int i = 0; i < themaIds.length; i++) {
                Integer id = FormUtils.StringToInteger(themaIds[i].trim());
                if (id == null) {
                    continue;
                }

                Themas t = (Themas) sess.get(Themas.class, id);
                if (t == null) {
                    continue;
                }

                Bron b = (Bron) t.getConnectie(request);
                if (b == null) {
                    continue;
                }

                Map themaAnalyseData = calcThemaAnalyseData(b, t, extraCriterium, geom);

                if (themaAnalyseData != null) {
                    themaAnalyseData = formatResults(themaAnalyseData);
                    results.put(t.getNaam(), themaAnalyseData);
                }
            }

            tx.commit();

        } catch (Exception e) {
            log.error("Fout tijdens analyse:" + e);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }

        return results;
    }

    private double roundOneDigit(double val) {
        return Math.round(10.0 * val) / 10.0;
    }

    /**
     * TODO vullen met properties
     *
     * @param results
     * @return
     */
    private Map formatResults(Map results) {
        String themaName = (String) results.get("themaName") + ": ";
        results.remove("themaName");
        String eenheid;
        double analyseFactor;

        eenheid = " [m2]";
        analyseFactor = 1.0;
        if (results.get("sumPolygon") != null) {
            double sumPolygon = ((Double) results.get("sumPolygon")).doubleValue();
            sumPolygon = roundOneDigit(sumPolygon / analyseFactor);
            results.put("sumPolygonFormatted", "Totaal oppervlak " + themaName + Double.toString(sumPolygon) + eenheid);
            results.remove("sumPolygon");
        }
        if (results.get("maxPolygon") != null) {
            double maxPolygon = ((Double) results.get("maxPolygon")).doubleValue();
            maxPolygon = roundOneDigit(maxPolygon / analyseFactor);
            results.put("maxPolygonFormatted", "Grootst oppervlak " + themaName + Double.toString(maxPolygon) + eenheid);
            results.remove("maxPolygon");
        }
        if (results.get("minPolygon") != null) {
            double minPolygon = ((Double) results.get("minPolygon")).doubleValue();
            minPolygon = roundOneDigit(minPolygon / analyseFactor);
            results.put("minPolygonFormatted", "Kleinst oppervlak " + themaName + Double.toString(minPolygon) + eenheid);
            results.remove("minPolygon");
        }
        if (results.get("avgPolygon") != null) {
            double avgPolygon = ((Double) results.get("avgPolygon")).doubleValue();
            avgPolygon = roundOneDigit(avgPolygon / analyseFactor);
            results.put("avgPolygonFormatted", "Gemiddeld oppervlak " + themaName + Double.toString(avgPolygon) + eenheid);
            results.remove("avgPolygon");
        }
        eenheid = " [m]";
        analyseFactor = 1.0;
        if (results.get("sumLineString") != null) {
            double sumLineString = ((Double) results.get("sumLineString")).doubleValue();
            sumLineString = roundOneDigit(sumLineString / analyseFactor);
            results.put("sumLineStringFormatted", "Totale lengte " + themaName + Double.toString(sumLineString) + eenheid);
            results.remove("sumLineString");
        }
        if (results.get("maxLineString") != null) {
            double maxLineString = ((Double) results.get("maxLineString")).doubleValue();
            maxLineString = roundOneDigit(maxLineString / analyseFactor);
            results.put("maxLineStringFormatted", "Grootste lengte " + themaName + Double.toString(maxLineString) + eenheid);
            results.remove("maxLineString");
        }
        if (results.get("minLineString") != null) {
            double minLineString = ((Double) results.get("minLineString")).doubleValue();
            minLineString = roundOneDigit(minLineString / analyseFactor);
            results.put("minLineStringFormatted", "Kleinste lengte " + themaName + Double.toString(minLineString) + eenheid);
            results.remove("minLineString");
        }
        if (results.get("avgLineString") != null) {
            double avgLineString = ((Double) results.get("avgLineString")).doubleValue();
            avgLineString = roundOneDigit(avgLineString / analyseFactor);
            results.put("avgLineStringFormatted", "Gemiddelde lengte " + themaName + Double.toString(avgLineString) + eenheid);
            results.remove("avgLineString");
        }

        eenheid = " []";
        analyseFactor = 1.0;
        if (results.get("countPolygon") != null) {
            int countPolygon = ((Integer) results.get("countPolygon")).intValue();
            results.put("countPolygonFormatted", "Aantal vlakken " + themaName + Integer.toString(countPolygon) + eenheid);
            results.remove("countPolygon");
        }
        if (results.get("countLineString") != null) {
            int countLineString = ((Integer) results.get("countLineString")).intValue();
            results.put("countLineStringFormatted", "Aantal lijnen " + themaName + Integer.toString(countLineString) + eenheid);
            results.remove("countLineString");
        }
        if (results.get("countPoint") != null) {
            int countPoint = ((Integer) results.get("countPoint")).intValue();
            results.put("countPointFormatted", "Aantal punten" + themaName + Integer.toString(countPoint) + eenheid);
            results.remove("countPoint");
        }
        if (results.get("countUnknownBinding") != null) {
            int countUnknownBinding = ((Integer) results.get("countUnknownBinding")).intValue();
            results.put("countUnknownBindingFormatted", "Aantal onbekende objecten " + themaName + Integer.toString(countUnknownBinding) + eenheid);
            results.remove("countUnknownBinding");
        }

        return results;
    }

    /**
     * Methode wordt aangeroepen door knop "analysewaarde" op tabblad "Analyse"
     * en berekent een waarde op basis van actieve thema, plus evt. een extra
     * zoekcriterium voor de administratieve waarde in de basisregel, en een
     * gekozen gebied onder het klikpunt.
     *
     * @throws Exception exception
     *
     * waarde
     */
    private Map calcThemaAnalyseData(Bron b, Themas t, String extraCriterium, Geometry analyseGeometry) throws Exception {
        Gegevensbron gb = t.getGegevensbron();

        //maak het eventuele extra filter.
        Filter extraFilter = null;
        if (extraCriterium != null && extraCriterium.length() != 0) {
            List thema_items = SpatialUtil.getThemaData(gb, true);
            extraFilter = calculateExtraFilter(thema_items, extraCriterium);
        }
        return calcThemaAnalyseData(b, t, extraFilter, analyseGeometry);
    }

    private Map calcThemaAnalyseData(Bron b, Themas t, Filter extraFilter, Geometry analyseGeometry) throws Exception {
        DataStore ds = b.toDatastore();
        try {
            Gegevensbron gb = t.getGegevensbron();

            //Haal alle features op die binnen de analyseGeometry vallen:
            List<Feature> features = DataStoreUtil.getFeatures(b, gb, analyseGeometry, extraFilter, null, maxFeatures, true);

            if (features == null || features.isEmpty()) {
                return null;
            }
            // werkt niet omdat er altijd Geometry in zit, dus in eerste feature kijken en hopen
            // dat ze allemaal het zelfde zijn :-(
            // GeometryType tgt = DataStoreUtil.getSchema(ds, t).getGeometryDescriptor().getType();
            // GeometryType tgt = features.get(0).getDefaultGeometryProperty().getDescriptor().getType();
            GeometryType tgt = DataStoreUtil.getGeometryType(features.get(0));
            Class binding = tgt.getBinding();

            double sumPolygon = 0.0; // surface
            double sumLineString = 0.0; // length
            double maxPolygon = 0.0;
            double maxLineString = 0.0;
            double minPolygon = 0.0;
            double minLineString = 0.0;
            double avgPolygon = 0.0;
            double avgLineString = 0.0;
            int countPolygon = 0;
            int countLineString = 0;
            int countPoint = 0;
            int countUnknownBinding = 0;

            for (Feature f : features) {
                Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
                if (geom == null) {
                    continue;
                }
                double thisArea = geom.getArea();
                double thisLength = geom.getLength();
                int thisCount = geom.getNumGeometries();

                if (binding == Polygon.class || binding == MultiPolygon.class) {
                    if (thisArea > maxPolygon) {
                        maxPolygon = thisArea;
                    }
                    if (thisArea < minPolygon || minPolygon <= 0) {
                        minPolygon = thisArea;
                    }
                    sumPolygon += thisArea;
                    countPolygon += thisCount;
                } else if (binding == LineString.class || binding == MultiLineString.class) {
                    if (thisLength > maxLineString) {
                        maxLineString = thisLength;
                    }
                    if (thisLength < minLineString || minLineString <= 0) {
                        minLineString = thisLength;
                    }
                    sumLineString += thisLength;
                    countLineString += thisCount;
                } else if (binding == Point.class || binding == MultiPoint.class) {
                    countPoint += thisCount;
                } else {
                    countUnknownBinding += thisCount;
                }
            }
            if (countPolygon > 0) {
                avgPolygon = sumPolygon / countPolygon;
            }
            if (countLineString > 0) {
                avgLineString = sumLineString / countLineString;
            }

            Map featureResults = new HashMap();
            featureResults.put("themaName", t.getNaam());
            if (binding == Polygon.class || binding == MultiPolygon.class) {
                featureResults.put("sumPolygon", Double.valueOf(sumPolygon));
                featureResults.put("maxPolygon", Double.valueOf(maxPolygon));
                featureResults.put("minPolygon", Double.valueOf(minPolygon));
                featureResults.put("avgPolygon", Double.valueOf(avgPolygon));
                featureResults.put("countPolygon", Integer.valueOf(countPolygon));
            } else if (binding == LineString.class || binding == MultiLineString.class) {
                featureResults.put("sumLineString", Double.valueOf(sumLineString));
                featureResults.put("maxLineString", Double.valueOf(maxLineString));
                featureResults.put("minLineString", Double.valueOf(minLineString));
                featureResults.put("avgLineString", Double.valueOf(avgLineString));
                featureResults.put("countLineString", Integer.valueOf(countLineString));
            } else if (binding == Point.class || binding == MultiPoint.class) {
                featureResults.put("countPoint", Integer.valueOf(countPoint));
            } else {
                featureResults.put("countUnknownBinding", Integer.valueOf(countUnknownBinding));
            }

            return featureResults;
        } finally {
            ds.dispose();
        }
    }

    public static Filter calculateExtraFilter(List thema_items, String extraCriterium) {
        ArrayList<Filter> andFilters = new ArrayList();
        if (thema_items != null && thema_items.size() > 0
                && extraCriterium != null && extraCriterium.length() > 0) {
            extraCriterium = extraCriterium.replaceAll("\\'", "''");
            Iterator it = thema_items.iterator();
            ArrayList<Filter> filters = new ArrayList();
            while (it.hasNext()) {
                ThemaData td = (ThemaData) it.next();
                Filter f = FilterBuilder.createLikeFilter(
                        DataStoreUtil.convertFullnameToQName(td.getKolomnaam()).getLocalPart(),
                        '%' + extraCriterium + '%');
                if (f != null) {
                    filters.add(f);
                }
            }
            Filter f = FilterBuilder.getFactory().or(filters);
            if (f != null) {
                andFilters.add(f);
            }
        }

        if (andFilters.isEmpty()) {
            return null;
        }
        if (andFilters.size() == 1) {
            return andFilters.get(0);
        } else {
            return FilterBuilder.getFactory().and(andFilters);
        }
    }

    /* LatLon coordinaten en latlon span teruggeven voor gebruik in Google Map url
     Input is Point in het midden en Point linksonder en rechtsboven van de huidige extent */
    public String[] getLatLonForRDPoint(String centerWkt, String minWkt, String maxWkt) {
        String[] llSpnParams = new String[4];

        Point centerPoint = convertWktToLatLonPoint(centerWkt);
        Point southWestPoint = convertWktToLatLonPoint(minWkt);
        Point northEastPoint = convertWktToLatLonPoint(maxWkt);

        double spnLat = northEastPoint.getX() - southWestPoint.getX();
        double spnLon = northEastPoint.getY() - southWestPoint.getY();

        llSpnParams[0] = Double.toString(centerPoint.getX());
        llSpnParams[1] = Double.toString(centerPoint.getY());
        llSpnParams[2] = Double.toString(spnLat);
        llSpnParams[3] = Double.toString(spnLon);

        return llSpnParams;
    }
    
    public String[] getLatLonForGoogleDirections(String destWkt) {
        String[] llParams = new String[4];
        
        Geometry startGeom = getStartLocation();
        if (startGeom != null) {
            Point start = convertWktToLatLonPoint(startGeom.toText());

            llParams[0] = Double.toString(start.getX());
            llParams[1] = Double.toString(start.getY());
        } else {
            llParams[0] = "";
            llParams[1] = "";
        }

        Point centerPoint = convertWktToLatLonPoint(destWkt);

        llParams[2] = Double.toString(centerPoint.getX());
        llParams[3] = Double.toString(centerPoint.getY());

        return llParams;
    }

    private Geometry getStartLocation() {
        Geometry startGeom = null;
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = null;

        if (ctx != null) {
            request = ctx.getHttpServletRequest();
        }

        HttpSession session = request.getSession(true);
        A11YResult a11yResult = (A11YResult) session.getAttribute("a11yResult");
        if (a11yResult != null && a11yResult.getStartWkt() != null) {
            startGeom = createGeomFromWkt(a11yResult.getStartWkt());
        }

        return startGeom;
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

    private Point convertWktToLatLonPoint(String wkt) {
        Point p = null;

        try {
            Geometry sourceGeometry = DataStoreUtil.createGeomFromWKTString(wkt);

            if (sourceGeometry != null) {
                CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:28992");
                CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");

                MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

                if (transform != null) {
                    Geometry targetGeometry = JTS.transform(sourceGeometry, transform);

                    if (targetGeometry != null) {
                        targetGeometry.setSRID(4326);
                        p = targetGeometry.getCentroid();
                    }
                }
            }

        } catch (Exception ex) {
            log.error("Fout tijdens conversie wkt naar latlon: " + ex);
        }

        return p;
    }

    /**
     *
     * @param elementId element in html pagina waar nieuwe waarde naar wordt
     * geschreven
     * @param themaId id van thema waar update betrekking op heeft
     * @param keyName naam van primary key voor selectie van juiste row
     * @param keyValue waarde van primary key voor selectie
     * @param attributeName kolomnaam die veranderd moet worden
     * @param oldValue oude waarde van de kolom
     * @param newValue nieuwe waarde van de kolom
     * @return array van 2 return waarden: 1=elementId, 2=oude of nieuwe waarde
     * met fout indicatie
     */
    public String[] setAttributeValue(String elementId, String themaId, String keyName, String keyValue, String attributeName, String oldValue, String newValue) {
        String[] returnValue = new String[2];
        Transaction transaction = null;
        try {
            returnValue[0] = elementId;
            returnValue[1] = oldValue + " (fout)";

            Integer id = FormUtils.StringToInteger(themaId);
            int keyValueInt = FormUtils.StringToInt(keyValue);
            if (id == null || keyValueInt == 0) {
                return returnValue;
            }
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = sess.beginTransaction();

            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, id);
            if (gb == null) {
                return returnValue;
            }

            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();
            Bron b = (Bron) gb.getBron(request);

            if (b == null) {
                return returnValue;
            }

            if (b.checkType(Bron.TYPE_JDBC)) {
                Connection conn = DriverManager
                        .getConnection(b.getUrl(), b.getGebruikersnaam(), b.getWachtwoord());

                /*let op indien table een view is dan kan deze alleen geupdate worden indien
                 * er een UPDATE INSTEAD rule is aangemaakt */
                String tableName = gb.getAdmin_tabel();

                try {
                    String retVal = SpatialUtil.setAttributeValue(conn, tableName, keyName, keyValueInt, attributeName, newValue);

                    if (retVal != null) {
                        returnValue[1] = retVal;
                    }

                } catch (SQLException ex) {
                    log.error("", ex);
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            log.error("", ex);
                        }
                    }
                }
            } else {
                log.error("Incorrecte bron: " + gb.getBron().getNaam(), new UnsupportedOperationException("Function only supports jdbc connections"));
            }
        } catch (Exception e) {
            log.error("", e);
        }

        return returnValue;
    }

    /**
     * In eerste instantie direct uit jsp via dwr aanroepen, later wrappen voor
     * meer veiligheid
     *
     * @param x_input
     * @param y_input
     * @param cols
     * @param sptn
     * @param distance
     * @param srid
     * @return
     *
     * TODO: ook een WFS thema moet mogelijk worden.
     */
    public String[] getData(String x_input, String y_input, String[] cols, int themaId, double distance, int srid) throws SQLException {
        String[] results = new String[cols.length + 3];
        return new String[]{"Fout bij laden van data. Functie nog niet omgezet"};

    }
    
    public String getKaartlaagInfoTekst(Integer themaId) {
        if (themaId == null || themaId < 1) {
            return null;
        }
        
        String infoTekst = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;
                
        try {
            tx = sess.beginTransaction();

            Themas thema = (Themas) sess.get(Themas.class, themaId);
            if (thema != null) {
                infoTekst = thema.getInfo_tekst();
            }

            tx.commit();

        } catch (Exception e) {
            log.error("Fout tijdens ophalen Kaartlaag info tekst: ", e);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }

        return infoTekst;
    }
}
