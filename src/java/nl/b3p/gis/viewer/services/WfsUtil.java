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
package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Polygon;
import java.io.StringReader;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.ogc.utils.OGCConstants;
import nl.b3p.ogc.utils.OGCRequest;
import nl.b3p.ogc.utils.OgcWfsClient;
import nl.b3p.xml.wfs.FeatureType;
import nl.b3p.xml.wfs.GetFeature;
import nl.b3p.xml.wfs.WFS_Capabilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exolab.castor.xml.Unmarshaller;
import org.opengis.feature.Feature;
import org.w3c.dom.Element;

/**
 *
 * @author Roy Braam
 */
public class WfsUtil {
    private static final Log log = LogFactory.getLog(WfsUtil.class);
    /** Creates a new instance of WfsUtil */
    public WfsUtil() {
    }
    public static ArrayList getFeatureNameList(WFS_Capabilities cap){
        if (cap==null)
            return null;
        nl.b3p.xml.wfs.FeatureTypeList ftl = cap.getFeatureTypeList();
        ArrayList tns = null;
        if (ftl != null) {
            tns = new ArrayList();
            for (int i = 0; i < ftl.getFeatureTypeCount(); i++) {
                nl.b3p.xml.wfs.FeatureType ft = ftl.getFeatureType(i);
                if(ft.getName()!=null && ft.getName().length()>0){
                    tns.add(ft.getName());
                }
            }
        }
        return tns;
    }
    
   static public ArrayList getFeatureElements(Connecties c,String adminTable) throws Exception{
        if (c!=null && c.getType().equalsIgnoreCase(Connecties.TYPE_WFS)){
            OGCRequest or = createOGCRequest(c);
            if (adminTable==null || adminTable.length()<1)
                return null;
            or.addOrReplaceParameter(OGCRequest.WFS_PARAM_TYPENAME,adminTable);
            ArrayList nl= OgcWfsClient.getDescribeFeatureElements(OgcWfsClient.getDescribeFeatureType(or));
            return nl;
        } else{
            return null;
        }
    }
    
    static public WFS_Capabilities getCapabilities(Connecties c) throws Exception{
        if (c != null && c.getType().equalsIgnoreCase(Connecties.TYPE_WFS)){
            OGCRequest or = createOGCRequest(c);
            return OgcWfsClient.getCapabilities(or);
        }else
            return null;
    }
    
    public static ArrayList getWFSObjects(Themas t, double[] coords, String srsName, double distance,HttpServletRequest request, String geom) throws Exception {
        Connecties conn =getWfsConnection(t,request);
        if (conn==null)
            return null;
        OGCRequest or = createOGCRequest(conn);
        or.addOrReplaceParameter(OGCRequest.WFS_PARAM_TYPENAME,t.getAdmin_tabel());
        //er wordt alleen nog maar 1.0.0 ondersteund door jump
        or.addOrReplaceParameter(OGCRequest.VERSION,OGCConstants.WFS_VERSION_100);
        GetFeature gf = OgcWfsClient.getGetFeatureRequest(or);
        //beide nodig voor het maken van een bbox wfs query
        WFS_Capabilities cap= OgcWfsClient.getCapabilities(or);
        FeatureType ft=OgcWfsClient.getCapabilitieFeatureType(cap,t.getAdmin_tabel());

        if(geom == null){
            if (coords.length==2){
                double distance2=distance/2;
                double[] newCoords= new double[4];
                newCoords[0]=coords[0]-distance2;
                newCoords[1]=coords[1]-distance2;
                newCoords[2]=coords[0]+distance2;
                newCoords[3]=coords[1]+distance2;
                coords=newCoords;
            }else if(coords.length==10){
                double[] newCoords= new double[4];
                newCoords[0]=coords[0];
                newCoords[1]=coords[1];
                newCoords[2]=coords[4];
                newCoords[3]=coords[5];
                coords=newCoords;
            }
            if (coords.length!=4) {
                throw new Exception("Polygons not supported! If polygon got 5 xy-coords a bbox will be created with the 1st and 3th coord");
            }
            OgcWfsClient.addBboxFilter(gf,getGeometryAttributeName(t,request),coords,srsName, ft);
        }else{
            if(geom.startsWith("POINT")){
                String point = geom.substring(6, geom.length()-1);
                String[] coordsPoint = point.split(" ");

                if (coordsPoint.length==2){
                    double distance2=distance/2;
                    double[] newCoords= new double[4];
                    newCoords[0]=Double.parseDouble(coordsPoint[0])-distance2;
                    newCoords[1]=Double.parseDouble(coordsPoint[1])-distance2;
                    newCoords[2]=Double.parseDouble(coordsPoint[0])+distance2;
                    newCoords[3]=Double.parseDouble(coordsPoint[1])+distance2;
                    coords=newCoords;
                }
                OgcWfsClient.addBboxFilter(gf,getGeometryAttributeName(t,request),coords,srsName, ft);
            }else if(geom.startsWith("POLYGON")){
                String polygon = geom.substring(9, geom.length()-2);
                OgcWfsClient.addPolygonFilter(gf, getGeometryAttributeName(t,request), polygon, srsName, ft);
            }else{
                /* het is een LINESTRING */
            }
        }
        return OgcWfsClient.getFeatureElements(gf,or);
    }
    
