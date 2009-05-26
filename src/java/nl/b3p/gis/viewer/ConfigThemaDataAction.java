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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.DataTypen;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.WaardeTypen;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.services.WfsUtil;
import nl.b3p.ogc.utils.OGCConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Query;
import org.hibernate.Session;
import org.w3c.dom.Element;

/**
 *
 * @author Chris
 */
public class ConfigThemaDataAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigThemaAction.class);
    protected static final String CHANGE = "change";
    protected static final String CREATEALLTHEMADATA = "createAllThemaData";
    private int DEFAULTBASISCOLUMNS = 6;

    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(CHANGE);
        crudProp.setDefaultForwardName(SUCCESS);
        map.put(CHANGE, crudProp);

        crudProp = new ExtendedMethodProperties(CREATEALLTHEMADATA);
        crudProp.setDefaultForwardName(SUCCESS);
        map.put(CREATEALLTHEMADATA, crudProp);
        return map;
    }

    protected ThemaData getThemaData(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("themaDataID"));
        ThemaData td = null;
        if (id == null && createNew) {
            td = new ThemaData();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            td = (ThemaData) sess.get(ThemaData.class, id);
        }
        return td;
    }

    protected ThemaData getFirstThemaData(Themas t) {
        if (t == null) {
            return null;
        }
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Query q = sess.createQuery("from ThemaData where thema.id = :themaID order by dataorder, label");
        List cs = q.setParameter("themaID", t.getId()).setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (ThemaData) cs.get(0);
        }
        ThemaData td = new ThemaData();
        td.setThema(t);
        return td;
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

    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        super.createLists(dynaForm, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("listThemas", sess.createQuery("from Themas where code in ('1', '2') order by belangnr").list());
        request.setAttribute("listWaardeTypen", sess.createQuery("from WaardeTypen order by naam").list());
        request.setAttribute("listDataTypen", sess.createQuery("from DataTypen order by naam").list());
        Themas t = null;
        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            t = getThema(dynaForm, false);
            if (t == null) {
                t = getFirstThema();
            }
        } else {
            t = td.getThema();
        }
        if (t == null) {
            return;
        }
        Query q = sess.createQuery("from ThemaData where thema.id = :themaID order by dataorder, label");
        request.setAttribute("listThemaData", q.setParameter("themaID", t.getId()).list());

        String connectieType = Connecties.TYPE_JDBC;
        if (SpatialUtil.validJDBCConnection(t)) {
            Connection conn =SpatialUtil.getJDBCConnection(t);
            request.setAttribute("listAdminTableColumns", SpatialUtil.getColumnNames(t.getAdmin_tabel(), conn));
        } else if (WfsUtil.validWfsConnection(t)) {
            connectieType = Connecties.TYPE_WFS;
            Connecties conn = WfsUtil.getWfsConnection(t,request);
            List elements = WfsUtil.getFeatureElements(conn, t.getAdmin_tabel());
            if (elements != null) {
                ArrayList elementsNames = new ArrayList();
                for (int i = 0; i < elements.size(); i++) {
                    Element e = (Element) elements.get(i);
                    elementsNames.add(e.getAttribute("name"));
                }
                request.setAttribute("listAdminTableColumns", elementsNames);
            }
        }
        request.setAttribute("connectieType", connectieType);
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            Themas t = getThema(dynaForm, false);
            if (t == null) {
                t = getFirstThema();
            }
            td = getFirstThemaData(t);
        }
        populateThemaDataForm(td, dynaForm, request);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward change(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        ThemaData td = getFirstThemaData(t);
        populateThemaDataForm(td, dynaForm, request);
        return super.unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward create(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        dynaForm.initialize(mapping);
        String val = "";
        if (t != null) {
            val = Integer.toString(t.getId());
        }
        dynaForm.set("themaID", val);
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request);
        return getDefaultForward(mapping, request);
    }

    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            td = getFirstThemaData(t);
        }
        populateThemaDataForm(td, dynaForm, request);
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

        ThemaData t = getThemaData(dynaForm, true);
        if (t == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateThemaDataObject(dynaForm, t, request);

        sess.saveOrUpdate(t);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(t);
        populateThemaDataForm(t, dynaForm, request);

        return super.save(mapping, dynaForm, request, response);
    }

    public ActionForward createAllThemaData(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List attributes = null;
        if (SpatialUtil.validJDBCConnection(t)){
            Connection conn = SpatialUtil.getJDBCConnection(t);
            attributes = SpatialUtil.getColumnNames(t.getAdmin_tabel(), conn);
            if (attributes != null && attributes.size() != 0) {
                //als het attribuut van het type geometry is moet hij niet worden gebruikt
                for (int i = 0; i < attributes.size(); i++) {
                    String attr = (String) attributes.get(i);
                    int type = SpatialUtil.getColumnDatatype(t, attr, conn);
                    if (type == java.sql.Types.OTHER) {
                        /*if (attributes==null){
                        attributes=new ArrayList();
                        attributes.add(attr);
                        }*/
                        attributes.remove(attr);
                    }
                }
            }

        } else if (WfsUtil.validWfsConnection(t)) {
            //connectieType=Connecties.TYPE_WFS;
            Connecties conn = WfsUtil.getWfsConnection(t,request);
            List elements = WfsUtil.getFeatureElements(conn, t.getAdmin_tabel());
            if (elements != null) {
                ArrayList elementsNames = new ArrayList();
                for (int i = 0; i < elements.size(); i++) {
                    Element e = (Element) elements.get(i);
                    if (e.getAttribute("type") == null || !e.getAttribute("type").equalsIgnoreCase(OGCConstants.WFS_OBJECT_GEOMETRYTYPE)) {
                        elementsNames.add(e.getAttribute("name"));
                    }
                }
                attributes = elementsNames;
            }
        }
        if (attributes != null) {
            List bestaandeObjecten = SpatialUtil.getThemaData(t, false);
            for (int i = 0; i < attributes.size(); i++) {
                boolean bestaatAl = false;
                String themadataobject = (String) attributes.get(i);
                for (int j = 0; j < bestaandeObjecten.size() && !bestaatAl; j++) {
                    ThemaData td = (ThemaData) bestaandeObjecten.get(j);
                    if (td.getKolomnaam() != null && themadataobject.compareTo(td.getKolomnaam()) == 0) {
                        bestaatAl = true;
                    }
                }
                if (!bestaatAl) {
                    ThemaData td = new ThemaData();
                    if (attributes.size() <= DEFAULTBASISCOLUMNS)
                        td.setBasisregel(true);
                    else
                        td.setBasisregel(false);
                    td.setDataType((DataTypen) sess.get(DataTypen.class, DataTypen.DATA));
                    String netteNaam=themadataobject;
                    if (netteNaam.indexOf("{")>=0 && netteNaam.indexOf("}")>=0){
                        netteNaam=netteNaam.substring(netteNaam.indexOf("}")+1);
                    }
                    td.setLabel(netteNaam);
                    td.setKolomnaam(themadataobject);                    
                    td.setThema(t);
                    td.setWaardeType((WaardeTypen) sess.get(WaardeTypen.class, WaardeTypen.STRING));
                    sess.saveOrUpdate(td);
                } else {
                    //niks doen
                }
            }
        }
        //haal opnieuw de ThemaData objecten op om te kijken of er een Extra data veld is of nog moet aangemaakt worden.
        if (attributes!=null && attributes.size() > DEFAULTBASISCOLUMNS){
            List bestaandeObjecten = SpatialUtil.getThemaData(t, false);
            boolean extraBestaat = false;
            boolean nietBasisregels = false;
            for (int i = 0; i < bestaandeObjecten.size(); i++) {
                ThemaData td = (ThemaData) bestaandeObjecten.get(i);
                if (!td.isBasisregel()) {
                    nietBasisregels = true;
                }
                if (td.getCommando() != null && td.getCommando().toLowerCase().startsWith("viewerdata.do?aanvullendeinfo=t")) {
                    extraBestaat = true;
                }
            }
            if (!extraBestaat && nietBasisregels) {
                ThemaData td = createDefaultExtraThemaData(t);
                sess.saveOrUpdate(td);
            }
        }
        return unspecified(mapping, dynaForm, request, response);
    }

    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ThemaData td = getThemaData(dynaForm, false);
        if (td == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Themas t = td.getThema();
        t.getThemaData().remove(td);
        sess.delete(td);
        sess.flush();

        td = getFirstThemaData(t);
        dynaForm.initialize(mapping);
        populateThemaDataForm(td, dynaForm, request);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request);
        return getDefaultForward(mapping, request);
    }

    private void populateThemaDataForm(ThemaData td, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (td == null) {
            return;
        }
        if (td.getId() != null) {
            dynaForm.set("themaDataID", Integer.toString(td.getId()));
        } else {
            dynaForm.set("themaDataID", "");
        }
        dynaForm.set("label", td.getLabel());
        dynaForm.set("eenheid", td.getEenheid());
        dynaForm.set("omschrijving", td.getOmschrijving());
        String val = "";
        if (td.getThema() != null) {
            val = Integer.toString(td.getThema().getId());
        }
        dynaForm.set("themaID", val);
        dynaForm.set("basisregel", new Boolean(td.isBasisregel()));
        dynaForm.set("voorbeelden", td.getVoorbeelden());
        dynaForm.set("kolombreedte", FormUtils.IntToString(td.getKolombreedte()));
        val = "";
        if (td.getWaardeType() != null) {
            val = Integer.toString(td.getWaardeType().getId());
        }
        dynaForm.set("waardeTypeID", val);
        val = "";
        if (td.getDataType() != null) {
            val = Integer.toString(td.getDataType().getId());
        }
        dynaForm.set("dataTypeID", val);
        dynaForm.set("commando", td.getCommando());
        dynaForm.set("kolomnaam", td.getKolomnaam());
        if (td.getDataorder() != null) {
            dynaForm.set("dataorder", FormUtils.IntToString(td.getDataorder()));
        }
    }

    private void populateThemaDataObject(DynaValidatorForm dynaForm, ThemaData td, HttpServletRequest request) {

        Boolean b = (Boolean) dynaForm.get("basisregel");
        td.setBasisregel(b == null ? false : b.booleanValue());
        td.setCommando(FormUtils.nullIfEmpty(dynaForm.getString("commando")));
        if (FormUtils.nullIfEmpty(dynaForm.getString("dataorder")) != null) {
            td.setDataorder(Integer.parseInt(dynaForm.getString("dataorder")));
        }
        td.setEenheid(FormUtils.nullIfEmpty(dynaForm.getString("eenheid")));
        td.setKolombreedte(FormUtils.StringToInt(dynaForm.getString("kolombreedte")));
        td.setKolomnaam(FormUtils.nullIfEmpty(dynaForm.getString("kolomnaam")));
        td.setLabel(FormUtils.nullIfEmpty(dynaForm.getString("label")));
        td.setOmschrijving(FormUtils.nullIfEmpty(dynaForm.getString("omschrijving")));
        td.setVoorbeelden(FormUtils.nullIfEmpty(dynaForm.getString("voorbeelden")));

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        int tId = 0, dId = 0, wId = 0;
        try {
            tId = Integer.parseInt(dynaForm.getString("themaID"));
        } catch (NumberFormatException ex) {
            log.error("Illegal themaID", ex);
        }
        try {
            dId = Integer.parseInt(dynaForm.getString("dataTypeID"));
        } catch (NumberFormatException ex) {
            log.error("Illegal dataTypeID", ex);
        }
        try {
            wId = Integer.parseInt(dynaForm.getString("waardeTypeID"));
        } catch (NumberFormatException ex) {
            log.error("Illegal waardeTypeID", ex);
        }
        Themas t = (Themas) sess.get(Themas.class, new Integer(tId));
        td.setThema(t);
        DataTypen d = (DataTypen) sess.get(DataTypen.class, new Integer(dId));
        td.setDataType(d);
        WaardeTypen w = (WaardeTypen) sess.get(WaardeTypen.class, new Integer(wId));
        td.setWaardeType(w);
    }

    protected ThemaData createDefaultExtraThemaData(Themas t) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        ThemaData td = new ThemaData();
        td.setLabel("Extra");
        td.setBasisregel(true);
        td.setKolombreedte(50);
        td.setWaardeType((WaardeTypen) sess.get(WaardeTypen.class, WaardeTypen.STRING));
        td.setDataType((DataTypen) sess.get(DataTypen.class, DataTypen.URL));
        td.setCommando("viewerdata.do?aanvullendeinfo=t&");
        td.setThema(t);
        return td;
    }
}
