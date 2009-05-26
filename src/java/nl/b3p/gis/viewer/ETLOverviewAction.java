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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 * ETLOverviewAction definition:
 */
public class ETLOverviewAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(ETLOverviewAction.class);
    private static final String KNOP = "knop";
    private static final String[] STATUS = new String[]{"NO", "OAO", "OGO", "GO", "VO", "OO"};

    /**
     * Return een hashmap. Deze is verplicht overriden vanwege abstracte klasse BaseHibernateAction.
     *
     * @return Map
     */
    // <editor-fold defaultstate="" desc="protected Map getActionMethodPropertiesMap() method.">
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;
        hibProp = new ExtendedMethodProperties(KNOP);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(KNOP, hibProp);

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
        createOverview(dynaForm, request);
        return mapping.findForward(SUCCESS);
    }
    // </editor-fold>
    /**
     * Methode die zorg draagt voor het vullen en op het request zetten van de overzichtsresultaten
     * van de verschillende Thema afwijkingen die tijdens het laatste ETL proces aan het licht gekomen
     * zijn. Per Thema wordt er een onderscheidt gemaakt in de verschillende statussen die aan het
     * Thema toegekend kan zijn.
     *
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     */
    private void createOverview(DynaValidatorForm dynaForm, HttpServletRequest request) throws NotSupportedException, SQLException {
        List themalist = getValidThemas(false, null, request);
        ArrayList overview = new ArrayList();
        if (themalist != null) {
            Iterator it = themalist.iterator();
            while (it.hasNext()) {
                Themas t = (Themas) it.next();
                ArrayList count = getCount(t);
                if (count != null) {
                    overview.add(count);
                }
            }
        }
        request.setAttribute("overview", overview);
    }

    /**
     * Een methode die van een bepaald thema bepaald hoeveel hits er van elke status zijn.
     * De verschillende statussen die gebruikt worden zijn:
     * - NO
     * - OGO
     * - OAO
     * - GO
     * - VO
     * - OO
     *
     * Naast deze statussen wordt ook berekend wat het totaal aantal statussen is en wat het percentage
     * onvolledige objecten is binnen het thema. Deze wordt berekend uit het percentage OGO
     * en OAO.
     *
     * Om deze berekening snel uit te kunnen voeren is de database geoptimaliseerd. Hiertoe zijn er van de
     * huidige bruikbare tabellen indices gemaakt zodat de database een snelle response kan leveren.
     *
     * Een dergelijke index is als volgt gemaakt:
     * CREATE INDEX tankstations_centroid_status_index ON tankstations_centroid USING BTREE (status_etl)
     *
     * Een dergelijke index kan ook weer verwijderd worden. Dit kan als volgt:
     * DROP INDEX tankstations_centroid_status_index
     *
     * @param thema_naam String met de naam van het thema waar de telling van verricht moet worden.
     * @param admin_tabel String met de naam van de tabel van het thema waar de tellingen uit opgehaald kunnen worden.
     *
     * @return ArrayList met de tellingen van de verschillende statussen voor het betreffende thema.
     */
    // <editor-fold defaultstate="" desc="private ArrayList getCount(String thema_naam, String admin_tabel)">
    private ArrayList getCount(Themas t) throws NotSupportedException, SQLException {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = SpatialUtil.getJDBCConnection(t);
        if (connection==null) {
            throw new NotSupportedException("Kan geen JDBC connectie ophalen van thema. Wfs wordt niet ondersteund.");
        }
        boolean exists = false;
        try {
            exists = SpatialUtil.isEtlThema(t, connection);
        } catch (SQLException ex) {
            log.error("", ex);
        }
        if (!exists) {
            return null;
        }
        String admin_tabel = t.getAdmin_tabel();
        if (admin_tabel == null) {
            return null;
        }
        String selectQuery = "SELECT COUNT(1) as Total ";
        for (int i = 0; i < 6; i++) {
            selectQuery += ", (SELECT COUNT(1) FROM " + admin_tabel +
                    " AS subresult WHERE subresult.status_etl = '" +
                    STATUS[i] + "') AS Status_" + STATUS[i] + " ";
        }
        selectQuery += "FROM " + admin_tabel;


        ArrayList count = new ArrayList();
        count.add(t.getId());
        count.add(t.getNaam());
        try {
            int total = 0;
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                for (int i = 0; i < 6; i++) {
                    count.add(new Integer(rs.getInt("Status_" + STATUS[i])));
                }
                total = rs.getInt("Total");
                count.add(new Integer(total));
                statement.close();
            }

            Integer amountOGO = (Integer) count.get(3);
            Integer amountOAO = (Integer) count.get(4);
            if (total != 0 && amountOGO != null && amountOAO != null) {
                int percent = ((amountOGO.intValue() + amountOAO.intValue()) * 100) / total;
                count.add(new Integer(percent));
            } else {
                count.add(new Integer(0));
            }
            statement.close();
        } catch (SQLException ex) {
            log.error("", ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                log.error("", ex);
            }
        }

        return count;
    }
    // </editor-fold>
}