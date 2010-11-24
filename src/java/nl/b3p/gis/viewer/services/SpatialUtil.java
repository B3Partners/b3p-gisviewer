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
import java.util.Iterator;
import java.util.List;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.DataTypen;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.WaardeTypen;
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
    
    public static Gegevensbron getGegevensbron(String gbId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        if (gbId == null || gbId.length() == 0) {
            return null;
        }

        Integer id = new Integer(gbId);
        Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, id);
        
        return gb;
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

    static public List<ThemaData> getThemaData(Gegevensbron gb, boolean basisregel) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        String query = "from ThemaData td where gegevensbron = :gb ";
        if (basisregel) {
            query += "and td.basisregel = :br ";
        }
        query += " order by td.dataorder, td.label";
        Query q = sess.createQuery(query);
        q.setInteger("gb", gb.getId());
        if (basisregel) {
            q.setBoolean("br", basisregel);
        }

        List themadata = q.list();

        return themadata;
    }

    public static String setAttributeValue(Connection conn, String tableName, String keyName, int keyValue, String attributeName, String newValue) throws SQLException {
        if (conn == null) {
            return null;
        }
        StringBuilder sq = new StringBuilder();
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
