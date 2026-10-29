package com.fluxora.repository;

import com.fluxora.entity.Task;
import com.fluxora.entity.TaskPriority;
import com.fluxora.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>{
    List<Task> findAllByProjectIdAndProjectUserId(Long projectId, Long userId);

    Optional<Task> findByIdAndProjectUserId(Long id, Long userId);

    List<Task> findAllByProjectUserId(Long id);

    @Query(value = """
            SELECT t
            FROM Task t
            WHERE t.project.id = :projectId
                AND t.project.user.id = :userId
                AND (:status IS NULL OR t.status = :status)
                AND (:priority IS NULL OR t.priority = :priority)
                AND (CAST(:dueDateFrom AS date) IS NULL OR t.dueDate >= :dueDateFrom)
                AND (CAST(:dueDateTo AS date) IS NULL OR t.dueDate <= :dueDateTo)
            """,
            countQuery = """
                        SELECT COUNT(t)
                        FROM Task t
                        WHERE t.project.id = :projectId
                        AND t.project.user.id = :userId
                        AND (:status IS NULL OR t.status = :status)
                        AND (:priority IS NULL OR t.priority = :priority)
                        AND (CAST(:dueDateFrom AS date) IS NULL OR t.dueDate >= :dueDateFrom)
                        AND (CAST(:dueDateTo AS date) IS NULL OR t.dueDate <= :dueDateTo)
                        """)
    Page<Task> findAllWithFilters(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("priority")TaskPriority priority,
            @Param("dueDateFrom")LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            Pageable pageable
            );

}
