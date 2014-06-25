package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import static nl.b3p.gis.viewer.print.PrintServlet.fontPath;
import static nl.b3p.gis.viewer.print.PrintServlet.fopConfig;
import nl.b3p.gis.viewer.report.ReportInfo;
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
 * @author Boy de Wit, B3Partners
 */
public class ReportServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(Data2PDF.class);

    private static String xsl_report = null;
    private static final String TEMP_XML_FILE = "/tmp/data.xml";

    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        checkRequestParams(request, response);

        String gegevensbronId = request.getParameter("gbId");
        String recordId = request.getParameter("recordId");
        String reportType = request.getParameter("reportType");
        String pk = request.getParameter("pk");

        /* Kaartbeeld ophalen */
        CombineImageSettings settings = null;
        try {
            settings = getCombineImageSettings(request);

            settings.setWidth(500);
            settings.setHeight(375);
        } catch (Exception ex) {
            log.error("Fout tijdens ophalen combine settings: ", ex);
        }

        Date now = new Date();
        SimpleDateFormat df = new SimpleDateFormat("d MMMMM yyyy", new Locale("NL"));

        Transaction tx = null;
        try {
            tx = HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();

            GisPrincipal user = GisPrincipal.getGisPrincipal(request);
            if (user == null) {
                writeErrorMessage(response, "Kan de data niet ophalen omdat u niet bent ingelogd.");
                return;
            }

            Gegevensbron gb = SpatialUtil.getGegevensbron(gegevensbronId);

            /* Create new ReportInfo object */
            if (pk != null && !pk.isEmpty()) {
                recordId = pk;
            }

            ReportInfo info = new ReportInfo();
            
            ReportInfo.Bron startBron = createReportBron(gb, recordId, false, settings, info);

            if (reportType == null || reportType.isEmpty()) {
                info.setTitel("Rapport");
            } else {
                info.setTitel(reportType);
            }

            info.setDatum(df.format(now));
            info.setBron(startBron);

            if (log.isDebugEnabled()) {
                createXmlOutput(info, TEMP_XML_FILE);
            }

            try {
                createPdfOutput(info, xsl_report, response);
            } catch (MalformedURLException ex) {
                writeErrorMessage(response, ex.getMessage());
                log.error("Fout tijdens maken rapport pdf: ", ex);
            } catch (SAXException ex) {
                writeErrorMessage(response, ex.getMessage());
                log.error("Fout tijdens maken rapport pdf: ", ex);
            }

            tx.commit();

        } finally {
            if (tx != null) {
                tx.rollback();
            }

            HibernateUtil.getSessionFactory().getCurrentSession().close();
        }
    }

    private String createImageUrl(String wkt, CombineImageSettings settings) {
        /* Geometrie ophalen */
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

        String imageUrl = null;
        BASE64Encoder enc = new BASE64Encoder();
        try {
            CombineImagesHandler.combineImage(baos, settings);

            byte[] imageBytes = baos.toByteArray();
            imageUrl = enc.encode(imageBytes);
        } catch (Exception ex) {
        }
        
        return imageUrl;
    }

    private ReportInfo.Bron createReportBron(Gegevensbron gb,
            String recordId, boolean isChild,
            CombineImageSettings settings, ReportInfo info) {

        ReportInfo.Bron.LAYOUT table_type = ReportInfo.Bron.LAYOUT.FLAT_TABLE;

        if (isChild) {
            table_type = ReportInfo.Bron.LAYOUT.SIMPLE_TABLE;
        }

        /* TODO: 
         * Alleen labels in record plaatsen die in basisregel staan.
         * Bij ophalen data moet wel de waarde van de pk column nog 
         * opgehaald kunnen worden om data op te halen van gekoppelde plusjes
         */

        /* Ophalen labels */
        String[] columnNames = getObjectDataColumns(gb);

        /* Ophalen waardes */
        List<Object> data = null;
        try {
            
            // eerste keer wel geom ophalen voor tonen plaatje */
            if (table_type.equals(ReportInfo.Bron.LAYOUT.FLAT_TABLE)) {
                data = getData(gb.getBron(), gb, recordId, columnNames, isChild, true);
            } else {
                data = getData(gb.getBron(), gb, recordId, columnNames, isChild, false);
            }            
            
        } catch (Exception ex) {
            log.error("Fout bij ophalen ReportInfo data ", ex);
        }

        /* Uit Gegevensbron juiste pk column index ophalen */
        int pkIndex = 0;
        String pkColumn = gb.getAdmin_pk();

        for (int i = 0; i < columnNames.length; i++) {
            String column = columnNames[i];
            if (column.equalsIgnoreCase(pkColumn)) {
                pkIndex = i;
            }
        }

        /* Vullen bron object */
        ReportInfo.Bron bron = new ReportInfo.Bron();
        bron.setTitel(gb.getNaam());
        bron.setLayout(table_type);

        String[] labelNames = getObjectDataLabels(gb);
        bron.setLabels(labelNames);
        
        String imageUrl = null;
        List<ReportInfo.Record> records = new ArrayList<ReportInfo.Record>();
        for (Object obj : data) {
            String[] items = (String[]) obj;
            String pkValue = items[pkIndex];
            
            if (settings != null && table_type.equals(ReportInfo.Bron.LAYOUT.FLAT_TABLE)) {
                String wkt = items[items.length - 1];
                imageUrl = createImageUrl(wkt, settings);
            }            

            ReportInfo.Record record = new ReportInfo.Record();

            record.setId(pkValue);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                info.setImage_url(imageUrl);
            }

            // remove pk from items array
            List<String> list = new ArrayList<String>(Arrays.asList(items));
            list.remove(pkValue);
            items = list.toArray(new String[0]);

            record.setValues(items);

            bron.addRecord(record);

            List<ReportInfo.Bron> subBronnen = null;
            Set children = gb.getChildren();

            /* Sort op volgordenr */
            List<Gegevensbron> childList = new ArrayList<Gegevensbron>(children);
            Collections.sort(childList);

            for (Gegevensbron child : childList) {
                Gegevensbron gbChild = (Gegevensbron) child;

                ReportInfo.Bron childBron = createReportBron(gbChild, pkValue, true, null, null);
                if (childBron == null || childBron.getRecords() == null
                        || childBron.getRecords().size() < 1) {

                    continue;
                }

                if (subBronnen == null) {
                    subBronnen = new ArrayList<ReportInfo.Bron>();
                }

                subBronnen.add(childBron);
            }

            if (subBronnen != null && subBronnen.size() > 0) {
                for (ReportInfo.Bron childSubbron : subBronnen) {
                    record.addBron(childSubbron);
                }
            }
        }

        return bron;
    }

    public String[] getObjectDataColumns(Gegevensbron gb) {
        Set themadata = gb.getThemaData();

        Iterator it = themadata.iterator();
        ArrayList columns = new ArrayList();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            if (td.getKolomnaam() != null) {
                if (!columns.contains(td.getKolomnaam())) {

                    if (!td.getKolomnaam().equalsIgnoreCase("the_geom")
                            && !td.getKolomnaam().equalsIgnoreCase("geometry")) {

                        // basisregel of pk column
                        if (td.isBasisregel()
                                || td.getKolomnaam().equalsIgnoreCase(gb.getAdmin_pk())) {
                            columns.add(td.getKolomnaam());
                        }
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

    public String[] getObjectDataLabels(Gegevensbron gb) {
        List<String> columns = new ArrayList();

        Iterator it = gb.getThemaData().iterator();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();

            if (td.getKolomnaam() != null
                    && !td.getKolomnaam().equalsIgnoreCase("the_geom")
                    && !td.getKolomnaam().equalsIgnoreCase("geometry")) {

                // basisregel labels
                if (td.isBasisregel()) {
                    if (td.getLabel() != null && !td.getLabel().isEmpty()) {

                        if (!columns.contains(td.getLabel())) {
                            columns.add(td.getLabel());
                        }
                    }
                }
            }
        }

        String[] arr = new String[columns.size()];
        arr = columns.toArray(arr);

        return arr;
    }

    public List getData(Bron b, Gegevensbron gb, String recordId,
            String[] propertyNames, boolean isChild, boolean addGeom) throws IOException,
            Exception {

        String[] pks = new String[1];
        pks[0] = recordId;

        String column = null;
        if (!isChild) {
            column = gb.getAdmin_pk();
        } else {
            column = gb.getAdmin_fk();
        }

        Filter filter = FilterBuilder.createOrEqualsFilter(
                DataStoreUtil.convertFullnameToQName(column).getLocalPart(), pks);

        List<ThemaData> items = SpatialUtil.getThemaData(gb, false);
        List<String> propnames = DataStoreUtil.themaData2PropertyNames(items);

        ArrayList<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propnames, null, addGeom);
        ArrayList result = new ArrayList();

        int len = 0;
        if (addGeom) {
            len = propertyNames.length + 1;
        } else {
            len = propertyNames.length;
        }

        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);

            String[] row = new String[len];

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

            if (geom != null && addGeom) {
                String wkt = geom.toText();
                row[row.length - 1] = wkt;
            }

            result.add(row);
        }

        return result;
    }

    public static void createXmlOutput(ReportInfo object, String xmlFile) {
        try {
            File file = new File(xmlFile);
            JAXBContext jaxbContext = JAXBContext.newInstance(ReportInfo.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    true);

            jaxbMarshaller.marshal(object, file);
        } catch (JAXBException e) {
            log.error("Fout tijdens maken rapport xml: ", e);
        }
    }

    public static void createPdfOutput(ReportInfo info, String template,
            HttpServletResponse response) throws MalformedURLException,
            IOException, SAXException {

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
            foUserAgent.setTitle("Rapport A");

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            /* Setup Jaxb */
            JAXBContext jc = JAXBContext.newInstance(ReportInfo.class);
            JAXBSource src = new JAXBSource(jc, info);

            /* Setup xslt */
            Source xsltSrc = new StreamSource(xslFile);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);
            if (transformer
                    == null) {
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
            String fileName = "Rapport_" + date + ".pdf";

            response.setHeader(
                    "Content-Disposition", "attachment; filename=" + fileName);
            response.getOutputStream()
                    .write(out.toByteArray());
            response.getOutputStream()
                    .flush();

        } catch (Exception ex) {
            log.error("Fout tijdens maken rapport pdf: ", ex);
        } finally {
            out.close();
        }
    }

    private void writeErrorMessage(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = response.getWriter();
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>Report message</title>");
        pw.println("<script type=\"text/javascript\"> if(window.parent && (typeof window.parent.showCsvError == 'function')) { window.parent.showCsvError(); } </script>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<h1>Fout</h1>");
        pw.println("<h3>" + message + "</h3>");
        pw.println("</body>");
        pw.println("</html>");
    }

    private void checkRequestParams(HttpServletRequest request,
            HttpServletResponse response) {

        String gegevensbronId = request.getParameter("gbId");
        String recordId = request.getParameter("recordId");
        String reportType = request.getParameter("reportType");

        if (gegevensbronId == null || gegevensbronId.isEmpty()
                || recordId == null || recordId.isEmpty()
                || reportType == null || reportType.isEmpty()) {

            try {
                writeErrorMessage(response, "Ongeldige rapport parameters.");
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            if (config.getInitParameter("xsl_report") != null) {
                xsl_report = getServletContext().getRealPath(config.getInitParameter("xsl_report"));
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
     * Handles the HTTP <code>GET</code> method.
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
     * Handles the HTTP <code>POST</code> method.
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
        return "Objectdata 2 Report Servlet";
    }// </editor-fold>
}
