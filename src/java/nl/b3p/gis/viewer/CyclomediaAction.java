package nl.b3p.gis.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    

    /**
     * This method is called when a user has clicked a link to a Cyclomedia image.
    */
    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String imageId = (String) request.getParameter("imageId");
        
        if (imageId != null && !imageId.equals("")) {            
            /* Fetch Cyclomedia configuration items */            
            // apikey
            // accountid
            // wachtwoord
            // saved private key from uploaded file
            
            String accountId = "b3p";
            char[] password = "5000".toCharArray();
            String date = (new Date()).toString();
            String token ="X" + accountId + "&" + imageId + "&" + date + "Z";
            
            File bestand = new File("C:\\tmp\\venray_5000.pfx");
            
            KeyStore ks = java.security.KeyStore.getInstance(CERT_TYPE);
            ks.load(new FileInputStream(bestand), password); 
            
            /* private key opvissen */
            Enumeration<String> aliases = ks.aliases();
            PrivateKey privateKey = null;
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                
                Key key = ks.getKey(alias, password);
                String keyFormat = key.getFormat();
                
                if ( (key instanceof RSAPrivateCrtKeyImpl) && keyFormat.equals(KEY_FORMAT) ) {
                    privateKey = (PrivateKey)key;
                }
            }
                    
            Signature instance = Signature.getInstance(SIG_ALGORITHM);
            instance.initSign(privateKey);
            instance.update(privateKey.getEncoded());
            byte[] signature = instance.sign();
            
            //openssl_sign(token, signature, , OPENSSL_ALGO_SHA1);
            
            /* Genereate TID */
            String tid = "";
            
            /* Set TID and imageId for jsp */            
            request.setAttribute("tid", tid);
            request.setAttribute("imageId", imageId);
        }

        return mapping.findForward(SUCCESS);
    }  
}
