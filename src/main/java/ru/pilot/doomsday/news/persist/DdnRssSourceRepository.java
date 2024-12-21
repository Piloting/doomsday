package ru.pilot.doomsday.news.persist;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DdnRssSourceRepository extends JRepository<DdnRssSource, Long> {
    
}
