package nl.b3p.gis.viewer;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.db.Meldingen;
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
import org.apache.struts.action.ActionMessage;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class MeldingAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(MeldingAction.class);
    protected static final String PREPARE_MELDING = "prepareMelding";
    protected static final String SEND_MELDING = "sendMelding";
    protected static final String MELDINGFW = "melding";

    /**
     * Return een hashmap die een property koppelt aan een Action.
     *
     * @return Map hashmap met action properties.
     */
    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(PREPARE_MELDING);
        hibProp.setDefaultForwardName(MELDINGFW);
        hibProp.setAlternateForwardName(MELDINGFW);
        hibProp.setAlternateMessageKey("error.preparemelding.failed");
        map.put(PREPARE_MELDING, hibProp);

        hibProp = new ExtendedMethodProperties(SEND_MELDING);
        hibProp.setDefaultForwardName(MELDINGFW);
        hibProp.setDefaultMessageKey("message.sendmelding.success");
        hibProp.setAlternateForwardName(MELDINGFW);
        hibProp.setAlternateMessageKey("error.sendmelding.failed");
        map.put(SEND_MELDING, hibProp);

        return map;
    }

    protected Meldingen getMelding(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("meldingID"));
        Meldingen c = null;
        if (id == null && createNew) {
            c = new Meldingen();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            c = (Meldingen) sess.get(Meldingen.class, id);
        }
        return c;
    }

    protected Meldingen getFirstMelding() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Meldingen order by kenmerk").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Meldingen) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("allMeldingen", sess.createQuery("from Meldingen order by kenmerk").list());
    }

    private void useInstellingen(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        Map instellingen = getInstellingenMap(request);

        String[] mts = ((String) instellingen.get("meldingType")).split(",");
        request.setAttribute("meldingTypes", mts);
        String[] mss = ((String) instellingen.get("meldingStatus")).split(",");
        request.setAttribute("meldingStatus", mss);

        populateFromInstellingen(instellingen, dynaForm, request);
    }

    public ActionForward prepareMelding(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        dynaForm.initialize(mapping);
        useInstellingen(dynaForm, request);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward sendMelding(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        useInstellingen(dynaForm, request);

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            //addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Meldingen m = new Meldingen();
        populateMeldingenObject(dynaForm, m, request);
        long stamp = (new Date()).getTime() - 1290000000000l;
        String kenmerk = dynaForm.getString("prefixKenmerk") +
                Long.toString(stamp, 32);
        m.setKenmerk(kenmerk);
        m.setDatumOntvangst(new Date());

        Integer ggbId = (Integer) dynaForm.get("gegevensbron");
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Gegevensbron ggb = (Gegevensbron) sess.get(Gegevensbron.class, ggbId);
        DataStore ds = ggb.getBron().toDatastore();
        try {
            String typename = ggb.getAdmin_tabel();
            SimpleFeatureType ft = ds.getSchema(typename);
            SimpleFeature f = m.getFeature(ft);
            writeMelding(ds, f);
        } finally {
            ds.dispose();
        }

        populateMeldingenForm(m, dynaForm, request);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        ActionMessage amsg = new ActionMessage("melding.kenmerk", m.getKenmerk());
        addAttributeMessage(request, ACKNOWLEDGE_MESSAGES, amsg);

        return getDefaultForward(mapping, request);
    }

    private void writeMelding(DataStore dataStore2Write, SimpleFeature feature) throws IOException {
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

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Meldingen c = getMelding(dynaForm, false);
        if (c == null) {
            c = getFirstMelding();
        }
        populateMeldingenForm(c, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Meldingen c = getMelding(dynaForm, false);
        if (c == null) {
            c = getFirstMelding();
        }
        populateMeldingenForm(c, dynaForm, request);
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

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

        Meldingen c = getMelding(dynaForm, true);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateMeldingenObject(dynaForm, c, request);

        sess.saveOrUpdate(c);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(c);
        populateMeldingenForm(c, dynaForm, request);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Meldingen c = getMelding(dynaForm, false);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(c);
        sess.flush();

        dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private void populateMeldingenForm(Meldingen m, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        if (m == null) {
            return;
        }
        if (m.getId() != null) {
            dynaForm.set("meldingID", Integer.toString(m.getId().intValue()));
        }
        dynaForm.set("meldingTekst", m.getMeldingTekst());
        dynaForm.set("emailMelder", m.getEmailZender());
        dynaForm.set("adresMelder", m.getAdresZender());
        dynaForm.set("naamMelder", m.getNaamZender());
        dynaForm.set("meldingType", m.getMeldingType());
        dynaForm.set("meldingStatus", m.getMeldingStatus());
        dynaForm.set("meldingCommentaar", m.getMeldingCommentaar());
        dynaForm.set("kenmerk", m.getKenmerk());
    }

    private Map getInstellingenMap(HttpServletRequest request) throws Exception {

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Set roles = user.getRoles();

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = null;
        try {
            rollenPrio = configKeeper.getConfiguratie("rollenPrio", "rollen");
        } catch (Exception ex) {
            log.debug("Fout bij ophalen configKeeper configuratie: " + ex);
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
        if ((map == null) || (map.size() == 0)) {
            map = configKeeper.getConfigMap("default");
        }
        return map;
    }

    private void populateFromInstellingen(Map instellingen, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        String[] mss = ((String) instellingen.get("meldingStatus")).split(",");
        if (mss != null && mss.length > 0) {
            dynaForm.set("meldingStatus", mss[0]);
        }
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

    }

    private void populateMeldingenObject(DynaValidatorForm dynaForm, Meldingen m, HttpServletRequest request) {
        m.setMeldingTekst(dynaForm.getString("meldingTekst"));
        m.setEmailZender(dynaForm.getString("emailMelder"));
        m.setAdresZender(dynaForm.getString("adresMelder"));
        m.setNaamZender(dynaForm.getString("naamMelder"));
        m.setMeldingType(dynaForm.getString("meldingType"));
        m.setMeldingStatus(dynaForm.getString("meldingStatus"));
        m.setMeldingCommentaar(dynaForm.getString("meldingCommentaar"));
        m.setNaamOntvanger("balie");
        try {
            m.setTheGeom(DataStoreUtil.createGeomFromWKTString(dynaForm.getString("wkt")));
        } catch (Exception ex) {
            log.error("error converting wkt for melding: ", ex);
        }
    }
}
