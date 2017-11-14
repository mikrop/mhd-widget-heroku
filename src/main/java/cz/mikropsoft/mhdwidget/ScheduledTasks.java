package cz.mikropsoft.mhdwidget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
public class ScheduledTasks {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MhdController controller;

    @Scheduled(cron = "0 0 12 * * *") // jednou denně o půlnoci
    public void update() throws IOException, URISyntaxException {
        controller.linkyUpdate();
        controller.zastavkyUpdate();
    }

}
