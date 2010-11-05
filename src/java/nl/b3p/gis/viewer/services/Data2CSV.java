package nl.b3p.gis.viewer.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.csv.CsvOutputStream;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.Filter;

/**
 *
 * @author Roy
 */
public class Data2CSV extends HttpServlet {

    private static final Log log = LogFactory.getLog(Data2CSV.class);
    private static String HTMLTITLE = "Data naar CSV";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String gegevensbronId = request.getParameter("themaId");
        String objectIds = request.getParameter("objectIds");
        String seperator = request.getParameter("seperator");

        if (seperator == null || seperator.length() != 1) {
            seperator = ",";
        }
        char sep = seperator.charAt(0);
        OutputStream out = response.getOutputStream();
        CsvOutputStream cos = new CsvOutputStream(new OutputStreamWriter(out), sep, false);
        Transaction tx = HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
        try {
            Gegevensbron gb = SpatialUtil.getGegevensbron(gegevensbronId);
            
            String[] ids = null;
            if (objectIds != null) {
                ids = objectIds.split(",");
            }

            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            if (user == null) {
                writeErrorMessage(response, out, "Kan de data niet ophalen omdat u niet bent ingelogd.");
                return;
            }
            /*
            if (!themaAllowed(thema, user)) {
                writeErrorMessage(response, out, "U heeft geen rechten op dit thema.");
                return;
            }

            */

            Bron b = gb.getBron(request);

            if (b == null){
                throw new ServletException("Gegevensbron (id " + gb.getId() + ") Bron null.");
            }

            List data = null;
            String[] propertyNames = getThemaPropertyNames(gb);
            try {
                data = getData(b, gb, ids, propertyNames);
            } catch (Exception ex) {
                writeErrorMessage(response, out, ex.getMessage());
                log.error("Fout bij laden csv data.",ex);
                return;
            }

            response.setContentType("text/csv");
            response.setHeader(FileUploadBase.CONTENT_DISPOSITION, "attachment; filename=\"" + gb.getNaam() + ".csv\";");
            
            cos.writeRecord(propertyNames);
            for (int i = 0; i < data.size(); i++) {
                String[] row = (String[]) data.get(i);
                cos.writeRecord(row);
            }
        } finally {
            if (cos != null) {
                cos.close();
            }
            HibernateUtil.getSessionFactory().getCurrentSession().close();
            if (out != null) {
                out.close();
            }

        }
    }
    //TODO: Kijken of uitgebreide data ook moet worden geexporteerd.
    
    public String[] getThemaPropertyNames(Gegevensbron gb) {
        Set themadata = gb.getThemaData();
        
        Iterator it = themadata.iterator();
        ArrayList columns = new ArrayList();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            if (td.getKolomnaam() != null) {
                if (!columns.contains(td.getKolomnaam())) {
                    columns.add(td.getKolomnaam());
                }
            }
        }
        String[] s = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            s[i] = (String) columns.get(i);
        }
        return s;
    }

    public List getData(Bron b, Gegevensbron gb, String[] pks, String[] propertyNames)throws IOException, Exception {

        Filter filter = FilterBuilder.createOrEqualsFilter(
                DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart(), pks);
        List<ThemaData> items = SpatialUtil.getThemaData(gb, false);
        List<String> propnames = DataStoreUtil.themaData2PropertyNames(items);
        ArrayList<Feature> features=DataStoreUtil.getFeatures(b, gb, null, filter, propnames, null, false);
        ArrayList result = new ArrayList();
        for (int i=0; i < features.size(); i++) {
            Feature f = features.get(i);
            String[] row = new String[propertyNames.length];

            for (int p=0; p< propertyNames.length; p++) {
                Property property = f.getProperty(propertyNames[p]);
                if (property!=null && property.getValue()!=null && property.getValue().toString()!=null){
                    row[p]= property.getValue().toString().trim();
                }else{
                    row[p] = "";
                }
            }
            result.add(row);
        }
        return result;
    }

    /**
     * Writes a error message to the response
     */
    private void writeErrorMessage(HttpServletResponse response, OutputStream out, String message) {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = new PrintWriter(out);
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>" + HTMLTITLE + "</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<h1>Fout</h1>");
        pw.println("<h3>" + message + "</h3>");
        pw.println("</body>");
        pw.println("</html>");

    }

    /**
     * Controleerd of een gebruiker rechten heeft op dit thema.
     */
    private boolean themaAllowed(Themas t, GisPrincipal user) {
        List layersFromRoles = user.getLayerNames(false);
        if (layersFromRoles == null) {
            return false;
        }

        if (t.getWms_layers_real() == null) {
            return false;
        }

        String[] themaLayers = t.getWms_layers_real().split(",");
        for (int i = 0; i < themaLayers.length; i++) {
            if (!layersFromRoles.contains(themaLayers[i])) {
                return false;
            }
        }
        return true;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
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
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
