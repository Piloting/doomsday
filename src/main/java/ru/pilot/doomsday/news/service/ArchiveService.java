package ru.pilot.doomsday.news.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.pilot.doomsday.news.util.RetryRestTemplate;
import ru.pilot.doomsday.news.dto.ArchiveCalendarTimes;

import static ru.pilot.doomsday.news.util.MiscUtils.YYYYMMDD;

/**
 * Сервис по работе с archive.org
 */
@Log4j2
@Service
@AllArgsConstructor
public class ArchiveService {
    private static final String ARCHIVE_URL = "http://web.archive.org";
    private static final String ARCHIVE_CALENDAR_POSTFIX_URL = ARCHIVE_URL + "/__wb/calendarcaptures/2?url=";

    private final RetryRestTemplate retryRestTemplate;

    /**
     * Получение из archive.org по ресурсу и году количество снимков в каждом дне.
     * Позволяет не делать запросы в дни, когда страница не сохранялась
     */
    public Map<LocalDate, Integer> getCountTimesByYear(Integer year, String rssUrl) {
        //GET http://web.archive.org/__wb/calendarcaptures/2?url=https%3A%2F%2Flenta.ru%2Frss&date=2024&groupby=day
        String url = ARCHIVE_CALENDAR_POSTFIX_URL + rssUrl + "&date=" + year + "&groupby=day";

        Map<LocalDate, Integer> countTimeMap = new HashMap<>();

        try {
            ResponseEntity<ArchiveCalendarTimes> response = retryRestTemplate.getForEntity(url, ArchiveCalendarTimes.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ArchiveCalendarTimes body = ObjectUtils.defaultIfNull(response.getBody(), new ArchiveCalendarTimes());
                for (List<Object> item : ListUtils.emptyIfNull(body.getItems())) {
                    if (item.size() < 3) {
                        continue;
                    }
                    String yyyymmdd = year + StringUtils.leftPad(item.get(0).toString(), 4, "0");
                    LocalDate date = LocalDate.parse(yyyymmdd, YYYYMMDD);

                    countTimeMap.put(date, Integer.parseInt(item.get(2).toString()));
                }
            }
            return countTimeMap;
        } catch (Exception e) {
            log.error("Error url: {}", url, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Получение из archive.org по ресурсу и дате ссылок на конкретные снимки в этом дне
     */
    public List<String> loadTimeUrls(LocalDate date, String rssUrl) {
        // сначала загружаем GET запросом метки времени за дату 
        List<Integer> timeList = getTimesFromArchive(date, rssUrl);
        // затем формируем по ним URL к архиву
        return timeToUrl(rssUrl, date, timeList);
    }

    /**
     * Получение из archive.org по ресурсу и дате временных меток (число, кодирующее время) на конкретные снимки в этом дне
     */
    private List<Integer> getTimesFromArchive(LocalDate date, String rssUrl) {
        //http://web.archive.org/__wb/calendarcaptures/2?url=https%3A%2F%2Fria.ru%2Fexport%2Frss2%2Farchive%2Findex.xml&date=20240620
        String url = ARCHIVE_CALENDAR_POSTFIX_URL + rssUrl + "&date=" + date.format(YYYYMMDD);
        
        try {
            ResponseEntity<ArchiveCalendarTimes> response = retryRestTemplate.getForEntity(url, ArchiveCalendarTimes.class);

            List<Integer> timeList = new ArrayList<>();
            if (response.getStatusCode().is2xxSuccessful()) {
                ArchiveCalendarTimes body = response.getBody();
                if (body != null) {
                    List<List<Object>> items = body.getItems();
                    if (items != null) {
                        timeList.addAll(parseJsonTimes(items));
                    }
                }
            }
            return timeList;
        } catch (Exception e) {
            log.error("Error url: {}", url, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Извлечение временных меток снимков из rest ответа
     */
    private static List<Integer> parseJsonTimes(List<List<Object>> items) {
        return items.stream()
                .filter(a -> "200".equals(a.get(1).toString()))
                .map(List::getFirst)
                .map(o -> StringUtils.isNumeric(o.toString()) ? Integer.parseInt(o.toString()) : null)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Преобразование временных меток (число, кодирующее время) в конкретные URL страниц RSS по этим меткам
     */
    private List<String> timeToUrl(String rssUrl, LocalDate date, List<Integer> timeList) {
        // http://web.archive.org/web/20240427025214/https://ria.ru/export/rss2/archive/index.xml
        List<String> urlList = new ArrayList<>(timeList.size());
        for (Integer time : timeList) {
            String timePart = StringUtils.leftPad(time.toString(), 6, "0");
            String url = ARCHIVE_URL + "/web/" + date.format(YYYYMMDD) + timePart + "if_" + "/" + rssUrl;
            urlList.add(url);
        }
        return urlList;
    }
}
