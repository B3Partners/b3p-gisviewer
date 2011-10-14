package nl.b3p.gis.viewer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.downloads.Dispatcher;
import nl.b3p.gis.viewer.services.DispatcherServlet;
import nl.b3p.gis.viewer.downloads.DownloadThread;
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

    private final String UUIDS = "uuids";
    private final String EMAIL = "email";
    private final String FORMAAT = "formaat";

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String[] uuids = dynaForm.getString(UUIDS).split(",");

        if (uuids == null || uuids.length < 1) {
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "Er zijn geen kaarten geselecteerd");
        }

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String[] uuids = dynaForm.getString(UUIDS).split(",");

        if (uuids != null && uuids.length > 0) {
            Dispatcher dispatcher = DispatcherServlet.getDispatcher();
            DownloadThread cdt = new DownloadThread(dispatcher.getThreadGroup());

            cdt.setUuids(uuids);
            cdt.setEmail(dynaForm.getString(EMAIL));
            cdt.setFormaat(dynaForm.getString(FORMAAT));

            dispatcher.addCall(cdt);
        }else{
            addAlternateMessage(mapping, request, GENERAL_ERROR_KEY, "Er zijn geen kaarten geselecteerd");
        }

        return mapping.findForward(SUCCESS);
    }
}
