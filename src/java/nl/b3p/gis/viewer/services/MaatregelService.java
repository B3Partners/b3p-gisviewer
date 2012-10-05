/*
 * Copyright (C) 2012 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.util.List;
import nl.b3p.viewer.openbareruimte.entities.Maatregel;
import nl.b3p.viewer.openbareruimte.entities.MaatregelEigenschap;
import nl.b3p.viewer.openbareruimte.entities.MaatregelGepland;
import nl.b3p.viewer.openbareruimte.entities.RawCrow;
import nl.b3p.viewer.openbareruimte.vragen.Factory;
import nl.b3p.viewer.openbareruimte.vragen.MaatregelForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Roy Braam
 */
public class MaatregelService {
    private static final Log log = LogFactory.getLog(MaatregelService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * Get maatregelen that are possible for the given type.
     */
    public String getMaatregelen(String vlakType) throws JSONException{
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;
        JSONObject response = new JSONObject();     
        response.put("success", false);           
        try {            
            JSONArray array = new JSONArray();
            tx = sess.beginTransaction();
            //List<Maatregel> maatregelen=sess.createCriteria(Maatregel.class).createAlias("VlakMaatregel", "v").add(Restrictions.eq("v.vlakType",vlakType)).list();
            String hql="FROM Maatregel";
            if (vlakType!=null){
                hql+=" m where m.id in (SELECT maatregel from VlakMaatregel where lower(vlakType) = :v)";
            }
            Query q = sess.createQuery(hql);
            if (vlakType!=null){
                q.setParameter("v", vlakType.toLowerCase());
            }
            List<Maatregel> maatregelen=q.list();
            
            for (Maatregel m : maatregelen){
                JSONObject js = new JSONObject();
                js.put("id", m.getId());
                js.put("omschrijving", m.getOmschrijving() );
                array.put(js);
            }
            response.put("results",array);
            sess.flush();
            tx.commit();
            response.put("success",true);                        
        }catch(Exception e){
            log.error("Error while getting maatregelen",e);
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            response.put("error","Error tijdens ophalen maatregelen"+e.getMessage());
        }
        return response.toString();
    }
    /**
     * Get stored maatregelen for feature.
     */
    public String getGeplandeMaatregelen(Integer bronId,String featureId) throws JSONException{
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx=null;
        String error="";
        JSONObject json = new JSONObject();
        json.put("success",false);
        try{
            tx=sess.beginTransaction();
            //MaatregelGepland mg=(MaatregelGepland) sess.get(MaatregelGepland.class, id);            
            List<MaatregelGepland> mgs = sess.createQuery("FROM MaatregelGepland where bronId= :b AND featureId= :f")
                    .setParameter("b", bronId).setParameter("f",featureId).list();
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw,mgs);
            json.put("results",new JSONArray(sw.toString()));            
            sess.flush();
            tx.commit();
            json.put("success",true);
        }catch(Exception e){
            log.error("Error while getting saved maatregel",e);
            if (tx!=null && tx.isActive()){
                tx.rollback();
            }
            json.put("error","error while getting saved maatregel "+e.getMessage());
        }
        return json.toString();
    }
    /**
     * Get questions for maatregel type.
     */
    public String getVragen(String maatregel) throws JSONException {
        JSONObject response = new JSONObject();
        String error = "";
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;
        if (maatregel != null) {
            try {
                tx = sess.beginTransaction();
                response.put("success", false);
                String[] tokens = maatregel.split("\\.");
                if (tokens.length != 3) {
                    error = "Maatregel param moet xx.xx.xx zijn";
                } else {
                    Integer wc = new Integer(tokens[0]);
                    Integer swc = new Integer(tokens[1]);
                    Integer volgnr = new Integer(tokens[2]);
                    Criteria c=sess.createCriteria(RawCrow.class);
                    c.add(Restrictions.eq("wc", wc));
                    c.add(Restrictions.eq("swc", swc));
                    c.add(Restrictions.eq("volgnr", volgnr));
                    c.addOrder(Order.asc("deficode"));
                    c.addOrder(Order.asc("regelnr"));
                    List<RawCrow> list = c.list();
                    
                    MaatregelForm form = Factory.createVragenSorted(list);
                    
                   
                    StringWriter sw = new StringWriter();
                    mapper.writeValue(sw,form);
                    
                    response.put("result",new JSONObject(sw.toString()));                       
                    response.put("success", true);
                }
                tx.commit();
            } catch (Exception e) {
                log.error("Error while getting maatregelen",e);
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                response.put("error","Error tijdens ophalen van vragen"+e.getMessage());
            }
        }
        return response.toString();
    }
    /**
     * Save the maatregel
     */
    public String save(String geplandeMaatregel) throws JSONException{
        JSONObject json = new JSONObject();
        json.put("success",false);
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx=null;
        String error="";
        try{
            tx= sess.beginTransaction();
            JSONObject plan = new JSONObject(geplandeMaatregel);
            //maatregel needed.
            if (!plan.has("maatregel")){
                throw new Exception("No maatregel found!");
            }
            MaatregelGepland mg = null;
            if (plan.has("id")){
                mg = (MaatregelGepland) sess.get(MaatregelGepland.class,plan.getLong("id"));
            }        
            if (mg==null){
                mg=new MaatregelGepland();
            }
            Maatregel m = (Maatregel) sess.get(Maatregel.class,plan.getString("maatregel"));
            mg.setMaatregel(m);
            if (plan.has("hoeveelheid")){                
                mg.setHoeveelheid(plan.getInt("hoeveelheid"));
            }else{
                mg.setHoeveelheid(null);
            }
            if (plan.has("objectType") && plan.get("objectType")!=null){
                mg.setObjectType(plan.getString("objectType"));
            }else{
                mg.setObjectType(null);
            }
            if (plan.has("featureId")){
                mg.setFeatureId(plan.getString("featureId"));
            }
            if (plan.has("bronId")){
                mg.setBronId(plan.getInt("bronId"));
            }
            if (mg.getEigenschappen()!=null){
                for (MaatregelEigenschap eigenschap : mg.getEigenschappen()){
                    sess.delete(eigenschap);
                }                
                mg.setEigenschappen(null);
                sess.flush();                
            }            
            if (plan.has("eigenschappen")){
                JSONArray eigenschappen = plan.getJSONArray("eigenschappen");
                for(int i=0; i < eigenschappen.length(); i++){
                    JSONObject eigenschap = eigenschappen.getJSONObject(i);
                    MaatregelEigenschap me = mapper.readValue(eigenschap.toString(),MaatregelEigenschap.class);
                    mg.addEigenschap(me);
                }
            }
            if (mg.getId()!=null){
                sess.save(mg);
            }else{
                sess.persist(mg);
            }
            sess.flush();
            tx.commit();            
            json.put("success",true);
            json.put("error",error);
        }catch(Exception e){            
            log.error("Error while saving",e);
            if (tx!=null && tx.isActive()){
                tx.rollback();
            }
            json.put("error", "error while saving: "+e.getMessage());
        }
        return json.toString();
    }
    
