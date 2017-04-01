package org.har01d.crawler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.har01d.crawler.bean.CollectionsUrls;
import org.har01d.crawler.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageCrawler implements Crawler {

    private static final Logger logger = LoggerFactory.getLogger(ImageCrawler.class);

    @Autowired
    private Parser parser;

    @Autowired
    private CollectionsUrls collectionsUrls;

    @Value("${app.collection.crawler.thread}")
    private int threads;

    private ExecutorService threadPool;

    @PostConstruct
    public void init() {
        threadPool = Executors.newFixedThreadPool(threads, new MyThreadFactory("Crawler"));
    }

    @Override
    public void crawler() throws InterruptedException {
        for (String url : collectionsUrls.getUrls()) {
            threadPool.submit(() -> {
                String collectionsUrl = url;
                while (collectionsUrl != null) {
                    try {
                        collectionsUrl = parser.parse(collectionsUrl);
                    } catch (Exception e) {
                        logger.error("parse collection page failed: " + collectionsUrl, e);
                    }
                }
                logger.info("crawler for {} completed.", url);
            });
        }

        threadPool.shutdown();
        if (!threadPool.awaitTermination(3L, TimeUnit.HOURS)) {
            threadPool.shutdownNow();
        }

        logger.info("ImageCrawler completed.");
    }
}
