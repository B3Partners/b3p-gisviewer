/*
 * CycloramaFetcherServlet.java
 *
 * Created on 3 mei 2006, 10:59
 *
 */
package nl.b3p.gis.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

//import sun.misc.*;
/**
 * @author Chris van Lith
 */
public class CycloramaFetcherServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(CycloramaFetcherServlet.class);
    private static final String imageIdLabel = "ImageID";
    private static final String tidLabel = "TID";
    private static PrivateKey key = null;
    private static Provider provBC = null;
    private static String privateBase64Key = null;
    private static String accountId;
    private static String cyclomediaUrl;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        privateBase64Key = getConfigValue(config, "privateBase64Key", null);
        accountId = getConfigValue(config, "accountId", null);
        cyclomediaUrl = getConfigValue(config, "cyclomediaUrl", "http://live.cyclomedia.nl/cycloscopeliteview/cycloviewui.asp?");
        if (privateBase64Key == null || accountId==null) {
            log.error("error getting key for cyclomedia client");
            return;
        }
        initSecurity();
    }

    private void initSecurity() {
        // Determine cyclomedia private key for signing
        Security.addProvider(new BouncyCastleProvider());
        provBC = Security.getProvider("BC");
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", provBC);
            Base64 b64 = new Base64();
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(b64.decode(privateBase64Key.getBytes()));
            key = keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            log.error("error initializing cyclomedia client: ", e);
        }
    }

    private static String getConfigValue(ServletConfig config, String parameter, String defaultValue) {
        String tmpval = config.getInitParameter(parameter);
        if (tmpval == null || tmpval.trim().length() == 0) {
            tmpval = defaultValue;
        }
        log.info("ConfigValue(" + parameter + ", " + tmpval + ")");
        if (tmpval==null)
            return null;
        else
            return tmpval.trim();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String imageId = request.getParameter("imageId");
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        Date now = new Date();
        sdf.applyPattern("yyyyMMddHHmm");
        String issuedTime = sdf.format(now);
        sdf.applyPattern("z");
        String timeZone = sdf.format(now);
        String redirectUrl = null;
        try {
            redirectUrl = createRedirectUrl(issuedTime, timeZone, imageId);
        } catch (Exception ex) {
            log.error("", ex);
            throw new ServletException("Error creating redirect url: ",ex);
        }
        response.sendRedirect(redirectUrl);
    }

    protected String createToken(String issuedTime, String timeZone, String imageId) {
        StringBuffer t = new StringBuffer();
        t.append("W");
        t.append(accountId);
        t.append("&");
        t.append(issuedTime);
        t.append("&");
        t.append(timeZone);
        t.append("&");
        t.append(imageId);
        return t.toString();
    }

    protected String createRedirectUrl(String issuedTime, String timeZone, String imageId) throws Exception {
        String token = createToken(issuedTime, timeZone, imageId);
        String tidsig = sign(token);

        String tid = "W" + accountId + "&" + issuedTime + "&" + timeZone + "&" + tidsig;

        log.debug("token: " + token);
        log.debug("tid: " + tid);

        StringBuffer theUrl = new StringBuffer(cyclomediaUrl);
        addQSPair(theUrl, imageIdLabel, imageId, false);
        addQSPair(theUrl, tidLabel, tid, true);
        return theUrl.toString();
    }

    private static String sign(String token) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        Signature sha = Signature.getInstance("MD5withRSA", provBC);
        sha.initSign(key);
        sha.update(token.getBytes());
        byte[] sig = sha.sign();
        return byteArrayToReversedHexStr(sig);
    }

    //This method converts an incoming array of
    // bytes into a string that represents each of
    // the bytes as two hex characters.
    private static String byteArrayToReversedHexStr(byte[] data) {
        String output = "";
        String tempStr = "";
        int tempInt = 0;
        for (int cnt = data.length - 1; cnt >= 0; cnt--) {
            //Deposit a byte into the 8 lsb of an int.
            tempInt = data[cnt] & 0xFF;
            //Get hex representation of the int as a
            // string.
            tempStr = Integer.toHexString(tempInt);
            //Append a leading 0 if necessary so that
            // each hex string will contain two
            // characters.
            if (tempStr.length() == 1) {
                tempStr = "0" + tempStr;
            }
            //Concatenate the two characters to the
            // output string.
            output = output + tempStr;
        }//end for loop
        return output.toUpperCase();
    }

    private void addQSPair(StringBuffer queryString, String name, String value, boolean amp) {
        if (queryString == null) {
            return;
        }
        try {
            if (amp) {
                queryString.append("&");
            }
            queryString.append(URLEncoder.encode(name, "ISO-8859-1"));
            queryString.append("=");
            queryString.append(URLEncoder.encode(value, "ISO-8859-1"));
        } catch (UnsupportedEncodingException ex) {
            log.error("UnsupportedEncodingException: ", ex);
        }
    }

    public static void main(String[] args) throws Exception {
        CycloramaFetcherServlet cyclomediaWMSClient = new CycloramaFetcherServlet();
        // maarssen
        accountId = "2372";
        cyclomediaUrl = "http://live.cyclomedia.nl/cycloscopeliteview/cycloviewui.asp?";
        privateBase64Key = "MIIBOgIBAAJBAPPSWn66HQDg88shBGH+7/vRxUMI4y+Ug8qZMjutA3jFbFjm4NmFKOIiAXt0icEf6vY66LOTbd6mP6mnejOaKkkCAwEAAQJBAMNCfn5mhbuiaxsNgfkItR+xyov4nhgIk9K4BOaNk+4ufG3YK+WuyRQa0DZE9uUndSka8E2LLN3ZcfWkiZ8/LEECIQD+yXI38enlgzlCARPAcmL2L0eRReJANoEhk/QWbZNgHQIhAPT7isbEPXE08THONbh1oYhGfddWnQ0RpcqkVNMuRLMdAiAHw4GsfL2g1b/P6BJ/Ab1MPSKUJaoAROjoaga9DDe6bQIgRw2t8nh4WZ1BV3C3pAh6EUxgs1QruN6ld2CyOY3x3wECIA5NYgbwMUN3sfLehJF5FxWfE0NiLTgqVH1sQG+BNVAl";
        String imageId = "0K002JUG";
        cyclomediaWMSClient.initSecurity();

        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        Date now = new Date();
        sdf.applyPattern("yyyyMMddHHmm");
        String issuedTime = sdf.format(now);
        sdf.applyPattern("z");
        String timeZone = sdf.format(now);
        String redirectUrl = cyclomediaWMSClient.createRedirectUrl(issuedTime, timeZone, imageId);
        System.out.println(redirectUrl);
    }
}
