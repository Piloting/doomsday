package ru.pilot.doomsday.news.persist;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public interface DdnTask {
    @Id
    @Column(name = "task_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @ManyToOne
    DdnRssSource rssSource();

    @Key
    LocalDate rssDate();
    LocalDateTime createDate();

    LoadState loadState();
}
