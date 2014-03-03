package nl.b3p.gis.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.db.CyclomediaAccount;
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

    public Map getDefaultInstellingen() {
        String appCode = "default";

        Map map = null;
        try {
            map = getConfigMap(appCode);
        } catch (Exception ex) {
        }

        if (map == null || map.size() < 1) {
            writeDefaultApplicatie(appCode);
        }

        try {
            map = getConfigMap(appCode);
        } catch (Exception ex) {
        }

        return map;
    }

    public static void writeDefaultApplicatie(String appCode) {
        Configuratie cfg = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        cfg = new Configuratie();
        cfg.setProperty("useCookies");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("multipleActiveThemas");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("dataframepopupHandle");
        cfg.setPropval("null");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLeftPanel");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("autoRedirect");
        cfg.setPropval("2");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useSortableFunction");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layerDelay");
        cfg.setPropval("5000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("refreshDelay");
        cfg.setPropval("1000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("minBboxZoeken");
        cfg.setPropval("1000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("zoekConfigIds");
        cfg.setPropval("\"-1\"");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("maxResults");
        cfg.setPropval("500");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("defaultSearchRadius");
        cfg.setPropval("10000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("usePopup");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useDivPopup");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("usePanelControls");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("expandAll");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tolerance");
        cfg.setPropval("4");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useInheritCheckbox");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLegendInTree");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useMouseOverTabs");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layoutAdminData");
        cfg.setPropval("admindata1");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tabs");
        cfg.setPropval("\"themas\",\"legenda\"");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("planSelectieIds");
        cfg.setPropval("-1");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showRedliningTools");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showBufferTool");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showSelectBulkTool");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showNeedleTool");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showPrintTool");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLayerSelectionTool");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layerGrouping");
        cfg.setPropval("lg_cluster");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupWidth");
        cfg.setPropval("90%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupHeight");
        cfg.setPropval("20%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupLeft");
        cfg.setPropval("5%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupTop");
        cfg.setPropval("75%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("defaultdataframehoogte");
        cfg.setPropval("150");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("viewerType");
        cfg.setPropval("openlayers");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("viewerTemplate");
        cfg.setPropval("standalone");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("objectInfoType");
        cfg.setPropval("popup");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("treeOrder");
        cfg.setPropval("volgorde");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tabWidth");
        cfg.setPropval("300");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);
        //BAG
        cfg = new Configuratie();
        cfg.setProperty("bagMaxBouwjaar");
        cfg.setPropval("" + Calendar.getInstance().get(Calendar.YEAR));
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagMinBouwjaar");
        cfg.setPropval("1000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagMaxOpp");
        cfg.setPropval("16000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagMinOpp");
        cfg.setPropval("0");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagOppAttr");
        cfg.setPropval("OPPERVLAKTE");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagBouwjaarAttr");
        cfg.setPropval("BOUWJAAR");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagGebruiksfunctieAttr");
        cfg.setPropval("GEBRUIKSFUNCTIE");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagGeomAttr");
        cfg.setPropval("the_geom");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useUserWmsDropdown");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("datasetDownload");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showServiceUrl");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("activeTab");
        cfg.setPropval("themas");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("transSliderTab");
        cfg.setPropval("legenda");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        String tileRes = "3440.64,1720.32,860.16,430.08,215.04,107.52,"
                + "53.76,26.88,13.44,6.72,3.36,1.68,0.84,0.42,0.21,0.105";
        
        cfg = new Configuratie();
        cfg.setProperty("tilingResolutions");
        cfg.setPropval(tileRes);
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);  
        
        cfg = new Configuratie();
        cfg.setProperty("showInfoTab");
        cfg.setPropval("off");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);
        
        cfg = new Configuratie();
        cfg.setProperty("helpUrl");
        cfg.setPropval(null);
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);
        
        cfg = new Configuratie();
        cfg.setProperty("showGoogleMapsIcon");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);
        
        cfg = new Configuratie();
        cfg.setProperty("showBookmarkIcon");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);
        
        cfg = new Configuratie();
        cfg.setProperty("contactUrl");
        cfg.setPropval(null);
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);
        
        sess.flush();
    }

    public CyclomediaAccount getCyclomediaAccount(String appCode) {
        CyclomediaAccount account = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List results = sess.createQuery("from CyclomediaAccount where app_code = :appcode")
                .setParameter("appcode", appCode).setMaxResults(1).list();

        if (results != null && results.size() == 1) {
            return (CyclomediaAccount) results.get(0);
        }

        return account;
    }
}
