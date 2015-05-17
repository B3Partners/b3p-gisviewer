package nl.b3p.gis.viewer.admindata;

/**
 *
 * @author Boy de Wit
 */
public class RecordChildBean {

    private String id;
    private String title;
    private int aantalRecords;

    private Integer gegevensBronBeanId;
    
    private String themaId;
    private String cql;
    private String wkt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAantalRecords() {
        return aantalRecords;
    }

    public void setAantalRecords(int aantalRecords) {
        this.aantalRecords = aantalRecords;
    }

    public Integer getGegevensBronBeanId() {
        return gegevensBronBeanId;
    }

    public void setGegevensBronBeanId(Integer gegevensBronBeanId) {
        this.gegevensBronBeanId = gegevensBronBeanId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAantalRecords(int fk) {

        return -1;
    }

    public String getCql() {
        return cql;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public String getThemaId() {
        return themaId;
    }

    public void setThemaId(String themaId) {
        this.themaId = themaId;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }
}
