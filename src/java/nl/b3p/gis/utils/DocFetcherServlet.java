package nl.b3p.gis.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.StreamCopy;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DocFetcherServlet extends HttpServlet {

    private static final String host = AuthScope.ANY_HOST;
    private static final int port = AuthScope.ANY_PORT;
    private static final int RTIMEOUT = 20000;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static Log log = LogFactory.getLog(DocFetcherServlet.class);

    protected static enum FetchMethod {

        HTTP, FILE
    }
    protected static String locationPrefix;
    protected static int directoryKeepDepth;
    protected static FetchMethod fetchMethod;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String dd = getConfigValue(config, "directoryKeepDepth", "-1");
        try {
            directoryKeepDepth = Integer.parseInt(dd);
        } catch (NumberFormatException nfe) {
            throw new ServletException("Ongeldige directoryKeepDepth init-param");
        }
        locationPrefix = getConfigValue(config, "locationPrefix", "");
        String fm = getConfigValue(config, "fetchMethod", "HTTP");
        if (fm.equalsIgnoreCase("HTTP")) {
            fetchMethod = FetchMethod.HTTP;
        } else {
            fetchMethod = FetchMethod.FILE;
        }
    }

    private static String getConfigValue(ServletConfig config, String parameter, String defaultValue) {
        String tmpval = config.getInitParameter(parameter);
        if (tmpval == null || tmpval.trim().length() == 0) {
            tmpval = defaultValue;
        }
        log.debug("ConfigValue(" + parameter + ", " + tmpval + ")");
        return tmpval.trim();
    }

    private static String extractFileName(String fileName) {
        int dirSepPos = Math.max(fileName.lastIndexOf('\\'), fileName.lastIndexOf('/'));
        if (dirSepPos != -1) {
            return fileName.substring(dirSepPos + 1);
        } else {
            return fileName;
        }
    }

    private String transformFilename(String filename) {
        String debugStr = "transformFilename(): origineel=\"" + filename + "\"; getransformeerd=\"";

        String transformedFilename = filename;

        if (directoryKeepDepth != -1) {
            String[] paths = filename.split("(/|\\\\)");
            if (paths.length > 0) {
                transformedFilename = "";
                int start = Math.max(0, paths.length - 1 - directoryKeepDepth);
                for (int i = start; i < paths.length; i++) {
                    transformedFilename = transformedFilename + "/" + paths[i];
                }
            }
        }
        transformedFilename = locationPrefix + transformedFilename;
        debugStr = debugStr + transformedFilename + "\"";
        log.debug(debugStr);
        return transformedFilename;
    }

    public static String getLastBitFromUrl(final String url) {
        return url.replaceFirst(".*/([^/?]+).*", "$1");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        /* REST instead of ? in url eq.
         * http://domain.com/gisviewer/services/DocFetcher/abc.jpg
         */
        String uri = request.getPathInfo();
        //String lastPart = getLastBitFromUrl(uri);        
        String fileName = transformFilename(uri);

        switch (fetchMethod) {
            case FILE:
                getDocByFS(fileName, response);
                break;
            case HTTP:
            default:
                getDocByHttp(fileName, response);
                break;
        }
    }

    private void getDocByFS(String fileName, HttpServletResponse response) throws IOException {
        /* XXX spaties?/;/non-ASCII niet toegestaan in bestandsnaam */
        String headerFileName = extractFileName(fileName);
        String contentType = null;
        InputStream input = null;

        try {
            File file = new File(fileName);
            if (!file.exists()) {
                log.error("Bestand bestaat niet voor " + fileName);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document " + headerFileName + " (bestand bestaat niet)");
                return;
            }

            contentType = getServletContext().getMimeType(fileName);
            if (contentType == null) {
                contentType = DEFAULT_MIME_TYPE;
            }
            input = new FileInputStream(file);

            response.setContentType(contentType);
            response.addHeader("Content-Disposition", "attachment; filename=" + headerFileName);

            try {
                OutputStream output = response.getOutputStream();
                StreamCopy.copy(input, output);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (Exception e) {
            log.error("Exception bij sturen document voor " + fileName, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fout tijdens sturen document: " + e.getClass() + ": " + e.getMessage());
        }
    }

    private void getDocByHttp(String location, HttpServletResponse response) throws IOException {
        getDocByHttp(location, null, null, response);
    }

    private void getDocByHttp(String location, String username, String password, HttpServletResponse response) throws IOException {
        /* XXX spaties?/;/non-ASCII niet toegestaan in bestandsnaam */
        String headerFileName = extractFileName(location);
        String contentType = null;
        InputStream input = null;

        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().
                getParams().setConnectionTimeout(RTIMEOUT);

        if (username != null && password != null) {
            client.getParams().setAuthenticationPreemptive(true);
            Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
            AuthScope authScope = new AuthScope(host, port);
            client.getState().setCredentials(authScope, defaultcreds);
        }

        // Create a method instance.
        GetMethod method = new GetMethod(location);

        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                log.error("Host: " + location + " error: " + method.getStatusLine().getReasonPhrase());
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Document " + headerFileName + " ("
                        + method.getStatusLine().getReasonPhrase()
                        + ")");
                return;
            }
            if (method.getResponseHeader("Content-Type") != null) {
                contentType = method.getResponseHeader("Content-Type").getValue();
            }

            input = method.getResponseBodyAsStream();

            response.setContentType(contentType);
            response.addHeader("Content-Disposition", "attachment; filename=" + headerFileName);

            try {
                OutputStream output = response.getOutputStream();
                StreamCopy.copy(input, output);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (Exception e) {
            log.error("Exception bij sturen document voor " + location, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fout tijdens sturen document: " + e.getClass() + ": " + e.getMessage());
        } finally {
            // Release the connection.
            method.releaseConnection();
        }

    }
}