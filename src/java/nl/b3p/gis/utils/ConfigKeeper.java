package nl.b3p.gis.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConfigKeeper {

    private static final Log log = LogFactory.getLog(ConfigKeeper.class);

    public ConfigKeeper() {
    }

    public Collection<Configuratie> getConfig(String setting) {
        String hql = "from Configuratie";

        if (setting != null && setting.length() != 0) {
            hql += " where setting = '" + setting + "'";
        }

        hql += " order by id";

        Session sess = null;
        Transaction tx = null;
        Collection allResults = null;

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            tx = sess.beginTransaction();

            Query q = sess.createQuery(hql);
            allResults = q.list();

            tx.commit();

        } catch (Exception ex) {
            log.error("Fout tijdens ConfigKeeper constructor: " + ex.getLocalizedMessage());
            tx.rollback();
        }

        return allResults;
    }

    public Map<String, Object> getConfigMap(String setting)
            throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Map dbconfig = new HashMap();
        Collection<Configuratie> configs = getConfig(setting);
        for (Configuratie config : configs) {

            String property = config.getProperty();
            String propval = config.getPropval();
            Object propvalObject = propval;

            String type = config.getType();
            if (type != null && type.length() > 0 && propval != null) {
                Class typeClass = Class.forName(type);
                Constructor typeConstructor = typeClass.getConstructor(String.class);
                propvalObject = typeConstructor.newInstance(new Object[]{propval});
            }

            dbconfig.put(property, propvalObject);
        }
        
        return dbconfig;
    }

    public Configuratie getConfiguratie(String property, String setting) {
        if (property == null || property.length() == 0
                || setting == null || setting.length() == 0) {
            return null;
        }

        String hql = "from Configuratie where property = :property and setting = :setting";
        Session sess = null;
        Transaction tx = null;
        Configuratie configuratie = null;

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            tx = sess.beginTransaction();

            Query q = sess.createQuery(hql).setParameter("setting", setting).setParameter("property", property);
            configuratie = (Configuratie) q.uniqueResult();

            tx.commit();

        } catch (Exception ex) {
            log.error("Fout tijdens getConfiguratie: " + ex.getLocalizedMessage());
            tx.rollback();
        }

        if (configuratie == null) {
            configuratie = new Configuratie();
            configuratie.setProperty(property);
            configuratie.setSetting(setting);
        }

        return configuratie;
    }
}
