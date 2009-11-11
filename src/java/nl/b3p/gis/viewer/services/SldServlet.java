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
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.db.Themas;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
            Themas th = getThemas(id);
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
            
            featureType = featureType.substring(featureType.indexOf("_")+1);
            String geometryType=null;
            try{
                geometryType=getGeomtryType(th);
            }catch(Exception e){
                log.error("Error getting geometry type: ",e);
            }
            Node child = createNamedLayerConstraint(doc, featureType, sldattribuut, visibleValue,geometryType);
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

	private Themas getThemas(String id) throws Exception {
		Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = sess.beginTransaction();
        Themas t = null;
		try {
			t = SpatialUtil.getThema(id);

            tx.commit();
		} catch(Exception e) {
            if(tx.isActive()) {
				tx.rollback();
			}
            log.error("Exception occured, rollback", e);

            throw e;
		}
		return t;
	}

    private String getGeomtryType(Themas t) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = sess.beginTransaction();
        String geometryType=null;
        try {
            if (Connecties.TYPE_JDBC.equals(t.getConnectie().getType())) {
                geometryType=SpatialUtil.getThemaGeomType(t, t.getConnectie().getJdbcConnection());
            }else{
                throw new UnsupportedOperationException("Not supported for other then JDBC themas");
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            log.error("Exception occured, rollback", e);

            throw e;
        }
        return geometryType;
    }

    public Document getDefaultSld() throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream fi = new FileInputStream(defaultSldPath);
        Document doc = db.parse(fi);
        return doc;
    }

    public Node createNamedLayerConstraint(Document doc, String featureName, String attribute, String[] value, String geometryType) {
        Node featureTypeConstraint = createFeatureTypeConstraint(doc, attribute, value,geometryType);

        Node userStyle = doc.createElementNS(SLDNS, "UserStyle");
        userStyle.appendChild(featureTypeConstraint);

        Node name = doc.createElementNS(SLDNS, "Name");
        name.appendChild(doc.createTextNode(featureName));

        Node namedLayer = doc.createElementNS(SLDNS, "NamedLayer");
        namedLayer.appendChild(name);
        namedLayer.appendChild(userStyle);

        return namedLayer;
    }

    public Node createFeatureTypeConstraint(Document doc, String attribute, String[] value,String geometryType) {

        Node featureTypeStyle = doc.createElementNS(SLDNS, "FeatureTypeStyle");
        Node rule = doc.createElement( "Rule");


        Node filter = doc.createElementNS(OGCNS, "Filter");

        Node temp;

        if (value.length == 1) {
            temp = filter;
        } else {
            Node oRFilter = doc.createElementNS(OGCNS, "Or");
            filter.appendChild(oRFilter);
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

        rule.appendChild(filter);
        if (geometryType==null || geometryType.toLowerCase().indexOf("polygon")>=0)
            rule.appendChild(createStylePolygon(doc, attribute));
        else if (geometryType==null || geometryType.toLowerCase().indexOf("line")>=0)
            rule.appendChild(createStyleLine(doc, attribute));
        else if (geometryType==null || geometryType.toLowerCase().indexOf("point")>=0)
            rule.appendChild(createStylePoint(doc, attribute));
                
        featureTypeStyle.appendChild(rule);
        return featureTypeStyle;
    }

    private Node createStylePoint(Document doc, String geoProperty){


        Node pointSymbolizer = doc.createElement("PointSymbolizer");
        Node geo = doc.createElement("Geometry");
        Node propName = doc.createElementNS(OGCNS, "PropertyName");
        propName.appendChild(doc.createTextNode(geoProperty));
        geo.appendChild(propName);
        Node graphic = doc.createElement("Graphic");
        Node mark = doc.createElement("Mark");
        Node wkn = doc.createElement("WellKnownName");
        wkn.appendChild(doc.createTextNode("circle"));
        Node fill = doc.createElement("Fill");
        Element cssParam = doc.createElement("CssParameter");
        cssParam.setAttribute("name", "fill");
        cssParam.appendChild(doc.createTextNode("#ff0000"));
        Node size = doc.createElement("Size");
        size.appendChild(doc.createTextNode("10.0"));
        
        fill.appendChild(cssParam);
        mark.appendChild(wkn);
        mark.appendChild(fill);

        graphic.appendChild(mark);
        graphic.appendChild(size);

        pointSymbolizer.appendChild(geo);
        pointSymbolizer.appendChild(graphic);

        return pointSymbolizer;
    }

    private Node createStylePolygon(Document doc, String geoProperty){


        Node polygonSymbolizer = doc.createElement("PolygonSymbolizer");
        Node geo = doc.createElement("Geometry");
        Node propName = doc.createElementNS(OGCNS, "PropertyName");
        propName.appendChild(doc.createTextNode(geoProperty));
        geo.appendChild(propName);

        Node fill = doc.createElement("Fill");
        Element cssParam2 = doc.createElement("CssParameter");
        cssParam2.setAttribute("name", "fill");
        cssParam2.appendChild(doc.createTextNode("#ff0000"));
        fill.appendChild(cssParam2);

        Node stroke = doc.createElement("Stroke");
        Element cssParam = doc.createElement("CssParameter");
        cssParam.setAttribute("name", "stroke");
        cssParam.appendChild(doc.createTextNode("#ff00ff"));
        stroke.appendChild(cssParam);
         
        polygonSymbolizer.appendChild(geo);
        polygonSymbolizer.appendChild(stroke);
        polygonSymbolizer.appendChild(fill);

        return polygonSymbolizer;
    }

    private Node createStyleLine(Document doc, String geoProperty){
   

        Node lineSymbolizer = doc.createElement("LineSymbolizer");
        Node geo = doc.createElement("Geometry");
        Node propName = doc.createElementNS(OGCNS, "PropertyName");
        propName.appendChild(doc.createTextNode(geoProperty));
        geo.appendChild(propName);

        Node stroke = doc.createElement("Stroke");

        Element cssStroke = doc.createElement("CssParameter");
        cssStroke.setAttribute("name", "stroke");
        cssStroke.appendChild(doc.createTextNode("#ff00ff"));
        stroke.appendChild(cssStroke);

        Element cssDash = doc.createElement("CssParameter");
        cssDash.setAttribute("name", "stroke-dasharray");
        cssDash.appendChild(doc.createTextNode("10.0 5 5 10"));
        stroke.appendChild(cssDash);

       // Node graphicFill = doc.createElement("GraphicFill");
        //graphicFill.appendChild(graphic);
        lineSymbolizer.appendChild(geo);
        lineSymbolizer.appendChild(stroke);

        return lineSymbolizer;
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
