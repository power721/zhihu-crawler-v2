package org.har01d.crawler.parser;

import java.io.IOException;
import org.har01d.crawler.domain.Question;

public interface QuestionParser {
    boolean parse(Question question) throws IOException;
}
