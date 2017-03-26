package org.har01d.crawler.service;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.har01d.crawler.domain.Image;
import org.har01d.crawler.domain.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImageWorker implements Worker {

    private static final Logger logger = LoggerFactory.getLogger(ImageWorker.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private Downloader downloader;

    @Autowired
    private LinkedBlockingQueue<Image> queue;

    @Value("${app.worker.thread}")
    private int threads = 5;

    @Value("${app.worker.retry}")
    private int retry = 3;

    private ExecutorService threadPool;

    private boolean isDone;

    @PostConstruct
    public void init() {
        threadPool = Executors.newFixedThreadPool(threads, new MyThreadFactory("Worker"));
    }

    @Override
    public void run() {
        for (int i = 0; i < threads; ++i) {
            threadPool.submit(this::work);
        }
        threadPool.shutdown();
    }

    @Override
    public void work() {
        try {
            while (true) {
                Image image = queue.poll(500L, TimeUnit.MILLISECONDS);
                if (image == null) {
                    if (isDone) {
                        break;
                    }
                    continue;
                }

                for (int i = 0; i < retry; ++i) {
                    try {
                        if (downloader.download(image)) {
                            imageRepository.save(image);
                            break;
                        }
                        Thread.sleep(500L);
                    } catch (IOException e) {
                        logger.error("download image {} failed!", image.getUrl(), e);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.error("download image interrupted!", e);
        } catch (Exception e) {
            logger.error("exception occurred!", e);
        }

    }

    public void done() {
        isDone = true;
    }

}
