/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
import nl.b3p.gis.viewer.GetViewerDataAction;
import nl.b3p.gis.viewer.db.DataTypen;
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
import org.geotools.data.ows.FeatureSetDescription;
import org.geotools.data.ows.WFSCapabilities;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.data.wfs.v1_0_0.WFS_1_0_0_DataStore;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.FilterCapabilities;
import org.geotools.filter.FilterTransformer;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.SchemaFactory;
import org.geotools.xml.schema.Schema;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
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

    static {
        Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());

        Schema appSchema = APPSchema.getInstance();
        SchemaFactory.registerSchema(appSchema.getURI(), appSchema);

        Schema msSchema = MSSchema.getInstance();
        SchemaFactory.registerSchema(msSchema.getURI(), msSchema);
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
    public static ArrayList<Feature> getFeatures(Bron b, Themas t, Geometry geom, Filter extraFilter, List<String> propNames, Integer maximum) throws IOException, Exception {
        DataStore ds = b.toDatastore();
        try {
            Filter geomFilter = createIntersectFilter(t, ds, geom);
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
            return getFeatures(ds, t, filter, propNames, maximum);
        } finally {
            ds.dispose();
        }
    }

    /**
     * De beste functie om te gebruiken. Open en dispose de DataStore zelf bij het meegeven.
     * Alle filters zijn gecombineerd in Filter f. (geometry filter en extra filter)
     * Het adminfilter wordt automatisch toegevoegd.
     */
    public static ArrayList<Feature> getFeatures(DataStore ds, Themas t, Filter f, List<String> propNames, Integer maximum) throws IOException, Exception {
        ArrayList<Filter> filters = new ArrayList();
        Filter adminFilter = getThemaFilter(t);
        if (adminFilter != null) {
            filters.add(adminFilter);
        }
        if (f != null) {
            filters.add(f);
        }
        Filter filter = null;
        if (filters.size() == 1) {
            filter = filters.get(0);
        } else if (filters.size() > 1) {
            filter = ff.and(filters);
        } else {
            throw new Exception("Geen filter gemaakt. Data wordt niet getoond");
        }
        if (log.isDebugEnabled()) {
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
        QName ftName = convertFullnameToQName(t.getAdmin_tabel());
        if (ds instanceof WFS_1_1_0_DataStore) {
            fs = ds.getFeatureSource(new NameImpl(ftName.getNamespaceURI(), ftName.getLocalPart()));
            // hierkomt een FeatureSource uit met als typename een prefixed name
            // bv: app:bestemmingsplangebied
        } else {
            fs = ds.getFeatureSource(ftName.getLocalPart());
        }

        DefaultQuery query = null;
        if (ds instanceof WFS_1_1_0_DataStore) {
            // deze query moet passen bij feature source dus moet prefixed name zijn
            String prefixedName = ftName.getPrefix() + ":" + ftName.getLocalPart();
            query = new DefaultQuery(prefixedName, filter);
            query.setNamespace(new URI(ftName.getNamespaceURI())); // wordt niet gebruikt
        } else {
            query = new DefaultQuery(ftName.getLocalPart(), filter);
        }

        int max;
        if (maximum != null) {
            max = maximum.intValue();
        } else {
            max = maxFeatures;
        }
        if (max > 0) {
            query.setMaxFeatures(max);
        }
        if (propNames != null) {
            //zorg er voor dat de pk ook wordt opgehaald
            String adminPk = DataStoreUtil.convertFullnameToQName(t.getAdmin_pk()).getLocalPart();
            if (adminPk != null && adminPk.length()>0 && !propNames.contains(adminPk)) {
                propNames.add(adminPk);
            }

            // zorg ervoor dat de geometry wordt opgehaald, indien aanwezig.
            String geomAttributeName = getGeometryAttributeName(ds, t);
            if (geomAttributeName != null && geomAttributeName.length() > 0  && !propNames.contains(geomAttributeName)) {
                propNames.add(geomAttributeName);
            }
            /*Als een themaDataObject van het type query is en er zitten [] in
            dan moeten deze ook worden opgehaald*/
            Iterator<ThemaData> it = SpatialUtil.getThemaData(t, false).iterator();
            while (it.hasNext()) {
                ThemaData td = it.next();
                //als de td van het type query is.
                if (td.getDataType() != null && td.getDataType().getId() == DataTypen.QUERY) {
                    String commando = td.getCommando();
                    //als er in het commando [replaceme] voorkomt
                    while (commando.indexOf("[") != -1 && commando.indexOf("]") != -1) {
                        //haal alle properties er uit.en stuur deze mee in de query
                        int beginIndex = commando.indexOf("[") + 1;
                        int endIndex = commando.indexOf("]");
                        QName propName = convertFullnameToQName(commando.substring(beginIndex, endIndex));
                        //geen dubbele meegeven.
                        if (propName!=null && !propNames.contains(propName.getLocalPart())) {
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
                query.setPropertyNames(propNames);
            }
        }

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
        Filter filter = ff.intersects(ff.property(geomAttributeName), ff.literal(geom));
        if (ds instanceof WFS_1_0_0_DataStore) {
            WFS_1_0_0_DataStore wfsDs = (WFS_1_0_0_DataStore) ds;
            //als filter intersect niet wordt ondersteund, probeer het dan met een disjoint.
            if (!wfsDs.getCapabilities().getFilterCapabilities().fullySupports(filter)) {
                filter = ff.not(ff.disjoint(ff.property(geomAttributeName), ff.literal(geom)));
            }
            if (!wfsDs.getCapabilities().getFilterCapabilities().fullySupports(filter)) {
                if (!(geom instanceof Point)) {
                    Envelope env = geom.getEnvelopeInternal();
                    CoordinateReferenceSystem crs = getSchema(ds, t).getGeometryDescriptor().getCoordinateReferenceSystem();
                    ReferencedEnvelope bbox = new ReferencedEnvelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(), crs);
                    filter = ff.bbox(ff.property(geomAttributeName), bbox);
                }
            }
            if (!wfsDs.getCapabilities().getFilterCapabilities().supports(filter)) {
                log.info("Intersect,disjoint and bbox filters niet ondersteund. We geven het op: Filter wordt toegepast aan de client kant (java code).");
            }
        }
        if (ds instanceof WFS_1_1_0_DataStore) {
            // TODO
        }
        return filter;
    }

    public static boolean isFilterSupported(WFS_1_0_0_DataStore ds, Filter filter) throws IOException {
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
        if ((adminQuery != null) && (adminQuery.length() > 0)) {
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
        return getSchema(ds, t).getGeometryDescriptor().getName().getLocalPart();
    }

    /**
     * Haal het thema schema op van de datastore. Dit is het schema van het feature type dat bij thema
     * als Admin_tabel is ingevuld. zie ook getSchema(DataStore,String);
     */
    public static SimpleFeatureType getSchema(DataStore ds, Themas t) throws Exception {
        return getSchema(ds, t.getAdmin_tabel());
    }

    /**
     * Haalt het schema op van de featureType met de naam: 'featureName'
     * Als het log op DebugEnabled staat dan wordt er in het log ook een lijst met mogelijke schemas getoond.
     */
    public static SimpleFeatureType getSchema(DataStore ds, String featureName) throws Exception {
        QName ftName = convertFullnameToQName(featureName);
        try {
            if (ds instanceof WFS_1_1_0_DataStore) {
                return ds.getSchema(new NameImpl(ftName.getNamespaceURI(), ftName.getLocalPart()));
            }
            return ds.getSchema(ftName.getLocalPart());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                String schemas = "Er is een fout opgetreden bij het ophalen van het schema (" + featureName + "). Waarschijnlijk is het schema/featureType niet gevonden. Mogelijke schemas: ";
                String[] typenames = ds.getTypeNames();
                for (int i = 0; i < typenames.length; i++) {
                    schemas += "\n";
                    schemas += typenames[i];
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
    public static List<String> getAttributeNames(Bron b, Themas t) throws Exception {
        if (b == null) {
            return new ArrayList<String>();
        }
        DataStore ds = b.toDatastore();
        try {
            return getAttributeNames(ds, t.getAdmin_tabel());
        } finally {
            ds.dispose();
        }
    }

    /**
     * Geeft een lijst terug met String objecten waarin de mogelijke attributeNames staan.
     */
    public static List<String> getAttributeNames(DataStore ds, String featureName) throws Exception {
        QName ftName = convertFullnameToQName(featureName);
        ArrayList<String> attributen = new ArrayList();
        try {
            SimpleFeatureType featureType = getSchema(ds, ftName.getLocalPart());
            List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
            //maak een lijst met mogelijke attributen en de binding class namen.
            for (int i = 0; i < descriptors.size(); i++) {
                attributen.add(descriptors.get(i).getName().getLocalPart());
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
                String prp = convertFullnameToQName(themaData.get(i).getKolomnaam()).getLocalPart();
                if (!propNamesList.contains(prp)) {
                    propNamesList.add(prp);
                }
            }
        }
        return propNamesList;
    }

    public static Name[] getTypeNames(DataStore ds) throws IOException {
        String[] tna = ds.getTypeNames();
        if (tna == null || tna.length == 0) {
            return new Name[]{};
        }
        Name[] typeNames = new Name[tna.length];
        for (int i = 0; i < tna.length; i++) {
            QName qname = convertFullnameToQName(tna[i]);
            if (qname==null) {
                typeNames[i] = new NameImpl("");
            } else {
                typeNames[i] = new NameImpl(qname.getNamespaceURI(), qname.getLocalPart());
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
    static public Name getThemaGeomName(Themas t, GisPrincipal user) throws IOException, Exception {
        Bron b = t.getConnectie(user);
        if (b == null) {
            return null;
        }
        QName n = DataStoreUtil.convertFullnameToQName(t.getSpatial_tabel());
        if (n==null || n.getLocalPart() == null) {
            n = DataStoreUtil.convertFullnameToQName(t.getAdmin_tabel());
        }
        if (n==null || n.getLocalPart() == null) {
            return null;
        }
        DataStore ds = b.toDatastore();
        try {
            SimpleFeatureType sft = ds.getSchema(n.getLocalPart());
            if (sft.getGeometryDescriptor() != null) {
                return sft.getGeometryDescriptor().getName();
            }
        } finally {
            ds.dispose();
        }
        return null;
    }

    static public String getThemaGeomType(Themas t, GisPrincipal user) throws IOException, Exception  {
        Bron b = t.getConnectie(user);
        if (b == null) {
            return null;
        }
        QName n = DataStoreUtil.convertFullnameToQName(t.getSpatial_tabel());
        if (n==null || n.getLocalPart() == null) {
            n = DataStoreUtil.convertFullnameToQName(t.getAdmin_tabel());
        }
        if (n==null || n.getLocalPart() == null) {
            return null;
        }
        DataStore ds = b.toDatastore();
        try {
            SimpleFeatureType sft = ds.getSchema(n.getLocalPart());
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

    public static QName convertFullnameToQName(String ln) {
        if (ln==null) {
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
                        nsUri = new URI("http://www.kaartenbalie.nl/unknown");
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
            }
        }
        return null;
    }

    public static String convertFeature2WKT(Feature f) {
        if (f == null || f.getDefaultGeometryProperty() == null) {
            return null;
        }
        Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();
        if (geom != null && geom.isSimple() && geom.isValid()) {
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
//        String url = "http://x5.b3p.nl/cgi-bin/mapserv_fwtools?map=/srv/maps/kaartenbalie.map&SERVICE=WFS&VERSION=1.0.0&REQUEST=GetCapabilities";
//        String url = "http://afnemers.ruimtelijkeplannen.nl:80/afnemers/services?SERVICE=WFS&VERSION=1.0.0&REQUEST=GetCapabilities";
        params.put(WFSDataStoreFactory.URL.key, url);
        params.put(WFSDataStoreFactory.TIMEOUT.key, 60000);
        params.put(WFSDataStoreFactory.USERNAME.key, "Beheerder");
        params.put(WFSDataStoreFactory.PASSWORD.key, "***REMOVED***");
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
                if (fds.getNamespace()!=null) {
                    continue;
                }
                QName qname = convertFullnameToQName(fds.getName());
                fds.setName(qname.getLocalPart());
                fds.setNamespace(new URI(qname.getNamespaceURI()));
            }
            FilterCapabilities filterCap = wfscap.getFilterCapabilities();
            filterCap.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
            boolean b = filterCap.supports(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
            wfscap.setFilterCapabilities(filterCap);
        }
        if (ds instanceof WFS_1_1_0_DataStore) {
            WFS_1_1_0_DataStore wfs110ds = (WFS_1_1_0_DataStore) ds;
//            throw new Exception("WFS 1.1.0 datastore kent niet alle geometry elementen, dus nu niet gebruiken");
        }

        String prefixedFTName = "demowfs_rivieren_nl";
//        String prefixedFTName = "app:roowfs_Bestemmingsplangebied";
        QName ftName = convertFullnameToQName(prefixedFTName);


        DefaultQuery query = null;
        SimpleFeatureType sft = null;
        FeatureSource fs = null;

        if (ds instanceof WFS_1_1_0_DataStore) {
            sft = ds.getSchema(new NameImpl(ftName.getNamespaceURI(), ftName.getLocalPart()));
            fs = ds.getFeatureSource(new NameImpl(ftName.getNamespaceURI(), ftName.getLocalPart())); // hierkomt een FeatureSource uit met als typename een prefixed name
        } else {
            sft = ds.getSchema(ftName.getLocalPart());
            fs = ds.getFeatureSource(ftName.getLocalPart());
        }

        String ftNaam = sft.getTypeName();
        Name name  = sft.getName();
        String ftNaam2 = sft.getName().getLocalPart();
        String ftNs = sft.getName().getNamespaceURI();
        String geomAttributeName = sft.getGeometryDescriptor().getLocalName();
        String geomAttributeName2 = sft.getGeometryDescriptor().getName().getLocalPart();
        String geomAttributeNS = sft.getGeometryDescriptor().getName().getNamespaceURI();
        CoordinateReferenceSystem crs = sft.getGeometryDescriptor().getCoordinateReferenceSystem();

        ReferencedEnvelope bbox = new ReferencedEnvelope(201000, 380000, 202000, 381000, crs);
        Filter filter = ff.bbox(ff.property(geomAttributeName), bbox);

        if (ds instanceof WFS_1_1_0_DataStore) {
            // deze query moet passen bij feature source dus moet prefixed name zijn
            String prefixedName = ftName.getPrefix() + ":" + ftName.getLocalPart();
            query = new DefaultQuery(prefixedName, filter);
            query.setNamespace(new URI(ftName.getNamespaceURI())); // wordt niet gebruikt
        } else {
            query = new DefaultQuery(ftName.getLocalPart(), filter);
        }

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
                throw new IOException("Error parsing WFS 1.0.0 capabilities", e);
            }
        }

    }
}
