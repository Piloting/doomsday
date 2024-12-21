package ru.pilot.doomsday.news.persist;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DdnWordRepository extends JRepository<DdnWord, Long> {
    
}
