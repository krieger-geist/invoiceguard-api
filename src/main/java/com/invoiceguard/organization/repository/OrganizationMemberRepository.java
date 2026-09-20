package com.invoiceguard.organization.repository;

import com.invoiceguard.organization.entity.OrganizationMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByUserId(UUID userId);

    Optional<OrganizationMember> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    List<OrganizationMember> findByOrganizationId(UUID organizationId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}
