package nl.b3p.gis.viewer.db;

public class ThemaData {

    private Integer id;
    private String label;
    private String eenheid;
    private String omschrijving;
    private boolean basisregel;
    private String voorbeelden;
    private int kolombreedte;
    private WaardeTypen waardeType;
    private DataTypen dataType;
    private String commando;
    private String kolomnaam;
    private Integer dataorder;
    private Gegevensbron gegevensbron;

    /**
     * Creates a new instance of ThemaData
     */
    public ThemaData() {
    }

    public boolean isBasisregel() {
        return basisregel;
    }

    public void setBasisregel(boolean basisregel) {
        this.basisregel = basisregel;
    }

    public String getCommando() {
        return commando;
    }

    public void setCommando(String commando) {
        this.commando = commando;
    }

    public DataTypen getDataType() {
        return dataType;
    }

    public void setDataType(DataTypen dataType) {
        this.dataType = dataType;
    }

    public Integer getDataorder() {
        return dataorder;
    }

    public void setDataorder(Integer dataorder) {
        this.dataorder = dataorder;
    }

    public String getEenheid() {
        return eenheid;
    }

    public void setEenheid(String eenheid) {
        this.eenheid = eenheid;
    }

    public Gegevensbron getGegevensbron() {
        return gegevensbron;
    }

    public void setGegevensbron(Gegevensbron gegevensbron) {
        this.gegevensbron = gegevensbron;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getKolombreedte() {
        return kolombreedte;
    }

    public void setKolombreedte(int kolombreedte) {
        this.kolombreedte = kolombreedte;
    }

    public String getKolomnaam() {
        return kolomnaam;
    }

    public void setKolomnaam(String kolomnaam) {
        this.kolomnaam = kolomnaam;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOmschrijving() {
        return omschrijving;
    }

    public void setOmschrijving(String omschrijving) {
        this.omschrijving = omschrijving;
    }

    public String getVoorbeelden() {
        return voorbeelden;
    }

    public void setVoorbeelden(String voorbeelden) {
        this.voorbeelden = voorbeelden;
    }

    public WaardeTypen getWaardeType() {
        return waardeType;
    }

    public void setWaardeType(WaardeTypen waardeType) {
        this.waardeType = waardeType;
    }
    
    @Override
    public Object clone() {
        ThemaData cloneObj = new ThemaData();
    
        if (this.label != null) {
            cloneObj.label = this.label;
        }
        
        if (this.eenheid != null) {
            cloneObj.eenheid = this.eenheid;
        }
        
        if (this.omschrijving != null) {
            cloneObj.omschrijving = this.omschrijving;
        }
        
        cloneObj.basisregel = this.basisregel;
        
        if (this.voorbeelden != null) {
            cloneObj.voorbeelden = this.voorbeelden;
        }
        
        cloneObj.kolombreedte = this.kolombreedte;
        
        if (this.waardeType != null) {
            cloneObj.waardeType = this.waardeType;
        }
        
        if (this.dataType != null) {
            cloneObj.dataType = this.dataType;
        }
        
        if (this.commando != null) {
            cloneObj.commando = this.commando;
        }
        
        if (this.kolomnaam != null) {
            cloneObj.kolomnaam = this.kolomnaam;
        }

        if (this.dataorder != null) {
            cloneObj.dataorder = new Integer(this.dataorder);
        }
        
        if (this.gegevensbron != null) {
            cloneObj.gegevensbron = this.gegevensbron;
        }

        return cloneObj;
    }
}
