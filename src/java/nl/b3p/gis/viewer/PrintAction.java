/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 * 
 * This file is part of B3P Gisviewer.
 * 
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer;

import com.lowagie.text.DocWriter;
import com.lowagie.text.Document;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.combineimages.CombineImagesServlet;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.print.PrintInfo;
import nl.b3p.gis.viewer.print.PrintServlet;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.struts.BaseHibernateAction;
import nl.b3p.imagetool.CombineImageSettings;
import nl.b3p.imagetool.CombineImagesHandler;
import nl.b3p.ogc.utils.OGCRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

public class PrintAction extends BaseHibernateAction {

    private static final Log logFile = LogFactory.getLog(PrintAction.class);

    protected static final String PRINT = "print";
    protected static final String IMAGE = "image";
    private static final String METADATA_TITLE = "Kaartexport B3P Gisviewer";
    private static final String METADATA_AUTHOR = "B3P Gisviewer";
    private static final String OUTPUT_PDF_PRINT = "PDF_PRINT";
    private static final String OUTPUT_PDF = "PDF";
    private static final String OUTPUT_RTF = "RTF";
    private static final int MAXSIZE = 2048;
    private static String logoPath = null;
    private static String extraImagePath = null;
    private static String disclaimer = null;
    private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MMMM-yyyy", new Locale("NL"));
    private static float footerHeight = 25;
    private static boolean addFooter = true;
    private static int maxResponseTime = 30000;

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(PRINT);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(PRINT, hibProp);

        hibProp = new ExtendedMethodProperties(IMAGE);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setAlternateForwardName(FAILURE);
        map.put(IMAGE, hibProp);

