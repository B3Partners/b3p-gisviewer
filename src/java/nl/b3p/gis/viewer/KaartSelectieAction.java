package nl.b3p.gis.viewer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.db.UserKaartlaag;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.gis.viewer.db.UserLayerStyle;
import nl.b3p.gis.viewer.db.UserService;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

/**
 * @author Boy de Wit
 */
public class KaartSelectieAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(KaartSelectieAction.class);

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        return mapping.findForward(SUCCESS);
    }

    private void testAddService() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        UserService us = new UserService("123", "http://service.nl/service?", "Groep 1");
        UserLayer ul = new UserLayer(us, "laag 1", true, true);
        UserLayerStyle uls = new UserLayerStyle(ul, "default");

        ul.addStyle(uls);
        us.addLayer(ul);

        sess.save(us);
    }
}