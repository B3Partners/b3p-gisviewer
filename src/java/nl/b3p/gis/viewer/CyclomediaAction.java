package nl.b3p.gis.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import sun.security.rsa.RSAPrivateCrtKeyImpl;

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
        
        if (imageId != null && !imageId.equals("")) {     
            
            /* Configureerbaar maken. Opslaan in CyclomediaAccount entity */
            // apikey
            // acountId
            // wachtwoord
            // privatekey input veld of eenmalig via pfx upload ophalen
            
            String b3pApiKey = "K3MRqDUdej4JGvohGfM5e78xaTUxmbYBqL0tSHsNWnwdWPoxizYBmjIBGHAhS3U1";
            String accountId = "5155";
            String wachtwoord = "5155";
            
            DateFormat df = new SimpleDateFormat("yyyy-M-d H:mm");         
            String date = df.format(new Date());
            String token ="X" + accountId + "&" + imageId + "&" + date + "Z";
           
            /* Lees private key uit pfx bestand */
            File bestand = new File("C:\\tmp\\stichtsevecht_5155.pfx");
            String base64 = getBase64EncodedSignatureFromPfx(bestand, wachtwoord, token);
            
            /* TID */   
            String tid = URLEncoder.encode(token + "&" + base64, URL_ENCODING);
            
            /* Set Apikey, imageId and TID on request for jsp */
            request.setAttribute("apiKey", b3pApiKey);
            request.setAttribute("imageId", imageId);
            request.setAttribute("tid", tid);
        }
        
        return mapping.findForward(SUCCESS);
    }
    
    private PrivateKey getPrivateKeyFromPfxFile(File bestand, String password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        
        PrivateKey privateKey = null;
        
        KeyStore ks = java.security.KeyStore.getInstance(CERT_TYPE);
        ks.load(new FileInputStream(bestand), password.toCharArray());            

        Enumeration<String> aliases = ks.aliases();        
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            Key key = ks.getKey(alias, password.toCharArray());
            String keyFormat = key.getFormat();

            if ( (key instanceof RSAPrivateCrtKeyImpl) && keyFormat.equals(KEY_FORMAT) ) {
                privateKey = (PrivateKey)key;
            }
        }
        
        return privateKey;
    }
    
    private String getBase64EncodedSignatureFromPfx(File bestand, String password, String token)
            throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, InvalidKeyException,
            SignatureException {
        
        String base64 = null;

        PrivateKey privateKey = getPrivateKeyFromPfxFile(bestand, password);
        byte[] signature = sign(privateKey, token);    

        /* Base 64 encode signature */
        Base64 encoder = new Base64();
        base64 = new String(encoder.encode(signature));
        
        return base64;
    }
    
    private byte[] sign(PrivateKey privateKey, String token) 
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        
        Signature instance = Signature.getInstance(SIG_ALGORITHM);
        instance.initSign(privateKey);
        instance.update(token.getBytes());
        byte[] signature = instance.sign();
        
        return signature;
    }
    
    private String getBase64EncodedPrivateKey(PrivateKey privateKey) {
        String base64EncodedPrivateKey = null;
        Base64 encoder = new Base64();
        
        base64EncodedPrivateKey = new String(encoder.encode(privateKey.getEncoded()));
        
        return base64EncodedPrivateKey;
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
