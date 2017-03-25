package org.har01d.crawler;

import java.util.concurrent.LinkedBlockingQueue;
import org.har01d.crawler.domain.Image;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MainApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean
    public LinkedBlockingQueue<Image> queue() {
        return new LinkedBlockingQueue<>(100);
    }

    @Override
    public void run(String... strings) throws Exception {

    }
}
