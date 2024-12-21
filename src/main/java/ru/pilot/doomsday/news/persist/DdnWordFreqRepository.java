package ru.pilot.doomsday.news.persist;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DdnWordFreqRepository extends JRepository<DdnWordFreq, Long> {
    
}
