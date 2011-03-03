package nl.b3p.gis.viewer.print;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;

/**
 *
 * @author Boy de Wit
 */
public class PrintServlet extends HttpServlet {

    private static final Log logFile = LogFactory.getLog(PrintServlet.class);

    public static String xsl_A4_Liggend = null;

    public static void createOutput(PrintInfo info, String mimeType, String template,
            boolean addJavascript, HttpServletResponse response) throws MalformedURLException, IOException {

        File xslFile = new File(template);
        String path = new File(xslFile.getParent()).toURI().toString();

        /* Setup fopfactory */
        FopFactory fopFactory = FopFactory.newInstance();

        /* Set BaseUrl so that fop knows paths to images etc... */
        fopFactory.setBaseURL(path);

        /* Setup output stream */
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            /* Construct fop */
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            Fop fop = fopFactory.newFop(mimeType, foUserAgent, out);

            /* Setup Jaxb */
            JAXBContext jc = JAXBContext.newInstance(PrintInfo.class);
            JAXBSource src = new JAXBSource(jc, info);

            /* Setup xslt */
            Source xsltSrc = new StreamSource(xslFile);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);

            Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            /* Setup response */
            response.setContentType(mimeType);
            response.setContentLength(out.size());

            /* use postprocessing with itext to add Javascript to output */
            if (addJavascript) {
                addJsToPdfOutput(out, response);
            } else {
                response.getOutputStream().write(out.toByteArray());
            }
            
            response.getOutputStream().flush();

        } catch (Exception ex) {
            logFile.error("Fout tijdens print output: ", ex);
        } finally {
            out.close();
        }
    }

    private static void addJsToPdfOutput(ByteArrayOutputStream out, HttpServletResponse response) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(out.toByteArray());
        int n = reader.getNumberOfPages();

        Document document = new Document(reader.getPageSizeWithRotation(1));

        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        PdfContentByte cb = writer.getDirectContent();
        PdfImportedPage page;

        int rotation;
        int i = 0;

        while (i < n) {
            i++;
            document.setPageSize(reader.getPageSizeWithRotation(i));
            document.newPage();
            page = writer.getImportedPage(reader, i);
            rotation = reader.getPageRotation(i);

            if (rotation == 90 || rotation == 270) {
                cb.addTemplate(page, 0, -1f, 1f, 0, 0,
                reader.getPageSizeWithRotation(i).getHeight());
            } else {
                cb.addTemplate(page, 1f, 0, 0, 1f, 0, 0);
            }
        }

        writer.addJavaScript("this.print({bSilent:true,bShrinkToFit:true});");

        document.close();
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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