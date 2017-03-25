package org.har01d.crawler.service;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.har01d.crawler.domain.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AnswerWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AnswerWorker.class);

    @Autowired
    private LinkedBlockingQueue<Question> queue;

    private int counter;
    private boolean isDone;

    @Override
    public void run() {
        try {
            while (true) {
                Question question = queue.poll(500L, TimeUnit.MILLISECONDS);
                if (question == null) {
                    if (isDone) {
                        break;
                    }
                    continue;
                }

//                try {
//
//                } catch (IOException e) {
//                    logger.error("get answers for question {} failed!", question.getUrl(), e);
//                }
            }
        } catch (InterruptedException e) {
            logger.error("get answers interrupted!", e);
        } catch (Throwable e) {
            logger.error("exception occurred!", e);
        }

    }
}
