package nl.b3p.gis.viewer.db;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Gegevensbron implements Comparable {

    private static final Log log = LogFactory.getLog(Gegevensbron.class);

    private Integer id;

    private String naam;
    private Bron bron;
    private String admin_tabel;
    private String admin_pk;
    private String admin_query;
    private Gegevensbron parent;
    private Set children = new HashSet();
    private String admin_fk;
    private String admin_tabel_opmerkingen;

    private Set themaData;
    private Set themas;

    public Gegevensbron() {
    }

    public String getAdmin_fk() {
        return admin_fk;
    }

    public void setAdmin_fk(String admin_fk) {
        this.admin_fk = admin_fk;
    }

    public String getAdmin_pk() {
        return admin_pk;
    }

    public void setAdmin_pk(String admin_pk) {
        this.admin_pk = admin_pk;
    }

    public String getAdmin_query() {
        return admin_query;
    }

    public void setAdmin_query(String admin_query) {
        this.admin_query = admin_query;
    }

    public String getAdmin_tabel() {
        return admin_tabel;
    }

    public void setAdmin_tabel(String admin_tabel) {
        this.admin_tabel = admin_tabel;
    }

    public String getAdmin_tabel_opmerkingen() {
        return admin_tabel_opmerkingen;
    }

    public void setAdmin_tabel_opmerkingen(String admin_tabel_opmerkingen) {
        this.admin_tabel_opmerkingen = admin_tabel_opmerkingen;
    }

    public Bron getBron() {
        return bron;
    }

    public void setBron(Bron bron) {
        this.bron = bron;
    }

    public Set getChildren() {
        return children;
    }

    public void setChildren(Set children) {
        this.children = children;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public Gegevensbron getParent() {
        return parent;
    }

    public void setParent(Gegevensbron parent) {
        this.parent = parent;
    }

    public int compareTo(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Set getThemaData() {
        return themaData;
    }

    public void setThemaData(Set themaData) {
        this.themaData = themaData;
    }

    public Bron getBron(HttpServletRequest request) {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        return getBron(user);
    }

    public Bron getBron(GisPrincipal user) {
        Bron b = bron;
        if (b == null && admin_tabel != null &&
                admin_tabel.length() > 0 && user != null) {

            b = user.getKbWfsConnectie();
        }
        return b;
    }

    public Set getThemas() {
        return themas;
    }

    public void setThemas(Set themas) {
        this.themas = themas;
    }
}