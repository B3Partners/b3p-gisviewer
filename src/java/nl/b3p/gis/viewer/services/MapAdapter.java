package nl.b3p.gis.viewer.services;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

class MapAdapter extends XmlAdapter<RecordElements[], Map<Integer, ObjectdataPdfInfo.Record>> {

    public RecordElements[] marshal(Map<Integer, ObjectdataPdfInfo.Record> arg0) throws Exception {
        RecordElements[] mapElements = new RecordElements[arg0.size()];
        int i = 0;
        for (Map.Entry<Integer, ObjectdataPdfInfo.Record> entry : arg0.entrySet()) {
            mapElements[i++] = new RecordElements(entry.getKey(), entry.getValue());
        }

        return mapElements;
    }

    public Map<Integer, ObjectdataPdfInfo.Record> unmarshal(RecordElements[] arg0) throws Exception {
        Map<Integer, ObjectdataPdfInfo.Record> r = new HashMap<Integer, ObjectdataPdfInfo.Record>();
        for (RecordElements mapelement : arg0) {
            r.put(mapelement.key, mapelement.value);
        }
        return r;
    }
}