package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PmdpExporter implements MhdExporter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MhdService service;

    @Override
    public Iterable<Zastavka> zastavkyUpdate(Collection<Linka> linky) throws IOException, URISyntaxException {
        Assert.assertNotNull(linky);

        List<Zastavka> result = new ArrayList<>();
        for (Linka linka : linky) {

            Document document = Jsoup.parse(new URL(linka.getUrl()), 20000);
            Element stations = document.select("div[id=stations]").get(0);
            Elements spans = stations.select("[class=actual],[class=normal]");

            for (Element span : spans) {
                service.saveZastavka(linka, span)
                        .thenApply(zastavka -> {
                            try {

                                Document doc = Jsoup.parse(new URL(zastavka.getUrl()), 20000);
                                Elements tables = doc.select("table[class=resultTable]");
                                Elements trs = tables.get(0).select("tr");

                                for (Element tr : trs) {
                                    service.saveSpoje(zastavka.getId(), tr)
                                            .thenApply(spoje -> {

//                                                logger.debug("      Aktualizace spojů");
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
//                                                logger.debug("         Zpracování zastávky {} dokončeno", zastavka.getJmeno());
                                                return spoje;

                                            });
                                }
                                return zastavka;

                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
            }
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
            String th = next.select(
                    "[class=linesList_TramStyle],[class=linesList_TrolStyle],[class=linesList_BusStyle],[class=linesList_NightStyle],[class=linesList_NightTrolStyle],[class=linesList_DiversionStyle]").text();
            Elements startEnd = MhdService.selectStartEnd(next);
            if (startEnd.isEmpty() || th.isEmpty()) {
                continue;
            }
            result.add(service.saveLinka(next));

            next = iterator.next();
            startEnd = MhdService.selectStartEnd(next);
            if (startEnd.isEmpty() || th.isEmpty()) {
                continue;
            }
            result.add(service.saveLinka(next));

        }

        Period period = new Period(start, DateTime.now());
        logger.debug("Linky http://jizdnirady.pmdp.cz byly exportovan za: {}", PeriodFormat.getDefault().print(period));

        return result;
    }

}
