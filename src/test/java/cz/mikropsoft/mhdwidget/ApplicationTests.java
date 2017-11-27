package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Spoj;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import cz.mikropsoft.mhdwidget.repository.LinkaRepository;
import cz.mikropsoft.mhdwidget.repository.SpojRepository;
import cz.mikropsoft.mhdwidget.repository.ZastavkaRepository;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = MhdWidgetApplication.class)
@AutoConfigureMockMvc
public class ApplicationTests {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MhdExporter exporter;

    @Autowired
    private LinkaRepository linkaRepository;
    @Autowired
    private ZastavkaRepository zastavkaRepository;
    @Autowired
    private SpojRepository spojRepository;

    @BeforeClass
    public static void init() throws Exception {
    }

    @Test
    public void testSaveAsync() throws Exception {

        Linka linkaToSave = new Linka("Odnikud nikam", new LocalDate(), "http://www.linka.cz");
        Linka savedLinka = linkaRepository.save(linkaToSave);
        Zastavka zastavkaToSave = new Zastavka(savedLinka, "Zastávka", "http://www.zastavka.cz");
        Zastavka savedZastavka = zastavkaRepository.save(zastavkaToSave);

        for (int i=1; i<3; i++) {
            CompletableFuture.supplyAsync(() -> {

                Spoj spojToSave = new Spoj(savedZastavka, new LocalTime());
                Spoj savedSpoj = spojRepository.save(spojToSave);
                return savedSpoj;

            }).thenApply(spoj -> {

                logger.debug("Spoj uložen: ", spoj);
                return spoj;

            }).exceptionally(e -> {
                logger.error("Failed to save spoj", e);
                throw new IllegalStateException(e);
            });
        }
    }

    @Test
    public void testExport() throws Exception {

        Linka next = exporter.linkySave().iterator().next();
        Collection<Linka> linky = Collections.singletonList(next);
        Spliterator<Zastavka> spliterator = exporter.zastavkyUpdate(linky).spliterator();
        List<Zastavka> zastavky = StreamSupport.stream(spliterator, false)
                .collect(Collectors.toList());
        Assert.assertFalse(CollectionUtils.isEmpty(zastavky));

    }

}
