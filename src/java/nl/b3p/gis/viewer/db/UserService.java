package nl.b3p.gis.viewer.db;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Boy de Wit
 */
public class UserService {

    private Integer id;
    private String code;
    private String url;
    private String groupname;
    private String sld_url;
    private Set user_layers = new HashSet<UserLayer>();

    public UserService() {
    }

    public UserService(String code, String url, String groupname) {
        this.code = code;
        this.url = url;
        this.groupname = groupname;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGroupname() {
        return groupname;
    }

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSld_url() {
        return sld_url;
    }

    public void setSld_url(String sld_url) {
        this.sld_url = sld_url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set getUser_layers() {
        return user_layers;
    }

    public void setUser_layers(Set user_layers) {
        this.user_layers = user_layers;
    }

    public void addLayer(UserLayer ul) {
        this.user_layers.add(ul);
    }
}
