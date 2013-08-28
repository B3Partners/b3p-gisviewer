package nl.b3p.gis.viewer.services;

import javax.xml.bind.annotation.XmlElement;
import nl.b3p.gis.viewer.services.ObjectdataPdfInfo.Record;

class RecordElements {

    @XmlElement
    public Integer key;
    
    @XmlElement
    public Record value;

    private RecordElements() {
    }

    public RecordElements(Integer key, Record value) {
        this.key = key;
        this.value = value;
    }
}