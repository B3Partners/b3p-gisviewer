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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.NotSupportedException;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.DataTypen;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.struts.BaseHibernateAction;
import nl.b3p.wms.capabilities.Layer;
import nl.b3p.wms.capabilities.ServiceProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.opengis.feature.Feature;

public abstract class BaseGisAction extends BaseHibernateAction {

    private static final Log log = LogFactory.getLog(BaseGisAction.class);
    public static final String URL_AUTH = "code";

    protected String getOrganizationCode(HttpServletRequest request) {
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        if (gp != null) {
            ServiceProvider sp = gp.getSp();
            if (sp != null) {
                return sp.getOrganizationCode();
            } else {
                log.error("Er is geen serviceprovider aanwezig bij GisPrincipal met naam: " + gp.getName());
                return null;
            }
        } else {
            log.error("Er is geen GisPrincipal aanwezig.");
            return null;
        }
    }

    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        String code = null;
        if (gp != null) {
            code = gp.getCode();
        }
        // zet kaartenbalie url
        request.setAttribute("kburl", HibernateUtil.createPersonalKbUrl(code));
        request.setAttribute("kbcode", code);

        String organizationcode = getOrganizationCode(request);
        if (organizationcode != null && organizationcode.length() > 0) {
            request.setAttribute("organizationcode", getOrganizationCode(request));
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

    protected Themas getThema(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, boolean analysethemaid) {
        String themaid = "";
        if (analysethemaid) {
            themaid = (String) request.getParameter("analysethemaid");
        } else {
            themaid = (String) request.getParameter("themaid");
        }
        log.error("THEMAID: " + themaid);
        return getThema(themaid, request);
    }

    /**
     * Get the thema en doe wat checks
     */
    private Themas getThema(String themaid, HttpServletRequest request) {
        Themas t = SpatialUtil.getThema(themaid);

        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            return t;
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

        // Check de rechten op alle layers uit het thema
        if (!checkThemaLayers(t, layersFromRoles)) {
            return null;
        }

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
        // Als geen check via kaartenbalie dan alle layers doorgeven
        if (!HibernateUtil.isCheckLoginKaartenbalie()) {
            return configuredThemasList;
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

        // Voeg alle themas toe die layers hebben die volgens de rollen
        // acceptabel zijn (voldoende rechten dus).
        List layersFound = new ArrayList();
        List checkedThemaList = new ArrayList();
        if (configuredThemasList != null) {
            Iterator it2 = configuredThemasList.iterator();
            while (it2.hasNext()) {
                Themas t = (Themas) it2.next();
                if (checkThemaLayers(t, layersFromRoles)) {
                    checkedThemaList.add(t);
                    layersFound.add(t.getWms_layers_real());
                }
            }
        }

        // Als geen cluster dan hier stoppen.
        if (ctl == null) {
            return checkedThemaList;
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
            Layer l= user.getLayer(layer);
            if (l!=null){
                Themas t = new Themas();
                t.setNaam(l.getTitle());
                t.setId(new Integer(tid++));
                if(user.hasLegendGraphic(l)){
                    t.setWms_legendlayer_real(layer);
                }
                if ("1".equalsIgnoreCase(l.getQueryable())){
                    t.setWms_querylayers_real(layer);
                }
                t.setWms_layers_real(layer);
                t.setCluster(c);
                // voeg extra laag als nieuw thema toe
                extraThemaList.add(t);
            }
        }
        if (extraThemaList.size() > 0) {
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

        // Dit is te streng alleen op wms layer checken
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
     * Om getPks backwardcompatable te maken
     */
    @Deprecated 
    protected List getPks(Themas t, DynaValidatorForm dynaForm, HttpServletRequest request) throws SQLException, NotSupportedException {
        return getPks(t,dynaForm,request,null);
    }
    /**
     * DOCUMENT ME!!!
     *
     * @param t Themas
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param pksField String. De naam van de Parameter waar alle  primary keys
     *      in opgeslagen zijn. Als het null is wordt de pk naam van het thema
     *      gebruikt om het van het request te halen. Dit is gedaan omdat de pk
     *      uit een namespace kan bestaan voorbeeld "{www.b3p.nl}id" Dit gaat fout in js
     *      als je dit gebruikt om een ref te maken naar het veld. Vandaar dat je dus ook
     *      een andere kan opgeven als parameter naam. Dus dat is niet verstandig om te gebruiken!
     * @return List
     *
     * @throws SQLException
     *
     * @see Themas
     */
    protected List getPks(Themas t, DynaValidatorForm dynaForm, HttpServletRequest request, String pksField) throws SQLException, NotSupportedException {
        ArrayList pks = new ArrayList();

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = null;
        if (SpatialUtil.validJDBCConnection(t)) {
            connection = t.getConnectie().getJdbcConnection();
        }else{
            log.error("Thema heeft geen JDBC connectie: "+t.getNaam(),new UnsupportedOperationException("Can not create a JDBC connection, this function is only supported for JDBC connections"));
            return null;
        }
        int dt = SpatialUtil.getPkDataType(t, connection);
        String adminPk = t.getAdmin_pk();
        String adminIds=null;
        if (pksField==null){
            adminIds = request.getParameter(adminPk);
        }else{
            adminIds = request.getParameter(pksField);
        }

        String[] adminIdsArr = adminIds.split(",");
        for (int i = 0; i < adminIdsArr.length; i++) {
            String adminId = adminIdsArr[i];
            switch (dt) {
                case java.sql.Types.SMALLINT:
                    pks.add(new Short(adminId));
                    break;
                case java.sql.Types.INTEGER:
                    pks.add(new Integer(adminId));
                    break;
                case java.sql.Types.BIGINT:
                    pks.add(new Long(adminId));
                    break;
                case java.sql.Types.BIT:
                    pks.add(new Boolean(adminId));
                    break;
                case java.sql.Types.DATE:
                    //                pks.add(new Date(adminId));
                    break;
                case java.sql.Types.DECIMAL:
                case java.sql.Types.NUMERIC:
                    pks.add(new BigDecimal(adminId));
                    break;
                case java.sql.Types.REAL:
                    pks.add(new Float(adminId));
                    break;
                case java.sql.Types.FLOAT:
                case java.sql.Types.DOUBLE:
                    pks.add(new Double(adminId));
                    break;
                case java.sql.Types.TIME:
                    //                pks.add(new Time(adminId));
                    break;
                case java.sql.Types.TIMESTAMP:
                    //                pks.add(new Timestamp(adminId));
                    break;
                case java.sql.Types.TINYINT:
                    pks.add(new Byte(adminId));
                    break;
                case java.sql.Types.CHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.VARCHAR:
                    pks.add(adminId);
                    break;
                case java.sql.Types.NULL:
                default:
                    return null;
            }
        }
        return pks;
    }

    /**
     * DOCUMENT ME!!!
     *
     * @param t Themas
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     *
     * @return List
     *
     * @throws Exception
     *
     * @see Themas
     */
    // <editor-fold defaultstate="" desc="protected List findPks(Themas t, ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request)">
    protected List findPks(Themas t, ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
        String[] coordString = null;
        String geom =  request.getParameter("geom");
        double[] coords = null;
        if (request.getParameter("coords") != null && !request.getParameter("coords").equals("")) {
            coordString = request.getParameter("coords").split(",");
            coords = new double[coordString.length];
            for (int i = 0; i < coordString.length; i++) {
                coords[i] = Double.parseDouble(coordString[i]);
            }
        }else{
            coords = new double[0];
        }
        String s = request.getParameter("scale");
        double scale = 0.0;
        try {
            if (s != null) {
                scale = Double.parseDouble(s);
            //af ronden op 1 decimaal

            }
        } catch (NumberFormatException nfe) {
            scale = 0.0;
            log.info("Scale is geen double dus wordt genegeerd");
        }
        double distance = 10.0;
        if (scale > 0.0) {
            distance = scale * (distance);
            distance = Math.round(distance * 1000) / 1000;
            if (distance < 1.0) {
                distance = 1.0;
            }
        } else {
            distance = 10.0;
        }
        int srid = 28992; // RD-new

        ArrayList pks = new ArrayList();

        String saf = t.getSpatial_admin_ref();
        if (saf == null || saf.length() == 0) {
            saf = t.getAdmin_pk();
        }
        String sptn = t.getSpatial_tabel();
        if (sptn == null || sptn.length() == 0) {
            sptn = t.getAdmin_tabel();
        }
        if (sptn==null || saf==null || sptn.length()==0 || saf.length()==0){
            return null;
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = null;
        if (SpatialUtil.validJDBCConnection(t)) {
            connection = t.getConnectie().getJdbcConnection();
        }else{
            log.error("Thema heeft geen JDBC connectie: "+t.getNaam(),new UnsupportedOperationException("Can not create a JDBC connection, this function is only supported for JDBC connections"));
            return null;
        }
        try {
            String organizationcodekey = t.getOrganizationcodekey();
            String organizationcode = getOrganizationCode(request);
            String geomColumnName = SpatialUtil.getTableGeomName(t, connection);
            String q = null;
            if (organizationcodekey != null && organizationcodekey.length() > 0 &&
                    organizationcode != null && organizationcode.length() > 0) {
                q = SpatialUtil.InfoSelectQuery(saf, sptn, geomColumnName, coords, distance, srid, organizationcodekey, organizationcode, geom);
            } else {
                q = SpatialUtil.InfoSelectQuery(saf, sptn, geomColumnName, coords, distance, srid, geom);
            }

            PreparedStatement statement = connection.prepareStatement(q);
            try {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    pks.add(rs.getObject(saf));
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }
        return pks;
    }
    // </editor-fold>

    /**
     * Een protected methode het object thema ophaalt dat hoort bij een bepaald id.
     *
     * @param identifier String which identifies the object thema to be found.
     *
     * @return a Themas object representing the object thema.
     *
     */
    // <editor-fold defaultstate="" desc="private Themas getObjectThema(String identifier) method.">
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
    // </editor-fold>

    /**
     * Een protected methode die het Thema Geometry type ophaalt uit de database.
     *
     * @param themaGeomTabel De table for which the Geometry type is requested.
     *
     * @return a String with the Geometry type.
     *
     */
    protected String getThemaGeomType(Themas thema) throws Exception {
        String themaGeomType = thema.getView_geomtype();
        if (themaGeomType != null) {
            return themaGeomType;
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = null;
        if (SpatialUtil.validJDBCConnection(thema)) {
            connection = thema.getConnectie().getJdbcConnection();
        }else{
            log.error("Thema heeft geen JDBC connectie: "+thema.getNaam(),new UnsupportedOperationException("Can not create a JDBC connection, this function is only supported for JDBC connections"));
            return null;
        }
        return SpatialUtil.getThemaGeomType(thema, connection);
    }

    /**
     * Een protected methode die de Analysenaam van een bepaalde tabel en kolom samenstelt met
     * de opgegeven waarden.
     *
     * @param analyseGeomTabel De table for which the analysename is requested.
     * @param analyseGeomIdColumn De column for which the analysename is requested.
     * @param analyseGeomId De id for which the analysename is requested.
     *
     * @return a String with the analysename.
     *
     */
    protected String getAnalyseNaam(String analyseGeomId, Themas t) throws NotSupportedException, SQLException {

        String analyseGeomTabel = t.getSpatial_tabel();
        String analyseGeomIdColumn = t.getSpatial_admin_ref();
        int themaid = t.getId().intValue();

        String analyseNaam = t.getNaam();
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Connection connection = null;
        if (SpatialUtil.validJDBCConnection(t)) {
            connection = t.getConnectie().getJdbcConnection();
        }else{
            log.error("Thema heeft geen JDBC connectie: "+t.getNaam(),new UnsupportedOperationException("Can not create a JDBC connection, this function is only supported for JDBC connections"));
            return null;
        }
        try {
            String statementString = "select * from \"" + analyseGeomTabel +
                    "\" where " + analyseGeomIdColumn + " = ";
            String newAnalyseGeomId = "\'" + analyseGeomId + "\'";
            try {
                int intGeomId = Integer.parseInt(analyseGeomId);
                newAnalyseGeomId = "" + intGeomId;
            } catch (Exception e) {
            }
            statementString += newAnalyseGeomId;
            log.info(statementString);
            PreparedStatement statement =
                    connection.prepareStatement(statementString);
            PreparedStatement statement2 =
                    connection.prepareStatement("select kolomnaam from thema_data where thema = " +
                    themaid + " order by dataorder");

            try {
                ResultSet rs = statement.executeQuery();
                ResultSet rs2 = statement2.executeQuery();
                if (rs.next() && rs2.next()) {
                    if (rs2.next()) {
                        String extraString = rs.getString(rs2.getString("kolomnaam"));
                        if (extraString != null) {
                            analyseNaam += " " + extraString;
                        }
                    }
                }
            } finally {
                statement.close();
                statement2.close();
            }
        } catch (SQLException ex) {
            log.error("", ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                log.error("", ex);
            }
        }

        return analyseNaam;
    }

    /**
     *
     * @param query String
     * @param sess Session
     * @param result StringBuffer
     * @param columns String[]
     */
    // <editor-fold defaultstate="" desc="private void executeQuery(String query, Session sess, StringBuffer result, String[] columns)">
    protected void executeQuery(String query, Session sess, StringBuffer result, String[] columns, Connection connection) throws NotSupportedException, SQLException {
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            try {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    for (int i = 0; i < columns.length; i++) {
                        if (i != 0) {
                            result.append(", ");
                        }
                        Object resultObject = rs.getObject(columns[i]);
                        if (resultObject instanceof java.lang.Double) {
                            double resultDouble = ((Double) resultObject).doubleValue();
                            resultDouble *= 100;
                            resultDouble = Math.round(resultDouble);
                            resultDouble /= 100;
                            result.append(resultDouble);
                        } else if (resultObject != null) {
                            result.append(resultObject);
                        }
                    }
                    result.append("<br/>");
                }
            } finally {
                statement.close();
            }
        } catch (SQLException ex) {
            log.error("", ex);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                log.error("", ex);
            }
        }
    }
    // </editor-fold>

    /**
     * DOCUMENT ME!!!
     *
     * @param rs ResultSet
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
    protected AdminDataRowBean getRegel(ResultSet rs, Themas t, List thema_items) throws SQLException, UnsupportedEncodingException {
        //ArrayList regel = new ArrayList();
        AdminDataRowBean regel = new AdminDataRowBean();
        regel.setPrimairyKey(rs.getObject(t.getAdmin_pk()));
        Iterator it = thema_items.iterator();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            /*
             * Controleer eerst om welk datatype dit themadata object om draait.
             * Binnen het Datatype zijn er drie mogelijkheden, namelijk echt data,
             * een URL of een Query.
             * In alle drie de gevallen moeten er verschillende handelingen verricht
             * worden om deze informatie op het scherm te krijgen.
             *
             * In het eerste geval, wanneer het gaat om data, betreft dit de kolomnaam.
             * Als deze kolomnaam ingevuld staat hoeft deze alleen opgehaald te worden
             * en aan de arraylist regel toegevoegd te worden.
             */
            if (td.getDataType().getId() == DataTypen.DATA && td.getKolomnaam() != null && !td.getKolomnaam().equals("")) {
                regel.addValue(rs.getObject(td.getKolomnaam()));

            /*
             * In het tweede geval dient de informatie in de thema data als link naar een andere
             * informatiebron. Deze link zal enigszins aangepast moeten worden om tot vollende
             * werkende link te dienen.
             */
            } else if (td.getDataType().getId() == DataTypen.URL && td.getCommando() != null) {
                StringBuffer url = new StringBuffer(td.getCommando());
                url.append(Themas.THEMAID);
                url.append("=");
                url.append(t.getId());

                String adminPk = t.getAdmin_pk();
                Object value = rs.getObject(adminPk);
                if (value != null) {
                    url.append("&");
                    url.append(adminPk);
                    url.append("=");
                    url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                }

                String kolomNaam = td.getKolomnaam();
                if (kolomNaam != null && kolomNaam.length() > 0 && !kolomNaam.equalsIgnoreCase(adminPk)) {
                    value = rs.getObject(kolomNaam);
                    if (value != null) {
                        url.append("&");
                        url.append(kolomNaam);
                        url.append("=");
                        url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                    }
                }

                regel.addValue(url.toString());

            /*
             * De laatste mogelijkheid betreft een query. Vanuit de themadata wordt nu een
             * een commando url opgehaald en deze wordt met de kolomnaam aangevuld.
             */
            } else if (td.getDataType().getId() == DataTypen.QUERY) {
                StringBuffer url = new StringBuffer(td.getCommando());
                String kolomNaam = td.getKolomnaam();
                if (kolomNaam == null || kolomNaam.length() == 0) {
                    kolomNaam = t.getAdmin_pk();
                }
                Object value = rs.getObject(kolomNaam);
                if (value != null) {
                    url.append(value.toString().trim());
                    regel.addValue(url.toString());
                } else {
                    regel.addValue("");
                }
            } else if (td.getDataType().getId() == DataTypen.FUNCTION) {
                String keyName = t.getAdmin_pk();
                Object keyValue = rs.getObject(keyName);
                String attributeName = td.getKolomnaam();
                Object attributeValue = null;

                String attributeValue2 = "";
                if (attributeName == null || attributeName.length() == 0) {
                    attributeName = keyName;
                    attributeValue = keyValue;
                } else {
                    attributeValue = rs.getObject(attributeName);
                }
                attributeValue2 = attributeValue.toString();
                if (keyValue != null) {
                    // De attributeValue ook eerst vooraan erbij zetten om die te kunnen tonen op de admindata pagina - Drie hekjes als scheidingsteken
                    // Een aantal waardes worden ge-escaped zodat er geen JavaScript fouten optreden
                    StringBuffer function = new StringBuffer("");
                    function.append(attributeValue);
                    function.append("###" + td.getCommando());
                    function.append("(this, ");
                    function.append("'" + td.getThema().getId() + "'");
                    function.append(",");
                    function.append("'" + keyName.replaceAll("'", "\\\\'") + "'");
                    function.append(",");
                    function.append("'" + keyValue + "'");
                    function.append(",");
                    function.append("'" + attributeName.replaceAll("'", "\\\\'") + "'");
                    function.append(",");
                    function.append("'" + attributeValue2.replaceAll("'", "\\\\'") + "'");
                    function.append(",");
                    function.append("'" + td.getEenheid() + "'");
                    function.append(")");
                    regel.addValue(function.toString());
                } else {
                    regel.addValue("");
                }
            } else /*
             * Indien een datatype aan geen van de voorwaarden voldoet wordt er een
             * lege regel aan de regel arraylist toegevoegd.
             */ {
                regel.addValue("");
            }
        }
        return regel;
    }

    private String convertAttributeName(String rawName, Feature f) {
        if (rawName == null || rawName.trim().length() == 0) {
            return null;
        }
        String attName = rawName.trim();
        if (f.getProperty(attName)!=null) {
            return attName;
        }
        attName=removeNamespace(attName);
        if (f.getProperty(attName)!=null) {
            return attName;
        }
        return null;
    }

    public static String removeNamespace(String rawName){
        if (rawName==null){
            return rawName;
        }
        String returnValue= new String(rawName);
        if (returnValue.indexOf("{")>=0 && returnValue.indexOf("}") >=0 )
            returnValue= returnValue.substring(returnValue.indexOf("}")+1);
        if (returnValue.split(":").length > 1) {
            returnValue = returnValue.split(":")[1];
        }
        return returnValue;
    }

    /**
     * Zelfde als getRegel met Resultset maar nu met Feature
     *
     * @param rs ResultSet
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
    protected AdminDataRowBean getRegel(Feature f, Themas t, List thema_items) throws SQLException, UnsupportedEncodingException {
        AdminDataRowBean regel = new AdminDataRowBean();

        String adminPk = convertAttributeName(t.getAdmin_pk(), f);
        if (adminPk != null) {
            regel.setPrimairyKey(f.getProperty(adminPk).getValue());
        }
        Iterator it = thema_items.iterator();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            /*
             * Controleer of de kolomnaam van dit themadata object wel voorkomt in de feature.
             * zoniet kan het zijn dat er een prefix ns in staat. Die moet er dan van afgehaald worden. 
             * Als het dan nog steeds niet bestaat: een lege toevoegen.
             */
            String kolomnaam = convertAttributeName(td.getKolomnaam(), f);
            /*
             * Controleer om welk datatype dit themadata object om draait.
             * Binnen het Datatype zijn er drie mogelijkheden, namelijk echt data,
             * een URL of een Query.
             * In alle drie de gevallen moeten er verschillende handelingen verricht
             * worden om deze informatie op het scherm te krijgen.
             *
             * In het eerste geval, wanneer het gaat om data, betreft dit de kolomnaam.
             * Als deze kolomnaam ingevuld staat hoeft deze alleen opgehaald te worden
             * en aan de arraylist regel toegevoegd te worden.
             */
            if (td.getDataType().getId() == DataTypen.DATA && kolomnaam != null) {
                if (f.getProperty(kolomnaam).getValue()==null){
                    regel.addValue(null);
                }else{
                    regel.addValue(f.getProperty(kolomnaam).getValue().toString());
                }
            /*
             * In het tweede geval dient de informatie in de thema data als link naar een andere
             * informatiebron. Deze link zal enigszins aangepast moeten worden om tot vollende
             * werkende link te dienen.
             */
            } else if (td.getDataType().getId() == DataTypen.URL) {
                StringBuffer url;
                if (td.getCommando() != null) {
                    url = new StringBuffer(td.getCommando());
                } else {
                    url = new StringBuffer();
                }
                url.append(Themas.THEMAID);
                url.append("=");
                url.append(t.getId());

                Object value = null;
                if (adminPk != null) {
                    value = f.getProperty(adminPk).getValue();
                    if (value != null) {
                        url.append("&");
                        url.append(adminPk);
                        url.append("=");
                        url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                    }
                }

                if (kolomnaam != null && kolomnaam.length() > 0 && !kolomnaam.equalsIgnoreCase(adminPk)) {
                    value = f.getProperty(kolomnaam).getValue();
                    if (value != null) {
                        url.append("&");
                        url.append(kolomnaam);
                        url.append("=");
                        url.append(URLEncoder.encode(value.toString().trim(), "utf-8"));
                    }
                }

                regel.addValue(url.toString());

            /*
             * De laatste mogelijkheid betreft een query. Vanuit de themadata wordt nu een
             * een commando url opgehaald en deze wordt met de kolomnaam aangevuld.
             */
            } else if (td.getDataType().getId() == DataTypen.QUERY) {
                StringBuffer url;
                if (td.getCommando() != null) {
                    url = new StringBuffer(td.getCommando());
                } else {
                    url = new StringBuffer();
                }

                Object value = null;
                if (kolomnaam != null) {
                    value = f.getProperty(kolomnaam).getValue();
                }
                if (value != null) {
                    url.append(value.toString().trim());
                    regel.addValue(url.toString());
                } else {
                    regel.addValue("");
                }
            } else if (td.getDataType().getId() == DataTypen.FUNCTION) {
                Object keyValue = null;
                if (adminPk != null) {
                    keyValue = f.getProperty(adminPk).getValue();
                }
                if (keyValue != null) {
                    String attributeName = kolomnaam;
                    Object attributeValue = null;
                    if (attributeName != null) {
                        attributeValue = f.getProperty(attributeName).getValue();
                    } else {
                        attributeName = adminPk;
                        attributeValue = keyValue;
                    }

                    // De attributeValue ook eerst vooraan erbij zetten om die te kunnen tonen op de admindata pagina - Drie hekjes als scheidingsteken
                    StringBuffer function = new StringBuffer("");
                    function.append(attributeValue);
                    function.append("###" + td.getCommando());
                    function.append("(this, ");
                    function.append("'" + td.getThema().getId() + "'");
                    function.append(",");
                    function.append("'" + adminPk + "'");
                    function.append(",");
                    function.append("'" + keyValue + "'");
                    function.append(",");
                    function.append("'" + attributeName + "'");
                    function.append(",");
                    function.append("'" + attributeValue + "'");
                    function.append(",");
                    function.append("'" + td.getEenheid() + "'");
                    function.append(")");
                    regel.addValue(function.toString());
                } else {
                    regel.addValue("");
                }
            } else /*
             * Indien een datatype aan geen van de voorwaarden voldoet wordt er een
             * lege regel aan de regel arraylist toegevoegd.
             */ {
                regel.addValue("");
            }
        }
        return regel;
    }

