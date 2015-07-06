package nl.b3p.gis.viewer;

import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.imagetool.CombineImageSettings;
import nl.b3p.imagetool.CombineImagesHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

/**
 *
 * @author jytte
 */
public class ImageAction extends PrintAction {

    private static final Log logFile = LogFactory.getLog(ImageAction.class);
    private static int maxResponseTime = 30000;

    @Override
     public ActionForward image(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String username = null;
        String password = null;
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        if (gp != null) {
            username = gp.getName();
            password = gp.getPassword();
        }

        //bbox ophalen
        Double minx = new Double(request.getParameter("minx"));
        Double miny = new Double(request.getParameter("miny"));
        Double maxx = new Double(request.getParameter("maxx"));
        Double maxy = new Double(request.getParameter("maxy"));
        minx = minx - 30;
        miny = miny - 30;
        maxx = maxx + 30;
        maxy = maxy + 30;
        String bbox = minx.toString() + "," + miny.toString() + "," + maxx.toString() + "," + maxy.toString();


        CombineImageSettings settings = null;
        String imageId = request.getParameter("imageId");
        if (imageId != null && request.getSession().getAttribute(imageId) != null) {
            settings = (CombineImageSettings) request.getSession().getAttribute(imageId);
            response.setContentType(settings.getMimeType());
            response.setDateHeader("Expires", System.currentTimeMillis() + (1000 * 60 * 60 * 24));

            String keepAlive = request.getParameter("keepAlive");
            if (keepAlive == null || keepAlive.length() == 0) {
                request.getSession().removeAttribute(imageId);
            }
        }
        if (settings == null) {
            logFile.error("No settings for image found");
            this.addAlternateMessage(mapping, request, null, "No settings for image found");
            return this.getAlternateForward(mapping, request);
        }

        //settings aanpassen
        settings.setHeight(300);
        settings.setWidth(300);
        settings.setBbox(bbox);

        OutputStream os = null;
        try {
            //response.setContentType("text/html;charset=UTF-8");
            os = response.getOutputStream();
            CombineImagesHandler.combineImage(response.getOutputStream(), settings,
                    settings.getMimeType(), maxResponseTime, username, password);
        } finally {
            os.close();
        }
        return null;
    }
}
