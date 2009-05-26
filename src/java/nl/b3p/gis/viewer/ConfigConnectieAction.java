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
import nl.b3p.gis.viewer.db.Connecties;
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
public class ConfigConnectieAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigConnectieAction.class);

    protected Connecties getConnectie(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("id"));
        Connecties c = null;
        if (id == null && createNew) {
            c = new Connecties();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            c = (Connecties) sess.get(Connecties.class, id);
        }
        return c;
    }

    protected Connecties getFirstConnectie() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Connecties order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Connecties) cs.get(0);
        }
        return null;
    }

    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("allConnecties", sess.createQuery("from Connecties order by naam").list());
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Connecties c = getConnectie(dynaForm, false);
        if (c == null) {
            c = getFirstConnectie();
        }
        populateConnectieForm(c, dynaForm, request);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Connecties c = getConnectie(dynaForm, false);
        if (c == null) {
            c = getFirstConnectie();
        }
        populateConnectieForm(c, dynaForm, request);
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

        Connecties c = getConnectie(dynaForm, true);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateConnectieObject(dynaForm, c, request);

        sess.saveOrUpdate(c);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(c);
        populateConnectieForm(c, dynaForm, request);

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

        Connecties c = getConnectie(dynaForm, false);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(c);
        sess.flush();

        return super.delete(mapping, dynaForm, request, response);
    }

    private void populateConnectieForm(Connecties c, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (c == null) {
            return;
        }
        dynaForm.set("id", Integer.toString(c.getId()));
        dynaForm.set("naam", c.getNaam());
        dynaForm.set("url", c.getConnectie_url());
        dynaForm.set("gebruikersnaam", c.getGebruikersnaam());
        dynaForm.set("wachtwoord", c.getWachtwoord());
    }

    private void populateConnectieObject(DynaValidatorForm dynaForm, Connecties c, HttpServletRequest request) {
        if (FormUtils.nullIfEmpty(dynaForm.getString("id"))!=null){
            c.setId(new Integer (dynaForm.getString("id")));
        }
        c.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        c.setConnectie_url(FormUtils.nullIfEmpty(dynaForm.getString("url")));
        c.setGebruikersnaam(FormUtils.nullIfEmpty(dynaForm.getString("gebruikersnaam")));
        c.setWachtwoord(FormUtils.nullIfEmpty(dynaForm.getString("wachtwoord")));
    }
}
