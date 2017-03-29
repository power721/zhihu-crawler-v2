package org.har01d.crawler.parser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.crawler.bean.HttpConfig;
import org.har01d.crawler.bean.QuestionInfo;
import org.har01d.crawler.domain.Question;
import org.har01d.crawler.domain.QuestionRepository;
import org.har01d.crawler.util.HttpUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CollectionPageParser implements Parser {

    private static final Logger logger = LoggerFactory.getLogger(CollectionPageParser.class);
    private static final Pattern QUESTION_ID_PATTERN = Pattern.compile("/question/(\\d+)");

    @Autowired
    private HttpConfig httpConfig;

    @Autowired
    private QuestionRepository repository;

    @Autowired
    private QuestionParser parser;

    @Value("${app.collection.selector.href.question}")
    private String questionSelector;

    @Value("${app.collection.selector.href.next}")
    private String nextUrlSelector;

    @Value("${app.url.question}")
    private String questionApi;

    @Override
    public String parse(String baseUrl) throws IOException {
        String html = HttpUtils.getHtml(baseUrl, httpConfig);
        Document doc = Jsoup.parse(html);
        Elements hrefs = doc.select(questionSelector);

        long time = System.currentTimeMillis();
        for (Element href : hrefs) {
            String url = href.attr("href");
            if (url != null) {
                long id = getQuestionId(url);

                Question question = repository.findOne(id);
                if (question == null) {
                    QuestionInfo info = getQuestionInfo(id);
                    if (info == null) {
                        continue;
                    }
                    question = createQuestion(info);
                    repository.save(question);
                    logger.info("create question {}:{} {}", question.getId(), question.getTitle(), question.getUrl());
                }

                if (question.getAccessedTime() + TimeUnit.DAYS.toMillis(1) < time) {
                    if (parser.parse(question)) {
                        question.setAccessedTime(System.currentTimeMillis());
                    } else {
                        question.setAccessedTime(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
                    }
                    repository.save(question);
                }
            }
        }

        Elements pages = doc.select(nextUrlSelector);
        for (Element page : pages) {
            String text = page.text();
            if ("下一页".equals(text)) {
                String url = page.attr("href");
                String nextUrl = HttpUtils.normalizeUrl(baseUrl, url);
                logger.info("get next page url: {}", nextUrl);
                return nextUrl;
            }
        }
        return null;
    }

    private Question createQuestion(QuestionInfo info) {
        Question question;
        question = new Question();
        question.setId(info.getId());
        question.setUrl(info.getUrl());
        question.setTitle(info.getTitle());
        question.setCreatedTime(info.getCreatedTime());
        question.setUpdatedTime(info.getUpdatedTime());
        return question;
    }

    private long getQuestionId(String url) {
        Matcher matcher = QUESTION_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return Long.valueOf(matcher.group(1));
        }
        return 0L;
    }

    private QuestionInfo getQuestionInfo(long id) {
        // https://www.zhihu.com/api/v4/questions/40438585
        try {
            String url = questionApi.replace("{id}", String.valueOf(id));
            String json = HttpUtils.get(url, httpConfig);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(json);

            QuestionInfo info = new QuestionInfo();
            info.setId(id);
            info.setUrl((String) jsonObject.get("url"));
            info.setTitle((String) jsonObject.get("title"));
            info.setUpdatedTime((Long) jsonObject.get("updated_time"));
            info.setCreatedTime((Long) jsonObject.get("created"));
            return info;
        } catch (Exception e) {
            logger.warn("get question info failed!", e);
        }
        return null;
    }
}
