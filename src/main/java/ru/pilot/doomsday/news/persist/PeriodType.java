package ru.pilot.doomsday.news.persist;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

import org.babyfish.jimmer.sql.EnumType;

/**
 * Типы периодов для метрик + метод получения начала\конца этого периода
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum PeriodType {
    DAY(null, null),
    MONTH(TemporalAdjusters.firstDayOfMonth(), TemporalAdjusters.lastDayOfMonth()),
    YEAR(TemporalAdjusters.firstDayOfYear(), TemporalAdjusters.lastDayOfYear())
    ;

    private final TemporalAdjuster startTA;
    private final TemporalAdjuster endTA;

    PeriodType(TemporalAdjuster startTA, TemporalAdjuster endTA) {
        this.startTA = startTA;
        this.endTA = endTA;
    }

    public LocalDate getStart(LocalDate date) {
        return startTA != null ? date.with(startTA) : date;
    }

    public LocalDate getEnd(LocalDate date) {
        return endTA != null ? date.with(endTA) : date;
    }
}
