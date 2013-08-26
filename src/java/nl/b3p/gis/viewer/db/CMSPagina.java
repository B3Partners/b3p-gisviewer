package nl.b3p.gis.viewer.db;

import java.util.Date;
import nl.b3p.gis.utils.Slugify;
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
    private Boolean showPlainAndMapButton;
    private Date cdate;    
    private String sefUrl;    
    private Integer cmsMenu;
    private Boolean loginRequired;

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

    public Boolean getShowPlainAndMapButton() {
        return showPlainAndMapButton;
    }

    public void setShowPlainAndMapButton(Boolean showPlainAndMapButton) {
        this.showPlainAndMapButton = showPlainAndMapButton;
    }

    public String getSefUrl() {
        return Slugify.slugify(getTitel());
    }

    public void setSefUrl(String sefUrl) {
        this.sefUrl = sefUrl;
    }

    public Integer getCmsMenu() {
        return cmsMenu;
    }

    public void setCmsMenu(Integer cmsMenu) {
        this.cmsMenu = cmsMenu;
    }

    public Boolean getLoginRequired() {
        return loginRequired;
    }

    public void setLoginRequired(Boolean loginRequired) {
        this.loginRequired = loginRequired;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject me = new JSONObject();
        
        me.put("id", this.getId());
        me.put("titel", this.getTitel());
        me.put("tekst", this.getTekst());
        me.put("thema", this.getThema());
        me.put("showPlainAndMapButton", this.getShowPlainAndMapButton());
        me.put("cdate", this.getCdate());
        me.put("sefUrl", this.getSefUrl());
        me.put("cmsMenu", this.getCmsMenu());
        me.put("loginRequired", this.getLoginRequired());
        
        return me;
    }
}