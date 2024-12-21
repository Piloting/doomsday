package ru.pilot.doomsday.news.persist;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.babyfish.jimmer.sql.EnumType;
import ru.pilot.doomsday.news.service.DictionaryService;

/**
 * Типы метрик и запросы, по их наполнению
 */
@Getter
@RequiredArgsConstructor
@EnumType(EnumType.Strategy.ORDINAL)
public enum Metric {
    // 0 Количество повторений каждого слова в каждом периоде. Число
    COUNT_WORDS(""" 
              insert into ddn_word_freq(
                     period_type, rss_date, word_id, word_count
              )
              select ?,
                     DATE_TRUNC('%PERIOD', new_date),
                     ddn_new_word.word_id,
                     count(1)
                from ddn_new
                join ddn_new_word on ddn_new_word.new_id = ddn_new.new_id
               where new_date between ? and ?
               group by DATE_TRUNC('%PERIOD', new_date), ddn_new_word.word_id
                  on conflict (word_id, period_type, rss_date)
                  do update set word_count = EXCLUDED.word_count;
            """,
            (periodType, startDate, endDate) -> List.of(periodType.ordinal(), startDate, endDate)),
    // 1 Количество новостей в каждом периоде. Число
    COUNT_NEWS("""
            insert into ddn_metric_value(
                        metric, period_type, rss_date, value_num, value_str
            )
            select ?,
                   ?,
                   DATE_TRUNC('%PERIOD', new_date),
                   count(1),
                   null
              from ddn_new
             where new_date between ? and ?
             group by DATE_TRUNC('%PERIOD', new_date)
               on conflict (metric, period_type, rss_date)
               do update set value_num = EXCLUDED.value_num;
            """,
            (periodType, startDate, endDate) -> List.of(1, periodType.ordinal(), startDate, endDate)),
    // 2 Топ 30 слов в каждом периоде. Строка
    TOP_30("""
            insert into ddn_metric_value(
                   metric, period_type, rss_date, value_num, value_str
            )
            select ?,
                   ?,
                   q.rss_date,
                   null,
                   string_agg(q.word || '|' || q.word_count, ', ')
              from (
                    select rss_date,
                           word_count,
                           word,
                           row_number() OVER (PARTITION BY rss_date ORDER BY word_count DESC) rang
                      from ddn_word_freq
                      join ddn_word on ddn_word.word_id = ddn_word_freq.word_id
                     where period_type = ?
                       and rss_date between ? and ?
            ) q
            where rang <= 30
            group by q.rss_date
               on conflict (metric, period_type, rss_date)
               do update set value_str = EXCLUDED.value_str;
            """,
            (periodType, startDate, endDate) -> List.of(2, periodType.ordinal(), periodType.ordinal(), startDate, endDate)),
    // 3 процент новостей о войне в каждом периоде
    WAR_PRC("""
            insert into ddn_metric_value(
                   metric, period_type, rss_date, value_num, value_str
            )
            select ?,
                   ?,
                   DATE_TRUNC('%PERIOD', new_date),
                   (count(1)::decimal)/ddn_metric_value.value_num * 100,
                   null
            from ddn_new
            join ddn_metric_value on ddn_metric_value.period_type = ?
                                 and ddn_metric_value.rss_date    = DATE_TRUNC('%PERIOD', new_date)
                                 and ddn_metric_value.metric      = 1
            where ddn_new.new_date between ? and ?
              and exists (
                          select 1
                            from ddn_new_word
                            join ddn_word on ddn_word.word_id = ddn_new_word.word_id
                           where ddn_new.new_id = ddn_new_word.new_id
                             and ddn_word.word in (%WAR_WORDS)
                         )
            group by DATE_TRUNC('%PERIOD', new_date), ddn_metric_value.value_num
               on conflict (metric, period_type, rss_date)
               do update set value_num = EXCLUDED.value_num;
            """,
            (periodType, startDate, endDate) -> ListUtils.union(
                    List.of(3, periodType.ordinal(), periodType.ordinal(), startDate, endDate), DictionaryService.WAR_WORDS.stream().toList())
    )

    ;

    // запрос с параметрами
    private final String calcQuery;
    // средство установки параметров в запрос
    private final MetricParamSetter paramSetter;

    public String getCalcQuery(PeriodType periodType) {
        return calcQuery
                .replaceAll("%PERIOD", periodType.name())
                .replaceAll("%WAR_WORDS", StringUtils.repeat("?", ",", DictionaryService.WAR_WORDS.size()));
    }

    public interface MetricParamSetter {
        List<Object> getParam(PeriodType periodType, LocalDate startDate, LocalDate endDate);
    }
}
