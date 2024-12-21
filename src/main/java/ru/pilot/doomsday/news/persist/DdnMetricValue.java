package ru.pilot.doomsday.news.persist;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;

@Entity
@KeyUniqueConstraint
public interface DdnMetricValue {
    @Id
    @Column(name = "metric_value_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    Metric metric();

    @Key
    LocalDate rssDate();

    @Key
    PeriodType periodType();

    @Nullable
    Long valueNum();

    @Nullable
    String valueStr();
}
