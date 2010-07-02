/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.gis.geotools;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

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
    public static Filter createEqualsFilterFromKVP(String key, String value){
        Expression e;
        try{
            int iid=Integer.parseInt(value);
            e=ff.literal(iid);
        }catch(NumberFormatException nfe){
            e=ff.literal(value);
        }
        return ff.equals(ff.property(key), e);
    }
}
