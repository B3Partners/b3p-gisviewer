package nl.b3p.gis.viewer.db;

import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Boy de Wit
 */
public class CMSPagina {
    private Integer id;
    private String titel;
    private String tekst;
    private String thema;
    private Date cdate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getTekst() {
        return tekst;
    }

    public void setTekst(String tekst) {
        this.tekst = tekst;
    }

    public String getThema() {
        return thema;
    }

    public void setThema(String thema) {
        this.thema = thema;
    }

    public Date getCdate() {
        return cdate;
    }

    public void setCdate(Date cdate) {
        this.cdate = cdate;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject me = new JSONObject();
        
        me.put("id", this.getId());
        me.put("titel", this.getTitel());
        me.put("tekst", this.getTekst());
        me.put("thema", this.getThema());
        me.put("cdate", this.getCdate());
        
        return me;
    }

}
