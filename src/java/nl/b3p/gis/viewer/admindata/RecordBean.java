package nl.b3p.gis.viewer.admindata;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Boy de Wit
 */
public class RecordBean {

    private Object id;
    private String wkt;

    private List<RecordValueBean> values;
    private List<RecordChildBean> childs;

    private boolean showMagicWand;

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public List<RecordChildBean> getChilds() {
        return childs;
    }

    public void setChilds(List<RecordChildBean> childs) {
        this.childs = childs;
    }

    public List<RecordValueBean> getValues() {
        return values;
    }

    public void setValues(List<RecordValueBean> values) {
        this.values = values;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    public boolean isShowMagicWand() {
        return showMagicWand;
    }

    public void setShowMagicWand(boolean showMagicWand) {
        this.showMagicWand = showMagicWand;
    }

    public void addValue(RecordValueBean rvb) {
        if (values == null) {
            values = new ArrayList();
        }

        values.add(rvb);
    }

    public void addChild(RecordChildBean rcb) {
        if (childs == null) {
            childs = new ArrayList();
        }

        childs.add(rcb);
    }
}
