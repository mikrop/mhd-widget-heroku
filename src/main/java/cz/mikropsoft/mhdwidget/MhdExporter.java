package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Zastavka;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Rozhraní pro export jízdních řádů do jednotné JSON struktury.
 */
public interface MhdExporter {

    /**
     * Exportuje kolekci {@link Zastavka}, navázaných na předané {@link Linka}.
     *
     * @param linky kolekce linek, jejichž zastávky budou aktualizovány
     * @return aktualizované {@link Zastavka}
     * @throws IOException
     * @throws URISyntaxException
     */
    Iterable<Zastavka> zastavkyUpdate(Iterable<Linka> linky) throws IOException, URISyntaxException;

    /**
     * Uloží všechny linky.
     *
     * @return aktualizované {@link Linka}
     * @throws IOException
     * @throws URISyntaxException
     */
    Iterable<Linka> linkySave() throws IOException, URISyntaxException;

}
