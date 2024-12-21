package ru.pilot.doomsday.news.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.data.util.Pair;
import ru.pilot.doomsday.news.dto.SkipException;

import static org.apache.commons.lang3.ObjectUtils.min;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

public class MiscUtils {
    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final Charset ISO_8859_15 = Charset.forName("ISO-8859-15");
    public static final String WINDOWS_1251 = "windows-1251";
    public static final Charset WIN_1251 = Charset.forName(WINDOWS_1251);
    public static final String START_XML = "<?xml version=";

    public static String tm(long tm) {
        return DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - tm, true, true);
    }

    public static String removeBadSymbol(String title) {
        String s = title.startsWith("...") ? title.substring(3) : title;
        s = s.replace("\u200B", "");
        s = s.replace(" ", "");
        s = s.replace("\u200D", "");
        return s.trim();
    }

    public static String repairBadSymbols(String body) {
        // если в начале всякая фигня
        if (!body.startsWith(START_XML)) {
            int index = body.indexOf(START_XML);
            if (index < 0) {
                throw new SkipException("Bad XML");
            }
            body = body.substring(index);
        }

        // отрежем небольшой кусок в начале для ускорения поиска
        String top = body.substring(0, min(1000, body.length()));

        boolean existsRuSymbol = containsIgnoreCase(top, "е") || containsIgnoreCase(top, "а");
        if (!existsRuSymbol) {
            // исправление битой кодировки
            if (containsIgnoreCase(top, "encoding=\"" + WINDOWS_1251)) {
                body = changeEncoding(body, ISO_8859_15, WIN_1251);
            } else if (interfaxBadEncode(top)) {
                body = changeEncoding(body, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8);
            }
        }

        // удаление битых символов
        body = deleteBadSymbols(body);
        return body;
    }

    private static boolean interfaxBadEncode(String topRss) {
        return containsIgnoreCase(topRss, "encoding=\"utf-8") && topRss.contains("Ð");
    }

    public static String changeEncoding(String source, Charset from, Charset to) {
        byte[] fromBytes = source.getBytes(from);
        return new String(fromBytes, to);
    }

    public static String deleteBadSymbols(String body) {
        return body.replaceAll("\\p{Cntrl}", " ");
    }


    /**
     * bad. No more limit * 2
     */
    public static <T> List<T> limitElement(List<T> list, int limit) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        int size = list.size();
        int ever = size / limit;

        if (size <= limit) {
            return list;
        }

        List<T> newList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i % ever == 0) {
                newList.add(list.get(i));
            }
        }
        return newList;
    }

    public static List<Pair<LocalDate, LocalDate>> splitByYear(LocalDate start, LocalDate end) {
        if (start.getYear() == end.getYear()) {
            return Collections.singletonList(Pair.of(start, end));
        }

        List<Pair<LocalDate, LocalDate>> periodList = new ArrayList<>();

        LocalDate delimiter = start;
        for (int yearInt = start.getYear() ; yearInt <= end.getYear(); yearInt++) {
            LocalDate startYear = LocalDate.of(yearInt, 1, 1);
            LocalDate endYear = LocalDate.of(yearInt, 12, 31);

            periodList.add(Pair.of(
                    ObjectUtils.max(delimiter, startYear),
                    ObjectUtils.min(end, endYear)
            ));

            delimiter = endYear;
        }

        return periodList;
    }
}
