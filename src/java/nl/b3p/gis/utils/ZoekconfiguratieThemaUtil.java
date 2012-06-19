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
package nl.b3p.gis.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nl.b3p.gis.viewer.db.ZoekconfiguratieThemas;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.ZoekConfiguratie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class ZoekconfiguratieThemaUtil {

    private static final Log log = LogFactory.getLog(ZoekconfiguratieThemaUtil.class);

    public Integer[] getThemas(int zoekconfiguratieId) {

        Integer[] ids = {};

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = null;

        try {
            tx = sess.beginTransaction();

            ZoekConfiguratie zc = (ZoekConfiguratie) sess.get(ZoekConfiguratie.class, zoekconfiguratieId);
            List<ZoekconfiguratieThemas> zoekconfigThemas = sess.createQuery("from ZoekconfiguratieThemas WHERE zoekconfiguratie = :id").setParameter("id", zc).list();
            List<Integer> themaIds = new ArrayList<Integer>();
            for (Iterator<ZoekconfiguratieThemas> it = zoekconfigThemas.iterator(); it.hasNext();) {
                ZoekconfiguratieThemas zoekconfigThema = it.next();
                themaIds.add(zoekconfigThema.getThema().getId());
            }
            ids = themaIds.toArray(new Integer[themaIds.size()]);

            tx.commit();

        } catch (Exception ex) {
            log.error("Fout tijdens ophalen themas: " + ex);

            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }


        return ids;

    }
}
