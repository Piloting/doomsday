package ru.pilot.doomsday.news.persist;


import java.time.LocalDateTime;
import java.util.List;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinTable;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.ManyToOne;

@Entity
@KeyUniqueConstraint
public interface DdnNew {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "new_id")
    long id();

    @Key
    String guid();

    LocalDateTime newDate();
    String title();
    String link();

    @ManyToOne
    DdnRssSource rssSource();

    @ManyToMany
    @JoinTable(
            name = "ddn_new_word",
            joinColumnName = "new_id",
            inverseJoinColumnName = "word_id"
    )
    List<DdnWord> words();
}