        return map;

    }

    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        CombineImageSettings settings = getCombineImageSettings(request);
        String imageId = CombineImagesServlet.uniqueName("");
        request.getSession().setAttribute(imageId, settings);
        dynaForm.set("imageId", imageId);

        return mapping.findForward(SUCCESS);
    }

    public ActionForward image(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String username = null;
        String password = null;
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        if (gp != null) {
            username = gp.getName();
            password = gp.getPassword();
        }

        CombineImageSettings settings = null;
        String imageId = request.getParameter("imageId");
        if (imageId != null && request.getSession().getAttribute(imageId) != null) {
            settings = (CombineImageSettings) request.getSession().getAttribute(imageId);
            response.setContentType(settings.getMimeType());
            response.setDateHeader("Expires", System.currentTimeMillis() + (1000 * 60 * 60 * 24));

            String keepAlive = request.getParameter("keepAlive");
            if (keepAlive == null || keepAlive.length() == 0) {
                request.getSession().removeAttribute(imageId);
            }
        }
        if (settings == null) {
            logFile.error("No settings for image found");
            this.addAlternateMessage(mapping, request, null, "No settings for image found");
            return this.getAlternateForward(mapping, request);
        }

        OutputStream os = null;
        try {
            //response.setContentType("text/html;charset=UTF-8");
            os = response.getOutputStream();
            CombineImagesHandler.combineImage(response.getOutputStream(), settings,
                    settings.getMimeType(), maxResponseTime, username, password);
        } finally {
            os.close();
        }
        return null;
    }

    public ActionForward print(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* ophalen form waardes */
        String title = FormUtils.nullIfEmpty(dynaForm.getString("title"));
        String imageId = FormUtils.nullIfEmpty(dynaForm.getString("imageId"));
        String imageSize = FormUtils.nullIfEmpty(dynaForm.getString("imageSize"));
        String pageSize = FormUtils.nullIfEmpty(dynaForm.getString("pageSize"));
        boolean landscape = Boolean.valueOf(dynaForm.getString("landscape")).booleanValue();
        String outputType = FormUtils.nullIfEmpty(dynaForm.getString("outputType"));

        /* kwaliteit is nieuwe width voor getMap verzoek */
        int kwaliteit = new Integer(imageSize).intValue();

        /* huidige CombineImageSettings ophalen */
        CombineImageSettings settings = (CombineImageSettings) request.getSession().getAttribute(imageId);

        /* bbox klaarzetten voor xsl */
        String minx = Double.toString(settings.getBbox().getMinx());
        String miny = Double.toString(settings.getBbox().getMiny());
        String maxx = Double.toString(settings.getBbox().getMaxx());
        String maxy = Double.toString(settings.getBbox().getMaxy());
        String bbox = minx + "," + miny + "," + maxx + "," + maxy;

        /* ratio bepalen voor berekenen nieuwe height */
        float ratio = calcPixelRatio(settings.getWidth(), settings.getHeight());        
        int newHeight = calcNewHeight(ratio, kwaliteit, settings.getHeight());

        /* nieuwe width en height opgeven voor nieuw verzoek */
        settings.setWidth(kwaliteit);
        settings.setHeight(newHeight);

        /* tijdelijk plaatje maken, pad wordt gebruikt in xsl */
        String imageUrl = createTempImage(settings);

        /* nu */
        Date now = new Date();
        SimpleDateFormat df = new SimpleDateFormat("dd-M-yyyy", new Locale("NL"));

        /* nieuw (xml) Object voor gebruik met fop */
        PrintInfo info = new PrintInfo();
        info.setTitel(title);
        info.setDatum(df.format(now));
        info.setImageUrl(imageUrl);
        info.setBbox(bbox);
        info.setMapWidth(kwaliteit);
        info.setMapHeight(newHeight);

        /* doorgeven mimetype en template */
        String mimeType = null;

        if (outputType != null && outputType.equals(OUTPUT_PDF) || outputType.equals(OUTPUT_PDF_PRINT)) {
            mimeType = MimeConstants.MIME_PDF;
        } else if (outputType != null && outputType.equals(OUTPUT_RTF)) {
            mimeType = MimeConstants.MIME_RTF;
        } else {
            mimeType = MimeConstants.MIME_PDF;
        }

        String template = null;

        if (landscape && pageSize.equals("A4")) {
            template = PrintServlet.xsl_A4_Liggend;
        } else if (landscape && pageSize.equals("A3")) {
            template = PrintServlet.xsl_A4_Liggend;
        } else if (!landscape && pageSize.equals("A4")) {
            template = PrintServlet.xsl_A4_Liggend;
        } else if (!landscape && pageSize.equals("A3")) {
            template = PrintServlet.xsl_A4_Liggend;
        } else {
            template = PrintServlet.xsl_A4_Liggend;
        }

        /* add javascript print dialog to pdf ? */
        boolean addJavascript = false;
        if (outputType != null && outputType.equals(OUTPUT_PDF_PRINT)) {
            addJavascript = true;
        }

        /* Maak de output */
        PrintServlet.createOutput(info, mimeType, template, addJavascript, response);

        return null;
    }

    private String createTempImage(CombineImageSettings settings) throws IOException {

        File temp = File.createTempFile("kaart_", ".png");
        temp.deleteOnExit();

        FileOutputStream out = new FileOutputStream(temp);

        try {
            CombineImagesHandler.combineImage(out,settings,settings.getMimeType(),30000);

        } catch (Exception e) {
            logFile.error("Fout bij maken tijdelijk plaatje: " + e);
        } finally {
            out.close();
        }

        return temp.toURI().toString();
    }

    /* berekend nieuwe hoogte voor plaatje zodat deze overeenkomt
     * met ingestelde kwaliteit (width) */
    private int calcNewHeight(float ratio, int kwaliteit, int height) {
        int hoogte = height;

        if (kwaliteit >= height) {
            hoogte = new Float(kwaliteit * ratio).intValue();
        } else {
            hoogte = new Float(kwaliteit / ratio).intValue();
        }

        return hoogte;
    }

    private float calcPixelRatio(int width, int height) {
        float factor = 0;

        if (width >= height) {
            factor = new Float(height).floatValue() / width;
        } else {
            factor = new Float(width).floatValue() / height;
        }

        return factor;
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
                logFile.error("Fout bij ophalen export logo: ", e);
            }
        }
        if (extraImagePath != null) {
            try {
                extraImage = Image.getInstance(extraImagePath);
            } catch (Exception e) {
                logFile.error("Fout bij ophalen van de noord pijl: ", e);
            }
        }
        if (outputType.equalsIgnoreCase(OUTPUT_PDF) ||
                outputType.equalsIgnoreCase(OUTPUT_PDF_PRINT)) {
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

    private Image getImage(HttpServletRequest request, float imageWidth,
            float imageHeight, int resize) throws Exception {

        String username = null;
        String password = null;
        GisPrincipal gp = GisPrincipal.getGisPrincipal(request);
        if (gp != null) {
            username = gp.getName();
            password = gp.getPassword();
        }

        CombineImageSettings settings = null;
        String imageId = request.getParameter("imageId");
        if (imageId != null && request.getSession().getAttribute(imageId) != null) {
            settings = (CombineImageSettings) request.getSession().getAttribute(imageId);

            String keepAlive = request.getParameter("keepAlive");
            if (keepAlive == null || keepAlive.length() == 0) {
                request.getSession().removeAttribute(imageId);
            }
        }
        if (settings == null) {
            return null;
        }

        //Vergroot het plaatje naar de imagesize die mee gegeven is.
        //als die waarde leeg is doe dan de MAXSIZE waarde.
        int imageSize = MAXSIZE;
        try {
            imageSize = Integer.parseInt(FormUtils.nullIfEmpty(request.getParameter("imageSize")));
        } catch (NumberFormatException nfe) {
            imageSize = MAXSIZE;
        }

        int width = settings.getWidth();
        int height = settings.getHeight();
        float factor = 0;
        if (width >= height) {
            factor = new Float(imageSize).floatValue() / width;
        } else {
            factor = new Float(imageSize).floatValue() / height;
        }
        settings.setWidth(new Double(Math.floor(width * factor)).intValue());
        settings.setHeight(new Double(Math.floor(height * factor)).intValue());

        //zorg er voor dat het plaatje binnen de marging van het document komt.
        if (resize > 0) {
            imageHeight -= resize;
            imageWidth -= Math.round(resize * factor);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CombineImagesHandler.combineImage(baos, settings,
                settings.getMimeType(), maxResponseTime, username, password);
        Image map = Image.getInstance(baos.toByteArray());
        map.setAlignment(Image.ALIGN_CENTER);
        map.scaleToFit(imageWidth, imageHeight);

        return map;
    }

    /**
     *Voeg de metadata toe aan het document.
     */
    private void setDocumentMetadata(Document doc) {
        doc.addTitle(METADATA_TITLE);
        doc.addAuthor(METADATA_AUTHOR);
    }

    private CombineImageSettings getCombineImageSettings(HttpServletRequest request) throws Exception {
        String url = FormUtils.nullIfEmpty(request.getParameter("urls"));
        String wkt = FormUtils.nullIfEmpty(request.getParameter("wkts"));
        CombineImageSettings settings = new CombineImageSettings();

        String[] urls = null;
        if (url != null) {
            logFile.debug("Urls: " + url);
            urls = url.split(";");
            settings.setUrls(urls);
        }
        String[] wkts = null;
        if (wkt != null) {
            logFile.debug("WKT: " + wkt);
            wkts = wkt.split(";");
            settings.setWktGeoms(wkts);
        }
        if (urls == null || urls.length == 0) {
            throw new Exception("Er zijn geen paden naar plaatjes gevonden");
        }

        String reqWidth = request.getParameter(OGCRequest.WMS_PARAM_WIDTH);
        String reqHeight = request.getParameter(OGCRequest.WMS_PARAM_HEIGHT);
        String reqBbox = request.getParameter(OGCRequest.WMS_PARAM_BBOX);
        if (reqWidth != null && reqHeight != null && reqBbox != null) {
            // gebruik info uit request
            settings.setWidth(new Integer(reqWidth));
            settings.setHeight(new Integer(reqHeight));
            settings.setBbox(reqBbox);
        } else {
            // bereken width, height en bbox uit eerste url,
            // want dit zal voor alle urls hetzelfde moeten zijn
            String url1 = urls[0];
            OGCRequest ogcr = new OGCRequest(url1);
            int width = Integer.parseInt(ogcr.getParameter(OGCRequest.WMS_PARAM_WIDTH));
            int height = Integer.parseInt(ogcr.getParameter(OGCRequest.WMS_PARAM_HEIGHT));
            String bbox = ogcr.getParameter(OGCRequest.WMS_PARAM_BBOX);
            settings.setWidth(width);
            settings.setHeight(height);
            settings.setBbox(bbox);
        }

        String mimeType = FormUtils.nullIfEmpty(request.getParameter(OGCRequest.WMS_PARAM_FORMAT));
        if (mimeType != null) {
            settings.setMimeType(mimeType);
        }
        return settings;
    }

}
