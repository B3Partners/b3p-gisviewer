package nl.b3p.gis.utils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.PolygonExtracter;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Boy de Wit
 */
public class EditUtil {

    private static final Log log = LogFactory.getLog(EditUtil.class);
    private static final int QUAD_SEGS = 16;

    /**
     * Constructor
     **/
    public EditUtil() throws Exception {
    }

    public String buffer(String wkt, double bufferafstand) throws Exception {

        if ( (wkt == null) || (wkt.length() < 1) ) {
            throw new Exception("Kan niet bufferen. Er is nog geen handeling geselecteerd.");
        }

        if (bufferafstand == 0) {
            throw new Exception("De bufferafstand mag niet 0 zijn.");
        }

        String buffer = "";
        Geometry geom = createGeomFromWKTString(wkt);

        if (geom == null) {
            throw new Exception("Kan niet bufferen. Geometrie is incorrect.");
        }
 
        Geometry result = geom.buffer(bufferafstand, QUAD_SEGS, BufferOp.CAP_BUTT);

        if (result == null)
            throw new Exception("Resultaat buffer geeft incorrecte geometrie.");
   
        Geometry poly = getLargestPolygonFromMultiPolygon(result);

        if (poly == null)
            throw new Exception("Bufferfout bij omzetten MultiPolygon naar grootste Polygon.");

        buffer = poly.toText();
        
        return buffer;
    }

    private Geometry createGeomFromWKTString(String wktstring) throws Exception {
        WKTReader wktreader = new WKTReader(new GeometryFactory(new PrecisionModel(), 28992));
        try {
            return wktreader.read(wktstring);
        } catch (ParseException ex) {
            throw new Exception(ex);
        }

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

        if (line instanceof LinearRing)
            return new Polygon((LinearRing)line, null, new GeometryFactory(new PrecisionModel(), 28992));

        return null;
    }

    private int getNumInteriorRings(Geometry geom)
    {
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
}
