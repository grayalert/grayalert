package com.github.grayalert.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DBManager {

    private final LogExampleRepository repo;
    private final EntityManager entityManager;
    private final Clock clock;

    @Transactional
    public List<LogExample> load() {
        TypedQuery<LogExample> query = entityManager.createQuery("SELECT le FROM LogExample le", LogExample.class);
        List<LogExample> resultList = query.getResultList();
        return resultList;
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
