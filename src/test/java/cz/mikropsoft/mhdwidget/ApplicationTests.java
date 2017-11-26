package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.Linka;
import cz.mikropsoft.mhdwidget.model.Zastavka;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MhdWidgetApplication.class)
public class ApplicationTests {

    @Autowired
    private MhdExporter exporter;

    @Test
    public void testExport() throws IOException, URISyntaxException {

        Linka next = exporter.linkySave().iterator().next();
        Collection<Linka> linky = Collections.singletonList(next);
        Spliterator<Zastavka> spliterator = exporter.zastavkyUpdate(linky).spliterator();
        List<Zastavka> zastavky = StreamSupport.stream(spliterator, false)
                .collect(Collectors.toList());
        Assert.assertFalse(CollectionUtils.isEmpty(zastavky));

    }

}
