/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.gis.viewer.db;

/**
 *
 * @author Boy de Wit
 */
public class UserKaartlaag {

    private Integer id;
    private String code;
    private Integer themaid;
    private Boolean default_on;

    public UserKaartlaag() {
    }

    public UserKaartlaag(String code, Integer themaid, Boolean default_on) {
        this.code = code;
        this.themaid = themaid;
        this.default_on = default_on;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Integer getThemaid() {
        return themaid;
    }

    public void setThemaid(Integer themaid) {
        this.themaid = themaid;
    }

    @Override
    public Object clone() {
        UserKaartlaag cloneObj = new UserKaartlaag();

        if (this.code != null) {
            cloneObj.code = this.code;
        }

        if (this.themaid != null) {
            cloneObj.themaid = new Integer(this.themaid);
        }

        if (this.default_on != null) {
            cloneObj.default_on = this.default_on;
        }

        return cloneObj;
    }
}
