package com.invoiceguard.organization.service;

import com.invoiceguard.exception.DuplicateResourceException;
import com.invoiceguard.organization.entity.OrganizationMember;
import com.invoiceguard.organization.entity.OrganizationMemberStatus;
import com.invoiceguard.organization.repository.OrganizationMemberRepository;
import com.invoiceguard.security.Role;
import com.invoiceguard.user.entity.User;
import com.invoiceguard.user.entity.UserStatus;
import com.invoiceguard.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the link between users and organisations. Invitation in v1 creates
 * the user account eagerly (with a random unusable password the invitee
 * resets via the password-reset flow) rather than a separate "pending
 * invite" email token type — this keeps the token taxonomy to two kinds
 * (email verification, password reset) instead of three, and reuses the
 * password-reset flow as the invite-acceptance flow.
 */
@Service
public class OrganizationMemberService {

    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public OrganizationMemberService(OrganizationMemberRepository memberRepository, UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMember> listMembers(UUID organizationId) {
        return memberRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public OrganizationMember addExistingUserAsAdmin(UUID organizationId, UUID userId) {
        return addExistingUserWithRole(organizationId, userId, Role.ORGANIZATION_ADMIN);
    }

    /** Used both by registration (always ORGANIZATION_ADMIN) and by dev-only seed data (any role). */
    @Transactional
    public OrganizationMember addExistingUserWithRole(UUID organizationId, UUID userId, Role role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(organizationId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(OrganizationMemberStatus.ACTIVE);
        member.setJoinedAt(Instant.now());
        return memberRepository.save(member);
    }

    @Transactional
    public OrganizationMember invite(
            UUID organizationId, UUID invitedByUserId, String email, String firstName, String lastName, Role role) {
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseGet(() -> createPlaceholderUser(email, firstName, lastName));

        if (memberRepository.existsByUserIdAndOrganizationId(user.getId(), organizationId)) {
            throw new DuplicateResourceException("User is already a member of this organisation");
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(organizationId);
        member.setUserId(user.getId());
        member.setRole(role);
        member.setStatus(OrganizationMemberStatus.INVITED);
        member.setInvitedBy(invitedByUserId);
        return memberRepository.save(member);
    }

    private User createPlaceholderUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        // Random, never-communicated password hash — the invitee must go through
        // the password-reset flow to set a real password and activate the account.
        user.setPasswordHash("!" + UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }
}
