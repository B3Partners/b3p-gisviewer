/*
 * Copyright (C) 2011 B3Partners B.V.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.ogc.sld.SldNamedLayer;
import nl.b3p.ogc.sld.SldWriter;
import nl.b3p.wms.capabilities.Layer;
import nl.b3p.wms.capabilities.Style;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Boy de Wit
 */
public class UserLayerSldServlet extends HttpServlet {
    
    private final String MIME_XML = "text/xml";

    private static final Log log = LogFactory.getLog(UserLayerSldServlet.class);

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("UserLayerSldServlet processRequest.");

        String ids = request.getParameter("layerids");
        
        if (ids != null || !ids.equals("")) {
            String[] layerids = ids.split(",");            
            List<Style> styles = getKbStyles(layerids);          
            SldWriter sldWriter = new SldWriter();
            
            try {
                List<SldNamedLayer> namedLayers = sldWriter.createNamedLayersWithKBStyles(styles);
                String body = sldWriter.createSLD(namedLayers);
                createSld(body, response);     
            } catch (SAXParseException sax) {
                log.error("SLD is ongeldig voor layerids: " + ids);
                writeErrorMessage(response, "SLD is ongeldig.");
            } catch (Exception ex) {
                log.error("Fout tijdens maken user sld: ", ex);
                writeErrorMessage(response, "Fout tijdens maken user sld.");
            }            
        } else {
            writeErrorMessage(response, "Geen geldig layerid meegegeven.");
        }
    }
    
    private List<Style> getKbStyles(String[] layerids) {
        List<Style> styles = new ArrayList<Style>();
        
        Session sess = null;
        Transaction tx = null;
        
        try {            
            sess = HibernateUtil.getSessionFactory().getCurrentSession();
            tx = sess.beginTransaction();
            
            for (int i=0; i < layerids.length; i++) {
                UserLayer layer = (UserLayer) sess.get(UserLayer.class, new Integer(layerids[i]));
                
                if (layer != null && layer.getSld_part() != null) {
                    String part = layer.getSld_part();
                
                    Style style = new Style();
                    style.setName(layer.getName());
                    style.setSldPart(part);

                    Layer l = new Layer();
                    l.setName(layer.getName());
                    style.setLayer(l);

                    styles.add(style);
                }
            }
            
            tx.commit();
        } catch (Exception ex) {
            log.error("Fout tijdens maken user sld: ", ex);
            
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
        
        return styles;
    }
    
    private void createSld(String body, HttpServletResponse response) {
        PrintWriter out = null;
        
        try {    
            out = response.getWriter();            
            response.setContentType(MIME_XML);            
            out.write(body);
        } catch (Exception ex) {
            log.error("Fout tijdens maken user sld: ", ex);
        } finally {
            out.close();
        }
    }
    
    private void writeErrorMessage(HttpServletResponse response, String message) {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>User Layer SLD Servlet Error</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("" + message + "");
            out.println("</body>");
            out.println("</html>");
            out.close();
        } catch (IOException ex) {
            log.error(ex);
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    // </editor-fold>
}
