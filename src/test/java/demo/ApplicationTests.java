package demo;

import cz.mikropsoft.mhdwidget.MhdExporter;
import cz.mikropsoft.mhdwidget.MhdWidgetApplication;
import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MhdWidgetApplication.class)
public class ApplicationTests {

    @Autowired
    private MhdExporter exporter;

    @Test
    public void testExport() throws IOException, URISyntaxException {
        Iterable<Linka> linky = exporter.linkySave();
        Linka linka = linky.iterator().next();
        Iterable<Zastavka> zastavky = exporter.zastavkyUpdate(Collections.singletonList(linka));
        Assert.assertNotNull(zastavky);
    }

}
