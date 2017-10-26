package cz.mikropsoft.mhdwidget.repository;

import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import org.joda.time.LocalTime;

public interface SpojRepositoryCustom {

    /**
     * Vrací {@link Spoj} nesoucí informaci o nejbližším odjeydu, jeho předcházejícím a následujícím {@link Spoj}i.
     *
     * @param zastavka {@link Zastavka}, ze které chceme odjíždět
     * @param clientLocalTime aktuální čas klienta
     * @return aktuální {@link Spoj}
     */
    Spoj findAktualniByZastavka(Zastavka zastavka, LocalTime clientLocalTime);

}
