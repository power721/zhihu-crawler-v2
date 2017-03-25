package org.har01d.crawler.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.har01d.crawler.domain.HttpConfig;
import org.har01d.crawler.domain.Image;
import org.har01d.crawler.domain.ImageRepository;
import org.har01d.crawler.exception.ServerSideException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageDownloader implements Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDownloader.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private HttpConfig httpConfig;

    @Value("app.image.directory")
    private String imageDirectory;

    private int counter;

    @Override
    public boolean download(final Image image) throws IOException {
        String imageUrl = image.getUrl();
        String[] components = imageUrl.split("/");
        final String fileName = components[components.length - 1];
        final File file = new File(imageDirectory, fileName);

        String userAgent = httpConfig.getUserAgent();
        final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(httpConfig.getConnectTimeout())
            .setConnectionRequestTimeout(httpConfig.getConnectionRequestTimeout())
            .setCookieSpec(CookieSpecs.STANDARD)
            .setSocketTimeout(httpConfig.getSocketTimeout()).build();

        List<Header> headers =
            httpConfig.getHeaders().entrySet().stream().map(entry -> new BasicHeader(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        try (CloseableHttpClient httpClient =
            HttpClients.custom().setDefaultRequestConfig(requestConfig).setDefaultHeaders(headers)
                .setDefaultCookieStore(httpConfig.getCookieStore())
                .setUserAgent(userAgent).setConnectionTimeToLive(600L, TimeUnit.SECONDS).build()) {

            HttpGet httpget = new HttpGet(imageUrl);
            LOGGER.info("Executing download request {}", httpget.getRequestLine());

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
                            LOGGER.warn("download {} failed, expected size {}, got {}!", imageUrl, size, file.length());
                            return false;
                        }
                        LOGGER.info("download {} completed, file size {}, total download {} images.", imageUrl,
                            file.length(), counter);

                        image.setPath(file.getAbsolutePath());
                        imageRepository.save(image);
                        counter++;
                        return true;
                    }
                } else if (status >= 500 && status <= 599) {
                    throw new ServerSideException("Unexpected response status: " + status);
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            return httpClient.execute(httpget, responseHandler);
        }
    }

}
