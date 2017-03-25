package org.har01d.crawler;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.har01d.crawler.bean.CollectionsUrls;
import org.har01d.crawler.domain.Image;
import org.har01d.crawler.parser.Parser;
import org.har01d.crawler.service.ImageWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class MainApplication implements CommandLineRunner {

    @Autowired
    private Parser parser;

    @Autowired
    private ImageWorker worker;

    @Autowired
    private CollectionsUrls collectionsUrls;

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean("images")
    public LinkedBlockingQueue<Image> imageQueue() {
        return new LinkedBlockingQueue<>(100);
    }

    @Override
    public void run(String... strings) throws Exception {
        new Thread(worker, "worker").start();
        for (String url : collectionsUrls.getUrls()) {
            while (url != null) {
                url = parser.parse(url);
            }
        }
    }
}