    /**
     * DOCUMENT ME!!!
     *
     * @param params Map
     * @param key String
     *
     * @return String
     */
    protected String getStringFromParam(Map params, String key) {
        Object ob = params.get(key);
        String zoekopties_waarde = null;
        String string = null;
        if (ob instanceof String) {
            string = (String) ob;
        }
        if (ob instanceof String[]) {
            string = ((String[]) ob)[0];
        }
        return string;
    }

    /**
     *Compare 2 thema datalists voor het tonen in de admindata. (dus niet volledige vergelijking maar alleen op label en basisregel)
     */
    public boolean compareThemaDataLists(List list1, List list2) {
        int basisRegelTeller1 = 0;
        int basisRegelTeller2 = 0;
        for (int i1 = 0; i1 < list1.size(); i1++) {
            ThemaData td1 = (ThemaData) list1.get(i1);
            if (td1.isBasisregel()) {
                basisRegelTeller1++;
            }
            if (td1.isBasisregel() && td1.getLabel() != null) {
                boolean bevatGelijke = false;
                for (int i2 = 0; i2 < list2.size(); i2++) {
                    ThemaData td2 = (ThemaData) list2.get(i2);
                    if (td2.isBasisregel()) {
                        basisRegelTeller2++;
                    }
                    if (td2.isBasisregel() && td1.getLabel().equalsIgnoreCase(td2.getLabel())) {
                        bevatGelijke = true;
                        break;
                    }
                }
                if (!bevatGelijke) {
                    return false;
                }
            }
        }
        if (basisRegelTeller1 == basisRegelTeller2) {
            return true;
        } else {
            return false;
        }
    }
}
