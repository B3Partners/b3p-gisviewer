package nl.b3p.gis.viewer.db;

import java.util.Set;
import java.sql.Connection;
import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Themas implements Comparable {

    private static final Log log = LogFactory.getLog(Themas.class);
    public static final String THEMAID = "themaid";
    private Integer id;
    private String code;
    private String naam;
    private String metadata_link;
    private int belangnr;
    private Clusters cluster;
    private String opmerkingen;
    private boolean analyse_thema;
    private boolean locatie_thema;
    private boolean visible = false;
    private String wms_url;
    private String wms_layers;
    private String wms_layers_real;
    private String wms_querylayers;
    private String wms_querylayers_real;
    private String wms_legendlayer;
    private String wms_legendlayer_real;
    private Set themaVerantwoordelijkheden;
    private Set themaApplicaties;
    private Integer update_frequentie_in_dagen;
    private String view_geomtype;
    private String organizationcodekey;
    private String maptipstring;
    private String sldattribuut;
    private boolean uitgebreid;
    private String layoutadmindata;
    private String style;
    private Gegevensbron gegevensbron;
    private String info_tekst;

    /** Creates a new instance of Themas */
    public Themas() {
    }

    public boolean isAnalyse_thema() {
        return analyse_thema;
    }

    public void setAnalyse_thema(boolean analyse_thema) {
        this.analyse_thema = analyse_thema;
    }

    public int getBelangnr() {
        return belangnr;
    }

    public void setBelangnr(int belangnr) {
        this.belangnr = belangnr;
    }

    public Clusters getCluster() {
        return cluster;
    }

    public void setCluster(Clusters cluster) {
        this.cluster = cluster;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Gegevensbron getGegevensbron() {
        return gegevensbron;
    }

    public void setGegevensbron(Gegevensbron gegevensbron) {
        this.gegevensbron = gegevensbron;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLayoutadmindata() {
        return layoutadmindata;
    }

    public void setLayoutadmindata(String layoutadmindata) {
        this.layoutadmindata = layoutadmindata;
    }

    public boolean isLocatie_thema() {
        return locatie_thema;
    }

    public void setLocatie_thema(boolean locatie_thema) {
        this.locatie_thema = locatie_thema;
    }

    public String getMaptipstring() {
        return maptipstring;
    }

    public void setMaptipstring(String maptipstring) {
        this.maptipstring = maptipstring;
    }

    public String getMetadata_link() {
        return metadata_link;
    }

    public void setMetadata_link(String metadata_link) {
        this.metadata_link = metadata_link;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getOpmerkingen() {
        return opmerkingen;
    }

    public void setOpmerkingen(String opmerkingen) {
        this.opmerkingen = opmerkingen;
    }

    public String getOrganizationcodekey() {
        return organizationcodekey;
    }

    public void setOrganizationcodekey(String organizationcodekey) {
        this.organizationcodekey = organizationcodekey;
    }

    public String getSldattribuut() {
        return sldattribuut;
    }

    public void setSldattribuut(String sldattribuut) {
        this.sldattribuut = sldattribuut;
    }

    public Set getThemaApplicaties() {
        return themaApplicaties;
    }

    public void setThemaApplicaties(Set themaApplicaties) {
        this.themaApplicaties = themaApplicaties;
    }

    public Set getThemaVerantwoordelijkheden() {
        return themaVerantwoordelijkheden;
    }

    public void setThemaVerantwoordelijkheden(Set themaVerantwoordelijkheden) {
        this.themaVerantwoordelijkheden = themaVerantwoordelijkheden;
    }

    public boolean isUitgebreid() {
        return uitgebreid;
    }

    public void setUitgebreid(boolean uitgebreid) {
        this.uitgebreid = uitgebreid;
    }

    public Integer getUpdate_frequentie_in_dagen() {
        return update_frequentie_in_dagen;
    }

    public void setUpdate_frequentie_in_dagen(Integer update_frequentie_in_dagen) {
        this.update_frequentie_in_dagen = update_frequentie_in_dagen;
    }

    public String getView_geomtype() {
        return view_geomtype;
    }

    public void setView_geomtype(String view_geomtype) {
        this.view_geomtype = view_geomtype;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getWms_layers() {
        return wms_layers;
    }

    public void setWms_layers(String wms_layers) {
        this.wms_layers = wms_layers;
    }

    public String getWms_layers_real() {
        return wms_layers_real;
    }

    public void setWms_layers_real(String wms_layers_real) {
        this.wms_layers_real = wms_layers_real;
    }

    public String getWms_legendlayer() {
        return wms_legendlayer;
    }

    public void setWms_legendlayer(String wms_legendlayer) {
        this.wms_legendlayer = wms_legendlayer;
    }

    public String getWms_legendlayer_real() {
        return wms_legendlayer_real;
    }

    public void setWms_legendlayer_real(String wms_legendlayer_real) {
        this.wms_legendlayer_real = wms_legendlayer_real;
    }

    public String getWms_querylayers() {
        return wms_querylayers;
    }

    public void setWms_querylayers(String wms_querylayers) {
        this.wms_querylayers = wms_querylayers;
    }

    public String getWms_querylayers_real() {
        return wms_querylayers_real;
    }

    public void setWms_querylayers_real(String wms_querylayers_real) {
        this.wms_querylayers_real = wms_querylayers_real;
    }

    public String getWms_url() {
        return wms_url;
    }

    public void setWms_url(String wms_url) {
        this.wms_url = wms_url;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
    
    public String getInfo_tekst() {
        return info_tekst;
    }

    public void setInfo_tekst(String info_tekst) {
        this.info_tekst = info_tekst;
    }

    public int compareTo(Object o) {
        if (!(o instanceof Themas)) {
            throw new ClassCastException("A Themas object expected.");
        }
        int ob = ((Themas) o).getBelangnr();
        int tb = this.getBelangnr();

        String on = ((Themas) o).getNaam();
        String tn = this.getNaam();

        int verschil = tb - ob;
        if (verschil != 0 || on == null || tn == null) {
            return verschil;
        }
        return tn.compareTo(on);
    }

    public Bron getConnectie(HttpServletRequest request) {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        return getConnectie(user);
    }

    public Bron getConnectie(GisPrincipal user) {
        Gegevensbron gb = getGegevensbron();
        Bron b = null;

        if (gb != null) {
            b = gb.getBron();
        }

        if (b == null && user != null) {
            b = user.getKbWfsConnectie();
        }

        return b;
    }

    public Connection getJDBCConnection() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean hasValidAdmindataSource(GisPrincipal user) {
        if (getGegevensbron() == null) {
            return false;
        }

        Gegevensbron gb = getGegevensbron();
        Bron b = gb.getBron();

        if (b == null) {
            // moet kaartenbalie wfs zijn want geen bron
            // we zoeken naar featuretype met zelfde naam
            return user.acceptWfsFeatureType(gb.getAdmin_tabel());
        } else {
            // andere externe bronnen checken we niet, mogelijk later
            return true;
        }
    }

    public static class NameComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (!(o1 instanceof Themas)) {
                throw new ClassCastException("A Themas object 1 expected.");
            }
            if (!(o2 instanceof Themas)) {
                throw new ClassCastException("A Themas object 2 expected.");
            }

            String o1n = ((Themas) o1).getNaam();
            String o2n = ((Themas) o2).getNaam();

            return o1n.compareTo(o2n);
        }
    }
}
