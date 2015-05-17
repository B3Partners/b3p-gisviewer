package nl.b3p.gis.viewer.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Configuratie {

    private static final Log log = LogFactory.getLog(Configuratie.class);

    private Integer id;
    private String property;
    private String propval;
    private String setting;
    private String type;

    public Configuratie() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    @Override
    public Object clone() {
        Configuratie cloneObj = new Configuratie();

        if (this.property != null) {
            cloneObj.property = this.property;
        }

        if (this.propval != null) {
            cloneObj.propval = this.propval;
        }

        if (this.setting != null) {
            cloneObj.setting = this.setting;
        }

        if (this.type != null) {
            cloneObj.type = this.type;
        }

        return cloneObj;
    }
}
