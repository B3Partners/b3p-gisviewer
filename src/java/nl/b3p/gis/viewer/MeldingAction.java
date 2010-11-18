package nl.b3p.gis.viewer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.Meldingen;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import nl.b3p.gis.geotools.DataStoreUtil;

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
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.preparemelding.failed");
        map.put(PREPARE_MELDING, hibProp);

        hibProp = new ExtendedMethodProperties(SEND_MELDING);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateMessageKey("message.sendmelding.success");
        hibProp.setAlternateForwardName(FAILURE);
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

    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        request.setAttribute("meldingTypes", new String[]{"klacht", "Suggestie"});
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("allMeldingen", sess.createQuery("from Meldingen order by kenmerk").list());
    }

    public ActionForward prepareMelding(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        populateFromInstellingen(dynaForm);
        
        createLists(dynaForm, request);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(MELDINGFW);
    }

    public ActionForward sendMelding(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Meldingen m = new Meldingen();
        populateMeldingenObject(dynaForm, m, request);
        long now = (new Date()).getTime();
        m.setKenmerk(Long.toString(now, 32));
        m.setDatumOntvangst(new Date());
        dynaForm.set("opmerking", constructMessage(m));

        createLists(dynaForm, request);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(MELDINGFW);
    }

    private String constructMessage(Meldingen m) {
        String message = "Een bericht";

        Geometry geom = m.getTheGeom();
        if (geom != null) {
            Point p = geom.getCentroid();
            message += " voor locatie met RD-coordinaten (";
            message += (int) p.getX();
            message += ",";
            message += (int) p.getY();
            message += ")";
        }
        message += " is verzonden naar de beheerder.";
        message += " Uw referentienummer is: \"" + m.getKenmerk() +"\".";

        return message;

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

    private void populateMeldingenForm(Meldingen m, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (m == null) {
            return;
        }
        dynaForm.set("meldingID", Integer.toString(m.getId().intValue()));
        dynaForm.set("meldingTekst", m.getMeldingTekst());
        dynaForm.set("emailMelder", m.getEmailZender());
        dynaForm.set("adresMelder", m.getAdresZender());
        dynaForm.set("naamMelder", m.getNaamZender());
        dynaForm.set("meldingType", m.getMeldingType());
        dynaForm.set("meldingStatus", m.getMeldingStatus());
        dynaForm.set("meldingCommentaar", m.getMeldingCommentaar());
        dynaForm.set("kenmerk", m.getKenmerk());

        populateFromInstellingen(dynaForm);
    }

    private void populateFromInstellingen(DynaValidatorForm dynaForm) {
        dynaForm.set("welkomTekst", "");
        dynaForm.set("prefixKenmerk", "");
        dynaForm.set("zendEmailMelder", new Boolean(false));
        dynaForm.set("layoutStylesheetMelder", "");
        dynaForm.set("naamBehandelaar", "balie");
        dynaForm.set("emailBehandelaar", "");
        dynaForm.set("zendEmailBehandelaar", new Boolean(false));
        dynaForm.set("layoutStylesheetBehandelaar", "");
        dynaForm.set("objectSoort", "Point");
        dynaForm.set("icoonTekentool", "");
        dynaForm.set("opmerking", "Nog geen melding verstuurd!");
        dynaForm.set("gegevensbron", "1");

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
