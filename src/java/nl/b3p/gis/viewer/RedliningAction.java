package nl.b3p.gis.viewer;

import java.io.IOException;
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
import org.hibernate.Session;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.viewer.db.Redlining;
import org.apache.struts.action.ActionMessage;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Boy de Wit
 */
public class RedliningAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(RedliningAction.class);

    protected static final String PREPARE_REDLINING = "prepareRedlining";
    protected static final String SEND_REDLINING = "sendRedlining";
    protected static final String REDLINING_FORWARD = "redlining";

    /* Database gegevens worden uit gegevensbron gehaald echter zijn de
     * namen van de redlining tabellen hardcoded */
    protected static final String TABLE_RL_OBJECT = "redlining_object";
    protected static final String TABLE_RL_GROEP_PROJECT = "redlining_groep_project";
    protected static final String TABLE_RL_PROJECT = "redlining_project";

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

        return map;
    }

    protected Redlining getRedlining(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("redliningID"));
        Redlining rl = null;
        if (id == null && createNew) {
            rl = new Redlining();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            rl = (Redlining) sess.get(Redlining.class, id);
        }
        return rl;
    }

    protected Redlining getFirstRedlining() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Redlining order by id").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Redlining) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        //request.setAttribute("allMeldingen", sess.createQuery("from Meldingen order by kenmerk").list());
    }

    private void useInstellingen(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        Map instellingen = getInstellingenMap(request);

        if (instellingen != null) {

            /*
            if (instellingen.get("meldingType") != null) {
                String[] mts = ((String) instellingen.get("meldingType")).split(",");
                request.setAttribute("meldingTypes", mts);
            }

            if (instellingen.get("meldingStatus") != null) {
                String[] mss = ((String) instellingen.get("meldingStatus")).split(",");
                request.setAttribute("meldingStatus", mss);
            }*/
        }

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

        Integer ggbId = 201; //(Integer) dynaForm.get("gegevensbron");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Gegevensbron ggb = (Gegevensbron) sess.get(Gegevensbron.class, ggbId);
        DataStore ds = ggb.getBron().toDatastore();
        try {
            String typename = TABLE_RL_OBJECT; //ggb.getAdmin_tabel();
            SimpleFeatureType ft = ds.getSchema(typename);
            SimpleFeature f = rl.getFeature(ft);
            writeRedlining(ds, f);
        } finally {
            ds.dispose();
        }

        populateRedliningForm(rl, dynaForm, request);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    private void writeRedlining(DataStore dataStore2Write, SimpleFeature feature) throws IOException {
        String typename = feature.getFeatureType().getTypeName();
        FeatureWriter writer = dataStore2Write.getFeatureWriterAppend(typename, Transaction.AUTO_COMMIT);
        try {
            SimpleFeature newFeature = (SimpleFeature) writer.next();
            newFeature.setAttributes(feature.getAttributes());
            writer.write();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Redlining rl = getRedlining(dynaForm, false);

        if (rl == null) {
            rl = getFirstRedlining();
        }
        populateRedliningForm(rl, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Redlining rl = getRedlining(dynaForm, false);
        if (rl == null) {
            rl = getFirstRedlining();
        }

        populateRedliningForm(rl, dynaForm, request);
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Redlining rl = getRedlining(dynaForm, true);
        if (rl == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateRedliningObject(dynaForm, rl, request);

        sess.saveOrUpdate(rl);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(rl);
        populateRedliningForm(rl, dynaForm, request);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Redlining rl = getRedlining(dynaForm, false);
        if (rl == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(rl);
        sess.flush();

        dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private void populateRedliningForm(Redlining rl, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        if (rl == null) {
            return;
        }
        if (rl.getId() != null) {
            dynaForm.set("redliningID", Integer.toString(rl.getId().intValue()));
        }

        dynaForm.set("projectid", rl.getProjectid().toString());
        dynaForm.set("fillcolor", rl.getFillcolor());
    }

    private Map getInstellingenMap(HttpServletRequest request) throws Exception {

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Set roles = user.getRoles();

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

        Map map = configKeeper.getConfigMap(echteRol);
        if ((map == null) || (map.isEmpty())) {
            map = configKeeper.getConfigMap("default");
        }
        return map;
    }

    private void populateFromInstellingen(Map instellingen, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        String[] mss = null;

        /*
        dynaForm.set("welkomTekst", instellingen.get("meldingWelkomtekst"));
        dynaForm.set("prefixKenmerk", instellingen.get("meldingPrefix"));
        dynaForm.set("zendEmailMelder", instellingen.get("meldingEmailmelder"));
        dynaForm.set("layoutStylesheetMelder", instellingen.get("meldingLayoutEmailMelder"));
        dynaForm.set("naamBehandelaar", instellingen.get("meldingNaam"));
        dynaForm.set("emailBehandelaar", instellingen.get("meldingEmail"));
        dynaForm.set("zendEmailBehandelaar", instellingen.get("meldingEmailBehandelaar"));
        dynaForm.set("layoutStylesheetBehandelaar", instellingen.get("meldingLayoutEmailBehandelaar"));
        dynaForm.set("objectSoort", instellingen.get("meldingObjectSoort"));
        dynaForm.set("icoonTekentool", instellingen.get("meldingTekentoolIcoon"));
        dynaForm.set("opmerking", "Nog geen melding verstuurd!");
        dynaForm.set("gegevensbron", instellingen.get("meldingGegevensbron"));
        */

    }

    private void populateRedliningObject(DynaValidatorForm dynaForm, Redlining rl, HttpServletRequest request) {

        Integer projectid = FormUtils.StringToInteger(dynaForm.getString("projectid"));

        rl.setProjectid(projectid);
        rl.setFillcolor(dynaForm.getString("fillcolor"));
        rl.setOpmerking(dynaForm.getString("opmerking"));

        try {
            rl.setThe_geom(DataStoreUtil.createGeomFromWKTString(dynaForm.getString("wkt")));
        } catch (Exception ex) {
            logger.error("Fout tijdens omzetten wkt voor redlining: ", ex);
        }
    }
}
