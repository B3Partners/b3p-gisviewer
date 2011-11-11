package nl.b3p.gis.viewer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

/**
 * @author Boy
*/
public class CyclomediaAction extends ViewerCrudAction {
    private static final Log log = LogFactory.getLog(CyclomediaAction.class);

    /**
     * This method is called when a user has clicked a link to a Cyclomedia image.
    */
    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String imageId = (String) request.getParameter("imageId");
        
        if (imageId != null && !imageId.equals("")) {            
            /* Fetch Cyclomedia configuration items */
            
            // apikey
            // accountid
            // wachtwoord
            // saved private key from uploaded file
            
            /* Genereate TID */
            
            /* Set TID and imageId for  jsp */
        }

        return mapping.findForward(SUCCESS);
    }
   
}
