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

    Optional<Task> findByIdAndProjectUserId(Long id, Long userId);

    List<Task> findAllByProjectUserId(Long id);

    Page<Task> findAllByProjectUserIdAndDueDateBeforeAndStatusNotIn(
            Long userId,
            LocalDate today,
            List<TaskStatus> excludedStatuses,
            Pageable pageable);

    Page<Task> findAllByProjectUserIdAndDueDateAndStatusNotIn(
            Long userId,
            LocalDate today,
            List<TaskStatus> excludedStatuses,
            Pageable pageable);

    Page<Task> findAllByProjectUserIdAndDueDateBetweenAndStatusNotIn(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            List<TaskStatus> excludedStatuses,
            Pageable pageable);

    @Query(value = """
            SELECT t
            FROM Task t
            WHERE t.project.id = :projectId
                AND t.project.user.id = :userId
                AND (:status IS NULL OR t.status = :status)
                AND (:priority IS NULL OR t.priority = :priority)
                AND (CAST(:dueDateFrom AS date) IS NULL OR t.dueDate >= :dueDateFrom)
                AND (CAST(:dueDateTo AS date) IS NULL OR t.dueDate <= :dueDateTo)
                AND (:categoryId IS NULL OR EXISTS (
                    SELECT c.id
                    FROM Task taskWithCategory
                    JOIN taskWithCategory.categories c
                    WHERE taskWithCategory.id = t.id
                        AND c.id = :categoryId
                        AND c.user.id = :userId
                                    ))
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
                        AND (:categoryId IS NULL OR EXISTS (
                                SELECT c.id
                                FROM Task taskWithCategory
                                JOIN taskWithCategory.categories c
                                WHERE taskWithCategory.id = t.id
                                    AND c.id = :categoryId
                                    AND c.user.id = :userId
                                                ))
                        """)
    Page<Task> findAllWithFilters(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("priority")TaskPriority priority,
            @Param("dueDateFrom")LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("categoryId") Long categoryId,
            Pageable pageable
            );

}
