package ru.pilot.doomsday.news.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Словари из ресурсов приложения
 */
public class DictionaryService {
   public static final Set<String> EXCLUDE_WORDS = new HashSet<>();
   public static final Set<String> WAR_WORDS = new HashSet<>();

   public static void load(){
       if (!EXCLUDE_WORDS.isEmpty()) {
           return;
       }
       EXCLUDE_WORDS.addAll(loadWords("dictionary/exclude.txt"));
       WAR_WORDS.addAll(loadWords("dictionary/war.txt"));
   }

    private static Set<String> loadWords(String fileName) {
        final Set<String> excludeWords;
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if (resourceAsStream == null){
            throw new RuntimeException(fileName + " not found!");
        }
        excludeWords = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))
                .lines()
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        return excludeWords;
    }

}
