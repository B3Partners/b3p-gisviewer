package nl.b3p.gis.viewer.downloads;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import nl.b3p.gis.viewer.services.DownloadServlet;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.writers.StreamingShapeWriter;
import nl.b3p.ogc.utils.OGCConstants;
import nl.b3p.ogc.utils.OGCRequest;
import nl.b3p.ogc.utils.geotools.Util;
import nl.b3p.xml.ows.v100.OnlineResource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.ows.OperationType;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.v1_0_0.WFS_1_0_0_DataStore;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.StreamingParser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Boy
*/
public class DownloadThread extends Thread {
    
    private static final Log log = LogFactory.getLog(DownloadThread.class);

    private static final String GML = "GML";
    private static final String SHP = "SHP";

    private static final int maxFileNameLength = 50;
    private static final String ZIPNAME = "dataset.zip";
    private static final String EXTENSION = ".zip";

    public static final int STATUS_NEW = 0;
    public static final int STATUS_STARTED = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_ERROR = 3;

    private static int threadCounter = 1;
    private static int SERVICE_EXCEPTION_TEST_LENGTH = 10000;
    private String email;
    private String[] uuids;
    private String formaat;
    private Integer threadStatus;
    private boolean running = false;
    static final Logging logging = Logging.ALL;

    /**
     * Creates a new instance of DownloadThread
     */
    public DownloadThread(ThreadGroup tg) {
        super(tg, nextThreadName());
        threadStatus = STATUS_NEW;
    }

