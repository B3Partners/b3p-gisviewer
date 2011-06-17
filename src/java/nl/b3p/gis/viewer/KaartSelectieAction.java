package nl.b3p.gis.viewer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.db.UserKaartlaag;
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
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();        

        //UserKaartlaag ukl = new UserKaartlaag("1", 1, true);
        //sess.save(ukl);

        //List user_kaartgroepen = sess.createQuery("from UserKaartgroep").list();
        //request.setAttribute("user_kaartgroepen", user_kaartgroepen);

        return mapping.findForward(SUCCESS);
    }
}