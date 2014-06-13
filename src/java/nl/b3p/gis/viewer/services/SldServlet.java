package nl.b3p.gis.viewer.services;

import java.io.FileInputStream;
import javax.xml.transform.Transformer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.viewer.db.Gegevensbron;
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
    public static final String SLDTYPE_NAMEDSTYLE= "NamedStyle";
    public static final String SLDTYPE_USERSTYLE = "UserStyle";
    public static final String DEFAULT_STYLE= "default";
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
     * visibleValue: In de SLD wordt deze waarde gebruikt in de 'LITERAL' voor de vergelijking. (comme gescheiden)
     * id: Gaat vervallen: gebruik themaId (comme gescheiden)
     * themaId(optioneel als clusterId is gegeven): de id's van de themas waarvoor een sld gemaakt moet worden. (comme gescheiden)
     * clusterId(optioneel als themaId is gegeven): de id's van de clusters waarvoor een sld gemaakt moet worden. (comme gescheiden)(alle thema's in een cluster)
     * sldType(o): Het type NamedLayer SLD (Ondersteund: UserStyle (default/voor highlight), LayerFeatureConstraints)
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        OutputStream out = response.getOutputStream();
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = sess.beginTransaction();
        try {            
            String url = request.getRequestURL().toString();
            log.debug("SLD Url: " + url);
            
            String[] visibleValue = null;
            String themaId = null;            
            
            // rest params ?
            if (FormUtils.nullIfEmpty(request.getParameter("propvalue")) != null) {
                visibleValue = new String[1];
                visibleValue[0] = request.getParameter("propvalue");
            }
            
            // get parameters            
            if (request.getParameter("visibleValue") != null) {
                visibleValue = request.getParameter("visibleValue").split(",");
            }
            if (visibleValue == null) {
                throw new Exception("visibleValue is verplicht");
            }
            
            //get themaId and/or clusterId            
            String clusterId = null;
            if (FormUtils.nullIfEmpty(request.getParameter("id")) != null) {
                themaId = request.getParameter("id");
            }
            
            if (FormUtils.nullIfEmpty(request.getParameter("themaId")) != null) {
                themaId = request.getParameter("themaId");
            }
            if (FormUtils.nullIfEmpty(request.getParameter("clusterId")) != null) {
                clusterId = request.getParameter("clusterId");
            }
            if (themaId == null && clusterId == null) {
                throw new Exception("id of clusterId is verplicht");
            }
            //get sldtype default: SLDTYPE_USERSTYLE
            String sldType = SLDTYPE_USERSTYLE;
            if (FormUtils.nullIfEmpty(request.getParameter("sldType")) != null) {
                sldType = request.getParameter("sldType");
            }
            
            //create root doc
            Document doc = getDefaultSld();
            Node root = doc.getDocumentElement();
            //create thema list
            List<Themas> themaList = new ArrayList();
            if (themaId != null) {
                String[] themaIds=themaId.split(",");
                for (int i=0;i < themaIds.length; i++){
                    Themas th = SpatialUtil.getThema(themaIds[i]);
                    if (th == null) {
                        log.error("Can't find thema with id: " + themaId);
                    } else {
                        themaList.add(th);
                    }
                }
            }
            //get the list of themas in the cluster
            if (clusterId != null) {
                String[] clusterIds= clusterId.split(",");
                for (int i=0; i < clusterIds.length; i++){
                    try {
                        Integer cid = Integer.parseInt(clusterIds[i]);
                        List<Themas> clusterThemaList = getClusterThemas(cid);
                        if (clusterThemaList == null || clusterThemaList.isEmpty()) {
                            log.warn("No cluster or no themas in cluster: " + clusterIds[i]);
                        } else {
                            themaList.addAll(clusterThemaList);
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("clusterId is NAN: " + clusterIds[i]);
                    }
                }
            }

            for (int i = 0; i < themaList.size(); i++) {
                Themas th = themaList.get(i);

                Gegevensbron gb = th.getGegevensbron();

                String sldattribuut = th.getSldattribuut();
                if (sldattribuut == null || sldattribuut.length() == 0) {
                    sldattribuut = DataStoreUtil.convertFullnameToQName(gb.getAdmin_pk()).getLocalPart();
                }
                if (sldattribuut == null || sldattribuut.length() == 0) {
                    log.debug("thema heeft geen sld attribuut");
                    continue;
                }
                String featureType = th.getWms_layers_real();
                if (featureType == null || featureType.length() == 0) {
                    log.debug("thema heeft geen featuretype");
                    continue;
                }

                featureType = featureType.substring(featureType.indexOf("_") + 1);
                String geometryType = null;
                try {
                    geometryType = getGeomtryType(th, request);
                } catch (Exception e) {
                    log.debug("Error getting geometry type. Creating the style with a polygonsimbolizer.");
                }
                
                if (FormUtils.nullIfEmpty(request.getParameter("propname")) != null) {
                    sldattribuut = request.getParameter("propname");
                }
                
                Node child = createNamedLayer(doc, featureType, sldattribuut, visibleValue, geometryType, sldType);
                root.appendChild(child);
            }
            DOMSource domSource = new DOMSource(doc);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            response.setContentType("text/xml");
            t.transform(domSource, new StreamResult(out));

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            log.error("Fout bij maken sld: ", e);
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter pw = new PrintWriter(out);
            pw.write(e.getMessage());
        } finally {
            out.close();
        }
    }

    private List<Themas> getClusterThemas(Integer id) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        return sess.createQuery("from Themas where cluster=:p").setInteger("p", id).list();
    }

    private String getGeomtryType(Themas t, HttpServletRequest request) throws Exception {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        String geometryType = DataStoreUtil.getThemaGeomType(t, user);
        return geometryType;
    }

    public Document getDefaultSld() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        FileInputStream fi = new FileInputStream(defaultSldPath);
        Document doc = db.parse(fi);
        return doc;
    }
    public Node createNamedLayer(Document doc, String featureName, String attribute, String[] value, String geometryType, String sldType) {
        return createNamedLayer(doc,featureName,attribute,value,geometryType,sldType,false);
    }
    public Node createNamedLayer(Document doc, String featureName, String attribute, String[] value, String geometryType, String sldType,boolean not) {
        Node name = doc.createElementNS(SLDNS, "Name");
        name.appendChild(doc.createTextNode(featureName));
        Node namedLayer = doc.createElementNS(SLDNS, "NamedLayer");
        namedLayer.appendChild(name);

        if (sldType.equalsIgnoreCase(SLDTYPE_USERSTYLE)) {
            Node featureTypeStyle = createFeatureTypeStyle(doc, attribute, value, geometryType);
            Node userStyle = doc.createElementNS(SLDNS, "UserStyle");
            userStyle.appendChild(featureTypeStyle);
            namedLayer.appendChild(userStyle);
        } else if (sldType.equalsIgnoreCase(SLDTYPE_NAMEDSTYLE)) {
            //Node layerFeatureConstraints = createLayerFeatureConstraints(doc, attribute, value, geometryType);
            Node layerFeatureConstraints = doc.createElementNS(SLDNS, "LayerFeatureConstraints");
            Node featureTypeConstraints = createFeatureTypeConstraint(doc, attribute, value,not);

            layerFeatureConstraints.appendChild(featureTypeConstraints);
            namedLayer.appendChild(layerFeatureConstraints);

            Node namedStyle=doc.createElementNS(SLDNS,"NamedStyle");
            Node styleName=doc.createElementNS(SLDNS, "Name");
            styleName.appendChild(doc.createTextNode(DEFAULT_STYLE));
            namedStyle.appendChild(styleName);
            namedLayer.appendChild(namedStyle);
        }
        return namedLayer;
    }

    public Node createFeatureTypeStyle(Document doc, String attribute, String[] value, String geometryType) {

        Node featureTypeStyle = doc.createElementNS(SLDNS, "FeatureTypeStyle");
        Node rule = doc.createElement("Rule");

        Node filter = createFilter(doc, attribute, value,false);

        rule.appendChild(filter);
        if (geometryType == null || geometryType.toLowerCase().indexOf("polygon") >= 0) {
            rule.appendChild(createDashedLinePolygon(doc));
        } else if (geometryType == null || geometryType.toLowerCase().indexOf("line") >= 0) {
            rule.appendChild(createStyleLine(doc, attribute));
        } else if (geometryType == null || geometryType.toLowerCase().indexOf("point") >= 0) {
            rule.appendChild(createStylePoint(doc, attribute));
        }

        featureTypeStyle.appendChild(rule);
        return featureTypeStyle;
    }

    private Node createStylePoint(Document doc, String geoProperty) {


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

    private Node createStylePolygon(Document doc, String geoProperty) {

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
    
    private Node createDashedLinePolygon(Document doc) {

        Node polygonSymbolizer = doc.createElement("PolygonSymbolizer");
        
        Node stroke = doc.createElement("Stroke");
        
        Element cssParam1 = doc.createElement("CssParameter");
        cssParam1.setAttribute("name", "stroke");
        cssParam1.appendChild(doc.createTextNode("#ff0000"));
        stroke.appendChild(cssParam1);
        
        Element cssParam2 = doc.createElement("CssParameter");
        cssParam2.setAttribute("name", "stroke-width");
        cssParam2.appendChild(doc.createTextNode("3"));
        stroke.appendChild(cssParam2);
        
        Element cssParam3 = doc.createElement("CssParameter");
        cssParam3.setAttribute("name", "stroke-dasharray");
        cssParam3.appendChild(doc.createTextNode("10 8"));
        stroke.appendChild(cssParam3);

        Node fill = doc.createElement("Fill");
        Element cssParam4 = doc.createElement("CssParameter");
        cssParam4.setAttribute("name", "fill-opacity");
        cssParam4.appendChild(doc.createTextNode("0"));
        fill.appendChild(cssParam4);
        
        polygonSymbolizer.appendChild(stroke);
        polygonSymbolizer.appendChild(fill);

        return polygonSymbolizer;
    }

    private Node createStyleLine(Document doc, String geoProperty) {


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

    private Node createFilter(Document doc, String attribute, String[] value,boolean not) {
        Node filter = doc.createElementNS(OGCNS, "Filter");

        Node nodeToUse=filter;
        if (not){
            Node notFilter = doc.createElementNS(OGCNS,"Not");
            filter.appendChild(notFilter);
            nodeToUse=notFilter;
        }
        if (value.length > 1) {
            Node orFilter = doc.createElementNS(OGCNS, "Or");
            nodeToUse.appendChild(orFilter);
            nodeToUse=orFilter;
        }

        for (int i = 0; i < value.length; i++) {
            Node propertyName = doc.createElementNS(OGCNS, "PropertyName");
            propertyName.appendChild(doc.createTextNode(attribute));
            Node literal = doc.createElementNS(OGCNS, "Literal");
            literal.appendChild(doc.createTextNode(value[i]));

            Node propertyIsEqualTo = doc.createElementNS(OGCNS, "PropertyIsEqualTo");
            propertyIsEqualTo.appendChild(propertyName);
            propertyIsEqualTo.appendChild(literal);

            nodeToUse.appendChild(propertyIsEqualTo);
        }
        return filter;
    }

    private Node createFeatureTypeConstraint(Document doc, String attribute, String[] value,boolean not) {
        Node featureTypeConstraint = doc.createElementNS(SLDNS, "FeatureTypeConstraint");
        Node filter = createFilter(doc, attribute, value,not);
        featureTypeConstraint.appendChild(filter);
        return featureTypeConstraint;
    }

    @Override
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
