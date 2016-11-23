package nl.b3p.gis.viewer.services;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.geotools.FilterBuilder;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.services.ObjectdataPdfInfo.Record;
import nl.b3p.imagetool.CombineImageSettings;
import nl.b3p.imagetool.CombineImagesHandler;
import nl.b3p.ogc.utils.OGCRequest;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.hibernate.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.Filter;

/**
 *
 * @author Roy
 */
public class Data2PDF extends HttpServlet {

    private static final Log log = LogFactory.getLog(Data2PDF.class);
    private static String HTMLTITLE = "Exporteren";
    private static String xsl_data2pdf = null;
    private static String xsl_data2html = null;
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

        String appCode = (String) request.getParameter("appCode"); 
        String gegevensbronId = request.getParameter("gbId");
        String objectIds = request.getParameter("objectIds");
        String orientation = request.getParameter("orientation");
        String format = request.getParameter("format");

        if (gegevensbronId == null || gegevensbronId.equals("")
                || objectIds == null || objectIds.equals("")) {
            writeErrorMessage(response, "Ongeldige request parameters.");
            log.error("Ongeldige request parameters.");

            return;
        }

        if ((xsl_data2pdf == null || xsl_data2pdf.equals("")) 
                 && (format == null || format.equalsIgnoreCase("pdf")) ) {
            writeErrorMessage(response, "Xsl PDF template niet geconfigureerd.");
            log.error("Xsl PDF template niet geconfigureerd.");

            return;
        }
        if ((xsl_data2html == null || xsl_data2html.equals("")) 
                && format != null && format.equalsIgnoreCase("html") ) {
            writeErrorMessage(response, "Xsl HTML template niet geconfigureerd.");
            log.error("Xsl HTML template niet geconfigureerd.");

            return;
        }

