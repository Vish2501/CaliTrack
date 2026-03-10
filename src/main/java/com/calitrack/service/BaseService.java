package com.calitrack.service;

import com.calitrack.entity.BaseEntity;
import com.calitrack.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public abstract class BaseService<T extends BaseEntity, ID extends Long> {
    
    protected abstract BaseRepository<T, ID> getRepository();
    
    public List<T> findAll() {
        return getRepository().findAll();
    }
    
    public Page<T> findAll(Pageable pageable) {
        return getRepository().findAll(pageable);
    }
    
    public Optional<T> findById(ID id) {
        return getRepository().findById(id);
    }
    
    public T save(T entity) {
        return getRepository().save(entity);
    }
    
    public void deleteById(ID id) {
        getRepository().deleteById(id);
    }
    
    public boolean existsById(ID id) {
        return getRepository().existsById(id);
    }
}

