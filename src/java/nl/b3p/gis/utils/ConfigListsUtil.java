/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.gis.utils;

import nl.b3p.gis.geotools.DataStoreUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.geotools.data.DataStore;
import org.hibernate.Transaction;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

/**
 *
 * @author Roy
 */
public class ConfigListsUtil {

    private static final Log log = LogFactory.getLog(ConfigListsUtil.class);

    private static final StringArrayComperator stringArrayComperator= new StringArrayComperator();
    
    private static Bron getBron(Session sess, Integer bronId) {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        return getBron(sess, user, bronId);
    }

    public static Bron getBron(Session sess, GisPrincipal user, Integer bronId) {
        Bron b = null;
        if (bronId == null || bronId.intValue() == 0) {
            b = user.getKbWfsConnectie();
        } else if (bronId.intValue() > 0) {
            b = (Bron) sess.get(Bron.class, bronId);
        }
        return b;
    }

    public static List getPossibleFeaturesById(Integer connId) {
        List l = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;

        Bron b = null;

        try {
            tx = sess.beginTransaction();

            b = getBron(sess, connId);
            l = getPossibleFeatures(b);

            tx.commit();
        } catch (Exception e) {
            log.error("Fout tijdens ophalen attributen o.b.v. id: ", e);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }

        if (l == null && b != null) {
            String serviceUrl = b.getUrl();

            if (serviceUrl != null) {
                l = new ArrayList<String>();
                l.add("SERVICE_ERROR");
                l.add(serviceUrl);
            }
        }

        return l;
    }

    public static List getPossibleAttributesById(Integer connId, String feature) {
        List l = null;
        
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;

        try {
            tx = sess.beginTransaction();

            Bron b = getBron(sess, connId);

            if (b != null) {
                l = getPossibleAttributes(b, feature);
            }

            tx.commit();
        } catch (Exception e) {
            log.error("Fout tijdens ophalen attributen: ", e);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }

        return l;
    }

    /**
     * Maakt een lijst met mogelijke features voor de gegeven wfs connectie en gebruiker
     */
    public static List getPossibleFeatures(Bron b) throws Exception {
        if (b == null) {
            return null;
        }
        ArrayList returnValue = null;
        DataStore ds= b.toDatastore();
        try{
            Name[] features = DataStoreUtil.getTypeNames(ds);
            if (features != null) {
                returnValue = new ArrayList();
                for (int i = 0; i < features.length; i++) {
                    String[] s = new String[2];
                    String nsu = features[i].getNamespaceURI();
                    if (nsu != null && nsu.length()!=0) {
                        s[0] = "{" + features[i].getNamespaceURI() + "}";
                    } else {
                        s[0] = "";
                    }
                        s[0] += features[i].getLocalPart();
                    s[1] = features[i].getLocalPart();
                    returnValue.add(s);
                }
            }
        }finally{
            ds.dispose();
        }
        Collections.sort(returnValue,stringArrayComperator);
        return returnValue;
    }
    /**
     * Maakt een lijst met mogelijke attributen van een meegegeven featureType.
     */
    public static List getPossibleAttributes(Bron b, String type) throws Exception {
        if (b == null || type == null) {
            return null;
        }
        List returnValue = new ArrayList();
        DataStore ds=b.toDatastore();
        returnValue=DataStoreUtil.getAttributeNames(ds, type);
        returnValue = new ArrayList();
        try{
            SimpleFeatureType sft=DataStoreUtil.getSchema(ds, type);
            List<AttributeDescriptor> attributes=sft.getAttributeDescriptors();
            Iterator<AttributeDescriptor> it= attributes.iterator();
            while(it.hasNext()){
                AttributeDescriptor attribute=it.next();
                String[] s = new String[2];
                if (attribute.getName().getNamespaceURI()!=null) {
                    s[0]  = "{" + attribute.getName().getNamespaceURI() + "}";
                } else {
                    s[0] = "";
                }
                s[0] += attribute.getLocalName();
                s[1] = attribute.getLocalName();
                returnValue.add(s);
            }
        }finally{
            ds.dispose();
        }
        return returnValue;
    }    
    
    private static class StringArrayComperator implements Comparator{
        public int compare(Object o1, Object o2) {
            if (o1 instanceof String[] && o2 instanceof String[]){
                String[] s1=(String[])o1;
                String[] s2=(String[])o2;
                //compare alle strings met elkaar
                for (int i=0; i < s1.length; i++){
                    int compare=s1[i].compareToIgnoreCase(s2[i]);
                    if (compare!=0){
                        return compare;
                    }
                }
                //return of de ene langer is dan de ander
                return s1.length - s2.length;
            }
            return 0;
        }
         
    }

}
