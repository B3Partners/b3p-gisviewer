package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.UserKaartgroep;
import nl.b3p.gis.viewer.db.UserKaartlaag;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.gis.viewer.db.UserService;
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

    /* Functie die alle clusters teruggeeft voor in boom kaartgroep config */
    public static List getClusters() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String hquery = "FROM Clusters ";
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
     * Haal een Thema op uit de database door middel van het meegegeven thema
     * id.
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

        String hquery = "FROM Themas t LEFT JOIN FETCH t.gegevensbron ";
        if (locatie) {
            hquery += "WHERE locatie_thema = true ";
        }

        hquery += "ORDER BY t.belangnr, t.naam DESC";

        Query q = sess.createQuery(hquery);

        return q.list();
    }

    static public List<ThemaData> getThemaData(Gegevensbron gb, boolean basisregel) {
        if (gb == null) {
            return new ArrayList<ThemaData>();
        }

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

    public static List<UserKaartgroep> getUserKaartGroepen(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartgroep> groepen = sess.createQuery("from UserKaartgroep where code = :code")
                .setParameter("code", code)
                .list();

        return groepen;
    }

    public static List<UserKaartlaag> getUserKaartLagen(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartlaag> lagen = sess.createQuery("from UserKaartlaag where code = :code")
                .setParameter("code", code)
                .list();

        return lagen;
    }

    public static List<UserService> getUserServices(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserService> services = sess.createQuery("from UserService where code = :code")
                .setParameter("code", code)
                .list();

        return services;
    }

    public static List<UserService> getValidUserServices(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserService> validServices = new ArrayList();

        /* selecteer eerst alle services */
        List<UserService> services = sess.createQuery("from UserService where code = :code"
                + " order by groupname")
                .setParameter("code", code)
                .list();

        /* geef alleen services terug die ook layers aan hebben */
        for (UserService service : services) {
            List<UserLayer> layers = sess.createQuery("from UserLayer where serviceid = :ser"
                    + " and show = :show")
                    .setParameter("ser", service)
                    .setParameter("show", true)
                    .list();

            if (layers != null && layers.size() > 0) {
                validServices.add(service);
            }
        }

        return validServices;
    }

    public static List<UserLayer> getValidUserLayers(UserService service) {
        List<UserLayer> validUserLayers = new ArrayList();

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List<UserLayer> layers = sess.createQuery("from UserLayer where serviceid = :service and"
                + " show = :show order by name, title, id")
                .setParameter("service", service)
                .setParameter("show", true)
                .list();

        /* kijken of er ook parent layers in de List erbij moeten */
        for (UserLayer layer : layers) {
            if (layer.getParent() != null) {
                if (!validUserLayers.contains(layer.getParent())) {
                    validUserLayers.add(layer.getParent());
                }
            }
        }

        layers.addAll(validUserLayers);

        return layers;
    }
}
