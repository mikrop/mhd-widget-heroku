package cz.mikropsoft.mhdwidget;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import cz.mikropsoft.mhdwidget.model.*;
import cz.mikropsoft.mhdwidget.repository.LinkaRepository;
import cz.mikropsoft.mhdwidget.repository.SpojRepository;
import cz.mikropsoft.mhdwidget.repository.ZastavkaRepository;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@RestController
public class MhdController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProjectionFactory factory;
    @Autowired
    private ObjectMapper mapper;
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
    public @ResponseBody ResponseEntity<?> linkyUpdate() throws IOException, URISyntaxException {
        return ResponseEntity.ok(exporter.linkySave());
    }

    /**
     * Aktualizuje {@link Linka}, předaného ID.
     *
     * @param id linky, jejíž zastávky budou aktualizovány
     * @return kolekce aktualizovaných {@link Zastavka}
     * @throws IOException
     * @throws URISyntaxException
     */
    @RequestMapping(value = "/db/linka/{id}/update", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<?> updateLinkaById(@PathVariable("id") int id) throws IOException, URISyntaxException {
        Linka linka = linkaRepository.findOne(id);
        Assert.assertNotNull("Linka ID: " + id + " nebyla nalezena.", linka);
        return ResponseEntity.ok(exporter.zastavkyUpdate(Collections.singletonList(linka)));
    }

    /**
     * Exportuje všechny {@link Zastavka} ze stránek PMDP.
     *
     * @return kolekce {@link Zastavka}
     * @throws IOException
     * @throws URISyntaxException
     */
    @RequestMapping(value = "/db/zastavky/update", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<?> zastavkyUpdate() throws IOException, URISyntaxException {
        List<Linka> linky = linkaRepository.toUpdate();
        return ResponseEntity.ok(exporter.zastavkyUpdate(linky));
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
        Page<ZastavkaProjection> page = zastavky.map(zastavka -> factory.createProjection(ZastavkaProjection.class, zastavka));
        List<ZastavkaProjection> content = page.getContent();
        return ResponseEntity.ok(content);
    }

    /**
     * Všechny zastávky (projekce pro fultext).
     *
     * @return resource {@link ZastavkaProjection}
     */
    @RequestMapping(value="/api/zastavka/{id}/spoje", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<?> getSpoje(@PathVariable("id") int id) {
        Zastavka zastavka = zastavkaRepository.findOne(id);
        Assert.assertNotNull("Zastávka ID: " + id + " nebyla nalezena.", zastavka);
        List<Spoj> spoje = spojRepository.findByZastavkaOrderByOdjezd(zastavka);
        Page<SpojProjection> page = new PageImpl<>(spoje).map(spoj -> factory.createProjection(SpojProjection.class, spoj));
        List<SpojProjection> content = page.getContent();
        return ResponseEntity.ok(content);
    }

    /**
     * Aktuální spoj.
     *
     * @param clientLocalTime čas na klientu
     * @param id zastávky, z jaké chceme odjíždět
     * @return {@link AktualniSpoj}
     * @throws IOException
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

    /**
     * {@link Resource} zastávky.
     *
     * @param id zastávky, jejíž {@Spoj}e chceme stahnout
     * @return {@Spoj}e pro offline verzi
     */
    @RequestMapping(value = "/api/zastavka/{id}/resource", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<?> getZastavkaResource(@PathVariable("id") int id,
                                                               HttpServletResponse response) throws IOException {
        Zastavka zastavka = zastavkaRepository.findOne(id);
        Assert.assertNotNull("Zastávka s ID: " + id + " nebyla nalezena.", zastavka);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        List<Spoj> spoje = spojRepository.findByZastavkaOrderByOdjezd(zastavka);
        Page<Spoj> page = new PageImpl<>(spoje);
        Page<SpojProjection> projected = page.map(spoj -> factory.createProjection(SpojProjection.class, spoj));
        List<SpojProjection> content = projected.getContent();
        mapper.writeValue(os, content);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        ByteArrayResource resource = new ByteArrayResource(os.toByteArray(), "zastavka-" + id + ".json");
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .contentType(MediaType.parseMediaType("application/json"))
                .body(resource);
    }

}
