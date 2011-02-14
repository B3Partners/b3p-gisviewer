package nl.b3p.gis.viewer.services;

import com.lowagie.text.DocWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.html.HtmlWriter;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.rtf.RtfWriter2;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.imagetool.CombineImageSettings;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Roy
 */
public class CreateMapPDF extends HttpServlet {

    private static final int RTIMEOUT = 20000;
    private static final String host = AuthScope.ANY_HOST; // "localhost";
    private static final int port = AuthScope.ANY_PORT;
    private static final Log log = LogFactory.getLog(CreateMapPDF.class);
    private static final String METADATA_TITLE = "Kaartexport B3P Gisviewer";
    private static final String METADATA_AUTHOR = "B3P Gisviewer";
    private static final String OUTPUT_PDF_PRINT = "PDF_PRINT";
    private static final String OUTPUT_PDF = "PDF";
    private static final String OUTPUT_RTF = "RTF";
    private static final int MAXSIZE = 2048;
    private static String logoPath = null;
    private static String extraImagePath = null;
    private static String disclaimer = null;
    private static SimpleDateFormat sdf = null;
    private static float footerHeight = 25;
    private static boolean addFooter = true;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String title = FormUtils.nullIfEmpty(request.getParameter("title"));
        /**
         * Haal alle form properties op.
         */
        String remark = FormUtils.nullIfEmpty(request.getParameter("remark"));

        CombineImageSettings settings = null;
        String imageSource =  null;
        String imageId = FormUtils.nullIfEmpty(request.getParameter("imageId"));
        if (imageId != null && request.getSession().getAttribute(imageId) != null) {
            settings = (CombineImageSettings) request.getSession().getAttribute(imageId);

            String servletUrl = request.getRequestURL().toString();
            String servletName = this.getServletName();
            String servletPath = request.getServletPath();
            servletUrl = servletUrl.substring(0, servletUrl.length() - servletPath.length()) + "/CreateImage";
            imageSource = servletUrl + "?imageId=" + imageId;
        }

//        String mapUrl = FormUtils.nullIfEmpty(request.getParameter("mapUrl"));

        String pageSize = FormUtils.nullIfEmpty(request.getParameter("pageSize"));
        boolean landscape = Boolean.valueOf(request.getParameter("landscape")).booleanValue();
        String outputType = OUTPUT_PDF_PRINT;
        if (FormUtils.nullIfEmpty(request.getParameter("outputType")) != null) {
            outputType = FormUtils.nullIfEmpty(request.getParameter("outputType"));
        }
        int imageSize = 0;
        if (FormUtils.nullIfEmpty(request.getParameter("imageSize")) != null) {
            try {
                imageSize = Integer.parseInt(FormUtils.nullIfEmpty(request.getParameter("imageSize")));
            } catch (NumberFormatException nfe) {
                imageSize = 0;
            }
        }
        //return error als mapUrl null is.
        if (imageId == null || imageId.length() == 0) {
            throw new ServletException("Geen kaart om te plaatsen in de pdf.");
        }
//        OGCRequest ogcr = new OGCRequest(mapUrl);

        Document doc = null;
        try {
            DocWriter dw = null;
            doc = createDocument(pageSize, landscape);
            String filename = title;
            if (filename == null) {
                filename = "kaartexport";
            }
            //Maak writer set response headers en maak de filenaam.
            if (outputType.equalsIgnoreCase(OUTPUT_PDF)
                    || outputType.equalsIgnoreCase(OUTPUT_PDF_PRINT)) {
                dw = PdfWriter.getInstance(doc, response.getOutputStream());
                response.setContentType("application/pdf");
                filename += ".pdf";
            } else if (outputType.equalsIgnoreCase(OUTPUT_RTF)) {
                dw = RtfWriter2.getInstance(doc, response.getOutputStream());
                response.setContentType("application/rtf");
                filename += ".rtf";
            } else {
                dw = HtmlWriter.getInstance(doc, response.getOutputStream());
                response.setContentType("text/html");
                filename += ".html";
            }
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\";");

            doc.open();
            setDocumentMetadata(doc);
            //set title
            if (title != null) {
                Paragraph titleParagraph = new Paragraph(title);
                titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
                doc.add(titleParagraph);
            }
            /*
            Vergroot het plaatje naar de imagesize die mee gegeven is.
            als die waarde leeg is doe dan de MAXSIZE waarde.
             */
            int width = 0;
            int height = 0;
//            width = Integer.parseInt(ogcr.getParameter(ogcr.WMS_PARAM_WIDTH));
//            height = Integer.parseInt(ogcr.getParameter(ogcr.WMS_PARAM_HEIGHT));
            if (imageSize == 0) {
                imageSize = MAXSIZE;
            }
            float factor = 0;
            if (width >= height) {
                factor = new Float(imageSize).floatValue() / width;
            } else {
                factor = new Float(imageSize).floatValue() / height;
            }
            width = new Double(Math.floor(width * factor)).intValue();
            height = new Double(Math.floor(height * factor)).intValue();
//            ogcr.addOrReplaceParameter(ogcr.WMS_PARAM_WIDTH, "" + width);
//            ogcr.addOrReplaceParameter(ogcr.WMS_PARAM_HEIGHT, "" + height);
            /*zorg er voor dat het plaatje binnen de marging van het document komt.
             */
            float imageWidth = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
            float imageHeight = doc.getPageSize().getHeight() - doc.topMargin() - doc.bottomMargin();
            try {
                Image map = getImage(imageSource, request);
                map.setAlignment(Image.ALIGN_CENTER);

                /* indien liggend dan plaatje wat verkleinen zodat opmerking er
                 * ook onder past */
                int resize = 75;

                if (landscape && remark != null) {
                    imageHeight -= resize;
                    imageWidth -= Math.round(resize * factor);
                }
                if (landscape && addFooter) {
                    imageHeight -= resize;
                    imageWidth -= Math.round(resize * factor);
                }
                if (landscape && title != null) {
                    imageHeight -= resize;
                    imageWidth -= Math.round(resize * factor);
                }

                map.scaleToFit(imageWidth, imageHeight);
                doc.add(map);
            } catch (Exception ex) {
                log.error("Kan kaart image niet toevoegen.", ex);
                doc.add(new Phrase("Kan kaart image niet toevoegen."));
            }
            if (addFooter) {
                Element footer = createfooter(doc, outputType);
                //als het een table is dan goed uitlijnen
                if (footer instanceof PdfPTable) {
                    PdfPTable table = (PdfPTable) footer;
                    table.setTotalWidth(new float[]{80, imageWidth - 107, 25});
                    table.setLockedWidth(true);
                    doc.add(table);
                } else {
                    doc.add(footer);
                }
            }

