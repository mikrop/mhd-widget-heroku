package cz.mikropsoft.mhdwidget;

import cz.mikropsoft.mhdwidget.model.*;
import cz.mikropsoft.mhdwidget.repository.LinkaRepository;
import cz.mikropsoft.mhdwidget.repository.SpojRepository;
import cz.mikropsoft.mhdwidget.repository.ZastavkaRepository;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@RestController
public class MhdController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProjectionFactory factory;
    @Autowired
    private MhdExporter exporter;
    @Autowired
    private LinkaRepository linkaRepository;
    @Autowired
    private ZastavkaRepository zastavkaRepository;
    @Autowired
    private SpojRepository spojRepository;

    /**
     * Aktualizuje seznam linek.
     *
     * @return kolekce evidovaných {@link Linka}
     * @throws IOException
     * @throws URISyntaxException
     */
    @RequestMapping(value = "/db/linky/update", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Iterable<Linka> linkyUpdate() throws IOException, URISyntaxException {
        return exporter.saveLinky();
    }

    /**
     * Aktualizuje {@link Linka}, předaného ID.
     *
     * @param id linky, jejíž zastávky budou aktualizovány
     * @return kolekce aktualizovaných {@link Zastavka}
     * @throws IOException
     */
    @RequestMapping(value = "/db/linka/{id}/update", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Iterable<Zastavka> updateLinkaById(@PathVariable("id") int id) throws IOException {
        Linka linka = linkaRepository.findOne(id);
        Assert.assertNotNull("Linka ID: " + id + " nebyla nalezena.", linka);
        return exporter.zastavkyUpdate(Collections.singletonList(linka));
    }

    /**
     * Exportuje všechny {@link Zastavka} ze stránek PMDP.
     *
     * @return kolekce {@link Zastavka}
     * @throws IOException
     * @throws URISyntaxException
     */
    @RequestMapping(value = "/db/zastavky/update", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Iterable<Zastavka> zastavkyUpdate() throws IOException, URISyntaxException {
        return exporter.zastavkyUpdate(linkaRepository.toUpdate());
    }

    /**
     * Všechny zastávky (projekce pro fultext).
     *
     * @param pageable stránkování
     * @return resource {@link ZastavkaProjection}
     */
    @RequestMapping(value="/api/zastavky", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<?> getZastavky(Pageable pageable) {
        Page<Zastavka> zastavky = zastavkaRepository.findAll(pageable);
        Page<ZastavkaProjection> projected = zastavky.map(zastavka -> factory.createProjection(ZastavkaProjection.class, zastavka));
        List<ZastavkaProjection> content = projected.getContent();
        return ResponseEntity.ok(content);
    }

    /**
     * Aktuální spoj.
     *
     * @param clientLocalTime čas na klientu
     * @param id zastávky, z jaké chceme odjíždět
     * @return {@link AktualniSpoj}
     */
    @RequestMapping(value = "/api/zastavka/{id}/aktualni/{time}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<AktualniSpoj> getAktualniSpoj(
            @PathVariable("id") int id,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            @PathVariable("time") LocalTime clientLocalTime // 23:06:00.394+01:00
    ) throws IOException {
        Assert.assertNotNull("Nebyl předán 'Client-Local-Time' obsahující aktuální čas klienta.", clientLocalTime);
        Zastavka zastavka = zastavkaRepository.findOne(id);
        Assert.assertNotNull("Zastávka s ID: " + id + " nebyla nalezena.", zastavka);
        Spoj spoj = spojRepository.findAktualniByZastavka(zastavka, new org.joda.time.LocalTime(clientLocalTime.getHour(), clientLocalTime.getMinute(), clientLocalTime.getSecond()));
        Assert.assertNotNull("Nenalezen aktuální spoj k zastávce: " + zastavka.getJmeno() + ", ve směru: " + zastavka.getLinka().getSmer(), spoj);
        AktualniSpoj projection = factory.createProjection(AktualniSpoj.class, spoj);
        return ResponseEntity.ok(projection);
    }

//    /**
//     * {@link Resource} zastávky.
//     *
//     * @param id zastávky, kterou chceme stahnout
//     * @return zastávka pro offline verzi
//     */
//    @RequestMapping(value = "/api/zastavka/{id}/resource", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE, method = RequestMethod.GET)
//    public @ResponseBody Resource getZastavkaResource(@PathVariable("id") int id, HttpServletResponse response) {
//        ByteArrayInputStream inputStream = new ByteArrayInputStream(LocalDateTime.now().toString().getBytes());
//        return new InputStreamResource(inputStream);
//    }

}
