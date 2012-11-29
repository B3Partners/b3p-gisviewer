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

public class SolParcPaginaAction extends BaseGisAction {
    
    private static final Log logger = LogFactory.getLog(SolParcPaginaAction.class);
    protected static final String SOLPARC_MAATSCHAPPELIJK = "maatschappelijkevoorzieningen";
    protected static final String SOLPARC_ACTUEEL = "actuelezaken";
    protected static final String SOLPARC_WIJKGERICHT = "wijkgerichtwerken";
    protected static final String SOLPARC_OPENBARERUIMTE = "beheeropenbareruimte";
    protected static final String SOLPARC_NATUUR = "natuurmilieucultuurhistorie";
    protected static final String SOLPARC_GEMEENTE = "gemeenteopdekaart";
    private static final String PAGE_GISVIEWER_SOLPARC_MAATSCHAPPELIJK = "solparc_maatschappelijk";
    private static final String PAGE_GISVIEWER_SOLPARC_ACTUEEL = "solparc_actueel";
    private static final String PAGE_GISVIEWER_SOLPARC_WIJKGERICHT = "solparc_wijkgericht";
    private static final String PAGE_GISVIEWER_SOLPARC_OPENBARERUIMTE = "solparc_openbareruimte";
    private static final String PAGE_GISVIEWER_SOLPARC_NATUUR = "solparc_natuur";
    private static final String PAGE_GISVIEWER_SOLPARC_GEMEENTE = "solparc_gemeente";
    
    public ActionForward maatschappelijkevoorzieningen(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
            List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_SOLPARC_MAATSCHAPPELIJK);
            request.setAttribute("icon", PAGE_GISVIEWER_SOLPARC_MAATSCHAPPELIJK);
            request.setAttribute("titel", "Maatschappelijke voorzieningen");
            request.setAttribute("tekstBlokken", tekstBlokken);
            return getDefaultForward(mapping, request);
    }

    public ActionForward actuelezaken(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
            List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_SOLPARC_ACTUEEL);
            request.setAttribute("icon", PAGE_GISVIEWER_SOLPARC_ACTUEEL);
            request.setAttribute("titel", "Actuele zaken");
            request.setAttribute("tekstBlokken", tekstBlokken);
            return getDefaultForward(mapping, request);
    }

    public ActionForward wijkgerichtwerken(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
            List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_SOLPARC_WIJKGERICHT);
            request.setAttribute("icon", PAGE_GISVIEWER_SOLPARC_WIJKGERICHT);
            request.setAttribute("titel", "Wijkgericht werken");
            request.setAttribute("tekstBlokken", tekstBlokken);
            return getDefaultForward(mapping, request);
    }

    public ActionForward beheeropenbareruimte(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
            List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_SOLPARC_OPENBARERUIMTE);
            request.setAttribute("icon", PAGE_GISVIEWER_SOLPARC_OPENBARERUIMTE);
            request.setAttribute("titel", "Beheer openbare ruimte");
            request.setAttribute("tekstBlokken", tekstBlokken);
            return getDefaultForward(mapping, request);
    }

    public ActionForward natuurmilieucultuurhistorie(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
            List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_SOLPARC_NATUUR);
            request.setAttribute("icon", PAGE_GISVIEWER_SOLPARC_NATUUR);
            request.setAttribute("titel", "Natuur, milieu en historie");
            request.setAttribute("tekstBlokken", tekstBlokken);
            return getDefaultForward(mapping, request);
    }

    public ActionForward gemeenteopdekaart(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
            List tekstBlokken = getTekstBlokken(PAGE_GISVIEWER_SOLPARC_GEMEENTE);
            request.setAttribute("icon", PAGE_GISVIEWER_SOLPARC_GEMEENTE);
            request.setAttribute("titel", "Gemeente op de kaart");
            request.setAttribute("tekstBlokken", tekstBlokken);
            return getDefaultForward(mapping, request);
    }

    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;
        
        hibProp = new ExtendedMethodProperties(SOLPARC_MAATSCHAPPELIJK);
        hibProp.setDefaultForwardName(SOLPARC_MAATSCHAPPELIJK);
        hibProp.setAlternateForwardName(SOLPARC_MAATSCHAPPELIJK);
        map.put(SOLPARC_MAATSCHAPPELIJK, hibProp);

        hibProp = new ExtendedMethodProperties(SOLPARC_ACTUEEL);
        hibProp.setDefaultForwardName(SOLPARC_ACTUEEL);
        hibProp.setAlternateForwardName(SOLPARC_ACTUEEL);
        map.put(SOLPARC_ACTUEEL, hibProp);

        hibProp = new ExtendedMethodProperties(SOLPARC_WIJKGERICHT);
        hibProp.setDefaultForwardName(SOLPARC_WIJKGERICHT);
        hibProp.setAlternateForwardName(SOLPARC_WIJKGERICHT);
        map.put(SOLPARC_WIJKGERICHT, hibProp);

        hibProp = new ExtendedMethodProperties(SOLPARC_OPENBARERUIMTE);
        hibProp.setDefaultForwardName(SOLPARC_OPENBARERUIMTE);
        hibProp.setAlternateForwardName(SOLPARC_OPENBARERUIMTE);
        map.put(SOLPARC_OPENBARERUIMTE, hibProp);

        hibProp = new ExtendedMethodProperties(SOLPARC_NATUUR);
        hibProp.setDefaultForwardName(SOLPARC_NATUUR);
        hibProp.setAlternateForwardName(SOLPARC_NATUUR);
        map.put(SOLPARC_NATUUR, hibProp);

        hibProp = new ExtendedMethodProperties(SOLPARC_GEMEENTE);
        hibProp.setDefaultForwardName(SOLPARC_GEMEENTE);
        hibProp.setAlternateForwardName(SOLPARC_GEMEENTE);
        map.put(SOLPARC_GEMEENTE, hibProp);
        
        return map;
    }
}
