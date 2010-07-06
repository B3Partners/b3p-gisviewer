/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.geotools;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.b3p.gis.viewer.GetViewerDataAction;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.wfs.v1_0_0.WFS_1_0_0_DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.FilterTransformer;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 17-jun-2010, 17:12:27
 */
public class DataStoreUtil {

    private static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
    private static final Log log = LogFactory.getLog(GetViewerDataAction.class);
    
    public static final int maxFeatures = 1000;
    static{
        Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
    }
        /**
     * Haal de features op van het Thema
     * De 3 mogelijke filters worden gecombineerd als ze gevuld zijn (1: ThemaFilter, 2: ExtraFilter 3: GeometryFilter)
     * LETOP!: De DataStore wordt in deze functie geopend en gesloten. Als je dus al een datastore hebt geopend, gebruik dan de functie
     * waarin je de DataStore mee kan geven.
     * @param t Het thema waarvan de features moeten worden opgehaald
     * @param geom De geometrie waarmee de features moeten intersecten (mag null zijn)
     * @param extraFilter een extra filter dat wordt gebruikt om de features op te halen
     * @param maximum Het maximum aantal features die gereturned moeten worden. (default is geset op 1000)
     *
     */
    public static ArrayList<Feature> getFeatures(Themas t, Geometry geom, Filter extraFilter, Integer maximum) throws IOException, Exception {
        Bron b = t.getConnectie();
        DataStore ds = b.toDatastore();
        try{
            Filter geomFilter = createIntersectFilter(t, ds, geom);
            ArrayList<Filter> filters=new ArrayList();
            if (geomFilter!=null){
                filters.add(geomFilter);
            }if (extraFilter!=null){
                filters.add(extraFilter);
            }
            Filter filter = null;
            if (filters.size()==1)
                filter = filters.get(0);
            else if (filters.size() > 1)
                filter= ff.and(filters);
            return getFeatures(ds,t,filter,maximum);
        }finally{
            ds.dispose();
        }        
    }    
    /**
     * De beste functie om te gebruiken. Open en dispose de DataStore zelf bij het meegeven.
     * Alle filters zijn gecombineerd in Filter f. (geometry filter en extra filter)
     * Het adminfilter wordt automatisch toegevoegd.
     */
    public static ArrayList<Feature> getFeatures(DataStore ds,Themas t, Filter f, Integer maximum) throws IOException, Exception {        
        ArrayList<Filter> filters= new ArrayList();
        Filter adminFilter = getThemaFilter(t);        
        if (adminFilter!=null){
            filters.add(adminFilter);
        }
        if(f!=null){
            filters.add(f);
        }
        Filter filter = null;
        if (filters.size()==1)
            filter = filters.get(0);
        else if (filters.size() > 1)
            filter= ff.and(filters);
        else
           throw new Exception("Geen filter gemaakt. Data wordt niet getoond");

        FeatureSource fs = ds.getFeatureSource(t.getAdmin_tabel());
        FilterTransformer ft= new FilterTransformer();
        String s=ft.transform(filter);
        log.info("Do query with filter: "+s);
        DefaultQuery query = new DefaultQuery(t.getAdmin_tabel(), filter);
        int max;
        if (maximum != null) {
            max = maximum.intValue();
        } else {
            max = maxFeatures;
        }
        if (max > 0)
            query.setMaxFeatures(max);

        FeatureCollection fc = fs.getFeatures(query);
        FeatureIterator fi = fc.features();
        ArrayList<Feature> features = new ArrayList();
        while (fi.hasNext()) {
            features.add(fi.next());
        }
        return features;
    }

    public static Filter createIntersectFilter(Themas t, DataStore ds, Geometry geom) throws Exception {
        if (geom == null) {
            return null;
        }
        String geomAttributeName = getGeometryAttributeName(ds, t);
        if (geomAttributeName == null) {
            log.error("Thema heeft geen geometry");
            return null;
        }
        Filter filter= ff.intersects(ff.property(geomAttributeName), ff.literal(geom));
        if (ds instanceof WFS_1_0_0_DataStore){
            WFS_1_0_0_DataStore wfsDs = (WFS_1_0_0_DataStore)ds;
            //als filter intersect niet wordt ondersteund, probeer het dan met een disjoint.
            if (!wfsDs.getCapabilities().getFilterCapabilities().supports(filter)){
                filter=ff.not(ff.disjoint(ff.property(geomAttributeName), ff.literal(geom)));
            }
            if (!wfsDs.getCapabilities().getFilterCapabilities().supports(filter)){
                if (!(geom instanceof Point)){
                    Envelope env=geom.getEnvelopeInternal();
                    CoordinateReferenceSystem crs = getSchema(ds, t).getGeometryDescriptor().getCoordinateReferenceSystem();
                    ReferencedEnvelope bbox = new ReferencedEnvelope( env.getMinX(),env.getMinY(), env.getMaxX(), env.getMaxY(),crs);
                    filter = ff.bbox(ff.property(geomAttributeName),bbox);
                }
            }
            if (!wfsDs.getCapabilities().getFilterCapabilities().supports(filter)){
                log.info("Intersect,disjoint and bbox filters niet ondersteund. We geven het op: Filter wordt toegepast aan de client kant (java code).");
            }             
        }
        return filter;
    }

