package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import cz.mikropsoft.mhdwidget.repository.LinkaRepository;
import cz.mikropsoft.mhdwidget.repository.SpojRepository;
import cz.mikropsoft.mhdwidget.repository.ZastavkaRepository;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@Service
public class MhdService {

    /** Logování. */
    private Logger logger = LoggerFactory.getLogger(getClass());

    // Očekávaný formát vstupních dat
    private static final DateTimeFormatter ld = DateTimeFormat.forPattern("dd.MM.yyyy");
    private static final DateTimeFormatter lt = DateTimeFormat.forPattern("HH:mm");

    @Autowired
    private ZastavkaRepository zastavkaRepository;
    @Autowired
    private LinkaRepository linkaRepository;
    @Autowired
    private SpojRepository spojRepository;

    /**
     * Z předaného {@link Element}u vybere {@code td[class=linesList_StartEnd]}.
     *
     * @param next zdroj
     * @return výsledek hledání
     */
    static Elements selectStartEnd(Element next) {
        return next.select("td[class=linesList_StartEnd]");
    }

    /**
     * Serializuje tabulku s odjezdy do objektu {@link Spoj}.
     *
     * @param zastavkaId identifikátor {@link Zastavka}
     * @param tr
     * @return spoje
     */
    @Transactional
    public CompletableFuture<List<Spoj>> saveSpoje(Integer zastavkaId, Element tr) {
        return CompletableFuture.supplyAsync(() -> {

            String hour = tr.select("td[class=hour]").text();
            Elements select = tr.select("td[class=normal]");

            List<Spoj> result = new ArrayList<>();
            for (Element normal : select) {
                String minute = normal.text().toLowerCase();
                String substring = minute.substring(0, 2);
                LocalTime odjezd = lt.parseLocalTime(hour + ":" + substring);

                try {

                    Zastavka zastavka = zastavkaRepository.findOne(zastavkaId);
                    Spoj spoj = spojRepository.save(new Spoj(zastavka, odjezd));
                    logger.debug("Ulozeny spoj: {}", spoj);
                    result.add(spoj);

                } catch (Exception e) {
                    logger.error("Chyba ulození spojů.", e);
                }
            }
//            if (!CollectionUtils.isEmpty(result)) { // Vyparsoval se alespoň jeden spoj
//                try {
//                    Iterable<Spoj> spoje = spojRepository.save(result);
//                    return StreamSupport.stream(spoje.spliterator(), false)
//                            .collect(Collectors.toList());
//                } catch (Exception e) {
//                    logger.error("Chyba ulození spojů.", e);
//                }
//            }
            return result;

        }, ForkJoinPool.commonPool());
    }

    /**
     * Serializuje tabulku s odjezdy do objektu {@link Zastavka}.
     *
     * @param linka na jaké je {@link Zastavka} umístěna
     * @param span
     * @return zastávky
     */
    @Transactional
    public CompletableFuture<Zastavka> saveZastavka(Linka linka, Element span) {
        return CompletableFuture.supplyAsync(() -> {

            Element link = span.select("a").first();
            String jmeno = link.text();
            String absHref = link.attr("abs:href");

            Zastavka result = zastavkaRepository.findByLinkaAndJmeno(linka, jmeno);
            if (result == null) {
                result = zastavkaRepository.save(new Zastavka(linka, jmeno, absHref));
            }
            return result;

        }, ForkJoinPool.commonPool());
    }

    /**
     * Exportuje jiždní řád z předaného řádku tabulky obsahujícího odkaz na jízdní řád.
     *
     * @param next
     * @return uložená {@link Linka}
     * @throws URISyntaxException
     */
    @Transactional
    public Linka saveLinka(Element next) throws URISyntaxException {

        Elements startEnd = selectStartEnd(next);
        String smer = startEnd.text();
//        logger.debug("Smer:  {}", smer);

        Element link = startEnd.select("a").first();
        String absHref = link.attr("abs:href");
        URIBuilder builder = new URIBuilder(absHref);
        List<NameValuePair> queryParams = builder.getQueryParams();
        LocalDate platnostDo = ld.parseLocalDate(queryParams.get(4).getValue());

        Linka linka = linkaRepository.findBySmer(smer);
        if (linka != null) {
            linka.setPlatnostDo(platnostDo);
            linka.setSmer(absHref);
        } else {
            linka = new Linka(smer, platnostDo, absHref);
        }
        return linkaRepository.save(linka);

    }

}
