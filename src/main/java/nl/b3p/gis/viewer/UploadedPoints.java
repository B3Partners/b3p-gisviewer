package nl.b3p.gis.viewer;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Boy de Wit
 */
public class UploadedPoints {
    private Map points;
    
    public UploadedPoints() {   
        points = new HashMap();
    }
    
    public UploadedPoints(HashMap points) {
        this.points = points;
    }
    
    public void addPoint(String label, String wkt) {
        if (points != null) {
            points.put(label, wkt);
        }
    }

    public Map getPoints() {
        return points;
    }

    public void setPoints(Map points) {
        this.points = points;
    }
}