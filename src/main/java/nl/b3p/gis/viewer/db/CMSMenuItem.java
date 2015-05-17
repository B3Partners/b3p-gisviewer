/*
 * Copyright (C) 2013 B3Partners
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer.db;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Boy de Wit
 */
public class CMSMenuItem {
    
    private Integer id;
    private String titel;
    private String url;
    private String icon;
    private Integer volgordenr;
    private Date cdate;
    private Set cmsMenus = new HashSet<CMSMenu>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getVolgordenr() {
        return volgordenr;
    }

    public void setVolgordenr(Integer volgordenr) {
        this.volgordenr = volgordenr;
    }

    public Date getCdate() {
        return cdate;
    }

    public void setCdate(Date cdate) {
        this.cdate = cdate;
    }

    public Set getCmsMenus() {
        return cmsMenus;
    }

    public void setCmsMenus(Set cmsMenus) {
        this.cmsMenus = cmsMenus;
    }
    
    public void addMenu(CMSMenu menu) {
        cmsMenus.add(menu);
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject me = new JSONObject();

        me.put("id", this.getId());
        me.put("titel", this.getTitel());
        me.put("url", this.getUrl());
        me.put("icon", this.getIcon());
        me.put("volgordenr", this.getVolgordenr());
        me.put("cdate", this.getCdate());
        me.put("cmsMenus", this.getCmsMenus());

        return me;
    }
}