    /**
     * Delete feature maatregel with id
     */
    public String deleteMaatregel(Long id) throws JSONException{
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx=null;
        JSONObject json = new JSONObject();
        json.put("success",false);
        try{
            tx=sess.beginTransaction();
            MaatregelGepland mg=(MaatregelGepland) sess.get(MaatregelGepland.class, id);            
            sess.delete(mg);
            sess.flush();
            tx.commit();
            json.put("success",true);
        }catch(Exception e){
            log.error("Error while deleting maatregel",e);
            if (tx!=null && tx.isActive()){
                tx.rollback();
            }
            json.put("error","Error while deleting maatregel "+e.getMessage());
        }
        return json.toString();
    }
    /**
     * get stored maatregel
     */
    public String getSavedMaatregel(Long id) throws JSONException{
        Session sess= HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx=null;
        String error="";
        JSONObject json = new JSONObject();
        json.put("success",false);
        try{
            tx=sess.beginTransaction();
            MaatregelGepland mg=(MaatregelGepland) sess.get(MaatregelGepland.class, id);            
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw,mg);
            json.put("result",new JSONObject(sw.toString()));
            json.put("success",true);
            tx.commit();
        }catch(Exception e){
            log.error("Error while getting saved maatregel",e);
            if (tx!=null && tx.isActive()){
                tx.rollback();
            }
            json.put("error","error while getting saved maatregel "+e.getMessage());
        }
        return json.toString();
    }
}
