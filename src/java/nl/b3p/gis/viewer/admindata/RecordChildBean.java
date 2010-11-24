package nl.b3p.gis.viewer.admindata;

/**
 *
 * @author Boy de Wit
 */
public class RecordChildBean {

    private String title;
    private int aantalRecords;

    private int gegevensBronBeanId;

    public int getAantalRecords() {
        return aantalRecords;
    }

    public void setAantalRecords(int aantalRecords) {
        this.aantalRecords = aantalRecords;
    }

    public int getGegevensBronBeanId() {
        return gegevensBronBeanId;
    }

    public void setGegevensBronBeanId(int gegevensBronBeanId) {
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
}
