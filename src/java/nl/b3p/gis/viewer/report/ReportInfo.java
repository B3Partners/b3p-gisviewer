package nl.b3p.gis.viewer.report;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "reportinfo")
public class ReportInfo {
    
    private String titel;
    private String datum;
    private Map<Integer, ReportInfo.Bron> bronnen;

    public ReportInfo() {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Bron {

        public enum TABLE_TYPE {
            FLAT_TABLE,
            SIMPLE_TABLE,
            ONE_ROW_TABLE,
            CALC_TABLE
        };
        
        private ReportInfo.Bron.TABLE_TYPE tableType;
        private Integer id;
        private String[] labels;
        private Map<Integer, String[]> records;

        public TABLE_TYPE getTableType() {
            return tableType;
        }

        public void setTableType(TABLE_TYPE tableType) {
            this.tableType = tableType;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String[] getLabels() {
            return labels;
        }

        public void setLabels(String[] labels) {
            this.labels = labels;
        }

        public Map<Integer, String[]> getRecords() {
            return records;
        }

        public void setRecords(Map<Integer, String[]> records) {
            this.records = records;
        }
        
        public void addRecord(Integer key, String[] values) {
            if (this.records == null) {
                this.records = new HashMap<Integer, String[]>();
            }
            
            this.records.put(key, values);  
        }
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

    public Map<Integer, Bron> getBronnen() {
        return bronnen;
    }

    public void setBronnen(Map<Integer, Bron> bronnen) {
        this.bronnen = bronnen;
    }
}
