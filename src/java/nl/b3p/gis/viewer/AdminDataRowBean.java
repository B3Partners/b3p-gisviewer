/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 * 
 * This file is part of B3P Gisviewer.
 * 
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.List;

public class AdminDataRowBean {
    
    private Object primairyKey;
    private String wkt;
    private List values=null;

    public Object getPrimairyKey() {
        return primairyKey;
    }

    public void setPrimairyKey(Object primairyKey) {
        this.primairyKey = primairyKey;
    }

    public List getValues() {
        return values;
    }

    public void setValues(List values) {
        this.values = values;
    }
    
    public void addValue(Object o){
        if (values==null){
            values=new ArrayList();
        }
        values.add(o);
    }

    /**
     * @return the wkt
     */
    public String getWkt() {
        return wkt;
    }

    /**
     * @param wkt the wkt to set
     */
    public void setWkt(String wkt) {
        this.wkt = wkt;
    }
    
}
