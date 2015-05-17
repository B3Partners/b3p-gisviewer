package nl.b3p.gis.utils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.PolygonExtracter;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.DataStore;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.Feature;

/**
 *
 * @author Boy de Wit
 */
public class EditUtil {

    private static final Log log = LogFactory.getLog(EditUtil.class);
    private static final int QUAD_SEGS = 8;
    protected static final double DEFAULTTOLERANCE = 5.0;

    /**
     * Constructor
     *
     */
    public EditUtil() throws Exception {
    }

    public String buffer(String wkt, Double bufferafstand) throws Exception {

        if ((wkt == null) || (wkt.length() < 1)) {
            throw new Exception("Kan niet bufferen. Er is nog geen handeling geselecteerd.");
        }

        if (bufferafstand == null || bufferafstand == 0) {
            throw new Exception("De bufferafstand mag niet 0 zijn.");
        }

        String buffer = "";
        Geometry geom = null;
        geom = DataStoreUtil.createGeomFromWKTString(wkt);

        if (geom == null || geom.isEmpty()) {
            throw new Exception("Kan niet bufferen. Geometrie is incorrect.");
        }

        if (geom.toString().indexOf("POINT") != -1 && bufferafstand < 0) {
            throw new Exception("De bufferafstand moet groter dan 0 zijn.");
        }

        Geometry result = null;

        if (geom.toString().indexOf("POINT") != -1) {
            result = geom.buffer(bufferafstand);
        } else {
            result = geom.buffer(bufferafstand, QUAD_SEGS, BufferOp.CAP_ROUND);
        }

        if (result == null || result.isEmpty()) {
            throw new Exception("Resultaat buffer geeft incorrecte geometrie.");
        }

        Geometry poly = getLargestPolygonFromMultiPolygon(result);

        if (poly == null || poly.isEmpty()) {
            throw new Exception("Bufferfout bij omzetten MultiPolygon naar grootste Polygon.");
        }

        buffer = poly.toText();

        return buffer;
    }

    //TODO CvL
    public String getHighlightWktForThema(String themaIds, String wktPoint, String schaal, String tol, String currentWkt, String appCode) {

        String wkt = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;

        try {
            tx = sess.beginTransaction();

            Geometry geom = DataStoreUtil.createGeomFromWKTString(wktPoint);

            if (geom != null) {
                Themas thema = SpatialUtil.getThema(themaIds);

                if (thema != null) {
                    List thema_items = SpatialUtil.getThemaData(thema.getGegevensbron(), true);

                    if (thema_items.size() > 0) {
                        WebContext ctx = WebContextFactory.get();
                        HttpServletRequest request = ctx.getHttpServletRequest();
                        Bron b = (Bron) thema.getConnectie(request);

                        if (b != null) {
                            GisPrincipal user = GisPrincipal.getGisPrincipal(request);

                            if (thema.hasValidAdmindataSource(user)) {
                                /* geom bufferen met tolerance distance */
                                double distance = getDistance(schaal, tol);

                                if (distance > 0) {
                                    geom = geom.buffer(distance);
                                }

                                Gegevensbron gb = thema.getGegevensbron();
                                Integer maximum = ConfigKeeper.getMaxNumberOfFeatures(appCode);
                                ArrayList<Feature> features = DataStoreUtil.getFeatures(b, gb, geom, null, DataStoreUtil.basisRegelThemaData2PropertyNames(thema_items), maximum, true);

                                if ((features != null) && (features.size() > 0)) {
                                    Feature f = features.get(0);

                                    if (features.size() > 1) {
                                        log.debug("Service geeft meerdere features terug. Eerste feature gebruikt. Wkt string = " + wkt);
                                    }

                                    if (f != null || f.getDefaultGeometryProperty() != null) {
                                        wkt = DataStoreUtil.selecteerKaartObjectWkt(f);
                                    }
                                }

                            }
                        }
                    }
                }
            }

            tx.commit();

        } catch (Exception ex) {
            log.error("Fout tijdens ophalen wkt: " + ex);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }

        /* Probeer eerdere wkt te mergen met nieuw geselecteerde object wkt */
        Geometry currentGeom;
        Geometry newGeom;
        Geometry mergedGeom = null;

        try {
            if (currentWkt != null && !currentWkt.equals("")) {
                currentGeom = DataStoreUtil.createGeomFromWKTString(currentWkt);

                if (currentGeom != null && wkt != null && !wkt.equals("")) {
                    newGeom = DataStoreUtil.createGeomFromWKTString(wkt);
                    mergedGeom = currentGeom.union(newGeom);
                }
            }

            if (mergedGeom != null) {
                wkt = mergedGeom.toText();
            }
        } catch (Exception ex) {
            log.debug("Fout tijdens mergen wkt");
        }

        if (wkt == null) {
            log.debug("Wkt string is leeg bij selecteren kaartobject.");
            return "-1";
        }

        return wkt;
    }

