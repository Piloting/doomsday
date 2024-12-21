package ru.pilot.doomsday.news;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.pilot.doomsday.news.persist.DdnNew;
import ru.pilot.doomsday.news.persist.DdnNewDraft;
import ru.pilot.doomsday.news.persist.DdnNewRepository;
import ru.pilot.doomsday.news.persist.DdnRssSource;
import ru.pilot.doomsday.news.persist.DdnRssSourceRepository;
import ru.pilot.doomsday.news.persist.DdnTask;
import ru.pilot.doomsday.news.persist.DdnTaskDraft;
import ru.pilot.doomsday.news.persist.DdnTaskRepository;
import ru.pilot.doomsday.news.persist.DdnWord;
import ru.pilot.doomsday.news.persist.DdnWordDraft;
import ru.pilot.doomsday.news.persist.DdnWordRepository;
import ru.pilot.doomsday.news.persist.Fetchers;
import ru.pilot.doomsday.news.persist.LoadState;
import ru.pilot.doomsday.news.service.TaskService;

@SpringBootTest
public class EntityTest {
    @Autowired
    private DdnTaskRepository ddnTaskRepository;
    @Autowired
    private DdnRssSourceRepository ddnRssSourceRepository;
    @Autowired
    private DdnNewRepository ddnNewRepository;
    @Autowired
    private DdnWordRepository ddnWordRepository;
    @Autowired
    private TaskService taskService;

    @Test
    void addTask() {
        DdnRssSource rssSource = ddnRssSourceRepository.findAll(1, 1).get().findAny().get();

        long beforeCount = ddnTaskRepository.findAll(0, 10).getTotalElements();

        DdnTask newTask = DdnTaskDraft.$.produce(c -> {
            c.setCreateDate(LocalDateTime.now());
            c.setLoadState(LoadState.NEW);
            c.setRssDate(LocalDate.now());
            c.setRssSource(rssSource);
        });

        ddnTaskRepository.save(newTask);

        long afterCount = ddnTaskRepository.findAll(0, 10).getTotalElements();
        Assertions.assertEquals(beforeCount+1, afterCount);
    }

    @Test
    void addNew() {
        DdnRssSource rssSource = ddnRssSourceRepository.findAll(1, 1).get().findAny().get();

        long beforeCount = ddnNewRepository.findAll(1, 10).getTotalElements();

        DdnNew newNew = DdnNewDraft.$.produce(n -> {
            n.setGuid("Guid");
            n.setLink("Link");
            n.setNewDate(LocalDateTime.now());
            n.setRssSource(rssSource);
            n.setTitle("Title");
            n.setRssSource(rssSource);
        });

        ddnNewRepository.save(newNew);

        Page<DdnNew> all = ddnNewRepository.findAll(0, 10);
        long afterCount = all.getTotalElements();
        Assertions.assertEquals(beforeCount+1, afterCount);

        DdnNew ddnNew = all.getContent().getFirst();
        Assertions.assertEquals("Guid", ddnNew.guid());
        Assertions.assertEquals("Link", ddnNew.link());
        Assertions.assertEquals("Title", ddnNew.title());
    }

    @Test
    void addNewWithoutWord() {
        DdnRssSource rssSource = ddnRssSourceRepository.findAll(1, 1).get().findAny().get();

        long totalElements = ddnNewRepository.findAll(1, 10).getTotalElements();

        ddnNewRepository.save(createNew(rssSource, "My Word Title Title"));
        ddnNewRepository.save(createNew(rssSource, "My Word Title Title"));
        ddnNewRepository.save(createNew(rssSource, "My2 Word2 Title2 Title"));

        List<DdnNew> all = ddnNewRepository.findAll(
                Fetchers.DDN_NEW_FETCHER
                        .words(Fetchers.DDN_WORD_FETCHER.allTableFields()),
                Sort.by("id"));

        for (DdnNew ddnNew : all) {
            String words = ddnNew.words().stream().map(DdnWord::word).collect(Collectors.joining(", "));
            System.out.println(words);
        }

        String allWords = ddnWordRepository.findAll().stream().map(DdnWord::word).collect(Collectors.joining(", "));
        System.out.println(allWords);
    }

    @Test
    void addNewWithWord() {
        DdnRssSource rssSource = ddnRssSourceRepository.findAll(1, 1).get().findAny().get();

        long totalElements = ddnNewRepository.findAll(1, 10).getTotalElements();

        ddnNewRepository.save(createNew(rssSource, "My Word Title Title"));
        ddnNewRepository.save(createNew(rssSource, "My Word Title Title"));
        ddnNewRepository.save(createNew(rssSource, "My2 Word2 Title2 Title"));

        List<DdnNew> allNews = ddnNewRepository.findAll(
                Fetchers.DDN_NEW_FETCHER
                        .allTableFields()
                        .words(Fetchers.DDN_WORD_FETCHER.allTableFields()),
                Sort.by("id"));

        for (DdnNew ddnNew : allNews) {
            DdnNew updatesNews = ddnNew;
            for (String word : ddnNew.title().split(" ")) {
                long id = addWord(word);
                updatesNews = DdnNewDraft.$.produce(updatesNews, n -> n.addIntoWords(w -> w.setId(id)));
            }
            ddnNewRepository.save(updatesNews);
        }

        allNews = ddnNewRepository.findAll(
                Fetchers.DDN_NEW_FETCHER
                        .allTableFields()
                        .words(Fetchers.DDN_WORD_FETCHER.allTableFields()),
                Sort.by("id"));
        for (DdnNew ddnNew : allNews) {
            System.out.println(ddnNew.words().stream().map(DdnWord::word).collect(Collectors.joining(", ")));
        }

        String allWords = ddnWordRepository.findAll().stream().map(DdnWord::word).collect(Collectors.joining(", "));
        System.out.println(allWords);
    }

    private long addWord(String word) {
        DdnWord newWord = DdnWordDraft.$.produce(w -> w.setWord(word));
        newWord = ddnWordRepository.save(newWord);
        long id = newWord.id();
        return id;
    }

    private static DdnNew createNew(DdnRssSource rssSource, String title) {
        return DdnNewDraft.$.produce(n -> {
            n.setGuid(UUID.randomUUID().toString());
            n.setLink(UUID.randomUUID().toString());
            n.setNewDate(LocalDateTime.now());
            n.setRssSource(rssSource);
            n.setTitle(title);
            n.setRssSource(rssSource);
        });
    }

    @Test
    void selectNewWithoutWords() {
        org.babyfish.jimmer.Page<DdnNew> withoutWords = ddnNewRepository.findWithoutWords(Pageable.ofSize(1));
        List<DdnNew> rows = withoutWords.getRows();
        System.out.println(rows);
    }

    @Disabled
    @Test
    void DdnRssSource() {
        for (DdnRssSource rssSource : ddnRssSourceRepository.findAll(Sort.by("id"))) {
            taskService.loadRss(rssSource);
        }
    }
}
