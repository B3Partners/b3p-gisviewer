package nl.b3p.gis.viewer.services;

import java.io.*;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Boy
 */
public class GetBlobServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(GetBlobServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        String gbId = request.getParameter("gbId");
        String recordId = request.getParameter("recordId");
        String columnName = request.getParameter("columnName");
        String mimeType = request.getParameter("mimeType");

        if (gbId != null && recordId != null && columnName != null && mimeType != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            Transaction tx = null;
            Connection conn = null;

            try {
                tx = sess.beginTransaction();

                Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, new Integer(gbId));
                conn = getConnection(gb.getBron());

                if (gb == null || conn == null) {
                    writeMessage(response, "Fout tijdens verbinden.");
                }

                Integer id = new Integer(recordId);
                String adminTabel = gb.getAdmin_tabel();
                String primaryKey = gb.getAdmin_pk();
                
                Object obj = getBlob(id, columnName, adminTabel, primaryKey, conn);

                if (obj != null && mimeType != null && !mimeType.contains("html")) {
                    byte[] arr = convertObjectToByteArray(obj);                    
                    createBinary(response, arr, mimeType, columnName);
                } else if (obj != null && mimeType != null && mimeType.contains("html")) {                    
                    createHtml(response, obj);
                } else {
                    writeMessage(response, "Geen document gevonden.");
                }

                tx.commit();
            } catch (Exception e) {
                log.error("Fout tijdens ophalen blob: ", e);
                
                writeMessage(response, "Fout tijdens ophalen blob.");
                
                if (tx.isActive()) {
                    tx.rollback();
                }
            } finally {
                DbUtils.close(conn);
            }
        } else {
            writeMessage(response, "Verplichte parameters ontbreken.");
        }
    }

    private byte[] convertObjectToByteArray(Object obj)
            throws IOException {
        
        byte[] arr = null;
        
        if (obj instanceof Blob) {
            try {
                Blob b = (Blob) obj;

                arr = b.getBytes(1, (int) b.length());
            } catch (Exception ex) {
                log.error("Fout bij omzetten naar blob");
            }

            return arr;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput objOut = null;
        
        try {
            objOut = new ObjectOutputStream(bos);
            objOut.writeObject(obj);
            
            arr = bos.toByteArray();
        } finally {
            if (objOut != null) {
                objOut.close();
            }
            
            bos.close();
        }

        return arr;
    }

    private void createBinary(HttpServletResponse response, byte[] arr, String mimeType, 
            String column) throws SQLException, IOException {

        ServletOutputStream out = response.getOutputStream();
        
        response.setContentType(mimeType);
        response.setContentLength(arr.length);
        response.setHeader("Content-disposition", "inline; filename=\""+column+"\"");

        out.write(arr);
        out.flush();
    }

    private void createHtml(HttpServletResponse response, Object file)
            throws SQLException, IOException {

        if (file == null) {
            writeMessage(response, "Fout tijdens maken output html.");
            
            return;
        }
        
        PrintWriter out = response.getWriter();

        response.setContentType("text/html");
        
        if (file instanceof String) {
            out.println(file.toString());
        } else {
            Clob clob = (Clob) file;
            int len = (int)clob.length();
            out.println(clob.getSubString(1, len));
        }
        
        out.flush();
    }

    public Object getBlob(final Integer recordId, String column, String table, String pk,
            Connection conn) {

        if (conn == null || recordId < 1) {
            return null;
        }

        String sql = "select " + column + " from " + table + " where " + pk + " = ?";
        
        try {
            return new QueryRunner().query(conn, sql, new ResultSetHandler<Object>() {
                public Object handle(ResultSet rs) throws SQLException {
                    Object obj = null;

                    while (rs.next()) {
                        obj = rs.getBlob(1);
                    }

                    return obj;
                }
            }, recordId);
        } catch (SQLException sqlEx) {
            log.error("Fout tijdens sql voor ophalen blob: ", sqlEx);
        }

        return null;
    }

    private Connection getConnection(Bron b) {

        if (b != null) {
            try {
                Connection conn = null;
                Properties connectionProps = new Properties();
                connectionProps.put("user", b.getGebruikersnaam());
                connectionProps.put("password", b.getWachtwoord());

                /* TODO: make generic for postgres and oracle. DataSource ?*/
                Class.forName("oracle.jdbc.driver.OracleDriver");
                
                String url = replaceSchema(b.getUrl());
                
                return DriverManager.getConnection(url, connectionProps);
            } catch (Exception ex) {
                log.error("Fout bij maken connectie: ", ex);
            }
        }

        return null;
    }
    
    private String replaceSchema(String url) {
        if (url != null) {
            int idxLast = url.lastIndexOf(":");
            int idxLastDot = url.lastIndexOf(".");
            
            if (idxLastDot > idxLast) {
                url = url.substring(0, idxLastDot);
            }            
        }

        return url;
    }

    private void writeMessage(HttpServletResponse response, String message) {
        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out;
        try {
            out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Ophalen Document</title>");
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            log.error("Servlet error: ", ex);
        }
    }
}
