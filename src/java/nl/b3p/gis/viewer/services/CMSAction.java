package nl.b3p.gis.viewer.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.BaseGisAction;
import nl.b3p.gis.viewer.db.CMSPagina;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.*;
import org.apache.struts.validator.DynaValidatorForm;

public class CMSAction extends BaseGisAction {

    private static final Log logger = LogFactory.getLog(CMSAction.class);
    
    private static final String CMS = "cms";   
    private static final String CMS_PAGE_ID = "cmsPageId";
    private static final String CMS_THEME = "theme";
    private static final String PAGE_GISVIEWER_HOME = "gisviewer_home";

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
        
        populateCMSPage(request, cmsPageId);
        populateTekstblok(request, PAGE_GISVIEWER_HOME);

        return mapping.findForward(SUCCESS);
    }

    private void populateTekstblok(HttpServletRequest request, String page) {
        List tekstBlokken = getTekstBlokken(page);
        request.setAttribute("tekstBlokken", tekstBlokken);
    }
    
    private void populateCMSPage(HttpServletRequest request, Integer pageID) {        
        if (pageID != null && pageID > 0) {
            CMSPagina cmsPage = getCMSPage(pageID);
        
            request.setAttribute("cmsPage", cmsPage);
            request.setAttribute("theme", cmsPage.getThema());
        }        
        
        /* User can also set theme directly, eq &theme=b3p */
        String theme = request.getParameter(CMS_THEME);        
        if (theme != null && !theme.equals("")) {
            request.setAttribute("theme", theme);
        }
        
        if (theme == null && pageID == null) {
            request.setAttribute("theme", "");
        }
    }
}
