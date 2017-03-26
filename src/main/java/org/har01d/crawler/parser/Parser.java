package org.har01d.crawler.parser;

import java.io.IOException;

public interface Parser {
    String parse(String baseUrl) throws IOException;
}
