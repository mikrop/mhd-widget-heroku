package cz.mikropsoft.mhdwidget.repository;

import cz.mikropsoft.mhdwidget.model.Linka;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@RepositoryRestResource(path = "linka", collectionResourceRel = "linky", itemResourceRel = "linka")
public interface LinkaRepository extends PagingAndSortingRepository<Linka, Integer> {

    /**
     * URL pro linku předaného směru http://mhdwidgetapi-mikropsoft.rhcloud.com/linka/search/bySmer?smer=Slovany - Bolevec
     *
     * @param smer směr požadované linky
     * @return {@link Linka}
     */
    @RestResource(path = "bySmer")
    Linka findBySmer(@Param("smer") String smer);

    /**
     * Vrací linky jejichž {@link Linka#platnostDo} je menší než aktuální datum.
     *
     * @return linky, které je potřeba aktualizovat
     */
    @Query("SELECT l FROM Linka l WHERE l.platnostDo < CURRENT_DATE")
    List<Linka> toUpdate();

}
