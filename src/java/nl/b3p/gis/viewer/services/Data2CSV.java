/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.gis.viewer.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.csv.CsvOutputStream;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.db.Themas;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;

/**
 *
 * @author Roy
 */
public class Data2CSV extends HttpServlet {
   private static final Log log = LogFactory.getLog(Data2CSV.class);

   private static String HTMLTITLE="Data naar CSV";
    /**
    * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * @param request servlet request
    * @param response servlet response
    */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String themaId= request.getParameter("themaId");
        String objectIds= request.getParameter("objectIds");
        String seperator= request.getParameter("seperator");
        if (seperator==null || seperator.length()!=1){
            seperator=",";
        }
        char sep= seperator.charAt(0);
        OutputStream out = response.getOutputStream();
        CsvOutputStream cos= new CsvOutputStream(new OutputStreamWriter(out),sep,false);
        Transaction tx = HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
        try {
            Themas thema=SpatialUtil.getThema(themaId);
            response.setContentType("text/csv");
            response.setHeader(FileUploadBase.CONTENT_DISPOSITION, "attachment; filename=\""+thema.getNaam()+".csv\";");

            String[] pks=null;
            if (objectIds!=null){
                pks=objectIds.split(",");
            }
            
            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            if (user==null){
                writeErrorMessage(response,out,"Kan de data niet ophalen omdat u niet bent ingelogd.");
                return;
            }if (!themaAllowed(thema,user)){
                writeErrorMessage(response,out,"U heeft geen rechten op dit thema.");
                return;
            }
            Connection conn=null;
            if (WfsUtil.validWfsConnection(thema)){
                log.error("Connection type "+Connecties.TYPE_WFS+" not supported");
                throw new ServletException("Connection type "+Connecties.TYPE_WFS+" not supported");
            }else if (SpatialUtil.validJDBCConnection(thema)){
                try {
                    conn = thema.getConnectie().getJdbcConnection();
                } catch (SQLException ex) {
                    writeErrorMessage(response, out,"Kan geen verbinding maken met datasource. Reden: "+ex.getMessage());
                    return;
                }
            }
            List data=null;
            try {
                data=getData(conn,thema,pks);
            } catch (SQLException ex) {
                writeErrorMessage(response,out, ex.getMessage());
                log.error(ex);
                return;
            }
            String[] columns=getThemaColumnNames(thema);
            cos.writeRecord(columns);
            for(int i=0; i < data.size(); i++) {
                String[] row = (String[])data.get(i);
                cos.writeRecord(row);
            }
        } finally {
            if (cos!=null)
                cos.close();
            HibernateUtil.getSessionFactory().getCurrentSession().close();
            if (out!=null)
                out.close();

        }
    }
    //TODO: Kijken of uitgebreide data ook moet worden geexporteerd.
    /**
     * Haal de kolomnamen op uit de themadata. Elke kolomnaam wordt maar 1 keer toegevoegd.
     */
    public String[] getThemaColumnNames(Themas thema){
        Set themadata=thema.getThemaData();
        Iterator it=themadata.iterator();
        ArrayList columns=new ArrayList();
        while(it.hasNext()){
            ThemaData td= (ThemaData)it.next();
            if(td.getKolomnaam()!=null){
                if (!columns.contains(td.getKolomnaam()))
                    columns.add(td.getKolomnaam());
            }
        }
        String[] s=new String[columns.size()];
        for (int i=0; i < columns.size(); i++){
            s[i]=(String)columns.get(i);
        }
        return s;
    }
    /**
     * Haalt de data op. Van een thema (t) waarvan de pk is meegegeven
     */
    public List getData(Connection conn,Themas t, String[] pks) throws SQLException {
        String[] columns=getThemaColumnNames(t);
        String q= createSelectQuery(t, pks, columns);
        PreparedStatement statement =conn.prepareStatement(q);
        ResultSet rs= statement.executeQuery();
        ArrayList result=new ArrayList();
        while(rs.next()){
            String[] row = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                String s = rs.getString(columns[i]);
                if (s == null) {
                    row[i] = "";
                } else {
                    row[i] = s.trim();
                }
            }
            result.add(row);
        }
        return result;
    }
    /**
     * Maak de query
     */
    public String createSelectQuery(Themas t, String[] pks, String[] columns) {
        StringBuffer sb= new StringBuffer();
        sb.append("SELECT ");
        for (int i=0; i < columns.length; i++){
            if (i!=0){
                sb.append(", ");
            }
            sb.append("\""+columns[i]+"\"");
        }
        /*TODO: Spatial component toevoegen?? Hou dan ook rekening met dat een spatial object uit een andere
        tabel kan komen*/
        sb.append(" FROM ");
        sb.append("\""+t.getAdmin_tabel()+"\"");
        if (pks!=null){
            sb.append(" WHERE ");
            for (int i=0; i < pks.length; i++){
                if (i!=0)
                    sb.append("OR ");
                sb.append("\""+t.getAdmin_pk()+"\"");
                sb.append(" = ");
                sb.append("'"+pks[i]+"' ");
            }
        }
        log.debug("Do query for csv output: "+sb.toString());
        return sb.toString();
    }


    /**
     * Writes a error message to the response
     */
    private void writeErrorMessage(HttpServletResponse response, OutputStream out, String message){
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = new PrintWriter(out);
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>"+HTMLTITLE+"</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<h1>Fout</h1>");
        pw.println("<h3>"+message+"</h3>");
        pw.println("</body>");
        pw.println("</html>");

    }
    /**
     * Controleerd of een gebruiker rechten heeft op dit thema.
     */
    private boolean themaAllowed(Themas t,GisPrincipal user){
        List layersFromRoles = user.getLayerNames(false);
        if (layersFromRoles==null)
            return false;

        if (t.getWms_layers_real()==null)
            return false;

        String[] themaLayers=t.getWms_layers_real().split(",");
        for (int i=0; i < themaLayers.length; i++){
            if (!layersFromRoles.contains(themaLayers[i])){
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
    * Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
