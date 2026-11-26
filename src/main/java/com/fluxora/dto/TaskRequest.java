package com.fluxora.dto;

import com.fluxora.enums.TaskPriority;
import com.fluxora.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
    @NotBlank(message = "The title is mandatory.")
    @Size(max = 150, message = "The title must have a maximum of 150 characters.")
    String title,

    @Size(max = 1000, message = "The description must be a maximum of 1,000 characters.")
    String description,

    TaskStatus status,

    TaskPriority priority,

    LocalDate dueDate,

    Long assigneeId
){
    public TaskRequest(String title, String description){
        this(title, description, null, null, null,null);
    }
}
