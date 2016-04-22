package nl.b3p.gis.viewer.downloads;

import com.vividsolutions.jts.geom.Geometry;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import nl.b3p.commons.services.SizeLimitedOutputStream;
import nl.b3p.commons.services.StreamCopy;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.ThemaData;
import nl.b3p.gis.viewer.services.DownloadServlet;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.writers.StreamingShapeWriter;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.wfs.v1_0_0.WFS_1_0_0_DataStore;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;

/**
 * @author Boy
*/
public class DownloadThread extends Thread {
    
    private static final Log log = LogFactory.getLog(DownloadThread.class);

    private static final String FORMAAT_GML = "GML";
    private static final String FORMAAT_SHP = "SHP";
    
    private static final int BUFFER_SIZE = 8192;
    private static final String ISO_CHARSET = "ISO-8859-1";
    private static final String UTF8_CHARSET = "UTF-8";
    
    private static String ZIPNAME;
    private static final String EXTENSION = ".zip";
    private static final String GML_EXTENSION = ".gml";

    public static final int STATUS_NEW = 0;
    public static final int STATUS_STARTED = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_ERROR = 3;

    private static int threadCounter = 1;
    private String email;
    private String[] uuids;
    private String wkt;
    private String formaat;
    private Integer threadStatus;
    static final Logging logging = Logging.ALL;

    private String applicationPath;
    private Bron kaartenbalieBron = null;
    
    private final long MAX_ZIP_FILESIZE = 1024L * 1024L * 100; // 100MB
    
    public static final int MAIL_TYPE_SUCCES = 1;
    public static final int MAIL_TYPE_TOO_LARGE = 2;
    public static final int MAIL_TYPE_ERROR = 99;
    
    private boolean stop = false;

    /**
     * Creates a new instance of DownloadThread
     */
    public DownloadThread(ThreadGroup tg) {
        super(tg, nextThreadName());
        threadStatus = STATUS_NEW;
    }

    @Override
    public void run() {
        if (!stop) {
            threadStatus = STATUS_STARTED;
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
                    boolean error = false;

                    Integer gegevensbronId = new Integer(uuid);
                    Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, gegevensbronId);

                    String title = gb.getNaam();                
                    log.debug("STARTED DownloadThread voor gegevensbron: " + title);

                    try {
                        if (getFormaat().equals(FORMAAT_SHP)) {
                            writeShapesToWorkingDir(workingDir, gb);
                        }  

                        if (getFormaat().equals(FORMAAT_GML)) {
                            writeGMLToWorkingDir(workingDir, title, gb);
                        } 

                    } catch (Exception e) {
                        error = true;
                        log.debug("Dataset opgehaald met fouten: ", e);
                    }

                    if (error) {                    
                        log.debug("Error while getting data for uuid: "+ uuid);
                        erroredTitles.add(title);
                    } else {                    
                        successTitles.add(title);
                    }

                }

                if (uuids.length - erroredTitles.size() > 0) {
                    ZipOutputStream zip = null;
                    FileOutputStream fos = null;                
                    SizeLimitedOutputStream limitOut = null;

                    try {
                        fos = new FileOutputStream(zipFile);
                        limitOut = new SizeLimitedOutputStream(fos, MAX_ZIP_FILESIZE);                    
                        zip = new ZipOutputStream(limitOut);

                        putDirInZip(zip, new File(workingDir.getAbsolutePath()), "");                    
                    } catch (Exception ex) {                    
                        throw new Exception("Error creating zip: ", ex);
                    } finally {
                        try {
                            if (zip != null) {
                                zip.close();
                            }
                            if (limitOut != null) {
                                limitOut.close();
                            }
                            if (fos != null) {
                                fos.close();
                            }   
                        } catch (Exception e) {
                            log.error("Cannot close zip.", e);
                        }                                  
                    }
                }

                String downloadLink = folder + File.separator + zipFileName;            
                sendEmail(zipFile, downloadLink, erroredTitles, successTitles, MAIL_TYPE_SUCCES);

