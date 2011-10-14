package nl.b3p.gis.viewer.services;

import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.viewer.downloads.Dispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Boy
 */
public class DispatcherServlet extends HttpServlet {
   private static final Log log = LogFactory.getLog(DispatcherServlet.class);
   private static ArrayList threads = new ArrayList();
   private static ArrayList finishedThreads = new ArrayList();
   private static int maxThreadLog=300;
   private static int refreshRate=10;
   private static int dispatcherSleepTime=10000;
   private static int maxAliveThreads=10;
   private static Dispatcher dispatcher = null;


    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
            }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            if (config.getInitParameter("maxThreadLog") != null) {
                maxThreadLog=Integer.parseInt(config.getInitParameter("maxThreadLog"));
            }
            if (config.getInitParameter("refreshRate") != null) {
                refreshRate=Integer.parseInt(config.getInitParameter("refreshRate"));
            }
            if (config.getInitParameter("dispatcherSleepTime")!=null){
                dispatcherSleepTime=Integer.parseInt(config.getInitParameter("dispatcherSleepTime"));
            }
            if (config.getInitParameter("maxAliveThreads")!=null){
                maxAliveThreads=Integer.parseInt(config.getInitParameter("dispatcherSleepTime"));
            }

        } catch (Exception e) {
            log.error("",e);
            throw new ServletException(e);
        }
        int priority = Thread.currentThread().getPriority(); // TODO what value?
        ThreadGroup tg = new ThreadGroup("DispatcherThreads");
        dispatcher = new Dispatcher(tg, false, priority, true);
        dispatcher.setDispatcherSleepTime(dispatcherSleepTime);
        dispatcher.setMaxAliveThreads(maxAliveThreads);
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

    /** Destroys the servlet.
     */
    @Override
    public void destroy() {
        dispatcher.setDispatcherActive(false);
        super.destroy();
    }

    public static Dispatcher getDispatcher(){
        return dispatcher;
    }

}
