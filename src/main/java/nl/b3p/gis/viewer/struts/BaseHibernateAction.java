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
/**
 * Een base class voor het initialiseren en uitvoeren van alle Hibernate actions.
 * Alle Actions die door een gebruiker of het systeem aangeroepen worden komen
 * langs deze klasse om de communicatie met Hibernate te bewerkstelligen. Eventuele
 * fouten zullen ook hier worden afgevangen en worden via deze klasse op een voor
 * de gebruiker duidelijke manier afgehandeld en op het scherm getoond.
 */
package nl.b3p.gis.viewer.struts;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.struts.ExtendedMethodAction;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hibernate.Session;
import org.hibernate.Transaction;

public abstract class BaseHibernateAction extends ExtendedMethodAction {

    private static final Log log = LogFactory.getLog(BaseHibernateAction.class);
    protected static final String SUCCESS = "success";
    protected static final String FAILURE = "failure";
    protected static final String VALIDATION_ERROR_KEY = "error.validation";
    protected static final String RIGHTS_ERROR_KEY = "error.rights";

    /**
     * Een Struts action die doorverwijst naar de strandaard forward.
     *
     * @param mapping ActionMapping die gebruikt wordt voor deze forward.
     * @param request HttpServletRequest die gebruikt wordt voor deze forward.
     *
     * @return ActionForward met de struts forward waar deze methode naar toe moet verwijzen.
     *
     */
    // <editor-fold defaultstate="" desc="protected ActionForward getUnspecifiedDefaultForward(ActionMapping mapping, HttpServletRequest request)">
    protected ActionForward getUnspecifiedDefaultForward(ActionMapping mapping, HttpServletRequest request) {
        return mapping.findForward(SUCCESS);
    }
    // </editor-fold>
    /**
     * Een Struts action execute die verwijst naar de standaard action als alles goed verloopt en anders een
     * alternatieve forward aanroept.
     *
     * @param mapping ActionMapping die gebruikt wordt voor deze forward.
     * @param form ActionForm die gebruikt wordt voor deze forward.
     * @param request HttpServletRequest die gebruikt wordt voor deze forward.
     * @param response HttpServletResponse die gebruikt wordt voor deze forward.
     *
     * @return ActionForward met de struts forward waar deze methode naar toe moet verwijzen.
     *
     * @throws Exception
     *
     */
    // <editor-fold defaultstate="" desc="public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception">
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        Session sess = getHibernateSession();
        Transaction tx = sess.beginTransaction();

        ActionForward forward = null;
        String msg = null;
        try {
            forward = super.execute(mapping, form, request, response);
            tx.commit();
            return forward;
        } catch (Exception e) {
            if(tx.isActive()) {
				tx.rollback();
			}
            log.error("Exception occured, rollback", e);

            if (e instanceof org.hibernate.JDBCException) {
                msg = e.getMessage();
                SQLException sqle = ((org.hibernate.JDBCException) e).getSQLException();
                msg = msg + ": " + sqle;
                SQLException nextSqlE = sqle.getNextException();
                if (nextSqlE != null) {
                    msg = msg + ": " + nextSqlE;
                }
            } else if (e instanceof java.sql.SQLException) {
                msg = e.getMessage();
                SQLException nextSqlE = ((java.sql.SQLException) e).getNextException();
                if (nextSqlE != null) {
                    msg = msg + ": " + nextSqlE;
                }
            } else {
                msg = e.getMessage();
            }
            addAlternateMessage(mapping, request, null, msg);
            return getAlternateForward(mapping, request);
        }

    }
    // </editor-fold>
    /**
     * Return de hibernate session.
     *
     * @return Session met de huidige hibernate session.
     *
     * @see Session
     *
     */
    // <editor-fold defaultstate="" desc="protected Session getHibernateSession()">
    protected Session getHibernateSession() {
        return HibernateUtil.getSessionFactory().getCurrentSession();
    }
    // </editor-fold>
}