    public String getIdAndWktForRedliningObject(String wkt, Integer redLineGegevensbronId, String schaal, String tol, String appCode) throws Exception {

        /* Als er geen redlining gegevensbron bekend is dan HP */
        if (redLineGegevensbronId == null || redLineGegevensbronId < 0) {
            return "-1";
        }

        String jsonObject = null;

        /* Redlineobject zoeken en jsonObject teruggeven record zodat
         formulier met deze waardes ingevuld kan worden */
        Geometry geom = DataStoreUtil.createGeomFromWKTString(wkt);
        double distance = getDistance(schaal, tol);

        if (distance > 0) {
            geom = geom.buffer(distance);
        }

        ArrayList<Feature> features = doQueryRedliningObject(geom, redLineGegevensbronId, appCode);

        if ((features != null) && (features.size() > 0)) {
            Feature f = features.get(0);

            if (features.size() > 1) {
                log.debug("Meerdere redline objecten gevonden. Eerste feature gebruikt. Wkt string = " + wkt);
            }

            jsonObject = featureToJson(f).toString();
        }

        /* Als er geen redline object gevonden wordt dan HP */
        if (features == null || features.size() < 1 || jsonObject == null) {
            return "-1";
        }

        return jsonObject;
    }

    private JSONObject featureToJson(Feature f) throws JSONException {
        String wkt = "";

        if (f != null || f.getDefaultGeometryProperty() != null) {
            wkt = DataStoreUtil.selecteerKaartObjectWkt(f);
        }

        /* TODO: Zorgen dat f.getProperty hoofdlettergevoelig onafhankelijk
         * werkt, dus zowel voor Postgres als Oracle */
        String id = f.getProperty("id").getValue().toString();
        String projectnaam = f.getProperty("projectnaam").getValue().toString();
        String ontwerp = f.getProperty("ontwerp").getValue().toString();

        String opmerking = null;
        if (f.getProperty("opmerking").getValue() != null) {
            opmerking = f.getProperty("opmerking").getValue().toString();
        }

        JSONObject json = new JSONObject()
                .put("id", id)
                .put("projectnaam", projectnaam)
                .put("wkt", wkt)
                .put("ontwerp", ontwerp)
                .put("opmerking", opmerking);

        return json;
    }

    //TODO CvL
    protected ArrayList<Feature> doQueryRedliningObject(Geometry geom, Integer gbId, String appCode) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Transaction tx = null;
        DataStore ds = null;
        ArrayList<Feature> features = null;

        try {
            tx = sess.beginTransaction();

            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gbId);
            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();

            Bron b = gb.getBron(request);
            ds = b.toDatastore();

            /* Ophalen count van een RO Online WFS duurt best lang */
            if (b.getType().equals(Bron.TYPE_WFS)) {
                return new ArrayList();
            }

            List thema_items = SpatialUtil.getThemaData(gb, false);
            List<String> propnames = DataStoreUtil.themaData2PropertyNames(thema_items);

            /* TODO: groepnaam en projectfilter meegegeven */

            Integer maximum = ConfigKeeper.getMaxNumberOfFeatures(appCode);
            features = DataStoreUtil.getFeatures(b, gb, geom, null, propnames, maximum, true);

            tx.commit();

        } catch (Exception ex) {
            log.error("Fout tijdens ophalen redlining: ", ex);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }

        } finally {
            if (ds != null) {
                ds.dispose();
            }
        }

        return features;
    }

    private Geometry getLargestPolygonFromMultiPolygon(Geometry multipolygon) {

        if (multipolygon == null) {
            return null;
        }

        if (multipolygon.isEmpty()) {
            return null;
        }

        Polygon single = null;

        /* convert multipolygon to single polygons */
        List list = new ArrayList();
        list = PolygonExtracter.getPolygons(multipolygon);
        Iterator iter = list.iterator();

        double opp = 0;

        while (iter.hasNext()) {
            Polygon po = (Polygon) iter.next();
            double temp = po.getArea();

            if (temp > opp) {
                single = po;
                opp = temp;
            }
        }

        LineString line = single.getExteriorRing();

        if (line instanceof LinearRing) {
            return new Polygon((LinearRing) line, null, new GeometryFactory(new PrecisionModel(), 28992));
        }

        return null;
    }

    private int getNumInteriorRings(Geometry geom) {
        List list = new ArrayList();
        list = PolygonExtracter.getPolygons(geom);
        Iterator iter = list.iterator();

        Polygon single = null;

        double opp = 0;

        while (iter.hasNext()) {
            Polygon po = (Polygon) iter.next();
            double temp = po.getArea();

            if (temp > opp) {
                single = po;
                opp = temp;
            }
        }

        return single.getNumInteriorRing();
    }

    private double getDistance(String schaal, String tol) {
        String s = schaal;
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
            log.debug("Scale is geen double dus wordt genegeerd");
        }
        String tolerance = tol;
        double clickTolerance = DEFAULTTOLERANCE;
        try {
            if (tolerance != null) {
                clickTolerance = Double.parseDouble(tolerance);
            }
        } catch (NumberFormatException nfe) {
            clickTolerance = DEFAULTTOLERANCE;
            log.debug("Tolerance is geen double dus de default wordt gebruikt: " + DEFAULTTOLERANCE + " pixels");
        }
        double distance = clickTolerance;
        if (scale > 0.0) {
            distance = scale * (clickTolerance);
        }
        return distance;
    }
}
