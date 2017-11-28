package cz.mikropsoft.mhdwidget.repository;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@RepositoryRestResource(path = "zastavka", collectionResourceRel = "zastavky", itemResourceRel = "zastavka")
public interface ZastavkaRepository extends PagingAndSortingRepository<Zastavka, Integer> {

    /**
     * URL pro zastávku předaného jména http://mhdwidgetapi-mikropsoft.rhcloud.com/zastavka/search/byJmeno?jmeno=Brojova
     *
     * @param jmeno jméno požadované zastávky
     * @return kolekce {@link Zastavka}
     */
    @RestResource(path = "byJmeno")
    List<Zastavka> findByJmeno(@Param("jmeno") String jmeno);

//    /**
//     * Zastávky pro fulltext.
//     *
//     * @param search část/celé jméno zastávky
//     * @return kolekce {@link Zastavka}
//     */
//    @Query("SELECT z FROM Zastavka z WHERE UPPER(z.jmeno) LIKE CONCAT('%',UPPER(:search),'%')")
//    List<Zastavka> findWithPartOfJmeno(@Param("search") String search);

    /**
     * Vrací na předané {@link Linka} a předaného jména.
     *
     * @param linka {@link Linka}, na které zastávku hledáme
     * @param jmeno jméno zastávky
     * @return {@link Zastavka}
     */
    Zastavka findByLinkaAndJmeno(@Param("linka") Linka linka, @Param("jmeno") String jmeno);

}
