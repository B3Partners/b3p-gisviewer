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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.services.HibernateUtil;
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
public class ConfigClusterAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigThemaAction.class);

    protected Clusters getCluster(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("clusterID"));
        Clusters c = null;
        if (id == null && createNew) {
            c = new Clusters();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            c = (Clusters) sess.get(Clusters.class, id);
        }
        return c;
    }

    protected Clusters getFirstCluster() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Clusters order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Clusters) cs.get(0);
        }
        return null;
    }

    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("allClusters", sess.createQuery("from Clusters order by naam").list());
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Clusters c = getCluster(dynaForm, false);
        if (c == null) {
            c = getFirstCluster();
        }
        populateClustersForm(c, dynaForm, request);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Clusters c = getCluster(dynaForm, false);
        if (c == null) {
            c = getFirstCluster();
        }
        populateClustersForm(c, dynaForm, request);
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

        Clusters c = getCluster(dynaForm, true);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateClustersObject(dynaForm, c, request);

        sess.saveOrUpdate(c);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(c);
        populateClustersForm(c, dynaForm, request);

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

        Clusters c = getCluster(dynaForm, false);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(c);
        sess.flush();

        return super.delete(mapping, dynaForm, request, response);
    }

    private void populateClustersForm(Clusters c, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (c == null) {
            return;
        }
        dynaForm.set("clusterID", Integer.toString(c.getId().intValue()));
        dynaForm.set("naam", c.getNaam());
        dynaForm.set("omschrijving", c.getOmschrijving());
        dynaForm.set("belangnr", FormUtils.IntToString(c.getBelangnr()));
        dynaForm.set("metadatalink",c.getMetadatalink());
        dynaForm.set("default_cluster", new Boolean(c.isDefault_cluster()));
        dynaForm.set("hide_legend", new Boolean(c.isHide_legend()));
        dynaForm.set("hide_tree", new Boolean(c.isHide_tree()));
        dynaForm.set("background_cluster", new Boolean(c.isBackground_cluster()));
        dynaForm.set("extra_level", new Boolean(c.isExtra_level()));
        dynaForm.set("callable", new Boolean(c.isCallable()));
        dynaForm.set("default_visible", new Boolean (c.isDefault_visible()));
        String val = "";
        if (c.getParent() != null) {
            val = Integer.toString(c.getParent().getId().intValue());
        }
        dynaForm.set("parentID", val);
    }

    private void populateClustersObject(DynaValidatorForm dynaForm, Clusters c, HttpServletRequest request) {

        c.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        c.setOmschrijving(FormUtils.nullIfEmpty(dynaForm.getString("omschrijving")));
        if (dynaForm.getString("belangnr") != null && dynaForm.getString("belangnr").length() > 0) {
            c.setBelangnr(Integer.parseInt(dynaForm.getString("belangnr")));
        }
        c.setMetadatalink(FormUtils.nullIfEmpty(dynaForm.getString("metadatalink")));
        Boolean b = (Boolean) dynaForm.get("default_cluster");
        c.setDefault_cluster(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("hide_legend");
        c.setHide_legend(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("hide_tree");
        c.setHide_tree(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("background_cluster");
        c.setBackground_cluster(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("extra_level");
        c.setExtra_level(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("callable");
        c.setCallable(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("default_visible");
        c.setDefault_visible(b == null ? false : b.booleanValue());

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String parentID = FormUtils.nullIfEmpty(dynaForm.getString("parentID"));
        if (parentID != null) {
            int mId = 0;
            try {
                mId = Integer.parseInt(dynaForm.getString("parentID"));
            } catch (NumberFormatException ex) {
                log.error("Illegal parent id", ex);
            }
            Clusters m = (Clusters) sess.get(Clusters.class, new Integer(mId));
            c.setParent(m);
        }else{
            c.setParent(null);
        }
    }
}
