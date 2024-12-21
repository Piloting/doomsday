package ru.pilot.doomsday.news.util;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Обертка над RestTemplate
 * Добавлен: ретрай, таймаут, правка content-type
 */
@Log4j2
@Component
@EnableRetry
public class RetryRestTemplate {

    // archive.org блочит при частых вызовах. Добавим эмпирический таймаут
    private static LocalDateTime lastCallTime = null;
    private static final long CALL_NO_MORE_SEC = 13L;

    private final RestTemplate restTemplate;

    public RetryRestTemplate() {
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new BadAcceptInterceptor());
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 15000))
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
        log.info("HTTP call " + url);
        try {
            return getWithFakeHeaders(url, clazz);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                return getWithoutHeaders(url, clazz);
            } catch (Exception e2) {
                log.error("Try 2, {}", e.getMessage(), e);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Хз как правильно передавать куки, каким-то сайтам надо, каким-то нет, еще и когда как.
     * Тут вызов без кук
     */
    private <T> ResponseEntity<T> getWithoutHeaders(String url, Class<T> clazz) {
        waitIfNeed(url);
        ResponseEntity<T> forEntity = restTemplate.getForEntity(url, clazz);
        saveLastTime(url);
        return forEntity;
    }

    /**
     * Хз как правильно передавать куки, каким-то сайтам надо, каким-то нет, еще и когда как.
     * Тут вызов с фейковыми куками
     */
    private <T> ResponseEntity<T> getWithFakeHeaders(String url, Class<T> clazz) {
        waitIfNeed(url);
        ResponseEntity<T> exchange = restTemplate.exchange(url, HttpMethod.GET, mockHeader(), clazz);
        saveLastTime(url);
        return exchange;
    }

    /**
     * Сохраним в статике время последнего вызова archive.org для отсчета таймаута
     */
    private static void saveLastTime(String url) {
        if (url.contains("archive.org")) {
            lastCallTime = LocalDateTime.now();
        }
    }

    /**
     * Какие-то фейковые куки
     */
    public static HttpEntity<String> mockHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.setAcceptLanguage(Locale.LanguageRange.parse("ru,en;q=0.9"));
        headers.setCacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS));
        headers.add("priority", "u=0, i");
        headers.add("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"YaBrowser\";v=\"24.10\", \"Yowser\";v=\"2.5\"");
        headers.add("sec-ch-ua-mobile", "?0");
        headers.add("sec-ch-ua-platform", "\"Windows\"");
        headers.add("sec-fetch-dest", "document");
        headers.add("sec-fetch-mode", "navigate");
        headers.add("sec-fetch-site", "none");
        headers.add("sec-fetch-user", "?1");
        headers.add("upgrade-insecure-requests", "1");
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 YaBrowser/24.10.0.0 Safari/537.36");
        return new HttpEntity<>("body", headers);
    }

    /**
     * Таймаут для archive.org
     */
    private void waitIfNeed(String url) {
        if (lastCallTime == null || !url.contains("archive.org")) {
            return;
        }

        long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), lastCallTime.plusSeconds(CALL_NO_MORE_SEC));

        if (diff > 0) {
            try {
                Thread.sleep(diff);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}