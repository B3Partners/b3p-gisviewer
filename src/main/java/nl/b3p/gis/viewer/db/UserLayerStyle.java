package nl.b3p.gis.viewer.db;

/**
 *
 * @author Boy de Wit
 */
public class UserLayerStyle {

    private Integer id;
    private UserLayer layerid;
    private String name;

    public UserLayerStyle() {
    }

    public UserLayerStyle(UserLayer layerid, String name) {
        this.layerid = layerid;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UserLayer getLayerid() {
        return layerid;
    }

    public void setLayerid(UserLayer layerid) {
        this.layerid = layerid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
