package nl.b3p.gis.viewer.db;

public class ThemaData {

    private Integer id;
    private String label;
    private String eenheid;
    private String omschrijving;
    private Themas thema;
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

    /** 
     * Return het ID van de thema data.
     *
     * @return int ID van de thema data.
     */
    // <editor-fold defaultstate="" desc="public int getId()">
    public Integer getId() {
        return id;
    }
    // </editor-fold>
    /** 
     * Set het ID van de thema data.
     *
     * @param id int id van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setId(int id)">
    public void setId(Integer id) {
        this.id = id;
    }
    // </editor-fold>
    /** 
     * Return het label van de thema data.
     *
     * @return String met het label.
     */
    // <editor-fold defaultstate="" desc="public String getLabel()">
    public String getLabel() {
        return label;
    }
    // </editor-fold>
    /** 
     * Set het label van de thema data.
     *
     * @param label String met het label van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setLabel(String label)">
    public void setLabel(String label) {
        this.label = label;
    }
    // </editor-fold>
    /** 
     * Return de eenheid van de thema data.
     *
     * @return String met de eenheid van de thema data.
     */
    // <editor-fold defaultstate="" desc="public String getEenheid()">
    public String getEenheid() {
        return eenheid;
    }
    // </editor-fold>
    /** 
     * Set de eenheid van de thema data.
     *
     * @param eenheid String met de eenheid van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setEenheid(String eenheid)">
    public void setEenheid(String eenheid) {
        this.eenheid = eenheid;
    }
    // </editor-fold>
    /** 
     * Return de omschrijving van de thema data.
     *
     * @return String met de omschrijving van de thema data.
     */
    // <editor-fold defaultstate="" desc="public String getOmschrijving()">
    public String getOmschrijving() {
        return omschrijving;
    }
    // </editor-fold>
    /** 
     * Set de omschrijving van de thema data.
     *
     * @param omschrijving String met de omschrijving van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setOmschrijving(String omschrijving)">
    public void setOmschrijving(String omschrijving) {
        this.omschrijving = omschrijving;
    }
    // </editor-fold>
    /** 
     * Returns een boolean als deze thema data een basisregel is.
     *
     * @return boolean true als deze thema data een basisregel is, anders false.
     */
    // <editor-fold defaultstate="" desc="public boolean isAdministratief()">
    public boolean isBasisregel() {
        return basisregel;
    }

    /** 
     * Set deze thema data als een basisregel. Als deze thema data een basisregel is zet deze dan true, 
     * anders false.
     *
     * @param basisregel boolean met true als deze thema data een basisregel is, anders false.
     */
    // <editor-fold defaultstate="" desc="public void setAdministratief(boolean administratief)">
    public void setBasisregel(boolean basisregel) {
        this.basisregel = basisregel;
    }

