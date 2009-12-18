/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 * 
 * This file is part of B3P Gisviewer.
 * 
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 *
 * @author Chris
 */
public class ConfigThemaAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigThemaAction.class);
    private static final String REFRESHLISTS = "refreshLists";

    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();
        ExtendedMethodProperties crudProp = new ExtendedMethodProperties(REFRESHLISTS);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(REFRESHLISTS, crudProp);
        return map;
    }

    public ActionForward refreshLists(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        createLists(dynaForm, request);
        return mapping.findForward(SUCCESS);

    }

    protected Themas getThema(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("themaID"));
        Themas t = null;
        if (id == null && createNew) {
            t = new Themas();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            t = (Themas) sess.get(Themas.class, id);
        }
        return t;
    }

    protected Themas getFirstThema() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Themas order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Themas) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        super.createLists(dynaForm, request);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("allThemas", sess.createQuery("from Themas order by belangnr").list());
        request.setAttribute("allClusters",
                sess.createQuery("from Clusters where default_cluster=:defaultCluster order by naam").setBoolean("defaultCluster", false).list());
        request.setAttribute("listConnecties", sess.createQuery("from Connecties").list());
        request.setAttribute("listValidGeoms", SpatialUtil.VALID_GEOMS);

        Themas t = getThema(dynaForm, false);
        Connecties c = null;
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        int cId = -1;
        try {
            cId = Integer.parseInt(dynaForm.getString("connectie"));
        } catch (NumberFormatException nfe) {
            log.debug("No connection id found in form, input: " + dynaForm.getString("connectie"));
        }
        if (cId >= 0) {
            c = (Connecties) sess.get(Connecties.class, cId);
        }
        if (c == null) {
            c = user.getKbWfsConnectie();
        }
        //maak lijsten die iets te maken hebben met de admin/spatial_data
        List tns = ConfigListsUtil.getPossibleFeatures(c);
        /*if (t != null) {
        String sptn = t.getAdmin_tabel();
        if (sptn != null && !tns.contains(sptn)) {
        tns.add(sptn);
        }
        String atn = t.getSpatial_tabel();
        if (atn != null && !tns.contains(atn)) {
        tns.add(atn);
        }
        }*/
        request.setAttribute("listTables", tns);

        String adminTable = null;
        String spatialTable = null;
        adminTable = FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel"));
        spatialTable = FormUtils.nullIfEmpty(dynaForm.getString("spatial_tabel"));
        if (adminTable == null && t != null) {
            adminTable = t.getAdmin_tabel();
        }
        if (spatialTable == null && t != null) {
            spatialTable = t.getSpatial_tabel();
        }
        if (adminTable != null) {
            List atc = ConfigListsUtil.getPossibleAttributes(c, adminTable);
            request.setAttribute("listAdminTableColumns", atc);
        }
        if (spatialTable != null) {
            List stc = ConfigListsUtil.getPossibleAttributes(c, spatialTable);
            request.setAttribute("listSpatialTableColumns", stc);
        }
        if (user != null) {
            List lns = user.getLayers(false, true);
            /*if (t != null) {
            String wlr = t.getWms_layers_real();
            if (wlr != null && !lns.contains(wlr)) {
            lns.add(wlr);
            }
            String wqr = t.getWms_querylayers_real();
            if (wqr != null && !lns.contains(wqr)) {
            lns.add(wqr);
            }
            }*/
            request.setAttribute("listLayers", lns);
            List llns = user.getLayers(true, true);
            /*if (t != null) {
            String wllr = t.getWms_legendlayer_real();
            if (wllr != null && !lns.contains(wllr)) {
            lns.add(wllr);
            }
            }*/
            request.setAttribute("listLegendLayers", llns);
        }
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        populateThemasForm(t, dynaForm, request);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        populateThemasForm(t, dynaForm, request);
        return super.edit(mapping, dynaForm, request, response);
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

        Themas t = getThema(dynaForm, true);
        if (t == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateThemasObject(dynaForm, t, request);

        sess.saveOrUpdate(t);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(t);


        populateThemasForm(t, dynaForm, request);
        return super.save(mapping, dynaForm, request, response);
    }

    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Themas t = getThema(dynaForm, false);
        if (t == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(t);
        sess.flush();

        return super.delete(mapping, dynaForm, request, response);
    }

    private void populateThemasForm(Themas t, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (t == null) {
            return;
        }
        dynaForm.set("themaID", Integer.toString(t.getId()));
        dynaForm.set("themaCode", t.getCode());
        dynaForm.set("naam", t.getNaam());
        dynaForm.set("metadatalink", t.getMetadata_link());

        String valConnectie = "-1";
        String adminTable = t.getAdmin_tabel();
        if (adminTable != null && adminTable.length() > 0) {
            // adminTable kan alleen een waarde hebben, als er een connectie is.
            valConnectie = "0";
            if (t.getConnectie() != null) {
                valConnectie = Integer.toString(t.getConnectie().getId());
            }
        }
        dynaForm.set("connectie", valConnectie);
        dynaForm.set("belangnr", FormUtils.IntToString(t.getBelangnr()));
        String valCluster = "";
        if (t.getCluster() != null) {
            valCluster = Integer.toString(t.getCluster().getId().intValue());
        }
        dynaForm.set("clusterID", valCluster);
        dynaForm.set("opmerkingen", t.getOpmerkingen());
        dynaForm.set("analyse_thema", new Boolean(t.isAnalyse_thema()));
        dynaForm.set("locatie_thema", new Boolean(t.isLocatie_thema()));

        dynaForm.set("admin_tabel_opmerkingen", t.getAdmin_tabel_opmerkingen());
        dynaForm.set("admin_tabel", t.getAdmin_tabel());
        dynaForm.set("admin_pk", t.getAdmin_pk());
        dynaForm.set("admin_pk_complex", new Boolean(t.isAdmin_pk_complex()));
        dynaForm.set("admin_spatial_ref", t.getAdmin_spatial_ref());

        dynaForm.set("admin_query", "");
        if (t.getConnectie() != null) {
            if (t.getConnectie().getType().equals("jdbc")) {
                dynaForm.set("admin_query", t.getAdmin_query());
            } else if (t.getAdmin_query() != null && !t.getAdmin_query().startsWith("select")) {
                dynaForm.set("admin_query", t.getAdmin_query());
            }
        }

        dynaForm.set("spatial_tabel_opmerkingen", t.getSpatial_tabel_opmerkingen());
        dynaForm.set("spatial_tabel", t.getSpatial_tabel());
        dynaForm.set("spatial_pk", t.getSpatial_pk());
        dynaForm.set("spatial_pk_complex", new Boolean(t.isSpatial_pk_complex()));
        dynaForm.set("spatial_admin_ref", t.getSpatial_admin_ref());
        dynaForm.set("wms_url", t.getWms_url());
        dynaForm.set("wms_layers", t.getWms_layers());
        dynaForm.set("wms_layers_real", t.getWms_layers_real());
        dynaForm.set("wms_querylayers", t.getWms_querylayers());
        dynaForm.set("wms_querylayers_real", t.getWms_querylayers_real());
        dynaForm.set("wms_legendlayer", t.getWms_legendlayer());
        dynaForm.set("wms_legendlayer_real", t.getWms_legendlayer_real());
        dynaForm.set("thema_maptip", t.getMaptipstring());
        dynaForm.set("update_frequentie_in_dagen", FormUtils.IntegerToString(t.getUpdate_frequentie_in_dagen()));
        dynaForm.set("view_geomtype", t.getView_geomtype());
        dynaForm.set("visible", new Boolean(t.isVisible()));

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List themadataobjecten = sess.createQuery("select kolomnaam from ThemaData where thema = :thema").setEntity("thema", t).list();
        dynaForm.set("themadataobjecten", themadataobjecten.toArray(new String[themadataobjecten.size()]));
    }

    private void populateThemasObject(DynaValidatorForm dynaForm, Themas t, HttpServletRequest request) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        t.setCode(FormUtils.nullIfEmpty(dynaForm.getString("themaCode")));
        t.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        t.setMetadata_link(FormUtils.nullIfEmpty(dynaForm.getString("metadatalink")));
        Connecties conn = null;
        int connId = -1;
        try {
            connId = Integer.parseInt(dynaForm.getString("connectie"));
        } catch (NumberFormatException nfe) {
            log.debug("No connection id found in form, input: " + dynaForm.getString("connectie"));
        }
        if (connId > 0) {
            conn = (Connecties) sess.get(Connecties.class, connId);
        }
        t.setConnectie(conn);
        if (dynaForm.getString("belangnr") != null && dynaForm.getString("belangnr").length() > 0) {
            t.setBelangnr(Integer.parseInt(dynaForm.getString("belangnr")));
        }
        t.setOpmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("opmerkingen")));
        Boolean b = (Boolean) dynaForm.get("analyse_thema");
        t.setAnalyse_thema(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("locatie_thema");
        t.setLocatie_thema(b == null ? false : b.booleanValue());
        t.setAdmin_tabel_opmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel_opmerkingen")));
        t.setAdmin_tabel(FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel")));
        t.setAdmin_pk(FormUtils.nullIfEmpty(dynaForm.getString("admin_pk")));
        b = (Boolean) dynaForm.get("admin_pk_complex");
        t.setAdmin_pk_complex(b == null ? false : b.booleanValue());
        t.setAdmin_spatial_ref(FormUtils.nullIfEmpty(dynaForm.getString("admin_spatial_ref")));
        t.setAdmin_query(FormUtils.nullIfEmpty(dynaForm.getString("admin_query")));
        t.setSpatial_tabel_opmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("spatial_tabel_opmerkingen")));
        t.setSpatial_tabel(FormUtils.nullIfEmpty(dynaForm.getString("spatial_tabel")));
        t.setSpatial_pk(FormUtils.nullIfEmpty(dynaForm.getString("spatial_pk")));
        b = (Boolean) dynaForm.get("spatial_pk_complex");
        t.setSpatial_pk_complex(b == null ? false : b.booleanValue());
        t.setSpatial_admin_ref(FormUtils.nullIfEmpty(dynaForm.getString("spatial_admin_ref")));
        t.setMaptipstring(FormUtils.nullIfEmpty(dynaForm.getString("thema_maptip")));
        t.setWms_url(FormUtils.nullIfEmpty(dynaForm.getString("wms_url")));
        //komma separated layers
        t.setWms_layers(FormUtils.nullIfEmpty(dynaForm.getString("wms_layers")));
        t.setWms_layers_real(FormUtils.nullIfEmpty(dynaForm.getString("wms_layers_real")));
        //komma separated layers
        t.setWms_querylayers(FormUtils.nullIfEmpty(dynaForm.getString("wms_querylayers")));
        t.setWms_querylayers_real(FormUtils.nullIfEmpty(dynaForm.getString("wms_querylayers_real")));
        //one layer to create a wms legend image
        t.setWms_legendlayer(FormUtils.nullIfEmpty(dynaForm.getString("wms_legendlayer")));
        t.setWms_legendlayer_real(FormUtils.nullIfEmpty(dynaForm.getString("wms_legendlayer_real")));
        t.setUpdate_frequentie_in_dagen(FormUtils.StringToInteger(dynaForm.getString("update_frequentie_in_dagen")));
        t.setView_geomtype(FormUtils.nullIfEmpty(dynaForm.getString("view_geomtype")));
        b = (Boolean) dynaForm.get("visible");
        t.setVisible(b == null ? false : b.booleanValue());

        int cId = -1;
        try {
            cId = Integer.parseInt(dynaForm.getString("clusterID"));
        } catch (NumberFormatException ex) {
            log.error("Illegal Cluster id", ex);
        }
        Clusters c = (Clusters) sess.get(Clusters.class, new Integer(cId));
        t.setCluster(c);
    }
}
