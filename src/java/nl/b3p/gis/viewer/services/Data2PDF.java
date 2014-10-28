package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.services.ObjectdataPdfInfo.Record;
import nl.b3p.imagetool.CombineImageSettings;
import nl.b3p.imagetool.CombineImagesHandler;
import nl.b3p.ogc.utils.OGCRequest;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.hibernate.Transaction;
import org.json.JSONObject;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.Filter;
import org.xml.sax.SAXException;
import sun.misc.BASE64Encoder;

/**
 *
 * @author Roy
 */
public class Data2PDF extends HttpServlet {

    private static final Log log = LogFactory.getLog(Data2PDF.class);
    private static String HTMLTITLE = "Exporteer naar PDF";
    private static String xsl_data2pdf = null;
    private static Integer MAX_PDF_RECORDS = 25;
    
    public static String fopConfig = null;
    public static String fontPath = null;

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String gegevensbronId = request.getParameter("gbId");
        String objectIds = request.getParameter("objectIds");
        String orientation = request.getParameter("orientation");

        if (gegevensbronId == null || gegevensbronId.equals("")
                || objectIds == null || objectIds.equals("")) {
            writeErrorMessage(response, "Ongeldige request parameters.");
            log.error("Ongeldige request parameters.");

            return;
        }

        if (xsl_data2pdf == null || xsl_data2pdf.equals("")) {
            writeErrorMessage(response, "Xsl template niet geconfigureerd.");
            log.error("Xsl template niet geconfigureerd.");

            return;
        }

        CombineImageSettings settings = null;
        try {
            settings = getCombineImageSettings(request);
        } catch (Exception ex) {
            log.error("Fout tijdens ophalen combine settings: ", ex);
        }

