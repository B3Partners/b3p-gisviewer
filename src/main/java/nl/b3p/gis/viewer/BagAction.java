/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.viewer;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 16-aug-2011, 13:58:51
 */
public class BagAction extends ViewerCrudAction{
    private final static String BAGTHEMAID = "bagThemaId";
    private final static String PANDENGEGEVENSBRONID = "pandenGegevensBronId";
    private final static String VERBIJFSOBJECTENGEGEVENSBRONID = "verblijfsObjectenGegevensBronId";
    
    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        /* Applicatie instellingen ophalen */
        Applicatie app = null;
        HttpSession session = request.getSession(true);
        String appCode = (String) session.getAttribute("appCode");
        ConfigKeeper configKeeper = new ConfigKeeper();
        Map instellingen = configKeeper.getConfigMap(appCode, true);
        BaseGisAction.setCMSPageFromRequest(request);
        Integer bagkaartlaagid = (Integer) instellingen.get("bagkaartlaagid");

        if (bagkaartlaagid == null || bagkaartlaagid < 1) {
            addAlternateMessage(mapping, request, "bag.kaartlaag.error");
            return mapping.findForward(FAILURE);
        }
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Themas bagKaartLaag = (Themas) sess.get(Themas.class,bagkaartlaagid);
        //panden
        Gegevensbron pandengb= bagKaartLaag.getGegevensbron();
        if (pandengb==null){
            addAlternateMessage(mapping, request, "bag.kaartlaag.geen.bron");
            return mapping.findForward(FAILURE);  
        }   
        //Verblijfsobjecten
        List<Gegevensbron> childs= sess.createQuery("from Gegevensbron where parent = :p").setParameter("p", pandengb).list();
        if (childs.size()>1){
            addAlternateMessage(mapping, request, "bag.kaartlaag.multi.childs");
        }
        if (childs.isEmpty()){
            addAlternateMessage(mapping, request, "bag.kaartlaag.no.child");
            return mapping.findForward(FAILURE);  
        }
        Gegevensbron verblijfsObjectengb = childs.get(0);
        request.setAttribute(BAGTHEMAID, bagkaartlaagid);
        request.setAttribute(PANDENGEGEVENSBRONID, pandengb.getId());
        request.setAttribute(VERBIJFSOBJECTENGEGEVENSBRONID, verblijfsObjectengb.getId());  
        
        /* Set values for jsp */
        Integer maxBouwJaar = new Integer("1");
        Integer minBouwJaar = new Integer("0");
        Integer maxOpp = new Integer("1");
        Integer minOpp = new Integer("0");
        String bagBouwjaarAttr = null;
        String bagOppAttr = null;
        String bagGebruiksfunctieAttr = null;
        String bagGeomAttr = null;

        if(instellingen.get("bagMaxBouwjaar")==null) {
            addAlternateMessage(mapping,request,"bag.config.no.maxbouwjaar");
        } else {
            maxBouwJaar = (Integer) instellingen.get("bagMaxBouwjaar");
        }        
        if(instellingen.get("bagMinBouwjaar")==null) {
            addAlternateMessage(mapping,request,"bag.config.no.minbouwjaar");
        } else {
            minBouwJaar = (Integer) instellingen.get("bagMinBouwjaar");
        }
        if(instellingen.get("bagMaxOpp")==null) {
            addAlternateMessage(mapping,request,"bag.config.no.maxopp");
        } else {
            maxOpp = (Integer) instellingen.get("bagMaxOpp");
        }
        if(instellingen.get("bagMinOpp")==null) {
            addAlternateMessage(mapping,request,"bag.config.no.minopp");
        } else {
            minOpp = (Integer) instellingen.get("bagMinOpp");
        }
        if (instellingen.get("bagBouwjaarAttr") == null) {
            addAlternateMessage(mapping,request,"bag.config.no.attrib.bouwjaar");
        } else {
            bagBouwjaarAttr = (String) instellingen.get("bagBouwjaarAttr");
        }
        if (instellingen.get("bagOppAttr") == null) {
            addAlternateMessage(mapping,request,"bag.config.no.attrib.opp");
        } else {
            bagOppAttr = (String) instellingen.get("bagOppAttr");
        }
        if (instellingen.get("bagGebruiksfunctieAttr") == null) {
            addAlternateMessage(mapping,request,"bag.config.no.attrib.gfunctie");
        } else {
            bagGebruiksfunctieAttr = (String) instellingen.get("bagGebruiksfunctieAttr");
        }
        if (instellingen.get("bagGeomAttr") == null) {
            addAlternateMessage(mapping,request,"bag.config.no.attrib.geom");
        } else {
            bagGeomAttr = (String) instellingen.get("bagGeomAttr");
        }
        
        request.setAttribute("bagMaxBouwjaar", maxBouwJaar);
        request.setAttribute("bagMinBouwjaar", minBouwJaar);
        request.setAttribute("bagMaxOpp", maxOpp);
        request.setAttribute("bagMinOpp", minOpp);

        request.setAttribute("bagBouwjaarAttr", bagBouwjaarAttr);
        request.setAttribute("bagOppAttr", bagOppAttr);
        request.setAttribute("bagGebruiksfunctieAttr", bagGebruiksfunctieAttr);
        request.setAttribute("bagGeomAttr", bagGeomAttr);

        return mapping.findForward(SUCCESS);
    }
}