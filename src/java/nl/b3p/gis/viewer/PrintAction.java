/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 * 
 * This file is part of B3P Gisviewer.
 * 
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.combineimages.CombineImagesServlet;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.struts.BaseHibernateAction;
import nl.b3p.imagetool.CombineImageSettings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

public class PrintAction extends BaseHibernateAction {

    private static final Log log = LogFactory.getLog(PrintAction.class);
    protected static final String PRINT = "print";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(PRINT);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(PRINT, hibProp);

        return map;

    }

    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        CombineImageSettings settings = CombineImagesServlet.getCombineImageSettings(request);
        String imageId = CombineImagesServlet.uniqueName("");
        request.getSession().setAttribute(imageId, settings);
        request.setAttribute("imageId", imageId);

        return mapping.findForward(SUCCESS);
    }

    public ActionForward print(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        return mapping.findForward(SUCCESS);
    }
}
