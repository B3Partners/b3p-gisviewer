package nl.b3p.gis.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FileManagerServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
  * @author Chris van Lith
 */
public class DocFetcherServlet extends FileManagerServlet {

    protected static Log log = LogFactory.getLog(DocFetcherServlet.class);
    
     
    /**
     * no login required for get
     * @param request
     * @param response
     * @throws Exception 
     */
    protected void checkGetLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return;
    }

}
