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

import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.geotools.filter.FilterFactoryImpl2;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.services.WfsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.geotools.data.wfs.v1_1_0.WFSFeatureSource;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;

public class GetLocationData {

    private static final Log log = LogFactory.getLog(GetLocationData.class);
    private static int maxSearchResults = 10000;

    public GetLocationData() {
    }

    public String[] getArea(String elementId, String themaId, String attributeName, String compareValue, String eenheid) throws SQLException {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        sess.beginTransaction();
        Themas t = (Themas) sess.get(Themas.class, new Integer(themaId));
        String[] returnValue = new String[2];
        returnValue[0] = elementId;


        //Haal op met jdbc connectie
        double area = 0.0;
        if (SpatialUtil.validJDBCConnection(t)) {
            Connection conn =t.getConnectie().getJdbcConnection();
            if (conn==null)
                return null;
            String geomColumn = SpatialUtil.getTableGeomName(t, conn);
            String tableName = t.getSpatial_tabel();
            if (tableName == null) {
                tableName = t.getAdmin_tabel();
            }
            try {
                String q = SpatialUtil.getAreaQuery(tableName, geomColumn, attributeName, compareValue);
                PreparedStatement statement = conn.prepareStatement(q);
                try {
                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        area = new Double(rs.getString(1)).doubleValue();
                    }
                } finally {
                    statement.close();
                }
            } catch (SQLException ex) {
                log.error("", ex);
                returnValue[1] = "Fout (zie log)";
            } finally {
                sess.close();
            }

        }//Haal op met WFS
        else if (WfsUtil.validWfsConnection(t)) {
            try {
                WebContext ctx = WebContextFactory.get();
                HttpServletRequest request = ctx.getHttpServletRequest();
                Feature f = WfsUtil.getWfsObject(t, attributeName, compareValue,request);
                area = ((Geometry)f.getDefaultGeometryProperty().getValue()).getArea();
            } catch (Exception ex) {
                log.error("", ex);
                returnValue[1] = "Fout (zie log)";
            } finally {
                sess.close();
            }
        }
        if (eenheid != null && eenheid.equals("null")) {
            eenheid = null;
        }
        int divide = 0;
        if (eenheid != null) {
            if (eenheid.equalsIgnoreCase("km")) {
                divide = 1000000;
            } else if (eenheid.equalsIgnoreCase("hm")) {
                divide = 10000;
            }
        }
        if (returnValue[1] == null) {
            if (area > 0.0) {
                if (divide > 0) {
                    area /= divide;
                }
                area *= 100;
                area = Math.round(area);
                area /= 100;
            }
            String value = new String("" + area);
            if (eenheid != null) {
                value += " " + eenheid;
            } else {
                value += " m";
            }
            returnValue[1] = value;
        }
        return returnValue;
    }

    /**
     *
     * @param elementId element in html pagina waar nieuwe waarde naar wordt geschreven
     * @param themaId id van thema waar update betrekking op heeft
     * @param keyName naam van primary key voor selectie van juiste row
     * @param keyValue waarde van primary key voor selectie
     * @param attributeName kolomnaam die veranderd moet worden
     * @param oldValue oude waarde van de kolom
     * @param newValue nieuwe waarde van de kolom
     * @return array van 2 return waarden: 1=elementId, 2=oude of nieuwe waarde met fout indicatie
     */
    public String[] setAttributeValue(String elementId, String themaId, String keyName, String keyValue, String attributeName, String oldValue, String newValue) {
        String[] returnValue = new String[2];
        Transaction transaction = null;
        try {
            returnValue[0] = elementId;
            returnValue[1] = oldValue + " (fout)";

            Integer id = FormUtils.StringToInteger(themaId);
            int keyValueInt = FormUtils.StringToInt(keyValue);
            if (id == null || keyValueInt == 0) {
                return returnValue;
            }
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = sess.beginTransaction();
            Themas t = (Themas) sess.get(Themas.class, id);

            String connectionType = null;
            Connection conn = null;
            if (SpatialUtil.validJDBCConnection(t)) {
                try {
                    conn = t.getConnectie().getJdbcConnection();
                    connectionType = Connecties.TYPE_JDBC;
                } catch (SQLException ex) {
                    log.error("Invalid jdbc connection in thema: ", ex);
                }
            } else {
                log.error("Thema heeft geen JDBC connectie: "+t.getNaam(), new UnsupportedOperationException("Function only supports jdbc connections"));
                connectionType = Connecties.TYPE_WFS;
            }
            if (conn == null || connectionType == null) {
                return returnValue;
            }

            //Schrijf met jdbc connectie
            if (connectionType.equalsIgnoreCase(Connecties.TYPE_JDBC)) {
                String tableName = t.getSpatial_tabel();

                try {
                    String retVal = SpatialUtil.setAttributeValue(conn, tableName, keyName, keyValueInt, attributeName, newValue);
                    returnValue[1] = retVal;
                } catch (SQLException ex) {
                    log.error("", ex);
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            log.error("", ex);
                        }
                    }
                }
            } else if (connectionType.equalsIgnoreCase(Connecties.TYPE_WFS)) {
                // TODO WFS
                //Feature f=WfsUtil.getWfsObject(t,attributeName,oldValue);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return returnValue;
    }

    /**
     * In eerste instantie direct uit jsp via dwr aanroepen, later wrappen voor meer veiligheid
     * @param x_input
     * @param y_input
     * @param cols
     * @param sptn
     * @param distance
     * @param srid
     * @return
     *
     * TODO: ook een WFS thema moet mogelijk worden.
     */
    public String[] getData(String x_input, String y_input, String[] cols, int themaId, double distance, int srid) throws SQLException {
        String[] results = new String[cols.length + 3];
        try {
            double x, y;
            String rdx, rdy;
            try {
                x = Double.parseDouble(x_input);
                y = Double.parseDouble(y_input);
                rdx = Long.toString(Math.round(x));
                rdy = Long.toString(Math.round(y));
            } catch (NumberFormatException nfe) {
                return new String[]{nfe.getMessage()};
            }

            if (cols == null || cols.length == 0) {
                return new String[]{rdx, rdy, "No cols"};
            }
            /*if (sptn == null || sptn.length() == 0) {
            return new String[]{rdx, rdy, "No sptn"};
            }*/
            if (srid == 0) {
                srid = 28992; // RD-new
            }
            ArrayList columns = new ArrayList();
            for (int i = 0; i < cols.length; i++) {
                columns.add(cols[i]);
            }

            results[0] = rdx;
            results[1] = rdy;
            results[2] = "";

            Session sess = HibernateUtil.getSessionFactory().openSession();
            Themas t = (Themas) sess.get(Themas.class, new Integer(themaId));
            Connection connection = null;
            if (SpatialUtil.validJDBCConnection(t)) {
                connection = t.getConnectie().getJdbcConnection();
            }else{
                if (t!=null)
                    log.error("Thema heeft geen JDBC connectie: "+t.getNaam(),new UnsupportedOperationException("Only JDBC connection are supported by this method."));
                return results;
            }
            try {
                String geomColumn = SpatialUtil.getTableGeomName(t, connection);
                String sptn = t.getSpatial_tabel();
                if (sptn == null) {
                    sptn = t.getAdmin_tabel();
                }
                String q = SpatialUtil.closestSelectQuery(columns, sptn, geomColumn, x, y, distance, srid);
                PreparedStatement statement = connection.prepareStatement(q);
                try {
                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        results[2] = rs.getString("dist");
                        for (int i = 0; i < cols.length; i++) {
                            results[i + 3] = rs.getString(cols[i]);
                        }
                    }
                } finally {
                    statement.close();
                }
            } catch (SQLException ex) {
                log.error("", ex);
            } finally {
                sess.close();
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return results;
    }

    /**
     * Functie die door DWR ajax wordt gebruikt om bbox coordinaten op te halen met een aantal criterium.
     * @param waarden De waarde waar aan een object moet voldoen.
     * @param columns De kolommen waar de waarden mee vergeleken moeten worden.
     * @param themaIds de ids van de themas waar op gezocht moet worden
     * @param distance Als de bbox een punt is of kleiner dan 1 bij 1 dan wordt de bbox vergroot met de distance
     */
    public ArrayList getMapCoords(String[] waarden, String[] colomns, int[] themaIds, double distance, int maxResults) {
        double distance2 = 0.0;
        if (distance > 0) {
            distance2 = distance / 2;
        }
        ArrayList allcoords = new ArrayList();
        if (colomns.length != themaIds.length) {
            log.error("Aantal kolommen en themas is niet gelijk");
            MapCoordsBean mbc = new MapCoordsBean();
            mbc.setNaam("Zoeker is verkeerd geconfigureerd");
            allcoords.add(mbc);
            return allcoords;
        }

        Session sess = null;
        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            //controleer of alle benodigde data is meegegeven voor het zoeken op een thema.
            for (int ti = 0; ti < themaIds.length; ti++) {
                //controleer of er kolommen zijn om op te zoeken voor het thema.
                String[] cols = colomns[ti].split(",");
                if (cols == null || cols.length == 0) {
                    MapCoordsBean mbc = new MapCoordsBean();
                    mbc.setNaam("No cols");
                    allcoords.add(mbc);
                    return allcoords;
                }
                //als waarden.length > 1 dan zijn er meerdere waardes en moeten er dus evenveel kolommen meegegeven zijn.
                if (waarden.length > 1) {
                    if (waarden.length != cols.length) {
                        MapCoordsBean mbc = new MapCoordsBean();
                        mbc.setNaam("Number of values missing!");
                        allcoords.add(mbc);
                        return allcoords;
                    }
                }                
                Themas t = (Themas) sess.get(Themas.class, new Integer(themaIds[ti]));
                //controleer of thema bestaat
                if (t == null) {
                    MapCoordsBean mbc = new MapCoordsBean();
                    mbc.setNaam("Ongeldig thema met id: " + themaIds[ti] + " geconfigureerd");
                    allcoords.add(mbc);
                    return allcoords;
                }
                // Maximaal aantal zoekresultaten mag de maxSearchResults waarde niet overschrijden
                if(maxResults > maxSearchResults) maxResults = maxSearchResults;
                //maak thema connectie
                if (SpatialUtil.validJDBCConnection(t)) {
                    allcoords.addAll(getMapCoordsJdbcThema(t,sess,cols,waarden,distance2,maxResults));
                }
                else if (WfsUtil.validWfsConnection(t)) {
                    allcoords.addAll(getMapCoordsWfsThema(t,sess,cols,waarden,distance2,maxResults));
                }

            }
        } finally {
            if (sess != null) {
                sess.close();
            }
        }
        if (allcoords.size()>0) {
            return allcoords;
        }
        return null;
    }
    //}catch(Exception )

    private ArrayList getMapCoordsJdbcThema(Themas t, Session sess,String[] cols,String[] waarden,double distance2,int maxResults) {
        if (!SpatialUtil.validJDBCConnection(t)){
            return null;
        }
        ArrayList coords = new ArrayList();
        Connection connection = null;
        
        try {
            if (t.getConnectie() != null) {
                connection = t.getConnectie().getJdbcConnection();
            }
            String sptn = t.getSpatial_tabel();
            String geomcolomn = SpatialUtil.getTableGeomName(t, connection);
            if (sptn == null || sptn.length() == 0) {
                sptn = t.getAdmin_tabel();
            }
            //maak de query voor het ophalen van de objecten die voldoen aan de zoekopdracht
            StringBuffer q = new StringBuffer();
            q.append("select ");

            for (int i = 0; i < cols.length; i++) {
                if (cols[i] != null && cols[i].length() > 0) {
                    if (i != 0) {
                        q.append(",");
                    }
                    q.append("\"" + cols[i] + "\"");
                }
            }
            q.append(", astext(Envelope(collect(tbl.");
            q.append(geomcolomn);
            q.append("))) as bbox from \"");
            q.append(sptn);
            q.append("\" tbl where (");
            StringBuffer whereS = new StringBuffer();
            for (int i = 0; i < cols.length; i++) {
                //als er maar 1 waarde is dan op alle thema attributen de zelfde criteria los laten met een OR er tussen
                if (waarden.length == 1) {
                    if (i != 0) {
                        whereS.append(" or");
                    }
                    whereS.append(" lower(CAST(tbl.");
                    whereS.append("\"" + cols[i] + "\" AS VARCHAR)");
                    whereS.append(") like lower('%");
                    whereS.append(waarden[0].replaceAll("\\'", "''"));
                    whereS.append("%')");
                } else {
                    if (waarden[i].length() > 0) {
                        if (whereS.length() > 0) {
                            whereS.append(" AND");
                        }
                        whereS.append(" lower(CAST(tbl.");
                        whereS.append("\"" + cols[i] + "\" AS VARCHAR)");
                        whereS.append(") like lower('%");
                        whereS.append(waarden[i].replaceAll("\\'", "''"));
                        whereS.append("%')");
                    }
                }
            }
            q.append(whereS.toString());
            q.append(")");
            StringBuffer qc = new StringBuffer();
            for (int i = 0; i < cols.length; i++) {
                if (cols[i] != null && cols[i].length() > 0) {
                    if (i != 0) {
                        qc.append(",");
                    }
                    qc.append("\"" + cols[i] + "\"");
                }
            }
            q.append(" group by ");
            q.append(qc);
            q.append(" order by ");
            q.append(qc);

            if (maxResults > 0) {
                q.append(" LIMIT ");
                q.append(maxResults);
            }
            log.debug(q.toString());
            PreparedStatement statement = connection.prepareStatement(q.toString());
            try {
                ResultSet rs = statement.executeQuery();
                //int loopnum = 0;
                while (rs.next() && coords.size() <= maxResults) {
                    double minx, maxx, miny, maxy;
                    String envelope = rs.getString("bbox");
                    double[] bbox = SpatialUtil.wktEnvelope2bbox(envelope,28992);
                    if (bbox == null) {
                        StringBuffer errorMessage = new StringBuffer();
                        errorMessage.append("Er wordt geen BBOX gegeven door de database voor record met ");
                        for (int i = 0; i < cols.length; i++) {
                            if (rs.getString(cols[i]) != null) {
                                if (i != 0) {
                                    errorMessage.append(",");
                                }
                                errorMessage.append(cols[i]);
                                errorMessage.append("=");
                                errorMessage.append(rs.getString(cols[i]));
                            }
                        }
                        log.error(errorMessage.toString());
                        continue;
                    } else {
                        minx = bbox[0];
                        miny = bbox[1];
                        maxx = bbox[2];
                        maxy = bbox[3];
                    }
                    if (Math.abs(minx - maxx) < 1) {
                        //maxx = minx + distance;
                        minx -= distance2;
                        maxx += distance2;
                    }
                    if (Math.abs(miny - maxy) < 1) {
                        //maxy = miny + distance;
                        maxy += distance2;
                        miny -= distance2;
                    }
                    StringBuffer naam = new StringBuffer();
                    for (int i = 0; i < cols.length; i++) {
                        if (rs.getString(cols[i]) != null) {
                            if (i != 0) {
                                naam.append(",");
                            }
                            naam.append(rs.getString(cols[i]));
                        }
                    }
                    MapCoordsBean mbc = new MapCoordsBean();
                    mbc.setNaam(naam.toString());
                    mbc.setMinx(Double.toString(minx));
                    mbc.setMiny(Double.toString(miny));
                    mbc.setMaxx(Double.toString(maxx));
                    mbc.setMaxy(Double.toString(maxy));
                    coords.add(mbc);
                }
            } finally {
                statement.close();
            }

        } catch (SQLException ex) {
            log.error("", ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
            }
        }
        return coords;

    }

    private ArrayList getMapCoordsWfsThema(Themas t, Session sess,String[] cols,String[] waarden,double distance2, int maxResults){
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();
        Connecties conn= WfsUtil.getWfsConnection(t,request);
        if(conn==null)
            return null;
        ArrayList coords= new ArrayList();
        WFSDataStore ds=null;
        FeatureSource fs=null;
        FeatureCollection fc=null;
        FeatureIterator fi=null;
        try{
            String ft=null;
            
            if(conn != null){
                ds=conn.getDatastore();
                if (ds!=null){
                    ds.setMaxFeatures(maxResults);
                    String version=ds.getServiceVersion();
                    ft= "app:"+t.getAdmin_tabel().substring(t.getAdmin_tabel().indexOf("}")+1,t.getAdmin_tabel().length());
                    if (version.equals("1.0.0")){
                        fs=(org.geotools.data.wfs.v1_0_0.WFSFeatureSource)ds.getFeatureSource(ft);
                    }else{
                        fs=(org.geotools.data.wfs.v1_1_0.WFSFeatureSource)ds.getFeatureSource(ft);
                    }
                }
            }
            if (fs==null){
                throw new IOException("Kan geen FeatureSource worden gemaakt");
            }
            FilterFactoryImpl2 ff2 = new FilterFactoryImpl2(null);
            List orFilters= new ArrayList();
            List andFilters= new ArrayList();
            for (int i=0; i < cols.length; i++){
                if (waarden.length == 1){
                    Filter colFilter= ff2.like(ff2.property(cols[i]),ff2.literal("*"+waarden[0]+"*"),false);//(ff.property(cols[i]), ff.literal(waarden[0]),false);
                    orFilters.add(colFilter);
                }else if (waarden[i]!=null && waarden[i].length()>0){
                    Filter colFilter= ff2.like(ff2.property(cols[i]), ff2.literal("*"+waarden[i]+"*"),false);
                    andFilters.add(colFilter);
                }
            }
            Filter mainFilter=null;
            if (orFilters.size()>0){
                if (orFilters.size()==1){
                    mainFilter=(Filter)orFilters.get(0);
                }else{
                    mainFilter=ff2.or(orFilters);
                }
            }else if (andFilters.size()>0){
                if(andFilters.size()==1){
                    mainFilter=(Filter)andFilters.get(0);
                }else{
                    mainFilter=ff2.and(andFilters);
                }
            }
            if (mainFilter!=null){
                fc=fs.getFeatures(mainFilter);
            }else{
                fc=fs.getFeatures();
            }
            fi=fc.features();            
            while(fi.hasNext()){
                org.opengis.feature.simple.SimpleFeature feature= (SimpleFeature) fi.next();
                if (feature.getDefaultGeometryProperty()!=null){
                    StringBuffer naam = new StringBuffer();
                    for (int i = 0; i < cols.length; i++) {
                        if (feature.getProperty(cols[i]) != null && feature.getProperty(cols[i]).getValue()!=null) {
                            if (i != 0) {
                                naam.append(" ");
                            }
                            naam.append(feature.getProperty(cols[i]).getValue());
                        }
                    }
                    BoundingBox env=feature.getDefaultGeometryProperty().getBounds();
                    MapCoordsBean mbc = new MapCoordsBean();
                    mbc.setNaam(naam.toString());
                    mbc.setMinx(Double.toString(env.getMinX()));
                    mbc.setMiny(Double.toString(env.getMinY()));
                    mbc.setMaxx(Double.toString(env.getMaxX()));
                    mbc.setMaxy(Double.toString(env.getMaxY()));
                    coords.add(mbc);
                }
            }

        }catch(IOException ioe){
            ds=null;
            log.error("Fout bij maken DataStore",ioe);
            MapCoordsBean mbc = new MapCoordsBean();
            mbc.setNaam("Fout bij ophalen van WFSthema: " + t.getId() + ") "+t.getNaam());
            coords.add(mbc);
        }finally{
            if (fc!=null && fi!=null){
                fc.close(fi);
            }
            if (ds!=null){
                ds.dispose();
            }
        }
        return coords;
    }
}