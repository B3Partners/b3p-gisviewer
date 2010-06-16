package nl.b3p.gis.viewer.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Configuratie {

    private static final Log log = LogFactory.getLog(Configuratie.class);

    private Long id;
    private String property;
    private String propval;
    private String setting;
    private String type;

    public Configuratie() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropval() {
        return propval;
    }

    public void setPropval(String propval) {
        this.propval = propval;
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
