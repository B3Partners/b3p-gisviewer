package nl.b3p.gis.viewer.admindata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Boy de Wit
 */
public class GegevensBronBean {
    private int id;
    
    private String title;

    private List<LabelBean> labels;
    private List<RecordBean> records;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<LabelBean> getLabels() {
        return labels;
    }

    public void setLabels(List<LabelBean> labels) {
        this.labels = labels;
    }

    public List<RecordBean> getRecords() {
        return records;
    }

    public void setRecords(List<RecordBean> records) {
        this.records = records;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addLabel(LabelBean lb) {
        if (labels == null) {
            labels = new ArrayList();
        }

        labels.add(lb);
    }

    public void addRecord(RecordBean rb) {
        if (records == null) {
            records = new ArrayList();
        }

        records.add(rb);
    }

    public List<String> getKolomNamenList() {

        List kolomNamen = new ArrayList();

        Iterator iter = labels.iterator();
        while(iter.hasNext()) {
            LabelBean lb = (LabelBean)iter.next();

            if (lb.getKolomNaam() != null)
                kolomNamen.add(lb.getKolomNaam());
        }

        return kolomNamen;

    }
}
