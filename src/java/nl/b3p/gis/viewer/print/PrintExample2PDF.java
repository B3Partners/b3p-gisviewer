package nl.b3p.gis.viewer.print;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import nl.b3p.wms.capabilities.Layer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetLegendGraphicRequest;

public class PrintExample2PDF {

    private static final Log log = LogFactory.getLog(PrintExample2PDF.class);

    private FopFactory fopFactory = FopFactory.newInstance();
    private String legenduri = "";

    /**
     * Converts object to a PDF file.
     * @param team the ProjectTeam object
     * @param xslt the stylesheet file
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException In case of a FOP problem
     * @throws TransformerException In case of a XSL transformation problem
     */
    public void convertPersoonToPDF(PrintInfo info, File xslt, File pdf) throws FileNotFoundException, FOPException, TransformerConfigurationException, JAXBException, IOException, TransformerException {

        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        // configure foUserAgent as desired

        // Setup output
        FileOutputStream out = new FileOutputStream(pdf);

        try {
            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);
            SAXResult res = new SAXResult(fop.getDefaultHandler());

            // Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = factory.newTransformer(new StreamSource(xslt));

            JAXBContext jc = JAXBContext.newInstance(info.getClass());
            JAXBSource src =  new JAXBSource(jc, info);

            //printSourceXml(src);
            //transformer.setParameter("imageUrl", "http://www.spellenenzo.nl/res/logo.jpg");

            try {
                transformer.setParameter("imageUrl", getImageUrl());
            } catch (Exception ex) {
                log.error("Fout bij maken PDF:" + ex);
            }

            transformer.setParameter("legenduri", legenduri);

            transformer.transform(src, res);

        } finally {
            out.close();
        }
    }

    private void printSourceXml(Source src) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = factory.newTransformer();
        StringWriter w = new StringWriter();
        StreamResult res = new StreamResult(w);
        transformer.transform(src, res);
        System.out.println(w.toString());
    }

    private String getImageUrl() throws IOException, org.geotools.ows.ServiceException {
        String s = "";

        // http://public-wms.kaartenbalie.nl/wms/nederland?service=wms&request=getmap&Layers=wegen,basis_nl,water,gemeenten_2006&transparent=true&VERSION=1.1.1&WIDTH=742&HEIGHT=1000
        try {

            URL uri = new URL("http://public-wms.kaartenbalie.nl/wms/nederland?service=WMS&request=getCapabilities");
            WebMapServer wms = new WebMapServer(uri);
            String version = wms.getCapabilities().getVersion();
            List layers = wms.getCapabilities().getLayerList();

            GetLegendGraphicRequest glg = wms.createGetLegendGraphicRequest();

            String laag = "rivieren_nl";

            for (int i1=0; i1 < layers.size(); i1++) {
                Layer l = (Layer) layers.get(i1);

                //System.out.println(l.getName());

                if (l.getName().equals(laag))
                    glg.setLayer(l.getName());
            }

            glg.setFormat("image/png");

            legenduri = glg.getFinalURL().toString();

            s = "http://public-wms.kaartenbalie.nl/wms/nederland?service=wms&request=getmap&Layers=basis,basis_nl,"+laag+"&VERSION=1.1.1&WIDTH=385&HEIGHT=468";

        } catch (MalformedURLException ex) {
            log.error("Fout bij maken PDF:" + ex);
        }

        return s;
    }
    
}