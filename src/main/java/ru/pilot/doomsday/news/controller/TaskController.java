package ru.pilot.doomsday.news.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pilot.doomsday.news.dto.CreateTaskDto;
import ru.pilot.doomsday.news.persist.DdnRssSource;
import ru.pilot.doomsday.news.persist.DdnRssSourceRepository;
import ru.pilot.doomsday.news.persist.DdnTask;
import ru.pilot.doomsday.news.persist.DdnTaskDraft;
import ru.pilot.doomsday.news.persist.DdnTaskRepository;
import ru.pilot.doomsday.news.persist.LoadState;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Log4j2
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class TaskController {
    
    private final DdnTaskRepository ddnTaskRepository;
    private final DdnRssSourceRepository ddnRssSourceRepository;

    /**
     * Создать задание на загрузку с archive.org загрузку RSS за указанный период
     * Задание работает в фоне
     */
    @PostMapping
    @Transactional
    public void createTask(CreateTaskDto createTaskDto) {
        LocalDate from = firstNonNull(createTaskDto.from(), LocalDate.now());
        LocalDate to  = firstNonNull(createTaskDto.to(), LocalDate.now());

        List<DdnRssSource> sourceList = ddnRssSourceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        List<DdnTask> ddnTasks = new ArrayList<>();
        from.datesUntil(to)
                .forEach(dt -> {
                            for (DdnRssSource source : sourceList) {
                                ddnTasks.add(createTask(dt, source, now));
                            }
                        }
                );

        ddnTaskRepository.saveAll(ddnTasks);
        log.info("Done creating tasks. ADD {} tasks.", ddnTasks.size());
    }
    
    private DdnTask createTask(LocalDate dt, DdnRssSource source, LocalDateTime now) {
        return DdnTaskDraft.$.produce(c -> {
            c.setCreateDate(now);
            c.setLoadState(LoadState.NEW);
            c.setRssDate(dt);
            c.setRssSource(source);
        });
    }
}
