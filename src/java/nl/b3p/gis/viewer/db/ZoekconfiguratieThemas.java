/*
 * Copyright (C) 2012 Expression organization is undefined on line 4, column 61 in Templates/Licenses/license-gpl30.txt.
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

import nl.b3p.zoeker.configuratie.ZoekConfiguratie;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class ZoekconfiguratieThemas {
    
    private int id;
    
    private ZoekConfiguratie zoekconfiguratie;
    
    private Themas thema;

    public ZoekconfiguratieThemas() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Themas getThema() {
        return thema;
    }

    public void setThema(Themas thema) {
        this.thema = thema;
    }

    public ZoekConfiguratie getZoekconfiguratie() {
        return zoekconfiguratie;
    }

    public void setZoekconfiguratie(ZoekConfiguratie zoekconfiguratie) {
        this.zoekconfiguratie = zoekconfiguratie;
    }
}
