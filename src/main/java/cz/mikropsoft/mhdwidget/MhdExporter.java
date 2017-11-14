package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Zastavka;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Rozhraní pro export jízdních řádů do jednotné JSON struktury.
 */
public interface MhdExporter {

    /**
     * Uloží všechny linky.
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    Iterable<Linka> saveLinky() throws IOException, URISyntaxException;

    /**
     * Aktualizuje zastávky na předaných linkách.
     *
     * @param linky kolekce linek, jejichž zastávky budou aktualizovány
     * @return aktualizované {@link Zastavka}
     * @throws IOException
     */
    Iterable<Zastavka> zastavkyUpdate(Collection<Linka> linky) throws IOException;

}
