package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.admindata.CollectAdmindata;
import nl.b3p.gis.viewer.db.CMSPagina;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.UserKaartlaag;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.struts.BaseHibernateAction;
import nl.b3p.wms.capabilities.Layer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public abstract class BaseGisAction extends BaseHibernateAction {

    private static final Log logger = LogFactory.getLog(BaseGisAction.class);
    public static final String URL_AUTH = "code";
    public static final String APP_AUTH = "appCode";
    protected static final double DEFAULTTOLERANCE = 5.0;
    protected static final String ACKNOWLEDGE_MESSAGES = "acknowledgeMessages";

    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        String code = null;
        if (gp != null) {
            code = gp.getCode();
        }
        // zet kaartenbalie url
        request.setAttribute("kburl", HibernateUtil.createPersonalKbUrl(code));
        request.setAttribute("kbcode", code);

        String organizationcode = CollectAdmindata.getOrganizationCode(request);
        if (organizationcode != null && organizationcode.length() > 0) {
            request.setAttribute("organizationcode", organizationcode);
        }

    }

    /**
     * Haal alle themas op uit de database door middel van een in het request meegegeven thema id comma seperated list.
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     *
     * @return Themas
     *
     * @see Themas
     */
    protected ArrayList getThemas(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request) {
        String themaids = (String) request.getParameter("themaid");
        if (themaids == null || themaids.length() == 0) {
            return null;
        }
        String[] ids = themaids.split(",");
        ArrayList themas = null;
        for (int i = 0; i < ids.length; i++) {
            Themas t = getThema(ids[i], request);
            if (t != null) {
                if (themas == null) {
                    themas = new ArrayList();
                }
                themas.add(t);
            }
        }

        if (themas != null && themas.size() > 0)
            Collections.sort(themas);

        return themas;
    }

    /**
     * Haal een Thema op uit de database door middel van een in het request meegegeven thema id.
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     *
     * @return Themas
     *
     * @see Themas
     */
    protected Themas getThema(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request) {
        String themaid = (String) request.getParameter("themaid");
        return getThema(themaid, request);
    }

    protected Gegevensbron getGegevensbron(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request) {
        String bronId = (String) request.getParameter("themaid");

        return getGegevensbron(bronId, request);
    }

    private Gegevensbron getGegevensbron(String bronId, HttpServletRequest request) {
        Gegevensbron gb = SpatialUtil.getGegevensbron(bronId);

        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            return gb;
        }

        // Zoek layers die via principal binnen komen
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        if (user == null) {
            return null;
        }
        List layersFromRoles = user.getLayerNames(false);
        if (layersFromRoles == null) {
            return null;
        }

        return gb;
    }

    protected Themas getThema(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, boolean analysethemaid) {
        String themaid = "";
        if (analysethemaid) {
            themaid = (String) request.getParameter("analysethemaid");
        } else {
            themaid = (String) request.getParameter("themaid");
        }
        return getThema(themaid, request);
    }

    /**
     * Get the thema en doe wat checks
     */
    private Themas getThema(String themaid, HttpServletRequest request) {
        Themas t = SpatialUtil.getThema(themaid);

        if (t == null)
            return null;

        /*
        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            logger.debug("No kb login required, thema: " + t == null ? "<null>" : t.getNaam());
            return t;
        }

        // Zoek layers die via principal binnen komen
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        if (user == null) {
            logger.debug("No user found, thema: " + t == null ? "<null>" : t.getNaam());
            return null;
        }
        List layersFromRoles = user.getLayerNames(false);
        if (layersFromRoles == null) {
            logger.debug("No layers found, thema: " + t == null ? "<null>" : t.getNaam());
            return null;
        }

        // Check de rechten op alle layers uit het thema
        if (!checkThemaLayers(t, layersFromRoles)) {
            logger.debug("No rights for layers found, thema: " + t == null ? "<null>" : t.getNaam());
            return null;
        }
        */

        return t;
    }

    /**
     * Indien een cluster wordt meegegeven dan voegt deze functie ook de layers
     * die niet als thema geconfigureerd zijn, maar toch als role aan de principal
     * zijn meegegeven als dummy thema toe. Als dit niet de bedoeling is dan
     * dient null als cluster meegegeven te worden.
     *
     * @param locatie
     * @param request
     * @return
     */
    protected List getValidThemas(boolean locatie, List ctl, HttpServletRequest request) {
        List configuredThemasList = SpatialUtil.getValidThemas(locatie);

        List layersFromRoles = null;
        // Zoek layers die via principal binnen komen
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        if (user != null) {
            layersFromRoles = user.getLayerNames(false);
        }
        if (layersFromRoles == null) {
            return null;
        }

        // Voeg alle themas toe die layers hebben die volgens de rollen
        // acceptabel zijn (voldoende rechten dus).
        List layersFound = new ArrayList();
        List checkedThemaList = new ArrayList();
        if (configuredThemasList != null) {
            Iterator it2 = configuredThemasList.iterator();
            while (it2.hasNext()) {
                Themas t = (Themas) it2.next();

                // Als geen check via kaartenbalie dan alle layers doorgeven
                if (checkThemaLayers(t, layersFromRoles)
                        || !HibernateUtil.isCheckLoginKaartenbalie()) {
                    checkedThemaList.add(t);
                    layersFound.add(t.getWms_layers_real());
                }
            }
        }

        // als alleen configureerde layers getoond mogen worden,
        // dan hier stoppen
        if (!HibernateUtil.isUseKaartenbalieCluster()) {
            return checkedThemaList;
        }

        //zoek of maak een cluster aan voor als er kaarten worden gevonden die geen thema hebben.
        Clusters c = SpatialUtil.getDefaultCluster();
        if (c == null) {
            c = new Clusters();
            c.setNaam(HibernateUtil.getKaartenbalieCluster());
            c.setParent(null);
        }

        Iterator it = layersFromRoles.iterator();
        int tid = 100000;
        ArrayList extraThemaList = new ArrayList();
        // Kijk welke lagen uit de rollen nog niet zijn toegevoegd
        // en voeg deze alsnog toe via dummy thema en cluster.
        while (it.hasNext()) {
            String layer = (String) it.next();
            if (layersFound.contains(layer)) {
                continue;
            }

            // Layer bestaat nog niet dus aanmaken
            Layer l = user.getLayer(layer);
            if (l != null) {
                Themas t = new Themas();
                t.setNaam(l.getTitle());
                t.setId(new Integer(tid++));
                if (user.hasLegendGraphic(l)) {
                    t.setWms_legendlayer_real(layer);
                }
                if ("1".equalsIgnoreCase(l.getQueryable())) {
                    t.setWms_querylayers_real(layer);
                }
                t.setWms_layers_real(layer);
                t.setCluster(c);
                // voeg extra laag als nieuw thema toe
                extraThemaList.add(t);
            }
        }
        if (extraThemaList.size() > 0) {
            if (ctl == null) {
                ctl = new ArrayList();
            }
            ctl.add(c);
            for (int i = 0; i < extraThemaList.size(); i++) {
                checkedThemaList.add(extraThemaList.get(i));
            }
        }

        return checkedThemaList;
    }

    /**
     * Indien een cluster wordt meegegeven dan voegt deze functie ook de layers
     * die niet als thema geconfigureerd zijn, maar toch als role aan de principal
     * zijn meegegeven als dummy thema toe. Als dit niet de bedoeling is dan
     * dient null als cluster meegegeven te worden.
     *
     * @param locatie
     * @param request
     * @return
     */
    protected List getValidUserThemas(boolean locatie, List ctl, HttpServletRequest request) {
        List configuredThemasList = SpatialUtil.getValidThemas(locatie);

        List layersFromRoles = null;
        // Zoek layers die via principal binnen komen
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        if (user != null) {
            layersFromRoles = user.getLayerNames(false);
        }
        if (layersFromRoles == null) {
            return null;
        }

        /* ophalen user kaartlagen om custom boom op te bouwen */
        List<UserKaartlaag> lagen = SpatialUtil.getUserKaartLagen(user.getCode());

        // Voeg alle themas toe die layers hebben die volgens de rollen
        // acceptabel zijn (voldoende rechten dus).
        List layersFound = new ArrayList();
        List checkedThemaList = new ArrayList();
        if (configuredThemasList != null) {
            Iterator it2 = configuredThemasList.iterator();
            while (it2.hasNext()) {
                Themas t = (Themas) it2.next();

                /* controleren of thema in user kaartlagen voorkomt */
                if (lagen != null && lagen.size() > 0) {
                    boolean isInList = false;
                    for (UserKaartlaag laag : lagen) {
                        if (laag.getThemaid() == t.getId()) {
                            isInList = true;
                        }
                    }

                    if (!isInList) {
                        continue;
                    }
                }

                // Als geen check via kaartenbalie dan alle layers doorgeven
                if (checkThemaLayers(t, layersFromRoles)
                        || !HibernateUtil.isCheckLoginKaartenbalie()) {
                    checkedThemaList.add(t);
                    layersFound.add(t.getWms_layers_real());
                }
            }
        }

        // als alleen configureerde layers getoond mogen worden,
        // dan hier stoppen
        if (!HibernateUtil.isUseKaartenbalieCluster()) {
            return checkedThemaList;
        }

        //zoek of maak een cluster aan voor als er kaarten worden gevonden die geen thema hebben.
        Clusters c = SpatialUtil.getDefaultCluster();
        if (c == null) {
            c = new Clusters();
            c.setNaam(HibernateUtil.getKaartenbalieCluster());
            c.setParent(null);
        }

        Iterator it = layersFromRoles.iterator();
        int tid = 100000;
        ArrayList extraThemaList = new ArrayList();
        // Kijk welke lagen uit de rollen nog niet zijn toegevoegd
        // en voeg deze alsnog toe via dummy thema en cluster.
        while (it.hasNext()) {
            String layer = (String) it.next();
            if (layersFound.contains(layer)) {
                continue;
            }

            // Layer bestaat nog niet dus aanmaken
            Layer l = user.getLayer(layer);
            if (l != null) {
                Themas t = new Themas();
                t.setNaam(l.getTitle());
                t.setId(new Integer(tid++));
                if (user.hasLegendGraphic(l)) {
                    t.setWms_legendlayer_real(layer);
                }
                if ("1".equalsIgnoreCase(l.getQueryable())) {
                    t.setWms_querylayers_real(layer);
                }
                t.setWms_layers_real(layer);
                t.setCluster(c);
                // voeg extra laag als nieuw thema toe
                extraThemaList.add(t);
            }
        }
        if (extraThemaList.size() > 0) {
            if (ctl == null) {
                ctl = new ArrayList();
            }
            ctl.add(c);
            for (int i = 0; i < extraThemaList.size(); i++) {
                checkedThemaList.add(extraThemaList.get(i));
            }
        }

        return checkedThemaList;
    }

    /**
     * Voeg alle layers samen voor een thema en controleer of de gebruiker
     * voor alle layers rechten heeft. Zo nee, thema niet toevoegen.
     * @param t
     * @param request
     * @return
     */
    protected boolean checkThemaLayers(Themas t, List acceptableLayers) {
        if (t == null || acceptableLayers == null) {
            return false;
        }
        String wmsls = t.getWms_layers_real();
        if (wmsls == null || wmsls.length() == 0) {
            return false;
        }

        // Dit is te streng, alleen op wms layer checken
//        String wmsqls = t.getWms_querylayers_real();
//        if (wmsqls!=null && wmsqls.length()>0)
//            wmsls += "," + wmsqls;
//        String wmslls = t.getWms_legendlayer_real();
//        if (wmslls!=null && wmslls.length()>0)
//            wmsls += "," + wmslls;

        String[] wmsla = wmsls.split(",");
        for (int i = 0; i < wmsla.length; i++) {
            if (!acceptableLayers.contains(wmsla[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Een protected methode het object thema ophaalt dat hoort bij een bepaald id.
     *
     * @param identifier String which identifies the object thema to be found.
     *
     * @return a Themas object representing the object thema.
     *
     */
    protected Themas getObjectThema(String identifier) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Themas objectThema = null;
        try {
            int id = Integer.parseInt(identifier);
            objectThema = (Themas) sess.get(Themas.class, new Integer(id));
        } catch (NumberFormatException nfe) {
            objectThema = (Themas) sess.get(Themas.class, identifier);
        }
        return objectThema;
    }

    protected List getTekstBlokken(Integer cmsPageId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List tekstBlokken = sess.createQuery("from Tekstblok where cms_pagina = :id"
                + " order by volgordenr, cdate").setParameter("id", cmsPageId).list();

        return tekstBlokken;
    }
    
    protected CMSPagina getCMSPage(Integer pageID) {
        if (pageID == null || pageID < 1) {
            return null;
        }
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        return (CMSPagina) sess.get(CMSPagina.class, pageID);
    }
}
