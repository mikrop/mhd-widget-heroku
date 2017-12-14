package cz.mikropsoft.mhdwidget.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "SpojProjection", types = Spoj.class)
public interface SpojProjection {

    @Value("#{target.zastavka.id}")
    Integer getZastavkaId();

    @Value("#{target.predchozi.odjezd}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    LocalTime getPredchozi();

    @Value("#{target.odjezd}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    LocalTime getOdjezd();

    @Value("#{target.nasledujici.odjezd}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    LocalTime getNasledujici();

}
