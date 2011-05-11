/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.gis.viewer.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.zoeker.services.ZoekResultaat;
import nl.b3p.zoeker.services.Zoeker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 11-mei-2011, 14:58:39
 */
public class SearchAutocomplete extends HttpServlet {
    private static Log log = LogFactory.getLog(SearchAutocomplete.class);

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();
        try {
            String searchString = FormUtils.nullIfEmpty(request.getParameter("term"));
            searchString=("*"+searchString+"*");
            Integer zoekConfiguratieId=null;
            Integer maxResults=null;
            try{
                zoekConfiguratieId= new Integer(request.getParameter("zoekConfiguratieId"));
            }catch (Exception e){
                throw new ServletException("Missing zoekConfiguratieId or not a number",e);
            }
            try{
                maxResults=new Integer(request.getParameter("maxResults"));
            }catch(Exception e){
                log.info("no valid maxResult param found.");
                maxResults=null;
            }
            Zoeker zoeker = new Zoeker();
            List results=zoeker.zoek(new Integer[]{zoekConfiguratieId}, new String[]{searchString}, maxResults);

            JSONArray nieuw = new JSONArray();
            Iterator it=results.iterator();
            for(int i=0; it.hasNext(); i++){
                ZoekResultaat zoekResultaat = (ZoekResultaat) it.next();
               //JSONObject object = ja.getJSONObject(i);
                JSONObject obj = new JSONObject();
                obj.put("id", i);
                obj.put("label", zoekResultaat.getLabel());
                obj.put("value", zoekResultaat.getId());
                nieuw.put(obj);                
            }
            out.println(nieuw.toString());
        }catch (JSONException e) {
            log.error(e.getMessage());
        }finally{
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>


}
