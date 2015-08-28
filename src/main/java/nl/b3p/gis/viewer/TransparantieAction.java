/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.viewer;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Geert
 * Created on 16-aug-2011, 13:58:51
 */
public class TransparantieAction extends ViewerCrudAction{
    
    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        BaseGisAction.setCMSPageFromRequest(request);
        return mapping.findForward(SUCCESS);
    }
}