            if (remark != null) {
                doc.add(new Phrase(remark));
            }

            if (dw instanceof PdfWriter
                    && outputType.equalsIgnoreCase(OUTPUT_PDF_PRINT)) {
                PdfWriter dwpdf = (PdfWriter) dw;
                dwpdf.addJavaScript("this.print({bSilent:true,bShrinkToFit:true});");
            }

        } catch (DocumentException de) {
            log.error("Fout bij het maken van een document. Reden: ", de);
            throw new ServletException(de);
        } finally {
            doc.close();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            if (config.getInitParameter("logoPath") != null) {
                logoPath = getServletContext().getRealPath(config.getInitParameter("logoPath"));
            }
            if (config.getInitParameter("extraImagePath") != null) {
                extraImagePath = getServletContext().getRealPath(config.getInitParameter("extraImagePath"));
            }
            if (config.getInitParameter("disclaimer") != null) {
                disclaimer = config.getInitParameter("disclaimer");
            }
            if (config.getInitParameter("addFooter") != null) {
                addFooter = "true".equalsIgnoreCase(config.getInitParameter("addFooter"));
            }
            sdf = new SimpleDateFormat("dd-MMMM-yyyy", new Locale("NL"));

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public Document createDocument(String pageSize, boolean landscape) {
        Rectangle ps = PageSize.A4;
        if (pageSize != null) {
            ps = PageSize.getRectangle(pageSize);
        }
        Document doc = null;
        if (landscape) {
            doc = new Document(ps.rotate());
        } else {
            doc = new Document(ps);
        }
        return doc;
    }

    private Element createfooter(Document doc, String outputType) {
        Image logo = null;
        Image extraImage = null;
        if (logoPath != null) {
            try {
                logo = Image.getInstance(logoPath);
            } catch (Exception e) {
                log.error("Fout bij ophalen export logo: ", e);
            }
        }
        if (extraImagePath != null) {
            try {
                extraImage = Image.getInstance(extraImagePath);
            } catch (Exception e) {
                log.error("Fout bij ophalen van de noord pijl: ", e);
            }
        }
        if (outputType.equalsIgnoreCase(OUTPUT_PDF)) {
            PdfPTable mainTable = new PdfPTable(3);
            if (logo != null) {
                PdfPCell logoCell = new PdfPCell();
                logoCell.setFixedHeight(footerHeight);
                logoCell.setImage(logo);
                mainTable.addCell(logoCell);
            }
            Font font = new Font(Font.TIMES_ROMAN, 8, Font.BOLD);
            PdfPTable nestedTable = new PdfPTable(1);
            PdfPCell dateCell = new PdfPCell(new Phrase(sdf.format(new Date()), font));
            dateCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            nestedTable.addCell(dateCell);
            if (disclaimer != null) {
                PdfPCell disc = new PdfPCell(new Phrase(disclaimer, font));
                disc.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                nestedTable.addCell(disc);
            }
            PdfPCell cell;
            cell = new PdfPCell(nestedTable);
            cell.setBorder(Rectangle.NO_BORDER);
            mainTable.addCell(cell);
            if (extraImage != null) {
                PdfPCell extraCell = new PdfPCell();
                extraCell.setFixedHeight(footerHeight);
                extraCell.setImage(extraImage);
                mainTable.addCell(extraCell);
            }
            return mainTable;
        } else if (logo != null) {
            return logo;
        }
        return null;
    }

    private Image getImage(String mapUrl, HttpServletRequest request) throws IOException, Exception {
        String username = null;
        String password = null;
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        if (gp != null) {
            username = gp.getName();
            password = gp.getPassword();
        }

        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().
                getParams().setConnectionTimeout(RTIMEOUT);

        if (username != null && password != null) {
//            client.getParams().setAuthenticationPreemptive(true);
            Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
            AuthScope authScope = new AuthScope(host, port);
//            client.getState().setCredentials(authScope, defaultcreds);
        }


        // Create a method instance.
        GetMethod method = new GetMethod(mapUrl);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                log.error("Host: " + mapUrl + " error: " + method.getStatusLine().getReasonPhrase());
                throw new Exception("Host: " + mapUrl + " error: " + method.getStatusLine().getReasonPhrase());
            }
            byte[] ba = method.getResponseBody();
             return Image.getInstance(ba);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }


    /**
     *Voeg de metadata toe aan het document.
     */
    private void setDocumentMetadata(Document doc) {
        doc.addTitle(METADATA_TITLE);
        doc.addAuthor(METADATA_AUTHOR);
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
