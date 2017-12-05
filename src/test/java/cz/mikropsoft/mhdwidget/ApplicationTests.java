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
import org.junit.Before;
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

import java.util.*;
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
    private Zastavka zastavka;

    @Autowired
    private MhdExporter exporter;

    @Autowired
    private LinkaRepository linkaRepository;
    @Autowired
    private ZastavkaRepository zastavkaRepository;
    @Autowired
    private SpojRepository spojRepository;

    @Before
    public void init() throws Exception {
        Linka linka = new Linka("Odnikud nikam", new LocalDate());
        linka.setUrl("http://www.linka.cz");
        linkaRepository.save(linka);
        Zastavka zastavka = new Zastavka(linka, "Zastávka");
        zastavka.setUrl("http://www.zastavka.cz");
        this.zastavka = zastavkaRepository.save(zastavka);
    }

    @Test
    public void testSave() throws Exception {
        for (int i=1; i<10; i++) {
            Spoj spojToSave = new Spoj(zastavka, new LocalTime());
            Spoj savedSpoj = spojRepository.save(spojToSave);
            logger.debug("Spoj uložen: ", savedSpoj);
        }
    }

    @Test
    public void testSaveAsync() throws Exception {
        for (int i=1; i<10; i++) {
            CompletableFuture.supplyAsync(() -> {

                try {
                    Zastavka z = zastavkaRepository.findOne(zastavka.getId());
                    Spoj spojToSave = new Spoj(z, new LocalTime());
                    Spoj savedSpoj = spojRepository.save(spojToSave);
                    return savedSpoj;
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

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
