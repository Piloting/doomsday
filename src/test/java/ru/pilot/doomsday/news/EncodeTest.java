package ru.pilot.doomsday.news;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.pilot.doomsday.news.util.BadAcceptInterceptor;
import ru.pilot.doomsday.news.util.MiscUtils;

import static ru.pilot.doomsday.news.util.RetryRestTemplate.mockHeader;

@Disabled
public class EncodeTest {

    @Test
    public void test() {
        String bad = "<title>Ð\u0098Ð½Ñ\u0082ÐµÑ\u0080Ñ\u0084Ð°ÐºÑ\u0081</title>";
        Set<Charset> charsets = Set.of(
                Charset.forName("windows-1251"),
                Charset.forName("windows-1252"),
                StandardCharsets.UTF_8,
                Charset.forName("ISO-8859-15"),
                StandardCharsets.ISO_8859_1
        );

        for (Charset charset1 : charsets) {
            for (Charset charset2 : charsets) {
                String s = MiscUtils.changeEncoding(bad, charset1, charset2);
                System.out.println(charset1 + " -> " + charset2 + ": " + s);
            }
        }
    }

    @Test
    void currentLoadTest() {
        String url = "http://web.archive.org/web/20190508033303if_/https://iz.ru/xml/rss/all.xml";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new BadAcceptInterceptor());

        ResponseEntity<String> forEntity = restTemplate.exchange(url, HttpMethod.GET, mockHeader(), String.class);
        String body = forEntity.getBody();
        System.out.println(body);

        ResponseEntity<String> forEntity2 = restTemplate.getForEntity(url, String.class);
        body = forEntity2.getBody();
        System.out.println(body);
    }
}
