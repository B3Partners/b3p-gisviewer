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
/**
 * Een klasse specifiek voor de uitvoering van alle spatial query's
 * die binnen het project van belang zijn. Iedere query kent zijn eigen methode
 * of maakt gebruik van een combinatie van methoden om het gewenste resultaat
 * op een zo efficient mogelijke manier te bereiken.
 */
package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;

public class SpatialUtil {

    private static final Log log = LogFactory.getLog(SpatialUtil.class);
    public static final String MULTIPOINT = "multipoint";
    public static final String MULTILINESTRING = "multilinestring";
    public static final String MULTIPOLYGON = "multipolygon";
    public static final List VALID_GEOMS = Arrays.asList(new String[]{
                MULTIPOINT,
                MULTILINESTRING,
                MULTIPOLYGON
            });

    public static List getValidClusters() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String hquery = "FROM Clusters WHERE default_cluster = false ";
        hquery += "ORDER BY belangnr, naam DESC";
        Query q = sess.createQuery(hquery);
        return q.list();
    }

    public static Clusters getDefaultCluster() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String hquery = "FROM Clusters WHERE default_cluster = true ";
        hquery += "ORDER BY belangnr, naam DESC";
        Query q = sess.createQuery(hquery);
        List cl = q.list();
        if (cl != null && cl.size() > 0) {
            return (Clusters) cl.get(0);
        }
        return null;
    }

    /**
     * Haal een Thema op uit de database door middel van het meegegeven thema id.
     *
     * @param themaid String
     *
     * @return Themas
     *
     * @see Themas
     */
    public static Themas getThema(String themaid) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        if (themaid == null || themaid.length() == 0) {
            return null;
        }

        Integer id = new Integer(themaid);
        Themas t = (Themas) sess.get(Themas.class, id);
        return t;
    }

    public static List getValidThemas(boolean locatie) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String hquery = "FROM Themas ";
        if (locatie) {
            hquery += "WHERE locatie_thema = true ";
        }
        hquery += "ORDER BY belangnr, naam DESC";
        Query q = sess.createQuery(hquery);
        return q.list();
    }

    /**
     * DOCUMENT ME!
     *
     * @param t Themas
     * @param basisregel boolean
     *
     * @return List
     *
     */
    // <editor-fold defaultstate="" desc="static public List getThemaData(Themas t, boolean basisregel)">
    static public List<ThemaData> getThemaData(Themas t, boolean basisregel) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String query = "from ThemaData td where td.thema.id = :tid ";
        if (basisregel) {
            query += "and td.basisregel = :br ";
        }
        query += " order by td.dataorder, td.label";
        Query q = sess.createQuery(query);
        q.setInteger("tid", t.getId());
        if (basisregel) {
            q.setBoolean("br", basisregel);
        }
        return q.list();
    }
    // </editor-fold>

    public static String setAttributeValue(Connection conn, String tableName, String keyName, int keyValue, String attributeName, String newValue) throws SQLException {
        if (conn == null) {
            return null;
        }
        StringBuffer sq = new StringBuffer();
        sq.append("update \"");
        sq.append(tableName);
        sq.append("\" set \"");
        sq.append(attributeName);
        sq.append("\" = ? where \"");
        sq.append(keyName);
        sq.append("\" = ? ;");

        boolean orgAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(sq.toString());
            statement.setString(1, newValue);
            statement.setInt(2, keyValue);
            int r = statement.executeUpdate();
            if (r != 1) {
                throw new SQLException("update affects not just one row, but '" + r + "' rows!");
            }
            conn.commit();
        } catch (SQLException ex) {
            if (conn != null) {
                conn.rollback();
            }
            log.error("rollback, reason: ", ex);
            return null;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ex) {
                    log.error("", ex);
                }
            }
        }
        conn.setAutoCommit(orgAutoCommit);
        return newValue;
    }

    public static Geometry geometrieFromText(String wktgeom, int srid) {
        WKTReader wktreader = new WKTReader(new GeometryFactory(new PrecisionModel(), srid));
        try {
            Geometry geom = wktreader.read(wktgeom);
            return geom;
        } catch (ParseException p) {
            log.error("Can't create geomtry from wkt: " + wktgeom, p);
        }
        return null;
    }


}
