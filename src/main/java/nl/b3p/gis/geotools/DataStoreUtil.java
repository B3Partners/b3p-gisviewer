package nl.b3p.gis.geotools;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import nl.b3p.gis.viewer.db.DataTypen;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.store.DataFeatureCollection;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.data.wfs.v1_0_0.FeatureSetDescription;
import org.geotools.data.wfs.v1_0_0.WFSCapabilities;
import org.geotools.data.wfs.v1_0_0.WFS_1_0_0_DataStore;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.FilterCapabilities;
import org.geotools.filter.FilterTransformer;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.SchemaFactory;
import org.geotools.xml.schema.Schema;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * B3partners B.V. http://www.b3partners.nl
 *
 * @author Roy Created on 17-jun-2010, 17:12:27
 */
public class DataStoreUtil {

    private static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
    private static final Log log = LogFactory.getLog(DataStoreUtil.class);

    public static final int maxFeatures = 1000;
    public static final int MAX_COORDS_WKT = 250;

    static {
        Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());

        Schema appSchema = APPSchema.getInstance();
        SchemaFactory.registerSchema(appSchema.getURI(), appSchema);

        Schema msSchema = MSSchema.getInstance();
        SchemaFactory.registerSchema(msSchema.getURI(), msSchema);
    }

    /**
     * Haal de features op van het Thema De 3 mogelijke filters worden
     * gecombineerd als ze gevuld zijn (1: ThemaFilter, 2: ExtraFilter 3:
     * GeometryFilter) LETOP!: De DataStore wordt in deze functie geopend en
     * gesloten. Als je dus al een datastore hebt geopend, gebruik dan de
     * functie waarin je de DataStore mee kan geven.
     *
     * @param t Het thema waarvan de features moeten worden opgehaald
     * @param geom De geometrie waarmee de features moeten intersecten (mag null
     * zijn)
     * @param extraFilter een extra filter dat wordt gebruikt om de features op
     * te halen
     * @param maximum Het maximum aantal features die gereturned moeten worden.
     * (default is geset op 1000)
     *
     */
    public static ArrayList<Feature> getFeatures(Bron b, Gegevensbron gb, Geometry geom, Filter extraFilter, List<String> propNames, Integer maximum, boolean collectGeom) throws IOException, Exception {
        DataStore ds = b.toDatastore();

        try {
            Filter geomFilter = createIntersectFilter(gb, ds, geom);

            ArrayList<Filter> filters = new ArrayList();
            if (geomFilter != null) {
                filters.add(geomFilter);
            }
            if (extraFilter != null) {
                filters.add(extraFilter);
            }
            Filter filter = null;
            if (filters.size() == 1) {
                filter = filters.get(0);
            } else if (filters.size() > 1) {
                filter = ff.and(filters);
            }
            return getFeatures(ds, gb, filter, propNames, maximum, collectGeom);
        } finally {
            ds.dispose();
        }
    }

    public static List<Feature> getWfsFeaturesWithGeotools(Gegevensbron gb,
            Geometry geom) {

        List<Feature> features = new ArrayList();
        if (gb == null || gb.getBron() == null) {
            return features;
        }

        try {
            DataStore ds = gb.getBron().toDatastore();
            QName ftName = convertFeatureTypeToQName(gb.getAdmin_tabel(), ds);

            FeatureSource fs = null;
            FeatureIterator fi = null;

            fs = getFeatureSource(ds, ftName);
 
            SimpleFeatureType featureType = (SimpleFeatureType) fs.getSchema();
            if (featureType != null) {
                Filter filter = getBboxFilter(featureType, geom);
                fi = fs.getFeatures(filter).features();

                try {
                    while (fi.hasNext()) {
                        features.add(fi.next());
                    }
                } finally {
                    if (fs != null) {
                        fi.close();
                    }
                }
            }            
        } catch (IOException ioex) {
            log.error("Fout ophalen features.", ioex);
        } catch (Exception ioex) {
            log.error("Fout omzetten datastore.", ioex);
        }

        return features;
    }

    private static Filter getBboxFilter(SimpleFeatureType featureType, Geometry geom) {
        String geometryPropertyName = featureType.getGeometryDescriptor().getLocalName();

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

        CoordinateReferenceSystem targetCRS = featureType.getGeometryDescriptor()
                .getCoordinateReferenceSystem();

        BoundingBox bbox = null;
        try {
            bbox = JTS.toEnvelope(geom).toBounds(targetCRS);
        } catch (TransformException tex) {
            log.error("Fout omzetten naar bbox.", tex);
        }        
        
        return ff.bbox(ff.property(geometryPropertyName), bbox);
    }

    /**
     * De beste functie om te gebruiken. Open en dispose de DataStore zelf bij
     * het meegeven. Alle filters zijn gecombineerd in Filter f. (geometry
     * filter en extra filter) Het adminfilter wordt automatisch toegevoegd.
     */
    public static ArrayList<Feature> getFeatures(DataStore ds, Gegevensbron gb, Filter f, List<String> propNames, Integer maximum, boolean collectGeom) throws IOException, Exception {
        FeatureCollection fc = getFeatureCollection(ds, gb, f, propNames, maximum, collectGeom);
        if (fc==null) {
            return null;
        }
        FeatureIterator fi = fc.features();
        ArrayList<Feature> features = new ArrayList();
        try {
            while (fi.hasNext()) {
                features.add(fi.next());
            }
        } finally {
            if (fc != null) {
                fi.close();
            }
        }
        return features;
    }

    /**
     * Beperkingen bij de Geotools implementatie van WFS 1.0 en 1.1 maken het 
     * onmogelijk om zowel de prefix als de namespace url te bewaren. Deze 
     * methode probeert op basis van de
     * localpart van de qname de oorspronkelijke prefix terug te vinden in 
     * ds.getTypeNames(). Binnen een datastore mag een localpart van een 
     * typename dan maar ��n keer voorkomen. Dit is natuurlijk vanzelf waar 
     * indien er maar ��n namespace wordt gebruikt per datastore, hetgeen 
     * meestal het geval is.
     * Extra hack voor Mapserver: indien de namespace wfs is dan verwacht
     * Mapserver een typename zonder de wfs-prefix
     * @param qname typename met namespace
     * @param ds datastore waarin de typename te vinden is.
     * @return typename waarbij de namespace url is omgezet in een prefix
     * @throws IOException 
     */
    public static String reconstructPrefixedName(QName qname, DataStore ds) throws IOException {
        if (ds instanceof WFS_1_1_0_DataStore
                && qname.getNamespaceURI() != null
                && !qname.getNamespaceURI().isEmpty()
                //Hack: ism Mapserver strategy
                && !qname.getNamespaceURI().equals("http://www.opengis.net/wfs")) {
            
            //find prefix via ds.getTypeNames();
            String[] prefixedNames = ds.getTypeNames();
            for (int i = 0; i < prefixedNames.length; i++) {
                String[] lna = prefixedNames[i].split(":");
                String localName;
                if (lna.length == 2) {
                    localName = lna[1];
                } else {
                    localName = prefixedNames[i];
                }
                if (qname.getLocalPart().equals(localName)) {
                    return prefixedNames[i];
                }
            }
        }

        return qname.getLocalPart();
    }
   
    public static FeatureCollection getFeatureCollection(DataStore ds, Gegevensbron gb, Filter f, List<String> propNames, Integer maximum, boolean collectGeom) throws IOException, Exception {
        if (gb==null || gb.getAdmin_tabel()==null || gb.getAdmin_tabel().isEmpty()) {
            return null;
        }
        
        ArrayList<Filter> filters = new ArrayList();
        if (f != null) {
            filters.add(f);
        }

        Filter adminFilter = null;
        try {
            adminFilter = getThemaFilter(gb);
        } catch (CQLException cqle) {
            if (filters.isEmpty()) {
                String msg = cqle.getLocalizedMessage();
                throw new Exception("Error creating filter: " + msg, cqle);
            }
            log.debug("error creating filter: ", cqle);
        }
        if (adminFilter != null) {
            filters.add(adminFilter);
        }

        Filter filter = null;
        if (filters.size() == 1) {
            filter = filters.get(0);
        } else if (filters.size() > 1) {
            filter = ff.and(filters);
        } else {
            log.debug("No filter found. Using the Filter.INCLUDE (all)");
            filter = Filter.INCLUDE;
        }

        if (log.isDebugEnabled() && filter != null) {
            try {
                FilterTransformer ft = new FilterTransformer();
                String s = ft.transform(filter);
                log.debug("Do query with filter: " + s);
            } catch (Exception e) {
                log.debug("Cannot transform filter: " + filter.toString());
                log.debug("Error transform filter: " + e.getLocalizedMessage());
                if (e.getCause() != null) {
                    log.debug("Cause Error transform filter: " + e.getCause().getLocalizedMessage());
                }
            }
        }

        FeatureSource fs = null;
        QName ftName = convertFeatureTypeToQName(gb.getAdmin_tabel(), ds);
        fs = getFeatureSource(ds, ftName);
 
        DefaultQuery query = null;
        // query.setNamespace(new URI(ftName.getNamespaceURI())); 
        // waarom wordt dit niet gebruikt? slecht van geotools!
        // daarom deze query aanpassen, moet prefixed name zijn
        String prefixedName = reconstructPrefixedName(ftName, ds);
        query = new DefaultQuery(prefixedName, filter);

        int max;
        if (maximum != null) {
            max = maximum.intValue();
        } else {
            max = maxFeatures;
        }
        if (max > 0) {
            query.setMaxFeatures(max);
        }
        
        if (propNames == null) {
            propNames = new ArrayList();
        }

        //zorg er voor dat de pk ook wordt opgehaald
        String adminPk = null;
        String tmpAdminPk = gb.getAdmin_pk();
        if (tmpAdminPk != null) {
            adminPk = DataStoreUtil.convertColumnNameToQName(tmpAdminPk).getLocalPart();
        }

        if (adminPk != null && adminPk.length() > 0 && !propNames.contains(adminPk)) {
            propNames.add(adminPk);
        }

        String adminFk = null;
        String tmpAdminFk = gb.getAdmin_fk();
        if (tmpAdminFk != null) {
            adminFk = DataStoreUtil.convertColumnNameToQName(tmpAdminFk).getLocalPart();
        }

        if (adminFk != null && adminFk.length() > 0 && !propNames.contains(adminFk)) {
            propNames.add(adminFk);
        }

        if (collectGeom) {
            // zorg ervoor dat de geometry wordt opgehaald, indien aanwezig.
            String geomAttributeName = getGeometryAttributeName(ds, gb);
            if (geomAttributeName != null && geomAttributeName.length() > 0 && !propNames.contains(geomAttributeName)) {
                propNames.add(geomAttributeName);
            }
        }

        /*Als een themaDataObject van het type query is en er zitten [] in
         dan moeten deze ook worden opgehaald*/
        Iterator<ThemaData> it = SpatialUtil.getThemaData(gb, false).iterator();
        while (it.hasNext()) {
            ThemaData td = it.next();
            //als de td van het type query is.
            if (td.getDataType() != null && td.getDataType().getId() == DataTypen.QUERY) {
                String commando = td.getCommando();
                //als er in het commando [replaceme] voorkomt
                while (commando != null && commando.indexOf("[") != -1 && commando.indexOf("]") != -1) {
                    //haal alle properties er uit.en stuur deze mee in de query
                    int beginIndex = commando.indexOf("[") + 1;
                    int endIndex = commando.indexOf("]");
                    QName propName = convertColumnNameToQName(commando.substring(beginIndex, endIndex));
                    //geen dubbele meegeven.
                    if (propName != null && !propNames.contains(propName.getLocalPart())) {
                        propNames.add(propName.getLocalPart());
                    }
                    if (endIndex + 1 >= commando.length() - 1) {
                        commando = "";
                    } else {
                        commando = commando.substring(endIndex + 1);
                    }
                }
            }
        }
        if (propNames.size() > 0) {
            /* TODO: Als er een spatie in een Oracle column voorkomt dan gaat
             * getFeatures fout. */
            query.setPropertyNames(propNames);
        }
        
        FeatureCollection fc = null;
        try {
            fc = fs.getFeatures(query);
        } catch (Exception e) {
            //create a helpfull message.
            String message = "FeatureType: " + query.getTypeName() + "\n";
            message += "PropertyNames: ";
            for (int i = 0; i < propNames.size(); i++) {
                if (i != 0) {
                    message += ",";
                }
                message += propNames.get(i);
            }
            log.error(message);
            throw e;
        }
        return fc;
    }

    @Deprecated
    public static Filter createIntersectFilter(Themas t, DataStore ds, Geometry geom) throws Exception {
        String geomAttributeName = getGeometryAttributeName(ds, t);
        if (geomAttributeName == null) {
            log.error("Thema heeft geen geometry");
            throw new Exception("Thema heeft geen geometry");
        }
        CoordinateReferenceSystem crs = getSchema(ds, t)
                .getGeometryDescriptor().getCoordinateReferenceSystem();
        return createIntersectFilter(geomAttributeName, crs, ds, geom);
    }

    public static Filter createIntersectFilter(Gegevensbron gb, DataStore ds, Geometry geom) throws Exception {
        String geomAttributeName = getGeometryAttributeName(ds, gb);
        if (geomAttributeName == null) {
            log.warn("De datastore voor deze gegevensbron heeft geen geometry.");
            return null;
        }
        CoordinateReferenceSystem crs = getSchema(ds, gb).getGeometryDescriptor().getCoordinateReferenceSystem();
        return createIntersectFilter(geomAttributeName, crs, ds, geom);
    }

    public static Filter createIntersectFilter(String geomAttributeName, CoordinateReferenceSystem crs, DataStore ds, Geometry geom) throws Exception {
        if (geom == null) {
            return null;
        }
        Filter filter = ff.intersects(ff.property(geomAttributeName), ff.literal(geom));
        if (ds instanceof WFS_1_0_0_DataStore) {
            WFS_1_0_0_DataStore wfsDs = (WFS_1_0_0_DataStore) ds;
            //als filter intersect niet wordt ondersteund, probeer het dan met een disjoint.
            if (!wfsDs.getCapabilities().getFilterCapabilities().fullySupports(filter)) {
                Filter notFilter = ff.not(ff.disjoint(ff.property(geomAttributeName), ff.literal(geom)));
                Envelope env = geom.getEnvelopeInternal();
                ReferencedEnvelope bbox = new ReferencedEnvelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), crs);
                Filter bboxFilter = ff.bbox(ff.property(geomAttributeName), bbox);
                filter = ff.and(bboxFilter, notFilter);
            }
            if (!wfsDs.getCapabilities().getFilterCapabilities().fullySupports(filter)) {
                if (!(geom instanceof Point)) {
                    Envelope env = geom.getEnvelopeInternal();
                    ReferencedEnvelope bbox = new ReferencedEnvelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), crs);
                    filter = ff.bbox(ff.property(geomAttributeName), bbox);
                }
            }
            if (!wfsDs.getCapabilities().getFilterCapabilities().supports(filter)) {
                log.debug("Intersect,disjoint and bbox filters niet ondersteund. We geven het op: Filter wordt toegepast aan de client kant (java code).");
            }
        }
        if (ds instanceof WFS_1_1_0_DataStore) {
            // TODO
        }
        return filter;
    }

    public static Filter getThemaFilter(Themas t) throws CQLException {
        String adminQuery = t.getGegevensbron().getAdmin_query();
        if ((adminQuery != null) && (adminQuery.length() > 0)) {
            // Als er nog een oude select staat met ? dan maar fout laten gaan
            return CQL.toFilter(adminQuery);
        }
        return null;
    }

    //Thema helpers
    public static String getGeometryAttributeName(DataStore ds, Themas t) throws Exception {
        SimpleFeatureType schema = getSchema(ds, t);
        if (schema == null) {
            return null;
        }
        GeometryDescriptor gd = schema.getGeometryDescriptor();
        if (gd == null) {
            return null;
        }
        return gd.getName().getLocalPart();
    }

    /**
     * Haal het thema schema op van de datastore. Dit is het schema van het
     * feature type dat bij thema als Admin_tabel is ingevuld. zie ook
     * getSchema(DataStore,String);
     */
    public static SimpleFeatureType getSchema(DataStore ds, Themas t) throws Exception {

        return getSchema(ds, new QName(t.getGegevensbron().getAdmin_tabel()));
    }

    /**
     * Haalt het schema op van de featureType met de naam: 'featureName' Als het
     * log op DebugEnabled staat dan wordt er in het log ook een lijst met
     * mogelijke schemas getoond.
     * LET OP 1: voor WFS_1_0_0_Datastore is getSchema(Name) niet geimplementeerd: 
     * altijd null, gebruik getSchema(String).
     * LET OP 2: voor WFS_1_0_0_Datastore is getSchema(String) inclusief test met
     * en zonder prefix, bij GetFeatureSource(String).getSchema(String) zit deze
     * test op prefix niet.
     * LET OP 3: voor WFS_1_1_0_Datasore getSchema(Name) en getFeatureSource(Name)
     * wordt de prefix opgezocht in de datastore en voor de localname geplaatst
     * en daarna wordt gezocht zoals bij de String versie.
     * LET OP 4: mapserver en wfs 1.1 is tricky, in de GetCapability wordt geen
     * prefix en namespace mee gegeven en ieder featuretype krijgt de wfs namespace.
     * Bij DescribeFeatureType geeft mapserver een eigen prefix en namespace mee
     * ms en http://mapserver.gis.umn.edu/mapserver. Als Geotools de juiste mapserver
     * strategy meekrijgt (zie WFSDataStoreFactory.WFS_STRATEGY) dan is er een
     * workaround: bij opvragen van schema zorgen dat de http://www.opengis.net/wfs
     * niet meegegeven wordt.
     */
    public static SimpleFeatureType getSchema(DataStore ds, QName ftName) {
        try {
            if (ds instanceof WFS_1_1_0_DataStore 
                    && ftName.getNamespaceURI()!=null 
                    && !ftName.getNamespaceURI().isEmpty()
                    //Hack: ism Mapserver strategy
                    && !ftName.getNamespaceURI().equals("http://www.opengis.net/wfs")) {
                Name nn = qN2N(ftName);
                SimpleFeatureType sft = ds.getSchema(nn);
                return sft;
            } else {
                String ns = ftName.getLocalPart();
                SimpleFeatureType sft = ds.getSchema(ns);
                return sft;
             }            
        } catch (Exception e) {
            
            // NPE indien schema niet opgehaald kan worden,
            // wij maken er een leeg schema van
            //FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(ftName.getLocalPart());
            SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();

            if (ftName != null) {
                sftb.setName(ftName.getLocalPart());
            }

            return sftb.buildFeatureType();
            //  throw e;
        }
    }

    public static FeatureSource getFeatureSource(DataStore ds, QName ftName) throws IOException {
        if (ds instanceof WFS_1_1_0_DataStore
                && ftName.getNamespaceURI() != null
                && !ftName.getNamespaceURI().isEmpty()
                //Hack: ism Mapserver strategy
                && !ftName.getNamespaceURI().equals("http://www.opengis.net/wfs")) {
            Name nn = qN2N(ftName);
            FeatureSource fs = ds.getFeatureSource(nn);
            return fs;
        } else {
            String ns = ftName.getLocalPart();
            FeatureSource fs = ds.getFeatureSource(ns);
            return fs;
        }
    }
    
    public static boolean isFilterSupported(WFS_1_0_0_DataStore ds, Filter filter) throws IOException {
        return ds.getCapabilities().getFilterCapabilities().fullySupports(filter);
    }

    public static Filter getThemaFilter(Gegevensbron gb) throws CQLException {
        String adminQuery = gb.getAdmin_query();
        if ((adminQuery != null) && (adminQuery.length() > 0)) {
            // Als er nog een oude select staat met ? dan maar fout laten gaan
            return CQL.toFilter(adminQuery);
        }
        return null;
    }

    //Thema helpers
    public static String getGeometryAttributeName(DataStore ds, Gegevensbron gb) throws Exception {
        SimpleFeatureType schema = getSchema(ds, gb);
        if (schema == null) {
            return null;
        }
        GeometryDescriptor gd = schema.getGeometryDescriptor();
        if (gd == null) {
            return null;
        }
        return gd.getName().getLocalPart();
    }

    /**
     * Haal het thema schema op van de datastore. Dit is het schema van het
     * feature type dat bij thema als Admin_tabel is ingevuld. zie ook
     * getSchema(DataStore,String);
     */
    public static SimpleFeatureType getSchema(DataStore ds, Gegevensbron gb) throws Exception {

        if (gb==null || gb.getAdmin_tabel()==null || gb.getAdmin_tabel().isEmpty()) {
            return null;
        }
        return getSchema(ds, convertFeatureTypeToQName(gb.getAdmin_tabel(), ds));
    }

    public static List<QName> getAttributeNames(Bron b, Gegevensbron gb) throws Exception {
        if (b == null) {
            return new ArrayList<QName>();
        }
        DataStore ds = b.toDatastore();
        try {
            return getAttributeNames(ds, convertFeatureTypeToQName(gb.getAdmin_tabel(), ds));
        } finally {
            ds.dispose();
        }
    }

    /**
     * Geeft een lijst terug met String objecten waarin de mogelijke
     * attributeNames staan.
     */
    public static List<QName> getAttributeNames(DataStore ds, QName ftName) throws Exception {
        ArrayList<QName> attributen = new ArrayList();
        try {
            SimpleFeatureType featureType = getSchema(ds, ftName);
            List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
            //maak een lijst met mogelijke attributen en de binding class namen.
            for (int i = 0; i < descriptors.size(); i++) {
                attributen.add(n2Qn(descriptors.get(i).getName()));
            }
        } catch (Exception e) {
            // error reported earlier
        }
        return attributen;
    }

    public static List<String> themaData2PropertyNames(List<ThemaData> themaData) {
        ArrayList<String> propNamesList = new ArrayList();
        for (int i = 0; i < themaData.size(); i++) {
            if (themaData.get(i).getKolomnaam() != null) {
                String prp = convertColumnNameToQName(themaData.get(i).getKolomnaam()).getLocalPart();
                if (!propNamesList.contains(prp)) {
                    propNamesList.add(prp);
                }
            }
        }
        return propNamesList;
    }

    public static List<String> basisRegelThemaData2PropertyNames(List<ThemaData> themaData) {
        ArrayList<String> propNamesList = new ArrayList();
        for (int i = 0; i < themaData.size(); i++) {
            if (themaData.get(i).getKolomnaam() != null && themaData.get(i).isBasisregel()) {
                String prp = convertColumnNameToQName(themaData.get(i).getKolomnaam()).getLocalPart();
                if (!propNamesList.contains(prp)) {
                    propNamesList.add(prp);
                }
            }
        }
        return propNamesList;
    }

    public static QName[] getTypeNames(DataStore ds) throws IOException {
        List<Name> names = ds.getNames();
        QName[] typeNames = null;
        if (names == null || names.isEmpty()) {
            //find via 
            String[] localNames = ds.getTypeNames();
            typeNames = new QName[localNames.length];
            for (int i = 0; i < localNames.length; i++) {
                QName qname = new QName(localNames[i]);
                typeNames[i] = qname;
            }
        } else {
            typeNames = new QName[names.size()];
            for (int i = 0; i < names.size(); i++) {
                QName qname = n2Qn(names.get(i));
                typeNames[i] = qname;
            }
        }
        return typeNames;
    }

    /**
     * DOCUMENT ME!
     *
     * @param t Themas
     * @param conn Connection
     *
     * @return int
     *
     */
    static public QName getThemaGeomName(Themas t, GisPrincipal user) throws IOException, Exception {
        Gegevensbron gb = t.getGegevensbron();
        Bron b = null;

        if (gb != null) {
            b = gb.getBron(user);
        }

        if (b == null) {
            return null;
        }

        DataStore ds = b.toDatastore();
        QName n = DataStoreUtil.convertFeatureTypeToQName(gb.getAdmin_tabel(), ds);
        if (n == null || n.getLocalPart() == null) {
            return null;
        }

        try {
            SimpleFeatureType sft = getSchema(ds, n);
            if (sft.getGeometryDescriptor() != null) {
                return n2Qn(sft.getGeometryDescriptor().getName());
            }
        } finally {
            ds.dispose();
        }
        return null;
    }

    static public QName getThemaGeomName(Gegevensbron gb, GisPrincipal user) throws IOException, Exception {
        Bron b = gb.getBron();
        if (b == null) {
            return null;
        }
        
        DataStore ds = b.toDatastore();
        QName n = convertFeatureTypeToQName(gb.getAdmin_tabel(),ds);

        if (n == null || n.getLocalPart() == null) {
            return null;
        }

        try {
            SimpleFeatureType sft = getSchema(ds, n);
            if (sft.getGeometryDescriptor() != null) {
                return n2Qn(sft.getGeometryDescriptor().getName());
            }
        } finally {
            ds.dispose();
        }
        return null;
    }

    static public String getThemaGeomType(Themas t, GisPrincipal user) throws IOException, Exception {
        Gegevensbron gb = t.getGegevensbron();
        Bron b = null;

        if (gb != null) {
            b = gb.getBron(user);
        }

        if (b == null) {
            return null;
        }

        DataStore ds = b.toDatastore();
        QName n = convertFeatureTypeToQName(gb.getAdmin_tabel(),ds);
        if (n == null || n.getLocalPart() == null) {
            return null;
        }

        try {
            SimpleFeatureType sft = getSchema(ds, n);
            if (sft.getGeometryDescriptor() != null) {
                return sft.getGeometryDescriptor().getType().toString();
            }
        } finally {
            ds.dispose();
        }
        return null;
    }

    /**
     * Wordt nu niet gebruikt, maar dit is wel de best plaats voor later
     *
     * @param params
     * @return
     * @throws IOException
     */
    public static DataStore createDataStoreFromParams(Map params) throws IOException, Exception {
        return Bron.createDataStoreFromParams(params);
    }
    
    public static Name qN2N(QName qn) {
        if (qn==null) {
            return new NameImpl("");
        }
        return new NameImpl(qn.getNamespaceURI(), qn.getLocalPart());
    }
    
    public static QName n2Qn(Name n) {
        if (n==null) {
            return new QName("");
        }
        return new QName(n.getNamespaceURI(), n.getLocalPart());
    }
    
    public static String convertQNameToFullname(QName qn) {
        String returnValue = "";
        String nsu = qn.getNamespaceURI();
        if (nsu != null && nsu.length() != 0) {
            returnValue = "{" + nsu + "}";
        }
        returnValue += qn.getLocalPart();
        return returnValue;
    }

    public static QName convertFeatureTypeToQName(String ln, DataStore ds) throws IOException, Exception {
        if (ln == null  || ln.isEmpty()) {
            throw new Exception("FeatureType name may not be null or empty");
        }
        String localName = ln;
        String nsPrefix = "";
        String nsUriString = "";
        String[] temp = ln.split("}");
        if (temp.length > 1) {
            localName = temp[1];
            int index1 = ln.indexOf("{");
            int index2 = ln.indexOf("}");
            nsUriString = ln.substring(index1 + 1, index2);
            Schema schema = SchemaFactory.getInstance(nsUriString);
            nsPrefix = (schema == null ? "ns" + nsUriString.hashCode() : schema.getPrefix());
        }
        QName convName = new QName(nsUriString, localName, nsPrefix);
      
        List<Name> names = ds.getNames();
        String[] localNames = ds.getTypeNames();
        if (names == null || names.isEmpty()) {
            for (int i = 0; i < localNames.length; i++) {
                if (localNames[i].equals(convName.getLocalPart())) {
                    return convName;
                }
                if (localNames[i].equalsIgnoreCase(convName.getLocalPart())) {
                    log.debug("Ignore case for Oracle");
                    return convName;
                }
            }
        } else {
             for (Name name : names){
                String nameUri = name.getNamespaceURI();
                if (nameUri == null) {
                    nameUri = "";
                }
                if (convName.getNamespaceURI().equals(nameUri)) {
                    if (convName.getLocalPart().equals(name.getLocalPart())) {
                        return n2Qn(name);
                    }
                    if (convName.getLocalPart().equalsIgnoreCase(name.getLocalPart())) {
                        log.debug("Ignore case for Oracle");
                        return n2Qn(name);
                    }
                }
            }
        }
        
        StringBuilder sb = new StringBuilder("Typename not found in datastore, ");
        sb.append("looking for typename: ");
        sb.append(ln);
        sb.append(", found typenames: ");
            for (int i = 0; i < localNames.length; i++) {
                if (i!=0) {
                    sb.append(", ");
                }
                sb.append(localNames[i]);
            }
        sb.append("!");
       
        throw new Exception(sb.toString());
    }
 
    public static QName convertColumnNameToQName(String ln) {
        if (ln == null) {
            return null;
        }
        String localName = ln;
        String nsPrefix = "";
        String nsUriString = "";
        String[] temp = ln.split("}");
        if (temp.length > 1) {
            localName = temp[1];
            int index1 = ln.indexOf("{");
            int index2 = ln.indexOf("}");
            nsUriString = ln.substring(index1 + 1, index2);
            Schema schema = SchemaFactory.getInstance(nsUriString);
            nsPrefix = (schema == null ? "ns" + nsUriString.hashCode() : schema.getPrefix());
        } else {
            String[] lna = ln.split(":");
            if (lna.length > 1) {
                localName = lna[1];
                nsPrefix = lna[0];
                URI nsUri = null;
                Schema[] schemas = SchemaFactory.getSchemas(nsPrefix);
                if (schemas.length > 0) {
                    nsUri = schemas[0].getTargetNamespace();
                } else {
                    try {
                        nsUri = new URI("http://www.kaartenbalie.nl/"+nsPrefix);
                    } catch (URISyntaxException ex) {
                        log.debug(ex);
                    }
                }
                nsUriString = (nsUri == null ? "" : nsUri.toString());
            }
        }
        return new QName(nsUriString, localName, nsPrefix);
    }

    public static GeometryType getGeometryType(Feature f) {
        if (f == null || f.getDefaultGeometryProperty() == null) {
            return null;
        }
        Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
        Name name = null;
        Class binding = null;
        if (geom instanceof MultiPolygon) {
            name = new NameImpl("MULTIPOLYGON");
            binding = MultiPolygon.class;
        } else if (geom instanceof Polygon) {
            name = new NameImpl("POLYGON");
            binding = Polygon.class;
        } else if (geom instanceof MultiLineString) {
            name = new NameImpl("MULTILINESTRING");
            binding = MultiLineString.class;
        } else if (geom instanceof LineString) {
            name = new NameImpl("LINESTRING");
            binding = LineString.class;
        } else if (geom instanceof MultiPoint) {
            name = new NameImpl("MULTIPOINT");
            binding = MultiPoint.class;
        } else if (geom instanceof Point) {
            name = new NameImpl("POINT");
            binding = Point.class;
        } else {
            name = new NameImpl("GEOMETRY");
            binding = geom.getClass();
        }
        return new GeometryTypeImpl(name, binding, null, true, false, null, null, null);
    }

    public static ReferencedEnvelope convertFeature2Envelop(Feature f) {
        if (f == null || f.getDefaultGeometryProperty() == null) {
            return null;
        }
        Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
        if (geom != null && geom.isSimple() && geom.isValid()) {
            if (!(geom instanceof Point)) {
                Envelope env = geom.getEnvelopeInternal();
                CoordinateReferenceSystem crs = f.getDefaultGeometryProperty().getDescriptor().getCoordinateReferenceSystem();
                return new ReferencedEnvelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), crs);
            } else if ((geom instanceof Point)) {
                Envelope env = geom.getEnvelopeInternal();
                Double minX = env.getMinX() - 10;
                Double minY = env.getMinY() + 10;
                Double maxX = env.getMaxX() + 10;
                Double maxY = env.getMaxY() - 10;

                CoordinateReferenceSystem crs = f.getDefaultGeometryProperty().getDescriptor().getCoordinateReferenceSystem();
                return new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
            }
        }
        return null;
    }

    public static String convertFeature2WKT(Feature f, boolean fallback) {
        if (f == null || f.getDefaultGeometryProperty() == null) {
            return null;
        }
        Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
        if (geom != null && geom.isSimple() && geom.isValid()) {
            if (geom.getCoordinates() != null && geom.getCoordinates().length > MAX_COORDS_WKT && fallback) {
                geom = geom.getEnvelope();
            }
            WKTWriter wktw = new WKTWriter();
            return wktw.write(geom);
        }
        return null;
    }

    public static String selecteerKaartObjectWkt(Feature f) {
        if (f == null || f.getDefaultGeometryProperty() == null) {
            return null;
        }

        Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
        if (geom != null) {
            WKTWriter wktw = new WKTWriter();
            return wktw.write(geom);
        }

        return null;
    }

    public static Geometry createGeomFromWKTString(String wktstring) throws Exception {
        WKTReader wktreader = new WKTReader(new GeometryFactory(new PrecisionModel(), 28992));
        try {
            return wktreader.read(wktstring);
        } catch (ParseException ex) {
            throw new Exception(ex);
        }

    }

    static public void main(String[] args) throws URISyntaxException, IOException, Exception {
        HashMap params = new HashMap();
        String url = "http://localhost:8084/kaartenbalie/services/?SERVICE=WFS&VERSION=1.0.0&REQUEST=GetCapabilities";
        params.put(WFSDataStoreFactory.URL.key, url);
        params.put(WFSDataStoreFactory.TIMEOUT.key, 60000);
        params.put(WFSDataStoreFactory.USERNAME.key, "Beheerder");
        params.put(WFSDataStoreFactory.PASSWORD.key, "*****");
        DataStore ds = DataStoreFinder.getDataStore(params);

        /*omdat de WFS_1_0_0_Datastore niet met de opengis filters werkt even toevoegen dat
         er simpelle vergelijkingen kunnen worden gedaan. (de meeste servers kunnen dit natuurlijk);*/
        if (ds instanceof WFS_1_0_0_DataStore) {
            WFS_1_0_0_DataStore wfs100ds = (WFS_1_0_0_DataStore) ds;
            WFSCapabilities wfscap = wfs100ds.getCapabilities();
            // wfs 1.0.0 haalt prefix er niet af en zet de namespace niet
            // wfs 1.1.0 doet dit wel en hier fixen we dit.
            List<FeatureSetDescription> fdsl = wfscap.getFeatureTypes();
            for (FeatureSetDescription fds : fdsl) {
                if (fds.getNamespace() != null) {
                    continue;
                }
                QName qname = convertFeatureTypeToQName(fds.getName(), ds);
                fds.setName(qname.getLocalPart());
                fds.setNamespace(new URI(qname.getNamespaceURI()));
            }
            FilterCapabilities filterCap = wfscap.getFilterCapabilities();
            filterCap.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
            boolean b = filterCap.supports(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
            wfscap.setFilterCapabilities(filterCap);
        }

        String prefixedFTName = "demowfs_rivieren_nl";
//        String prefixedFTName = "app:roowfs_Bestemmingsplangebied";
        QName ftName = convertFeatureTypeToQName(prefixedFTName, ds);

        DefaultQuery query = null;
        SimpleFeatureType sft = null;
        FeatureSource fs = null;

        sft = getSchema(ds, ftName);
        fs = getFeatureSource(ds, ftName); // hierkomt een FeatureSource uit met namespace
 
            
        String geomAttributeName = sft.getGeometryDescriptor().getLocalName();
        CoordinateReferenceSystem crs = sft.getGeometryDescriptor().getCoordinateReferenceSystem();

        ReferencedEnvelope bbox = new ReferencedEnvelope(201000, 380000, 202000, 381000, crs);
        Filter filter = ff.bbox(ff.property(geomAttributeName), bbox);

        // deze query moet passen bij feature source dus moet prefixed name zijn
        String prefixedName = ftName.getPrefix() + ":" + ftName.getLocalPart();
        query = new DefaultQuery(prefixedName, filter);
        query.setNamespace(new URI(ftName.getNamespaceURI())); // wordt niet gebruikt
 
        query.setMaxFeatures(3);
        List<String> propNames = new ArrayList<String>();
        propNames.add(geomAttributeName);
        query.setPropertyNames(propNames);

        FeatureCollection fc = fs.getFeatures(query);

        FeatureIterator fi = fc.features();
        ArrayList<Feature> features = new ArrayList();
        while (fi.hasNext()) {
            features.add(fi.next());
            System.out.println("feature");
        }

        boolean test2 = false;
        if (test2) {
            String wfsCapabilitiesRawData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><WFS_Capabilities xmlns=\"http://www.opengis.net/wfs\" xmlns:app=\"http://www.deegree.org/app\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengeospatial.net/wfs/1.0.0/WFS-capabilities.xsd\" version=\"1.0.0\" updateSequence=\"0\"><Service><Name>Ruimtelijke ordeningsplannen (RO-Online WFS)</Name><Title>RO-Online: De landelijke voorziening voor digitale ruimtelijke ordeningsplannen</Title><Keywords>ruimtelijke ordening planologie bestemmingsplannen structuurvisies AMvB provinciale verordeningen wet ruimtelijke ordening besluit ruimtelijke ordening IMRO</Keywords><OnlineResource xsi:type=\"java:java.lang.String\">http://localhost:8084/kaartenbalie/services/</OnlineResource><Fees>none</Fees><AccessConstraints>NONE</AccessConstraints></Service><Capability><Request><GetFeature><ResultFormat><GML2/></ResultFormat><DCPType><HTTP><Get onlineResource=\"http://localhost:8084/kaartenbalie/services/\"/><Post onlineResource=\"http://localhost:8084/kaartenbalie/services/\"/></HTTP></DCPType></GetFeature><DescribeFeatureType><SchemaDescriptionLanguage><XMLSCHEMA/></SchemaDescriptionLanguage><DCPType><HTTP><Get onlineResource=\"http://localhost:8084/kaartenbalie/services/\"/><Post onlineResource=\"http://localhost:8084/kaartenbalie/services/\"/></HTTP></DCPType></DescribeFeatureType><GetCapabilities><DCPType><HTTP><Get onlineResource=\"http://localhost:8084/kaartenbalie/services/\"/><Post onlineResource=\"http://localhost:8084/kaartenbalie/services/\"/></HTTP></DCPType></GetCapabilities></Request></Capability><FeatureTypeList><FeatureType><Name>app:roowfs_Gebiedsaanduiding</Name><Title>app:Gebiedsaanduiding</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_ProvinciaalComplex</Name><Title>app:ProvinciaalComplex</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_ProvinciaalVerbinding</Name><Title>app:ProvinciaalVerbinding</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisiecomplex_R</Name><Title>app:Structuurvisiecomplex_R</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisiecomplex_P</Name><Title>app:Structuurvisiecomplex_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Plangebied</Name><Title>app:Plangebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Enkelbestemming</Name><Title>app:Enkelbestemming</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitgebied_P</Name><Title>app:Besluitgebied_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_NationaalGebied</Name><Title>app:NationaalGebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Figuur</Name><Title>app:Figuur</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitgebied_X</Name><Title>app:Besluitgebied_X</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Plangebied_PCP</Name><Title>app:Plangebied_PCP</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitvlak_A</Name><Title>app:Besluitvlak_A</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_ProvinciaalPlangebied</Name><Title>app:ProvinciaalPlangebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisiecomplex_G</Name><Title>app:Structuurvisiecomplex_G</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Dubbelbestemming</Name><Title>app:Dubbelbestemming</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitgebied</Name><Title>app:Besluitgebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_PlangebiedDigitaalWaarmerk</Name><Title>app:PlangebiedDigitaalWaarmerk</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitsubvlak_X</Name><Title>app:Besluitsubvlak_X</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisiegebied_G</Name><Title>app:Structuurvisiegebied_G</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Bouwvlak</Name><Title>app:Bouwvlak</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Bestemmingsplangebied</Name><Title>app:Bestemmingsplangebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitvlak_X</Name><Title>app:Besluitvlak_X</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisiegebied_R</Name><Title>app:Structuurvisiegebied_R</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisiegebied_P</Name><Title>app:Structuurvisiegebied_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitsubvlak_P</Name><Title>app:Besluitsubvlak_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_NationaalVerbinding</Name><Title>app:NationaalVerbinding</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_OnderdelenDigitaalWaarmerk</Name><Title>app:OnderdelenDigitaalWaarmerk</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Onthoudingsgebied</Name><Title>app:Onthoudingsgebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Functieaanduiding</Name><Title>app:Functieaanduiding</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisieverklaring_P</Name><Title>app:Structuurvisieverklaring_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitvlak_P</Name><Title>app:Besluitvlak_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_NationaalComplex</Name><Title>app:NationaalComplex</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Maatvoering</Name><Title>app:Maatvoering</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_NationaalPlangebied</Name><Title>app:NationaalPlangebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Bouwaanduiding</Name><Title>app:Bouwaanduiding</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitsubvlak_A</Name><Title>app:Besluitsubvlak_A</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisieplangebied_G</Name><Title>app:Structuurvisieplangebied_G</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Lettertekenaanduiding</Name><Title>app:Lettertekenaanduiding</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisieplangebied_R</Name><Title>app:Structuurvisieplangebied_R</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_ProvinciaalGebied</Name><Title>app:ProvinciaalGebied</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Structuurvisieplangebied_P</Name><Title>app:Structuurvisieplangebied_P</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>app:roowfs_Besluitgebied_A</Name><Title>app:Besluitgebied_A</Title><SRS>urn:ogc:def:crs:EPSG::28992</SRS><LatLongBoundingBox minx=\"-180.0\" miny=\"-90.0\" maxx=\"180.0\" maxy=\"90.0\"/><MetadataURL type=\"TC211\" format=\"XML\">http://afnemers.ruimtelijkeplannen.nl:80/afnemers/metadata.xml</MetadataURL></FeatureType><FeatureType><Name>demowfs_bebouwdekom_nl</Name><Title>bebouwdekom_nl</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"3.22989\" miny=\"50.709\" maxx=\"7.27394\" maxy=\"53.5672\"/></FeatureType><FeatureType><Name>demowfs_bebouwdekom nl</Name><Title>bebouwdekom nl</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"3.22989\" miny=\"50.709\" maxx=\"7.27394\" maxy=\"53.5672\"/></FeatureType><FeatureType><Name>demowfs_basis_nl</Name><Title>basis_nl</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_autowegen_nl</Name><Title>autowegen_nl</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_autowegen_elabels</Name><Title>autowegen_elabels</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_rivieren_nl</Name><Title>rivieren_nl</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_gemeenten_2006</Name><Title>gemeenten_2006</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"3.22989\" miny=\"50.709\" maxx=\"7.27394\" maxy=\"53.5672\"/></FeatureType><FeatureType><Name>demowfs_wijken_2006</Name><Title>wijken_2006</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_buurten_2006</Name><Title>buurten_2006</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_plan_lijnen</Name><Title>plan_lijnen</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType><FeatureType><Name>demowfs_plan_polygonen</Name><Title>plan_polygonen</Title><SRS>EPSG:28992</SRS><LatLongBoundingBox minx=\"-179.156\" miny=\"-74.7705\" maxx=\"179.909\" maxy=\"2.64457\"/></FeatureType></FeatureTypeList><ogc:Filter_Capabilities><ogc:Spatial_Capabilities><ogc:Spatial_Operators><ogc:BBOX/></ogc:Spatial_Operators></ogc:Spatial_Capabilities><ogc:Scalar_Capabilities><ogc:Comparison_Operators><ogc:Simple_Comparisons/><ogc:Like/><ogc:Between/><ogc:NullCheck/></ogc:Comparison_Operators></ogc:Scalar_Capabilities></ogc:Filter_Capabilities></WFS_Capabilities>";
            ByteArrayInputStream capabilitiesReader = new ByteArrayInputStream(wfsCapabilitiesRawData.getBytes("UTF-8"));
            Map hints = new HashMap();
            hints.put(DocumentFactory.VALIDATION_HINT, Boolean.FALSE);

            Object parsed;
            try {
                parsed = DocumentFactory.getInstance(capabilitiesReader, hints, null);
            } catch (Exception e) {
                throw new IOException("Error parsing WFS 1.0.0 capabilities");
            }
        }
    }

    public static void main2(String[] args) throws Exception {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 28992);
        Polygon poly = (Polygon) new WKTReader(gf).read("    POLYGON((202557 384630, 202523 384611, 202581 384507, 202637 384503, 202707 384674, 202698 384709, 202557 384630))");

        WFSDataStore wfsDatastore = null;
        try {
            Map params = new HashMap();
            String url
                    = "http://acceptatie.prvlimburg.nl/geoservices/wion";
            if (!url.endsWith("&") && !url.endsWith("?")) {
                url += url.indexOf("?") >= 0 ? "&" : "?";
            }

            params.put(WFSDataStoreFactory.TIMEOUT.key, 30000);
            params.put(WFSDataStoreFactory.URL.key, url
                    + "Request=GetCapabilities&Service=WFS&Version=1.0.0");
            wfsDatastore
                    = (WFSDataStore) DataStoreFinder.getDataStore(params);
            FilterFactory2 ff
                    = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

            String typeName = "BUIS_DUIKERS";
            String property = "msGeometry";

            Filter filter = ff.intersects(ff.property(property),
                    ff.literal(poly));
            System.out.println("supports:"
                    + ((WFS_1_0_0_DataStore) wfsDatastore).getCapabilities().getFilterCapabilities().fullySupports(filter));
            DefaultQuery query = new DefaultQuery(typeName, filter);
            query.setMaxFeatures(10);
            FeatureSource fs = wfsDatastore.getFeatureSource(typeName);

            FeatureCollection fc = fs.getFeatures(query);

            int count;
            if (fc instanceof DataFeatureCollection) {
                count = ((DataFeatureCollection) fc).getCount();
            } else {
                /* DataFeatureCollection.size() swallowt exception */
                count = fc.size();
            }

            System.out.println("Aantal features: " + count);
        } finally {
            if (wfsDatastore != null) {
                wfsDatastore.dispose();
            }
        }
    }
}
