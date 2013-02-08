package nl.b3p.gis.viewer;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.imagetool.CombineImageSettings;
import nl.b3p.imagetool.CombineImageWkt;
import nl.b3p.imagetool.CombineImagesHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author b3partners
 */
public class UploadedPointsWmsServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(UploadedPointsWmsServlet.class);
    public static final String UPLOAD_MAP_NAME = "uploadedPoints";
    private final String IMAGE_MIME_TYPE = "image/png";
    
    private static UploadedPoints points = null;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String req = request.getParameter("REQUEST");
        
        HttpSession session = request.getSession();
        if (session.getAttribute(UPLOAD_MAP_NAME) != null) {
            points = (UploadedPoints) session.getAttribute(UPLOAD_MAP_NAME);
        }

        /* Only on GetMap request and temp points are uploaded */
        if (req != null && req.equalsIgnoreCase("GetMap")) {
            String bbox = request.getParameter("BBOX");
            String width = request.getParameter("WIDTH");
            String height = request.getParameter("HEIGHT");

            CombineImageSettings settings = new CombineImageSettings();
            settings.setBbox(bbox);
            settings.setWidth(new Integer(width));
            settings.setHeight(new Integer(height));
            settings.setMimeType(IMAGE_MIME_TYPE);

            List<CombineImageWkt> wkts = new ArrayList<CombineImageWkt>();

            if (points != null && points.getPoints() != null 
                    && points.getPoints().size() > 0) {
                
                Iterator it = points.getPoints().entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    String label = (String) pairs.getKey();
                    String wkt = (String) pairs.getValue();

                    CombineImageWkt ciWkt = new CombineImageWkt(wkt);
                    ciWkt.setLabel(label);
                    ciWkt.setColor(Color.MAGENTA);
                    wkts.add(ciWkt);
                }
            }

            settings.setWktGeoms(wkts);

            try {
                CombineImagesHandler.combineImage(response.getOutputStream(), settings);
            } catch (Exception ex) {
                log.error("Fout tijdens maken tijdelijke punten: ", ex);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}