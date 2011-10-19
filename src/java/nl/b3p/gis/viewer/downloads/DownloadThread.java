package nl.b3p.gis.viewer.downloads;

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
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.services.DownloadServlet;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.writers.StreamingShapeWriter;
import nl.b3p.ogc.utils.OGCConstants;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureStore;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.v1_0_0.WFS_1_0_0_DataStore;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Boy
*/
public class DownloadThread extends Thread {
    
    private static final Log log = LogFactory.getLog(DownloadThread.class);

    private static final String FORMAAT_GML = "GML";
    private static final String FORMAAT_SHP = "SHP";

    private static final int maxFileNameLength = 50;
    private static String ZIPNAME;
    private static final String EXTENSION = ".zip";
    private static final String GML_EXTENSION = ".gml";

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

    private String applicationPath;
    private Bron kaartenbalieBron = null;

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
            
            String downloadPath = DownloadServlet.getDownloadPath() + File.separator;

            /* Create unique folder */
            String folder = DownloadServlet.uniqueName("");
            String folderName = downloadPath + folder;
            File workingDir = new File(folderName);
            if (!workingDir.mkdirs()) {
                throw new IOException("Cannot create folder: " + workingDir.getAbsolutePath());
            }

            /* Pad naar zipfile */
            String zipFileName = folder + EXTENSION;
            ZIPNAME = zipFileName;

            String zipFile = folderName + File.separator + zipFileName;

            ArrayList<String> erroredTitles = new ArrayList<String>();
            ArrayList<String> successTitles = new ArrayList<String>();

            for (String uuid : uuids) { 
                String error = null;

                Integer gegevensbronId = new Integer(uuid);
                Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gegevensbronId);

                String title = gb.getNaam();

                try {
                    if (getFormaat().equals(FORMAAT_SHP))
                        writeShapesToWorkingDir(workingDir, gb);

                    if (getFormaat().equals(FORMAAT_GML))
                        writeGMLToWorkingDir(workingDir, title, gb);
                    
                } catch (Exception e) {
                    log.debug("Dataset opgehaald met fouten: ", e);
                    error = "Dataset opgehaald met fouten: \n" + e.toString();
                    if (e.getCause()!=null){
                        error+="\nReden:\n"+e.getCause().toString();
                    }
                }

