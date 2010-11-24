package nl.b3p.gis.viewer.admindata;

/**
 *
 * @author Boy de Wit
 */
public class LabelBean {

    private int id;
    private String label;
    private String kolomNaam;
    private String commando;
    private int kolomBreedte;
    private String type;
    private String eenheid;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getKolomBreedte() {
        return kolomBreedte;
    }

    public void setKolomBreedte(int kolomBreedte) {
        this.kolomBreedte = kolomBreedte;
    }

    public String getKolomNaam() {
        return kolomNaam;
    }

    public void setKolomNaam(String kolomNaam) {
        this.kolomNaam = kolomNaam;
    }

    public String getCommando() {
        return commando;
    }

    public void setCommando(String commando) {
        this.commando = commando;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEenheid() {
        return eenheid;
    }

    public void setEenheid(String eenheid) {
        this.eenheid = eenheid;
    }
}