    @Override
    public void run() {
        threadStatus = STATUS_STARTED;
        running = true;
        Transaction tx = null;
        try {
            //start hibernate session
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            tx = sess.beginTransaction();
            //create unique working dir
            File workingDir = createUniqueFile(DownloadServlet.uniqueName(EXTENSION));
            if (!workingDir.mkdir()) {
                throw new IOException("Can't create directory: " + workingDir.getAbsolutePath());
            }

            //create zipfile
            String zipfilename = workingDir.getPath() + File.separator + ZIPNAME;
            log.debug("Zipfilename: " + zipfilename);
            
            //try to load the data in a zip for all the uuid
            ArrayList<String> erroredTitles = new ArrayList<String>();
            ArrayList<String> successTitles = new ArrayList<String>();
            for (String uuid : uuids) {                
                
                String error = null;
                
                String fileName = "uuid_" + uuid;
                fileName = createValidFileName(fileName);
                String workingPathDataset = workingDir.getAbsolutePath() + File.separator + fileName + File.separator;

                File filePathDataset = createUniqueFile(workingPathDataset);
                if (!filePathDataset.mkdir()) {
                    throw new IOException("Can't create directory: " + filePathDataset.getAbsolutePath());
                }
                
                if (error == null) {
                    try {
                        writeZipfileToWorkingDir(fileName, filePathDataset, formaat);
                    } catch (Exception e) {
                        log.debug("Dataset opgehaald met fouten: ", e);
                        error = "Dataset opgehaald met fouten: \n" + e.toString();
                        if (e.getCause()!=null){
                            error+="\nReden:\n"+e.getCause().toString();
                        }

                    }
                }

                if (error != null) {
                    
                    log.debug("error while getting data for uuid "+uuid+": "+error);
                    erroredTitles.add(fileName);
                } else {                    
                    successTitles.add(fileName);
                }

            }            
            
            //we have downloaded some datasetes. HURRAY! Zip and ship(later)
            if (uuids.length - erroredTitles.size() > 0) {
                ZipOutputStream zip = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(zipfilename);
                    zip = new ZipOutputStream(fos);
                    putDirInZip(zip, new File(workingDir.getAbsolutePath()), "");
                } catch (Exception ex) {
                    throw new Exception("Exception in creating zip: ", ex);
                } finally {
                    try {
                        if (zip != null) {
                            zip.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (Exception e) {
                        log.error("Can't close zip.", e);
                    }
                }
            }

            sendEmail(zipfilename, erroredTitles, successTitles);
            threadStatus = STATUS_FINISHED;
        } catch (Exception e) {
            threadStatus = STATUS_ERROR;
            log.error("Error downloading the data.", e);
        } finally {
            try {
                if (tx != null) {
                    tx.commit();
                }
            } catch (Exception e) {
                log.error("Error committing transaction, do rollback: ", e);
                tx.rollback();
            }
        }
    }

    private void writeZipfileToWorkingDir(String zipfilename, File workingDir, String formaat) throws Exception {
        //schrijf metadata in zip file.
        log.debug("Schrijven van Dataset naar directory voor uuid: " + zipfilename + " STARTED");

        List<OnlineResource> onlineResources = new ArrayList<OnlineResource>();
        byte[] buffer = new byte[8192];
        String title = zipfilename;
        for (OnlineResource or : onlineResources) {
            WFSDataStore datastore = null; //createDatastore(or);
            InputStream is = null;
            FileOutputStream fos = null;
            BufferedInputStream bis = null;
            try {
                if (datastore == null) {
                    throw new Exception("Can not connect with online resource: ");
                }
                is = getInputStream(datastore, zipfilename);
                if (formaat.equalsIgnoreCase(GML)) {
                    //create output gml file.
                    String gmlFileName = createValidFileName(title + "_" + zipfilename);
                    File gmlFile = new File(workingDir.getAbsolutePath()+ File.separator + gmlFileName + ".gml");
                    fos = new FileOutputStream(gmlFile);
                    bis = new BufferedInputStream(is);

                    int i = -1;
                    while (true) {
                        i = bis.read(buffer, 0, buffer.length);
                        if (i == -1) {
                            break;
                        }
                        fos.write(buffer, 0, i);
                    }
                    fos.close();
                    String serviceException= getServiceException(gmlFile);
                    if (serviceException!=null){
                        throw new Exception("ServiceExceptionReport returned by service: (first "+SERVICE_EXCEPTION_TEST_LENGTH+" bytes)"+serviceException);
                    }

                } else if (formaat.equalsIgnoreCase(SHP)) {
                    org.geotools.xml.Configuration configuration = getCorrectConfiguration(datastore, null);

                    CoordinateReferenceSystem crs=CRS.decode("EPSG:28992");

                    StreamingShapeWriter ssw = new StreamingShapeWriter(workingDir.getAbsolutePath()+File.separator, crs);

                    StreamingParser parser = new StreamingParser(configuration, is, SimpleFeature.class);
                    List<String> removeAttrNames = new ArrayList<String>();
                    removeAttrNames.add("boundedBy");
                    removeAttrNames.add("name");
                    removeAttrNames.add("description");
                    removeAttrNames.add("shape.area");
                    removeAttrNames.add("shape.len");
                    try {
                        SimpleFeature f = null;
                            while ((f = (SimpleFeature) parser.parse()) != null) {
                                f = Util.rebuildFeature(f, removeAttrNames);
                                ssw.write(f);
                            }
                        if (ssw.getFeaturesWritten() == 0) {
                            throw new Exception("Geen data gevonden dat kon worden geschreven naar de shape file.");
                        }
                    } finally {
                        log.info(ssw.getFeaturesWritten() + "/" + ssw.getFeaturesGiven() + " features written");
                        ssw.close();
                    }
                }
            }catch(Exception e){
                String errorString="Fout bij laden van onlineResource: "+ zipfilename +"\nname: " + zipfilename;
                log.debug(errorString,e);
                throw new Exception(errorString,e);
            } finally {
                if (datastore != null) {
                    datastore.dispose();
                }
                if (is != null) {
                    is.close();
                }
                if (bis != null) {
                    bis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }
           //overwritePrjFiles(workingDir.getAbsolutePath()+File.separator);
        }
        log.debug("Schrijven van DATASET naar directory voor uuid: " + zipfilename + " DONE");
    }

    private InputStream getInputStream(WFSDataStore datastore, String typeName) throws IOException, Exception {
        //WFSDataStore datastore =createDatastore(or);
        OperationType getFeatureOt = null;
        String version="1.0.0";
        if (datastore == null) {
            throw new Exception("DataStore is null");
        } else if (datastore instanceof WFS_1_0_0_DataStore) {
            getFeatureOt = ((WFS_1_0_0_DataStore) datastore).getCapabilities().getGetFeature();
        } else if (datastore instanceof WFS_1_1_0_DataStore) {
            getFeatureOt = ((WFS_1_0_0_DataStore) datastore).getCapabilities().getGetFeature();
            version="1.1.0";
        }

        HttpClient client = new HttpClient();
        HttpMethod method = null;
        //make request
        OGCRequest request = null;
        if (getFeatureOt != null && getFeatureOt.getGet() != null) {
            request = new OGCRequest(getFeatureOt.getGet().toString());
        } else if (getFeatureOt != null && getFeatureOt.getPost() != null) {
            request = new OGCRequest(getFeatureOt.getPost().toString());
        } else {
            throw new Exception("Service doesn't support GetFeature request or doesnt support GetFeature request with Http-post or -get");
        }

        request.addOrReplaceParameter(OGCRequest.SERVICE, OGCRequest.WFS_SERVICE_WFS);
        request.addOrReplaceParameter(OGCRequest.VERSION, version);
        request.addOrReplaceParameter(OGCRequest.REQUEST, OGCRequest.WFS_REQUEST_GetFeature);
        request.addOrReplaceParameter(OGCRequest.WFS_PARAM_TYPENAME, typeName);

        if (getFeatureOt.getGet() != null) {
            method = new GetMethod(request.getUrl());

        } else {
            method = new PostMethod(request.getUrlWithNonOGCparams());
            String body = request.getXMLBody();
            //work around voor ESRI post request. Contenttype mag geen text/xml zijn.
            //method.setRequestEntity(new StringRequestEntity(body, "text/xml", "UTF-8"));
            ((PostMethod) method).setRequestEntity(new StringRequestEntity(body, null, null));
        }
        int status = client.executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            return method.getResponseBodyAsStream();
        } else {
            throw new IOException("Can not read url (" + request.toString() + "). Server returning: httpstatus: " + status);
        }

    }    

    private void sendEmail(String zipFile, ArrayList<String> erroredTitles, ArrayList<String> successTitles) {
        File zip = new File(zipFile);
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        String status = null;
        String smtpHost = DownloadServlet.getSmtpHost();
        try {
            //Create the JavaMail session
            java.util.Properties properties = System.getProperties();

            properties.put("mail.smtp.host", smtpHost);
            javax.mail.Session mailSession = javax.mail.Session.getInstance(properties, null);
            mailSession.setDebug(true);

            //Contruct the message
            MimeMessage msg = new MimeMessage(mailSession);

            //Instelling instelling= (Instelling) sess.get(Instelling.class,Instelling.EMAILADRESVAN);
            String adresvan = DownloadServlet.getEmailFrom();
            // For Setting the from address
            if (adresvan == null) {
                throw new Exception("Instelling " + DownloadServlet.getEmailFrom() + " is verplicht.");
            }
            Address frmAddress = new InternetAddress(adresvan);
            msg.setFrom(frmAddress);

            // Parse and set the recipient addresses
            Address[] toAddresses = InternetAddress.parse(getEmail());
            msg.setRecipients(Message.RecipientType.TO, toAddresses);

            // Set the subject and text
            String onderwerp = null;
            if (zip.exists()) {
                onderwerp = DownloadServlet.getEmailSubject();
                if (onderwerp == null) {
                    log.error("Kan instelling " + DownloadServlet.getEmailSubject() + " niet vinden.");
                }
            }

            if (onderwerp != null) {
                msg.setSubject(onderwerp);
            }
            String tekst = "";
            //MimeBodyPart mbp = new MimeBodyPart();
            String klantStatus = "";

            if (successTitles.size() > 0){
                klantStatus = "De volgende kaart(en) zijn klaar gezet ("+successTitles.size()+"): ";
                for (String title : successTitles){
                    klantStatus+="<br/>"+title;
                }
            }
            if (erroredTitles.size() > 0) {
                if (klantStatus.length()>1){
                    klantStatus+="<br>";
                }
                klantStatus += "<br>De volgende kaart(en) is/zijn tijdelijk niet compleet of beschikbaar ("+erroredTitles.size()+"): ";
                for (String title : erroredTitles) {
                    klantStatus+="<br/>"+title;
                }
            }
            if (zip.exists()) {
                tekst = "Download link hier: ";

                String link = DownloadServlet.getDownloadServletPath() + "?download=" + zipFile;
                if (!tekst.contains(":link:")) {
                    tekst += "<br><br>De link naar de download is: " + link;
                } else {
                    tekst = tekst.replaceAll(":link:", link);
                }
            }
            if (!tekst.contains(":status:")) {
                tekst += "<br><br>" + klantStatus;
            } else {
                tekst = tekst.replaceAll(":status:", klantStatus);
            }
            //tekst = tekst.replaceAll(":naampersoon:", getNaamPersoon());

            msg.setContent(tekst, "text/html");

            msg.setSentDate(new Date());
            Transport.send(msg);
            status = "Het bericht met de code is met succes naar het volgende emailadres gestuurd: " + this.getEmail() + ".";
            log.debug(status);
        } catch (AddressException ae) {
            status = "Er is een fout opgetreden bij het opgegeven emailadres. ";
            log.error(status, ae);
        } catch (SendFailedException sfe) {
            status = "Er is een fout opgetreden bij het verzenden van het bericht.";
            log.error(status, sfe);
        } catch (MessagingException me) {
            status = "Er is een onbekende fout opgetreden bij het verzenden van het bericht. ";
            log.error(status, me);
        } catch (Exception e) {
            status = "Error";
            log.error(status, e);
        }

    }

    private void putDirInZip(ZipOutputStream zip, File dirFile, String rootEntity) throws IOException, Exception {
        byte[] buffer = new byte[8192];
        if (rootEntity == null) {
            rootEntity = "";
        }
        if (rootEntity.length() > 0) {
            if (rootEntity.lastIndexOf(File.separator) != rootEntity.length() - 1) {
                rootEntity += File.separator;
            }
            //zip.putNextEntry(new ZipEntry(rootEntity));
        }

        //zip.putNextEntry(new ZipEntry(kaart.getNaam()+"/"+xmlBestandsnaam));
        if (dirFile.exists()) {
            if (dirFile.isDirectory()) {
                File[] files = dirFile.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        putDirInZip(zip, files[i], rootEntity + files[i].getName() + File.separator);
                    } else {
                        //zorg dat hij zichzelf niet schrijft.
                        if (!files[i].getName().equalsIgnoreCase(ZIPNAME)) {
                            InputStream is =null;
                            is = new FileInputStream(files[i]);
                            
                            zip.putNextEntry(new ZipEntry(rootEntity + files[i].getName()));

                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                zip.write(buffer, 0, len);
                            }
                            zip.closeEntry();
                            is.close();
                            files[i].delete();

                        }

                    }
                }
                dirFile.delete();
            } else {
                throw new Exception("File is not a directory");
            }
        }
        zip.closeEntry();
    }

    private static synchronized String nextThreadName() {
        return ("DownloadKaartThread-" + threadCounter++);
    }

    private String createValidFileName(String name) {
        if (name == null) {
            name = "";
        }
        name = name.replaceAll("[:\\\\/*?|<>\"\']", "");
        if ("".equals(name)) {
            name = "unknown";
        }
        if (name.length() > maxFileNameLength) {
            name = name.substring(0, maxFileNameLength);
        }
        name=name.trim();
        return name;
    }

    /**
     * create a unique file
     */
    private File createUniqueFile(String path, int count) throws Exception {
        if (count == 100) {
            throw new Exception("Can not create unique File/Directory: " + path);
        }
        File workingDir;
        if (count != 0) {
            if (path.endsWith(File.separator)) {
                String newFileString=path.substring(0, path.length() - 2) + "_" + count + File.separator;
                workingDir = new File(newFileString);
            }else{
                workingDir = new File(path + "_" + count);
            }
        } else {
            workingDir = new File(path);
        }
        if (workingDir.exists()) {
            count++;
            return createUniqueFile(path, count);
        }
        return workingDir;
    }

    /**
     */
    private File createUniqueFile(String path) throws Exception {
        return createUniqueFile(path, 0);
    }

    public org.geotools.xml.Configuration getCorrectConfiguration(WFSDataStore ds, String outputFormat) {
        org.geotools.xml.Configuration gmlconfig;
        String version="1.0.0";
        if (ds instanceof WFS_1_1_0_DataStore){
            version="1.1.0";
        }
        if (version.equalsIgnoreCase(OGCConstants.WFS_VERSION_100) || (outputFormat != null && (outputFormat.toLowerCase().indexOf("gml/2") >= 0 || outputFormat.equalsIgnoreCase("gml2")))) {
            gmlconfig = new org.geotools.gml2.GMLConfiguration();
        } else {
            gmlconfig = new org.geotools.gml3.GMLConfiguration();
        }
        return gmlconfig;
    }

    private String getServiceException(File gmlFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        Writer writer = new StringWriter();
        FileInputStream fis = new FileInputStream(gmlFile);
        try{
            char[] buffer = new char[SERVICE_EXCEPTION_TEST_LENGTH];
            Reader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            int n=reader.read(buffer);
            writer.write(buffer, 0, n);
        }finally{
            fis.close();
        }
        String begin=writer.toString();
        if (begin.indexOf("<ServiceExceptionReport")>0 && begin.indexOf("<ServiceExceptionReport")< 1000){
            return begin;
        }else{
            return null;
        }

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFormaat() {
        return formaat;
    }

    public void setFormaat(String formaat) {
        this.formaat = formaat;
    }

    public String[] getUuids() {
        return uuids;
    }

    public void setUuids(String[] uuids) {
        this.uuids = uuids;
    }

    public Integer getThreadStatus() {
        return threadStatus;
    }
}