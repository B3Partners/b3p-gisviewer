package nl.b3p.gis.viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.gis.viewer.ViewerCrudAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.apache.struts.validator.DynaValidatorForm;

/**
 * @author Boy de Wit
 */
public class UploadTempPointsAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(UploadTempPointsAction.class);

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        dynaForm.initialize(mapping);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward list(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            return getAlternateForward(mapping, request);
        }

        /* Punten omzetten naar wkt strings en opslaan in pojo met labels */
        FormFile tempFormFile = (FormFile) dynaForm.get("uploadFile"); 
        String csvSeparatorChar = dynaForm.getString("csvSeparatorChar"); 
        
        if (csvSeparatorChar == null || csvSeparatorChar.equals("")) {
            csvSeparatorChar = ";";
        }
        
        if (tempFormFile != null && !tempFormFile.getFileName().equals("")) {
            String path = getServlet().getServletContext().getRealPath("")
                    + File.separatorChar + tempFormFile.getFileName();
            
            /* bestand eerst wegschrijven daarna de csv inlezen */
            File serverFile = new File(path);
            serverFile.deleteOnExit();
            
            FileOutputStream outputStream = new FileOutputStream(serverFile);
            outputStream.write(tempFormFile.getFileData());

            UploadedPoints points = readCsv(path, csvSeparatorChar);
            
            if (points.getPoints() != null && points.getPoints().size() > 0) {
                HttpSession session = request.getSession();
                session.setAttribute(UploadedPointsWmsServlet.UPLOAD_MAP_NAME, points);
            }
            
            tempFormFile.destroy();
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private UploadedPoints readCsv(String fileName, String csvSeparatorChar)
            throws FileNotFoundException, IOException {
        
        UploadedPoints points = new UploadedPoints();
        
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String strLine = null;
        StringTokenizer st = null;
        
        int lineNumber = 0, tokenNumber = 0;
        while ((fileName = br.readLine()) != null) {
            lineNumber++;
            
            //break comma separated line using ","
            st = new StringTokenizer(fileName, "@");
            while (st.hasMoreTokens()) {
                tokenNumber++;
                
                String line = st.nextToken();
                if (lineNumber > 1) {
                    String[] arr = line.split(csvSeparatorChar);

                    if (arr != null && arr.length == 3) {
                        String x = arr[0];
                        String y = arr[1];
                        String label = arr[2];
                        
                        points.addPoint(label, "POINT(" + x + " " + y + ")");
                    }
                }
            }
            
            //reset token number
            tokenNumber = 0;
        }
        
        return points;
    }
}