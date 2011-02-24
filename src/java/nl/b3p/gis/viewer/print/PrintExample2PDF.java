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
    public void convert2PDF(PrintInfo info, File xslt, File pdf) throws FileNotFoundException, FOPException, TransformerConfigurationException, JAXBException, IOException, TransformerException {

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
}