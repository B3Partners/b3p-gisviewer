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
public class CMSMenu {

    private Integer id;
    private String titel;
    private Date cdate;
    private Set cmsMenuItems = new HashSet<CMSMenuItem>();

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

    public Date getCdate() {
        return cdate;
    }

    public void setCdate(Date cdate) {
        this.cdate = cdate;
    }

    public Set getCmsMenuItems() {
        return cmsMenuItems;
    }

    public void setCmsMenuItems(Set cmsMenuItems) {
        this.cmsMenuItems = cmsMenuItems;
    }
    
    public void addMenuItem(CMSMenuItem item) {
        cmsMenuItems.add(item);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject me = new JSONObject();

        me.put("id", this.getId());
        me.put("titel", this.getTitel());
        me.put("cdate", this.getCdate());
        me.put("cmsMenuItems", this.getCmsMenuItems());

        return me;
    }
}
