package nl.b3p.gis.viewer.db;

import java.util.Date;
import java.util.Set;

/**
 *
 * @author Boy de Wit
 */
public class Applicatie {

    private Integer id;
    private String naam;
    private String code;
    private String gebruikersCode;
    private Applicatie parent;
    private Date datum_gebruikt;
    private Set children;

    public Applicatie() {
    }

    public Applicatie(String naam, String code) {
        this.naam = naam;
        this.code = code;
    }

    public Applicatie(String naam, String code, Applicatie parent) {
        this.naam = naam;
        this.code = code;
        this.parent = parent;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getDatum_gebruikt() {
        return datum_gebruikt;
    }

    public void setDatum_gebruikt(Date datum_gebruikt) {
        this.datum_gebruikt = datum_gebruikt;
    }

    public String getGebruikersCode() {
        return gebruikersCode;
    }

    public void setGebruikersCode(String gebruikersCode) {
        this.gebruikersCode = gebruikersCode;
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

    public Applicatie getParent() {
        return parent;
    }

    public void setParent(Applicatie parent) {
        this.parent = parent;
    }

    public Set getChildren() {
        return children;
    }

    public void setChildren(Set children) {
        this.children = children;
    }
}