    public static boolean isFilterSupported(WFS_1_0_0_DataStore ds, Filter filter) throws IOException{
        return ds.getCapabilities().getFilterCapabilities().fullySupports(filter);
    }

    public static Filter getThemaFilter(Themas t) {
        String adminQuery = t.getAdmin_query();
        if (adminQuery != null && !adminQuery.equals("")) {
            //als er select in de query staat dan dat stukje er afhalen.
            //Alleen het where stukje is nodig voor een cql filter.
            if (adminQuery.toLowerCase().startsWith("select")) {
                int beginIndex = adminQuery.toLowerCase().indexOf(" where ");
                if (beginIndex > 0) {
                    beginIndex += 7;
                    adminQuery = adminQuery.substring(beginIndex).trim();
                    if (adminQuery.indexOf("?") >= 0 && adminQuery.indexOf("?") <= 8) {
                        adminQuery = adminQuery.substring(adminQuery.indexOf("?") + 1).trim();
                    }
                    if (adminQuery.toLowerCase().startsWith("and")) {
                        adminQuery = adminQuery.substring(3).trim();
                    }
                } else {
                    adminQuery = null;
                }
            }
        }
        if (adminQuery != null) {
            try {
                return CQL.toFilter(adminQuery);
            } catch (Exception e) {
                log.error("Fout bij maken van filter: ", e);
            }
        }
        return null;
    }

    //Thema helpers
    public static String getGeometryAttributeName(DataStore ds, Themas t) throws Exception {
        return getSchema(ds, t).getGeometryDescriptor().getName().toString();
    }

    /**
     * Haal het thema schema op van de datastore. Dit is het schema van het feature type dat bij thema
     * als Admin_tabel is ingevuld. zie ook getSchema(DataStore,String);
     */
    public static SimpleFeatureType getSchema(DataStore ds, Themas t) throws Exception {
        return getSchema(ds,t.getAdmin_tabel());
    }
    /**
     * Haalt het schema op van de featureType met de naam: 'featureName'
     * Als het log op DebugEnabled staat dan wordt er in het log ook een lijst met mogelijke schemas getoond.
     */
    public static SimpleFeatureType getSchema(DataStore ds, String featureName) throws Exception {
        try{
            return ds.getSchema(featureName);
        }catch(Exception e){
            if(log.isDebugEnabled()){
                String schemas="Er is een fout opgetreden bij het ophalen van het schema ("+featureName+"). Waarschijnlijk is het schema/featureType niet gevonden. Mogelijke schemas: ";
                String[] typenames=ds.getTypeNames();
                for (int i=0; i < typenames.length; i++){                    
                    schemas+="\n";
                    schemas+=typenames[i];
                }
                log.debug(schemas);
            }
            throw e;
        }
    }
    /**
     * Geeft een lijst met attribute namen van de admin feature van het thema.
     * let op hier wordt een nieuwe DataStore geopend en gesloten! Als je dus al een
     * DataStore geopend hebt gebruik dan getAttributeNames(DataStore,String)! Dit
     * scheelt weer qua performance.
     */
    public static ArrayList<String> getAttributeNames(Themas t) throws Exception{
        if (t.getConnectie()!=null){
            Bron b= t.getConnectie();
            DataStore ds = b.toDatastore();
            try{
                return getAttributeNames(ds,t.getAdmin_tabel());
            }finally{
                ds.dispose();
            }
        }
        return null;
    }
    /**
     * Geeft een lijst terug met String objecten waarin de mogelijke attributeNames staan.
     */
    public static ArrayList<String> getAttributeNames(DataStore ds, String featureName) throws Exception{
        SimpleFeatureType featureType=getSchema(ds, featureName);
        List<AttributeDescriptor> descriptors =featureType.getAttributeDescriptors();
        ArrayList<String> attributen = new ArrayList();
        //maak een lijst met mogelijke attributen en de binding class namen.
        for (int i = 0; i < descriptors.size(); i++) {            
            attributen.add(descriptors.get(i).getName().toString());
        }
        return attributen;
    }    
}
