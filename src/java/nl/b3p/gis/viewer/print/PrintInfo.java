package nl.b3p.gis.viewer.print;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="persoon")
@XmlType(propOrder = {"naam", "leeftijd", "datum"})
public class PrintInfo {
    private String naam;
    private int leeftijd;
    private String datum;

    public PrintInfo() {
    }

    @XmlElement(name="leeftijd")
    public int getLeeftijd() {
        return leeftijd;
    }

    public void setLeeftijd(int leeftijd) {
        this.leeftijd = leeftijd;
    }

    @XmlElement(name="naam")
    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    @XmlElement(name="datum")
    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }
}
