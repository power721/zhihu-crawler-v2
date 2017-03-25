package org.har01d.crawler.bean;

import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.PostConstruct;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpConfig {

    private static final int CONNECTION_TIMEOUT_MS = 300 * 1000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 300 * 1000;
    private static final int SOCKET_TIMEOUT_MS = 300 * 1000;

    @Value("${app.user-agent}")
    private String userAgent;

    private int connectTimeout = CONNECTION_TIMEOUT_MS;
    private int connectionRequestTimeout = CONNECTION_REQUEST_TIMEOUT_MS;
    private int socketTimeout = SOCKET_TIMEOUT_MS;

    @Value("${app.token}")
    private String token;

    private BasicCookieStore cookieStore = new BasicCookieStore();

    @PostConstruct
    public void init() {
        BasicClientCookie cookie = new BasicClientCookie("z_c0", token);
        cookie.setDomain("www.zhihu.com");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }


    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }


    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

}