                if (error != null) {                    
                    log.debug("error while getting data for uuid "+uuid+": "+error);
                    erroredTitles.add(title);
                } else {                    
                    successTitles.add(title);
                }

            }            
            
            //we have downloaded some datasetes. HURRAY! Zip and ship(later)
            if (uuids.length - erroredTitles.size() > 0) {
                ZipOutputStream zip = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(zipFile);
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

            String downloadLink = folder + File.separator + zipFileName;

            sendEmail(zipFile, downloadLink, erroredTitles, successTitles);
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

    private void writeShapesToWorkingDir(File workingDir, Gegevensbron gb)
            throws Exception {

        Bron bron = getCorrectBron(gb);

        if (bron != null) {
            DataStore datastore = datastore = bron.toDatastore();

            FeatureCollection fc = null;
            FeatureIterator it = null;
            StreamingShapeWriter ssw = null;

            try {
                fc = getFeatureCollection(datastore, gb);

                if (fc != null && fc.size() > 0) {
                    it = fc.features();

                    ssw = new StreamingShapeWriter(workingDir.getAbsolutePath() + File.separator);

                    while (it.hasNext()) {
                        SimpleFeature feature = (SimpleFeature) it.next();
                        ssw.write((SimpleFeature) feature);
                    }
                }

            } catch (Exception ex) {
                log.error("Fout tijdens schrijven shape: ", ex);

            } finally {
                if (fc != null) {
                    fc.close(it);
                }
                if (datastore != null) {
                    datastore.dispose();
                }
                if (ssw != null) {
                    ssw.close();
                }
            }
        }
    }

    private void writeGMLToWorkingDir(File workingDir, String fileName, Gegevensbron gb)
            throws Exception {

        Bron bron = getCorrectBron(gb);

        if (bron != null) {
            DataStore datastore = datastore = bron.toDatastore();

            FeatureCollection fc = null;
            FeatureIterator it = null;
            FileOutputStream out = null;
            FileOutputStream out2 = null;

            try {
                fc = getFeatureCollection(datastore, gb);

                if (fc != null && fc.size() > 0) {
                    it = fc.features();

                    /* Ook xsd file maken voor gebruik als schema in gml ? */

                    /*
                    SimpleFeatureType ft = DataStoreUtil.getSchema(datastore, gb);

                    String xsdFile = workingDir + File.separator + fileName + ".xsd";
                    out2 = new FileOutputStream(xsdFile);

                    org.geotools.xml.Configuration configuration1 = new org.geotools.gml3.GMLConfiguration();
                    org.geotools.xml.Encoder encoder1 = new org.geotools.xml.Encoder(configuration1);

                    //URL locationURL = locationFile.toURI().toURL();
                    //URL baseURL = locationFile.getParentFile().toURI().toURL();
                    //encode.setBaseURL(baseURL);
                    //encode.setNamespace("location", locationURL.toExternalForm());
                    
                    encoder1.encode(ft, org.geotools.gml3.GML._FeatureCollection, out2);
                    */

                    String gmlFile = workingDir + File.separator + fileName + GML_EXTENSION;
                    out = new FileOutputStream(gmlFile);

                    org.geotools.xml.Configuration configuration = new org.geotools.gml3.GMLConfiguration();
                    org.geotools.xml.Encoder encoder = new org.geotools.xml.Encoder(configuration);

                    encoder.encode(fc, org.geotools.gml3.GML._FeatureCollection, out);
                }

            } catch (Exception ex) {
                log.error("Fout tijdens schrijven gml: ", ex);

            } finally {
                if (fc != null) {
                    fc.close(it);
                }
                if (datastore != null) {
                    datastore.dispose();
                }
                if (out != null) {
                    out.close();
                }
                if (out2 != null) {
                    out2.close();
                }
            }
        }
    }

    private void sendEmail(String zipFile, String zipFileName, ArrayList<String> erroredTitles, ArrayList<String> successTitles) {
        //String downloadPath = DownloadServlet.getDownloadPath() + File.separator;
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

                String link = getApplicationPath() + DownloadServlet.getDownloadServletPath() + "?download=" + zipFileName;
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
        }

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

                            /* Delete file */
                            files[i].delete();
                        }
                    }
                }

                /* Delete folder */
                dirFile.delete();
            } else {
                throw new Exception("File is not a directory");
            }
        }

        //zip.closeEntry();
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

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public Bron getKaartenbalieBron() {
        return kaartenbalieBron;
    }

    public void setKaartenbalieBron(Bron kaartenbalieBron) {
        this.kaartenbalieBron = kaartenbalieBron;
    }

    private FeatureCollection getFeatureCollection(DataStore datastore, Gegevensbron gb) throws IOException, Exception {
        FeatureCollection fc = null;

        if (datastore instanceof WFS_1_0_0_DataStore) {
            List<String> propnames = new ArrayList<String>();
            propnames.add(gb.getAdmin_pk());

            fc = DataStoreUtil.getFeatureCollection(datastore, gb, null, propnames, null, true);

        } else if (datastore instanceof WFS_1_1_0_DataStore) {
            List<String> propnames = new ArrayList<String>();
            propnames.add(gb.getAdmin_pk());

            fc = DataStoreUtil.getFeatureCollection(datastore, gb, null, propnames, null, true);

        } else { // JDBC Bron
            String typeName = gb.getAdmin_tabel();
            FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) datastore.getFeatureSource(typeName);
            fc = fs.getFeatures();
        }

        return fc;
    }

    private Bron getCorrectBron(Gegevensbron gb) {
        Bron bron = gb.getBron();

        /* Kaartenbalie bron gebruiken */
        if (bron == null && gb.getAdmin_tabel() != null && !gb.getAdmin_tabel().equals("")) {
            bron = getKaartenbalieBron();
        }

        return bron;
    }
}