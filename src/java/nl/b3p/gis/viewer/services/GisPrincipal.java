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
package nl.b3p.gis.viewer.services;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import nl.b3p.gis.viewer.BaseGisAction;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.wms.capabilities.Layer;
import nl.b3p.wms.capabilities.Roles;
import nl.b3p.wms.capabilities.ServiceProvider;
import nl.b3p.wms.capabilities.Style;
import nl.b3p.wms.capabilities.StyleDomainResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.filter.SecurityRequestWrapper;

public class GisPrincipal implements Principal {

    private static final Log log = LogFactory.getLog(GisPrincipal.class);
    private String name;
    private String password;
    /*TODO ipv code misschien hele kaartenbalie url??? */
    private String code;
    private Set roles;
    private ServiceProvider sp;
    private Connecties kbWfsConnectie;

    public GisPrincipal(String name, List roles) {
        this.name = name;
        this.roles = new HashSet();
        this.roles.addAll(roles);
    }

    public GisPrincipal(String name, String password, String code, ServiceProvider sp) {
        this.name = name;
        this.password = password;
        this.code = code;
        this.sp = sp;
        //create wfs connectie object.
        kbWfsConnectie = new Connecties();
        kbWfsConnectie.setType(Connecties.TYPE_WFS);
        kbWfsConnectie.setConnectie_url(HibernateUtil.getKbUrl());
        kbWfsConnectie.setNaam(HibernateUtil.kbWfsConnectieNaam);
        kbWfsConnectie.setGebruikersnaam(name);
        kbWfsConnectie.setWachtwoord(password);
        if (sp == null) {
            return;
        }
        this.roles = new HashSet();
        Set sproles = sp.getAllRoles();
        if (sproles == null || sproles.isEmpty()) {
            return;
        }
        Iterator it = sproles.iterator();
        while (it.hasNext()) {
            Roles role = (Roles) it.next();
            String sprole = role.getRole();
            if (sprole != null && sprole.length() > 0) {
                roles.add(sprole);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isInRole(String role) {
        return roles.contains(role);
    }

    public Set getRoles() {
        return roles;
    }

    public String toString() {
        return "GisPrincipal[name=" + name + "]";
    }

    /* TODO: implement equals/hashCode */
    public ServiceProvider getSp() {
        return sp;
    }

    public void setSp(ServiceProvider sp) {
        this.sp = sp;
    }

    public List getLayerNames(boolean legendGraphicOnly) {
        if (sp == null) {
            return null;
        }
        Set layers = sp.getAllLayers();
        if (layers == null || layers.isEmpty()) {
            return null;
        }
        List allLayers = new ArrayList();
        Iterator it = layers.iterator();
        while (it.hasNext()) {
            Layer layer = (Layer) it.next();
            String name = layer.getName();
            if (name != null && name.length() > 0) {
                if ((legendGraphicOnly && hasLegendGraphic(layer)) ||
                        !legendGraphicOnly) {
                    allLayers.add(name);
                }
            }
        }
        if (allLayers != null) {
            Collections.sort(allLayers);
        }
        return allLayers;
    }

    public List getLayers(boolean legendGraphicOnly, boolean nameOnly) {
        if (sp == null) {
            return null;
        }
        Set layers = sp.getAllLayers();
        if (layers == null || layers.isEmpty()) {
            return null;
        }
        List allLayers = new ArrayList();
        Iterator it = layers.iterator();
        while (it.hasNext()) {
            Layer layer = (Layer) it.next();
            if ((legendGraphicOnly && hasLegendGraphic(layer)) ||
                    !legendGraphicOnly) {
                if ((nameOnly && layer.getName() != null) ||
                        !nameOnly) {
                    allLayers.add(layer);
                }
            }
        }

        Collections.sort(allLayers);
        return allLayers;
    }

    public boolean hasLegendGraphic(Layer l) {
        return getLegendGraphicUrl(l) != null;
    }

    public String getLegendGraphicUrl(Layer l) {
        if (l == null) {
            return null;
        }
        Set styles = l.getStyles();
        if (styles == null || styles.isEmpty()) {
            return null;
        }
        Iterator it = styles.iterator();
        String legendUrl = null;
        while (it.hasNext()) {
            Style style = (Style) it.next();
            Set ldrs = style.getDomainResource();
            if (ldrs != null && !ldrs.isEmpty()) {
                Iterator it2 = ldrs.iterator();
                while (it2.hasNext()) {
                    StyleDomainResource sdr = (StyleDomainResource) it2.next();
                    if ("LegendURL".equalsIgnoreCase(sdr.getDomain())) {
                        legendUrl = sdr.getUrl();
                        if (style.getName().equalsIgnoreCase("default")) {
                            return legendUrl;
                        }
                    }
                }
            }
        }
        return legendUrl;
    }

    public Layer getLayer(String layerName) {
        if (sp == null) {
            return null;
        }
        Set layers = sp.getAllLayers();
        if (layers == null || layers.isEmpty()) {
            return null;
        }
        Iterator it = layers.iterator();
        while (it.hasNext()) {
            Layer layer = (Layer) it.next();
            String name = layer.getName();
            if (name == null || name.length() == 0) {
                continue;
            }
            if (name.equalsIgnoreCase(layerName)) {
                return layer;
            }
        }
        return null;
    }

    public String getLayerTitle(String layerName) {
        Layer layer = getLayer(layerName);
        if (layer == null) {
            return null;
        }
        return layer.getTitle();
    }

    public static GisPrincipal getGisPrincipal(HttpServletRequest request) {
        Principal user = request.getUserPrincipal();
        if (!(user instanceof GisPrincipal && request instanceof SecurityRequestWrapper)) {
            return null;
        }
        GisPrincipal gp = (GisPrincipal) user;

        String code = request.getParameter(BaseGisAction.URL_AUTH);
        if (code != null && code.length() != 0) {
            if (gp!=null && code.equals(gp.getCode())) {
                return gp;
            }

            // user is using different code, so invalidate session and login again
            HttpSession session = request.getSession();
            session.invalidate();
            String url = GisSecurityRealm.createCapabilitiesURL(code);
            gp = GisSecurityRealm.authenticateHttp(url, HibernateUtil.ANONYMOUS_USER, null, code);
        }

        // no principal, is login required?
        if (gp == null && !HibernateUtil.isCheckLoginKaartenbalie()) {
            // Fake Principal aanmaken
            gp = GisSecurityRealm.authenticateFake(HibernateUtil.ANONYMOUS_USER);
        }
        
        // log in found principal
        if (gp != null) {
            SecurityRequestWrapper srw = (SecurityRequestWrapper) request;
            srw.setUserPrincipal(gp);
            log.debug("Automatic login for user: " + gp.name);
        }

        return gp;
    }

    /**
     * @return the kbWfsConnectie
     */
    public Connecties getKbWfsConnectie() {
        return kbWfsConnectie;
    }

    /**
     * @param kbWfsConnectie the kbWfsConnectie to set
     */
    public void setKbWfsConnectie(Connecties kbWfsConnectie) {
        this.kbWfsConnectie = kbWfsConnectie;
    }
}
