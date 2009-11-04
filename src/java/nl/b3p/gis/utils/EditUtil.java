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

/**
 *
 * @author Boy de Wit
 */
public class EditUtil {

    private static final int QUAD_SEGS = 16;

    /**
     * Constructor
     **/
    public EditUtil() throws Exception {
    }

    public String buffer(String wkt, double bufferafstand) throws Exception {

        //TODO waarde kan null zijn
        if ( (wkt == null) || (wkt.length() < 1) ) {
            throw new Exception("Kan niet bufferen. Er is nog geen handeling geselecteerd.");
        }

        if (bufferafstand == 0) {
            throw new Exception("De bufferafstand mag niet 0 zijn.");
        }

        String buffer = "";

        Geometry geom = createGeomFromWKTString(wkt);

        if (geom == null) {
            throw new Exception("Kan niet bufferen. Geometrie is leeg.");
        }
 
        Geometry result = geom.buffer(bufferafstand, QUAD_SEGS, BufferOp.CAP_BUTT);

        if (result == null)
            throw new Exception("Bufferfout bij bufferen geometrie.");

        String testwkt = result.toText();
        Geometry g = null;
        
        if (testwkt.indexOf("MULTI") != -1)
        {
            g = getLargestPolygonFromMultiPolygon(result);

            if (g == null)
                throw new Exception("Bufferfout bij parsen MultiPolygon.");

            buffer = g.toText();
        } else {
            buffer = result.toText();
        }
        
        if ( (buffer.indexOf("POINT") != -1) || (buffer.indexOf("MULTI") != -1) )
            throw new Exception("Het resultaat van de buffer kan niet worden getekend.");

        /*
        if (buffer.indexOf("LINE") == -1)
        {
            if (getNumInteriorRings(geom) > 0)
                throw new Exception("Kan geen donut bufferen.");
        }
        */
        
        return buffer;
    }

    private Geometry createGeomFromWKTString(String wktstring) throws Exception {
        Geometry geom = null;

        try {
            geom = new WKTReader().read(wktstring);
        } catch (ParseException ex) {
            throw new Exception(ex);
        }

        return geom;
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
