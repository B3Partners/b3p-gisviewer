package nl.b3p.gis.viewer;

import com.vividsolutions.jts.geom.Geometry;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.GisPrincipal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.filter.text.cql2.CQLException;
import org.hibernate.Session;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.viewer.db.Redlining;
import nl.b3p.zoeker.configuratie.Bron;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * @author Boy de Wit
 */
public class RedliningAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(RedliningAction.class);

    protected static final String PREPARE_REDLINING = "prepareRedlining";
    protected static final String SEND_REDLINING = "sendRedlining";
    protected static final String REMOVE_REDLINING = "removeRedlining";
    protected static final String REDLINING_FORWARD = "redlining";

    /*
     * Return een hashmap die een property koppelt aan een Action.
     * @return Map hashmap met action properties.
     */
    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(PREPARE_REDLINING);
        hibProp.setDefaultForwardName(REDLINING_FORWARD);
        hibProp.setAlternateForwardName(REDLINING_FORWARD);
        hibProp.setAlternateMessageKey("error.prepareredlining.failed");
        map.put(PREPARE_REDLINING, hibProp);

        hibProp = new ExtendedMethodProperties(SEND_REDLINING);
        hibProp.setDefaultForwardName(REDLINING_FORWARD);
        hibProp.setDefaultMessageKey("message.sendredlining.success");
        hibProp.setAlternateForwardName(REDLINING_FORWARD);
        hibProp.setAlternateMessageKey("error.sendredlining.failed");
        map.put(SEND_REDLINING, hibProp);

        hibProp = new ExtendedMethodProperties(REMOVE_REDLINING);
        hibProp.setDefaultForwardName(REDLINING_FORWARD);
        hibProp.setDefaultMessageKey("message.removeredlining.success");
        hibProp.setAlternateForwardName(REDLINING_FORWARD);
        hibProp.setAlternateMessageKey("error.removeredlining.failed");
        map.put(REMOVE_REDLINING, hibProp);

        return map;
    }

    private void useInstellingen(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        Map instellingen = getInstellingenMap(request);        

        populateFromInstellingen(instellingen, dynaForm, request);
    }

    public ActionForward prepareRedlining(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        dynaForm.initialize(mapping);
        useInstellingen(dynaForm, request);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward sendRedlining(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        useInstellingen(dynaForm, request);

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            //addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Redlining rl = new Redlining();
        populateRedliningObject(dynaForm, rl, request);

        Integer ggbId = (Integer) dynaForm.get("gegevensbron");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Gegevensbron ggb = (Gegevensbron) sess.get(Gegevensbron.class, ggbId);
        ggb.getBron().getUrl();

        String adminPk = ggb.getAdmin_pk();

        DataStore ds = ggb.getBron().toDatastore();
        try {
            String typename = ggb.getAdmin_tabel();
            SimpleFeatureType ft = ds.getSchema(typename);
            SimpleFeature f = rl.getFeature(ft);

            Integer rlId = FormUtils.StringToInteger(dynaForm.getString("redliningID"));

            if (rlId != null && rlId > 0) {
                duUpdate(adminPk, ds, f, rlId);
            } else {
                doInsert(ds, f);
            }            
        } finally {
            ds.dispose();
        }

        populateRedliningForm(rl, dynaForm, request);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    public ActionForward removeRedlining(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Integer ggbId = (Integer) dynaForm.get("gegevensbron");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Gegevensbron ggb = (Gegevensbron) sess.get(Gegevensbron.class, ggbId);
        ggb.getBron().getUrl();
        DataStore ds = ggb.getBron().toDatastore();

        try {
            String typename = ggb.getAdmin_tabel();
            Integer rlId = FormUtils.StringToInteger(dynaForm.getString("redliningID"));

            if (rlId != null && rlId > 0) {
                doDelete(ds, typename, rlId);
            }
        } finally {
            ds.dispose();
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    private void doDelete(DataStore ds, String typename, Integer id) throws IOException, CQLException {
        Filter f = CQL.toFilter("ID = '" + id + "'");
        FeatureWriter writer = ds.getFeatureWriter(typename, f, Transaction.AUTO_COMMIT);

        try {
            while (writer.hasNext()) {
                SimpleFeature newFeature = (SimpleFeature) writer.next();
                writer.remove();
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void doInsert(DataStore dataStore2Write, SimpleFeature feature) throws IOException {
        String typename = feature.getFeatureType().getTypeName();
        FeatureWriter writer = dataStore2Write.getFeatureWriterAppend(typename, Transaction.AUTO_COMMIT);

        try {
            SimpleFeature newFeature = (SimpleFeature) writer.next();
            newFeature.setAttributes(feature.getAttributes());
            newFeature.setDefaultGeometry(feature.getDefaultGeometry());
            writer.write();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void duUpdate(String adminPk, DataStore ds, SimpleFeature feature, Integer id) throws IOException, CQLException {
        /* typename en filter voor de writer */
        String typename = feature.getFeatureType().getTypeName();
        Filter f = CQL.toFilter(adminPk + " = '"+ id + "'");

        FeatureWriter writer = ds.getFeatureWriter(typename, f, Transaction.AUTO_COMMIT);

        try {
            while (writer.hasNext()) {
                SimpleFeature newFeature = (SimpleFeature) writer.next();

                /* TODO: attributes niet hardcoded setten in ieder geval moet
                 het voor de postgres en oracle werken */
                List<Object> attributes = feature.getAttributes();
                
                String projectnaam = (String) feature.getAttribute("PROJECTNAAM");
                String ontwerp = (String) feature.getAttribute("ONTWERP");
                Object opmerking = (String) feature.getAttribute("OPMERKING");

                newFeature.setAttribute("PROJECTNAAM", projectnaam);
                newFeature.setAttribute("ONTWERP", ontwerp);
                newFeature.setAttribute("OPMERKING", opmerking);

                Object defaultGeom = feature.getDefaultGeometry();
                newFeature.setDefaultGeometry(defaultGeom);

                writer.write();
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }    

    private void populateRedliningForm(Redlining rl, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        if (rl == null) {
            return;
        }
        if (rl.getId() != null) {
            dynaForm.set("redliningID", Integer.toString(rl.getId().intValue()));
        }

        String orgCode = getOrganizationCode(request);

        if (orgCode != null) {
            dynaForm.set("groepnaam", orgCode);
        }

        /* klaarzetten huidige projecten */
        Integer ggbId = (Integer) dynaForm.get("gegevensbron");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, ggbId);
        List projecten = getDistinctProjectenForGroep(gb, orgCode);
        request.setAttribute("projecten", projecten);
    }

    private Map getInstellingenMap(HttpServletRequest request) throws Exception {

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Set roles = null;

        if (user != null) {
            roles = user.getRoles();
        }


        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = null;
        try {
            rollenPrio = configKeeper.getConfiguratie("rollenPrio", "rollen");
        } catch (Exception ex) {
            logger.debug("Fout bij ophalen configKeeper configuratie: " + ex);
        }

        String[] configRollen = null;
        if (rollenPrio != null && rollenPrio.getPropval() != null) {
            configRollen = rollenPrio.getPropval().split(",");
        }

        String echteRol = null;

        Boolean foundRole = false;
        for (int i = 0; i < configRollen.length; i++) {
            if (foundRole) {
                break;
            }
            String rolnaam = configRollen[i];

            if (roles != null) {
                Iterator iter = roles.iterator();
                while (iter.hasNext()) {
                    String inlogRol = iter.next().toString();
                    if (rolnaam.equals(inlogRol)) {
                        echteRol = rolnaam;
                        foundRole = true;
                        break;
                    }
                }
            }
        }

        Map map = configKeeper.getConfigMap(echteRol);
        if ((map == null) || (map.isEmpty())) {
            map = configKeeper.getConfigMap("default");
        }
        return map;
    }

    private void populateFromInstellingen(Map instellingen, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {

        String orgCode = getOrganizationCode(request);

        if (orgCode != null) {
            dynaForm.set("groepnaam", orgCode);
        }

        Integer gbId = (Integer) instellingen.get("redliningGegevensbron");
        dynaForm.set("gegevensbron", gbId);

        /* klaarzetten huidige projecten */
        if (gbId != null && gbId > 0) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gbId);

            List projecten = getDistinctProjectenForGroep(gb, orgCode);
            request.setAttribute("projecten", projecten);
        }
    }

    /* Omdat DISTINCT met een Datastore niet kan wordt hier via
     * een Connection en PreparedStatement een List teruggegeven van projectnamen */
    private List getDistinctProjectenForGroep(Gegevensbron gb, String groepnaam) throws SQLException {
        List projecten = new ArrayList();

        Bron bron = gb.getBron();

        String tabel = gb.getAdmin_tabel();

        /* TODO: Kijken waarom een url met SID.SCHEMANAAM momenteel niet werkt.
         * ORA-12505, TNS:listener does not currently know of SID given in connect descriptor
         *
         * jdbc:oracle:thin:@b3p-demoserver:1521:ORCL       werkt wel
         * jdbc:oracle:thin:@b3p-demoserver:1521:ORCL.GFO   werkt niet
         */
        String url = bron.getUrl();

        String user = bron.getGebruikersnaam();
        String passw = bron.getWachtwoord();

        String sql = "SELECT DISTINCT(PROJECTNAAM) FROM " + tabel + " ORDER BY PROJECTNAAM";

        if (bron != null) {
            Connection conn = null;

            try {
                conn = DriverManager.getConnection(url, user, passw);

                if (conn != null) {
                    PreparedStatement doSQL = conn.prepareStatement(sql);

                    ResultSet rs = doSQL.executeQuery();
                    while (rs.next()) {
                        String projectnaam = rs.getString(1);
                        projecten.add(projectnaam);
                    }
                }

            } catch (SQLException ex) {
                logger.error("Ophalen redliningprojecten mislukt: " + ex);
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
        }

        return projecten;
    }

    /* List van Features ophalen met een Filter op projectnaam */
    private List<Feature> getFeaturesInProject(Gegevensbron gb, String projectnaam) throws IOException, Exception {
        List projecten = new ArrayList();

        DataStore ds = gb.getBron().toDatastore();
        FeatureCollection fc = null;
        FeatureIterator it = null;

        try {
            FeatureStore fs = (FeatureStore) ds.getFeatureSource(gb.getAdmin_tabel());
            fc = fs.getFeatures(CQL.toFilter("projectnaam = '" + projectnaam + "'"));
            it = fc.features();

            while (it.hasNext()) {
                Feature f = (Feature) it.next();

                if (f != null) {
                    projecten.add(f);
                }
            }
        } finally {
            ds.dispose();

            if (fc != null) {
                fc.close(it);
            }
        }

        return projecten;
    }

    private void populateRedliningObject(DynaValidatorForm dynaForm, Redlining rl, HttpServletRequest request) {

        String groepnaam = dynaForm.getString("groepnaam");
        String projectnaam = dynaForm.getString("projectnaam");
        String new_projectnaam = dynaForm.getString("new_projectnaam");
        String ontwerp = dynaForm.getString("ontwerp");
        String opmerking = dynaForm.getString("opmerking");

        if (new_projectnaam != null && !new_projectnaam.equals("") && new_projectnaam.length() > 0) {
            rl.setProjectnaam(new_projectnaam);
        } else {
            rl.setProjectnaam(projectnaam);
        }

        rl.setGroepnaam(groepnaam);
        rl.setOntwerp(ontwerp);
        rl.setOpmerking(opmerking);

        try {
            Geometry geom = DataStoreUtil.createGeomFromWKTString(dynaForm.getString("wkt"));
            rl.setThe_geom(geom);
        } catch (Exception ex) {
            logger.error("Fout tijdens omzetten wkt voor redlining: ", ex);
        }
    }
}