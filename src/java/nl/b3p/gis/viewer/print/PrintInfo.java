package nl.b3p.gis.viewer.print;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="info")
@XmlType(propOrder = {"titel","opmerking","datum","kwaliteit","orientatie",
    "paginaFormaat","outputFormaat"})

public class PrintInfo {
    private String titel;
    private String opmerking;
    private String datum;
    private Integer kwaliteit;
    private String orientatie;
    private String paginaFormaat;
    private String outputFormaat;

    public PrintInfo() {
    }

    @XmlElement(name="datum")
    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    @XmlElement(name="kwaliteit")
    public Integer getKwaliteit() {
        return kwaliteit;
    }

    public void setKwaliteit(Integer kwaliteit) {
        this.kwaliteit = kwaliteit;
    }

    @XmlElement(name="opmerking")
    public String getOpmerking() {
        return opmerking;
    }

    public void setOpmerking(String opmerking) {
        this.opmerking = opmerking;
    }

    @XmlElement(name="orientatie")
    public String getOrientatie() {
        return orientatie;
    }

    public void setOrientatie(String orientatie) {
        this.orientatie = orientatie;
    }

    @XmlElement(name="outputFormaat")
    public String getOutputFormaat() {
        return outputFormaat;
    }

    public void setOutputFormaat(String outputFormaat) {
        this.outputFormaat = outputFormaat;
    }

    @XmlElement(name="paginaFormaat")
    public String getPaginaFormaat() {
        return paginaFormaat;
    }

    public void setPaginaFormaat(String paginaFormaat) {
        this.paginaFormaat = paginaFormaat;
    }

    @XmlElement(name="titel")
    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }
}
