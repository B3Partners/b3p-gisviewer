package nl.b3p.gis.viewer.db;

import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * @author Boy de Wit
 */
public class Redlining {

    private Integer id;
    private String groepnaam;
    private String projectnaam;
    private String fillcolor;
    private String opmerking;
    private Geometry the_geom;

    public String getFillcolor() {
        return fillcolor;
    }

    public void setFillcolor(String fillcolor) {
        this.fillcolor = fillcolor;
    }

    public String getGroepnaam() {
        return groepnaam;
    }

    public void setGroepnaam(String groepnaam) {
        this.groepnaam = groepnaam;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOpmerking() {
        return opmerking;
    }

    public void setOpmerking(String opmerking) {
        this.opmerking = opmerking;
    }

    public String getProjectnaam() {
        return projectnaam;
    }

    public void setProjectnaam(String projectnaam) {
        this.projectnaam = projectnaam;
    }

    public Geometry getThe_geom() {
        return the_geom;
    }

    public void setThe_geom(Geometry the_geom) {
        this.the_geom = the_geom;
    }

    public HashMap getAttributesMap() {
        HashMap hm = new HashMap();

        hm.put("groepnaam", groepnaam);
        hm.put("projectnaam", projectnaam);
        hm.put("fillcolor", fillcolor);
        hm.put("opmerking", opmerking);
        hm.put("the_geom", the_geom);

        return hm;
    }

    public SimpleFeature getFeature(SimpleFeatureType ft) {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(ft);

        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(ft.getAttributeDescriptors());
        for (AttributeDescriptor ad : attributeDescriptors) {
            String ln = ad.getLocalName().toLowerCase();
            Object lnv = this.getAttributesMap().get(ln);
            featureBuilder.add(lnv);
        }

        SimpleFeature f = featureBuilder.buildFeature( null );

        return f;
    }
}
