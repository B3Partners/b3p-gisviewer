/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.viewer;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        Map instellingen=getInstellingenMap(request);
        if(instellingen.get("bagkaartlaagid")==null){
            addAlternateMessage(mapping, request, "Er is geen BAG kaartlaag geconfigureerd in het viewerontwerp.");
            return mapping.findForward(FAILURE);  
        }
        Integer bagkaartlaagid= (Integer)instellingen.get("bagkaartlaagid");
        
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
        return mapping.findForward(SUCCESS);
    }
}

