package org.har01d.crawler.parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import org.har01d.crawler.bean.HttpConfig;
import org.har01d.crawler.domain.Answer;
import org.har01d.crawler.domain.AnswerRepository;
import org.har01d.crawler.domain.Image;
import org.har01d.crawler.domain.ImageRepository;
import org.har01d.crawler.domain.Question;
import org.har01d.crawler.util.HttpUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
public class QuestionPageParser implements QuestionParser {

    private static final Logger logger = LoggerFactory.getLogger(QuestionPageParser.class);

    @Autowired
    private LinkedBlockingQueue<Image> queue;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private HttpConfig httpConfig;

    @Value("${app.url.answer}")
    private String answerApi;

    @Override
    public boolean parse(Question question) throws IOException {
        int offset = 0;
        int limit = 20;
        boolean result = false;
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;

        while (true) {
            Map<String, String> data = new HashMap<>();
            data.put("sort_by", "default");
            data.put("include",
                "data[*].is_normal,is_sticky,collapsed_by,suggest_edit,comment_count,collapsed_counts,reviewing_comments_count,can_comment,content,editable_content,voteup_count,reshipment_settings,comment_permission,mark_infos,created_time,updated_time,relationship.is_authorized,is_author,voting,is_thanked,is_nothelp,upvoted_followees;data[*].author.is_blocking,is_blocked,is_followed,voteup_count,message_thread_token,badge[?(type=best_answerer)].topics");
            data.put("limit", String.valueOf(limit));
            data.put("offset", String.valueOf(offset));
            logger.info("{}: {}, limit: {}, offset: {}", question.getTitle(), question.getUrl(), limit, offset);
            String json;
            try {
                json = HttpUtils.get(answerApi.replace("{id}", String.valueOf(question.getId())), data, httpConfig);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }

            try {
                jsonObject = (JSONObject) parser.parse(json);
            } catch (ParseException e) {
                throw new IOException("parse JSON failed", e);
            }

            JSONArray items = (JSONArray) jsonObject.get("data");
            if (items.isEmpty()) {
                break;
            }

            for (Object item : items) {
                JSONObject object = (JSONObject) item;
                long id = (long) object.get("id");
                Answer answer = answerRepository.findOne(id);
                if (answer == null) {
                    answer = createAnswer(question, object);
                }

                if (answer.getAccessTime() >= answer.getUpdatedTime()) {
                    continue;
                }

                answer.setAccessTime(System.currentTimeMillis());
                answerRepository.save(answer);

                String content = (String) object.get("content");
                if (content == null) {
                    continue;
                }

                Document doc = Jsoup.parse(content);
                Elements images = doc.select("img");
                for (Element element : images) {
                    String imageUrl = element.attr("src");
                    logger.debug("original image url: {}", imageUrl);
                    if (isValidImage(imageUrl)) {
                        String name = getFileName(imageUrl);
                        Image image = imageRepository.findFirstByName(name);
                        if (image == null) {
                            image = new Image();
                            image.setUrl(imageUrl);
                            image.setName(name);
                            image.setAnswer(answer);
                        }
                        queue.offer(image);
                        result = true;
                    }
                }
            }

            offset += items.size();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        return result;
    }

    private Answer createAnswer(Question question, JSONObject object) {
        Answer answer;
        answer = new Answer();
        answer.setQuestion(question);
        answer.setId((Long) object.get("id"));
        answer.setUrl((String) object.get("url"));
        answer.setCreatedTime((Long) object.get("created_time"));
        answer.setUpdatedTime((Long) object.get("updated_time"));
        answer.setComments((Long) object.get("comment_count"));
        answer.setVotes((Long) object.get("voteup_count"));
        return answer;
    }

    private boolean isValidImage(String imageUrl) {
        return imageUrl != null && !imageUrl.isEmpty() && imageUrl.contains("zhimg.com") && imageUrl.contains("_b.");
    }

    private boolean isV2Image(String imageUrl) {
        return imageUrl.contains("v2-");
    }

    private String getFileName(String url) {
        String[] components = url.split("/");
        return components[components.length - 1];
    }

}