        Transaction tx = HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
        try {
            Gegevensbron gb = SpatialUtil.getGegevensbron(gegevensbronId);

            String decoded = URLDecoder.decode(objectIds, "UTF-8");
            String[] ids = decoded.split(",");

            if (ids != null && ids.length > MAX_PDF_RECORDS) {

                String msg = "Er mogen maximaal " + MAX_PDF_RECORDS + " records in een"
                        + " pdf geexporteerd worden. Momenteel zijn er " + ids.length + " records"
                        + " geselecteerd.";

                writeErrorMessage(response, msg);
                log.debug(msg);

                return;
            }

            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            if (user == null) {
                writeErrorMessage(response, "Kan de data niet ophalen omdat u niet bent ingelogd.");
                return;
            }

            Bron b = gb.getBron(request);

            if (b == null) {
                throw new ServletException("Gegevensbron (id " + gb.getId() + ") Bron null.");
            }

            List data = null;
            String[] propertyNames = getThemaPropertyNames(gb);
            try {
                data = getData(b, gb, ids, propertyNames);
            } catch (Exception ex) {
                writeErrorMessage(response, ex.getMessage());
                log.error("Fout bij laden pdf data.", ex);
                return;
            }

            Date now = new Date();
            SimpleDateFormat df = new SimpleDateFormat("d MMMMM yyyy", new Locale("NL"));

            ObjectdataPdfInfo pdfInfo = new ObjectdataPdfInfo();
            pdfInfo.setTitel("Export van " + gb.getNaam());
            pdfInfo.setDatum(df.format(now));

            Map<Integer, Record> records = new HashMap();

            /* TODO: Objectdata kolommen obv volgordenr toevoegen ? */
            settings.setWidth(500);
            settings.setHeight(375);

            BASE64Encoder enc = new BASE64Encoder();
            int i = 0;
            for (Object obj : data) {
                String[] items = (String[]) obj;

                Record record = new ObjectdataPdfInfo.Record();

                record.setId(i);

                /* Geometrie ophalen */
                String wkt = items[items.length - 1];
                Geometry geom = null;
                try {
                    geom = DataStoreUtil.createGeomFromWKTString(wkt);
                } catch (Exception ex) {
                    log.error("Fout tijdens knutselen geometrie: ", ex);
                }

                /* Bbox uit berekenen */
                Envelope bbox = geom.getEnvelopeInternal();

                double[] dbbox = new double[4];
                double bufferSize = 50;

                dbbox[0] = bbox.getMinX() - bufferSize;
                dbbox[1] = bbox.getMinY() - bufferSize;
                dbbox[2] = bbox.getMaxX() + bufferSize;
                dbbox[3] = bbox.getMaxY() + bufferSize;

                /* Bbox in imageSettings */
                settings.setBbox(dbbox);

                /* Maak met imageSettings plaatje */
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                String imageUrl = "";
                try {
                    CombineImagesHandler.combineImage(baos, settings);

                    byte[] imageBytes = baos.toByteArray();
                    imageUrl = enc.encode(imageBytes);
                } catch (Exception ex) {
                }

                record.setImageUrl(imageUrl);

                /* Add items */
                for (int j = 0; j < items.length - 1; j++) {
                    String string = items[j];
                    record.addItem(propertyNames[j], string);
                }

                records.put(i, record);
                i++;
            }

            pdfInfo.setRecords(records);

            //String xmlFile = "/home/boy/dev/tmp/data.xml";
            //createXmlOutput(pdfInfo, xmlFile);

            try {
                createPdfOutput(pdfInfo, xsl_data2pdf, response);
            } catch (MalformedURLException ex) {
                writeErrorMessage(response, ex.getMessage());
                log.error("Fout tijdens maken pdf: ", ex);
            } catch (SAXException ex) {
                writeErrorMessage(response, ex.getMessage());
                log.error("Fout tijdens maken pdf: ", ex);
            }

        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().close();
        }
    }

    public static void createXmlOutput(ObjectdataPdfInfo object, String xmlFile) {
        try {
            File file = new File(xmlFile);
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectdataPdfInfo.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(object, file);
        } catch (JAXBException e) {
            log.error("Fout tijdens maken objectdata export naar xml: ", e);
        }
    }

    public static void createPdfOutput(ObjectdataPdfInfo pdfInfo, String template,
            HttpServletResponse response) throws MalformedURLException, IOException, SAXException {

        /* TODO: Ook liggend maken via orientation param ? */

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
            foUserAgent.setTitle("Objectdata PDF Export");

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            /* Setup Jaxb */
            JAXBContext jc = JAXBContext.newInstance(ObjectdataPdfInfo.class);
            JAXBSource src = new JAXBSource(jc, pdfInfo);

            /* Setup xslt */
            Source xsltSrc = new StreamSource(xslFile);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);

            if (transformer == null) {
                log.error("Fout tijdens inlezen xsl bestand.");
                return;
            }

            Result res = new SAXResult(fop.getDefaultHandler());

            if (transformer != null) {
                transformer.transform(src, res);
            }

            /* Setup response */
            response.setContentType(MimeConstants.MIME_PDF);
            response.setContentLength(out.size());

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", new Locale("NL"));
            String date = df.format(now);
            String fileName = "Objectdata_Export_" + date + ".pdf";

            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();

        } catch (Exception ex) {
            log.error("Fout tijdens objectdata pdf export: ", ex);
        } finally {
            out.close();
        }
    }

    public String[] getThemaPropertyNames(Gegevensbron gb) {
        Set themadata = gb.getThemaData();

        Iterator it = themadata.iterator();
        ArrayList columns = new ArrayList();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            if (td.getKolomnaam() != null) {
                if (!columns.contains(td.getKolomnaam())) {

                    if (!td.getKolomnaam().equalsIgnoreCase("the_geom")
                            && !td.getKolomnaam().equalsIgnoreCase("geometry")) {
                        columns.add(td.getKolomnaam());
                    }
                }
            }
        }
        String[] s = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            s[i] = (String) columns.get(i);
        }
        return s;
    }

    public List getData(Bron b, Gegevensbron gb, String[] pks, String[] propertyNames) throws IOException, Exception {

        Filter filter = FilterBuilder.createOrEqualsFilter(
                DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart(), pks);
        List<ThemaData> items = SpatialUtil.getThemaData(gb, false);
        List<String> propnames = DataStoreUtil.themaData2PropertyNames(items);
        ArrayList<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propnames, null, true);
        ArrayList result = new ArrayList();
        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);

            String[] row = new String[propertyNames.length + 1];

            for (int p = 0; p < propertyNames.length; p++) {
                Property property = f.getProperty(propertyNames[p]);
                if (property != null && property.getValue() != null && property.getValue().toString() != null) {
                    row[p] = property.getValue().toString().trim();
                } else {
                    row[p] = "";
                }
            }

            SimpleFeatureImpl feature = (SimpleFeatureImpl) f;
            Geometry geom = (Geometry) feature.getDefaultGeometry();
            String wkt = geom.toText();

            row[row.length - 1] = wkt;

            result.add(row);
        }

        return result;
    }

    /**
     * Writes a error message to the response
     */
    private void writeErrorMessage(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>" + HTMLTITLE + "</title>");
        pw.println("<script type=\"text/javascript\"> if(window.parent && (typeof window.parent.showCsvError == 'function')) { window.parent.showCsvError(); } </script>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<h1>Fout</h1>");
        pw.println("<h3>" + message + "</h3>");
        pw.println("</body>");
        pw.println("</html>");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            if (config.getInitParameter("xsl_data2pdf") != null) {
                xsl_data2pdf = getServletContext().getRealPath(config.getInitParameter("xsl_data2pdf"));
            }

            fopConfig = getServletContext().getRealPath("/WEB-INF/xsl/fop.xml");
            fontPath = getServletContext().getRealPath("/WEB-INF/xsl/fonts");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private CombineImageSettings getCombineImageSettings(HttpServletRequest request) throws Exception {
        String jsonSettingsParam = FormUtils.nullIfEmpty(request.getParameter("jsonSettings"));
        String legendUrls = FormUtils.nullIfEmpty(request.getParameter("legendUrls"));

        JSONObject jsonSettings = new JSONObject(jsonSettingsParam);
        CombineImageSettings settings = CombineImageSettings.fromJson(jsonSettings);

        Map legendMap = new HashMap();
        if (legendUrls != null) {
            log.debug("legendUrls: " + legendUrls);
            String[] arr = legendUrls.split(";");

            for (int i = 0; i < arr.length; i++) {
                String[] legendUrlsArr = arr[i].split("#");
                legendMap.put(legendUrlsArr[0], legendUrlsArr[1]);
            }

            settings.setLegendMap(legendMap);
        }

        String mimeType = FormUtils.nullIfEmpty(request.getParameter(OGCRequest.WMS_PARAM_FORMAT));
        if (mimeType != null && !mimeType.equals("")) {
            settings.setMimeType(mimeType);
        }

        return settings;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
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
