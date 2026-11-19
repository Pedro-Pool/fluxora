package com.fluxora.repository;

import com.fluxora.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUserId(Long userId);

    Page<Project> findDistinctByMembers_User_Id(Long userId, Pageable pageable);

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    Optional<Project> findByIdAndMembers_User_Id(Long id, Long userId);
}
