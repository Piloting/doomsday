package ru.pilot.doomsday.news.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import ru.pilot.doomsday.news.persist.Metric;
import ru.pilot.doomsday.news.persist.PeriodType;
import ru.pilot.doomsday.news.util.MiscUtils;
import ru.pilot.doomsday.news.util.SqlUtil;

/**
 * Сервис по расчету метрик
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class MetricService {

    private final DataSource ds;

    /**
     * Максимальная дата рассчитанных метрик
     */
    public LocalDate getMaxDateWordCount() {
        return SqlUtil.simpleSelectGetDate(ds, "select max(rss_date) from ddn_word_freq", null);
    }

    /**
     * Расчет всех имеющихся метрик за период
     */
    public void calcMetrics(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate.isEqual(startDate) || endDate.isBefore(startDate)) {
            // Если с startDate проблема - ставим от endDate вчера
            startDate = endDate.plusDays(-1);
        }

        List<PeriodType> periodTypes = new ArrayList<>();
        periodTypes.add(PeriodType.DAY);

        if (startDate.getDayOfMonth() == 1 || ChronoUnit.MONTHS.between(startDate, endDate) > 0) {
            // нужно считать месяцы
            periodTypes.add(PeriodType.MONTH);
        }

        if (startDate.getDayOfYear() == 1 || ChronoUnit.YEARS.between(startDate, endDate) > 0) {
            // нужно считать годы
            periodTypes.add(PeriodType.YEAR);
        }

        for (Metric metric : Metric.values()) {
            for (PeriodType periodType : periodTypes) {
                calc(metric, periodType, startDate, endDate);
            }
        }
    }

    /**
     * Расчет конкретной метрики в периоде
     */
    private void calc(Metric metric, PeriodType periodType, LocalDate startDate, LocalDate endDate) {
        LocalDate firstDayPeriod = periodType.getStart(startDate);
        LocalDate endDayPeriod = periodType.getEnd(endDate);

        // если период больше года - разбить на выполнение по 1 году
        List<Pair<LocalDate, LocalDate>> periods = MiscUtils.splitByYear(firstDayPeriod, endDayPeriod);
        for (Pair<LocalDate, LocalDate> period : periods) {
            int count = SqlUtil.simpleUpdate(ds,
                    metric.getCalcQuery(periodType),
                    metric.getParamSetter().getParam(periodType, period.getFirst(), period.getSecond())
            );
            log.info("Calc metric: {} by {} - {} ({})", metric.name(), periodType.name(), period, count);
        }
    }
}
