/*
 * Copyright (C) 2012 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.jdbc.JDBCDataStore;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class EditFeature {

    private static final Log log = LogFactory.getLog(EditFeature.class);
    private final static String SUCCESS_PARAM = "success";

    /**
     * Retrieves the featuretype of a given Gegevensbron. It is a JSONObject,
     * which follows the next scheme: { success: boolean, featurename: String,
     * gegevensbronId: Integer, values: Array of editable columns in the format
     * { columnname: String, label: String, units: String, defaultValues:
     * (optional) Array of defaults, type: String with the datatype of the
     * column }, readonly: Array of non-editable columns in the format {
     * columnname: String, label: String, units: String, defaultValues:
     * (optional) Array of defaults, type: String with the datatype of the
     * column }, geom_attribute : (optional if editable) String geom_type :
     * (optional if editable) String [Polygon|LineString|Point] }
     *
     * @param gegevensBronId
     * @return
     * @throws JSONException
     */
    public String getFeatureType(Integer gegevensBronId) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(SUCCESS_PARAM, false);
        DataStore ds = null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        org.hibernate.Transaction tx = null;
        try {
            tx = sess.beginTransaction();

            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gegevensBronId);
            Bron b = gb.getBron();
            ds = b.toDatastore();
            SimpleFeatureType type = ds.getSchema(gb.getAdmin_tabel());
            Set<ThemaData> themadata = gb.getThemaData();
            List<ThemaData> editables = new ArrayList<ThemaData>();
            List<ThemaData> readonlyAttributes = new ArrayList<ThemaData>();
            for (Iterator<ThemaData> it = themadata.iterator(); it.hasNext();) {
                ThemaData themaData = it.next();
                if (themaData.isEditable()) {
                    editables.add(themaData);
                } else {
                    if (themaData.isBasisregel() && (!themaData.getDataType().getNaam().equals("Javascript functie"))) {
                        readonlyAttributes.add(themaData);
                    }
                }
            }

            JSONObject feat = new JSONObject();
            feat.put("featurename", gb.getNaam());
            feat.put("gegevensbronId", gb.getId());
            JSONArray vals = new JSONArray();
            for (Iterator<ThemaData> it1 = editables.iterator(); it1.hasNext();) {
                ThemaData themaData = it1.next();
                JSONObject obj = themaData.toJSON();
                AttributeDescriptor attr = type.getDescriptor(themaData.getKolomnaam());
                if (attr != null) {
                    String name = attr.getType().getBinding().getName();
                    name = name.substring(name.lastIndexOf(".") + 1);
                    obj.put("type", name);
                } else {
                    obj.put("type", "String");
                }

                vals.put(obj);
            }
            feat.put("values", vals);


            JSONArray readOnlyAttrs = new JSONArray();
            for (Iterator<ThemaData> it1 = readonlyAttributes.iterator(); it1.hasNext();) {
                ThemaData themaData = it1.next();
                JSONObject obj = themaData.toJSON();
                readOnlyAttrs.put(obj);
            }

            feat.put("readonly", readOnlyAttrs);
            if (gb.isGeometryeditable()) {
                GeometryDescriptor gp = type.getGeometryDescriptor();
                feat.put("geom_attribute", gp.getName().toString());
                feat.put("geom_type", getGeomType(gp.getType().getBinding()));
            }
            json.put("featuretype", feat);
            tx.commit();
            json.put(SUCCESS_PARAM, true);
        } catch (Exception ex) {
            log.error("Exception while getting the features in EditFeature.java:", ex);
            json.put("message", ex.getMessage());
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } finally {
            if (ds != null) {
                ds.dispose();
            }
        }
        return json.toString();
    }

    /**
     * Get features given a WKT and a distance from a Gegevensbron. This follows
     * the format: { success: boolean, features: (optional if success == true)
     * Array of JSONObjects, following the format:
     *
     * featurename: String, gegevensbronId: Integer, values: Array of editable
     * columns in the format { columnname: String, label: String, units: String,
     * defaultValues: (optional) Array of defaults, type: String with the
     * datatype of the column }, readonly: Array of non-editable columns in the
     * format { columnname: String, label: String, units: String, defaultValues:
     * (optional) Array of defaults, type: String with the datatype of the
     * column }, geom_attribute : (optional if editable) String, geom :
     * (optional if editable and known) String of Well Known Text }
     *
     * @param gegevensBronId The id of the Gegevensbron
     * @param wkt The Well Known Text of the clicking point
     * @param distance The Distance for which the wkt must be buffered.
     * Important for scale dependend retrieval of features
     * @return A Stringified JSONObject with the answer of the request
     * @throws JSONException
     */
    public String getFeatureByWkt(String gegevensBronId, String wkt, Double distance) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(SUCCESS_PARAM, false);
        DataStore ds = null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        org.hibernate.Transaction tx = null;
        try {
            tx = sess.beginTransaction();

            Integer gbIntId = Integer.valueOf(gegevensBronId);
            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gbIntId);

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            Bron b = gb.getBron();
            ds = b.toDatastore();
            SimpleFeatureType ft = ds.getSchema(gb.getAdmin_tabel());
            GeometryDescriptor gd = ft.getGeometryDescriptor();

            WKTReader reader = new WKTReader();
            Geometry g = reader.read(wkt);
            g = g.buffer(distance);
            Filter filter = ff.intersects(ff.property(gd.getLocalName()), ff.literal(g));
            json = getEditableFeatureAttributes(filter, gb);

            tx.commit();
            json.put(SUCCESS_PARAM, true);
        } catch (Exception ex) {
            log.error("Exception while getting the features in EditFeature.java:", ex);
            json.put("message", ex.getMessage());
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } finally {
            if (ds != null) {
                ds.dispose();
            }
        }
        return json.toString();
    }

    /**
     * Retrieves all the attributes and values for the given feature
     *
     * @param fid The featureId for which the attributes and values must be
     * retrieved.
     * @param gegevensBronId The id for the Gegevensbron in which the feature
     * which
     * @param fid resides
     * @return Returns the JSONObject with all the attributes and values
     * belonging to the feature with the following format: * { success: boolean,
     * features: (optional if success == true) Array of JSONObjects, following
     * the format:
     *
     * featurename: String, gegevensbronId: Integer, values: Array of editable
     * columns in the format { columnname: String, label: String, units: String,
     * defaultValues: (optional) Array of defaults, type: String with the
     * datatype of the column }, readonly: Array of non-editable columns in the
     * format { columnname: String, label: String, units: String, defaultValues:
     * (optional) Array of defaults, type: String with the datatype of the
     * column }, geom_attribute : (optional if editable) String, geom :
     * (optional if editable and known) String of Well Known Text }
     */
    public String getFeature(String fid, String gegevensBronId) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(SUCCESS_PARAM, false);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        org.hibernate.Transaction tx = null;
        try {
            tx = sess.beginTransaction();

            Integer gbIntId = Integer.valueOf(gegevensBronId);
            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gbIntId);

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            Filter filter = ff.equals(ff.property(gb.getAdmin_pk()), ff.literal(fid));
            json = getEditableFeatureAttributes(filter, gb);

            tx.commit();
            json.put(SUCCESS_PARAM, true);
        } catch (Exception ex) {
            log.error("Exception while getting the features in EditFeature.java:", ex);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            json.put("message", ex.getMessage());
        }
        return json.toString();
    }

    /**
     * Get a JSONObject for a given Filter and Gegevensbron
     *
     * @param filter The Filter describing which features must be retrieved
     * @param gb The Gegevensbron from which the features must be retrieved
     * @return the following JSONObject: * { success: boolean, features:
     * (optional if success == true) Array of JSONObjects, following the format:
     *
     * featurename: String, gegevensbronId: Integer, values: Array of editable
     * columns in the format { columnname: String, label: String, units: String,
     * defaultValues: (optional) Array of defaults, type: String with the
     * datatype of the column }, readonly: Array of non-editable columns in the
     * format { columnname: String, label: String, units: String, defaultValues:
     * (optional) Array of defaults, type: String with the datatype of the
     * column }, geom_attribute : (optional if editable) String, geom :
     * (optional if editable and known) String of Well Known Text }
     * @throws Exception
     */
    private JSONObject getEditableFeatureAttributes(Filter filter, Gegevensbron gb) throws Exception, JSONException {
        JSONObject json = new JSONObject();
        FeatureSource fs = null;
        FeatureCollection fc = null;
        DataStore ds = null;
        Iterator featureIt = null;
        try {
            Bron b = gb.getBron();
            ds = b.toDatastore();
            if (!(ds instanceof JDBCDataStore)) {
                throw new Exception("DataStore not of type JDBCDataStore.");
            }

            Set<ThemaData> themadata = gb.getThemaData();
            List<ThemaData> editables = new ArrayList<ThemaData>();
            List<ThemaData> readonlyAttributes = new ArrayList<ThemaData>();
            for (Iterator<ThemaData> it = themadata.iterator(); it.hasNext();) {
                ThemaData themaData = it.next();
                if (themaData.isEditable()) {
                    editables.add(themaData);
                } else {
                    if (themaData.isBasisregel() && (!themaData.getDataType().getNaam().equals("Javascript functie"))) {
                        readonlyAttributes.add(themaData);
                    }
                }
            }
            fs = ds.getFeatureSource(gb.getAdmin_tabel());
            fc = fs.getFeatures(filter);

            List<Point> multiPoints = new ArrayList<Point>();
            featureIt = fc.iterator();
            JSONArray features = new JSONArray();
            while (featureIt.hasNext()) {
                JSONObject feat = new JSONObject();
                SimpleFeature sf = (SimpleFeature) featureIt.next();
                feat.put("featurename", gb.getNaam());
                feat.put("gegevensbronId", gb.getId());
                JSONArray vals = new JSONArray();
                for (Iterator<ThemaData> it1 = editables.iterator(); it1.hasNext();) {
                    ThemaData themaData = it1.next();
                    Object attr = sf.getAttribute(themaData.getKolomnaam());
                    JSONObject obj = themaData.toJSON();
                    obj.put("value", attr);
                    if (attr != null) {
                        obj.put("type", attr.getClass().getSimpleName());
                    } else {
                        obj.put("type", "String");
                    }
                    vals.put(obj);
                }
                feat.put("values", vals);

                JSONArray readOnlyAttrs = new JSONArray();
                for (Iterator<ThemaData> it1 = readonlyAttributes.iterator(); it1.hasNext();) {
                    ThemaData themaData = it1.next();
                    JSONObject obj = themaData.toJSON();
                    if (themaData.getKolomnaam() != null) {
                        Object attr = sf.getAttribute(themaData.getKolomnaam());
                        obj.put("value", attr);
                    }
                    readOnlyAttrs.put(obj);
                }
                feat.put("readonly", readOnlyAttrs);
                Object fid = sf.getAttribute(gb.getAdmin_pk());
                if (fid == null) {
                    throw new Exception("Geen identifier gevonden voor feature");
                } else {
                    feat.put("fid", fid);
                }
                if (gb.isGeometryeditable()) {
                    GeometryAttribute gp = sf.getDefaultGeometryProperty();
                    Geometry geom = (Geometry) gp.getValue();
                    feat.put("geom_attribute", gp.getName().toString());

                    if (geom instanceof MultiPoint) {                        
                        for (int i =0; i < geom.getNumGeometries(); i++) {
                            Point p = (Point) geom.getGeometryN(i);
                            multiPoints.add(p);
                        }
                    } else {
                        feat.put("geom", geom.toText());
                    }

                    feat.put("geom_type", getGeomType(gp.getType().getBinding()));
                }
                
                if (multiPoints != null && multiPoints.size() > 0) {
                    for (Point p : multiPoints) {
                        feat.put("geom", p.toText());
                        features.put(feat);
                    }
                } else {
                    features.put(feat);
                }
            }
            
            json.put("features", features);

        } catch (IOException ex) {
            log.error("Fout bij ophalen attribuutinfo: ", ex);
            throw ex;
        } finally {
            if (ds != null) {
                ds.dispose();
            }
            fc.close(featureIt);
        }
        return json;
    }

    /**
     * Edits an existing feature or adds a new one
     *
     * @param json The details of the feature which must be editted: consists of
     * a jsonobject in the form: { fid: fid, gegevensbronId: gbId, geom:
     * geometry, mode: String [new|edit], columns :{ col1: val1, col2: val2 } }
     * @return Boolean indicating whether the operation succeeded or not
     * @throws JSONException
     */
    public Boolean editFeature(String json) throws JSONException {
        JSONObject feat = new JSONObject(json);

        String fid = feat.getString("fid");
        Integer gbId = feat.getInt("gegevensbronId");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        org.hibernate.Transaction tx = null;
        Transaction transaction = new DefaultTransaction("edit");
        DataStore ds = null;
        try {
            tx = sess.beginTransaction();

            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gbId);

            Bron b = gb.getBron();
            ds = b.toDatastore();
            if (!(ds instanceof JDBCDataStore)) {
                throw new Exception("DataStore not of type JDBCDataStore, exiting");
            }
            FeatureStore fs = (FeatureStore) ds.getFeatureSource(gb.getAdmin_tabel());
            fs.setTransaction(transaction);

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            Filter filter = ff.equals(ff.property(gb.getAdmin_pk()), ff.literal(fid));

            JSONObject attributes = feat.getJSONObject("columns");
            JSONArray names = attributes.names();

            List values = new ArrayList();
            List<AttributeDescriptor> columns = new ArrayList<AttributeDescriptor>();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String column = (String) names.get(i);
                    Object value = attributes.get(column);
                    AttributeDescriptor ad = (AttributeDescriptor) fs.getSchema().getDescriptor(column);

                    if (ad != null) {
                        columns.add(ad);
                        values.add(value);
                    }
                }
                Geometry g = null;
                if (feat.has("geometry_attribute") && feat.get("geometry_attribute") != null && feat.has("geom") && feat.get("geom") != null && gb.isGeometryeditable()) {
                    Object geom_attr = feat.get("geometry_attribute");
                    Object geom = feat.get("geom");
                    WKTReader reader = new WKTReader();
                    g = reader.read((String) geom);

                    AttributeDescriptor ad = (AttributeDescriptor) fs.getSchema().getDescriptor((String) geom_attr);

                    if (ad.getType().getBinding() == MultiPoint.class) {
                        int num = g.getNumGeometries();
                        if (num == 1) {
                            GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
                            Point[] points = new Point[] {(Point)g};
                            g = (MultiPoint) factory.createMultiPoint(points);
                        }
                    }

                    values.add(g);
                    columns.add(ad);

                }
                String mode = feat.getString("mode");
                if (mode.equals("edit")) {
                    fs.modifyFeatures(columns.toArray(new AttributeDescriptor[columns.size()]), values.toArray(new Object[values.size()]), filter);
                } else if (mode.equals("new")) {
                    SimpleFeature f = DataUtilities.template((SimpleFeatureType) fs.getSchema());
                    for (int i = 0; i < columns.size(); i++) {
                        AttributeDescriptor ad = columns.get(i);
                        Object v = values.get(i);
                        f.setAttribute(ad.getName(), v);
                    }

                    if (g != null) {
                        f.setDefaultGeometry(g);
                    }
                    fs.addFeatures(DataUtilities.collection(f));
                }

            }

            tx.commit();
            transaction.commit();
        } catch (Exception ex) {
            log.error("Exception while getting the features in EditFeature.java:", ex);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            try {
                transaction.rollback();
            } catch (IOException e) {
                log.error("Can't rollback geotools transaction:", e);
                return false;
            }
            return false;
        } finally {
            try {
                transaction.close();
                ds.dispose();
            } catch (IOException ex) {
                log.error("Can't close geotools transaction:", ex);
            }
        }
        return true;
    }

    /**
     * Remove a feature
     *
     * @param json The identification of the feature(s) to be deleted. Use the
     * format: { fid: the featureId, gegevensbronId: id (integer) of the
     * gegevensbron from which the feature must be deleted }
     * @return
     * @throws JSONException
     */
    public Boolean removeFeature(String json) throws JSONException {
        JSONObject feat = new JSONObject(json);

        String fid = feat.getString("fid");
        Integer gbId = feat.getInt("gegevensbronId");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        org.hibernate.Transaction tx = null;
        Transaction transaction = new DefaultTransaction("edit");
        DataStore ds = null;
        try {
            tx = sess.beginTransaction();

            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gbId);

            Bron b = gb.getBron();
            ds = b.toDatastore();
            if (!(ds instanceof JDBCDataStore)) {
                throw new Exception("DataStore not of type JDBCDataStore, exiting");
            }
            FeatureStore fs = (FeatureStore) ds.getFeatureSource(gb.getAdmin_tabel());
            fs.setTransaction(transaction);

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            Filter filter = ff.equals(ff.property(gb.getAdmin_pk()), ff.literal(fid));
            fs.removeFeatures(filter);

            tx.commit();
            transaction.commit();
        } catch (Exception ex) {
            log.error("Exception while getting the features in EditFeature.java:", ex);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            try {
                transaction.rollback();
            } catch (IOException e) {
                log.error("Can't rollback geotools transaction:", e);
                return false;
            }
            return false;
        } finally {
            try {
                transaction.close();
                ds.dispose();
            } catch (IOException ex) {
                log.error("Can't close geotools transaction:", ex);
            }
        }
        return true;
    }

    /**
     * Get the geometry type given a class
     *
     * @param clazz
     * @return Polygon | LineString | Point
     */
    private String getGeomType(Class clazz) {
        String type = "";

        if (MultiPolygon.class == clazz || Polygon.class == clazz) {
            type = "Polygon";
        } else if (MultiPoint.class == clazz || Point.class == clazz) {
            type = "Point";
        } else if (MultiLineString.class == clazz || LineString.class == clazz) {
            type = "LineString";
        }
        return type;
    }
}
