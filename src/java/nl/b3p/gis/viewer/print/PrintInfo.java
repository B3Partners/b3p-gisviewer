package nl.b3p.gis.viewer.print;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="info")
@XmlType(propOrder = {"titel","datum","imageUrl","bbox","mapWidth","mapHeight"})
public class PrintInfo {
    private String titel;
    private String datum;
    private String imageUrl;
    private String bbox;
    private int mapWidth;
    private int mapHeight;

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

    @XmlElement(name="mapWidth")
    public int getMapWidth() {
        return mapWidth;
    }

    public void setMapWidth(int mapWidth) {
        this.mapWidth = mapWidth;
    }

    @XmlElement(name="mapHeight")
    public int getMapHeight() {
        return mapHeight;
    }

    public void setMapHeight(int mapHeight) {
        this.mapHeight = mapHeight;
    }
}
