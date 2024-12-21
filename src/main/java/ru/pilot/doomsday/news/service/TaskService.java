package ru.pilot.doomsday.news.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.rometools.rome.feed.rss.Guid;
import com.rometools.rome.feed.rss.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.babyfish.jimmer.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.pilot.doomsday.news.dto.Rss;
import ru.pilot.doomsday.news.dto.SkipException;
import ru.pilot.doomsday.news.persist.DdnNew;
import ru.pilot.doomsday.news.persist.DdnNewDraft;
import ru.pilot.doomsday.news.persist.DdnRssSource;
import ru.pilot.doomsday.news.persist.DdnTask;
import ru.pilot.doomsday.news.persist.DdnTaskRepository;
import ru.pilot.doomsday.news.persist.Fetchers;
import ru.pilot.doomsday.news.persist.LoadState;
import ru.pilot.doomsday.news.util.MiscUtils;

import static java.util.stream.Collectors.groupingBy;
import static ru.pilot.doomsday.news.util.MiscUtils.removeBadSymbol;
import static ru.pilot.doomsday.news.util.MiscUtils.tm;

/**
 * Сервис по обработке заданий на загрузку RSS из archive.org
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DdnTaskRepository ddnTaskRepository;
    private final ArchiveService archiveService;
    private final RssService rssService;

    public void processByPage(int pageSize, LoadState loadState) {
        Page<DdnTask> taskPage = ddnTaskRepository.findDdnTaskByLoadState(
                loadState,
                PageRequest.of(0, pageSize, Sort.by("rssDate")),
                Fetchers.DDN_TASK_FETCHER.allScalarFields().rssSource(Fetchers.DDN_RSS_SOURCE_FETCHER.allTableFields())
        );

        process(taskPage.getRows());
    }

    private void process(List<DdnTask> taskPage) {
        // группировка по годам
        Map<Integer, List<DdnTask>> tasksByDate = taskPage
                .stream()
                .collect(groupingBy(t -> t.rssDate().getYear()));

        for (Map.Entry<Integer, List<DdnTask>> entry : tasksByDate.entrySet()) {
            Integer year = entry.getKey();
            List<DdnTask> taskList = entry.getValue();

            Set<String> allRssInYearUrls = taskList.stream().map(t -> t.rssSource().url()).collect(Collectors.toSet());

            Map<LocalDate, Set<String>> dateToExistRssMap = getCalendarRss(year, allRssInYearUrls);

            // сортировка для последовательной повторной обработки
            Comparator<DdnTask> comparing = Comparator.comparing(DdnTask::rssDate);
            comparing = comparing.thenComparing(t -> t.rssSource().id());
            taskList.sort(comparing);

            for (DdnTask ddnTask : taskList) {
                try {
                    processTaskItem(ddnTask, dateToExistRssMap);
                } catch (Exception e) {
                    log.error("ERROR processTaskItem {} - {}", ddnTask.rssDate(), ddnTask.rssSource().url(), e);
                    ddnTaskRepository.saveTaskStatus(ddnTask, LoadState.ERROR);
                }
            }
        }
    }

    private void processTaskItem(DdnTask ddnTask, Map<LocalDate, Set<String>> dateToExistRssMap) {
        long tm = System.currentTimeMillis();
        int addCount = 0;

        LocalDate localDate = ddnTask.rssDate();
        DdnRssSource rssSource = ddnTask.rssSource();

        if (rssExistInDate(dateToExistRssMap, localDate, rssSource)) {
            // запросить времена конкретных снимков за этот день
            List<String> rssTimeByDateUrls = loadTimeUrls(ddnTask);

            Map<String, DdnNew> noDuplicateNewsMap = new HashMap<>();
            // пройдемся по конкретным снимкам в течение дня
            for (String rssTimeByDateUrl : rssTimeByDateUrls) {
                // запросить конкретные страницы с RSS
                Rss rssByUrl;
                try {
                    rssByUrl = rssService.getRssByUrl(rssTimeByDateUrl);
                } catch (SkipException e) {
                    log.error(e.getMessage(), e);
                    continue;
                }

                for (Item item : rssByUrl.getItems()) {
                    rssItemProcess(item, rssSource, noDuplicateNewsMap);
                }
            }

            rssService.saveNews(noDuplicateNewsMap.values());
            addCount = noDuplicateNewsMap.size();
        }

        // сохранить состояние
        ddnTaskRepository.saveTaskStatus(ddnTask, LoadState.SUCCESS);

        log.info("{} ADD {} - {}, {}", localDate, addCount, rssSource.url(), tm(tm));
    }

    public void loadRss(DdnRssSource rssSource){
        // запросить конкретную страницу с RSS
        try {
            Map<String, DdnNew> noDuplicateNewsMap = new HashMap<>();
            Rss rssByUrl = rssService.getRssByUrl(rssSource.url());
            for (Item item : rssByUrl.getItems()) {
                rssItemProcess(item, rssSource, noDuplicateNewsMap);
            }
            rssService.saveNews(noDuplicateNewsMap.values());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void rssItemProcess(Item item, DdnRssSource rssSource, Map<String, DdnNew> noDuplicateNewsMap) {
        if (existsRequiredField(item)) {
            try {
                DdnNew aNew = createNew(item, rssSource);
                noDuplicateNewsMap.put(aNew.guid(), aNew);
            } catch (SkipException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private boolean existsRequiredField(Item item) {
        String guid = getGuid(item);
        return StringUtils.isNotBlank(item.getTitle()) && item.getPubDate() != null && StringUtils.isNotBlank(guid);
    }

    private boolean rssExistInDate(Map<LocalDate, Set<String>> dateToExistRssMap, LocalDate localDate, DdnRssSource rssSource) {
        Set<String> existInDateSources = dateToExistRssMap.get(localDate);
        return existInDateSources != null && existInDateSources.contains(rssSource.url());
    }

    private DdnNew createNew(Item item, DdnRssSource source) {
        return DdnNewDraft.$.produce(c -> {
            try {
                c.setGuid(getGuid(item));
                c.setLink(item.getLink());
                c.setTitle(removeBadSymbol(item.getTitle()));
                c.setNewDate(LocalDateTime.ofInstant(item.getPubDate().toInstant(), ZoneId.systemDefault()));
                c.setRssSourceId(source.id());
            } catch (Exception e) {
                log.error(item.toString());
                throw new SkipException("Bad ITEM");
            }
        });
    }

    private String getGuid(Item item) {
        Guid guid = item.getGuid();
        if (guid == null || StringUtils.isEmpty(guid.getValue())) {
            return item.getLink();
        } else {
            return guid.getValue();
        }
    }

    private Map<LocalDate, Set<String>> getCalendarRss(Integer year, Set<String> urls) {
        Map<LocalDate, Set<String>> dateToExistRssMap = new HashMap<>();
        for (String url : urls) {
            // получить данные
            Map<LocalDate, Integer> countTimesByYear = archiveService.getCountTimesByYear(year, url);

            // объединить
            for (Map.Entry<LocalDate, Integer> entry : countTimesByYear.entrySet()) {
                dateToExistRssMap.merge(entry.getKey(), new HashSet<>(Set.of(url)), (a,b) -> {b.addAll(a); return b;});
            }
        }

        return dateToExistRssMap;
    }

    private List<String> loadTimeUrls(DdnTask task) {
        List<String> urls = archiveService
                .loadTimeUrls(
                        task.rssDate(),
                        task.rssSource().url()
                );
        return MiscUtils.limitElement(urls, 7);
    }
}
