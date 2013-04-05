package nl.b3p.gis.viewer.db;

import java.util.Date;

/**
 *
 * @author Boy de Wit
 */
public class Tekstblok {
    private Integer id;
    private String titel;
    private String tekst;
    private String url;
    private Boolean toonUrl;
    private String pagina;
    private Integer volgordeNr;
    private String kleur;
    private String auteur;
    private Date cdate;
    private Boolean inlogIcon;
    private Integer hoogte;

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public Date getCdate() {
        return cdate;
    }

    public void setCdate(Date cdate) {
        this.cdate = cdate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPagina() {
        return pagina;
    }

    public void setPagina(String pagina) {
        this.pagina = pagina;
    }

    public String getTekst() {
        return tekst;
    }

    public void setTekst(String tekst) {
        this.tekst = tekst;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public Boolean getToonUrl() {
        return toonUrl;
    }

    public void setToonUrl(Boolean toonUrl) {
        this.toonUrl = toonUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getVolgordeNr() {
        return volgordeNr;
    }

    public void setVolgordeNr(Integer volgordeNr) {
        this.volgordeNr = volgordeNr;
    }

    public String getKleur() {
        return kleur;
    }

    public void setKleur(String kleur) {
        this.kleur = kleur;
    }
    
    public Boolean getInlogIcon() {
        return inlogIcon;
    }

    public void setInlogIcon(Boolean inlogIcon) {
        this.inlogIcon = inlogIcon;
    }

    public Integer getHoogte() {
        return hoogte;
    }

    public void setHoogte(Integer hoogte) {
        this.hoogte = hoogte;
    }

}
