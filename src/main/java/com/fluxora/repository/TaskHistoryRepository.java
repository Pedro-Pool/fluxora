package com.fluxora.repository;

import com.fluxora.entity.TaskHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    Page<TaskHistory> findAllByTaskId(Long taskId, Pageable pageable);

    Page<TaskHistory> findAllByTaskIdAndTaskProjectId(Long taskId, Long projectId, Pageable pageable);

    Page<TaskHistory> findAllByProjectId(Long projectId, Pageable pageable);

    @Transactional
    void deleteAllByUserId(Long userId);
}
