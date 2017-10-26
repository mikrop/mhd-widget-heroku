package cz.mikropsoft.mhdwidget.repository.impl;

import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import cz.mikropsoft.mhdwidget.repository.SpojRepositoryCustom;
import org.joda.time.LocalTime;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@NoRepositoryBean
public class SpojRepositoryImpl implements SpojRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Spoj findAktualniByZastavka(Zastavka zastavka, LocalTime clientLocalTime) {
        List<Spoj> results = entityManager.createQuery("SELECT s FROM Spoj s WHERE s.zastavka = ?1 AND s.odjezd >= ?2", Spoj.class)
                .setParameter(1, zastavka)
                .setParameter(2, clientLocalTime)
                .setMaxResults(1)
                .getResultList();

        if (results.isEmpty()) { // Dnes již nic nejede, vracím první zítřejší spoj
            results = entityManager.createNamedQuery("Spoj.findFirstByZastavka", Spoj.class)
                    .setParameter(1, zastavka)
                    .setMaxResults(1)
                    .getResultList();
        }
        return DataAccessUtils.singleResult(results);
    }

}
