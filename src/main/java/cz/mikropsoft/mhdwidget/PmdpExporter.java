package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.ObjectFactory;
import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import cz.mikropsoft.mhdwidget.repository.LinkaRepository;
import cz.mikropsoft.mhdwidget.repository.SpojRepository;
import cz.mikropsoft.mhdwidget.repository.ZastavkaRepository;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Exportuje zastávku z <a href="http://jizdnirady.pmdp.cz">webu</a> Plzenských dopravních podniků.
 */
@Component
@EnableAsync
public class PmdpExporter implements MhdExporter {

    private Logger logger = LoggerFactory.getLogger(getClass());

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
    private static Elements selectStartEnd(Element next) {
        return next.select("td[class=linesList_StartEnd]");
    }

    /**
     * Serializuje tabulku s odjezdy do objektu {@link Spoj}.
     *
     * @param zastavka {@link Zastavka}
     * @param tr spoje
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void saveSpoje(Zastavka zastavka, Element tr) {

        String hour = tr.select("td[class=hour]").text();
        Elements select = tr.select("td[class=normal]");

        List<Spoj> result = new ArrayList<>();
        for (Element normal : select) {
            String minute = normal.text().toLowerCase();
            String substring = minute.substring(0, 2);

            Zastavka _zastavka = zastavkaRepository.findOne(zastavka.getId());
            Spoj spoj = ObjectFactory.createSpoj(_zastavka, hour + ":" + substring);
            result.add(spoj);
        }
        if (!CollectionUtils.isEmpty(result)) { // Vyparsoval se alespoň jeden spoj
            Iterable<Spoj> spoje = spojRepository.save(result);
            List<Spoj> list = StreamSupport.stream(spoje.spliterator(), false)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Serializuje tabulku s odjezdy do objektu {@link Zastavka}.
     *
     * @param linka na jaké lince je {@link Zastavka} umístěna
     * @param span
     * @throws IOException
     */
    @Transactional
    public void saveZastavka(Linka linka, Element span) {

        Element link = span.select("a").first();
        String jmeno = link.text();

//        Executor executor = Executors.newFixedThreadPool(10);
//        CompletableFuture.supplyAsync(() -> {
        try {

//                logger.debug("Start parsování zastavky {}", jmeno);
            String absHref = link.attr("abs:href");

            URL url = new URL(absHref);
            Document doc = Jsoup.parse(url, 20000);
            Elements tables = doc.select("table[class=resultTable]");
            Elements trs = tables.get(0).select("tr");
//            return trs;

//            } catch (IOException e) {
//                throw new IllegalStateException(e);
//            }
//        }).thenApplyAsync(trs -> {

//            logger.debug("   Zastavka {} načtena", jmeno);
            Zastavka zastavka = zastavkaRepository.findByLinkaAndJmeno(linka, jmeno);
            if (zastavka == null) {
                zastavka = ObjectFactory.createZastavka(linka, jmeno);
                zastavka = zastavkaRepository.save(zastavka);
            }

            for (Element tr : trs) {
                try {
                    saveSpoje(zastavka, tr);
                } catch (Exception e) {
                    throw new IllegalStateException("Nepořařilo se uložit spoje zastávky: " + zastavka.getJmeno(), e);
                }
            }

//            logger.debug("      Aktualizace spojů");
            List<Spoj> spoje = spojRepository.findByZastavkaOrderByOdjezd(zastavka);
            Map<Integer, Spoj> map = spoje.stream()
                    .collect(Collectors.toMap(Spoj::getId, Function.identity()));
            map.values()
                    .forEach(spoj -> {

                        int id = spoj.getId();
                        Spoj predchozi = map.get(id - 1);
                        if (predchozi == null) { // Předchozí neexituje, tzn. předchozí je poslední
                            predchozi = spoje.get(spoje.size() - 1);
                        }
                        spoj.setPredchozi(predchozi);
                        Spoj nasledujici = map.get(id + 1);
                        if (nasledujici == null) { // Následující neexituje, tzn. nasledujici je první
                            nasledujici = spoje.get(0);
                        }
                        spoj.setNasledujici(nasledujici);
                    });
            spojRepository.save(spoje);
//            logger.debug("         Zpracování zastávky {} dokončeno", jmeno);
//            return zastavka;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

//        }/*, executor*/);
    }

    /**
     * Exportuje kolekci {@link Zastavka}, navázaných na předané {@link Linka} ze stránek PMDP.
     *
     * @param linky kolekce linek, jejichž zastávky budou aktualizovány
     * @return aktualizované {@link Zastavka}
     * @throws IOException
     */
    @Override
    public Iterable<Zastavka> zastavkyUpdate(Collection<Linka> linky) throws IOException {
        Assert.assertNotNull(linky);

        AtomicInteger step = new AtomicInteger();
        ConsoleProgressBar linkyBar = new ConsoleProgressBar("aktualizace zastávek", linky.size());
        for (Linka linka : linky) {

            Document doc = Jsoup.parse(new URL(linka.getUrl()), 20000);
            Element stations = doc.select("div[id=stations]").get(0);
            Elements spans = stations.select("[class=actual],[class=normal]");

            for (Element span : spans) {
                saveZastavka(linka, span);
            }
            linkyBar.setCurrent(step.incrementAndGet());
        }

        return zastavkaRepository.findAll();
    }

    /**
     * Exportuje jiždní řád z předaného řádku tabulky obsahujícího odkaz na jízdní řád.
     *
     * @param next
     * @return uložená {@link Linka}
     * @throws IOException
     * @throws URISyntaxException
     */
    @Transactional
    public Linka saveLinka(Element next) throws IOException, URISyntaxException {

        Elements startEnd = selectStartEnd(next);
        String smer = startEnd.text();
//        logger.debug("Smer:  {}", smer);

        Element link = startEnd.select("a").first();
        String absHref = link.attr("abs:href");
        URIBuilder builder = new URIBuilder(absHref);
        List<NameValuePair> queryParams = builder.getQueryParams();
        String platnostDo = queryParams.get(4).getValue();

        Linka linka = linkaRepository.findBySmer(smer);
        if (linka != null) {
            linka = ObjectFactory.updateLinka(linka, platnostDo, absHref);
        } else {
            linka = ObjectFactory.createLinka(smer, platnostDo, absHref);
        }
        return linkaRepository.save(linka);
    }

    /**
     * Exportuje jízdní řády ze stránek PMDP do kolekce {@link Linka}.
     *
     * @return kolekce {@link Linka}
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public Iterable<Linka> saveLinky() throws IOException, URISyntaxException {
        DateTime start = DateTime.now();

        URL url = new URL("http://jizdnirady.pmdp.cz/LinesList.aspx");
        Document doc = Jsoup.parse(url, 20000);
        Elements tables = doc.select("table[class=linesList]");
        Element table = tables.get(0);

        Elements trs = table.select("tr");
        Iterator<Element> iterator = trs.iterator();

        AtomicInteger step = new AtomicInteger();
        ConsoleProgressBar linkyBar = new ConsoleProgressBar("aktualizace linek", trs.size());
        while (iterator.hasNext()) {

            Element next = iterator.next();
            String th = next.select(
                    "[class=linesList_TramStyle],[class=linesList_TrolStyle],[class=linesList_BusStyle],[class=linesList_NightStyle],[class=linesList_NightTrolStyle],[class=linesList_DiversionStyle]").text();
            Elements startEnd = selectStartEnd(next);
            if (startEnd.isEmpty() || th.isEmpty()) {
                step.incrementAndGet();
                continue;
            }
            saveLinka(next);
            step.incrementAndGet();

            next = iterator.next();
            startEnd = selectStartEnd(next);
            if (startEnd.isEmpty() || th.isEmpty()) {
                step.incrementAndGet();
                continue;
            }
            saveLinka(next);
            linkyBar.setCurrent(step.incrementAndGet());

        }

        Period period = new Period(start, DateTime.now());
        logger.debug("Linky http://jizdnirady.pmdp.cz byly exportovan za: {}", PeriodFormat.getDefault().print(period));

        return linkaRepository.findAll();
    }

}
