package com.fluxora.dto;

public record TaskStatisticsResponse(
        long totalTasks,
        long pendingTasks,
        long inProgressTasks,
        long completedTasks,
        long canceledTasks,
        long overdueTasks
) {
}
