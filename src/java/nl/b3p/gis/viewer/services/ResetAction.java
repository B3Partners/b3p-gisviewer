package nl.b3p.gis.viewer.services;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.BaseGisAction;
import nl.b3p.wms.capabilities.Roles;
import nl.b3p.zoeker.configuratie.Bron;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.*;
import org.apache.struts.validator.DynaValidatorForm;
import org.securityfilter.filter.SecurityFilter;

public class ResetAction extends BaseGisAction {

    private static final Log logger = LogFactory.getLog(ResetAction.class);

    protected static final String CACHE = "cache";
    protected static final String CACHE_CONFIG = "configCache";
    protected static final String OPZOEKLIJST = "opzoeklijst";
    protected static final String LOGIN = "login";

    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(CACHE);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setDefaultMessageKey("reset.cache");
        hibProp.setAlternateForwardName(LOGIN);
        map.put(CACHE, hibProp);

        hibProp = new ExtendedMethodProperties(CACHE_CONFIG);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setDefaultMessageKey("reset.configcache");
        hibProp.setAlternateForwardName(LOGIN);
        map.put(CACHE_CONFIG, hibProp);

        hibProp = new ExtendedMethodProperties(OPZOEKLIJST);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setDefaultMessageKey("reset.opzoeklijst");
        hibProp.setAlternateForwardName(LOGIN);
        map.put(OPZOEKLIJST, hibProp);

        return map;
    }

    public ActionForward cache(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request, true);

        String lcvs = FormUtils.nullIfEmpty(request.getParameter(Bron.LIFECYCLE_CACHE_PARAM));

        if (user == null || !user.isInRole(Roles.ADMIN)) {
            SecurityFilter.saveRequestInformation(request);
            addAlternateMessage(mapping, request, null, " gebruiker heeft onvoldoende rechten om te resetten.");

            return getAlternateForward(mapping, request);
        }

        if (lcvs == null)
            lcvs = "0";

        /* wfs en wms cache legen */
        try {
            long lcl = Long.parseLong(lcvs);

            Bron.setDataStoreLifecycle(lcl);
            Bron.flushWfsCache();
            GisSecurityRealm.flushSPCache();
        } catch (NumberFormatException nfe) {
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        createLists(dynaForm, request);

        logger.info("Rechten zijn gereset voor de gisviewer.");

        return getDefaultForward(mapping, request);
    }

    public ActionForward configCache(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request, true);

        String lcvs = FormUtils.nullIfEmpty(request.getParameter(Bron.LIFECYCLE_CACHE_PARAM));

        if (user == null || !user.isInRole(Roles.ADMIN)) {
            SecurityFilter.saveRequestInformation(request);
            addAlternateMessage(mapping, request, null, " gebruiker heeft onvoldoende rechten om te resetten.");

            return getAlternateForward(mapping, request);
        }

        if (lcvs == null) {
            lcvs = "0";
        }

        /* wfs en wms cache legen */
        try {
            long lcl = Long.parseLong(lcvs);

            Bron.setDataStoreLifecycle(lcl);
            Bron.flushWfsCache();
            GisSecurityRealm.flushSPCache();
        } catch (NumberFormatException nfe) {
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        createLists(dynaForm, request);

        logger.info("Rechten zijn gereset voor de gisviewerconfig.");

        /* laat beheerder opnieuw inloggen zodat nieuwe schema wordt
         * opgehaald */
        HttpSession session = request.getSession();
        session.invalidate();

        return getDefaultForward(mapping, request);
    }

    public ActionForward opzoeklijst(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request, true);

        if (user == null || !user.isInRole(Roles.ADMIN)) {
            SecurityFilter.saveRequestInformation(request);
            addAlternateMessage(mapping, request, null, " gebruiker heeft onvoldoende rechten om te resetten.");

            return getAlternateForward(mapping, request);
        }
        
        ZoekConfiguratie.flushCachedResultListCache();

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        createLists(dynaForm, request);

        logger.info("Opzoeklijsten zijn gereset.");

        return getDefaultForward(mapping, request);
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        return mapping.findForward(SUCCESS);
    }
}
