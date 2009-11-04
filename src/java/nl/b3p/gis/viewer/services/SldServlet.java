package nl.b3p.gis.viewer.services;

import java.io.FileInputStream;
import javax.xml.transform.Transformer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import nl.b3p.gis.viewer.db.Themas;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author Roy
 */
public class SldServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(SldServlet.class);
    public static final String OGCNS = "http://www.opengis.net/ogc";
    public static final String SLDNS = "http://www.opengis.net/sld";
    public static final String SENS = "http://www.opengis.net/se";
    private String defaultSldPath;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     *
     * Parameters die meegegeven kunnen worden. Voor de optonele parameters die ontbreken worden de waarden
     * uit de web.xml gebruikt:
     * visibleValue: In de SLD wordt deze waarde gebruikt in de 'LITERAL' voor de vergelijking.
     * attributeName: (optional) de naam van het attribuut waarmee de waarde moet worden vergeleken
     * featureTypes: (optional) de featuretypes waarvoor een sld constraint moet worden gemaakt
     *
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        OutputStream out = response.getOutputStream();
        try {
            String style = null;
            if (request.getParameter("style") != null) {
                style = request.getParameter("style");
            } else {
                style = "NO";
            }

            String not = null;
            if (request.getParameter("notinfilter") != null) {
                not = request.getParameter("notinfilter");
            } else {
                not = "NO";
            }

            String visibleValue[] = null;
            if (request.getParameter("visibleValue") != null) {
                visibleValue = request.getParameter("visibleValue").split(",");
            }
            if (visibleValue == null) {
                throw new Exception("visibleValue is verplicht");
            }

            String id = null;
            if (request.getParameter("id") != null) {
                id = request.getParameter("id");
            }
            if (id == null) {
                throw new Exception("id is verplicht");
            }

            Document doc = getDefaultSld();
            Node root = doc.getDocumentElement();
            Themas th = SpatialUtil.getThema(id);
            if (th == null) {
                throw new Exception("geen thema gevonden");
            }
            String sldattribuut = th.getSldattribuut();
            if (sldattribuut == null || sldattribuut.length() == 0) {
                sldattribuut = th.getAdmin_pk();
            }
            if (sldattribuut == null || sldattribuut.length() == 0) {
                throw new Exception("thema heeft geen sld attribuut");
            }
            String featureType = th.getWms_layers_real();
            if (featureType == null || featureType.length() == 0) {
                throw new Exception("thema heeft geen featuretype");
            }

            Node child = createNamedLayerConstraint(doc, featureType, sldattribuut, visibleValue, style, not);
            root.appendChild(child);

            DOMSource domSource = new DOMSource(doc);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            response.setContentType("text/xml");
            t.transform(domSource, new StreamResult(out));

        } catch (Exception e) {
            log.error("Fout bij maken sld: ", e);
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter pw = new PrintWriter(out);
            pw.write(e.getMessage());
        } finally {
            out.close();
        }
    }

    public Document getDefaultSld() throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream fi = new FileInputStream(defaultSldPath);
        Document doc = db.parse(fi);
        return doc;
    }

    public Node createNamedLayerConstraint(Document doc, String featureName, String attribute, String[] value, String style, String not) {
        Node featureTypeConstraint = createFeatureTypeConstraint(doc, attribute, value, not);

        Node layerFeatureConstraints = doc.createElementNS(SLDNS, "LayerFeatureConstraints");
        layerFeatureConstraints.appendChild(featureTypeConstraint);

        Node name = doc.createElementNS(SLDNS, "Name");
        name.appendChild(doc.createTextNode(featureName));

        Node namedLayer = doc.createElementNS(SLDNS, "NamedLayer");
        namedLayer.appendChild(name);
        namedLayer.appendChild(layerFeatureConstraints);
        if (!style.equalsIgnoreCase("NO")) {
            Node namedStyle = doc.createElementNS(SLDNS, "NamedStyle");
            Node namedStyleName = doc.createElementNS(SLDNS, "Name");
            namedStyleName.appendChild(doc.createTextNode(style));
            namedStyle.appendChild(namedStyleName);
            namedLayer.appendChild(namedStyle);
        }
        return namedLayer;
    }

    public Node createFeatureTypeConstraint(Document doc, String attribute, String[] value, String not) {

        Node featureTypeConstraint = doc.createElementNS(SLDNS, "FeatureTypeConstraint");

        Node filter = doc.createElementNS(OGCNS, "Filter");
        Node temp2;
        if ("true".equals(not)) {
            Node notNode = doc.createElement("Not");
            filter.appendChild(notNode);
            temp2 = notNode;
        } else {
            temp2 = filter;
        }

        Node temp;

        if (value.length == 1) {
            temp = temp2;
        } else {
            Node oRFilter = doc.createElementNS(OGCNS, "Or");
            temp2.appendChild(oRFilter);
            temp = oRFilter;

        }

        for (int i = 0; i < value.length; i++) {
            Node propertyName = doc.createElementNS(OGCNS, "PropertyName");
            propertyName.appendChild(doc.createTextNode(attribute));
            Node literal = doc.createElementNS(OGCNS, "Literal");
            literal.appendChild(doc.createTextNode(value[i]));

            Node propertyIsEqualTo = doc.createElementNS(OGCNS, "PropertyIsEqualTo");
            propertyIsEqualTo.appendChild(propertyName);
            propertyIsEqualTo.appendChild(literal);

            temp.appendChild(propertyIsEqualTo);
        }

        featureTypeConstraint.appendChild(filter);
        return featureTypeConstraint;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            if (config.getInitParameter("defaultSld") != null) {
                defaultSldPath = getServletContext().getRealPath(config.getInitParameter("defaultSld"));
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
