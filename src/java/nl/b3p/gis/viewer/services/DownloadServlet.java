package nl.b3p.gis.viewer.services;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import javax.servlet.*;
import javax.servlet.http.*;
import nl.b3p.gis.viewer.downloads.RemoveFilesWithDelayThread;
import nl.b3p.gis.viewer.downloads.RemoveOldDownloads;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Boy
*/
public class DownloadServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(DownloadServlet.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static HashMap<String,RemoveFilesWithDelayThread> removeThreads = new HashMap<String,RemoveFilesWithDelayThread>();
    private static Random rg = null;

    private static String downloadPath = null;
    private static String smtpHost = null;
    private static String contactEmail = null;  

    private static String downloadServletPath = "/services/DownloadServlet";
    
    private static String mailDownloadSucces = null;
    private static String mailDownloadError = null;
    
    private static final int runTaskEverySeconds = 86400;
    private static final int folderAliveSeconds = 86400;
    
    private Timer timer = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            if (config.getInitParameter("downloadPath") != null) {
                downloadPath = config.getInitParameter("downloadPath");
            }
            if (config.getInitParameter("smtpHost") != null) {
                smtpHost = config.getInitParameter("smtpHost");
            }
            if (config.getInitParameter("contactEmail") != null) {
                contactEmail = config.getInitParameter("contactEmail");
            }
            
            mailDownloadSucces = getServletContext().getRealPath("/WEB-INF/txt/mail-download-succes.txt");
            mailDownloadError = getServletContext().getRealPath("/WEB-INF/txt/mail-download-error.txt");
            
            /* Task voor opruimen oude downloads */
            long delay = runTaskEverySeconds * 1000;
            
            timer = new Timer();
            timer.schedule(new RemoveOldDownloads(downloadPath, folderAliveSeconds), 0, delay);
            
        } catch (Exception e) {
            log.error("",e);
            throw new ServletException(e);
        }
    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        log.debug("DownloadServlet processRequest.");

        String downloadPath = getDownloadPath() + File.separator;
        String downloadCode = downloadPath + request.getParameter("download");

        if (downloadCode != null) {
            if (removeThreads.get(downloadCode)!=null){
                removeThreads.get(downloadCode).stopThread();
                removeThreads.remove(downloadCode);
            }  

            try {
                File zipfile = new File(downloadCode);

                if (zipfile.exists()) {
                    byte[] buffer = new byte[8192];
                    FileInputStream fis = new FileInputStream(downloadCode);

                    response.setContentType("application/zip");
                    response.setHeader(FileUploadBase.CONTENT_DISPOSITION, "attachment; filename=\"" + zipfile.getName() + "\";");
                    response.setContentLength((int) zipfile.length());
                    OutputStream os = response.getOutputStream();
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                    fis.close();
                    os.flush();

                    //after download remove the dir and file.
                    RemoveFilesWithDelayThread removeThread = new RemoveFilesWithDelayThread(downloadCode,zipfile.getParentFile(), 60000, removeThreads);
                    removeThreads.put(downloadCode, removeThread);
                    removeThread.start();

                } else {
                    writeErrorMessage(response, "De door u opgevraagde download is verlopen, mogelijk komt dit doordat u de download reeds heeft opgehaald.");
                }
            } catch (IOException ioe) {
                log.error("", ioe);
                
            } catch (Exception e) {                
                log.error("Error committing transaction, do rollback: ", e);
            }
        }
    }
    
    @Override
    public void destroy() {
        log.debug("DESTROY DownloadServlet.");
        
        /* Opruimen threads */
        if (timer != null) {            
            timer.cancel();
        }
        
        if (removeThreads != null && removeThreads.size() > 0) {            
            Collection threads = removeThreads.values();
            Iterator iter = threads.iterator();
            while(iter.hasNext()) {
                RemoveFilesWithDelayThread thread = (RemoveFilesWithDelayThread)iter.next();
                
                if (thread.isAlive()) {
                    thread.stopThread();   
                }
            }            
        }
        
        super.destroy();
    }

    private void writeErrorMessage(HttpServletResponse response, String message) {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Download Kaart</title>");
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

    public static String uniqueName(String extension) {
        // Gebruik tijd in milliseconden om gerekend naar een radix van 36.
        // Hierdoor ontstaat een lekker korte code.
        long now = (new Date()).getTime();
        String val1 = Long.toString(now, Character.MAX_RADIX).toUpperCase();
        // random nummer er aanplakken om zeker te zijn van unieke code
        if (rg==null) {
            rg = new Random();
        }
        long rnum = (long) rg.nextInt(1000);
        String val2 = Long.toString(rnum, Character.MAX_RADIX).toUpperCase();
        String thePath = "";
        
        return val1 + val2 + extension;
    }

    public static String getDownloadPath() {
        return downloadPath;
    }

    public static String getContactEmail() {
        return contactEmail;
    }

    public static String getSmtpHost() {
        return smtpHost;
    }

    public static String getDownloadServletPath() {
        return downloadServletPath;
    }

    public static String getMailDownloadSucces() {
        return mailDownloadSucces;
    }

    public static String getMailDownloadError() {
        return mailDownloadError;
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

    /** Returns a short description of the servlet.
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}
