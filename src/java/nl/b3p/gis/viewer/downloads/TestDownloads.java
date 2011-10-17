package nl.b3p.gis.viewer.downloads;

import com.vividsolutions.jts.geom.MultiPoint;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

/**
 *
 * @author Boy
 */
public class TestDownloads {
    private static final String SHP = "SHP";
    private static final String GML = "GML";
    private static final String EXTENSION_SHAPE = ".shp";
    private static final String EXTENSION_ZIP = ".shp";
    private static final String FORMAAT = "SHP";

    private static final int maxFileNameLength = 50;

    private static Random rg = null;

    private static final Log log = LogFactory.getLog(TestDownloads.class);

    public void test() throws Exception {

        /* Create a unique name for the zipfile */
        String zipfileName = uniqueName(EXTENSION_SHAPE);

        /* Create subfolder to put zipfile in */
        File workingDir = createUniqueFile("uuids", 0);
        if (!workingDir.mkdir()) {
            throw new IOException("Can't create directory: " + workingDir.getAbsolutePath());
        }

        String workingPathDataset = workingDir.getAbsolutePath() + File.separator + zipfileName + File.separator;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, new Integer(4));
        DataStore datastore = gb.getBron().toDatastore();        

        FeatureCollection fc = null;
        FeatureIterator it = null;
        org.geotools.data.Transaction transaction = null;

        try {
            FeatureStore fs = (FeatureStore) datastore.getFeatureSource(gb.getAdmin_tabel());
            fc = fs.getFeatures();

            if (fc != null && fc.size() > 0) {
                File newFile = new File(workingPathDataset);

                ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

                Map<String, Serializable> params = new HashMap<String, Serializable>();
                params.put("url", newFile.toURI().toURL());
                params.put("create spatial index", Boolean.TRUE);

                String tabelNaam = gb.getAdmin_tabel();
                SimpleFeatureType ft = datastore.getSchema(tabelNaam);
                SimpleFeatureType newFt = createFeatureType(ft);

                ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
                newDataStore.createSchema(newFt);

                transaction = new DefaultTransaction("create");
                FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;

                String typeName = newDataStore.getTypeNames()[0];
                featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore.getFeatureSource(typeName);

                featureStore.setTransaction(transaction);

                featureStore.addFeatures(fc);
                transaction.commit();
            }

        } catch (Exception ex) {
            log.error("Fout tijdens maken shape: ", ex);

            if (transaction != null) {
                transaction.rollback();
            }

        } finally {
            if (datastore != null) {
                datastore.dispose();
            }

            if (transaction != null) {
                transaction.close();
            }
        }
    }

    private static String createValidFileName(String name) {
        if (name == null) {
            name = "";
        }
        name = name.replaceAll("[:\\\\/*?|<>\"\']", "");
        if ("".equals(name)) {
            name = "unknown";
        }
        if (name.length() > maxFileNameLength) {
            name = name.substring(0, maxFileNameLength);
        }
        name=name.trim();
        return name;
    }

    private static File createUniqueFile(String path, int count) throws Exception {
        if (count == 100) {
            throw new Exception("Can not create unique File/Directory: " + path);
        }
        File workingDir;
        if (count != 0) {
            if (path.endsWith(File.separator)) {
                String newFileString=path.substring(0, path.length() - 2) + "_" + count + File.separator;
                workingDir = new File(newFileString);
            }else{
                workingDir = new File(path + "_" + count);
            }
        } else {
            workingDir = new File(path);
        }
        if (workingDir.exists()) {
            count++;
            return createUniqueFile(path, count);
        }
        return workingDir;
    }

    private static String uniqueName(String extension) {
        // Gebruik tijd in milliseconden om gerekend naar een radix van 36.
        // Hierdoor ontstaat een lekker korte code.
        long now = (new Date()).getTime();
        String val1 = Long.toString(now, Character.MAX_RADIX).toUpperCase();
        // random nummer er aanplakken om zeker te zijn van unieke code
        if (rg==null) {
            rg = new Random();
        }
        long rnum = (long) rg.nextInt(1000);
        String val2 = Long.toString(rnum, Character.MAX_RADIX).toUpperCase();
        String thePath = "";

        return val1 + val2 + extension;
    }

    private static SimpleFeatureType createFeatureType(SimpleFeatureType schema)
            throws NoSuchAuthorityCodeException, FactoryException {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(schema.getName());
        builder.setCRS(CRS.decode("EPSG:28992"));

        for ( AttributeDescriptor at : schema.getAttributeDescriptors() ) {
             String name = at.getLocalName();
             Class type = at.getType().getBinding();

             if (at instanceof GeometryDescriptor ) {
                 //type = MultiPoint.class;
             }

             builder.add(name,type);
        }
        
        builder.setDefaultGeometry(schema.getGeometryDescriptor().getName().getLocalPart());

        return builder.buildFeatureType();
    }
}
