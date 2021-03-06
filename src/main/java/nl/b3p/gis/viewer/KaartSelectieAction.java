package nl.b3p.gis.viewer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nl.b3p.commons.services.StreamCopy;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.UserKaartgroep;
import nl.b3p.gis.viewer.db.UserKaartlaag;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.gis.viewer.db.UserLayerStyle;
import nl.b3p.gis.viewer.db.UserService;
import nl.b3p.gis.viewer.services.DownloadServlet;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.ogc.sld.SldUserStyle;
import nl.b3p.ogc.sld.SldWriter;
import nl.b3p.ogc.utils.KBConfiguration;
import nl.b3p.ogc.utils.OGCConstants;
import nl.b3p.ogc.utils.OGCRequest;
import nl.b3p.wms.capabilities.Layer;
import nl.b3p.wms.capabilities.Style;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.wms.WMSUtils;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

/**
 * @author Boy de Wit
 */
public class KaartSelectieAction extends BaseGisAction {

    private static final Log log = LogFactory.getLog(KaartSelectieAction.class);
    protected static final String SAVE = "save";
    protected static final String SAVE_WMS_SERVICE = "saveWMSService";
    protected static final String DELETE_WMS_SERVICES = "deleteWMSServices";    
    protected static final String ERROR_DUPLICATE_WMS = "error.duplicate.wms";
    protected static final String PARAM_APPCODE = "appCode";

    protected static final String ERROR_SAVE_WMS = "error.save.wms";
    protected static final String ERROR_SAVE_WMS_IO = "error.save.wms.io";
    protected static final String ERROR_SAVE_WMS_SERVICE = "error.save.wms.service";
    protected static final String ERROR_SAVE_WMS_SAX = "error.save.wms.sax";
    
    private static final int BUFFER_SIZE = 8192;
    private static final String ISO_CHARSET = "ISO-8859-1";
    private static final String UTF8_CHARSET = "UTF-8";

    protected Map getActionMethodPropertiesMap() {
        Map map = new HashMap();

        ExtendedMethodProperties hibProp = null;

        hibProp = new ExtendedMethodProperties(SAVE);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setDefaultMessageKey("message.layerselection.success");
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("message.layerselection.failed");
        map.put(SAVE, hibProp);

        hibProp = new ExtendedMethodProperties(SAVE_WMS_SERVICE);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setDefaultMessageKey("message.userwms.success");
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("message.userwms.failed");
        map.put(SAVE_WMS_SERVICE, hibProp);

        hibProp = new ExtendedMethodProperties(DELETE_WMS_SERVICES);
        hibProp.setDefaultForwardName(SUCCESS);
        hibProp.setDefaultMessageKey("message.userwms.delete.success");
        hibProp.setAlternateForwardName(FAILURE);
        hibProp.setAlternateMessageKey("message.userwms.delete.failed");
        map.put(DELETE_WMS_SERVICES, hibProp);

        return map;
    }

    private void reloadFormData(HttpServletRequest request) throws JSONException, Exception {
        setTree(request);
        setUserviceTrees(request);
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response)
            throws JSONException, Exception {

        HttpSession session = request.getSession(true);
        String appCode = (String) session.getAttribute("appCode");
        Boolean useUserWmsDropdown = (Boolean) session.getAttribute("useUserWmsDropdown");

        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        /* Applicatie ophalen om te zien of deze read-only is */
        Applicatie app = KaartSelectieUtil.getApplicatie(appCode);

        if (app != null) {
            Boolean readOnly = app.getRead_only();

            if (readOnly) {
                dynaForm.set("currentAppReadOnly", "1");    
                request.setAttribute("currentAppReadOnly", "1");
            } else {
                dynaForm.set("currentAppReadOnly", "0");
                request.setAttribute("currentAppReadOnly", "0");
            }
            
            dynaForm.set("kaartNaam", app.getNaam());  
            request.setAttribute("kaartNaam", app.getNaam());
            
            String email = "";
            /* Juiste email klaarzetten bij opslaan nieuwe kaartselectie */
            if (app.getVersie() != null && app.getVersie().intValue() < 2) {                
                String emailSuffix = DownloadServlet.getEmailSuffix();
                
                if (emailSuffix != null && !emailSuffix.equals("")) {
                    email = emailSuffix;
                }
            } else if (app.getVersie() != null && app.getVersie().intValue() > 1
                    && app.getEmail() != null && !app.getEmail().equals("")) {
                
                email = app.getEmail();             
            }
            
            dynaForm.set("gebruikerEmail", email);
            request.setAttribute("gebruikerEmail", email);  
            
            /* versie zetten */
            if (app.getVersie() != null) {
                request.setAttribute("appVersion", app.getVersie());
            }
        }
        
        if (useUserWmsDropdown != null) {
            if (useUserWmsDropdown) {
                dynaForm.set("useUserWmsDropdown", "1");
                request.setAttribute("useUserWmsDropdown", "1");
                
                Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
                
                List<UserService> services = sess.createQuery("from UserService where"
                        + " use_in_list = :uselist order by name")
                        .setParameter("uselist", true)
                        .list();
                
                if (services != null && services.size() > 0) {
                    request.setAttribute("userWmsList", services);
                }                
            } else {
                dynaForm.set("useUserWmsDropdown", "0");
                request.setAttribute("useUserWmsDropdown", "0");
            }
        }

        return mapping.findForward(SUCCESS);
    }

    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String[] kaartgroepenAan = (String[]) dynaForm.get("kaartgroepenAan");
        String[] kaartlagenAan = (String[]) dynaForm.get("kaartlagenAan");
        String[] kaartgroepenDefaultAan = (String[]) dynaForm.get("kaartgroepenDefaultAan");
        String[] kaartlagenDefaultAan = (String[]) dynaForm.get("kaartlagenDefaultAan");
        String[] layersAan = (String[]) dynaForm.get("layersAan");
        String[] layersDefaultAan = (String[]) dynaForm.get("layersDefaultAan");
        String[] useLayerStyles = (String[]) dynaForm.get("useLayerStyles");

