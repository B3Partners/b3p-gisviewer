package nl.b3p.gis.viewer.report;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "reportinfo")
@XmlType(propOrder = {
    "titel",
    "datum",
    "bron"
})
public class ReportInfo {

    private String titel;
    private String datum;
    private ReportInfo.Bron bron;

    public ReportInfo() {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {
        "titel",
        "layout",
        "labels",
        "records"
    })
    public static class Bron {

        public enum LAYOUT {
            FLAT_TABLE,
            SIMPLE_TABLE,
            ONE_ROW_TABLE,
            CALC_TABLE
        };
        private String titel;
        private ReportInfo.Bron.LAYOUT layout;
        private String[] labels;
        private List<ReportInfo.Record> records;

        public Bron() {
        }

        public String getTitel() {
            return titel;
        }

        public void setTitel(String titel) {
            this.titel = titel;
        }

        public LAYOUT getLayout() {
            return layout;
        }

        public void setLayout(LAYOUT layout) {
            this.layout = layout;
        }

        public String[] getLabels() {
            return labels;
        }

        public void setLabels(String[] labels) {
            this.labels = labels;
        }

        public List<Record> getRecords() {
            return records;
        }

        public void setRecords(List<Record> records) {
            this.records = records;
        }

        public void addRecord(ReportInfo.Record record) {
            if (this.records == null) {
                this.records = new ArrayList<Record>();
            }

            this.records.add(record);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {
        "id",
        "values",
        "bronnen"
    })
    public static class Record {

        private Integer id;
        private String[] values;
        private List<ReportInfo.Bron> bronnen;

        // no arg constructor for xml
        public Record() {
        }

        public Record(String[] values, List<ReportInfo.Bron> bronnen) {

            this.values = values;
            this.bronnen = bronnen;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String[] getValues() {
            return values;
        }

        public void setValues(String[] values) {
            this.values = values;
        }

        public List<Bron> getBronnen() {
            return bronnen;
        }

        public void setBronnen(List<Bron> bronnen) {
            this.bronnen = bronnen;
        }

        public void addBron(ReportInfo.Bron bron) {
            if (this.bronnen == null) {
                this.bronnen = new ArrayList<Bron>();
            }

            this.bronnen.add(bron);
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

    public Bron getBron() {
        return bron;
    }

    public void setBron(Bron bron) {
        this.bron = bron;
    }
}
