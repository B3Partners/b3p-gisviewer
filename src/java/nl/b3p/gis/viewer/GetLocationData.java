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

import nl.b3p.gis.geotools.DataStoreUtil;
import com.vividsolutions.jts.geom.Geometry;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.geotools.data.wfs.v1_1_0.WFSFeatureSource;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.DataStore;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.opengis.feature.Feature;

public class GetLocationData {

    private static final Log log = LogFactory.getLog(GetLocationData.class);
    private static int maxSearchResults = 10000;

    public GetLocationData() {
    }

    public String[] getArea(String elementId, String themaId, String attributeName, String compareValue, String eenheid) throws SQLException {
        Session sess = null;
        double area = 0.0;
        String[] returnValue = new String[2];
        returnValue[0] = elementId;
        returnValue[1] = "Fout (zie log)";

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            sess.beginTransaction();
            Themas t = (Themas) sess.get(Themas.class, new Integer(themaId));
            if (t == null) {
                return returnValue;
            }

            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();
            Bron b = (Bron) t.getConnectie(request);
            if (b == null) {
                return returnValue;
            }
            DataStore ds = b.toDatastore();
            try{
                //haal alleen de geometry op.
                String geometryName= DataStoreUtil.getSchema(ds, t).getGeometryDescriptor().getLocalName();
                ArrayList<String> propertyNames=new ArrayList();
                propertyNames.add(geometryName);
                ArrayList<Feature> list=DataStoreUtil.getFeatures(ds, t, FilterBuilder.createEqualsFilter(attributeName,compareValue),propertyNames,1);
                if (list.size()>=1){
                    Feature f = list.get(0);
                    area = ((Geometry) f.getDefaultGeometryProperty().getValue()).getArea();
                }
            }finally{
                ds.dispose();
            }
            
        } catch (Exception ex) {
            log.error("", ex);
            return returnValue;
        } finally {
            sess.close();
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
            if (t == null) {
                return returnValue;
            }

            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();
            Bron b = (Bron) t.getConnectie(request);
            if (b == null) {
                return returnValue;
            }

            if (b.checkType(Bron.TYPE_JDBC)) {
                Connection conn = t.getJDBCConnection();
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
            } else {
                log.error("Thema heeft geen JDBC connectie: " + t.getNaam(), new UnsupportedOperationException("Function only supports jdbc connections"));
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
        return new String[]{"Fout bij laden van data. Functie nog niet omgezet"};
        /*try {
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
            if (t == null) {
                return results;
            }

            WebContext ctx = WebContextFactory.get();
            HttpServletRequest request = ctx.getHttpServletRequest();
            Connecties c = (Connecties) t.getConnectie(request);
            if (c == null) {
                return results;
            }

            String connectieType = c.getType();
            if (Connecties.TYPE_JDBC.equalsIgnoreCase(connectieType)) {
                Connection conn = t.getJDBCConnection();
                try {
                    String geomColumn = SpatialUtil.getTableGeomName(t, conn);
                    String sptn = t.getSpatial_tabel();
                    if (sptn == null) {
                        sptn = t.getAdmin_tabel();
                    }
                    String q = SpatialUtil.closestSelectQuery(columns, sptn, geomColumn, x, y, distance, srid);
                    PreparedStatement statement = conn.prepareStatement(q);
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
            } else if (Connecties.TYPE_WFS.equalsIgnoreCase(connectieType)) {
                log.error("Thema heeft een WFS connectie: " + t.getNaam(), new UnsupportedOperationException("Only JDBC connection are supported by this method."));
            }
        } catch (Exception e) {
            log.error("", e);
        }*/


    }
}