        /* userLayerIds wordt gebruikt om de layerid's te kunnen koppelen
        aan de textarea's */
        String[] userLayerIds = (String[]) dynaForm.get("userLayerIds");
        String[] useLayerSldParts = (String[]) dynaForm.get("useLayerSldParts");

        /* groepen en lagen die default aan staan ook toevoegen aan arrays */
        kaartgroepenAan = addDefaultOnValues(kaartgroepenDefaultAan, kaartgroepenAan);
        kaartlagenAan = addDefaultOnValues(kaartlagenDefaultAan, kaartlagenAan);
        layersAan = addDefaultOnValues(layersDefaultAan, layersAan);

        HttpSession session = request.getSession(true);
        String code = (String) session.getAttribute("appCode");
        
        String currentAppReadOnly = dynaForm.getString("currentAppReadOnly");

        String useUserWmsDropdown = dynaForm.getString("useUserWmsDropdown");
        Boolean makeAppReadOnly = (Boolean) dynaForm.get("makeAppReadOnly");

        Boolean isReadOnly = false;
        if (currentAppReadOnly != null && currentAppReadOnly.equals("1")) {
            isReadOnly = true;
        }

        Boolean makeReadOnly = false;
        if (makeAppReadOnly != null && makeAppReadOnly) {
            makeReadOnly = true;
        }

        String kaartNaam = dynaForm.getString("kaartNaam");
        String gebruikerEmail = dynaForm.getString("gebruikerEmail");
        
        String newkaartoption = dynaForm.getString("newkaartoption");
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        /* Als huidige Applicatie alleen lezen is kopie maken. Anders huidige applicatie
        opslaan met alleen-lezen keuze van gebruiker. */
        Applicatie currentApp = KaartSelectieUtil.getApplicatie(code);
        
        /* Indien gebruiker de optie heeft gekozen om als nieuwe kaart op te slaan 
         * dan behandelen als read-only zodat er een nieuwe app komt */
        if (newkaartoption != null && newkaartoption.equals("new")) {
            isReadOnly = true;
        }
        
        if (isReadOnly) {
            if (currentApp != null) {

                Applicatie app = KaartSelectieUtil.copyApplicatie(currentApp, makeReadOnly, true);
                
                if (kaartNaam != null && !kaartNaam.equals("") && gebruikerEmail != null 
                    && !gebruikerEmail.equals("")) {
                        app.setNaam(kaartNaam);
                        app.setEmail(gebruikerEmail);                    
                } else {
                    app.setNaam(currentApp.getNaam());
                    app.setEmail(currentApp.getEmail());
                }

                sess.save(app);
                sess.flush();

                code = app.getCode();
            }
        } else if (!isReadOnly && makeReadOnly) {
            currentApp.setRead_only(makeReadOnly);

            sess.save(currentApp);
            sess.flush();
        }

        /* Eerst alle huidige records verwijderen. Dan hoeven we geen
         * onoverzichtelijke if meuk toe te voegen om te kijken of er vinkjes
         * ergens wel of niet aan staan en dan wissen */
        removeExistingUserKaartgroepAndUserKaartlagen(code);
        resetExistingUserLayers(code);

        /* Opslaan user kaartgroepen */
        for (int i = 0; i < kaartgroepenAan.length; i++) {
            Integer clusterId = new Integer(kaartgroepenAan[i]);
            boolean defaultOn = isKaartGroepDefaultOn(kaartgroepenDefaultAan, clusterId);

            UserKaartgroep groep = getUserKaartGroep(code, clusterId);

            if (groep != null) {
                groep.setDefault_on(defaultOn);
                sess.merge(groep);
            } else {
                UserKaartgroep newGroep = new UserKaartgroep(code, clusterId, defaultOn);
                sess.save(newGroep);
            }
        }

        /* Opslaan user kaartlagen */
        for (int j = 0; j < kaartlagenAan.length; j++) {
            Integer themaId = new Integer(kaartlagenAan[j]);
            boolean defaultOn = isKaartlaagDefaultOn(kaartlagenDefaultAan, themaId);

            UserKaartlaag laag = getUserKaartlaag(code, themaId);

            if (laag != null) {
                laag.setDefault_on(defaultOn);
                sess.merge(laag);
            } else {
                UserKaartlaag newLaag = new UserKaartlaag(code, themaId, defaultOn);
                sess.save(newLaag);
            }
        }

        /* Opslaan Service layers */
        for (int k = 0; k < layersAan.length; k++) {
            Integer id = new Integer(layersAan[k]);
            boolean defaultOn = isKaartlaagDefaultOn(layersDefaultAan, id);

            UserLayer userLayer = (UserLayer) sess.get(UserLayer.class, id);
            if (userLayer != null) {
                userLayer.setDefault_on(defaultOn);
                userLayer.setShow(true);

                sess.merge(userLayer);
            }
        }

        /* Opslaan gekozen Layer Styles */
        for (int m = 0; m < useLayerStyles.length; m++) {
            String[] useStyle = useLayerStyles[m].split("@");

            Integer layerId = new Integer(useStyle[0]);
            String styleName = useStyle[1];

            if (layerId != null && layerId > 0 && styleName != null) {
                UserLayer ul = (UserLayer) sess.get(UserLayer.class, layerId);

                if (styleName.equals("default")) {
                    ul.setUse_style(null);
                } else {
                    ul.setUse_style(styleName);
                }

                sess.merge(ul);
            }
        }

        /* Opslaan ingevulde sld parts
        String[] userLayerIds is een hidden input met daarin een even grote
        array met de layer ids. */
        for (int n = 0; n < useLayerSldParts.length; n++) {
            String sldPart = useLayerSldParts[n];
            Integer layerId = new Integer(userLayerIds[n]);

            if (sldPart != null && layerId != null && layerId > 0) {
                UserLayer ul = (UserLayer) sess.get(UserLayer.class, layerId);

                if (sldPart == null || sldPart.equals("")) {
                    ul.setSld_part(null);
                } else {
                    ul.setSld_part(sldPart);
                }

                sess.merge(ul);
            }
        }

