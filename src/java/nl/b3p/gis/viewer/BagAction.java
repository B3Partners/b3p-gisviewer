/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.viewer;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        String appCode = request.getParameter(ViewerAction.APPCODE);
        Applicatie app = KaartSelectieUtil.getApplicatie(appCode);

        if (app == null) {
            Applicatie defaultApp = KaartSelectieUtil.getDefaultApplicatie();

            if (defaultApp != null)
                app = defaultApp;
        }

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map instellingen = configKeeper.getConfigMap(app.getCode());

        /* Indien niet aanwezig dan defaults laden */
        if ((instellingen == null) || (instellingen.size() < 1)) {
            instellingen = configKeeper.getDefaultInstellingen();
        }

        Integer bagkaartlaagid = (Integer) instellingen.get("bagkaartlaagid");

        if (bagkaartlaagid == null || bagkaartlaagid < 1) {
            addAlternateMessage(mapping, request, "Er is nog geen BAG Kaartlaag geconfigureerd door de beheerder.");
            return mapping.findForward(FAILURE);
        }
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Themas bagKaartLaag = (Themas) sess.get(Themas.class,bagkaartlaagid);
        //panden
        Gegevensbron pandengb= bagKaartLaag.getGegevensbron();
        if (pandengb==null){
            addAlternateMessage(mapping, request, "De BAG Kaartlaag die voor de BAG module is geconfigureerd heeft geen Gegevensbron");
            return mapping.findForward(FAILURE);  
        }   
        //Verblijfsobjecten
        List<Gegevensbron> childs= sess.createQuery("from Gegevensbron where parent = :p").setParameter("p", pandengb).list();
        if (childs.size()>1){
            addAlternateMessage(mapping, request, "De Gegevensbron van de  BAG Kaartlaag die voor de BAG module is geconfigureerd heeft meerdere childs, de eerste is gekozen.");
        }
        if (childs.size()==0){
            addAlternateMessage(mapping, request, "De Gegevensbron van de  BAG Kaartlaag die voor de BAG module is geconfigureerd heeft geen childs");
            return mapping.findForward(FAILURE);  
        }
        Gegevensbron verblijfsObjectengb = childs.get(0);             
        //set de attributen
        request.setAttribute(BAGTHEMAID, bagkaartlaagid);
        request.setAttribute(PANDENGEGEVENSBRONID, pandengb.getId());
        request.setAttribute(VERBIJFSOBJECTENGEGEVENSBRONID, verblijfsObjectengb.getId());  
        
        /************************************************
         * Set de max/min slider waarden
         */
        Integer maxBouwJaar = new Integer("1");
        Integer minBouwJaar = new Integer("0");
        Integer maxOpp = new Integer("1");
        Integer minOpp = new Integer("0");
        if(instellingen.get("bagMaxBouwjaar")==null)
            addAlternateMessage(mapping,request,"Geen maximaal bouwjaar ingesteld in de BAG module configuratie");
        else
            maxBouwJaar = (Integer) instellingen.get("bagMaxBouwjaar");
        if(instellingen.get("bagMinBouwjaar")==null)
            addAlternateMessage(mapping,request,"Geen minimaal bouwjaar ingesteld in de BAG module configuratie");
        else
            minBouwJaar = (Integer) instellingen.get("bagMinBouwjaar");
        if(instellingen.get("bagMaxOpp")==null)
            addAlternateMessage(mapping,request,"Geen maximaal oppervlakte ingesteld in de BAG module configuratie");
        else
            maxOpp = (Integer) instellingen.get("bagMaxOpp");
        if(instellingen.get("bagMinOpp")==null)
            addAlternateMessage(mapping,request,"Geen minimaal oppervlakte ingesteld in de BAG module configuratie");
        else
            minOpp = (Integer) instellingen.get("bagMinOpp");
        
        request.setAttribute("bagMaxBouwjaar", maxBouwJaar);
        request.setAttribute("bagMinBouwjaar", minBouwJaar);
        request.setAttribute("bagMaxOpp", maxOpp);
        request.setAttribute("bagMinOpp", minOpp);
        
        if(instellingen.get("bagBouwjaarAttr")==null)
            addAlternateMessage(mapping,request,"In de configuratie van de BAG module is de attribuut naam voor Bouwjaar niet ingevuld");
        else
            request.setAttribute("bagBouwjaarAttr",instellingen.get("bagBouwjaarAttr"));
        if(instellingen.get("bagOppAttr")==null)
            addAlternateMessage(mapping,request,"In de configuratie van de BAG module is de attribuut naam voor Oppervlakte niet ingevuld");
        else
            request.setAttribute("bagOppAttr",instellingen.get("bagOppAttr"));
        if(instellingen.get("bagGebruiksfunctieAttr")==null)
            addAlternateMessage(mapping,request,"In de configuratie van de BAG module is de attribuut naam voor Gebruiksfunctie niet ingevuld");
        else
            request.setAttribute("bagGebruiksfunctieAttr",instellingen.get("bagGebruiksfunctieAttr"));
        if(instellingen.get("bagGeomAttr")==null)
            addAlternateMessage(mapping,request,"In de configuratie van de BAG module is de attribuut naam voor Geometrie niet ingevuld");
        else
            request.setAttribute("bagGeomAttr",instellingen.get("bagGeomAttr"));
        
        return mapping.findForward(SUCCESS);
    }
}

