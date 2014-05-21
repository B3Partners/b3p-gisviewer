package nl.b3p.gis.viewer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.downloads.Dispatcher;
import nl.b3p.gis.viewer.services.DispatcherServlet;
import nl.b3p.gis.viewer.downloads.DownloadThread;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForward;
import org.apache.struts.validator.DynaValidatorForm;

/**
 *
 * @author Boy
 */
public class DownloadAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(DownloadAction.class);

    private final String EMAIL = "email";
    private final String FORMAAT = "formaat";

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String id = (String) request.getParameter("id");

        if (id == null || id.equals("-1")) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "Er zijn geen kaarten geselecteerd.");
        } else {
            dynaForm.set("uuids", id);
        }

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        String id = dynaForm.getString("uuids");
        String wkt = dynaForm.getString("wkt");

        if (id != null && !id.equals("-1")) {
            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            Bron kbBron = user.getKbWfsConnectie();

            Dispatcher dispatcher = DispatcherServlet.getDispatcher();
            DownloadThread cdt = new DownloadThread(dispatcher.getThreadGroup());

            String[] uuids = {id};
            cdt.setUuids(uuids);
            cdt.setEmail(dynaForm.getString(EMAIL));
            cdt.setFormaat(dynaForm.getString(FORMAAT));

            if (kbBron != null) {
                cdt.setKaartenbalieBron(kbBron);
            }
            
            if (wkt != null && !wkt.equals("")) {
                cdt.setWkt(wkt);
            }

            String servletpath = request.getRequestURL().toString();
            servletpath = servletpath.substring(0,servletpath.lastIndexOf("/"));
            cdt.setApplicationPath(servletpath);

            dispatcher.addCall(cdt);
            request.setAttribute("emailScheduled", true);
        }else{
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "Er zijn geen kaarten geselecteerd");
        }        

        return mapping.findForward(SUCCESS);
    }
}
