package nl.b3p.gis.viewer.print;

import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="info")
@XmlType(propOrder = {
    "titel",
    "datum",
    "imageUrl",
    "bbox",
    "opmerking",
    "kwaliteit",
    "columnOneItems",
    "columnTwoItems",
    "columnThreeItems",
    "legendItems",
    "scaleColumnOne",
    "scaleColumnTwo",
    "scaleColumnThree",
    "titleColumnOne",
    "titleColumnTwo",
    "titleColumnThree",
    "scale",
    "organizationcode"
})
public class PrintInfo {
    private String titel;
    private String datum;
    private String imageUrl;
    private String bbox;
    private String opmerking;
    private int kwaliteit;
    private Map columnOneItems;
    private Map columnTwoItems;
    private Map columnThreeItems;
    private Map legendItems;
    
    private String scaleColumnOne;
    private String scaleColumnTwo;
    private String scaleColumnThree;
    
    private String titleColumnOne;
    private String titleColumnTwo;
    private String titleColumnThree;
    
    private Integer scale;

    private String organizationcode;

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

    public Map getColumnOneItems() {
        return columnOneItems;
    }

    public void setColumnOneItems(Map columnOneItems) {
        this.columnOneItems = columnOneItems;
    }

    public Map getColumnTwoItems() {
        return columnTwoItems;
    }

    public void setColumnTwoItems(Map columnTwoItems) {
        this.columnTwoItems = columnTwoItems;
    }

    public Map getColumnThreeItems() {
        return columnThreeItems;
    }

    public void setColumnThreeItems(Map columnThreeItems) {
        this.columnThreeItems = columnThreeItems;
    }

    public Map getLegendItems() {
        return legendItems;
    }

    public void setLegendItems(Map legendItems) {
        this.legendItems = legendItems;
    }
    
    @XmlElement(name="scale")
    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public String getScaleColumnOne() {
        return scaleColumnOne;
    }

    public void setScaleColumnOne(String scaleColumnOne) {
        this.scaleColumnOne = scaleColumnOne;
    }

    public String getScaleColumnTwo() {
        return scaleColumnTwo;
    }

    public void setScaleColumnTwo(String scaleColumnTwo) {
        this.scaleColumnTwo = scaleColumnTwo;
    }

    public String getScaleColumnThree() {
        return scaleColumnThree;
    }

    public void setScaleColumnThree(String scaleColumnThree) {
        this.scaleColumnThree = scaleColumnThree;
    }

    public String getTitleColumnOne() {
        return titleColumnOne;
    }

    public void setTitleColumnOne(String titleColumnOne) {
        this.titleColumnOne = titleColumnOne;
    }

    public String getTitleColumnTwo() {
        return titleColumnTwo;
    }

    public void setTitleColumnTwo(String titleColumnTwo) {
        this.titleColumnTwo = titleColumnTwo;
    }

    public String getTitleColumnThree() {
        return titleColumnThree;
    }

    public void setTitleColumnThree(String titleColumnThree) {
        this.titleColumnThree = titleColumnThree;
    }
    
    @XmlElement(name="organizationcode")
    public String getOrganizationcode() {
        return organizationcode;
    }

    public void setOrganizationcode(String organizationcode) {
        this.organizationcode = organizationcode;
    }

}
