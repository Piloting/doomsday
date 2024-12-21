package ru.pilot.doomsday.news.persist;

import java.time.LocalDate;
import java.util.List;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface DdnTaskRepository extends JRepository<DdnTask, Long> {
    Page<DdnTask> findDdnTaskByLoadState(LoadState loadState, Pageable pageRequest, Fetcher<DdnTask> fetcher);
    List<DdnTask> findDdnTaskByLoadStateAndRssDate(LoadState loadState, LocalDate rssDate, Fetcher<DdnTask> fetcher);

    default void saveTaskStatus(DdnTask ddnTask, LoadState loadState) {
        DdnTask updTask = DdnTaskDraft.$.produce(ddnTask, draft -> draft.setLoadState(loadState));
        save(updTask);
    }
}
