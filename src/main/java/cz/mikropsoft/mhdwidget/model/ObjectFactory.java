package cz.mikropsoft.mhdwidget.model;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Factory pro vytváření objektů.
 */
public class ObjectFactory {

    // Očekávaný formát vstupních dat
    private static final DateTimeFormatter ld = DateTimeFormat.forPattern("dd.MM.yyyy");
    private static final DateTimeFormatter lt = DateTimeFormat.forPattern("HH:mm");

    /**
     * Z předaných parametrů vytvoří objekt {@link Linka}.
     *
     * @param smer odkud kam {@link Linka} jede
     * @param platnostDo datum do kdy jsou jízdní řády platné
     * @param url URL adresa
     * @return {@link Linka}
     */
    public static Linka createLinka(String smer, String platnostDo, String url) {
        Linka result = new Linka();
        result.setSmer(smer);
        return updateLinka(result, platnostDo, url);
    }

    /**
     * Aktualizuje předanou {@link Linka}.
     *
     * @param linka objekt k aktualizaci
     * @param platnostDo datum do kdy jsou jízdní řády platné
     * @param url URL adresa
     * @return {@link Linka}
     */
    public static Linka updateLinka(Linka linka, String platnostDo, String url) {
        linka.setPlatnostDo(ld.parseLocalDate(platnostDo));
        linka.setUrl(url);
        return linka;
    }

    /**
     * Z předaných parametrů vytvoří objekt {@link Zastavka}.
     *
     * @param linka {@link Linka} na které je {@link Zastavka} umístěna
     * @param jmeno jméno zastávky
     * @param url URL adresa
     * @return {@link Zastavka}
     */
    public static Zastavka createZastavka(Linka linka, String jmeno, String url) {
        Zastavka result = new Zastavka();
        result.setLinka(linka);
        result.setJmeno(jmeno);
        result.setUrl(url);
        return result;
    }

    /**
     * Z předaných parametrů vytvoří objekt {@link Spoj}.
     *
     * @param zastavka {@link Zastavka} ze které {@link Spoj} odjíždí
     * @param odjezd čas odjezdu
     * @return {@link Spoj}
     */
    public static Spoj createSpoj(Zastavka zastavka, String odjezd) {
        Spoj result = new Spoj();
        result.setZastavka(zastavka);
        result.setOdjezd(lt.parseLocalTime(odjezd));
        return result;
    }

}
