package org.har01d.crawler.service;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.har01d.crawler.domain.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Worker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

    @Autowired
    private Downloader downloader;

    @Autowired
    private LinkedBlockingQueue<Image> queue;

    private int counter;
    private boolean isDone;

    @Override
    public void run() {
        try {
            while (true) {
                Image image = queue.poll(500L, TimeUnit.MILLISECONDS);
                if (image == null) {
                    if (isDone) {
                        break;
                    }
                    continue;
                }

                try {
                    if (downloader.download(image)) {
                        counter++;
                    }
                } catch (IOException e) {
                    LOGGER.error("download image {} failed!", image.getUrl(), e);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("download image interrupted!", e);
        } catch (Throwable e) {
            LOGGER.error("exception occurred!", e);
        }

        LOGGER.info("download {} images.", counter);
    }

    public void done() {
        isDone = true;
    }

    public int getCounter() {
        return counter;
    }

}
