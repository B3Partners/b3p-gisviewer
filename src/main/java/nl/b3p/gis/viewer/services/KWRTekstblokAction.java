/*
 * Copyright (C) 2012 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.BaseGisAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

public class KWRTekstblokAction extends BaseGisAction {
    
    private static final Log logger = LogFactory.getLog(KWRTekstblokAction.class);
    protected static final String KWRPROJECTEN = "kwrprojecten";
    private static final String PAGE_GISVIEWER_KWRPROJECTEN = "gisviewer_kwrprojecten";
    
    public ActionForward kwrprojecten(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String param = request.getParameter(BaseGisAction.CMS_PAGE_ID);          
        Integer cmsPageId = null;
        
        if (param != null && !param.equals("")) {
            cmsPageId = new Integer(param);
        }        
        
        List tekstBlokken = getTekstBlokken(cmsPageId);
        request.setAttribute("tekstBlokken", tekstBlokken);
        
        return getDefaultForward(mapping, request);
    }
    
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();
        ExtendedMethodProperties hibProp = new ExtendedMethodProperties(KWRPROJECTEN);
        hibProp.setDefaultForwardName(KWRPROJECTEN);
        hibProp.setAlternateForwardName(KWRPROJECTEN);
        map.put(KWRPROJECTEN, hibProp);
        return map;
    }
}
