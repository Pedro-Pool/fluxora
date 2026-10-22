package com.fluxora.repository;

import com.fluxora.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUserId(Long userId);

    Optional<Project> findByIdAndUserId(Long id, Long userId);
}
