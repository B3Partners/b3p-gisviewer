package nl.b3p.gis.viewer.db;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Boy de Wit
 */
public class UserLayer {

    private Integer id;
    private UserService serviceid;
    private String title;
    private String name;
    private Boolean queryable;
    private String scalehint_min;
    private String scalehint_max;
    private String use_style;
    private String sld_part;
    private Boolean default_on;
    private UserLayer parent;

    private Set user_layer_styles = new HashSet<UserLayerStyle>();

    public UserLayer() {
    }

    public UserLayer(UserService serviceid, String name, Boolean queryable, Boolean default_on) {
        this.serviceid = serviceid;
        this.name = name;
        this.queryable = queryable;
        this.default_on = default_on;
    }

    public Boolean getDefault_on() {
        return default_on;
    }

    public void setDefault_on(Boolean default_on) {
        this.default_on = default_on;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getQueryable() {
        return queryable;
    }

    public void setQueryable(Boolean queryable) {
        this.queryable = queryable;
    }

    public String getScalehint_max() {
        return scalehint_max;
    }

    public void setScalehint_max(String scalehint_max) {
        this.scalehint_max = scalehint_max;
    }

    public String getScalehint_min() {
        return scalehint_min;
    }

    public void setScalehint_min(String scalehint_min) {
        this.scalehint_min = scalehint_min;
    }

    public UserService getServiceid() {
        return serviceid;
    }

    public void setServiceid(UserService serviceid) {
        this.serviceid = serviceid;
    }

    public String getSld_part() {
        return sld_part;
    }

    public void setSld_part(String sld_part) {
        this.sld_part = sld_part;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUse_style() {
        return use_style;
    }

    public void setUse_style(String use_style) {
        this.use_style = use_style;
    }

    public Set getUser_layer_styles() {
        return user_layer_styles;
    }

    public void setUser_layer_styles(Set user_layer_styles) {
        this.user_layer_styles = user_layer_styles;
    }

    public UserLayer getParent() {
        return parent;
    }

    public void setParent(UserLayer parent) {
        this.parent = parent;
    }

    public void addStyle(UserLayerStyle uls) {
        this.user_layer_styles.add(uls);
    }
}