    public static ArrayList getWFSObjects(Themas t, String key, String value,HttpServletRequest request) throws Exception {
        Connecties conn =getWfsConnection(t,request);
        if (conn==null)
            return null;
        OGCRequest or = createOGCRequest(conn);
        or.addOrReplaceParameter(OGCRequest.WFS_PARAM_TYPENAME,t.getAdmin_tabel());
        //er wordt alleen nog maar 1.0.0 ondersteund door jump
        or.addOrReplaceParameter(OGCRequest.VERSION,OGCConstants.WFS_VERSION_100);
        GetFeature gf = OgcWfsClient.getGetFeatureRequest(or);        
        OgcWfsClient.addPropertyIsEqualToFilter(gf,key,value);        
        return OgcWfsClient.getFeatureElements(gf,or);
    }
    
    public static Feature getWfsObject(Themas t, String attributeName, String compareValue,HttpServletRequest request) throws Exception{
        Connecties conn =getWfsConnection(t,request);
        if (conn==null)
            return null;
        OGCRequest or = createOGCRequest(conn);
        or.addOrReplaceParameter(OGCRequest.WFS_PARAM_TYPENAME,t.getAdmin_tabel());
        GetFeature gf = OgcWfsClient.getGetFeatureRequest(or);        
        OgcWfsClient.addPropertyIsEqualToFilter(gf,attributeName,compareValue);
        ArrayList features=OgcWfsClient.getFeatureElements(gf,or);
        if (features==null || features.size()!=1){
            throw new Exception("De gegeven id is niet uniek. Query geeft meerdere objecten");
        }
        return (Feature)features.get(0);
        
    }
    
    public static String getGeometryAttributeName(Themas t,HttpServletRequest request) throws Exception{
        Connecties conn =getWfsConnection(t,request);
        if (conn==null)
            return null;
        ArrayList elements = getFeatureElements(conn,t.getAdmin_tabel());
        if (elements!=null){
            for (int i=0; i < elements.size(); i++){
                Element e = (Element)elements.get(i);
                String type=e.getAttribute("type");
                if (type!=null && type.toLowerCase().startsWith("gml:")){
                    return e.getAttribute("name");
                }
            }
        }
        return null;
    }
    
    public static nl.b3p.xml.ogc.v100.Filter createFilterV100(Themas t, double x, double y, double distance, nl.b3p.xml.wfs.v100.capabilities.FeatureType feature,HttpServletRequest request) throws Exception{
        double lowerX,lowerY,upperX,upperY;
        double distance2=distance/2;
        lowerX=x-distance2;
        lowerY=y-distance2;
        upperX=x+distance2;
        upperY=y+distance2; 
        StringBuffer sb = new StringBuffer();
        sb.append("<Filter><BBOX><PropertyName>");
        sb.append(getGeometryAttributeName(t,request));
        sb.append("</PropertyName>");
        sb.append("<gml:Box srsName=\"http://www.opengis.net/gml/srs/epsg.xml#");
        sb.append(28992);
        sb.append("\"><gml:coordinates>");
        sb.append(lowerX).append(",").append(lowerY);
        sb.append(" ");
        sb.append(upperX).append(",").append(upperY);
        sb.append("</gml:coordinates></gml:Box>");
        sb.append("</BBOX></Filter>");
        return (nl.b3p.xml.ogc.v100.Filter)Unmarshaller.unmarshal(nl.b3p.xml.ogc.v100.Filter.class, new StringReader(sb.toString()));
        
    }
    public static nl.b3p.xml.ogc.v110.Filter createFilterV110(Themas t, double x, double y, double distance, nl.b3p.xml.wfs.v110.FeatureType feature,HttpServletRequest request) throws Exception{
        double lowerX,lowerY,upperX,upperY;
        double distance2=distance/2;
        lowerX=x-distance2;
        lowerY=y-distance2;
        upperX=x+distance2;
        upperY=y+distance2; 
        StringBuffer sb = new StringBuffer();
        sb.append("<Filter><Within><PropertyName>");
        sb.append(getGeometryAttributeName(t,request));
        //sb.append("app:geom");
        sb.append("</PropertyName>");
        sb.append("<gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#");
        sb.append(28992);
        sb.append("\"><gml:lowerCorner>");
        sb.append(lowerX).append(" ").append(lowerY);
        sb.append("</gml:lowerCorner><gml:upperCorner>");
        sb.append(upperX).append(" ").append(upperY);
        sb.append("</gml:upperCorner></gml:Envelope>");
        sb.append("</Within></Filter>");
        return (nl.b3p.xml.ogc.v110.Filter)Unmarshaller.unmarshal(nl.b3p.xml.ogc.v110.Filter.class, new StringReader(sb.toString()));
    }

    public static OGCRequest createOGCRequest(Connecties conn){
        OGCRequest or = new OGCRequest(conn.getConnectie_url());
        or.setUsername(conn.getGebruikersnaam());
        or.setPassword(conn.getWachtwoord());
        return or;
    }

    public static boolean validWfsConnection(Themas t){
        if (t==null)
            return false;
        return validWfsConnection(t.getConnectie());
    }
    public static Connecties getWfsConnection(Themas t,HttpServletRequest request){
        Connecties c=null;
        if (t!=null){
            c=t.getConnectie();
        }
        return getWfsConnection(c, request);
    }
    public static boolean validWfsConnection(Connecties c){
        if(c!=null && !c.getType().equalsIgnoreCase(Connecties.TYPE_WFS))
            return false;
        else
            return true;
    }
    public static Connecties getWfsConnection(Connecties c,HttpServletRequest request){
        if (!validWfsConnection(c))
            return null;
        if (c==null){
            GisPrincipal gp =GisPrincipal.getGisPrincipal(request);
            if (gp!=null) {
                c= gp.getKbWfsConnectie();
            }
        }
        return c;
    }

}
