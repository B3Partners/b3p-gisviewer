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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.NotSupportedException;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.geotools.FilterBuilder;
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
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.feature.type.Name;

public abstract class BaseGisAction extends BaseHibernateAction {

    private static final Log log = LogFactory.getLog(BaseGisAction.class);
    public static final String URL_AUTH = "code";
    protected static final double DEFAULTTOLERANCE = 5.0;

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
    protected AdminDataRowBean getRegel(Feature f, Themas t, List<ThemaData> thema_items) throws SQLException, UnsupportedEncodingException, Exception {
        AdminDataRowBean regel = new AdminDataRowBean();

        String adminPk = DataStoreUtil.convertFullnameToQName(t.getAdmin_pk()).getLocalPart();
        if (adminPk != null) {
            regel.setPrimaryKey(f.getProperty(adminPk).getValue());
        }
        String wkt = DataStoreUtil.convertFeature2WKT(f);
        if (wkt != null && wkt.length() != 0) {
            regel.setWkt(wkt);
        }
        Iterator it = thema_items.iterator();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            /*
             * Controleer of de kolomnaam van dit themadata object wel voorkomt in de feature.
             * zoniet kan het zijn dat er een prefix ns in staat. Die moet er dan van afgehaald worden. 
             * Als het dan nog steeds niet bestaat: een lege toevoegen.
             */
            String kolomnaam = td.getKolomnaam();
            if (kolomnaam!=null && kolomnaam.length()>0) {
                    kolomnaam = DataStoreUtil.convertFullnameToQName(td.getKolomnaam()).getLocalPart();
            }
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
                if (f.getProperty(kolomnaam).getValue() == null) {
                    regel.addValue(null);
                } else {
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
                String commando = url.toString();
                //Kijk of er in de waarde van de kolomnaam een komma zit. Zoja, splits het dan op.
                Object valueToSplit = null;
                if (kolomnaam != null && f.getProperty(kolomnaam) != null) {
                    valueToSplit = f.getProperty(kolomnaam).getValue();
                }
                HashMap fhm = toHashMap(f);
                List values = splitObject(valueToSplit, ",");
                List regelValues = new ArrayList();
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (commando.contains("[") || commando.contains("]")) {
                        //vervang de eventuele csv in 1 waarde van die csv
                        if (kolomnaam != null) {
                            fhm.put(kolomnaam, value);
                        }
                        String newCommando = replaceValuesInString(commando, fhm);
                        regelValues.add(newCommando);
                    } else {
                        if (value != null) {
                            url.append(value.toString().trim());
                            regelValues.add(url.toString());
                        } else {
                            regelValues.add("");
                        }
                    }
                }
                regel.addValue(regelValues);
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
    protected boolean compareThemaDataLists(List list1, List list2) {
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
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

    private HashMap toHashMap(Feature f) throws Exception {
        HashMap result = new HashMap();
        FeatureType ft = f.getType();
        Iterator it = ft.getDescriptors().iterator();
        while (it.hasNext()) {
            PropertyDescriptor pd = (PropertyDescriptor) it.next();
            String key = pd.getName().getLocalPart();
            Object value = f.getProperty(pd.getName()).getValue();
            result.put(key, value);
        }
        return result;
    }

    /**
     * Alle [kolomnamen] in de url worden vervangen door de waarde in de kolom.
     * Bijvoorbeeld:
     * http://plannen.kaartenbalie.nl/[planeigenaar]/[plannaam]/[planidentificyaty].html
     * Kan dan worden:
     * http://plannen.kaartenbalie.nl/gemeente/plansoen/p38.html
     */
    private String replaceValuesInString(String string, HashMap values) throws Exception {
        if (!string.contains("[") && !string.contains("]")) {
            return string;
        }
        StringBuffer url;
        if (string != null) {
            url = new StringBuffer(string);
        } else {
            url = new StringBuffer();
        }

        int begin = -1;
        int eind = -1;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '[') {
                if (begin == -1) {
                    begin = i;
                } else {
                    log.error("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
                    throw new Exception("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
                }
            } else if (c == ']') {
                eind = i;
                if (begin != -1 && eind != -1) {
                    String kolomnaam = url.substring(begin + 1, eind);
                    if (kolomnaam == null || kolomnaam.length() == 0) {
                        log.error("Commando \"" + string + "\" is niet correct. Geen kolomnaam aanwezig tussen [ en ].");
                        throw new Exception("Commando \"" + string + "\" is niet correct. Geen kolomnaam aanwezig tussen [ en ].");
                    }
                    Object value = values.get(kolomnaam);
                    if (value == null) {
                        value = "";
                    }
                    url.replace(begin, eind + 1, value.toString().trim());
                    begin = -1;
                    eind = -1;
                    i = 0;
                } else {
                    log.error("Commando \"" + string + "\" is niet correct. Er ontbreekt een [ .");
                    throw new Exception("Commando \"" + string + "\" is niet correct. Er ontbreekt een [ .");
                }
            } else if (i == url.length() - 1 && begin != -1) {
                log.error("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
                throw new Exception("Commando \"" + string + "\" is niet correct. Er ontbreekt een ] .");
            }
        }
        return url.toString();
    }

    private List splitObject(Object value, String seperator) {
        ArrayList values = new ArrayList();
        if (value == null) {
            values.add(value);
        } else if (value instanceof String) {
            String[] tokens = ((String) value).split(seperator);
            for (int i = 0; i < tokens.length; i++) {
                values.add(tokens[i]);
            }
        } else {
            values.add(value);
        }
        return values;
    }

    private boolean doExtraSearchFilter(Themas t, HttpServletRequest request) {
        if (FormUtils.nullIfEmpty(t.getSldattribuut()) != null && FormUtils.nullIfEmpty(request.getParameter(ViewerAction.SEARCH)) != null) {
            String searchId = request.getParameter(ViewerAction.SEARCHID);
            String searchClusterId = request.getParameter(ViewerAction.SEARCHCLUSTERID);
            if (FormUtils.nullIfEmpty(searchId) != null) {
                String[] searchIds = searchId.split(",");
                for (int i = 0; i < searchIds.length; i++) {
                    try {
                        if (t.getId().intValue() == Integer.parseInt(searchIds[i])) {
                            return true;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                }
            }
            if (FormUtils.nullIfEmpty(searchClusterId) != null) {
                String[] clusterIds = searchClusterId.split(",");
                for (int i = 0; i < clusterIds.length; i++) {
                    try {
                        if (isInCluster(t.getCluster(), Integer.parseInt(clusterIds[i]))) {
                            return true;
                        }
                    } catch (NumberFormatException nfe) {
                    }
                }
            }

        }
        return false;
    }

    /**
     * this cluster is or is in the cluster with id==clusterId
     */
    private boolean isInCluster(Clusters themaCluster, int clusterId) {
        if (themaCluster == null) {
            return false;
        } else if (themaCluster.getId() == clusterId) {
            return true;
        } else {
            return isInCluster(themaCluster.getParent(), clusterId);
        }
    }

    private Filter createSldFilter(Themas t, HttpServletRequest request) {
        if (doExtraSearchFilter(t, request)) {
            return FilterBuilder.createEqualsFilter(t.getSldattribuut(), request.getParameter(ViewerAction.SEARCH));
        }
        return null;
    }

    protected double[] getCoords(HttpServletRequest request) {
        double[] coords = null;
        if (request.getParameter("coords") != null && !request.getParameter("coords").equals("")) {
            String[] coordString = request.getParameter("coords").split(",");
            coords = new double[coordString.length];
            for (int i = 0; i < coordString.length; i++) {
                coords[i] = Double.parseDouble(coordString[i]);
            }
        }
        return coords;
    }

    protected Filter getExtraFilter(Themas t, HttpServletRequest request) {
        //controleer of er een extra filter meegegeven is en of die op dit thema moet worden toegepast.
        Filter sldFilter = createSldFilter(t, request);
        //controleer of er een organization code is voor dit thema
        String organizationcodekey = t.getOrganizationcodekey();
        String organizationcode = getOrganizationCode(request);
        if (FormUtils.nullIfEmpty(organizationcodekey) != null
                && FormUtils.nullIfEmpty(organizationcode) != null) {
            Filter organizationFilter = FilterBuilder.createEqualsFilter(organizationcodekey, organizationcode);
            if (sldFilter == null) {
                return organizationFilter;
            } else {
                return FilterBuilder.getFactory().and(sldFilter, organizationFilter);
            }
        }
        return sldFilter;
    }

    protected double getDistance(HttpServletRequest request) {
        String s = request.getParameter("scale");
        double scale = 0.0;
        try {
            if (s != null) {
                scale = Double.parseDouble(s);
                //af ronden op 6 decimalen
                scale = Math.round((scale * 1000000));
                scale = scale / 1000000;
            }
        } catch (NumberFormatException nfe) {
            scale = 0.0;
            log.debug("Scale is geen double dus wordt genegeerd");
        }
        String tolerance = request.getParameter("tolerance");
        double clickTolerance = DEFAULTTOLERANCE;
        try {
            if (tolerance != null) {
                clickTolerance = Double.parseDouble(tolerance);
            }
        } catch (NumberFormatException nfe) {
            clickTolerance = DEFAULTTOLERANCE;
            log.debug("Tolerance is geen double dus de default wordt gebruikt: " + DEFAULTTOLERANCE + " pixels");
        }
        double distance = clickTolerance;
        if (scale > 0.0) {
            distance = scale * (clickTolerance);
        }
        return distance;
    }
}
