package cz.mikropsoft.mhdwidget.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "ZastavkaProjection", types = Zastavka.class)
public interface ZastavkaProjection {

    @Value("#{target.id}")
    String getId();

    @Value("#{target.jmeno}")
    String getJmeno();

    @Value("#{target.linka.smer}")
    String getSmer();

}
