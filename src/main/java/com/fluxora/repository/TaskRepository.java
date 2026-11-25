package com.fluxora.repository;

import com.fluxora.dto.TaskStatisticsResponse;
import com.fluxora.entity.Task;
import com.fluxora.entity.TaskPriority;
import com.fluxora.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>{

    List<Task> findAllByProjectId(Long projectId);

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

    @Query("""
            SELECT new com.fluxora.dto.TaskStatisticsResponse(
                COUNT(t),
                COUNT(CASE WHEN t.status = com.fluxora.entity.TaskStatus.TODO THEN 1 ELSE NULL END),
                COUNT(CASE WHEN t.status = com.fluxora.entity.TaskStatus.IN_PROGRESS THEN 1 ELSE NULL END),
                COUNT(CASE WHEN t.status = com.fluxora.entity.TaskStatus.DONE THEN 1 ELSE NULL END),
                COUNT(CASE WHEN t.status = com.fluxora.entity.TaskStatus.CANCELED THEN 1 ELSE NULL END),
                COUNT(CASE WHEN t.dueDate < CURRENT_DATE AND t.status NOT IN(
                            com.fluxora.entity.TaskStatus.DONE,
                            com.fluxora.entity.TaskStatus.CANCELED
                            )
                            THEN 1
                            ELSE NULL
                        END)
                    )
                FROM Task t
                WHERE EXISTS (
                    SELECT pm.id
                    FROM ProjectMember pm
                    WHERE pm.project.id = t.project.id
                      AND pm.user.id = :userId
                )
            """)
    TaskStatisticsResponse findTaskStatistics(
            @Param("userId") Long userId
    );

    @Query("""
            SELECT t
            FROM Task t
            WHERE t.id = :taskId
                AND EXISTS (
                    SELECT pm.id
                    FROM ProjectMember pm
                    WHERE pm.project.id = t.project.id
                        AND pm.user.id = :userId
                )
            """)
    Optional<Task> findByIdAndProjectMemberUserId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

    @Query(value = """
            SELECT t
            FROM Task t
            WHERE EXISTS (
              SELECT pm.id
              FROM ProjectMember pm
              WHERE pm.project.id = t.project.id
                  AND pm.user.id = :userId
            )""",
            countQuery = """
            SELECT COUNT(t)
            FROM Task t
            WHERE EXISTS (
                SELECT pm.id
                FROM ProjectMember pm
                WHERE pm.project.id = t.project.id
                    AND pm.user.id = :userId
            )
""")
    Page<Task> findAllByProjectMemberUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM Task t
        WHERE EXISTS (
            SELECT pm.id
            FROM ProjectMember pm
            WHERE pm.project.id = t.project.id
              AND pm.user.id = :userId
        )
        """)
    List<Task> findAllTasksByProjectMemberUserId(
            @Param("userId") Long userId
    );

    @Query(value = """
            SELECT t
            FROM Task t
            WHERE t.project.id = :projectId
                AND EXISTS (
                SELECT pm.id
                FROM ProjectMember pm
                WHERE pm.project.id = t.project.id
                    AND pm.user.id = :userId
                )
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
                AND EXISTS (
                SELECT pm.id
                FROM ProjectMember pm
                WHERE pm.project.id = t.project.id
                    AND pm.user.id = :userId
                )
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
    Page<Task> findAllWithFiltersByProjectMember(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("dueDateFrom")LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("""
           SELECT t
           FROM Task t
           WHERE t.dueDate < :dueDate
                AND t.status NOT IN :statuses
                AND EXISTS (
                    SELECT pm.id
                    FROM ProjectMember pm
                    WHERE pm.project.id = t.project.id
                        AND pm.user.id = :userId
                )
           """)
    Page<Task> findOverdueTasksByProjectMember(
            @Param("userId") Long userId,
            @Param("dueDate") LocalDate dueDate,
            @Param("statuses") Collection<TaskStatus> statuses,
            Pageable pageable
    );

    @Query("""
           SELECT t
           FROM Task t
           WHERE t.dueDate = :dueDate
                AND t.status NOT IN :statuses
                AND EXISTS (
                    SELECT pm.id
                    FROM ProjectMember pm
                    WHERE pm.project.id = t.project.id
                        AND pm.user.id = :userId
                )
           """)
    Page<Task> findTasksDueTodayByProjectMember(
            @Param("userId") Long userId,
            @Param("dueDate") LocalDate dueDate,
            @Param("statuses") Collection<TaskStatus> statuses,
            Pageable pageable
    );

    @Query("""
           SELECT t
           FROM Task t
           WHERE t.dueDate BETWEEN :startDate AND :endDate
                AND t.status NOT IN :statuses
                AND EXISTS (
                    SELECT pm.id
                    FROM ProjectMember pm
                    WHERE pm.project.id = t.project.id
                        AND pm.user.id = :userId
                )
           """)
    Page<Task> findUpcomingTasksByProjectMember(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<TaskStatus> statuses,
            Pageable pageable
    );
}
