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

/*
 * Wat gaan we precies doen met deze ETL beheer tool? Het is de bedoeling dat een gebruiker
 * straks de mogelijkheid heeft om door middel van eenvoudige selectie criteria van een bepaald
 * thema op te kunnen vragen wat er voor gebreken in de objecten zijn bij dit thema.
 * Met eenvoudige selectie criteria wordt hier dan bedoelt dat een gebruiker aangeeft in welk
 * thema hij geinteresseerd is en vervolgens wat voor informatie hij over dit thema wil zien.
 * Hier kan bijvoorbeeld ingegeven worden welke status een object dient te hebben, wat voor
 * type object het om zou moeten gaan en binnen welke periodes (of batchperiode) dit object
 * verwerkt zou moeten zijn.
 *
 *
 */
package nl.b3p.gis.viewer;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.BaseGisAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class ETLTransformAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(ETLTransformAction.class);
    private static final String ADMINDATA = "admindata";
    private static final String EDIT = "edit";
    private static final String SHOWOPTIONS = "showOptions";

    /**
     * Return een hashmap die verschillende user gedefinieerde properties koppelt aan Actions.
     *
     * @return Map
     */
    // <editor-fold defaultstate="" desc="protected Map getActionMethodPropertiesMap() method.">
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;
        hibProp = new ExtendedMethodProperties(ADMINDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.admindata.failed");
        map.put(ADMINDATA, hibProp);

        hibProp = new ExtendedMethodProperties(EDIT);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.admindata.failed");
        map.put(EDIT, hibProp);

        hibProp = new ExtendedMethodProperties(SHOWOPTIONS);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.admindata.failed");
        map.put(SHOWOPTIONS, hibProp);

        return map;
    }
    // </editor-fold>
    /**
     * Actie die aangeroepen wordt vanuit het Struts frameword als een handeling aangeroepen wordt zonder property.
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     */
    // <editor-fold defaultstate="" desc="public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) method.">
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        createLists(dynaForm, request);
        return mapping.findForward(SUCCESS);
    }
    // </editor-fold>
    /**
     * Actie die aangeroepen wordt vanuit het Struts frameword als een handeling aangeroepen wordt met een showOption property.
     *
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     */
    // <editor-fold defaultstate="" desc="public ActionForward showOptions(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) method.">
    public ActionForward showOptions(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        createLists(dynaForm, request);
        String themaid = request.getParameter("themaid");
        Themas t = getThema(mapping, dynaForm, request);
        request.setAttribute("themaName", t.getNaam());
        request.setAttribute("themaid", themaid);
        request.setAttribute("layerToAdd", t.getWms_layers_real());
        request.setAttribute("kburl",HibernateUtil.getKbUrl());
        return mapping.findForward(SUCCESS);
    }
    // </editor-fold>
    /**
     * Actie die aangeroepen wordt vanuit het Struts frameword als een handeling aangeroepen wordt met een edit property.
     *
     * @param mapping The ActionMapping used to select this instance.
     * @param dynaForm The optional ActionForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     *
     * @return ActionForward
     *
     * @throws Exception
     */
    // <editor-fold defaultstate="" desc="public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) method.">
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        createLists(dynaForm, request);
        String themaid = (String) request.getParameter("themaid");
        String status = (String) request.getParameter("type");

        Themas t = getThema(mapping, dynaForm, request);
        request.setAttribute("themaName", t.getNaam());
        List thema_data = SpatialUtil.getThemaData(t, false);
        request.setAttribute("thema_items", thema_data);
        if (thema_data != null && !thema_data.isEmpty()) {
            request.setAttribute("regels", getThemaObjects(t, status, thema_data));
        } else {
            request.setAttribute("regels", null);
        }


        request.setAttribute("themaName", t.getNaam());
        return mapping.findForward("showData");
    }
    // </editor-fold>
    /**
     * /**
     * DOCUMENT ME!!!
     *
     * @param t Themas
     * @param thema_items List
     *
     * @return List
     *
     * @throws SQLException
     * @throws UnsupportedEncodingException
     *
     * @see Themas
     */
    protected List getThemaObjects(Themas t, String status, List thema_items) throws SQLException, UnsupportedEncodingException, NotSupportedException {
        if (t == null) {
            return null;
        }
        if(SpatialUtil.validJDBCConnection(t)){
            log.error("Thema heeft geen JDBC connectie. Andere connecties worden niet ondersteund.");
            return null;
        }
        ArrayList regels = new ArrayList();
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = t.getConnectie().getJdbcConnection();
        String taq = "select * from " + t.getSpatial_tabel() + " where status_etl = ? and etl_proces_id = (select max(etl_proces_id) from " + t.getSpatial_tabel() + ")";

        try {
            PreparedStatement statement = connection.prepareStatement(taq);
            statement.setString(1, status);
            try {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    regels.add(getRegel(rs, t, thema_items));
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
        return regels;
    }

    /*
     * Methode die aangeroepen wordt om de boomstructuur op te bouwen. De opbouw wordt op een iteratieve en recursieve
     * manier uitgevoerd waarbij over de verschillende thema's heen gewandeld wordt om van deze thema's de children
     * (clusters) te bepalen en in de juiste volgorde in de lijst te plaatsen.
     *
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     *
     * @throws Exception
     */
    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        super.createLists(dynaForm, request);
        List ctl = SpatialUtil.getValidClusters();
        List themalist = getValidThemas(false, null, request);
        List newThemalist = new ArrayList(themalist);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = null;

        Iterator it = themalist.iterator();
        while (it.hasNext()) {
            Themas t = (Themas) it.next();
            if (SpatialUtil.validJDBCConnection(t)){
                connection = t.getConnectie().getJdbcConnection();
                boolean etlExists = false;
                try {
                    etlExists = SpatialUtil.isEtlThema(t, connection);
                } catch (SQLException sqle) {
                }
                if (!etlExists) {
                    newThemalist.remove(t);
                }
            }
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ex) {
            log.error("", ex);
        }

        request.setAttribute("themalist", newThemalist);
    }
}
