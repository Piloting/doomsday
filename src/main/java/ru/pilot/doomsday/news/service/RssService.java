package ru.pilot.doomsday.news.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.pilot.doomsday.news.dto.Rss;
import ru.pilot.doomsday.news.dto.SkipException;
import ru.pilot.doomsday.news.persist.DdnNew;
import ru.pilot.doomsday.news.persist.DdnNewRepository;
import ru.pilot.doomsday.news.util.RetryRestTemplate;
import ru.pilot.doomsday.news.util.XmlConverter;

import static ru.pilot.doomsday.news.util.MiscUtils.repairBadSymbols;

/**
 * Загрузка и сохранение RSS
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RssService {
    private final RetryRestTemplate retryRestTemplate;
    private final DdnNewRepository ddnNewRepository;
    private final WordService wordProcess;

    /**
     * Начитка rss.xml по-конкретному url.
     * Полученный текст сначала посильно исправляется, затем преобразовывается в объект Rss
     */
    public Rss getRssByUrl(String url) {
        ResponseEntity<String> response = retryRestTemplate.getForEntity(url, String.class);
        String body = response.getBody();

        if (StringUtils.isNotEmpty(body)){
            body = repairBadSymbols(body);
        } else {
            throw new SkipException("Empty XML");
        }

        return toRssObject(body);
    }

    /**
     * Преобразование строки из http ответа к dto RSS
     */
    private Rss toRssObject(String body) {
        try {
            return XmlConverter.xmlToDto(body);
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("invalid xml")) {
                throw new SkipException("Invalid XML");
            } else {
                log.error(body);
                throw e;
            }
        }
    }

    /**
     * Сохранить в БД новости с обработкой отдельных слов
     */
    public void saveNews(Collection<DdnNew> newList) {
        List<DdnNew> toSave = new ArrayList<>(newList.size());
        for (DdnNew ddnNew : newList) {
            // вытаскиваем из заголовка слова, преобразуем к нормальной форме и сохраняем в БД
            toSave.add(wordProcess.wordProcess(ddnNew));
        }
        ddnNewRepository.saveAll(toSave);
    }
}
