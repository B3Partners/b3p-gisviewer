package nl.b3p.gis.viewer.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.BaseGisAction;
import nl.b3p.gis.viewer.db.CMSMenuItem;
import nl.b3p.gis.viewer.db.CMSPagina;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.*;
import org.apache.struts.validator.DynaValidatorForm;
import org.securityfilter.filter.SavedRequest;
import org.securityfilter.filter.SecurityFilter;

public class CMSAction extends BaseGisAction {

    private static final Log logger = LogFactory.getLog(CMSAction.class);
    private static final String CMS = "cms";    

    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp;

        hibProp = new ExtendedMethodProperties(CMS);
        hibProp.setDefaultForwardName(CMS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(CMS, hibProp);

        return map;
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String param = request.getParameter(CMS_PAGE_ID);

        Integer cmsPageId = null;

        if (param != null && !param.equals("")) {
            cmsPageId = new Integer(param);
        }

        if (request.getParameter("id") != null) {
            cmsPageId = Integer.parseInt(request.getParameter("id"));
        }

        /* Indien voor cms pagina ingelogd moet worden dan redirecten */
        CMSPagina cmsPage = getCMSPage(cmsPageId);
        Boolean loginRequired = null;
        if(cmsPage == null){
            
            return new RedirectingActionForward("/http_404.do");
        }
        
        if (cmsPage.getLoginRequired()) {
            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            if (user == null) {
                String url = prettifyCMSPageUrl(request, cmsPage);

                HttpSession session = request.getSession();
                session.setAttribute(SecurityFilter.SAVED_REQUEST_URL, url);
                session.setAttribute(SecurityFilter.SAVED_REQUEST, new SavedRequest(request));

                return new RedirectingActionForward("/login.do");
            }

        }

        populateCMSPage(request, cmsPageId);

        if (cmsPageId != null && cmsPageId > 0) {
            populateTekstblok(request, cmsPageId);
        }

        return mapping.findForward(SUCCESS);
    }

    private void populateTekstblok(HttpServletRequest request, Integer cmsPageId) {
        List tekstBlokken = getTekstBlokken(cmsPageId);
        request.setAttribute("tekstBlokken", tekstBlokken);
    }

    private void populateCMSPage(HttpServletRequest request, Integer pageID) {
        CMSPagina cmsPage = null;

        if (pageID != null && pageID > 0) {
            cmsPage = getCMSPage(pageID);

            if (cmsPage != null) {
                request.setAttribute("cmsPage", cmsPage);

                if (cmsPage.getThema() == null || cmsPage.getThema().equals("")) {
                    request.setAttribute("theme", "");
                } else {
                    request.setAttribute("theme", cmsPage.getThema());
                }

                if (cmsPage.getCmsMenu() != null) {
                    List<CMSMenuItem> items = getCMSMenuItems(cmsPage.getCmsMenu());
                    request.setAttribute("cmsMenuItems", items);
                }

            }
        }

        if (cmsPage == null) {
            request.setAttribute("theme", "");
        }else{
            if (cmsPage.getShowPlainAndMapButton()) {
                request.setAttribute("showPlainAndMapButton", true);
            }
        }

    }
}
