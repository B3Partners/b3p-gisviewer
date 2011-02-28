package nl.b3p.gis.viewer.admindata;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Boy de Wit
 */
public class GegevensBronBean {

    private Integer id;
    private String adminPk;
    private String title;
    private String csvPks = "";
    private List<LabelBean> labels;
    private List<RecordBean> records;
    private String parentHtmlId;
    private String layout;
    private Integer order;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAdminPk() {
        return adminPk;
    }

    public void setAdminPk(String adminPk) {
        this.adminPk = adminPk;
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

    public String getParentHtmlId() {
        return parentHtmlId;
    }

    public void setParentHtmlId(String parentHtmlId) {
        this.parentHtmlId = parentHtmlId;
    }

    public List<String> getKolomNamenList() {

        List kolomNamen = new ArrayList();

        if (labels == null) {
            return kolomNamen;
        }

        Iterator iter = labels.iterator();
        while (iter.hasNext()) {
            LabelBean lb = (LabelBean) iter.next();

            if (lb.getKolomNaam() != null) {
                kolomNamen.add(lb.getKolomNaam());
            }
        }

        return kolomNamen;

    }

    public String getCsvPks() {
        return csvPks;
    }

    public void setCsvPks(String csvPks) {
        this.csvPks = csvPks;
    }

    /* komma gescheiden String berekenen met id's van bijbehorende records */
    public void setCsvPksFromRecordBeans() {
        List<RecordBean> records = this.getRecords();
        if (records == null || records.isEmpty()) {
            return;
        }
        String keys = "";
        for (int i = 0; i < records.size(); i++) {
            RecordBean rb = records.get(i);
            if (rb.getId() == null) {
                continue;
            }
            if (i > 0) {
                keys += ",";
            }
            keys += rb.getId().toString();
        }
        try {
            this.csvPks = URLEncoder.encode(keys.trim(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            this.csvPks = "";
        }
    }

    /**
     * @return the layout
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout the layout to set
     */
    public void setLayout(String layout) {
        this.layout = layout;
    }
    public Integer getOrder(){
        return this.order;
    }
    public void setOrder(int order) {
        this.order=order;
    }
}
