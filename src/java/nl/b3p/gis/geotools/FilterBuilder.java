/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.gis.geotools;

import java.util.ArrayList;
import javax.xml.transform.TransformerException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.FilterTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 2-jul-2010, 9:00:05
 */
public class FilterBuilder {
    private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
    /**
     * Maak een propertyIsEqualTo filter van een kvp
     */
    public static Filter createEqualsFilter(String key, String value){
        Expression e;
        try{
            int iid=Integer.parseInt(value);
            e=ff.literal(iid);
        }catch(NumberFormatException nfe){
            e=ff.literal(value);
        }
        return ff.equals(ff.property(key), e);
    }

    public static FilterFactory2 getFactory(){
        return ff;
    }
    /**
     * Maak een OR filter met daarin allemaal propertyIsEqualTo filters.
     * Voor elke pks wordt een propertyIsEqualTo filter gemaakt en opgenomen in het or filter
     */
    public static Filter createOrEqualsFilter(String key, String[] pks) {
        ArrayList<Filter> filters = new ArrayList();
        PropertyName pn=ff.property(key);
        for(int i=0; i < pks.length; i++){
            filters.add(ff.equals(pn,ff.literal(pks[i])));
        }
        if (filters.size()==1){
            return filters.get(0);
        }else if (filters.size()>1){
            return ff.or(filters);
        }else{
            return null;
        }

    }

    public static Filter createLikeFilter(String key, String extraCriterium) {
        return ff.like(ff.property(key), extraCriterium);
    }

    public static void main(String[] args) throws TransformerException {
        FilterFactory ff = (FilterFactory) CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        ReferencedEnvelope bbox = new ReferencedEnvelope(195580, 466937, 196403, 467760, null);
        Filter filter = null;

//        filter = ff.bbox(ff.property("geometrie"), 195580,466937, 196403, 467760, "EPSG:28992");

        filter = ff.intersects(ff.property("geometrie"), Expression.NIL);
//      filter = ff.bbox(ff.property("geometrie"), (BoundingBox) ff.literal(JTS.toGeometry((com.vividsolutions.jts.geom.Envelope) bbox)));
        filter = ff.bbox(ff.property("geometrie"), bbox);
        FilterTransformer ft = new FilterTransformer();
        try {
            String s = ft.transform(filter);
            System.out.println(s);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }

    }


}
