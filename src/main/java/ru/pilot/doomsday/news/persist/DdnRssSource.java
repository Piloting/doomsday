package ru.pilot.doomsday.news.persist;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;

@Entity
@Immutable
@KeyUniqueConstraint
public interface DdnRssSource {
    @Id
    @Column(name = "rss_source_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String url();
    String sourceName();
    boolean sourceEnabled();
}
