package com.fluxora.service;

import com.fluxora.entity.Task;
import com.fluxora.entity.User;
import com.fluxora.repository.TaskRepository;
import com.fluxora.exception.TaskNotFoundException;
import com.fluxora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository){

        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }
    public List<Task> findAll(String email){
        User user = findUserByEmail(email);

        return taskRepository.findAllByUserId(user.getId());
    }
    public Task findById(Long id, String email){
        User user = findUserByEmail(email);

        return findTaskByIdAndUser(id, user.getId());
    }
    @Transactional
    public Task create(String title, String description, String email){
        User user = findUserByEmail(email);

        Task task = new Task(title, description);
        task.assignTo(user);

        return taskRepository.save(task);
    }
    @Transactional
    public Task update(Long id, String title, String description, String email){
        User user = findUserByEmail(email);

        Task task = findTaskByIdAndUser(id, user.getId());
        task.updateDetails(title, description);

        return task;
    }
    @Transactional
    public Task complete(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndUser(id, user.getId());
        task.completed();
        return task;
    }
    @Transactional
    public Task reopen(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndUser(id, user.getId());
        task.reopen();
        return task;
    }
    @Transactional
    public void delete(Long id, String email){
        User user = findUserByEmail(email);
        Task task = findTaskByIdAndUser(id, user.getId());
        taskRepository.delete(task);
    }
    private User findUserByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Authenticated user not found"
        ));
    }
    private Task findTaskByIdAndUser(Long id, Long userId){
        return taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

}
