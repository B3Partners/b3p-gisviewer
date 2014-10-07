package nl.b3p.gis.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProxySelector;
import java.util.Arrays;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.StreamCopy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

public class DocFetcherServlet extends HttpServlet {

    private static final String host = null;
    private static final int port = -1;
    private static final int RTIMEOUT = 20000;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static Log log = LogFactory.getLog(DocFetcherServlet.class);

    protected static enum FetchMethod {

        HTTP, FILE
    }
    protected String locationPrefix;
    protected int directoryKeepDepth;
    protected FetchMethod fetchMethod;

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
        
        log.debug("Uri: " + uri);
        log.debug("Filename: " + fileName);

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

           RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setStaleConnectionCheckEnabled(false)
            .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
            .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
            .setConnectionRequestTimeout(RTIMEOUT)
            .build();
        
        HttpClientBuilder hcb = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig);

        HttpClientContext context = HttpClientContext.create();
        if (username != null && password != null) {
            HttpHost targetHost = new HttpHost(host, port);
            CredentialsProvider credentialsProvider = 
                    new BasicCredentialsProvider();
            Credentials defaultcreds = 
                    new UsernamePasswordCredentials(username, password);
            AuthScope authScope = 
                    new AuthScope(targetHost.getHostName(), targetHost.getPort());
            credentialsProvider.setCredentials(authScope, defaultcreds);

            hcb = hcb.setDefaultCredentialsProvider(credentialsProvider);
        }
        //Use standard JRE proxy selector to obtain proxy information 
        SystemDefaultRoutePlanner routePlanner = 
                new SystemDefaultRoutePlanner(ProxySelector.getDefault());
        hcb.setRoutePlanner(routePlanner); 
        
        CloseableHttpClient client = hcb.build();
        
        HttpPost post = new HttpPost(location);
        CloseableHttpResponse iresponse = client.execute(post, context);

        try {

            int statusCode = iresponse.getStatusLine().getStatusCode();
            HttpEntity entity = iresponse.getEntity();
            input = entity.getContent();
            
            if (statusCode != 200) {
                log.error("Host: " + location + " error: " + iresponse.getStatusLine().getReasonPhrase());
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Document " + headerFileName + " ("
                        + iresponse.getStatusLine().getReasonPhrase()
                        + ")");
                return;
            }
            
            Header header = entity.getContentType();
            if (header == null || header.getValue().isEmpty()) {
                contentType = "image/png";
            } else {
                contentType = header.getValue();
            }

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
            iresponse.close();
        }
    }
}