        Transaction tx = HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
        try {
            Gegevensbron gb = SpatialUtil.getGegevensbron(gegevensbronId);

            String decoded = URLDecoder.decode(objectIds, "UTF-8");
            String[] ids = decoded.split(",");
            
            // check if number of max records specified in config
            MAX_PDF_RECORDS = getMaxPDFRecords(gb, appCode);

            if (ids != null && ids.length > MAX_PDF_RECORDS) {

                String msg = "Er mogen maximaal " + MAX_PDF_RECORDS + " records "
                        + " geexporteerd worden. Momenteel zijn er " + ids.length + " records"
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
            
            // see if need to display columns with data order
            Boolean withDataOrder = displayAllWithDataOrder(gb, appCode);
            
            // see if need to display only columns with basisRegel
            Boolean basisOnly = displayOnlyBasisRegel(gb, appCode);
            
            // if to display columns with dataOrder, then we want more than basisregel
            if (withDataOrder) {
                basisOnly = false;
            }
            
            String[] propertyNames = getThemaPropertyNames(gb, basisOnly, withDataOrder);
            Map columnLabels = getThemaLabelNames(gb, basisOnly, withDataOrder);
            try {
                data = getData(b, gb, ids, propertyNames, appCode);
            } catch (Exception ex) {
                writeErrorMessage(response, ex.getMessage());
                log.error("Fout bij laden pdf data.", ex);
                return;
            }

            Date now = new Date();
            SimpleDateFormat df = new SimpleDateFormat("d MMMMM yyyy", new Locale("NL"));

            ObjectdataPdfInfo pdfInfo = new ObjectdataPdfInfo();
            pdfInfo.setTitel(gb.getNaam());
            pdfInfo.setDatum(df.format(now));

            Map<Integer, Record> records = new HashMap();

            Base64 enc  = new Base64();
            
            String jsonSettingsParam = FormUtils.nullIfEmpty(request.getParameter("jsonSettings"));
            String legendUrls = FormUtils.nullIfEmpty(request.getParameter("legendUrls"));
            JSONObject jsonSettings = new JSONObject(jsonSettingsParam);
            String mimeType = FormUtils.nullIfEmpty(request.getParameter(OGCRequest.WMS_PARAM_FORMAT));
            if (mimeType != null && !mimeType.equals("")) {

            }
            int i = 0;
            for (Object obj : data) {
                CombineImageSettings settings = null;
                try {
                    settings = getCombineImageSettings(jsonSettings, legendUrls);
                } catch (Exception ex) {
                    log.error("Fout tijdens ophalen combine settings: ", ex);
                }
                /* TODO: Objectdata kolommen obv volgordenr toevoegen ? */
                settings.setWidth(500);
                settings.setHeight(375);
                settings.setMimeType(mimeType);

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
                    imageUrl = enc.encodeToString(imageBytes);
                } catch (Exception ex) {
                }

                record.setImageUrl(imageUrl);

                /* Add items */
                for (int j = 0; j < items.length - 1; j++) {
                    String string = items[j];
                    String propertyName = propertyNames[j];
                    String label = (String)columnLabels.get(propertyName);
                    record.addItem(label, string);
                }

                records.put(i, record);
                i++;
            }

            pdfInfo.setRecords(records);

            //String xmlFile = "/home/boy/dev/tmp/data.xml";
            //createXmlOutput(pdfInfo, xmlFile);

            if (format == null || format.isEmpty() || format.equalsIgnoreCase("pdf")) {
                createPdfOutput(pdfInfo, xsl_data2pdf, response);
            } else if (format.equalsIgnoreCase("html")) {
                createHtmlOutput(pdfInfo, xsl_data2html, response);
            } else if (format.equalsIgnoreCase("xml")) {
                createXmlOutput(pdfInfo, response);
            }

        } catch (JSONException ex) {
            log.error("Error creating CombineImageSettings: ",ex);
        } finally {
            HibernateUtil.getSessionFactory().getCurrentSession().close();
        }
    }

    public static void createXmlOutput(ObjectdataPdfInfo pdfInfo,
            HttpServletResponse response) throws IOException  {

        /* Setup output stream */
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectdataPdfInfo.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(pdfInfo, out);

            response.setContentType("text/xml");
            response.setContentLength(out.size());

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", new Locale("NL"));
            String date = df.format(new Date());
            String fileName = "Objectdata_Export_" + date + ".xml";

            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();
            // output pretty printed
        } catch (JAXBException jex) {
           log.error("Fout tijdens objectdata xml export: ", jex);
        } finally {
            out.close();
        }
    }
 
    public static void createHtmlOutput(ObjectdataPdfInfo pdfInfo, String template,
            HttpServletResponse response) throws IOException  {

        /* Setup output stream */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        File xslFile = new File(template);

        try {
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
            Result res = new StreamResult(out);
            transformer.transform(src, res);
            
            response.setContentType("text/html");
            response.setContentLength(out.size());

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", new Locale("NL"));
            String date = df.format(new Date());
            String fileName = "Objectdata_Export_" + date + ".html";

            response.setHeader("Content-Disposition", "inline; filename=" + fileName);
            response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();
            // output pretty printed
        } catch (JAXBException jex) {
            log.error("Fout tijdens objectdata html export: ", jex);
        } catch (TransformerConfigurationException ex) {
            log.error("Fout tijdens objectdata html export: ", ex);
        } catch (TransformerException ex) {
            log.error("Fout tijdens objectdata html export: ", ex);
        } finally {
            out.close();
        }
     }
    
    public static void createPdfOutput(ObjectdataPdfInfo pdfInfo, String template,
            HttpServletResponse response) throws IOException {

        /* TODO: Ook liggend maken via orientation param ? */
        File xslFile = new File(template);
        String path = new File(xslFile.getParent()).toURI().toString();
        /* Setup fopfactory */
        FopFactory fopFactory = FopFactory.newInstance();
        /* Setup output stream */
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            /* Set BaseUrl so that fop knows paths to images etc... */
             fopFactory.setBaseURL(path);
             fopFactory.getFontManager().setFontBaseURL(fontPath);
             fopFactory.setUserConfig(new File(fopConfig));

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
        return getThemaPropertyNames(gb, true, false);
    }
    
    public String[] getThemaPropertyNames(Gegevensbron gb, boolean basisOnly, boolean withDataOrder) {
        List themadata = new ArrayList(gb.getThemaData());
        Collections.sort(themadata);

        Iterator it = themadata.iterator();
        ArrayList columns = new ArrayList();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            if (td.getKolomnaam() != null) {
                if (!columns.contains(td.getKolomnaam())) {
                    if (td.isBasisregel() || !basisOnly) {
                        if (!td.getKolomnaam().equalsIgnoreCase("the_geom")
                                && !td.getKolomnaam().equalsIgnoreCase("geometry")) {
                            columns.add(td.getKolomnaam());
                        }
                        
                        // if we only to display columns with data order, remove if dataOrder null
                        if(td.getDataorder() == null && withDataOrder ){
                            columns.remove(columns.size() -1 );
                            
                            
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

    public Map getThemaLabelNames(Gegevensbron gb) {
        return getThemaLabelNames(gb, true, false);
    }

    public Map getThemaLabelNames(Gegevensbron gb, boolean basisOnly, boolean withDataOrder) {
        List themadata = new ArrayList(gb.getThemaData());
        Collections.sort(themadata);

        Iterator it = themadata.iterator();
        ArrayList columns = new ArrayList();
        Map labels = new HashMap();
        while (it.hasNext()) {
            ThemaData td = (ThemaData) it.next();
            if (td.getKolomnaam() != null) {
                if (!columns.contains(td.getKolomnaam())) {
                    
                    if (td.isBasisregel() || !basisOnly) {
                        if (!td.getKolomnaam().equalsIgnoreCase("the_geom")
                                && !td.getKolomnaam().equalsIgnoreCase("geometry")) {
                            columns.add(td.getKolomnaam());

                            if (td.getLabel() != null) {
                                labels.put(td.getKolomnaam(), td.getLabel());
                            } else {
                                labels.put(td.getKolomnaam(), td.getKolomnaam());
                            }
                            // if we only to display columns with data order, remove if dataOrder null
                            if (td.getDataorder() == null && withDataOrder){
                                labels.remove(td.getKolomnaam());
                                        
                            }
                        }
                    }
                }
            }
        }
        
        return labels;
    }

    public List getData(Bron b, Gegevensbron gb, String[] pks, String[] propertyNames, String appCode) throws IOException, Exception {

        Filter filter = FilterBuilder.createOrEqualsFilter(
                DataStoreUtil.convertColumnNameToQName(gb.getAdmin_pk()).getLocalPart(), pks);
        List<ThemaData> items = SpatialUtil.getThemaData(gb, false);
        List<String> propnames = DataStoreUtil.themaData2PropertyNames(items);
        Integer maximum = ConfigKeeper.getMaxNumberOfFeatures(appCode);
        ArrayList<Feature> features = DataStoreUtil.getFeatures(b, gb, null, filter, propnames, maximum, true);
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
            String wkt = null;
            
            // Ook records zonder geom kunnen zo in de pdf komen
            if(geom != null && !geom.isEmpty()){
                wkt = geom.toText();
            } else {
                wkt = "POINT (0 0)";
            }

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
            if (config.getInitParameter("xsl_data2html") != null) {
                xsl_data2html = getServletContext().getRealPath(config.getInitParameter("xsl_data2html"));
            }

            fopConfig = getServletContext().getRealPath("/WEB-INF/xsl/fop.xml");
            fontPath = getServletContext().getRealPath("/WEB-INF/xsl/fonts");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private CombineImageSettings getCombineImageSettings(JSONObject jsonSettings, String legendUrls) throws Exception {

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
        return settings;
    }
    
    public int getMaxPDFRecords(Gegevensbron gb, String appCode) {
        Integer maxPDFRecords = null;

        try {
            ConfigKeeper configKeeper = new ConfigKeeper();
            Map map = configKeeper.getConfigMap(appCode, true);
            if (map != null) {
                maxPDFRecords = (Integer) map.get("maxPDFRecords");

            }
        } catch (Exception ex) {
            // just pass and give back the default value
        }

        if (maxPDFRecords != null) {
            return maxPDFRecords;
        } else {
            return MAX_PDF_RECORDS;
        }

    }
    
    public boolean displayOnlyBasisRegel(Gegevensbron gb, String appCode) {
        Boolean basisRegelOnly = null;

        try {
            ConfigKeeper configKeeper = new ConfigKeeper();
            Map map = configKeeper.getConfigMap(appCode, true);
            if (map != null) {
                basisRegelOnly = (Boolean) map.get("displayOnlyBasisRegel");

            }
        } catch (Exception ex) {
            log.error(ex);
            // just pass and give back the value true
            // to display only basisRegel
        }

        if (basisRegelOnly != null) {
            return basisRegelOnly;
        } else {
            return true;
        }

    }
    
        public boolean displayAllWithDataOrder(Gegevensbron gb, String appCode) {
        Boolean displayAllWithDataOrder = null;

        try {
            ConfigKeeper configKeeper = new ConfigKeeper();
            Map map = configKeeper.getConfigMap(appCode, true);
            if (map != null) {
                displayAllWithDataOrder = (Boolean) map.get("displayAllWithDataOrder");

            }
        } catch (Exception ex) {
            log.error(ex);
            // just pass and give back the value false
            // to get default behaviour
        }

        if (displayAllWithDataOrder != null) {
            return displayAllWithDataOrder;
        } else {
            return false;
        }

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
