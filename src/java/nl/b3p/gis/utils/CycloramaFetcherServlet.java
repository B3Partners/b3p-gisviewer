/*
 * CycloramaFetcherServlet.java
 *
 * Created on 3 mei 2006, 10:59
 *
 */
package nl.b3p.gis.utils;

import com.Ostermiller.util.Base64;
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
        initSecurity();
        accountId = getConfigValue(config, "accountId", "2194");
        cyclomediaUrl = getConfigValue(config, "cyclomediaUrl", "http://live.cyclomedia.nl/cycloscopeliteview/cycloviewui.asp?");
    }

    private void initSecurity() {
        if (privateBase64Key == null) {
            StringBuffer privateBase64KeyDefault = new StringBuffer();
            privateBase64KeyDefault.append("MIIBPAIBAAJBAKjeVutOmj3gQAv5tRCEvCmDr8qCMCiavtJGFekZ6X9kO3i2Gsq+\n");
            privateBase64KeyDefault.append("4zS+c2/hPgquiJMVJ+9O+nlFYnQy7btrtxsCAwEAAQJBAIXDgWRpYgKLhRA3X7bS\n");
            privateBase64KeyDefault.append("/d2Ao5otIAq58VfNDoQT84LlUCYxmsJMrFox9ybqoQDQj68LuaaLiAjzQI5DS60z\n");
            privateBase64KeyDefault.append("m3ECIQDW8ykzf4SDpC6mA1ofn9fnnnfdv9JA1xvkNhYy4FC/cwIhAMkeRlss89UW\n");
            privateBase64KeyDefault.append("t4hNVpcLl4owhKZByVXbA5GYdbYJF++5AiEAkib5/8MXxi6PbV/gGpqjwiBU3lk8\n");
            privateBase64KeyDefault.append("S8w3cb947pTpMpMCIQC/EWoZ+Mz15o0aiw72lOa1PH7pTJqwXFA5pDRAasc40QIg\n");
            privateBase64KeyDefault.append("PbiSX7GYGWggb20XFCPHEf6MHwfH/EZYIXCYxz7K6DM=\n");
            privateBase64Key = privateBase64KeyDefault.toString();
        }
        // Determine cyclomedia private key for signing
        Security.addProvider(new BouncyCastleProvider());
        provBC = Security.getProvider("BC");
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", provBC);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateBase64Key.getBytes()));
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
        cyclomediaWMSClient.initSecurity();

        accountId = "2194";
        cyclomediaUrl = "http://live.cyclomedia.nl/cycloscopeliteview/cycloviewui.asp?";

        String imageId = "3N005KAW";
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
