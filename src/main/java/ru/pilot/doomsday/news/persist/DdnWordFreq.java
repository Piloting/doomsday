package ru.pilot.doomsday.news.persist;

import java.time.LocalDate;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.ManyToOne;

@Entity
@KeyUniqueConstraint
public interface DdnWordFreq {
    @Id
    @Column(name = "word_freq_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @ManyToOne
    DdnWord word();

    @Key
    LocalDate rssDate();

    @Key
    PeriodType periodType();

    Long wordCount();
}
