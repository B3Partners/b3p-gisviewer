package nl.b3p.gis.viewer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.CyclomediaAccount;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

/**
 * @author Boy
*/
public class CyclomediaAction extends ViewerCrudAction {
    private static final Log log = LogFactory.getLog(CyclomediaAction.class);
    
    private final String CERT_TYPE = "PKCS12";
    private final String KEY_FORMAT = "PKCS#8";
    private final String SIG_ALGORITHM = "SHA1withRSA";
    private final String URL_ENCODING = "utf-8";

    /**
     * This method is called when a user has clicked a link to a Cyclomedia image.
     * /gisviewer/globespotter.do?imageId=[foto]
    */
    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String imageId = (String) request.getParameter("imageId");
        
        /* Applicatie code ophalen */
        Applicatie app = null;
        HttpSession session = request.getSession(true);
        String appCode = (String) session.getAttribute("appCode");
        if (appCode != null && appCode.length() > 0) {
            app = KaartSelectieUtil.getApplicatie(appCode);
        }
        if (app == null) {
            app = KaartSelectieUtil.getDefaultApplicatie();
        }
        
        ConfigKeeper keeper = new ConfigKeeper();
        CyclomediaAccount cycloAccount = keeper.getCyclomediaAccount(app.getCode());
        
        if (imageId != null && !imageId.equals("") && cycloAccount != null) {
            
            String apiKey = cycloAccount.getApiKey();
            
            /* TODO: Vervangen voor niet hard-coded vervangen bij ongeldige api key.
             * Dit is momenteel de b3p api key */
            if (apiKey == null || apiKey.equals("")) {
                apiKey = "K3MRqDUdej4JGvohGfM5e78xaTUxmbYBqL0tSHsNWnwdWPoxizYBmjIBGHAhS3U1";
            }
            
            String accountId = cycloAccount.getAccountId();
            
            // api aangepast, seconden toegevoegd en GMT tijd ivm zomertijd
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            
            String date = df.format(new Date());
            String token ="X" + accountId + "&" + imageId + "&" + date + "Z";
            
            String privateBase64Key = cycloAccount.getPrivateBase64Key();
            
            if (privateBase64Key == null || privateBase64Key.equals("")) {
                log.error("Kon private key voor aanmaken TID niet ophalen!");
            }
            
            String tid = getTIDFromBase64EncodedString(privateBase64Key, token);
            
            /* Set Apikey, imageId and TID on request for jsp */
            request.setAttribute("apiKey", apiKey);
            request.setAttribute("imageId", imageId);
            request.setAttribute("tid", tid);
        }
        
        return mapping.findForward(SUCCESS);
    }
    
    private byte[] sign(PrivateKey privateKey, String token) 
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        
        Signature instance = Signature.getInstance(SIG_ALGORITHM);
        instance.initSign(privateKey);
        instance.update(token.getBytes());
        byte[] signature = instance.sign();
        
        return signature;
    }
    
    private String getTIDFromBase64EncodedString(String base64Encoded, String token)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            SignatureException, UnsupportedEncodingException {
        
        String tid = null;        
        Base64 encoder = new Base64();
        
        byte[] tempBytes = encoder.decode(base64Encoded.getBytes());
        
        KeyFactory rsaKeyFac = KeyFactory.getInstance("RSA");  
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(tempBytes);  
        RSAPrivateKey privKey = (RSAPrivateKey)rsaKeyFac.generatePrivate(encodedKeySpec);          

        byte[] signature = sign(privKey, token);

        String base64 = new String(encoder.encode(signature));
        tid = URLEncoder.encode(token + "&" + base64, URL_ENCODING);
        
        return tid;
    }
}
