package nl.b3p.gis.viewer.print;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import nl.b3p.commons.services.FormUtils;
import nl.b3p.imagetool.CombineImageSettings;
import nl.b3p.imagetool.CombineImagesHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

/**
 *
 * @author Boy de Wit
 */
public class PrintServlet extends HttpServlet {

    private static final Log logFile = LogFactory.getLog(PrintServlet.class);

    public static String xsl_A4_Liggend = null;
    public static String xsl_A4_Staand = null;
    public static String xsl_A3_Liggend = null;
    public static String xsl_A3_Staand = null;
    
    public static String fopConfig = null;
    public static String fontPath = null;

    public static CombineImageSettings settings = null;
    private static final int MAX_IMAGE_SIZE_PX = 2048;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {

        String sWidth = FormUtils.nullIfEmpty(request.getParameter("width"));
        String sHeight = FormUtils.nullIfEmpty(request.getParameter("height"));
        String sBbox = FormUtils.nullIfEmpty(request.getParameter("bbox"));

        if (sWidth == null || sHeight == null || sBbox == null) {
            throw new ServletException("Not all print parameters are given!");
        }

        Integer height = null;
        Integer width = null;

        try {
            width = new Integer(sWidth);
            height = new Integer(sHeight);
        } catch (Exception e) {
            throw new ServletException("One of the parameters given (widht,height and/or id) is not a Integer");
        }

        /* combine settings ophalen */
        CombineImageSettings imageSettings = getSettings();

        if (imageSettings == null) {
            throw new ServletException("No print settings found!");
        }

        /* nieuwe settings zetten */
        imageSettings.setWidth(width);
        imageSettings.setHeight(height);
        imageSettings.setBbox(sBbox);

        /* Nieuw plaatje klaarzetten */
        OutputStream out = response.getOutputStream();

        CombineImagesHandler.combineImage(out, imageSettings, imageSettings.getMimeType(), 0);

        response.setContentType(imageSettings.getMimeType());

        response.getOutputStream().flush();

    }

    public static CombineImageSettings getSettings() {
        return settings;
    }

    public static void setSettings(CombineImageSettings settings) {
        PrintServlet.settings = settings;
    }

    public static void createOutput(PrintInfo info, String mimeType, String template,
            boolean addJavascript, HttpServletResponse response) 
            throws MalformedURLException, IOException, SAXException {

        File xslFile = new File(template);
        String path = new File(xslFile.getParent()).toURI().toString();

        /* Setup fopfactory */
        FopFactory fopFactory = FopFactory.newInstance();

        /* Set BaseUrl so that fop knows paths to images etc... */
        fopFactory.setBaseURL(path);
        fopFactory.getFontManager().setFontBaseURL(fontPath);
        fopFactory.setUserConfig(new File(fopConfig));

        /* Setup output stream */
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            /* Construct fop */
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            foUserAgent.setCreator("Gisviewer webapplicatie");
            foUserAgent.setProducer("B3Partners");

            Date now = new Date();
            foUserAgent.setCreationDate(now);
            foUserAgent.setTitle("Kaart");

            Fop fop = fopFactory.newFop(mimeType, foUserAgent, out);

            /* Setup Jaxb */
            JAXBContext jc = JAXBContext.newInstance(PrintInfo.class);
            JAXBSource src = new JAXBSource(jc, info);

            /* Setup xslt */
            Source xsltSrc = new StreamSource(xslFile);
            
            String proxyHost = System.getProperty("http.proxyHost");      
            if (proxyHost != null && !proxyHost.equals("")) {
                logFile.debug("Printing behind proxy: " + proxyHost + " using path: " + path);
                xsltSrc.setSystemId(path);
            }            

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);

            Result res = new SAXResult(fop.getDefaultHandler());

            if (transformer != null) {
                transformer.transform(src, res);
            }

            /* Setup response */
            response.setContentType(mimeType);
            response.setContentLength(out.size());
			
            /* Set filename and extension */
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", new Locale("NL"));    
            String date = df.format(now);
            String fileName = "Kaart_" + date;

            if (mimeType.equals(MimeConstants.MIME_PDF)) {
                fileName += ".pdf";
            } else if (mimeType.equals(MimeConstants.MIME_RTF)) {
                fileName += ".rtf";
            }
            
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            if (config.getInitParameter("xsl_A4_Staand") != null) {
                xsl_A4_Staand = getServletContext().getRealPath(config.getInitParameter("xsl_A4_Staand"));
            }
            if (config.getInitParameter("xsl_A4_Liggend") != null) {
                xsl_A4_Liggend = getServletContext().getRealPath(config.getInitParameter("xsl_A4_Liggend"));
            }
            if (config.getInitParameter("xsl_A3_Staand") != null) {
                xsl_A3_Staand = getServletContext().getRealPath(config.getInitParameter("xsl_A3_Staand"));
            }
            if (config.getInitParameter("xsl_A3_Liggend") != null) {
                xsl_A3_Liggend = getServletContext().getRealPath(config.getInitParameter("xsl_A3_Liggend"));
            }
            
            fopConfig = getServletContext().getRealPath("/WEB-INF/xsl/fop.xml");
            fontPath = getServletContext().getRealPath("/WEB-INF/xsl/fonts");
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

        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(PrintServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(PrintServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}