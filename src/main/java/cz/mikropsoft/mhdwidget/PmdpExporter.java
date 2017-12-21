package cz.mikropsoft.mhdwidget;

import com.sun.istack.internal.Nullable;
import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Prostredek;
import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import cz.mikropsoft.mhdwidget.repository.SpojRepository;
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
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PmdpExporter implements MhdExporter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MhdService service;

    @Autowired
    private SpojRepository spojRepository;

    /**
     * Vrací {@link Prostredek}, na základě předaného stylu.
     *
     * @param select vyparsovaný element
     * @return dopravní prostředek, nebo {@code null}, pokud se z předaného stylu nepodařilo {@link Prostredek} určit
     */
    @Nullable
    private static Prostredek parseProstredek(@NotNull Elements select) {
        Assert.assertNotNull(select);

        switch (select.attr("class")) {
            case "linesList_BusStyle" :
            case "linesList_NightStyle" :
                return Prostredek.AUTOBUS;
            case "linesList_TrolStyle" :
            case "linesList_NightTrolStyle" :
                return Prostredek.TROLEJBUS;
            case "linesList_TramStyle" : return Prostredek.TRAMVAJ;
        }
        return null;
    }

    @Override
    public Iterable<Zastavka> zastavkyUpdate(Collection<Linka> linky) throws IOException, URISyntaxException {
        Assert.assertNotNull(linky);

        List<Zastavka> result = new ArrayList<>();
        for (Linka linka : linky) {
            CompletableFuture.runAsync(() -> {
                try {
                    logger.debug("Zpracování linky ve směru {}", linka.getSmer());

                    Document document = Jsoup.parse(new URL(linka.getUrl()), 20000);
                    Element stations = document.select("div[id=stations]").get(0);
                    Elements spans = stations.select("[class=actual],[class=normal]");

                    for (Element span : spans) {

                        Zastavka zastavka = service.saveZastavka(linka, span);
                        Document doc = Jsoup.parse(new URL(zastavka.getUrl()), 20000);
                        Elements tables = doc.select("table[class=resultTable]");
                        Elements trs = tables.get(0).select("tr");
                        logger.debug("    Zpracování zastávky {}", zastavka.getJmeno());

                        for (Element tr : trs) {
                            service.saveSpoje(zastavka, tr);
                        }

                        logger.debug("        Aktualizace vazeb mezi spoji");
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
                        spojRepository.save(map.values());
                        logger.debug("    Zastávka zpracována");

                    }
                    logger.debug("Linka ve směru {} zpracována", linka.getSmer());


                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).exceptionally(e -> {
                throw new IllegalStateException(e);
            });
        }
        return result;
    }

    @Override
    public Iterable<Linka> linkySave() throws IOException, URISyntaxException {
        DateTime start = DateTime.now();

        URL url = new URL("http://jizdnirady.pmdp.cz/LinesList.aspx");
        Document doc = Jsoup.parse(url, 20000);
        Elements tables = doc.select("table[class=linesList]");
        Element table = tables.get(0);

        Elements trs = table.select("tr");
        Iterator<Element> iterator = trs.iterator();

        List<Linka> result = new ArrayList<>();
        while (iterator.hasNext()) {

            Element next = iterator.next();
            Elements select = next.select(
                    "[class=linesList_TramStyle],[class=linesList_TrolStyle],[class=linesList_BusStyle],[class=linesList_NightStyle],[class=linesList_NightTrolStyle],[class=linesList_DiversionStyle]");
            String oznaceni = select.text();
            Elements startEnd = MhdService.selectStartEnd(next);
            if (startEnd.isEmpty() || oznaceni.isEmpty()) {
                continue;
            }
            Prostredek prostredek = parseProstredek(select);
            result.add(service.saveLinka(oznaceni, prostredek, next));

            next = iterator.next();
            startEnd = MhdService.selectStartEnd(next);
            if (startEnd.isEmpty() || oznaceni.isEmpty()) {
                continue;
            }
            prostredek = parseProstredek(select);
            result.add(service.saveLinka(oznaceni, prostredek, next));

        }

        Period period = new Period(start, DateTime.now());
        logger.debug("Linky http://jizdnirady.pmdp.cz byly exportovan za: {}", PeriodFormat.getDefault().print(period));

        return result;
    }

}
