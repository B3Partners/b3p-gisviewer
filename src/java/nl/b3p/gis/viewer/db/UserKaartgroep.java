package nl.b3p.gis.viewer.db;

/**
 * @author Boy de Wit
 */
public class UserKaartgroep {

    private Integer id;
    private String code;
    private Integer clusterid;
    private Boolean default_on;

    public UserKaartgroep() {
    }

    public UserKaartgroep(String code, Integer clusterid, Boolean default_on) {
        this.code = code;
        this.clusterid = clusterid;
        this.default_on = default_on;
    }

    public Integer getClusterid() {
        return clusterid;
    }

    public void setClusterid(Integer clusterid) {
        this.clusterid = clusterid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getDefault_on() {
        return default_on;
    }

    public void setDefault_on(Boolean default_on) {
        this.default_on = default_on;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