                threadStatus = STATUS_FINISHED;
            } catch (Exception e) {       
                threadStatus = STATUS_ERROR;
                log.error("Error downloading the data: ", e);

                sendErrorEmail();
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

            log.debug("ENDED DownloadThread.");          
        } // end if !stop     
    }

    private SimpleFeature cleanupFeature(SimpleFeature feature, Gegevensbron gb) {
        if (feature == null) {
            return feature;
        }

        Map<Object, Object> ud = feature.getUserData();
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(feature.getFeatureType());

        List<AttributeDescriptor> oldAttributeDescriptors = feature.getFeatureType().getAttributeDescriptors();
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(oldAttributeDescriptors);
        List<Object> attributes = feature.getAttributes();
        
        Set themadata = gb.getThemaData();
        for (int i = 0; i < attributeDescriptors.size(); i++) { // of oldAttributeDescriptors?
            int attributeID = i;
            Iterator it = themadata.iterator();
            while (it.hasNext()) {
                ThemaData td = (ThemaData) it.next();
                if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(td.getKolomnaam())) {
                    attributeID = -1;
                    break;
                }
            }
            if (attributeID >= 0) {
                attributeDescriptors.remove(attributeID);
                attributes.remove(attributeID);
            }
        }
 
        featureTypeBuilder.setAttributes(attributeDescriptors);
        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());
        feature = simpleFeatureBuilder.buildFeature(feature.getID(), attributes.toArray(new Object[attributes.size()]));
        feature.getUserData().putAll(ud);

        return feature;
    }
    
    private void writeShapesToWorkingDir(File workingDir, Gegevensbron gb)
            throws Exception {

        Bron bron = getCorrectBron(gb);

        if (bron != null) {
            DataStore datastore = bron.toDatastore();

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
                        
                        //strip ongeconfigureerde attributen van feature
                        feature = cleanupFeature(feature, gb);
                        
                        ssw.write( feature);
                    }
                }

            } catch (Exception ex) {
                log.error("Fout tijdens schrijven shape: ", ex);

            } finally {
                if (fc != null) {
                    it.close();
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
            DataStore datastore = bron.toDatastore();

            FeatureCollection fc = null;
            FeatureIterator it = null;
            FileOutputStream out = null;

            String gmlFile = workingDir + File.separator + fileName + GML_EXTENSION;
            
            try {
                fc = getFeatureCollection(datastore, gb);

                if (fc != null && fc.size() > 0) {
                    //helaas in geheugen omdat features aangepast moeten worden
                    DefaultFeatureCollection outputFeatureCollection 
                            = new DefaultFeatureCollection();
            
                    out = new FileOutputStream(gmlFile);
                    it = fc.features();
                    while (it.hasNext()) {
                        SimpleFeature feature = (SimpleFeature) it.next();

                        //strip ongeconfigureerde attributen van feature
                        feature = cleanupFeature(feature, gb);
                        outputFeatureCollection.add(feature);
                    }

                    org.geotools.xml.Configuration configuration = new org.geotools.gml3.GMLConfiguration();
                    org.geotools.xml.Encoder encoder = new org.geotools.xml.Encoder(configuration);
                    encoder.encode(outputFeatureCollection, org.geotools.gml3.GML._FeatureCollection, out);
                }
            } catch (Exception ex) {
                log.error("Fout tijdens schrijven gml: ", ex);

            } finally {
                if (fc != null) {
                    it.close();
                }
                if (datastore != null) {
                    datastore.dispose();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private void sendErrorEmail() {
        sendEmail("", "", null, null, MAIL_TYPE_ERROR);
    }
    
    private void sendEmail(String zipFile, String zipFileName, ArrayList<String> erroredTitles,
            ArrayList<String> successTitles, int mailType) {
        
        File zip = new File(zipFile);
        
        try {        
            java.util.Properties properties = System.getProperties();

            /* Set host */
            String smtpHost = DownloadServlet.getSmtpHost();
            properties.put("mail.smtp.host", smtpHost);

            javax.mail.Session mailSession = javax.mail.Session.getInstance(properties, null);
            mailSession.setDebug(false);

            /* From */
            MimeMessage msg = new MimeMessage(mailSession);
            String contactEmail = DownloadServlet.getContactEmail();
            if (contactEmail == null) {
                throw new Exception("Instelling contact e-mail is verplicht.");
            }
            
            Address frmAddress = new InternetAddress(contactEmail);
            msg.setFrom(frmAddress);

            /* To */
            Address[] toAddresses = InternetAddress.parse(getEmail());
            msg.setRecipients(Message.RecipientType.TO, toAddresses);

            /* Subject */
            String onderwerp = null;

            if (mailType == MAIL_TYPE_SUCCES) {
                onderwerp = "Het downloadbestand staat klaar.";
            } else if (mailType == MAIL_TYPE_TOO_LARGE) {
                onderwerp = "Het downloadbestand is te groot.";
            } else {
                onderwerp = "Fout tijdens klaarzetten van het downloadbestand.";
            }
            msg.setSubject(onderwerp);

            /* Get mail text from file */
            String file = null;
            if (mailType == MAIL_TYPE_SUCCES) {
                file = DownloadServlet.getMailDownloadSucces();
            } else {
                file = DownloadServlet.getMailDownloadError();
            }

            /* Object params */
            Object[] params = null;
            if (mailType == MAIL_TYPE_SUCCES && zip.exists()) {
                String link = getApplicationPath() + DownloadServlet.getDownloadServletPath()
                        + "?download=" + zipFileName;

                String lagenString = getLagenString(successTitles, erroredTitles);

                params = new Object[] {
                    link, // 0 = download link
                    lagenString, // 1 = lagen string
                    contactEmail // 2 = contact email
                };
            } else if (mailType == MAIL_TYPE_TOO_LARGE) {
                params = new Object[] {                
                    contactEmail // 0 = contact email
                };
            } else {
                params = new Object[] {                
                    contactEmail // 0 = contact email
                };
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();            
            InputStream in = null;
            in = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
            StreamCopy.copy(in, out);
            MessageFormat textMessage = new MessageFormat(out.toString(ISO_CHARSET), Locale.ENGLISH);   

            /* Content */
            String tekst = textMessage.format(params);        
            msg.setContent(tekst, "text/plain");

            /* Date */
            msg.setSentDate(new Date());
            
            /* Send it */
            Transport.send(msg);
        
        } catch (AddressException ae) {
            log.error("Fout bij het opgegeven emailadres. ", ae);
        } catch (SendFailedException sfe) {
            log.error("Fout bij het verzenden van het bericht.", sfe);
        } catch (MessagingException me) {
            log.error("Fout bij het verzenden van het bericht. ", me);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }
    
    private String getLagenString(ArrayList<String> successTitles, 
            ArrayList<String> erroredTitles) {
        
        String laagStatus = "";
        if (successTitles.size() > 0){
            laagStatus = "De volgende kaart(en) zijn klaar gezet ("+successTitles.size()+"): \n";
            for (String title : successTitles){
                laagStatus += "- " + title + "\n";
            }
        }
        
        if (erroredTitles.size() > 0) {
            if (laagStatus.length() < 1) {
                laagStatus = "De volgende kaart(en) is/zijn tijdelijk niet compleet of beschikbaar ("+erroredTitles.size()+"): \n";
            } else {
                laagStatus += "\nDe volgende kaart(en) is/zijn tijdelijk niet compleet of beschikbaar ("+erroredTitles.size()+"): \n";
            }            
            for (String title : erroredTitles) {
                laagStatus += "- " + title + "\n";
            }
        }
        
        return laagStatus;
    }

    private void putDirInZip(ZipOutputStream zip, File dirFile, String rootEntity)
            throws IOException, Exception {
        
        byte[] buffer = new byte[BUFFER_SIZE];
        
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
                            InputStream is = null;
                            is = new FileInputStream(files[i]);
                            
                            zip.putNextEntry(new ZipEntry(rootEntity + files[i].getName()));

                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                zip.write(buffer, 0, len);
                            }
                            
                            zip.closeEntry();
                            is.close();

                            /* Delete file */
                            log.debug("Deleting " + files[i].toString());
                            files[i].delete();
                        }
                    }
                }
                
                //dirFile.delete();
            } else {
                throw new Exception("File is not a directory");
            }
        }

        zip.closeEntry();
    }
    
    public void stopThread() {
        stop = true;
    }

    private static synchronized String nextThreadName() {
        return ("DownloadKaartThread-" + threadCounter++);
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
        
        /* Geometry filter meegeven ? */
        Geometry geom = null;
        String wktString = getWkt();
        if (wktString != null && !wktString.equals("")) {
            geom = DataStoreUtil.createGeomFromWKTString(wktString);
        }
        
        /* geom filter toevoegen */
        Filter geomFilter = null;
        if (geom != null) {
            geomFilter = DataStoreUtil.createIntersectFilter(gb, datastore, geom);
        }    

        if (datastore instanceof WFS_1_0_0_DataStore) {
            List<String> propnames = new ArrayList<String>();
            propnames.add(gb.getAdmin_pk());

            fc = DataStoreUtil.getFeatureCollection(datastore, gb, geomFilter, propnames, null, true);

        } else if (datastore instanceof WFS_1_1_0_DataStore) {
            List<String> propnames = new ArrayList<String>();
            propnames.add(gb.getAdmin_pk());

            fc = DataStoreUtil.getFeatureCollection(datastore, gb, geomFilter, propnames, null, true);

        } else { // JDBC Bron
            String typeName = gb.getAdmin_tabel();
            FeatureSource<SimpleFeatureType, SimpleFeature> fs = datastore.getFeatureSource(typeName);
            
            if (geomFilter != null)
                fc = fs.getFeatures(geomFilter);
            else
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

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }
}