        session.setAttribute("appCode", code);
        request.setAttribute("appCodeSaved", code); 
        
        if (useUserWmsDropdown != null && !useUserWmsDropdown.equals("")) {
            if (useUserWmsDropdown.equals("1")) {               
                List<UserService> services = sess.createQuery("from UserService where"
                        + " use_in_list = :uselist order by name")
                        .setParameter("uselist", true)
                        .list();
                
                if (services != null && services.size() > 0) {
                    request.setAttribute("userWmsList", services);
                } 
                
                session.setAttribute("useUserWmsDropdown", true);
                request.setAttribute("useUserWmsDropdown", "1"); 
            } else {
                session.setAttribute("useUserWmsDropdown", false);
                request.setAttribute("useUserWmsDropdown", "0"); 
            }            
        }
        
        /* Emailen als gebruiker naam en emailadres heeft ingevuld */ 
        if (kaartNaam != null && !kaartNaam.equals("") && gebruikerEmail != null 
                && !gebruikerEmail.equals("")) {
            
            String servletpath = request.getRequestURL().toString();
            servletpath = servletpath.substring(0,servletpath.lastIndexOf("/"));
            
            String url = servletpath + "/viewer.do?appCode=" + code + "&forceViewer=true";
            sendEmail(kaartNaam, gebruikerEmail, url);        
        }

