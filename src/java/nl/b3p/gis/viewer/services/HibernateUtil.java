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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil extends HttpServlet {

    private static final Log log = LogFactory.getLog(HibernateUtil.class);
    private static final SessionFactory sessionFactory;
    public static String ANONIEM_ROL = "anoniem";
    public static String GEBRUIKERS_ROL = "gebruiker";
    public static String THEMABEHEERDERS_ROL = "themabeheerder";
    public static String DEMOGEBRUIKERS_ROL = "demogebruiker";
    public static String BEHEERDERS_ROL = "beheerder";
    public static String ANONYMOUS_USER = "anoniem";
    private static String kburl = null;
    private static boolean checkLoginKaartenbalie = true;
    private static String kaartenbalieCluster = "Extra";
    private static boolean useKaartenbalieCluster = true;
    public static String kbWfsConnectieNaam = "Kaartenbalie WFS";
    public static String hibernateDialect = null;

    static {
        try {
            Configuration config = new Configuration();
            config.setNamingStrategy(org.hibernate.cfg.ImprovedNamingStrategy.INSTANCE);
            sessionFactory = config.configure().buildSessionFactory();
            hibernateDialect = config.getProperty("dialect");
        } catch (Throwable ex) {
            log.error("Initial SessionFactory creation failed", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * http://www.kaartenbalie.nl/kaartenbalie/service/0c462abe62b69b2f05d1e72862f251f6
     * kburl: http://www.kaartenbalie.nl/kaartenbalie/service/
     * code:  0c462abe62b69b2f05d1e72862f251f6
     *
     * Code kan alleen worden toegevoegd indien de kaartenbalie url eindigt
     * op een /. Er wordt een / toegevoegd indien dit niet het geval is.
     *
     * @param code
     * @return
     */
    public static String createPersonalKbUrl(String code) {
        if (code != null && code.startsWith("http://")) {
            return code;
        }
        String url = getKbUrl();
        url = url.trim();
        if (code != null && code.length()>0) {
            String reqparam = "";
            int pos = url.indexOf("?");
            if (pos>=0) {
                reqparam = url.substring(pos);
                url = url.substring(0,pos);
            }
            if (url.lastIndexOf('/') == url.length() - 1) {
                url += code + reqparam;
            } else {
                url += '/' + code + reqparam;
            }
        }
        return url;
    }

    public static String getKbUrl() {
        return kburl;
    }

    public static void setKbUrl(String aKburl) {
        kburl = aKburl;
    }

    public static boolean isCheckLoginKaartenbalie() {
        return checkLoginKaartenbalie;
    }

    public static void setCheckLoginKaartenbalie(boolean aCheckLoginKaartenbalie) {
        checkLoginKaartenbalie = aCheckLoginKaartenbalie;
    }

    public static String getKaartenbalieCluster() {
        return kaartenbalieCluster;
    }

    public static void setKaartenbalieCluster(String aKaartenbalieCluster) {
        kaartenbalieCluster = aKaartenbalieCluster;
    }

    public static boolean isUseKaartenbalieCluster() {
        return useKaartenbalieCluster;
    }

    public static void setUseKaartenbalieCluster(boolean aUseKaartenbalieCluster) {
        useKaartenbalieCluster = aUseKaartenbalieCluster;
    }

    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            String value = config.getInitParameter("kburl");
            if (value != null && value.length() > 0) {
                kburl = value;
            }
            value = config.getInitParameter("check_login_kaartenbalie");
            if (value != null && value.equalsIgnoreCase("false")) {
                checkLoginKaartenbalie = false;
            }
            value = config.getInitParameter("use_kaartenbalie_cluster");
            if (value != null && value.equalsIgnoreCase("false")) {
                useKaartenbalieCluster = false;
            }
            value = config.getInitParameter("anonymous_user");
            if (value != null && value.length() > 0) {
                ANONYMOUS_USER = value;
            }
            value = config.getInitParameter("gebruikers_rol");
            if (value != null && value.length() > 0) {
                GEBRUIKERS_ROL = value;
            }
            value = config.getInitParameter("themabeheerders_rol");
            if (value != null && value.length() > 0) {
                THEMABEHEERDERS_ROL = value;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Returns the SessionFactory of Hibernate.
     *
     * @return SessionFactory met de Hibernate session factory.
     *
     * @see SessionFactory
     */
    // <editor-fold defaultstate="" desc="public static SessionFactory getSessionFactory()">
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    // </editor-fold>
}
