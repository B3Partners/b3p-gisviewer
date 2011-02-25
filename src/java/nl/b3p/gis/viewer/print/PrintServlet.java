package nl.b3p.gis.viewer.print;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

/**
 *
 * @author Boy de Wit
 */
public class PrintServlet extends HttpServlet {

    private static final Log logFile = LogFactory.getLog(PrintServlet.class);

    private static String xsl_A4_Liggend = null;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /* Create object */
        Date now = new Date();
        SimpleDateFormat df = new SimpleDateFormat("dd-M-yyyy", new Locale("NL"));

        PrintInfo info = new PrintInfo();

        info.setTitel("Test titel");
        info.setDatum(df.format(now));

        /* kwaliteit is eigenlijk de width in een nieuwe getMap request */
        info.setKwaliteit(983);

        /* indien kwaliteit aangepast is nieuwe image klaarzetten en
         * imageId meegeven aan xsl */        
        info.setImageUrl("http://192.168.1.14:8084/kaartenbalie/services/fc050d16f589d1f82ffd43beba38f933?&SERVICE=WMS&REQUEST=GetMap&TIMEOUT=30&RATIO=1&STYLES=&TRANSPARENT=TRUE&SRS=EPSG:28992&VERSION=1.1.1&EXCEPTIONS=application/vnd.ogc.se_inimage&LAYERS=demo_gemeenten_2006&FORMAT=image/png&HEIGHT=700&WIDTH=983&BBOX=159196.374364191,376556.873274233,203279.231507048,407948.53146345");
        info.setBbox("159196.374364191,376556.873274233,203279.231507048,407948.53146345");
        info.setMapWidth(983);
        info.setMapHeight(700);

        File xslFile = new File(xsl_A4_Liggend);
        String path = new File(xslFile.getParent()).toURI().toString();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // XSLT
            Source xsltSrc = new StreamSource(xslFile);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);

            JAXBContext jc = JAXBContext.newInstance(PrintInfo.class);
            JAXBSource src = new JAXBSource(jc, info);

            FopFactory fopFactory = FopFactory.newInstance();
            fopFactory.setBaseURL(path);

            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            response.setContentType("application/pdf");
            response.setContentLength(out.size());

            response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();

        } catch (Exception ex) {
            logFile.error("Fout tijdens maken pdf: ", ex);
        } finally {
            out.close();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            if (config.getInitParameter("xsl_A4_Liggend") != null) {
                xsl_A4_Liggend = getServletContext().getRealPath(config.getInitParameter("xsl_A4_Liggend"));
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void printSourceXml(Source src) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = factory.newTransformer();
        StringWriter w = new StringWriter();
        StreamResult res = new StreamResult(w);
        transformer.transform(src, res);
        System.out.println(w.toString());
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