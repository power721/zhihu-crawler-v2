package org.har01d.crawler.service;

public interface Worker extends Runnable {
    void work();

    void done() throws InterruptedException;
}
