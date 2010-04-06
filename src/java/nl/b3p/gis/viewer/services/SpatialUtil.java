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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Connecties;
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
    public static final List INTERNAL_TABLES = Arrays.asList(new String[]{
                "data_typen",
                "thema_data",
                "themas",
                "waarde_typen",
                "connecties",
                "geometry_columns",
                "spatial_ref_sys",
                "etl_proces"
            });

    public static List getValidClusters() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String hquery = "FROM Clusters WHERE default_cluster = false ";
        hquery += "ORDER BY belangnr DESC";
        Query q = sess.createQuery(hquery);
        return q.list();
    }

    public static Clusters getDefaultCluster() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String hquery = "FROM Clusters WHERE default_cluster = true ";
        hquery += "ORDER BY belangnr DESC";
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
        hquery += "ORDER BY belangnr DESC";
        Query q = sess.createQuery(hquery);
        return q.list();
    }

    /**
     * DOCUMENT ME!
     *
     * @param t Themas
     * @param conn Connection
     *
     * @return int
     *
     */
    static public int getPkDataType(Themas t, Connection conn) throws SQLException {
        String adminPk = t.getAdmin_pk();
        return getColumnDatatype(t, adminPk, conn);
    }

    public static int getColumnDatatype(Themas t, String column, Connection conn) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        String dbtn = t.getAdmin_tabel();
        int dt = java.sql.Types.NULL;
        ResultSet rs = dbmd.getColumns(null, null, dbtn, column);
        if (rs.next()) {
            dt = rs.getInt("DATA_TYPE");
        }
        if (dt == java.sql.Types.NULL) {
            log.debug("java.sql.Types.NULL voor tabelnaam: " + dbtn + ", pknaam: " + column + ", SQL_DATA_TYPE:" + dt);
        }
        return dt;
    }

    /**
     * DOCUMENT ME!
     *
     * @param t Themas
     * @param conn Connection
     *
     * @return int
     *
     */
    static public String getTableGeomName(Themas t, Connection conn) throws SQLException {
        String table = t.getSpatial_tabel();
        if (table == null) {
            table = t.getAdmin_tabel();
        }
        return getTableGeomName(table, conn);
    }

    static public String getTableGeomName(String table, Connection conn) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet rs = dbmd.getColumns(null, null, table, null);
        try {
            while (rs.next()) {
                String typenaam = rs.getString("TYPE_NAME");
                if (typenaam.equalsIgnoreCase("geometry")) {
                    return rs.getString("COLUMN_NAME");
                }
            }
        } finally {
            rs.close();
        }
        return null;
    }

    static public List getColumnNames(String adminTable, Connection conn) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();

        if (adminTable == null) {
            return null;
        }
        ResultSet rs = dbmd.getColumns(null, null, adminTable, null);
        List columns = null;
        try {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if (columns == null) {
                    columns = new ArrayList();
                }
                columns.add(columnName);
            }
            if (columns != null) {
                Collections.sort(columns);
            }
        } finally {
            rs.close();
        }
        return columns;
    }

    static public List getTableNames(Connection conn) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        String[] types = new String[]{"TABLE", "VIEW"};
        ResultSet rs = dbmd.getTables(null, null, null, types);
        List tables = null;
        try {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (INTERNAL_TABLES.contains(tableName.toLowerCase())) {
                    continue;
                }
                if (tables == null) {
                    tables = new ArrayList();
                }
                tables.add(tableName);
            }
            if (tables != null) {
                Collections.sort(tables);
            }
        } finally {
            rs.close();
        }
        return tables;
    }

    static public String getThemaGeomType(Themas t, Connection conn) throws Exception {
        String themaGeomType = null;
        String themaGeomTabel = t.getSpatial_tabel();
        if (themaGeomTabel == null) {
            themaGeomTabel = t.getAdmin_tabel();
        }
        String q = "select * from geometry_columns gc where gc.f_table_name = '" + themaGeomTabel + "'";
        try {
            PreparedStatement statement = conn.prepareStatement(q);
            try {
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    themaGeomType = rs.getString("type");
                }
            } finally {
                statement.close();
            }
        } catch (SQLException ex) {
            log.error("", ex);
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                log.error("", ex);
            }
        }

        String tname = t.getNaam();
        if (themaGeomType == null) {
            log.error("Kan het type geo-object niet vinden: " + tname);
            throw new Exception("Kan het type geo-object niet vinden: " + tname);
        }
        return themaGeomType;
    }

    /**
     * @param x
     * @param y
     * @param srid
     * @return
     * @deprecated use createClickGeom(double[],int) instead.
     */
    static public String createClickGeom(double x, double y, int srid) {
        double[] coords = new double[2];
        coords[0] = x;
        coords[1] = y;
        return createClickGeom(coords, srid);
    }

    /**
     * DOCUMENT ME!
     *
     * @param x double
     * @param y double
     *
     * @return String
     *
     */
    // <editor-fold defaultstate="" desc="static public String createClickGeom(double x, double y, int srid)">
    static public String createClickGeom(double[] coords, int srid) {
        StringBuffer sq = new StringBuffer();
        sq.append(" GeomFromText ( ");
        if (coords.length == 2) {
            sq.append("'POINT(");
            sq.append(coords[0]);
            sq.append(" ");
            sq.append(coords[1]);
            sq.append(")'");
        } else if (coords.length > 2) {
            sq.append("'POLYGON((");
            for (int i = 0; i < coords.length; i += 2) {
                if (i != 0) {
                    sq.append(",");
                }
                sq.append(coords[i]);
                sq.append(" ");
                sq.append(coords[i + 1]);
            }
            sq.append("))'");
        }
        sq.append(", ");
        sq.append(srid);
        sq.append(") ");
        return sq.toString();
    }
    // </editor-fold>

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
    static public List getThemaData(Themas t, boolean basisregel) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String query = "from ThemaData td where td.thema.id = :tid ";
        if (basisregel) {
            query += "and td.basisregel = :br ";
        }
        query += " order by td.dataorder";
        Query q = sess.createQuery(query);
        q.setInteger("tid", t.getId());
        if (basisregel) {
            q.setBoolean("br", basisregel);
        }
        return q.list();
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param kolom String
     * @param tabel String
     * @param x double
     * @param y double
     * @param distance double
     * @param srid int
     *
     * @return String
     * @deprecated Use maxdistance(kolom,tabel,geomcolumn, x, y, distance,srid)
     */
    // <editor-fold defaultstate="" desc="static public String maxDistanceQuery(String kolom, String tabel, double x, double y, double distance, int srid)">
    static public String maxDistanceQuery(String kolom, String tabel, double x, double y, double distance, int srid) {
        return maxDistanceQuery(kolom, tabel, "the_geom", x, y, distance, srid);
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param kolom String
     * @param tabel String
     * @param geomKolom String
     * @param x double
     * @param y double
     * @param distance double
     * @param srid int
     *
     * @return String
     *
     */
    // <editor-fold defaultstate="" desc="static public String maxDistanceQuery(String kolom, String tabel, double x, double y, double distance, int srid)">
    static public String maxDistanceQuery(String kolom, String tabel, String geomKolom, double x, double y, double distance, int srid) {
        StringBuffer sq = new StringBuffer();
        sq.append("select \"");
        sq.append(kolom);
        sq.append("\" from \"");
        sq.append(tabel);
        sq.append("\" tbl where ");
        sq.append(" distance( ");
        sq.append(" tbl.\"" + geomKolom + "\", ");
        sq.append(createClickGeom(x, y, srid));
        sq.append(") < ");
        sq.append(distance);
        return sq.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param kolom String
     * @param tabel String
     * @param x double
     * @param y double
     * @param srid int
     *
     * @return String
     * @deprecated use intersectQuery(String kolom, String tabel, String geomColunn,double x, double y, int srid)
     */
    // <editor-fold defaultstate="" desc="static public String intersectQuery(String kolom, String tabel, double x, double y, int srid)">
    static public String intersectQuery(String kolom, String tabel, double x, double y, int srid) {
        return intersectQuery(kolom, tabel, "the_geom", x, y, srid);
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param kolom String
     * @param tabel String
     * @param geomKolom String
     * @param x double
     * @param y double
     * @param srid int
     *
     * @return String
     *
     */
    // <editor-fold defaultstate="" desc="static public String intersectQuery(String kolom, String tabel, double x, double y, int srid)">
    static public String intersectQuery(String kolom, String tabel, String geomKolom, double x, double y, int srid) {
        StringBuffer sq = new StringBuffer();
        sq.append("select \"");
        sq.append(kolom);
        sq.append("\" from \"");
        sq.append(tabel);
        sq.append("\" tbl where ");
        sq.append(" Intersects( ");
        sq.append(createClickGeom(x, y, srid));
        sq.append(", tbl.\"" + geomKolom);
        sq.append("\") = true ");
        return sq.toString();
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param kolom String
     * @param tabel String
     * @param x double
     * @param y double
     * @param distance double
     * @param srid int
     *
     * @return String
     * @deprecated uses deprecated function. use: InfoSelectQuery(String kolom, String tabel, String geomKolom, double[] coords, double distance, int srid)
     */
    // <editor-fold defaultstate="" desc="static public String InfoSelectQuery(String kolom, String tabel, double x, double y, double distance, int srid)">
    // <editor-fold defaultstate="" desc="static public String InfoSelectQuery(String kolom, String tabel, double x, double y, double distance, int srid)">
    static public String InfoSelectQuery(String kolom, String tabel, double[] coords, double distance, int srid, String geom) {
        // Als thema punten of lijnen dan afstand
        // Als thema polygon dan Intersects
        return InfoSelectQuery(kolom, tabel, coords, distance, srid, null, geom);
    }
    // </editor-fold>

    static public String InfoSelectQuery(String kolom, String tabel, String geomKolom, double[] coords, double distance, int srid, String geom) {
        // Als thema punten of lijnen dan afstand
        // Als thema polygon dan Intersects
        return InfoSelectQuery(kolom, tabel, geomKolom, coords, distance, srid, null, geom);
    }

    /**
     *
     * @param kolom
     * @param tabel
     * @param coords
     * @param distance
     * @param srid
     * @param organizationcodekey
     * @param organizationcode
     * @return infoselect query
     * @deprecated use InfoSelectQuery(String kolom, String tabel, String geomKolom, double[] coords, double distance, int srid, String organizationcodekey, String organizationcode)
     */
    static public String InfoSelectQuery(String kolom, String tabel, double[] coords, double distance, int srid, String extraWhere, String geom) {
        return InfoSelectQuery(kolom, tabel, "the_geom", coords, distance, srid, extraWhere, geom);
    }

    /**
     *
     * @param kolom
     * @param tabel
     * @param geomKolom
     * @param coords
     * @param distance
     * @param srid
     * @param organizationcodekey
     * @param organizationcode
     * @return infoselect query
     */
    static public String InfoSelectQuery(String kolom, String tabel, String geomKolom, double[] coords, double distance, int srid, String extraWhere, String geom) {
        StringBuffer sq = new StringBuffer();
        sq.append("select \"");
        sq.append(kolom);
        sq.append("\" from \"");
        sq.append(tabel);
        sq.append("\" tbl where ");

        if (extraWhere != null) {
            /*sq.append("tbl.\"" + organizationcodekey + "\" = '" + organizationcode + "'");
            sq.append(" and ");*/
            sq.append(extraWhere+" and ");
        }

        sq.append("((");
        if ((coords.length == 2 && geom == null) ||
                (geom != null && !geom.startsWith("POLYGON"))) {
            sq.append("(Dimension(tbl.\"" + geomKolom + "\") < 2) ");
            sq.append("and ");
            sq.append("(Distance(tbl.\"" + geomKolom + "\", ");
            if (coords.length == 2 && geom == null) {
                sq.append(createClickGeom(coords, srid));
            } else if (geom != null && !geom.startsWith("POLYGON")) {
                sq.append(" GeomFromText ( '" + geom + "'," + srid + ") ");
            }
            sq.append(") < ");
            sq.append(distance);
            sq.append(")");
            sq.append(") or (");
        }
        sq.append("Intersects(");
        if (geom == null) {
            sq.append(createClickGeom(coords, srid));
        } else {
            sq.append(" GeomFromText ( '" + geom + "'," + srid + ") ");
        }
        sq.append(", tbl.\"" + geomKolom + "\") = true");
        sq.append(")) order by Distance(tbl.\"" + geomKolom + "\", ");
        if (geom == null) {
            sq.append(createClickGeom(coords, srid));
        } else {
            sq.append(" GeomFromText ( '" + geom + "'," + srid + ") ");
        }
        sq.append(") LIMIT 500");

        log.debug("InfoSelectQuery: " + sq.toString());

        return sq.toString();
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param cols ArrayList
     * @param tabel String
     * @param x double
     * @param y double
     * @param distance double
     * @param srid int
     *
     * @return String
     * @deprecated use closestSelectQuery(ArrayList cols, String tabel, String geomKolom, double x, double y, double distance, int srid)
     */
    // <editor-fold defaultstate="" desc="static public String closestSelectQuery(ArrayList cols, String tabel, double x, double y, double distance, int srid)">
    static public String closestSelectQuery(ArrayList cols, String tabel, double x, double y, double distance, int srid) {
        return closestSelectQuery(cols, tabel, "the_geom", x, y, distance, srid);
    }
    // </editor-fold>

    /**
     *
     * @param cols
     * @param tabel
     * @param geomKolom
     * @param x
     * @param y
     * @param distance
     * @param srid
     * @return
     */
    static public String closestSelectQuery(ArrayList cols, String tabel, String geomKolom, double x, double y, double distance, int srid) {
        StringBuffer sq = new StringBuffer();
        sq.append("select ");
        Iterator it = cols.iterator();
        while (it.hasNext()) {
            sq.append("tbl.\"");
            sq.append(it.next());
            sq.append("\", ");
        }
        sq.append("(Distance(tbl.\"" + geomKolom + "\", ");
        sq.append(createClickGeom(x, y, srid));
        sq.append(")) as dist ");
        sq.append("from \"");
        sq.append(tabel);
        sq.append("\" tbl where ");
        sq.append(" distance( ");
        sq.append(" tbl.\"" + geomKolom + "\", ");
        sq.append(createClickGeom(x, y, srid));
        sq.append(") < ");
        sq.append(distance);
        sq.append(" order by dist limit 1");
        return sq.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param tabel String
     * @param searchparam String
     * @param param String
     *
     * @return String
     * @deprecated use postalcodeRDCoordinates(String tabel, String geomKolom, String searchparam, String param)
     */
    // <editor-fold defaultstate="" desc="static public String postalcodeRDCoordinates(String tabel, String searchparam, String param)">
    static public String postalcodeRDCoordinates(String tabel, String searchparam, String param) {
        return postalcodeRDCoordinates(tabel, "the_geom", searchparam, param);
    }
    // </editor-fold>

    /**
     * @param tabel
     * @param geomKolom
     * @param searchparam
     * @param param
     * @return
     */
    static public String postalcodeRDCoordinates(String tabel, String geomKolom, String searchparam, String param) {
        return "select distinct " + searchparam + " as naam, astext(tbl." + geomKolom + ") as pointsresult from " + tabel + " tbl where lower(tbl." + searchparam + ") = lower('" + param + "')";
    }

    /**
     * DOCUMENT ME!
     *
     * @param tabel String
     * @param searchparam String
     * @param param String
     *
     * @return String
     * @deprecated use cityRDCoordinates(String tabel, String geomKolom,String searchparam, String param)
     */
    // <editor-fold defaultstate="" desc="static public String cityRDCoordinates(String tabel, String searchparam, String param)">
    static public String cityRDCoordinates(String tabel, String searchparam, String param) {
        return cityRDCoordinates(tabel, "the_geom", searchparam, param);
    }
    // </editor-fold>

    /**
     * @param tabel
     * @param geomKolom
     * @param searchparam
     * @param param
     * @return
     */
    static public String cityRDCoordinates(String tabel, String geomKolom, String searchparam, String param) {
        return "select distinct " + searchparam + " as naam, astext(centroid(tbl." + geomKolom + ")) as pointsresult from " + tabel + " tbl where lower(tbl." + searchparam + ") like lower('%" + param + "%')";
    }

    /**
     * DOCUMENT ME!
     *
     * @param tabel String
     * @param searchparam String
     * @param hm String
     * @param n_nr String
     *
     * @return String
     * @deprecated use
     */
    // <editor-fold defaultstate="" desc="static public String wolHMRDCoordinates(String tabel, String searchparam, String hm, String n_nr)">
    static public String wolHMRDCoordinates(String tabel, String searchparam, String hm, String n_nr) {
        return wolHMRDCoordinates(tabel, "the_geom", searchparam, hm, n_nr);
    }
    // </editor-fold>

    /**
     * @param tabel
     * @param geomKolom
     * @param searchparam
     * @param hm
     * @param n_nr
     * @return
     */
    static public String wolHMRDCoordinates(String tabel, String geomKolom, String searchparam, String hm, String n_nr) {
        return "select " + searchparam + " as naam, astext(hecto." + geomKolom + ") as pointsresult from " + tabel + " hecto where (" + "(CAST(hecto." + searchparam + " AS FLOAT) - " + hm + ")*" + "(CAST(hecto." + searchparam + " AS FLOAT) - " + hm + ")) = " + "(select min(" + "(CAST(hecto." + searchparam + " AS FLOAT) - " + hm + ")*" + "(CAST(hecto." + searchparam + " AS FLOAT) - " + hm + ")) " + "from " + tabel + " hecto where lower(hecto.n_nr) = lower('" + n_nr + "') ) " + "AND lower(hecto.n_nr) = lower('" + n_nr + "')";

        /* Example query:
         * select astext(hecto.the_geom) from verv_nwb_hmn_p hecto where
         * ((CAST(hecto.hm AS FLOAT) - 10.2)*(CAST(hecto.hm AS FLOAT) - 10.2)) =
         * (select min((CAST(hecto.hm AS FLOAT) - 10.2)*(CAST(hecto.hm AS FLOAT)
         * - 10.2)) from verv_nwb_hmn_p hecto where hecto.n_nr = 'N261' )
         * AND hecto.n_nr = 'N261'
         */
    }

    /**
     * DOCUMENT ME!
     *
     * @param operator String
     * @param tb1 String
     * @param tb2 String
     * @param divide int
     * @param extraCriteria String
     *
     * @return String
     * @deprecated use intersectionArea(String operator,String tb1,String geomColumn1,String tb2, String geomColumn2, String idColumnName, String id, int divide, String extraCriteria)
     */
    // <editor-fold defaultstate="" desc="public static String intersectionArea(String operator,String tb1, String tb2, String id,int divide, String extraCriteria)">
    public static String intersectionArea(String operator, String tb1, String tb2, String id2, String id, int divide, String extraCriteria) {
        return intersectionArea(operator, tb1, "the_geom", tb2, "the_geom", id2, id, divide, extraCriteria);
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param operator String
     * @param tb1 String
     * @param geomColumn1 String
     * @param tb2 String
     * @param geomColumn2 String
     * @param idColumnName String
     * @param id String
     * @param divide int
     * @param extraCriteria String
     *
     * @return String
     *
     */
    // <editor-fold defaultstate="" desc="static public String intersectionArea(String operator,String tb1,String geomColumn1,String tb2, String geomColumn2,String idColumnName, String id,int divide, String extraCriteria)">
    static public String intersectionArea(String operator, String tb1, String geomColumn1, String tb2, String geomColumn2,
            String idColumnName, String id, int divide, String extraCriteria) {
        StringBuffer sq = new StringBuffer();
        sq.append("select (" + operator + "(area(Intersection(tb1.\"" + geomColumn1 + "\",tb2.\"" + geomColumn2 + "\"))))/" + divide + " as result ");
        sq.append("from \"" + tb1 + "\" tb1, \"" + tb2 + "\" tb2 ");
        sq.append("where tb2.\"" + idColumnName + "\" = ");
        String sqlId = "\'" + id + "\'";
        try {
            int intId = Integer.parseInt(id);
            sqlId = "" + intId;
        } catch (Exception e) {
        }
        sq.append(sqlId + " ");
        /*Voor optimalizatie van de query een where statement toevoegen
         *bij testen verkleinde de tijd een 4 voud
         */
        sq.append("and intersects(tb1.\"" + geomColumn1 + "\",tb2.\"" + geomColumn2 + "\")" + extraCriteria);
        return sq.toString();
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param operator String
     * @param tb1 String
     * @param tb2 String
     * @param id String
     * @param divide int
     * @param extraCriteria String
     *
     * @return String
     * @deprecated intersectionLength(String operator, String tb1, String geomColumn1, String tb2, String geomColumn2, String idColumnName, String id, int divide, String extraCriteria)
     */
    // <editor-fold defaultstate="" desc="static public String intersectionLength(String operator,String tb1,String tb2,String id,int divide, String extraCriteria)">
    static public String intersectionLength(String operator, String tb1, String tb2, String id2, String id, int divide, String extraCriteria) {
        return intersectionLength(operator, tb1, "the_geom", tb2, "the_geom", id2, id, divide, extraCriteria);
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param operator String
     * @param tb1 String
     * @param geomColumn1 String
     * @param tb2 String
     * @param geomColumn2 String
     * @param idColumnName String
     * @param id String
     * @param divide int
     * @param extraCriteria String
     *
     * @return String
     *
     */
    // <editor-fold defaultstate="" desc="static public String intersectionLength(String operator,String tb1,String geomColumn1,String tb2, String geomColumn2, String idColumnName, String id,int divide, String extraCriteria)">
    static public String intersectionLength(String operator, String tb1, String geomColumn1, String tb2, String geomColumn2,
            String idColumnName, String id, int divide, String extraCriteria) {
        StringBuffer sq = new StringBuffer();
        sq.append("select " + operator + "(length(Intersection(tb1.\"" + geomColumn1 + "\",tb2." + geomColumn2 + ")))/" + divide + " as result ");
        sq.append("from \"" + tb1 + "\" tb1, \"" + tb2 + "\" tb2 ");
        sq.append("where tb2.\"" + idColumnName + "\" = ");
        String sqlId = "\'" + id + "\'";
        try {
            int intId = Integer.parseInt(id);
            sqlId = "" + intId;
        } catch (Exception e) {
        }
        sq.append(sqlId + " ");
        /*Voor optimalizatie van de query een where statement toevoegen
         *bij testen verkleinde de tijd een 4 voud
         */
        sq.append("and intersects(tb1.\"" + geomColumn1 + "\",tb2.\"" + geomColumn2 + "\")" + extraCriteria);
        return sq.toString();
    }
    // </editor-fold>

    /**
     *Maakt een query string die alle objecten selecteerd uit tb1 waarvan het object een relatie heeft volgens de meegegeven relatie
     *met het geo object van tb2
     */
    /**
     * DOCUMENT ME!
     *
     * @param tb1 String
     * @param tb2 String
     * @param relationFunction String
     * @param saf String
     * @param analyseObjectId String
     * @param extraCriteriaString String
     *
     * @return String
     * @deprecated use hasRelationQuery(String tb1, String geomColumn1, String tb2, String geomColumn2, String relationFunction, String saf, String idColumnName, String analyseObjectId, String extraCriteriaString)
     */
    // <editor-fold defaultstate="" desc="static public String hasRelationQuery(String tb1,String tb2, String relationFunction,String saf, String analyseObjectId, String extraCriteriaString)">
    static public String hasRelationQuery(String tb1, String tb2, String relationFunction, String saf, String analyseObjectId, String extraCriteriaString) {
        //"select * from <themaGeomTabel> tb1, <analyseGeomTable> tb2 where tb1.<theGeom> tb2.<theGeom>";
        return hasRelationQuery(tb1, "the_geom", tb2, "the_geom", relationFunction, saf, "id", analyseObjectId, extraCriteriaString);
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!
     *
     * @param tb1 String
     * @param geomColumn1 String
     * @param tb2 String
     * @param geomColumn2 String
     * @param relationFunction String
     * @param saf String
     * @param idColumnName String
     * @param analyseObjectId String
     * @param extraCriteriaString String
     *
     * @return String
     *
     */
    static public String hasRelationQuery(String tb1, String geomColumn1, String tb2, String geomColumn2,
            String relationFunction, String saf, String idColumnName, String analyseObjectId, String extraCriteriaString) {
        StringBuffer sq = new StringBuffer();
        sq.append("select tb1.\"" + saf + "\" ");
        sq.append("from \"" + tb1 + "\" tb1, \"" + tb2 + "\" tb2 ");
        sq.append("where tb2.\"" + idColumnName + "\" = ");
        String sqlAnalyseObjectId = "\'" + analyseObjectId + "\'";
        try {
            int inObjectId = Integer.parseInt(analyseObjectId);
            sqlAnalyseObjectId = "" + inObjectId;
        } catch (Exception e) {
        }
        sq.append(sqlAnalyseObjectId + " ");
        sq.append("and " + relationFunction + "(tb1.\"" + geomColumn1 + "\", tb2.\"" + geomColumn2 + "\") ");
        sq.append(extraCriteriaString + " limit 300");
        return sq.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param select String
     * @param table1 String
     * @param table2 String
     * @param tableIdColumn1 String
     * @param tableId1 String
     * @param extraCriteria String
     *
     * @return String
     * @deprecated use withinQuery(String select, String table1, String geomColumn1, String table2, String geomColumn2, String tableIdColumn1, String tableId1, String extraCriteria)
     */
    static public String withinQuery(String select, String table1, String table2, String tableIdColumn1,
            String tableId1, String extraCriteria) {
        return withinQuery(select, table1, "the_geom", table2, "the_geom", tableIdColumn1, tableId1, extraCriteria);
    }

    /**
     *
     * @param select String
     * @param table1 String
     * @param geomColumn1 String
     * @param table2 String
     * @param geomColumn2 String
     * @param tableIdColumn1 String
     * @param tableId1 String
     * @param extraCriteria String
     *
     * @return String
     *
     */
    static public String withinQuery(String select, String table1, String geomColumn1, String table2,
            String geomColumn2, String tableIdColumn1, String tableId1, String extraCriteria) {
        StringBuffer sq = new StringBuffer();
        sq.append("select ");
        sq.append(select + " ");
        sq.append("from \"");
        sq.append(table1 + "\" tb1, \"" + table2 + "\" tb2 where ");
        sq.append("tb2.\"" + tableIdColumn1 + "\" = " + tableId1 + " ");
        sq.append("and Within(tb1.\"" + geomColumn1 + "\",tb2.\"" + geomColumn2 + "\")" + extraCriteria);
        return sq.toString();
    }

    public static String getAreaQuery(String tableName, String geomColumn, String attributeName, String compareValue) {
        StringBuffer sq = new StringBuffer();
        sq.append("select Area(\"");
        sq.append(geomColumn);
        sq.append("\") from \"");
        sq.append(tableName + "\" tb where tb.\"");
        sq.append(attributeName);
        sq.append("\" = '");
        sq.append(compareValue);
        sq.append("';");
        return sq.toString();
    }

    public static String getEnvelopeQuery(String tableName, String geomColumn, String attributeName, String compareValue) {
        StringBuffer sq = new StringBuffer();
        sq.append("select asText(ENVELOPE(\"");
        sq.append(geomColumn);
        sq.append("\")) from \"");
        sq.append(tableName + "\" tb where tb.\"");
        sq.append(attributeName);
        sq.append("\" = '");
        sq.append(compareValue);
        sq.append("';");
        return sq.toString();
    }

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

    /**
     * @param envelope the wkt envelope
     * @return double[4] or null.{minx,miny,maxx,maxy}
     */
    public static double[] wktEnvelope2bbox(String wkt, int srid) {
        double[] bbox = new double[4];
        if (wkt == null) {
            return null;
        } else {
            Geometry geom = geometrieFromText(wkt, srid);
            if (geom == null) {
                return null;
            }
            Envelope env = geom.getEnvelopeInternal();
            bbox[0] = env.getMinX();
            bbox[1] = env.getMinY();
            bbox[2] = env.getMaxX();
            bbox[3] = env.getMaxY();
            return bbox;
        }
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
