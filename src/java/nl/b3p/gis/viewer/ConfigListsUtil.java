/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.viewer;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.services.WfsUtil;
import nl.b3p.xml.wfs.WFS_Capabilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.w3c.dom.Element;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

/**
 *
 * @author Roy
 */
public class ConfigListsUtil {

    private static final Log log = LogFactory.getLog(ConfigListsUtil.class);

    private static Connecties getConnectie(Session sess, Integer connId) {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        return getConnectie(sess, user, connId);
    }

    public static Connecties getConnectie(Session sess, GisPrincipal user, Integer connId) {
        Connecties c = null;
        if (connId == null || connId.intValue() == 0) {
            c = user.getKbWfsConnectie();
        } else if (connId.intValue() > 0) {
            c = (Connecties) sess.get(Connecties.class, connId);
        }
        return c;
    }

    public static List getPossibleFeaturesById(Integer connId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            sess.beginTransaction();
            Connecties c = getConnectie(sess, connId);
            return getPossibleFeatures(c);
        } catch (Exception e) {
            log.error("getPossibleFeaturesById error: ", e);
        } finally {
            sess.close();
        }
        return null;
    }

    public static List getPossibleAttributesById(Integer connId, String feature) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        try {
            sess.beginTransaction();
            Connecties c = getConnectie(sess, connId);
            if (c == null) {
                return null;
            }
            if (Connecties.TYPE_JDBC.equalsIgnoreCase(c.getType())) {
                return getPossibleJDBCAttributes(c, feature);
            } else if (Connecties.TYPE_WFS.equalsIgnoreCase(c.getType())) {
                return getPossibleWFSAttributes(c, feature);
            }
        } catch (Exception e) {
            log.error("getPossibleAttributesById error: ", e);
        } finally {
            sess.close();
        }
        return null;
    }

    public static List getPossibleFeatures(Connecties c) throws Exception {
        if (c == null) {
            return null;
        }
        if (Connecties.TYPE_JDBC.equalsIgnoreCase(c.getType())) {
            return getPossibleJDBCFeatures(c);
        } else if (Connecties.TYPE_WFS.equalsIgnoreCase(c.getType())) {
            return getPossibleWFSFeatures(c);
        }
        return null;
    }

    /**
     * Maakt een lijst met mogelijke features voor de gegeven wfs connectie en gebruiker
     */
    public static List getPossibleWFSFeatures(Connecties c) throws Exception {
        if (c == null) {
            return null;
        }
        ArrayList returnValue = null;
        WFS_Capabilities cap = null;
        try {
            cap = WfsUtil.getCapabilities(c);
        } catch (Exception e) {
            log.error("fout bij ophalen capabilities", e);
        }
        List features = WfsUtil.getFeatureNameList(cap);
        if (features != null) {
            returnValue = new ArrayList();
            for (int i = 0; i < features.size(); i++) {
                String[] s = new String[2];
                s[0] = (String) features.get(i);
                s[1] = BaseGisAction.removeNamespace((String) features.get(i));
                returnValue.add(s);
            }
        }
        return returnValue;
    }

    /**
     * Maakt een lijst met mogelijke features voor de gegeven jdbc connectie en gebruiker
     */
    public static List getPossibleJDBCFeatures(Connecties c) throws Exception {
        if (c == null) {
            return null;
        }
        Connection conn = c.getJdbcConnection();
        if (conn == null) {
            return null;
        }
        ArrayList returnValue = null;
        try {
            List features = SpatialUtil.getTableNames(conn);
            if (features != null) {
                returnValue = new ArrayList();
            }
            for (int i = 0; i < features.size(); i++) {
                String[] s = new String[2];
                s[0] = (String) features.get(i);
                s[1] = BaseGisAction.removeNamespace((String) features.get(i));
                returnValue.add(s);
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return returnValue;
    }

    public static List getPossibleAttributes(Connecties c, String feature) throws Exception {
        if (c == null) {
            return null;
        }
        if (Connecties.TYPE_JDBC.equalsIgnoreCase(c.getType())) {
            return getPossibleJDBCAttributes(c, feature);
        } else if (Connecties.TYPE_WFS.equalsIgnoreCase(c.getType())) {
            return getPossibleWFSAttributes(c, feature);
        }
        return null;
    }

    /**
     * Maakt een lijst met mogelijke attributen van een meegegeven jdbc tabel.
     */
    public static List getPossibleJDBCAttributes(Connecties c, String tabel) throws Exception {
        if (c == null || tabel == null) {
            return null;
        }
        Connection conn = c.getJdbcConnection();
        if (conn == null) {
            return null;
        }
        ArrayList returnValue = null;
        try {
            List columns = SpatialUtil.getColumnNames(tabel, conn);
            if (columns == null) {
                return null;
            }
            returnValue = new ArrayList();
            for (int i = 0; i < columns.size(); i++) {
                String[] s = new String[2];
                s[0] = (String) columns.get(i);
                s[1] = BaseGisAction.removeNamespace((String) columns.get(i));
                returnValue.add(s);
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return returnValue;
    }

    /**
     * Maakt een lijst met mogelijke attributen van een meegegeven wfs feature.
     */
    public static List getPossibleWFSAttributes(Connecties c, String feature) throws Exception {
        if (c == null || feature == null) {
            return null;
        }
        List attrs =  WfsUtil.getFeatureElementNames(c, feature, true);
        if (attrs == null) {
            return null;
        }
        List returnValue = new ArrayList();
        for (int i = 0; i < attrs.size(); i++) {
            String name = (String) attrs.get(i);
            String[] s = new String[2];
            s[0] = name;
            s[1] = BaseGisAction.removeNamespace(name);
            returnValue.add(s);
        }
        return returnValue;
    }
}
