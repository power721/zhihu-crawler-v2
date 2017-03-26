package org.har01d.crawler.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.har01d.crawler.bean.HttpConfig;
import org.har01d.crawler.domain.Image;
import org.har01d.crawler.exception.ServerSideException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageDownloader implements Downloader {

    private static final Logger logger = LoggerFactory.getLogger(ImageDownloader.class);

    @Autowired
    private HttpConfig httpConfig;

    @Value("${app.image.directory}")
    private String imageDirectory;

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public boolean download(final Image image) throws IOException {
        String imageUrl = image.getUrl();
        final String fileName = getFileName(image);

        File directory = getDirectory(image);
        final File file = new File(directory, fileName);

        String userAgent = httpConfig.getUserAgent();
        final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(httpConfig.getConnectTimeout())
            .setConnectionRequestTimeout(httpConfig.getConnectionRequestTimeout())
            .setCookieSpec(CookieSpecs.STANDARD)
            .setSocketTimeout(httpConfig.getSocketTimeout()).build();

        try (CloseableHttpClient httpClient =
            HttpClients.custom().setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(httpConfig.getCookieStore())
                .setUserAgent(userAgent).setConnectionTimeToLive(600L, TimeUnit.SECONDS).build()) {

            HttpGet httpget = new HttpGet(imageUrl);
            logger.info("Executing download request {}", httpget.getRequestLine());

            // Create a custom response handler
            ResponseHandler<Boolean> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    if (entity == null) {
                        return false;
                    } else {
                        FileUtils.copyInputStreamToFile(entity.getContent(), file);
                        long size = entity.getContentLength();
                        if (size != file.length()) {
                            logger.warn("download {} failed, expected size {}, got {}!", imageUrl, size, file.length());
                            return false;
                        }
                        logger.info("download {} completed, size {}, {} images.", imageUrl,
                            file.length(), counter.incrementAndGet());

                        image.setPath(file.getAbsolutePath());
                        return true;
                    }
                } else if (status >= 500 && status <= 599) {
                    throw new ServerSideException("Unexpected response status: " + response.getStatusLine());
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + response.getStatusLine());
                }
            };

            return httpClient.execute(httpget, responseHandler);
        }
    }

    private File getDirectory(Image image) {
        return new File(imageDirectory, String.valueOf(image.getAnswer().getQuestion().getId()));
    }

    private String getFileName(Image image) {
        String[] components = image.getUrl().split("/");
        return components[components.length - 1];
    }

}
