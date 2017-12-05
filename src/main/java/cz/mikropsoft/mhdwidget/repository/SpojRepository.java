package cz.mikropsoft.mhdwidget.repository;

import cz.mikropsoft.mhdwidget.model.AktualniSpoj;
import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(
        path = "spoj",
        collectionResourceRel = "spoje",
        itemResourceRel = "spoj",
        excerptProjection = AktualniSpoj.class
)
public interface SpojRepository extends PagingAndSortingRepository<Spoj, Integer>, SpojRepositoryCustom {

    /**
     * Vrací všechny {@link Spoj}e z předané zastávky, seřazeny dle odjezdu.
     *
     * @param zastavka {@link Zastavka} jejíž spoje hledáme
     * @return všechny {@link Spoj}e
     */
    List<Spoj> findByZastavkaOrderByOdjezd(Zastavka zastavka);

}
