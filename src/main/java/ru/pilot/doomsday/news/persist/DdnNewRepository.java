package ru.pilot.doomsday.news.persist;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface DdnNewRepository extends JRepository<DdnNew, Long> {

    DdnNewTable tableNew = Tables.DDN_NEW_TABLE;

    default Page<DdnNew> findWithoutWords(Pageable pageable) {
        return sql()
                .createQuery(tableNew)
                .where(
                        Predicate.sql(
                              "not exists (select 1 from ddn_new_word where ddn_new_word.new_id = %e)",
                                it -> it.expression(tableNew.id())
                        )
                )
                .orderBy(tableNew.id())
                .select(tableNew)
                .fetchPage(pageable.getPageNumber(), pageable.getPageSize());
    }
}
