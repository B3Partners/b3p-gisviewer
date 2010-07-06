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

import nl.b3p.gis.geotools.DataStoreUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.data.DataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

public class GetViewerDataAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(GetViewerDataAction.class);
    protected static final String ADMINDATA = "admindata";
    protected static final String AANVULLENDEINFO = "aanvullendeinfo";
    protected static final String METADATA = "metadata";
    protected static final String OBJECTDATA = "objectdata";
    protected static final String ANALYSEDATA = "analysedata";
    protected static final String ANALYSEWAARDE = "analysewaarde";
    protected static final String ANALYSEOBJECT = "analyseobject";
    protected static final String PK_FIELDNAME_PARAM = "pkFieldName";

    private static final int ANALYSE_TYPE_AVG = 3;
    private static final int ANALYSE_TYPE_SUM = 4;

    /**
     * Return een hashmap die een property koppelt aan een Action.
     *
     * @return Map hashmap met action properties.
     */
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;
        hibProp = new ExtendedMethodProperties(ANALYSEWAARDE);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.analysewaarde.failed");
        map.put(ANALYSEWAARDE, hibProp);

        hibProp = new ExtendedMethodProperties(ANALYSEDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.analysedata.failed");
        map.put(ANALYSEDATA, hibProp);

        hibProp = new ExtendedMethodProperties(ANALYSEOBJECT);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.analyseobject.failed");
        map.put(ANALYSEOBJECT, hibProp);

        hibProp = new ExtendedMethodProperties(OBJECTDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.objectdata.failed");
        map.put(OBJECTDATA, hibProp);

        hibProp = new ExtendedMethodProperties(ADMINDATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.admindata.failed");
        map.put(ADMINDATA, hibProp);

        hibProp = new ExtendedMethodProperties(AANVULLENDEINFO);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.aanvullende.failed");
        map.put(AANVULLENDEINFO, hibProp);

        hibProp = new ExtendedMethodProperties(METADATA);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("error.metadata.failed");
        map.put(METADATA, hibProp);

        return map;
    }

    /**
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
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return mapping.findForward(SUCCESS);
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * administratieve data.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema_items
     * regels
     */
    public ActionForward admindata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayList themas = getThemas(mapping, dynaForm, request);
        ArrayList regels = new ArrayList();
        ArrayList ti = null;
        if (themas == null) {
            request.setAttribute("regels_list", regels);
            request.setAttribute("thema_items_list", ti);
            return mapping.findForward("admindata");
        }

        for (int i = 0; i < themas.size(); i++) {
            Themas t = (Themas) themas.get(i);
            if (t.getAdmin_tabel() != null) {
                try {
                    List thema_items = SpatialUtil.getThemaData(t, true);
                    int themadatanummer = 0;
                    if (ti != null) {
                        themadatanummer = ti.size();
                    }
                    if (ti != null) {
                        for (int a = 0; a < ti.size(); a++) {
                            if (compareThemaDataLists((List) ti.get(a), thema_items)) {
                                themadatanummer = a;
                                break;
                            }
                        }
                    }

                    Bron b = t.getConnectie();
                    String connectieType = Connecties.TYPE_EMPTY;
                    List l = null;
                    if (b != null) {
                        if (themadatanummer == regels.size()) {
                            regels.add(new ArrayList());
                        }
                        l = getThemaObjectsWithGeom(t, thema_items, request);
                    }
                    if (l != null && l.size() > 0) {
                        ((ArrayList) regels.get(themadatanummer)).addAll(l);
                        if (ti == null) {
                            ti = new ArrayList();
                        }
                        if (themadatanummer == ti.size()) {
                            ti.add(thema_items);
                        }
                    }
                } catch (Exception e) {
                    log.error("Fout bij laden admindata voor thema: ", e);
                    addAlternateMessage(mapping, request, "", e.getMessage());
                }
            }
        }
        request.setAttribute("regels_list", regels);
        request.setAttribute("thema_items_list", ti);
        return mapping.findForward("admindata");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * aanvullende info.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema_items
     * regels
     */
    public ActionForward aanvullendeinfo(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean addKaart = false;
        if (FormUtils.nullIfEmpty(request.getParameter("addKaart")) != null) {
            addKaart = true;
        }
        Themas t = getThema(mapping, dynaForm, request);
        if (t == null) {
            return mapping.findForward("aanvullendeinfo");
        }

        List thema_items = SpatialUtil.getThemaData(t, false);
        request.setAttribute("thema_items", thema_items);

        Bron b = t.getConnectie();
        if (b != null) {
            request.setAttribute("regels", getThemaObjectsWithId(t, thema_items, request));
        }
        return mapping.findForward("aanvullendeinfo");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * metadata.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * thema
     */
    public ActionForward metadata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(mapping, dynaForm, request);
        request.setAttribute("themas", t);
        return mapping.findForward("metadata");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * "Gebieden" tab.
     * @param mapping ActionMapping
     * @param dynaForm DynaValidatorForm
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     *
     * @return ActionForward
     *
     * @throws Exception
     *
     * object_data
     *
     */
    public ActionForward objectdata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(mapping, dynaForm, request, true);
        if (t == null) {
            return mapping.findForward("objectdata");
        }
        Bron b = t.getConnectie();
        if (b != null) {
            List tol = createLocatieThemaList(mapping, dynaForm, request);
            request.setAttribute("object_data", tol);
        }
        return mapping.findForward("objectdata");
    }

    /**
     * Methode is attributen ophaalt welke nodig zijn voor het tonen van de
     * "Analyse" tab.
     * @param mapping The ActionMapping used to select this instance.
     * @param dynaForm The DynaValidatorForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     * @return an Actionforward object.
     * @throws Exception Exception
     *
     * object_data
     * thema
     * lagen
     * xcoord
     * ycoord
     *
     */
    public ActionForward analysedata(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List tol = createLocatieThemaList(mapping, dynaForm, request);
        request.setAttribute("object_data", tol);
        return mapping.findForward("analysedata");
    }

    /**
     * Methode wordt aangeroepen door knop "analysewaarde" op tabblad "Analyse"
     * en berekent een waarde op basis van actieve thema, plus evt. een
     * extra zoekcriterium voor de administratieve waarde in de basisregel, en
     * een gekozen gebied onder het klikpunt.
     * @param mapping The ActionMapping used to select this instance.
     * @param dynaForm The DynaValidatorForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     * @return an Actionforward object.
     * @throws Exception exception
     *
     * waarde
     */
    public ActionForward analysewaarde(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Maakt nu gebruik van het activeanalysethema property door true mee te geven
        Themas t = getThema(mapping, dynaForm, request, true);
        if (t == null) {
            request.removeAttribute("waarde");
            return mapping.findForward("analyseobject");
        }
        List thema_items = SpatialUtil.getThemaData(t, true);
        //maak het eventuele extra filter.
        String organizationcodekey = t.getOrganizationcodekey();
        String organizationcode = getOrganizationCode(request);
        String extraCriterium = dynaForm.getString("extraCriteria");
        Filter extraFilter = calculateExtraFilter(thema_items, extraCriterium,
                organizationcodekey, organizationcode, "tb1");

        //bepaal het geselecteerde analyse object (het object waar de analyse mee gedaan moet worden)
        String geselecteerdObject = dynaForm.getString("geselecteerd_object");
        if (geselecteerdObject == null || geselecteerdObject.length() == 0) {
            log.error("Er is geen analysegebied aangegeven!");
            throw new Exception("Er is geen analysegebied aangegeven!");
        }
        String[] tokens = geselecteerdObject.split("_");
        if (tokens.length != 3) {
            log.error("Id van analysegebied verkeerd geformatteerd!");
            throw new Exception("Id van analysegebied verkeerd geformatteerd!");
        }
        //haal het analyseObject thema en bron op.
        Themas analyseObjectThema = SpatialUtil.getThema(tokens[1]);
        if (analyseObjectThema == null) {
            log.error("Kan het geselecteerde thema object niet vinden!");
            throw new Exception("Kan het geselecteerde thema object niet vinden!");
        }
        String analyseGeomId = tokens[2];
        //Haal de geometry binnen waarmee de analyse moet worden uitgevoerd.
        Filter f= FilterBuilder.createEqualsFilter(analyseObjectThema.getAdmin_pk(), analyseGeomId);

        ArrayList<Feature> analyseFeatures=null;
        DataStore dsAnalyse = analyseObjectThema.getConnectie().toDatastore();
        try{
            String geometryName= DataStoreUtil.getSchema(dsAnalyse, t).getGeometryDescriptor().getLocalName();
            analyseFeatures=DataStoreUtil.getFeatures(dsAnalyse, analyseObjectThema, f,new String[]{geometryName},1);
        }
        finally{
            dsAnalyse.dispose();
        }
        if (analyseFeatures==null || analyseFeatures.size()==0){
            log.error("De gekozen geometry kan niet worden gevonden");
            request.setAttribute("waarde", "De gekozen geometry kan niet worden gevonden");
        }
        Geometry analyseGeometry = (Geometry) analyseFeatures.get(0).getDefaultGeometryProperty().getValue();
        
        Bron b = t.getConnectie();
        if (b == null) {
            log.error("Thema heeft geen connectie: "+t.getNaam());
            request.setAttribute("waarde", "Thema heeft geen connectie: "+t.getNaam());
            return mapping.findForward("analyseobject");
        }
        
        DataStore ds = b.toDatastore();
        try {
            GeometryType tgt = DataStoreUtil.getSchema(ds, t).getGeometryDescriptor().getType();
                        
            //Haal alle features op die binnen de analyseGeometry vallen:
            ArrayList<Feature> features=DataStoreUtil.getFeatures(t, analyseGeometry, extraFilter,thema_items,-1);
            int zow = 0;
            try {
                zow = Integer.parseInt(dynaForm.getString("zoekopties_waarde"));
            } catch (NumberFormatException nfe) {
                log.debug("zoekopties_waarde fout: ", nfe);
            }
            double resultValue=0.0;
            String analyseDescr="";
            switch (zow) {
                case 1:
                    //maximale waarde.
                    Feature maxFeature=getFeatureWithGeometry(features,tgt,"max");
                    Geometry maxgeom= (Geometry) maxFeature.getDefaultGeometryProperty().getValue();
                    if (tgt.getBinding() == Polygon.class || tgt.getBinding() == MultiPolygon.class){
                        resultValue=maxgeom.getArea();
                    }else if (tgt.getBinding() == LineString.class || tgt.getBinding() == MultiLineString.class){
                        resultValue=maxgeom.getLength();
                    }else if (tgt.getBinding() == Point.class || tgt.getBinding() == MultiPoint.class){
                        resultValue+=maxgeom.getNumGeometries();
                    }
                    break;
                case 2:
                    //minimale waarde
                    Feature minFeature=getFeatureWithGeometry(features,tgt,"min");
                    Geometry mingeom= (Geometry) minFeature.getDefaultGeometryProperty().getValue();
                    if (tgt.getBinding() == Polygon.class || tgt.getBinding() == MultiPolygon.class){
                        resultValue=mingeom.getArea();
                    }else if (tgt.getBinding() == LineString.class || tgt.getBinding() == MultiLineString.class){
                        resultValue=mingeom.getLength();
                    }else if (tgt.getBinding() == Point.class || tgt.getBinding() == MultiPoint.class){
                        resultValue+=mingeom.getNumGeometries();
                    }

                    break;
                case 3:
                    //gemiddelde waarde
                    double sum=getSumOfGeometries(features,tgt);
                    resultValue=sum/features.size();
                    break;
                case 4:
                    //totaal lengte/oppervlakte/aantal
                    resultValue=getSumOfGeometries(features,tgt);
                    break;
                default:
                    log.error("Er is geen geldige selectie aangegeven door middel van de radio buttons!");
                    throw new Exception("Er is geen geldige selectie aangegeven door middel van de radio buttons!");
            }
            /* Maak er een mooi getal van (km2 of km) en rond netjes af.*/
            if (tgt.getBinding() == Polygon.class || tgt.getBinding() == MultiPolygon.class){
                resultValue=Math.round(resultValue/1000000000)*1000;
            }else if (tgt.getBinding() == LineString.class || tgt.getBinding() == MultiLineString.class){
                resultValue=Math.round(resultValue/1000000)*1000;
            }

            StringBuffer result = new StringBuffer("");
            result.append("<b>" + analyseDescr + " " + t.getNaam());            
            result.append(": ");
            result.append(resultValue);
            result.append("</b>");
            request.setAttribute("waarde", result.toString());
        } finally {
            ds.dispose();
        }
        return mapping.findForward("analyseobject");
    }

    /**
     * Methode wordt aangeroepen door knop "analyseobject" op tabblad "Analyse"
     * en bepaalt alle objecten uit het actieve thema, plus evt. een
     * extra zoekcriterium voor de administratieve waarde in de basisregel, in
     * een gekozen gebied onder het klikpunt.
     *
     * @param mapping The ActionMapping used to select this instance.
     * @param dynaForm The DynaValidatorForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     *
     * @return an Actionforward object.
     *
     * @throws Exception
     *
     * thema_items
     * regels
     *
     */
    public ActionForward analyseobject(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Maakt nu gebruik van het activeanalysethema property door true mee te geven
        Themas t = getThema(mapping, dynaForm, request, true);
        if (t == null) {
            request.removeAttribute("waarde");
            return mapping.findForward("analyseobject");
        }
        List thema_items = SpatialUtil.getThemaData(t, true);
        //maak het eventuele extra filter.
        String organizationcodekey = t.getOrganizationcodekey();
        String organizationcode = getOrganizationCode(request);
        String extraCriterium = dynaForm.getString("extraCriteria");
        Filter extraFilter = calculateExtraFilter(thema_items, extraCriterium,
                organizationcodekey, organizationcode, "tb1");

        //bepaal het geselecteerde analyse object (het object waar de analyse mee gedaan moet worden)
        String geselecteerdObject = dynaForm.getString("geselecteerd_object");
        if (geselecteerdObject == null || geselecteerdObject.length() == 0) {
            log.error("Er is geen analysegebied aangegeven!");
            throw new Exception("Er is geen analysegebied aangegeven!");
        }
        String[] tokens = geselecteerdObject.split("_");
        if (tokens.length != 3) {
            log.error("Id van analysegebied verkeerd geformatteerd!");
            throw new Exception("Id van analysegebied verkeerd geformatteerd!");
        }
        //haal het analyseObject thema en bron op.
        Themas analyseObjectThema = SpatialUtil.getThema(tokens[1]);
        if (analyseObjectThema == null) {
            log.error("Kan het geselecteerde thema object niet vinden!");
            throw new Exception("Kan het geselecteerde thema object niet vinden!");
        }
        String analyseGeomId = tokens[2];
        //Haal de geometry binnen waarmee de analyse moet worden uitgevoerd.
        Filter f= FilterBuilder.createEqualsFilter(analyseObjectThema.getAdmin_pk(), analyseGeomId);
        ArrayList<Feature> analyseFeatures=DataStoreUtil.getFeatures(analyseObjectThema, null, f,thema_items,1);
        if (analyseFeatures.size()==0){
            log.error("De gekozen geometry kan niet worden gevonden");
            request.setAttribute("waarde", "De gekozen geometry kan niet worden gevonden");
        }
        Geometry analyseGeometry = (Geometry) analyseFeatures.get(0).getDefaultGeometryProperty().getValue();

        Bron b = t.getConnectie();
        if (b == null) {
            log.error("Thema heeft geen connectie: "+t.getNaam());
            request.setAttribute("waarde", "Thema heeft geen connectie: "+t.getNaam());
            return mapping.findForward("analyseobject");
        }

        DataStore ds = b.toDatastore();
        try {
            //GeometryType tgt = DataStoreUtil.getSchema(ds, t).getGeometryDescriptor().getType();
            String geometryAttributeName=DataStoreUtil.getGeometryAttributeName(ds, t);
            Filter analyseFilter = null;
            int zoo = 0;
            try {
                zoo = Integer.parseInt(dynaForm.getString("zoekopties_object"));
            } catch (NumberFormatException nfe) {
                throw new Exception("fout bij laden analyse data: ",nfe);
            }
            FilterFactory2 ff = FilterBuilder.getFactory();
            if (zoo == 1) {
                analyseFilter = ff.disjoint(ff.property(geometryAttributeName), FilterBuilder.getFactory().literal(analyseGeometry));
            } else if (zoo == 2) {
                analyseFilter = ff.within(ff.property(geometryAttributeName), FilterBuilder.getFactory().literal(analyseGeometry));                
            } else if (zoo == 3) {
                analyseFilter = ff.intersects(ff.property(geometryAttributeName), FilterBuilder.getFactory().literal(analyseGeometry));
            } else {
                throw new UnsupportedOperationException("Deze analyse/zoek_optie is niet geimplementeerd!");
            }
            if (analyseFilter==null){
                throw new Exception("Fout bij maken analyse filter");
            }
            Filter filter= null;
            if (extraFilter!=null){
                filter=ff.and(extraFilter,analyseFilter);
            }else{
                filter=analyseFilter;
            }
            //Haal alle features op met de geometry
            ArrayList<Feature> features=DataStoreUtil.getFeatures(ds,t, filter,DataStoreUtil.themaData2PropertyNames(thema_items),null);
            ArrayList<AdminDataRowBean> regels = new ArrayList();
            for (int i = 0; i < features.size(); i++) {
                Feature feature = features.get(i);
                regels.add(getRegel(feature, t, thema_items));
            }
            request.setAttribute("thema_items_list", thema_items);
            request.setAttribute("regels_list", regels);
        }finally{
            ds.dispose();
        }
        
        
        /*ArrayList regels = new ArrayList();
        ArrayList ti = new ArrayList();
        regels.add(getThemaObjects(t, pks, thema_items));
        ti.add(thema_items);
        request.setAttribute("thema_items_list", ti);
        request.setAttribute("regels_list", regels);
        */
        return mapping.findForward("admindata");
    }

    public static Filter calculateExtraFilter(List thema_items, String extraCriterium,
            String organizationcodekey, String organizationcode, String tableAlias) {
        ArrayList<Filter> andFilters = new ArrayList();
        if (thema_items != null && thema_items.size() > 0
                && extraCriterium != null && extraCriterium.length() > 0) {
            extraCriterium = extraCriterium.replaceAll("\\'", "''");
            Iterator it = thema_items.iterator();
            ArrayList<Filter> filters = new ArrayList();
            while (it.hasNext()) {
                ThemaData td = (ThemaData) it.next();
                Filter f = FilterBuilder.createLikeFilter(td.getKolomnaam(), '%' + extraCriterium + '%');
                if (f != null) {
                    filters.add(f);
                }
            }
            Filter f = FilterBuilder.getFactory().or(filters);
            if (f != null) {
                andFilters.add(f);
            }
        }

        if (organizationcode != null && organizationcode.length() > 0
                && organizationcodekey != null && organizationcodekey.length() > 0) {
            Filter f = FilterBuilder.createEqualsFilter(organizationcodekey, organizationcode);
            if (f != null) {
                andFilters.add(f);
            }
        }
        if (andFilters.size() == 0) {
            return null;
        }
        if (andFilters.size() == 1) {
            return andFilters.get(0);
        } else {
            return FilterBuilder.getFactory().and(andFilters);
        }
    }

    protected List createLocatieThemaList(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {

        ArrayList objectdata = new ArrayList();
        List ctl = getValidThemas(true, null, request);
        if (ctl == null || ctl.isEmpty()) {
            return null;
        }

        Iterator it = ctl.iterator();
        while (it.hasNext()) {
            Themas t = (Themas) it.next();
            List thema_items = SpatialUtil.getThemaData(t, true);
            //List pks = findPks(t, mapping, dynaForm, request);
            List ao = getThemaObjectsWithGeom(t, thema_items, request);
            if (ao == null || ao.isEmpty()) {
                continue;
            }
            ArrayList thema = new ArrayList();
            thema.add(t.getId());
            thema.add(t.getNaam());
            thema.add(ao);
            objectdata.add(thema);
        }
        return objectdata;
    }

    protected ArrayList<AdminDataRowBean> getThemaObjectsWithGeom(Themas t, List thema_items, HttpServletRequest request) throws Exception {
        if (t == null) {
            return null;
        }
        if (thema_items == null || thema_items.isEmpty()) {
            //throw new Exception("Er is geen themadata geconfigureerd voor thema: " + t.getNaam() + " met id: " + t.getId());
            return null;
        }
        Bron bron = t.getConnectie();
        //Haal de juiste info op
        Geometry geom = getGeometry(request);
        Filter extraFilter = getExtraFilter(t, request);
        ArrayList<Feature> features = DataStoreUtil.getFeatures(t, geom, extraFilter,thema_items, null);
        ArrayList<AdminDataRowBean> regels = new ArrayList();
        for (int i = 0; i < features.size(); i++) {
            Feature f = (Feature) features.get(i);
            regels.add(getRegel(f, t, thema_items));
        }
        return regels;
    }

    protected List getThemaObjectsWithId(Themas t, List thema_items, HttpServletRequest request) throws Exception {
        if (t == null) {
            return null;
        }
        if (thema_items == null || thema_items.isEmpty()) {
            return null;
        }
        //Bron b=t.getConnectie();

        String adminPk = t.getAdmin_pk();
        adminPk = removeNamespace(adminPk);
        String id = null;
        if (adminPk != null) {
            id = request.getParameter(adminPk);
        }
        if (id == null) {
            return null;
        }

        /*String extraCql= adminPk+" = ";
        try{
        int iid=Integer.parseInt(id);
        extraCql+=iid;
        }catch(NumberFormatException nfe){
        extraCql+="'"+id+"'";
        }*/
        Filter filter = FilterBuilder.createEqualsFilter(adminPk, id);
        List regels = new ArrayList();

        List<Feature> features = DataStoreUtil.getFeatures(t, null, filter, thema_items,null);
        for (int i = 0; i < features.size(); i++) {
            Feature f = (Feature) features.get(i);
            regels.add(getRegel(f, t, thema_items));
        }
        return regels;
    }

    /**
     *
     * @param t Themas
     * @param pks List
     * @param thema_items List
     *
     * @return List
     *
     * @throws SQLException, UnsupportedEncodingException
     *
     * @see Themas
     */
    /*protected List getThemaObjects(Themas t, List pks, List thema_items) throws SQLException, UnsupportedEncodingException, NotSupportedException, Exception {
    if (t == null) {
    return null;
    }
    if (pks == null || pks.isEmpty()) {
    return null;
    }
    if (thema_items == null || thema_items.isEmpty()) {
    return null;
    }
    Connecties c = t.getConnectie();
    if (c == null || !Connecties.TYPE_JDBC.equalsIgnoreCase(c.getType())) {
    log.error("Deze functie kun je alleen aanroepen met JDBC connecties");
    return null;
    }
    ArrayList regels = new ArrayList();

    Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
    Connection connection = t.getConnectie().getJdbcConnection();
    try {
    int dt = SpatialUtil.getPkDataType(t, connection);
    String taq = t.getAdmin_query();
    for (int i = 1; i <= pks.size(); i++) {
    PreparedStatement statement = connection.prepareStatement(taq);
    Object pkObject = pks.get(i - 1);
    if (pkObject == null) {
    continue;
    }
    switch (dt) {
    case java.sql.Types.SMALLINT:
    statement.setShort(1, ((Short) pkObject).shortValue());
    break;
    case java.sql.Types.INTEGER:
    statement.setInt(1, ((Integer) pkObject).intValue());
    break;
    case java.sql.Types.BIGINT:
    statement.setLong(1, ((Long) pkObject).longValue());
    break;
    case java.sql.Types.BIT:
    statement.setBoolean(1, ((Boolean) pkObject).booleanValue());
    break;
    case java.sql.Types.DATE:
    statement.setDate(1, (Date) pkObject);
    break;
    case java.sql.Types.DECIMAL:
    case java.sql.Types.NUMERIC:
    statement.setBigDecimal(1, (BigDecimal) pkObject);
    break;
    case java.sql.Types.REAL:
    statement.setFloat(1, ((Float) pkObject).floatValue());
    break;
    case java.sql.Types.FLOAT:
    case java.sql.Types.DOUBLE:
    statement.setDouble(1, ((Double) pkObject).doubleValue());
    break;
    case java.sql.Types.TIME:
    statement.setTime(1, (Time) pkObject);
    break;
    case java.sql.Types.TIMESTAMP:
    statement.setTimestamp(1, (Timestamp) pkObject);
    break;
    case java.sql.Types.TINYINT:
    statement.setByte(1, ((Byte) pkObject).byteValue());
    break;
    case java.sql.Types.CHAR:
    case java.sql.Types.LONGVARCHAR:
    case java.sql.Types.VARCHAR:
    statement.setString(1, (String) pkObject);
    break;
    case java.sql.Types.NULL:
    default:
    //                        SpatialUtil.testMetaData(connection);
    return null;
    }
    try {
    ResultSet rs = statement.executeQuery();
    while (rs.next()) {
    regels.add(getRegel(rs, t, thema_items));
    }
    } finally {
    statement.close();
    }
    }
    } finally {
    connection.close();
    }
    return regels;
    }*/
    /**
     * Haalt een envelop op van een bepaalde feature(s) aangegeven.
     * @param t Thema
     * @param attributeName attribuut naam waarmee de vergelijking wordt uitgevoerd.
     * @param attributeValue attribuut waarde die rechts van het = teken staat.
     * @return de envelop van alle gevonden features bij elkaar
     */
    /* public double[] getThemaEnvelope(Themas t, String attributeName, String attributeValue) {
    if (t == null) {
    return null;
    }
    Connecties c = t.getConnectie();
    if (c == null || !Connecties.TYPE_JDBC.equalsIgnoreCase(c.getType())) {
    log.error("Deze functie kun je alleen aanroepen met JDBC connecties");
    return null;
    }
    String envelope = null;
    try {
    Connection conn = t.getConnectie().getJdbcConnection();
    String geomColumn = SpatialUtil.getTableGeomName(t, conn);
    String tableName = t.getSpatial_tabel();
    if (tableName == null) {
    tableName = t.getAdmin_tabel();
    }

    String q = SpatialUtil.getEnvelopeQuery(tableName, geomColumn, attributeName, attributeValue);
    PreparedStatement statement = conn.prepareStatement(q);
    try {
    ResultSet rs = statement.executeQuery();
    if (rs.next()) {
    envelope = rs.getString(1);
    }
    } finally {
    statement.close();
    }
    } catch (SQLException se) {
    log.error("getFeatureEnvelope error ", se);
    }
    return SpatialUtil.wktEnvelope2bbox(envelope, 28992);
    }*/
    private Geometry getGeometry(HttpServletRequest request) {
        String geom = request.getParameter("geom");
        double distance = getDistance(request);
        Geometry geometry = null;
        if (geom != null) {
            geometry = SpatialUtil.geometrieFromText(geom, 28992);
        } else {
            GeometryFactory gf = new GeometryFactory();
            double[] coords = getCoords(request);
            if (coords.length == 2) {
                geometry = gf.createPoint(new Coordinate(coords[0], coords[1]));
            } else if (coords.length == 10) {
                Coordinate[] coordinates = new Coordinate[5];
                for (int i = 0; i < coordinates.length; i++) {
                    coordinates[i] = new Coordinate(coords[i * 2], coords[i * 2 + 1]);
                }
                geometry = gf.createPolygon(gf.createLinearRing(coordinates), null);
            }
        }
        if (geometry != null) {
            geometry = geometry.buffer(distance);
        }
        return geometry;
    }
    //calculatie voor gebruik bij analyse tool
    private double getSumOfGeometries(ArrayList<Feature> features,GeometryType gt) {
        Iterator<Feature> it = features.iterator();
        Class binding = gt.getBinding();
        double d=0.0;
        while(it.hasNext()){
            Feature f = it.next();
            Object o=f.getDefaultGeometryProperty().getValue();
            if (o!=null){
                Geometry geom = (Geometry)o;
                if (binding == Polygon.class || binding == MultiPolygon.class){
                    d+=geom.getArea();
                }else if (binding == LineString.class || binding == MultiLineString.class){
                    d+=geom.getLength();
                }else if (binding == Point.class || binding == MultiPoint.class){
                    d+=geom.getNumGeometries();
                }
            }
        }       
        return d;
    }
    private Feature getFeatureWithGeometry(ArrayList<Feature> features,GeometryType gt,String type){
        Class binding = gt.getBinding();
        if (binding == Point.class){
            return null;
        }
        Iterator<Feature> it = features.iterator();        
        Feature feature=null;
        while(it.hasNext()){
            Feature f = it.next();
            double value=0.0;
            Object o=f.getDefaultGeometryProperty().getValue();
            if (o!=null){
                if (feature==null){
                    feature=f;
                    continue;
                }
                Geometry featureGeom=(Geometry) feature.getDefaultGeometryProperty().getValue();
                Geometry currentGeom=(Geometry) f.getDefaultGeometryProperty().getValue();
                if (binding == Polygon.class || binding == MultiPolygon.class){
                    if ((type.equalsIgnoreCase("max") && currentGeom.getArea() > featureGeom.getArea()) ||
                            (type.equalsIgnoreCase("min") && currentGeom.getArea() < featureGeom.getArea())){
                        feature=f;
                    }
                }else if (binding == LineString.class ||binding == MultiLineString.class){
                    if ((type.equalsIgnoreCase("max") && currentGeom.getLength() > featureGeom.getLength()) ||
                            (type.equalsIgnoreCase("min") && currentGeom.getLength() < featureGeom.getLength())){
                        feature=f;
                    }
                }else if (binding == MultiPoint.class){
                    if ((type.equalsIgnoreCase("max") && currentGeom.getNumGeometries() > featureGeom.getNumGeometries()) ||
                            (type.equalsIgnoreCase("min") && currentGeom.getNumGeometries() < featureGeom.getNumGeometries())){
                        feature=f;
                    }
                }
            }
        }
        return feature;

    }
}
