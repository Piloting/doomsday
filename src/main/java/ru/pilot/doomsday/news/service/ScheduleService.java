package ru.pilot.doomsday.news.service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import ru.pilot.doomsday.news.persist.DdnRssSource;
import ru.pilot.doomsday.news.persist.DdnRssSourceRepository;
import ru.pilot.doomsday.news.persist.LoadState;

import static ru.pilot.doomsday.news.util.MiscUtils.tm;

/**
 * Фоновые процессы
 *  - задание по загрузке старых RSS из archive.org
 *  - задание по текущей загрузке RSS с новостных сайтов
 *  - задание по расчету метрик за вчера
 */
@Log4j2
@Component
@RequiredArgsConstructor
@EnableScheduling
@ConditionalOnProperty(value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
public class ScheduleService {

    private final DdnRssSourceRepository ddnRssSourceRepository;
    private final TaskService taskService;
    private final MetricService metricService;

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(2);
        return threadPoolTaskScheduler;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void taskProcess() {
        // задание для выполнения тасков загрузки из архива
        long tm = System.currentTimeMillis();
        taskService.processByPage(1000, LoadState.NEW);
        log.info("END taskService. {}", tm(tm));
    }

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.HOURS)
    public void currentNewProcess() {
        // задание по загрузке текущий новостей из rss
        long tm = System.currentTimeMillis();
        for (DdnRssSource rssSource : ddnRssSourceRepository.findAll(Sort.by("id"))) {
            taskService.loadRss(rssSource);
        }
        log.info("END currentNewProcess. {}", tm(tm));
    }

    @Scheduled(cron = "0 0 1 * * *") // в 01:00
    public void taskCalcCountWord() {
        // Задание по подсчету статистики
        log.info("taskCalcMetric");

        // посчитать прошлый день (точнее с последнего расчета, вдруг не считали несколько дней)
        LocalDate now = LocalDate.now();
        LocalDate from = metricService.getMaxDateWordCount();

        metricService.calcMetrics(from, now);
    }

}
