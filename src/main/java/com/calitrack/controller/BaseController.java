package com.calitrack.controller;

import com.calitrack.dto.ApiResponse;
import com.calitrack.entity.BaseEntity;
import com.calitrack.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public abstract class BaseController<T extends BaseEntity, ID extends Long> {
    
    protected abstract BaseService<T, ID> getService();
    
    protected ResponseEntity<ApiResponse<List<T>>> getAll() {
        List<T> entities = getService().findAll();
        return ResponseEntity.ok(ApiResponse.success(entities));
    }
    
    protected ResponseEntity<ApiResponse<Page<T>>> getAll(Pageable pageable) {
        Page<T> entities = getService().findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(entities));
    }
    
    protected ResponseEntity<ApiResponse<T>> getById(ID id) {
        Optional<T> entity = getService().findById(id);
        return entity.map(e -> ResponseEntity.ok(ApiResponse.success(e)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Entity not found")));
    }
    
    protected ResponseEntity<ApiResponse<T>> create(T entity) {
        T savedEntity = getService().save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Entity created successfully", savedEntity));
    }
    
    protected ResponseEntity<ApiResponse<T>> update(ID id, T entity) {
        if (!getService().existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Entity not found"));
        }
        T updatedEntity = getService().save(entity);
        return ResponseEntity.ok(ApiResponse.success("Entity updated successfully", updatedEntity));
    }
    
    protected ResponseEntity<ApiResponse<Void>> delete(ID id) {
        if (!getService().existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Entity not found"));
        }
        getService().deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Entity deleted successfully", null));
    }
}

