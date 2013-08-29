package nl.b3p.gis.viewer.services;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pdfinfo")
public class ObjectdataPdfInfo {

    private String titel;
    private String datum;
    private Map<Integer, Record> records;

    public ObjectdataPdfInfo() {
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Record {

        private Integer id;
        private String imageUrl;
        private Map<String, String> items;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Map<String, String> getItems() {
            return items;
        }

        public void setItems(Map<String, String> items) {
            this.items = items;
        }
        
        public void addItem(String key, String value) {
            if (this.items == null) {
                this.items = new HashMap<String, String>();
            }
            
            this.items.put(key, value);  
        }
    }

    //@XmlJavaTypeAdapter(MapAdapter.class)
    public Map<Integer, Record> getRecords() {
        return records;
    }

    public void setRecords(Map<Integer, Record> records) {
        this.records = records;
    }
}
