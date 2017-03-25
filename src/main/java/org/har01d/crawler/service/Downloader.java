package org.har01d.crawler.service;

import java.io.IOException;
import org.har01d.crawler.domain.Image;

public interface Downloader {
    boolean download(Image image) throws IOException;
}
