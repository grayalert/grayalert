package com.github.grayalert.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DBManager {

    private final LogExampleRepository repo;
    private final EntityManager entityManager;
    private final Clock clock;

    @Transactional
    public Long getMaxLastTimestamp() {
        return entityManager.createQuery("SELECT MAX(le.lastTimestamp) FROM LogExample le", Long.class)
            .getSingleResult();
    }

    @Transactional
    public List<LogExample> load(Long maxAge) {
        if (maxAge == null) {
            TypedQuery<LogExample> query = entityManager.createQuery(
                "SELECT le FROM LogExample le order by lastTimestamp desc, firstTimestamp desc", LogExample.class);
            return query.getResultList();
        } else {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(LogExample.class);
            var root = cq.from(LogExample.class);

            long cutoffTime = clock.millis() - (maxAge);

            var lastTimestampGt = cb.greaterThan(root.get("lastTimestamp"), cutoffTime);
            var lastTimestampNull = cb.isNull(root.get("lastTimestamp"));
            var firstTimestampGt = cb.greaterThan(root.get("firstTimestamp"), cutoffTime);
            var orPredicate = cb.or(
                lastTimestampGt,
                cb.and(lastTimestampNull, firstTimestampGt)
            );

            cq.select(root)
              .where(orPredicate)
              .orderBy(
                  cb.desc(root.get("lastTimestamp")),
                  cb.desc(root.get("firstTimestamp"))
              );

            return entityManager.createQuery(cq).getResultList();
        }
    }

    @Transactional
    public void save(List<LogExample> examples) {
        repo.saveAll(examples);
    }

    @Transactional
    public int deleteOlderThan(long seconds) {
        return deleteOlderThan(seconds, clock.millis());
    }

    @Transactional
    public void clear() {
        entityManager.createQuery("DELETE FROM LogExample")
                .executeUpdate();
    }
    @Transactional
    public int deleteOlderThan(long seconds, long referenceTime) {
        long cutoffTime = referenceTime - (seconds * 1000);
        return entityManager.createQuery("DELETE FROM LogExample le WHERE le.lastTimestamp < :cutoffTime OR (le.lastTimestamp IS NULL AND le.firstTimestamp < :cutoffTime)")
                .setParameter("cutoffTime", cutoffTime)
                .executeUpdate();
    }
}
