/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.gis.viewer;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.db.Connecties;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.gis.viewer.services.WfsUtil;
import nl.b3p.xml.wfs.WFS_Capabilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.w3c.dom.Element;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

/**
 *
 * @author Roy
 */
public class ConfigListsUtil {
    private static final Log log = LogFactory.getLog(ConfigListsUtil.class);

    public static List getPossibleFeaturesById(Integer connId){
        List returnValue = null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        try{
            sess.beginTransaction();
            Connecties c=null;
            if (connId!=null){
                c = (Connecties) sess.get(Connecties.class, connId);
            }else{
                WebContext ctx = WebContextFactory.get();
                HttpServletRequest request = ctx.getHttpServletRequest();
                GisPrincipal user = GisPrincipal.getGisPrincipal(request);
                c= user.getKbWfsConnectie();
            }
            if (c!=null)
                returnValue= getPossibleFeatures(c);
        }catch (Exception e){
            log.error("getPossibleFeaturesById error: ",e);
        }finally{
            sess.close();
        }
        return returnValue;
    }

    public static List getPossibleAttributesById(Integer connId, String feature){
        List returnValue = null;
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        try{
            sess.beginTransaction();
            Connecties c=null;
            if (connId!=null){
                c = (Connecties) sess.get(Connecties.class, connId);
            }else{
                WebContext ctx = WebContextFactory.get();
                HttpServletRequest request = ctx.getHttpServletRequest();
                GisPrincipal user = GisPrincipal.getGisPrincipal(request);
                c= user.getKbWfsConnectie();
            }
            if (c!=null)
                returnValue= getPossibleAttributes(c,feature);
        }catch (Exception e){
            log.error("getPossibleAttributesById error: ",e);
        }finally{
            sess.close();
        }
        return returnValue;
    }
    /**
     * Maakt een lijst met mogelijke features voor de gegeven connectie en gebruiker
     * Zowel jdbc en wfs
     */
    public static List getPossibleFeatures(Connecties c) throws Exception{
        ArrayList returnValue=null;
        if (SpatialUtil.validJDBCConnection(c)) {
            Connection conn=null;           
            try{
                conn = c.getJdbcConnection();
                List features=SpatialUtil.getTableNames(conn);
                if (features!=null)
                    returnValue=new ArrayList();
                for (int i=0; i < features.size(); i++){
                    String[] s = new String[2];
                    s[0]=(String)features.get(i);
                    s[1]=BaseGisAction.removeNamespace((String)features.get(i));
                    returnValue.add(s);
                }
            }finally{
                if (conn!=null)
                    conn.close();
            }
            return returnValue;
        }else if (WfsUtil.validWfsConnection(c)) {
            WFS_Capabilities cap=null;
            try{
                cap = WfsUtil.getCapabilities(c);
            }catch(Exception e){
                log.error("fout bij ophalen capabilities",e);
            }
            List features= WfsUtil.getFeatureNameList(cap);
            if (features!=null){
                returnValue=new ArrayList();
                for (int i=0; i < features.size(); i++){
                    String[] s = new String[2];
                    s[0]=(String)features.get(i);
                    s[1]=BaseGisAction.removeNamespace((String)features.get(i));
                    returnValue.add(s);
                }
            }
            return returnValue;
        }else{
            return null;
        }
    }
    /**
     * Maakt een lijst met mogelijke attributen van een meegegeven feature.
     * Zowel jdbc en wfs
     */
    public static List getPossibleAttributes(Connecties c, String feature) throws Exception {
        ArrayList returnValue=null;
        if (feature == null) {
            return null;
        }else if (SpatialUtil.validJDBCConnection(c)) {
            Connection conn=null;            
            try{
                conn = c.getJdbcConnection();
                List columns=SpatialUtil.getColumnNames(feature, conn);
                if (columns==null)
                    return null;
                returnValue=new ArrayList();
                for (int i=0; i < columns.size(); i++){
                    String[] s = new String[2];
                    s[0]=(String)columns.get(i);
                    s[1]=BaseGisAction.removeNamespace((String)columns.get(i));
                    returnValue.add(s);
                }
            }finally{
                if (conn!=null)
                    conn.close();
            }
            return returnValue;
        }else if (WfsUtil.validWfsConnection(c)) {
            List elements = WfsUtil.getFeatureElements(c, feature);
            if (elements == null) {
                return null;
            }
            returnValue = new ArrayList();
            for (int i = 0; i < elements.size(); i++) {
                Element e = (Element) elements.get(i);
                String name=e.getAttribute("name");
                String[] s=new String[2];
                s[0]=name;
                s[1]=BaseGisAction.removeNamespace(name);
                returnValue.add(s);
            }
            return returnValue;
        }else{
            return null;
        }
    }


}
