package org.har01d.crawler;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import org.har01d.crawler.domain.Image;
import org.har01d.crawler.service.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class MainApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

    @Autowired
    private Crawler crawler;

    @Autowired
    private Worker worker;

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean("images")
    public LinkedBlockingQueue<Image> imageQueue() {
        return new LinkedBlockingQueue<>(100);
    }

    @Override
    public void run(String... strings) throws Exception {
        logger.info("crawling start.");
        Date startTime = new Date();
        new Thread(worker).start();
        crawler.crawler();
        worker.done();
        Date endTime = new Date();
        logger.info("crawling completed, start at {}, duration {} ms.", startTime,
            (endTime.getTime() - startTime.getTime()));
    }
}
