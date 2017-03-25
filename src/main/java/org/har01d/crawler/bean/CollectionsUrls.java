package org.har01d.crawler.bean;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.collection")
public class CollectionsUrls {

    private List<String> urls = new ArrayList<>();

    public List<String> getUrls() {
        return urls;
    }
}