    /** 
     * Return de voorbeelden van de thema data.
     *
     * @return String met de voorbeelden van de thema data.
     */
    // <editor-fold defaultstate="" desc="public String getVoorbeelden()">
    public String getVoorbeelden() {
        return voorbeelden;
    }
    // </editor-fold>
    /** 
     * Set de voorbeelden van de thema data.
     *
     * @param voorbeelden String met de voorbeelden van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setVoorbeelden(String voorbeelden)">
    public void setVoorbeelden(String voorbeelden) {
        this.voorbeelden = voorbeelden;
    }
    // </editor-fold>
    /** 
     * Return de kolombreedte van de thema data.
     *
     * @return int met de kolombreedte van de thema data.
     */
    // <editor-fold defaultstate="" desc="public int getKolombreedte()">
    public int getKolombreedte() {
        return kolombreedte;
    }
    // </editor-fold>
    /** 
     * Set de kolombreedte van de thema data.
     *
     * @param kolombreedte int met de kolombreedte van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setKolombreedte(int kolombreedte)">
    public void setKolombreedte(int kolombreedte) {
        this.kolombreedte = kolombreedte;
    }
    // </editor-fold>
    /** 
     * Return het thema van de thema data.
     *
     * @return Themas met het thema van de thema data.
     *
     * @see Themas
     */
    // <editor-fold defaultstate="" desc="public Themas getThema()">
    public Themas getThema() {
        return thema;
    }
    // </editor-fold>
    /** 
     * Set de thema van de thema data.
     *
     * @param thema Themas met de thema van de thema data.
     *
     * @see Themas
     */
    // <editor-fold defaultstate="" desc="public void setThema(Themas thema)">
    public void setThema(Themas thema) {
        this.thema = thema;
    }
    // </editor-fold>
    /** 
     * Return het waardeType van de thema data.
     *
     * @return WaardeTypen met het waardeType van de thema data.
     *
     * @see WaardeTypen
     */
    // <editor-fold defaultstate="" desc="public WaardeTypen getWaardeType()">
    public WaardeTypen getWaardeType() {
        return waardeType;
    }
    // </editor-fold>
    /** 
     * Set het waardeType van de thema data.
     *
     * @param waardeType String met het waardeType van de thema data.
     *
     * @see WaardeTypen
     */
    // <editor-fold defaultstate="" desc="public void setWaardeType(WaardeTypen waardeType)">
    public void setWaardeType(WaardeTypen waardeType) {
        this.waardeType = waardeType;
    }
    // </editor-fold>
    /** 
     * Return de kolomnaam van de thema data.
     *
     * @return String met de kolomnaam van de thema data.
     */
    // <editor-fold defaultstate="" desc="public String getKolomnaam()">
    public String getKolomnaam() {
        return kolomnaam;
    }
    // </editor-fold>
    /** 
     * Set de kolomnaam van de thema data.
     *
     * @param kolomnaam String met de kolomnaam van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setKolomnaam(String kolomnaam)">
    public void setKolomnaam(String kolomnaam) {
        this.kolomnaam = kolomnaam;
    }
    // </editor-fold>
    /** 
     * Return het dataType van de thema data.
     *
     * @return String met het dataType van de thema data.
     *
     * @see DataTypen
     */
    // <editor-fold defaultstate="" desc="public DataTypen getDataType()">
    public DataTypen getDataType() {
        return dataType;
    }
    // </editor-fold>
    /** 
     * Set het dataType van de thema data.
     *
     * @param dataType DataTypen met het dataType van de thema data.
     *
     * @see DataTypen
     */
    // <editor-fold defaultstate="" desc="public void setDataType(DataTypen dataType)">
    public void setDataType(DataTypen dataType) {
        this.dataType = dataType;
    }
    // </editor-fold>
    /** 
     * Return het commando van de thema data.
     *
     * @return String met het commando van de thema data.
     */
    // <editor-fold defaultstate="" desc="public String getCommando()">
    public String getCommando() {
        return commando;
    }
    // </editor-fold>
    /** 
     * Set het commando van de thema data.
     *
     * @param commando String met het commando van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setCommando(String commando)">
    public void setCommando(String commando) {
        this.commando = commando;
    }
    // </editor-fold>
    /** 
     * Return de dataorder van de thema data.
     *
     * @return String met de dataorder van de thema data.
     */
    // <editor-fold defaultstate="" desc="public String getDataorder()">
    public Integer getDataorder() {
        return dataorder;
    }
    // </editor-fold>
    /** 
     * Set de dataorder van de thema data.
     *
     * @param dataorder String met de dataorder van de thema data.
     */
    // <editor-fold defaultstate="" desc="public void setDataorder(String dataorder)">
    public void setDataorder(Integer dataorder) {
        this.dataorder = dataorder;
    }

    public Gegevensbron getGegevensbron() {
        return gegevensbron;
    }

    public void setGegevensbron(Gegevensbron gegevensbron) {
        this.gegevensbron = gegevensbron;
    }
    // </editor-fold>
}
