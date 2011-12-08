package nl.b3p.gis.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
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
            
            /* Wat Venray foto id's */
            // 5D05VXT6, 5D05VXU4,5D05VXUE
            imageId = "5D05VXT6";
            
            String accountId = "venray_intern"; // B3_develop
            char[] password = "5000".toCharArray(); //***REMOVED***
            
            DateFormat df = new SimpleDateFormat("yyyy-M-d H:m");         
            String date = df.format(new Date());
            String token ="X" + accountId + "&" + imageId + "&" + date;
            
            String b3pApiKey = "K3MRqDUdej4JGvohGfM5e78xaTUxmbYBqL0tSHsNWnwdWPoxizYBmjIBGHAhS3U1";
            String venrayApiKey = "978e273a8b2f117a2c3156744b6fca0ebef1bedd";            
           
            File bestand = new File("C:\\tmp\\venray_5000.pfx");
            
            KeyStore ks = java.security.KeyStore.getInstance(CERT_TYPE);
            ks.load(new FileInputStream(bestand), password);            
            
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
            
            Base64 encoder = new Base64();
            String base64 = new String(encoder.encode(signature));
            
            /* Genereate TID */            
            String tid = URLEncoder.encode(token + "&" + base64, "utf-8");
            
            /* Set TID and imageId for jsp */   
            request.setAttribute("imageId", imageId);
            request.setAttribute("apiKey", venrayApiKey);
            request.setAttribute("tid", tid);    
        }

        return mapping.findForward(SUCCESS);
    }  
}
