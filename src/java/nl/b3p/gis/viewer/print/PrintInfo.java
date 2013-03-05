package nl.b3p.gis.viewer.print;

import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="info")
@XmlType(propOrder = {"titel","datum","imageUrl","bbox","opmerking","kwaliteit","scale","legendItems"})
public class PrintInfo {
    private String titel;
    private String datum;
    private String imageUrl;
    private String bbox;
    private String opmerking;
    private int kwaliteit;
    private Integer scale;
    private Map legendItems;

    public PrintInfo() {
    }    

    @XmlElement(name="titel")
    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    @XmlElement(name="datum")
    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    @XmlElement(name="imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @XmlElement(name="bbox")
    public String getBbox() {
        return bbox;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    @XmlElement(name="opmerking")
    public String getOpmerking() {
        return opmerking;
    }

    public void setOpmerking(String opmerking) {
        this.opmerking = opmerking;
    }

    @XmlElement(name="kwaliteit")
    public int getKwaliteit() {
        return kwaliteit;
    }

    public void setKwaliteit(int kwaliteit) {
        this.kwaliteit = kwaliteit;
    }

    @XmlElement(name="scale")
    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Map getLegendItems() {
        return legendItems;
    }

    public void setLegendItems(Map legendItems) {
        this.legendItems = legendItems;
    }
}