        KaartSelectieUtil.populateKaartSelectieForm(code, request);

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }
    
    private void sendEmail(String kaartNaam, String gebruikerEmail, String link) {        
        try {        
            java.util.Properties properties = System.getProperties();

            /* Set host */
            String smtpHost = DownloadServlet.getSmtpHost();
            properties.put("mail.smtp.host", smtpHost);

            javax.mail.Session mailSession = javax.mail.Session.getInstance(properties, null);
            mailSession.setDebug(false);

            /* From */
            MimeMessage msg = new MimeMessage(mailSession);
            String contactEmail = DownloadServlet.getContactEmail();
            if (contactEmail == null) {
                throw new Exception("Instelling contact e-mail is verplicht.");
            }
            
            Address frmAddress = new InternetAddress(contactEmail);
            msg.setFrom(frmAddress);

            /* To */
            Address[] toAddresses = InternetAddress.parse(gebruikerEmail);
            msg.setRecipients(Message.RecipientType.TO, toAddresses);

            /* Subject */
            String onderwerp = "Gisviewer link naar " + kaartNaam + " kaart";
            msg.setSubject(onderwerp);

            /* Get mail text from file */
            String file = DownloadServlet.getMailKaartselectie();

            /* Object params */
            Object[] params = null;

            params = new Object[] {
                kaartNaam, // 0 = kaart naam
                link, // 1 = download link
                contactEmail // 2 = contact email
            };

            ByteArrayOutputStream out = new ByteArrayOutputStream();            
            InputStream in = null;
            in = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
            StreamCopy.copy(in, out);
            MessageFormat textMessage = new MessageFormat(out.toString(ISO_CHARSET), Locale.ENGLISH);   

            /* Content */
            String tekst = textMessage.format(params);        
            msg.setContent(tekst, "text/plain");

            /* Date */
            msg.setSentDate(new Date());
            
            /* Send it */
            Transport.send(msg);
        
        } catch (AddressException ae) {
            log.error("Fout bij het opgegeven emailadres. ", ae);
        } catch (SendFailedException sfe) {
            log.error("Fout bij het verzenden van het bericht.", sfe);
        } catch (MessagingException me) {
            log.error("Fout bij het verzenden van het bericht. ", me);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    public ActionForward saveWMSService(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String groupName = (String) dynaForm.get("groupName");
        String serviceUrl = (String) dynaForm.get("serviceUrl");
        String sldUrl = (String) dynaForm.get("sldUrl");
        
        Boolean useWmsDropdown = false;        
        String selectedUserWMSId = (String) dynaForm.get("selectedUserWMSId");        
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        
        if (selectedUserWMSId != null && !selectedUserWMSId.equals("")) {
            UserService service = (UserService) sess.get(UserService.class, new Integer(selectedUserWMSId));
            
            if (service != null) {
                useWmsDropdown = true;
                
                groupName = service.getGroupname();
                serviceUrl = service.getUrl();
                sldUrl = service.getSld_url();
            }
        }

        /* controleren of serviceUrl al voorkomt bij applicatie */
        HttpSession session = request.getSession(true);
        String code = (String) session.getAttribute("appCode");
        
        String currentAppReadOnly = dynaForm.getString("currentAppReadOnly");
        Boolean makeAppReadOnly = (Boolean) dynaForm.get("makeAppReadOnly");

        Boolean isReadOnly = false;
        if (currentAppReadOnly != null && currentAppReadOnly.equals("1")) {
            isReadOnly = true;
        }

        Boolean makeReadOnly = false;
        if (makeAppReadOnly != null && makeAppReadOnly) {
            makeReadOnly = true;
        }

        /* Als huidige Applicatie alleen lezen is kopie maken. Anders huidige applicatie
        opslaan met alleen-lezen keuze van gebruiker. */
        Applicatie currentApp = KaartSelectieUtil.getApplicatie(code);
        Applicatie app = null;
        
        if (isReadOnly) {
            if (currentApp != null) {

                app = KaartSelectieUtil.copyApplicatie(currentApp, makeReadOnly, true);
                app.setNaam(currentApp.getNaam());

                sess.save(app);
                sess.flush();

                code = app.getCode();
            }
        } else if (!isReadOnly && makeReadOnly) {
            currentApp.setRead_only(makeReadOnly);

            sess.save(currentApp);
            sess.flush();
        }

        /* TODO: Deze check even uitgezet zodat je meerdere eigen wms'en
         * kunt toevoegen met andere sld url's
        
        if (userAlreadyHasThisService(code, serviceUrl)) {
            reloadFormData(request);

            addMessage(request, ERROR_DUPLICATE_WMS, serviceUrl);
            return getAlternateForward(mapping, request);
        }
        */

        /* WMS Service layers ophalen met Geotools */
        OGCRequest wmsUrl = checkWmsUrl(serviceUrl.trim());

        URL url = null;
        try {
            url = new URL(wmsUrl.getUrl());
        } catch (MalformedURLException mfex) {
            //will not happen
        }
        
        WebMapServer wms = null;
        org.geotools.data.ows.Layer[] layers = null;
        try {            
            wms = new WebMapServer(url);
            layers = WMSUtils.getNamedLayers(wms.getCapabilities());            
        } catch (IOException ioex) {
            log.error("Kan geen verbinding maken naar de WMS Service.", ioex);

            reloadFormData(request);
            addMessage(request, ERROR_SAVE_WMS_IO, url.toString());
            return getAlternateForward(mapping, request);
        } catch (ServiceException se) {
            log.error("De WMS Service geeft een fout status terug.", se);

            reloadFormData(request);
            addMessage(request, ERROR_SAVE_WMS_SERVICE, url.toString());
            return getAlternateForward(mapping, request);
        } catch (SAXException saxe) {
            log.error("De WMS Service geeft een ongeldig document terug.", saxe);

            reloadFormData(request);
            addMessage(request, ERROR_SAVE_WMS_SAX, url.toString());
            return getAlternateForward(mapping, request);
        } catch (Exception ex) {
            log.error("Fout tijdens opslaan WMS Service: ", ex);

            reloadFormData(request);
            addMessage(request, ERROR_SAVE_WMS, url.toString());
            return getAlternateForward(mapping, request);
        }

        /* Nieuwe UserService entity aanmaken en opslaan */
        UserService us = new UserService(code, serviceUrl, groupName);

        /* Eerst parents ophalen. */
        List<org.geotools.data.ows.Layer> parents = getParentLayers(layers);

        for (org.geotools.data.ows.Layer layer : parents) {
            UserLayer ul = createUserLayers(us, layer, null);
            us.addLayer(ul);
        }

        /* Indien geen parents gevonden maar wel layers dan gewoon allemaal
         * toevoegen. */
        if (parents.size() < 1) {
            for (int i = 0; i < layers.length; i++) {
                UserLayer ul = createUserLayers(us, layers[i], null);
                us.addLayer(ul);
            }
        }
        
        if (sldUrl != null && !sldUrl.equals("")) {
            us.setSld_url(sldUrl);
        }

        sess.save(us);
        
        // Sld url opslaan en styles bij juiste layers opslaan
        // NamedLayers uit Sld ophalen
        SldWriter sldwriter = new SldWriter();
        if (sldUrl != null && !sldUrl.equals("")) {
            sldwriter.parseByUrl(sldUrl);
        }
        
        // Kijken of voor deze layer UserStyles in bijbehorende NamedLayer
        // voorkomen. Zo ja, deze als Style opslaan in database 
        Set<UserLayer> layerSet = us.getUser_layers();
        for (UserLayer userLayer : layerSet) {
            //get only the userStyles for this layer
            List<SldUserStyle> userStyles = sldwriter.getUserStyles(userLayer.getName());

            for (SldUserStyle userStyle : userStyles) {
                //TODO Wat gebeurt hier? telkens weer ander sldpart in zelfde
                //var opslaan?? of is er altijd maar ��n userStyle?
                userLayer.setSld_part(userStyle.getSldPart());
                UserLayerStyle uls = new UserLayerStyle();
                uls.setLayerid(userLayer);
                if (userLayer.getName() == null && userLayer.getName().equals("")) {
                    uls.setName(userStyle.getName() + "_SLD");
                } else {
                    uls.setName(userLayer.getName());
                }
                userLayer.addStyle(uls);
            }
            sess.save(userLayer);
        }
       
        session.setAttribute("appCode", code);
        request.setAttribute("appCodeSaved", code);

        KaartSelectieUtil.populateKaartSelectieForm(code, request);
        
        if (useWmsDropdown) {            
            List<UserService> services = sess.createQuery("from UserService where"
                    + " use_in_list = :uselist order by name")
                    .setParameter("uselist", true)
                    .list();

            if (services != null && services.size() > 0) {
                request.setAttribute("userWmsList", services);
            } 

            session.setAttribute("useUserWmsDropdown", true);
            request.setAttribute("useUserWmsDropdown", "1");            
        } else {
            session.setAttribute("useUserWmsDropdown", false);
            request.setAttribute("useUserWmsDropdown", "0"); 
        }        
        
        if (app != null) {
            Boolean readOnly = app.getRead_only();

            if (readOnly) {
                dynaForm.set("currentAppReadOnly", "1");    
                request.setAttribute("currentAppReadOnly", "1");
            } else {
                dynaForm.set("currentAppReadOnly", "0");
                request.setAttribute("currentAppReadOnly", "0");
            }
            
            dynaForm.set("kaartNaam", app.getNaam());
            dynaForm.set("gebruikerEmail", app.getEmail());
            
            request.setAttribute("kaartNaam", app.getNaam());
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }
    
    public ActionForward deleteWMSServices(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        HttpSession session = request.getSession(true);
        Boolean useUserWmsDropdown = (Boolean) session.getAttribute("useUserWmsDropdown");
        
        String[] servicesAan = (String[]) dynaForm.get("servicesAan");
        String code = (String) session.getAttribute("appCode");
        
        Applicatie app = KaartSelectieUtil.getApplicatie(code);

        for (int i = 0; i < servicesAan.length; i++) {
            Integer serviceId = new Integer(servicesAan[i]);
            KaartSelectieUtil.removeService(serviceId);
        }

        KaartSelectieUtil.populateKaartSelectieForm(code, request);
        
        if (useUserWmsDropdown) {    
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            List<UserService> services = sess.createQuery("from UserService where"
                    + " use_in_list = :uselist order by name")
                    .setParameter("uselist", true)
                    .list();

            if (services != null && services.size() > 0) {
                request.setAttribute("userWmsList", services);
            } 

            session.setAttribute("useUserWmsDropdown", true);
            request.setAttribute("useUserWmsDropdown", "1");            
        } else {
            session.setAttribute("useUserWmsDropdown", false);
            request.setAttribute("useUserWmsDropdown", "0"); 
        }
        
        if (app != null) {
            Boolean readOnly = app.getRead_only();

            if (readOnly) {
                dynaForm.set("currentAppReadOnly", "1");    
                request.setAttribute("currentAppReadOnly", "1");
            } else {
                dynaForm.set("currentAppReadOnly", "0");
                request.setAttribute("currentAppReadOnly", "0");
            }
            
            dynaForm.set("kaartNaam", app.getNaam());
            dynaForm.set("gebruikerEmail", app.getEmail());
            
            request.setAttribute("kaartNaam", app.getNaam());
        }

        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private List<org.geotools.data.ows.Layer> getParentLayers(org.geotools.data.ows.Layer[] layers) {
        List<org.geotools.data.ows.Layer> parents = new ArrayList();

        for (int i = 0; i < layers.length; i++) {
            if (layers[i].getChildren().length > 0 || layers[i].getParent() == null) {
                parents.add(layers[i]);
            }
        }

        return parents;
    }

    private UserLayer createUserLayers(UserService us, org.geotools.data.ows.Layer layer, UserLayer parent) {
        String layerTitle = layer.getTitle();
        String layerName = layer.getName();
        boolean queryable = layer.isQueryable();
        double scaleMin = layer.getScaleDenominatorMin();
        double scaleMax = layer.getScaleDenominatorMax();

        UserLayer ul = new UserLayer();

        ul.setServiceid(us);

        if (layerName != null && !layerName.equals("")) {
            ul.setName(layerName);
        }

        if (layerTitle != null && !layerTitle.equals("")) {
            ul.setTitle(layerTitle);
        }

        ul.setQueryable(queryable);

        if (scaleMin > 0) {
            ul.setScalehint_min(Double.toString(scaleMin));
        }

        if (scaleMax > 0) {
            ul.setScalehint_max(Double.toString(scaleMax));
        }

        List<StyleImpl> styles = layer.getStyles();

        for (StyleImpl style : styles) {
            String styleName = style.getName();

            if (styleName != null && !styleName.equals("")) {
                UserLayerStyle uls = new UserLayerStyle(ul, styleName);
                ul.addStyle(uls);
            }
        }

        if (parent != null) {
            ul.setParent(parent);
        }

        org.geotools.data.ows.Layer[] childs = layer.getChildren();
        for (int i = 0; i < childs.length; i++) {
            UserLayer child = createUserLayers(us, childs[i], ul);
            us.addLayer(child);
        }

        return ul;
    }

    private void setTree(HttpServletRequest request) throws JSONException, Exception {

        List ctl = SpatialUtil.getValidClusters();

        /* Nodig zodat ongeconfigureerde layers niet getoond worden */
        HibernateUtil.setUseKaartenbalieCluster(false);

        List themalist = getValidThemas(false, ctl, request);
        Map rootClusterMap = getClusterMap(themalist, ctl, null);

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        JSONObject treeObject = createJasonObject(rootClusterMap, user);
        request.setAttribute("tree", treeObject);
    }

    private Map getClusterMap(List themalist, List clusterlist, Clusters rootCluster) throws JSONException, Exception {
        if (themalist == null || clusterlist == null) {
            return null;
        }
        List childrenList = getThemaList(themalist, rootCluster);

        List subclusters = null;
        Iterator it = clusterlist.iterator();
        while (it.hasNext()) {
            Clusters cluster = (Clusters) it.next();
            if (rootCluster == cluster.getParent()) {
                Map clusterMap = getClusterMap(themalist, clusterlist, cluster);
                if (clusterMap == null || clusterMap.isEmpty()) {
                    continue;
                }
                if (subclusters == null) {
                    subclusters = new ArrayList();
                }
                subclusters.add(clusterMap);
            }
        }

        if ((childrenList == null || childrenList.isEmpty()) && ((subclusters == null || subclusters.isEmpty()))) {
            return null;
        }
        Map clusterNode = new HashMap();
        clusterNode.put("subclusters", subclusters);
        clusterNode.put("children", childrenList);
        clusterNode.put("cluster", rootCluster);

        return clusterNode;
    }

    private List getThemaList(List themalist, Clusters rootCluster) throws JSONException, Exception {
        if (themalist == null) {
            return null;
        }
        ArrayList children = null;
        Iterator it = themalist.iterator();
        while (it.hasNext()) {
            Themas thema = (Themas) it.next();
            if (thema.getCluster() == rootCluster) {
                if (children == null) {
                    children = new ArrayList();
                }
                children.add(thema);
            }
        }
        return children;
    }

    protected JSONObject createJasonObject(Map rootClusterMap, GisPrincipal user) throws JSONException {
        JSONObject root = new JSONObject().put("id", "root").put("type", "root").put("title", "root");
        if (rootClusterMap == null || rootClusterMap.isEmpty()) {
            return root;
        }
        List clusterMaps = (List) rootClusterMap.get("subclusters");
        if (clusterMaps == null || clusterMaps.isEmpty()) {
            return root;
        }

        List<UserKaartgroep> groepen = SpatialUtil.getUserKaartGroepen(user.getCode());
        List<UserKaartlaag> lagen = SpatialUtil.getUserKaartLagen(user.getCode());

        root.put("children", getSubClusters(clusterMaps, null, user, 0, groepen, lagen));

        return root;
    }

    private JSONArray getSubClusters(List subclusterMaps, JSONArray clusterArray,
            GisPrincipal user, int order, List<UserKaartgroep> groepen, List<UserKaartlaag> lagen)
            throws JSONException {

        if (subclusterMaps == null) {
            return clusterArray;
        }

        Iterator it = subclusterMaps.iterator();
        while (it.hasNext()) {
            Map clMap = (Map) it.next();

            Clusters cluster = (Clusters) clMap.get("cluster");
            JSONObject jsonCluster = new JSONObject();
            if (cluster.getId() != null) {
                jsonCluster.put("id", cluster.getId().intValue());
            }
            jsonCluster.put("type", "child");

            String titel = cluster.getNaam();
            if (titel == null || titel.equals("")) {
                titel = "(geen naam opgegeven)";
            }

            jsonCluster.put("title", titel);
            jsonCluster.put("cluster", true);

            if (cluster.isExclusive_childs()) {
                jsonCluster.put("exclusive_childs", true);
            } else {
                jsonCluster.put("exclusive_childs", false);
            }
            setExtraClusterProperties(jsonCluster, cluster);

            jsonCluster.put("visible", true);

            if (cluster.getMetadatalink() != null) {
                String metadatalink = cluster.getMetadatalink();
                metadatalink = metadatalink.replaceAll("%id%", "" + cluster.getId());
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            /* kijken of cluster voorkomt in user kaartgroepen */
            jsonCluster.put("groepSelected", false);
            jsonCluster.put("groupDefaultOn", false);

            for (UserKaartgroep kaartGroep : groepen) {
                if (cluster.getId().intValue() == kaartGroep.getClusterid().intValue()) {
                    jsonCluster.put("groepSelected", true);

                    if (kaartGroep.getDefault_on()) {
                        jsonCluster.put("groupDefaultOn", true);
                    }
                }
            }

            List childrenList = (List) clMap.get("children");

            JSONArray childrenArray = new JSONArray();
            order = getChildren(childrenArray, childrenList, user, order, lagen);
            List subsubclusterMaps = (List) clMap.get("subclusters");
            childrenArray = getSubClusters(subsubclusterMaps, childrenArray, user, order, groepen, lagen);
            jsonCluster.put("children", childrenArray);

            if (clusterArray == null) {
                clusterArray = new JSONArray();
            }
            clusterArray.put(jsonCluster);

        }
        return clusterArray;
    }

    private void setExtraClusterProperties(JSONObject jsonCluster, Clusters cluster) throws JSONException {
        if (cluster.isDefault_cluster()) {
            jsonCluster.put("default_cluster", true);
        } else {
            jsonCluster.put("default_cluster", false);
        }
        if (cluster.isHide_legend()) {
            jsonCluster.put("hide_legend", true);
        } else {
            jsonCluster.put("hide_legend", false);
        }
        if (cluster.isHide_tree()) {
            jsonCluster.put("hide_tree", true);
        } else {
            jsonCluster.put("hide_tree", false);
        }
        if (cluster.isBackground_cluster()) {
            jsonCluster.put("background", true);
        } else {
            jsonCluster.put("background", false);
        }
        if (cluster.isExtra_level()) {
            jsonCluster.put("extra_level", true);
        } else {
            jsonCluster.put("extra_level", false);
        }
        if (cluster.isCallable()) {
            jsonCluster.put("callable", true);
        } else {
            jsonCluster.put("callable", false);
        }
    }

    private int getChildren(JSONArray childrenArray, List children, GisPrincipal user,
            int order, List<UserKaartlaag> lagen) throws JSONException {

        if (children == null || childrenArray == null) {
            return order;
        }

        Iterator it = children.iterator();
        while (it.hasNext()) {
            Themas th = (Themas) it.next();

            /* TODO: validAdmindataSource ging eerst via th.hasValidAdmindataSource(user)
             * maar dit duurt soms erg lang, nu wordt er gekeken of er een gegevensbron is */
            boolean validAdmindataSource = false;
            Gegevensbron themaGb = th.getGegevensbron();

            if (themaGb != null && themaGb.getAdmin_pk() != null) {
                validAdmindataSource = true;
            }

            // Check of er een admin source is met rechten
            if (th.isAnalyse_thema() && !validAdmindataSource) {
                log.debug(th.getNaam() + "' is analyse kaartlaag maar"
                        + " hier is nog geen gegevensbron voor geconfigureerd.");
            }

            Integer themaId = th.getId();
            String ttitel = th.getNaam();

            if (ttitel == null || ttitel.equals("")) {
                ttitel = "(geen naam opgegeven)";
            }

            JSONObject jsonCluster = new JSONObject().put("id", themaId).put("type", "child").put("title", ttitel).put("cluster", false);

            order++;
            jsonCluster.put("order", order);

            if (th.getOrganizationcodekey() != null && th.getOrganizationcodekey().length() > 0) {
                jsonCluster.put("organizationcodekey", th.getOrganizationcodekey().toUpperCase());
            } else {
                jsonCluster.put("organizationcodekey", "");
            }

            if (th.getMaptipstring() != null) {
                jsonCluster.put("maptipfield", th.getMaptipstring());
            }

            jsonCluster.put("visible", "on");
            jsonCluster.put("analyse", "off");

            /* Extra property die gebruikt wordt voor highLightThemaObject
             * deze gebruikte eerste analyse property maar de highlight knop
             * hoeft nu alleen maar te highlighten en geen info meer op te halen.
             * Dus analyse moet ook aanstaan als er geen valdAdmindatasource is.
             */
            if (th.isAnalyse_thema()) {
                jsonCluster.put("highlight", "on");
            } else {
                jsonCluster.put("highlight", "off");
            }

            /* kijken of thema voorkomt in user kaartlagen */
            jsonCluster.put("kaartSelected", false);
            jsonCluster.put("kaartDefaultOn", false);

            for (UserKaartlaag kaartlaag : lagen) {
                if (th.getId().intValue() == kaartlaag.getThemaid().intValue()) {
                    jsonCluster.put("kaartSelected", true);

                    if (kaartlaag.getDefault_on()) {
                        jsonCluster.put("kaartDefaultOn", true);
                    }
                }
            }

            /*Set some cluster properties that are used by the thema.*/
            Clusters cluster = th.getCluster();
            if (cluster.getId() != null) {
                jsonCluster.put("clusterid", "c" + cluster.getId().intValue());
            }
            setExtraClusterProperties(jsonCluster, cluster);

            Layer layer = null;
            if (th.getWms_layers_real() != null) {
                jsonCluster.put("wmslayers", th.getWms_layers_real());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers_real() != null && !validAdmindataSource) {
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers_real());
                }
                if (th.getWms_legendlayer_real() != null) {
                    jsonCluster.put("legendurl", user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer_real()), th.getStyle()));
                }
                layer = user.getLayer(th.getWms_layers_real());
            } else {
                jsonCluster.put("wmslayers", th.getWms_layers());
                //if admintable is set then don't add the queryLayer
                if (th.getWms_querylayers() != null && !validAdmindataSource) {
                    jsonCluster.put("wmsquerylayers", th.getWms_querylayers());
                }
                if (th.getWms_legendlayer() != null) {
                    jsonCluster.put("legendurl", user.getLegendGraphicUrl(user.getLayer(th.getWms_legendlayer()), th.getStyle()));
                }
                layer = user.getLayer(th.getWms_layers());
            }
            //toevoegen scale hints
            if (layer != null) {
                NumberFormat formatter = new DecimalFormat("#.#####");

                double shmax = -1.0;
                try {
                    shmax = Double.parseDouble(layer.getScaleHintMax());
                } catch (NumberFormatException nfe) {
                    log.debug("max scale hint not valid: " + layer.getScaleHintMax());
                }
                if (shmax > 0) {
                    jsonCluster.put("scalehintmax", formatter.format(shmax));
                }
                double shmin = -1.0;
                try {
                    shmin = Double.parseDouble(layer.getScaleHintMin());
                } catch (NumberFormatException nfe) {
                    log.debug("min scale hint not valid: " + layer.getScaleHintMin());
                }
                if (shmin > 0) {
                    jsonCluster.put("scalehintmin", formatter.format(shmin));
                }
            }
            if (th.getMetadata_link() != null) {
                String metadatalink = th.getMetadata_link();
                metadatalink = metadatalink.replaceAll("%id%", "" + themaId);
                jsonCluster.put("metadatalink", metadatalink);
            } else {
                jsonCluster.put("metadatalink", "#");
            }

            childrenArray.put(jsonCluster);
        }

        return order;
    }

    private UserKaartgroep getUserKaartGroep(String code, Integer clusterId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List groepen = sess.createQuery("from UserKaartgroep where code = :code and"
                + " clusterid = :clusterid").setParameter("code", code).setParameter("clusterid", clusterId).setMaxResults(1).list();

        if (groepen != null && groepen.size() == 1) {
            return (UserKaartgroep) groepen.get(0);
        }

        return null;
    }

    private UserKaartlaag getUserKaartlaag(String code, Integer themaId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List groepen = sess.createQuery("from UserKaartlaag where code = :code and"
                + " themaid = :themaid").setParameter("code", code).setParameter("themaid", themaId).setMaxResults(1).list();

        if (groepen != null && groepen.size() == 1) {
            return (UserKaartlaag) groepen.get(0);
        }

        return null;
    }

    private UserLayer getUserLayerById(Integer layerId) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List layers = sess.createQuery("from UserLayer where id = :id").setParameter("id", layerId).setMaxResults(1).list();

        if (layers != null && layers.size() == 1) {
            return (UserLayer) layers.get(0);
        }

        return null;
    }

    private boolean isKaartGroepDefaultOn(String[] kaartgroepenDefaultAan, Integer clusterId) {

        boolean defaultOn = false;

        for (int i = 0; i < kaartgroepenDefaultAan.length; i++) {
            Integer defaultOnId = new Integer(kaartgroepenDefaultAan[i]);

            if (defaultOnId.intValue() == clusterId.intValue()) {
                defaultOn = true;
                break;
            }
        }

        return defaultOn;
    }

    private boolean isKaartlaagDefaultOn(String[] kaartlagenDefaultAan, Integer themaId) {

        boolean defaultOn = false;

        for (int i = 0; i < kaartlagenDefaultAan.length; i++) {
            Integer defaultOnId = new Integer(kaartlagenDefaultAan[i]);

            if (defaultOnId.intValue() == themaId.intValue()) {
                defaultOn = true;
                break;
            }
        }

        return defaultOn;
    }

    private void removeExistingUserKaartgroepAndUserKaartlagen(String code) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserKaartgroep> groepen = sess.createQuery("from UserKaartgroep where code = :code").setParameter("code", code).list();

        List<UserKaartlaag> lagen = sess.createQuery("from UserKaartlaag where code = :code").setParameter("code", code).list();

        for (UserKaartgroep groep : groepen) {
            sess.delete(groep);
        }

        for (UserKaartlaag laag : lagen) {
            sess.delete(laag);
        }
    }

    private void resetExistingUserLayers(String code) {
        List<UserService> services = SpatialUtil.getUserServices(code);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        for (UserService service : services) {
            Set<UserLayer> userLayers = service.getUser_layers();

            for (UserLayer layer : userLayers) {
                layer.setDefault_on(null);
                layer.setShow(null);

                sess.merge(layer);
            }
        }
    }

    private String[] addDefaultOnValues(String[] defaults, String[] current) {
        List col2 = new ArrayList();

        /* eerst huidige waardes erin stoppen */
        col2.addAll(Arrays.asList(current));

        /* daarna de nieuwe default waardes toevoegen */
        for (int j = 0; j < defaults.length; j++) {
            if (!col2.contains(defaults[j])) {
                col2.add(defaults[j]);
            }
        }

        return (String[]) col2.toArray(new String[0]);
    }

    private void setUserviceTrees(HttpServletRequest request) throws JSONException, Exception {
        List<JSONObject> servicesTrees = new ArrayList();

        /* user services ophalen */
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        String code = user.getCode();

        List<UserService> services = SpatialUtil.getUserServices(code);

        /* per service een tree maken */
        for (UserService service : services) {
            JSONObject tree = createUserServiceTree(service);
            servicesTrees.add(tree);
        }

        /* lijst van tree's klaarzetten */
        request.setAttribute("servicesTrees", servicesTrees);
    }

    protected JSONObject createUserServiceTree(UserService service) throws JSONException, Exception {
        JSONObject root = new JSONObject();

        root.put("id", "0");
        root.put("title", service.getGroupname());
        root.put("name", service.getGroupname());
        root.put("serviceid", service.getId());

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List ctl = sess.createQuery("from UserLayer where serviceid = :service"
                + " order by name, title, id").setParameter("service", service).list();

        Map rootLayerMap = getUserLayersMap(ctl, null);
        List lMaps = (List) rootLayerMap.get("sublayers");

        root.put("children", getSubLayers(lMaps, null));

        return root;
    }

    private Map getUserLayersMap(List layerList, UserLayer root) throws JSONException, Exception {
        if (layerList == null) {
            return null;
        }

        List subLayers = null;
        Iterator it = layerList.iterator();
        while (it.hasNext()) {
            UserLayer la = (UserLayer) it.next();
            if (root == la.getParent()) {
                Map lMap = getUserLayersMap(layerList, la);
                if (lMap == null || lMap.isEmpty()) {
                    continue;
                }
                if (subLayers == null) {
                    subLayers = new ArrayList();
                }
                subLayers.add(lMap);
            }
        }

        Map lNode = new HashMap();
        lNode.put("sublayers", subLayers);
        lNode.put("userlayer", root);

        return lNode;
    }

    private JSONArray getSubLayers(List subLayers, JSONArray layersArray) throws JSONException {
        if (subLayers == null) {
            return layersArray;
        }

        Iterator it = subLayers.iterator();
        while (it.hasNext()) {
            Map lMap = (Map) it.next();

            UserLayer layer = (UserLayer) lMap.get("userlayer");

            JSONObject jsonLayer = new JSONObject();

            jsonLayer.put("id", layer.getId().intValue());
            jsonLayer.put("serviceid", layer.getServiceid().getId().intValue());
            jsonLayer.put("title", layer.getTitle());
            jsonLayer.put("name", layer.getName());
            jsonLayer.put("queryable", layer.getQueryable());
            jsonLayer.put("scalehintmin", layer.getScalehint_min());
            jsonLayer.put("scalehintmax", layer.getScalehint_max());
            jsonLayer.put("use_style", layer.getUse_style());
            jsonLayer.put("sld_part", layer.getSld_part());
            jsonLayer.put("show", layer.getShow());
            jsonLayer.put("default_on", layer.getDefault_on());

            JSONArray styles = getLayerStyles(layer);
            if (styles != null && styles.length() > 0) {
                jsonLayer.put("styles", styles);
            }

            List subsubMaps = (List) lMap.get("sublayers");

            if (subsubMaps != null && !subsubMaps.isEmpty()) {
                JSONArray childrenArray = new JSONArray();

                childrenArray = getSubLayers(subsubMaps, childrenArray);
                jsonLayer.put("children", childrenArray);
            }

            if (layersArray == null) {
                layersArray = new JSONArray();
            }

            layersArray.put(jsonLayer);
        }

        return layersArray;
    }

    private JSONArray getLayerStyles(UserLayer layer) throws JSONException {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List<UserLayerStyle> styles = sess.createQuery("from UserLayerStyle where"
                + " layerid = :layer").setParameter("layer", layer).list();

        JSONArray arr = new JSONArray();
        for (UserLayerStyle uls : styles) {
            arr.put(uls.getName());
        }

        return arr;
    }

    private boolean userAlreadyHasThisService(String code, String url) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<UserService> services = sess.createQuery("from UserService where"
                + " code = :code and url = :url").setParameter("code", code).setParameter("url", url).setMaxResults(1).list();

        if (services != null && services.size() == 1) {
            return true;
        }

        return false;
    }
    
    private String getUniqueStyleName(Set<Style> styles, String name) throws Exception {        
        return getUniqueStyleName(styles,name,null);
    }
    
    private String getUniqueStyleName(Set<Style> styles, String name, Integer tries) throws Exception {    
        if (tries!=null && tries==10)
            throw new Exception("Can't create unique name for style");
                
        String newName=name;
        if (tries!=null)
            newName+=tries;
        
        Iterator<Style> it = styles.iterator();
        boolean unique=true;
        while (it.hasNext()&& unique){
            Style s= it.next();
            if (s.getName().equals(newName))
                unique=false;
        }
        if (!unique){
            if (tries==null)
                tries= new Integer("0");
            tries++;            
            return getUniqueStyleName(styles, name, tries);
        }
        return newName;
    }
    
    protected OGCRequest checkWmsUrl(String url) throws Exception {
        OGCRequest ogcrequest = new OGCRequest(url);
        if (ogcrequest.containsParameter(OGCConstants.WMS_REQUEST)
                && !OGCConstants.WMS_REQUEST_GetCapabilities.equalsIgnoreCase(ogcrequest.getParameter(OGCConstants.WMS_REQUEST))) {
            log.error(KBConfiguration.UNSUPPORTED_REQUEST);
            throw new Exception(KBConfiguration.UNSUPPORTED_REQUEST);
        } else {
            ogcrequest.addOrReplaceParameter(OGCConstants.WMS_REQUEST, OGCConstants.WMS_REQUEST_GetCapabilities);
        }
        if (ogcrequest.containsParameter(OGCConstants.WMS_SERVICE)
                && !OGCConstants.WMS_SERVICE_WMS.equalsIgnoreCase(ogcrequest.getParameter(OGCConstants.WMS_SERVICE))) {
            log.error(KBConfiguration.UNSUPPORTED_SERVICE);
            throw new Exception(KBConfiguration.UNSUPPORTED_SERVICE);
        } else {
            ogcrequest.addOrReplaceParameter(OGCConstants.WMS_SERVICE, OGCConstants.WMS_SERVICE_WMS);
        }

        if (ogcrequest.containsParameter(OGCConstants.WMS_VERSION)
                && !OGCConstants.WMS_VERSION_111.equalsIgnoreCase(ogcrequest.getParameter(OGCConstants.WMS_VERSION))) {
            log.error(KBConfiguration.UNSUPPORTED_VERSION);
            throw new Exception(KBConfiguration.UNSUPPORTED_VERSION);
        } else {
            ogcrequest.addOrReplaceParameter(OGCConstants.WMS_VERSION, OGCConstants.WMS_VERSION_111);
        }

        return ogcrequest;
    }
}
