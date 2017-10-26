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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exportuje zastávku z <a href="http://jizdnirady.pmdp.cz">webu</a> Plzenských dopravních podniků.
 */
@Component
public class PmdpExporter implements MhdExporter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ZastavkaRepository zastavkaRepository;
    @Autowired
    private LinkaRepository linkaRepository;
    @Autowired
    private SpojRepository spojRepository;

    /**
     * Serializuje tabulku s odjezdy do objektu {@link Spoj}.
     *
     * @param zastavka {@link Zastavka}
     * @param next
     * @return {@link Zastavka}
     */
    private Iterable<Spoj> saveSpoje(Zastavka zastavka, Element next) {

        String hour = next.select("td[class=hour]").text();
        Elements select = next.select("td[class=normal]");

        List<Spoj> result = new ArrayList<>();
        for (Element normal : select) {
            String minute = normal.text().toLowerCase();
            String substring = minute.substring(0, 2);
            Spoj spoj = ObjectFactory.createSpoj(zastavka, hour + ":" + substring);
            result.add(spoj);
        }
        return spojRepository.save(result);
    }

    /**
     * Serializuje tabulku s odjezdy do objektu {@link Zastavka}.
     *
     * @param linka na jaké lince je {@link Zastavka} umístěna
     * @param next
     * @return {@link Zastavka}
     * @throws IOException
     */
    @Transactional
    public Zastavka saveZastavka(Linka linka, Element next) throws IOException {

        Element link = next.select("a").first();
        String jmeno = link.text();
        logger.debug("    Zastavka: {}", jmeno);
        String absHref = link.attr("abs:href");

        URL url = new URL(absHref);
        Document doc = Jsoup.parse(url, 20000);
        Elements tables = doc.select("table[class=resultTable]");
        Iterator<Element> trs = tables.get(0).select("tr").iterator(); // Pokud bude zveřejněn jiný než pracovní den je potřeba upravit index 0 na 1 nebo 2

        Zastavka result = zastavkaRepository.findByLinkaAndJmeno(linka, jmeno);
        if (result == null) {
            Zastavka zastavka = ObjectFactory.createZastavka(linka, jmeno);
            result = zastavkaRepository.save(zastavka);
            while (trs.hasNext()) {
                saveSpoje(result, trs.next());
            }
        }

        List<Spoj> spoje = spojRepository.findByZastavkaOrderByOdjezd(result);
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
                    spojRepository.save(spoj);

                });

        return result;
    }

    /**
     * Exportuje kolekci {@link Zastavka}, navázaných na předané {@link Linka} ze stránek PMDP.
     *
     * @param linky kolekce linek, jejichž zastávky budou aktualizovány
     * @return aktualizované {@link Zastavka}
     * @throws IOException
     */
    @Override
    public Iterable<Zastavka> zastavkyUpdate(Iterable<Linka> linky) throws IOException {

        Assert.assertNotNull(linky);
        for (Linka linka : linky) {
            Document doc = Jsoup.parse(new URL(linka.getUrl()), 20000);
            Element stations = doc.select("div[id=stations]").get(0);
            Iterator<Element> spans = stations.select("[class=actual],[class=normal]").iterator();

            while (spans.hasNext()) {
                saveZastavka(linka, spans.next());
            }
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
        logger.debug("Smer:  {}", smer);

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
     * Z předaného {@link Element}u vybere {@code td[class=linesList_StartEnd]}.
     *
     * @param next zdroj
     * @return výsledek hledání
     */
    private static Elements selectStartEnd(Element next) {
        return next.select("td[class=linesList_StartEnd]");
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
        Document doc = Jsoup.parse(url, 10000);
        Elements tables = doc.select("table[class=linesList]");
        Element table = tables.get(0);

        Iterator<Element> trs = table.select("tr").iterator();
        while (trs.hasNext()) {

            Element next = trs.next();
            String th = next.select(
                    "[class=linesList_TramStyle],[class=linesList_TrolStyle],[class=linesList_BusStyle],[class=linesList_NightStyle],[class=linesList_NightTrolStyle],[class=linesList_DiversionStyle]").text();
            Elements startEnd = selectStartEnd(next);
            if (startEnd.isEmpty() || th.isEmpty()) {
                continue;
            }
            saveLinka(next);

            next = trs.next();
            startEnd = selectStartEnd(next);
            if (startEnd.isEmpty() || th.isEmpty()) {
                continue;
            }
            saveLinka(next);

        }

        Period period = new Period(start, DateTime.now());
        logger.debug("Obsah http://jizdnirady.pmdp.cz byl exportovan za: " + period);

        return linkaRepository.findAll();
    }

}
