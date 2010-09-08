package nl.b3p.gis.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

public class ConfigKeeper {

    private static final Log log = LogFactory.getLog(ConfigKeeper.class);

    public ConfigKeeper() {
    }

    public Collection<Configuratie> getConfig(String setting) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List results = sess.createQuery("from Configuratie where setting = :setting ORDER BY id")
                .setParameter("setting", setting).list();

        return results;
    }

    public Configuratie getConfiguratie(String property, String setting) {

        if (property == null || property.length() == 0
                || setting == null || setting.length() == 0) {
            return null;
        }
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List results = sess.createQuery("from Configuratie where property = :prop"
                + " AND setting = :setting")
                .setParameter("prop", property)
                .setParameter("setting", setting).list();
        
        Configuratie configuratie = null;

        if (results.size() == 1) {
            configuratie = (Configuratie) results.get(0);
        }

        if (results.size() < 1) {
            configuratie = new Configuratie();
            configuratie.setProperty(property);
            configuratie.setSetting(setting);
        }

        return configuratie;
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
}
