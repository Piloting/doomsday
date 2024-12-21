package ru.pilot.doomsday.news.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.demidko.aot.WordformMeaning;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import ru.pilot.doomsday.news.persist.DdnNew;
import ru.pilot.doomsday.news.persist.DdnNewDraft;
import ru.pilot.doomsday.news.persist.DdnWord;
import ru.pilot.doomsday.news.persist.DdnWordDraft;
import ru.pilot.doomsday.news.persist.DdnWordRepository;

/**
 * Сервис работы со словами - разбиение, пропуск фигни, приведение к нормальной форме, сохранение в БД
 */
@Log4j2
@Service
public class WordService {

    private final LuceneMorphology luceneMorph;
    private final DdnWordRepository ddnWordRepository;

    public WordService(DdnWordRepository ddnWordRepository) {
        this.ddnWordRepository = ddnWordRepository;

        try {
            luceneMorph =  new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Разбивка на слова заголовка новости и их сохранение
     */
    public DdnNew wordProcess(DdnNew ddnNew) {
        Set<String> rawWords = getRawWord(ddnNew.title());

        for (String rawWord : rawWords) {
            String normForm = getNormalForm(rawWord);
            if (notUtilWorld(normForm)){
                long idWord = addWord(normForm);
                ddnNew = DdnNewDraft.$.produce(ddnNew, n -> n.addIntoWords(w -> w.setId(idWord)));
            }
        }
        return ddnNew;
    }

    /**
     * Добавление слова через Jimmer
     */
    private long addWord(String word) {
        DdnWord newWord = DdnWordDraft.$.produce(w -> w.setWord(word));
        newWord = ddnWordRepository.save(newWord);
        return newWord.id();
    }

    /**
     * Приведение к нормальной форме слова
     */
    private String getNormalForm(String rawWord) {
        String normForm = null;
        // тут есть проблема - нормальных форм у слова может быть несколько, в зависимости от смысла слова.
        // как правильно выбрать нужную форму, я не придумал, поэтому беру 1 попавшуюся

        // 1 вариант - https://github.com/demidko/aot
        // если не знает слово - вернет null
        List<WordformMeaning> wordformList = WordformMeaning.lookupForMeanings(rawWord);
        if (CollectionUtils.isNotEmpty(wordformList)) {
            normForm = wordformList.stream()
                    .map(WordformMeaning::getLemma)
                    .map(WordformMeaning::toString)
                    .findFirst()
                    .orElse(null);
        }

        if (normForm == null) {
            // 2 вариант - https://github.com/AKuznetsov/russianmorphology
            // если не знает слово - придумывает)
            normForm = luceneMorph.getNormalForms(rawWord).stream().findFirst().orElse(null);
        }

        return ObjectUtils.firstNonNull(normForm, rawWord);
    }

    /**
     * Разбиение строки на отдельные слова, слова отделяются от ненужного
     */
    private Set<String> getRawWord(String title) {
        title = title.toLowerCase();
        title = title.replaceAll("[^а-я]", " ");
        title = title.replaceAll("\\s+", " ");
        Set<String> split = new HashSet<>(Arrays.asList(title.split("\\s")));
        return split.stream()
                .filter(this::notUtilWorld)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * Проверка, что слово важное
     */
    private boolean notUtilWorld(String tag) {
        return tag.length() > 1 && !DictionaryService.EXCLUDE_WORDS.contains(tag);
    }
}
