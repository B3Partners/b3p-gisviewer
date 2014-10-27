package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.Filter;
import sun.misc.BASE64Encoder;

/**
 *
 * @author Chris van Lith
 */
public class ReportServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(ReportServlet.class);

    private static String xsl_report = null;
    private static String PK_ONLY = "only pk no basisregel";

    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        try {
            checkRequestParams(request);

            String gegevensbronId = request.getParameter("gbId");
            String recordId = request.getParameter("recordId");
            String reportType = request.getParameter("reportType");
            String pk = request.getParameter("pk");

            /* Kaartbeeld ophalen */
            CombineImageSettings settings = null;
            settings = getCombineImageSettings(request);
            settings.setWidth(500);
            settings.setHeight(375);

            Date now = new Date();
            SimpleDateFormat df = new SimpleDateFormat("d MMMMM yyyy", new Locale("NL"));

            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            Transaction t=null;
            try {
                t=sess.beginTransaction();

                GisPrincipal user = GisPrincipal.getGisPrincipal(request);
                if (user == null) {
                    throw new Exception("Kan de data niet ophalen omdat u niet bent ingelogd.");
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
                    createXmlOutput(info);
                }

                createPdfOutput(info, xsl_report, response);
                
                t.commit();

            } finally {
                if (sess.isOpen()) {
                    sess.close();
                }
            }
        } catch (Exception e) {
            log.error(e);
            writeErrorMessage(response, e.getMessage());
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
            CombineImageSettings settings, ReportInfo info) throws Exception {
        
        if (gb==null) {
            throw new Exception("Geen gegevensbron gevonden");
        }

        ReportInfo.Bron.LAYOUT table_type = ReportInfo.Bron.LAYOUT.FLAT_TABLE;
        if (isChild) {
            table_type = ReportInfo.Bron.LAYOUT.SIMPLE_TABLE;
        }

        // Ophalen labels plus pk kolom
        Map<String,String> dataColumns = getObjectDataColumns(gb);
        if (dataColumns==null || dataColumns.isEmpty()) {
            throw new Exception("Geen kolommen gevonden voor gegevensbron: " + gb.getNaam());
        }
        String pkColumn = gb.getAdmin_pk();
        
        // eerste keer wkt ophalen voor tonen plaatje
        String wkt = null;
        if (table_type.equals(ReportInfo.Bron.LAYOUT.FLAT_TABLE)) {
            wkt = getWktForImageUrl(gb.getBron(), gb, recordId);
        }
        // Ophalen waardes via met alle kolomnamen plus pk
        List<String> columnNames = new ArrayList<String>();
        columnNames.addAll(dataColumns.keySet());
        List<Map> data = getData(gb.getBron(), gb, recordId, columnNames, isChild); 
        
        // Bepaal alle kolomnamen in basisregel
        List<String> basisregelColumnNames = new ArrayList<String>();
        for (String cn : dataColumns.keySet()) {
            if (!dataColumns.get(cn).equals(PK_ONLY)) {
                basisregelColumnNames.add(cn);
            }
        }
        
        // Vullen bron object
        ReportInfo.Bron bron = new ReportInfo.Bron();
        bron.setTitel(gb.getNaam());
        bron.setLayout(table_type);

        String imageUrl = null;
        for (Map<String,String> row : data) {
            String pkValue = row.get(pkColumn);
            if (settings != null && table_type.equals(ReportInfo.Bron.LAYOUT.FLAT_TABLE) 
                    && wkt != null) {
                imageUrl = createImageUrl(wkt, settings);
            }            
            ReportInfo.Record record = new ReportInfo.Record();
            record.setId(pkValue);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                info.setImage_url(imageUrl);
            }

            String[] values = new String[basisregelColumnNames.size()];
            for (int i=0; i < basisregelColumnNames.size(); i++) {
               values[i] = row.get(basisregelColumnNames.get(i));
            }
            record.setValues(values);
            bron.addRecord(record);
            List<ReportInfo.Bron> subBronnen = null;
            Set children = gb.getChildren();
            // Sort op volgordenr
            List<Gegevensbron> childList = new ArrayList<Gegevensbron>(children);
            if (childList.size() > 0) {
                Collections.sort(childList);
            }

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
        
        String[] labelNames = new String[basisregelColumnNames.size()];
        for (int i = 0; i < basisregelColumnNames.size(); i++) {
            labelNames[i] = dataColumns.get(basisregelColumnNames.get(i));
        }
        bron.setLabels(labelNames);

        return bron;
    }

    public Map<String,String> getObjectDataColumns(Gegevensbron gb) {
        Set themadata = gb.getThemaData();
        if (themadata==null || themadata.isEmpty()) {
            return null;
        }
        List tdList = new ArrayList();
        tdList.addAll(themadata);
        Collections.sort(tdList);

        Iterator it = tdList.iterator();
        Map<String,String> dataColumns = new LinkedHashMap<String,String>();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            if (td.getKolomnaam() != null) {
                if (!dataColumns.containsKey(td.getKolomnaam())) {
                    // pk column
                    if (td.getKolomnaam().equalsIgnoreCase(gb.getAdmin_pk())) {
                        dataColumns.put(td.getKolomnaam(), PK_ONLY);
                    }
                }
                // basisregel
                if (td.isBasisregel()) {
                    dataColumns.put(td.getKolomnaam(), td.getLabel());
                }
            }
        }
        return dataColumns;
    }

    public List<Map> getData(Bron b, Gegevensbron gb, String recordId,
            List<String> propertyNames, boolean isChild) throws IOException,
            Exception {
        
        //creeer filter voor de juiste records obv kolomnaam en id
        String column = null;
        if (!isChild) {
            column = gb.getAdmin_pk();
        } else {
            column = gb.getAdmin_fk();
        }
        Filter filter = FilterBuilder.createOrEqualsFilter(
                DataStoreUtil.convertFullnameToQName(column).getLocalPart(), 
                new String[] {recordId});

        List<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propertyNames, null, false);
        List result = new ArrayList();

        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);

            Map<String,String> row = new HashMap<String,String>();

            for (int p = 0; p < propertyNames.size(); p++) {
                Property property = f.getProperty(propertyNames.get(p));
                if (property != null && property.getValue() != null && property.getValue().toString() != null) {
                    row.put(propertyNames.get(p), property.getValue().toString().trim());
                } else {
                    row.put(propertyNames.get(p), "-");
                }
            }

            result.add(row);
        }

        return result;
    }
    
    private String getWktForImageUrl(Bron b, Gegevensbron gb, String recordId) throws Exception {

        String wkt = null;
        String column = gb.getAdmin_pk();

        Filter filter = FilterBuilder.createOrEqualsFilter(
                DataStoreUtil.convertFullnameToQName(column).getLocalPart(), 
                new String[] {recordId});

        List<ThemaData> items = SpatialUtil.getThemaData(gb, false);
        List<String> propnames = DataStoreUtil.themaData2PropertyNames(items);

        ArrayList<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propnames, null, true);
        
        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);

            SimpleFeatureImpl feature = (SimpleFeatureImpl) f;
            Geometry geom = (Geometry) feature.getDefaultGeometry();

            if (geom != null) {
                wkt = geom.toText();
            }
        }

        return wkt;    
    }

    public static void createXmlOutput(ReportInfo object) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(ReportInfo.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                true);
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(object, sw);
        log.debug("xml data for report:" + sw.toString());
    }

    public static void createPdfOutput(ReportInfo info, String template,
            HttpServletResponse response) throws Exception {

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

    private void checkRequestParams(HttpServletRequest request) throws Exception {

        String gegevensbronId = request.getParameter("gbId");
        String recordId = request.getParameter("recordId");
        String reportType = request.getParameter("reportType");

        if (gegevensbronId == null || gegevensbronId.isEmpty()
                || recordId == null || recordId.isEmpty()
                || reportType == null || reportType.isEmpty()) {
            throw new Exception("Ongeldige rapport parameters.");
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
