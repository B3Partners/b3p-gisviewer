package nl.b3p.gis.viewer.services;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Boy de Wit
 */
public class GetThemesServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(GetThemesServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String themes = "";
        
        String path = getServletContext().getRealPath("/");
        
        /* Add / or \ if needed */
        if (!path.substring(path.length()-1).equals(File.separator)) {
            path += File.separator;
        }
        
        File folder = new File(path + "themes");
        
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                if (themes.equals("")) {
                    themes += fileEntry.getName();
                } else {
                    themes += "," + fileEntry.getName();
                }                
            }
        }

        response.getWriter().print(themes);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return 
     */
    @Override
    public String getServletInfo() {
        return "Servlet to get a String with web/themes folder listing.";
    }
    // </editor-fold>
}
