package com.fluxora.service;

import com.fluxora.entity.ProjectMember;
import com.fluxora.entity.ProjectRole;

public interface ProjectPermissionValidator {
    ProjectMember validateMembership(
            Long projectId,
            String requesterEmail
    );
    void validateOwner(ProjectMember membership);

    void validateCanAddMember(
            ProjectMember requesterMembership,
            ProjectRole role
    );
}
