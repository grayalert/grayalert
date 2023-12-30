package com.github.grayalert.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DBManager {

    private final EntityManager entityManager;
    @Transactional
    public List<LogExample> load() {
        TypedQuery<LogExample> query = entityManager.createQuery("SELECT le FROM LogExample le", LogExample.class);
        List<LogExample> resultList = query.getResultList();
        return resultList;
    }

    @Transactional
    public void save(List<LogExample> examples) {
        examples.forEach(entityManager::merge);
    }
}
