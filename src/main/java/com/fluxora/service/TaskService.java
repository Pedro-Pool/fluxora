package com.fluxora.service;

import com.fluxora.entity.Task;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }
    public List<Task> findAll(){
        return taskRepository.findAll();
    }
    public Task findById(Long id){
        return taskRepository.findById(id).
                orElseThrow(() ->
                        new TaskNotFoundException(id)
                );
    }
    @Transactional
    public Task create(String title, String description){
        Task task = new Task(title, description);
        return taskRepository.save(task);
    }
    @Transactional
    public Task update(Long id, String title, String description){
        Task task = findById(id);

        task.updateDetails(title, description);

        return task;
    }
    @Transactional
    public Task complete(Long id){
        Task task = findById(id);
        task.completed();
        return task;
    }
    @Transactional
    public Task reopen(Long id){
        Task task = findById(id);
        task.reopen();
        return task;
    }
    @Transactional
    public void delete(Long id){
        Task task = findById(id);
        taskRepository.delete(task);
    }

}
