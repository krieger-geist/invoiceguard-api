package com.invoiceguard.organization.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.organization.dto.MemberInviteRequest;
import com.invoiceguard.organization.dto.MemberResponse;
import com.invoiceguard.organization.entity.OrganizationMember;
import com.invoiceguard.organization.service.OrganizationMemberService;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.user.entity.User;
import com.invoiceguard.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/organizations/members")


@Tag(name = "Organization Members", description = "List and invite members into the current organisation.")
public class OrganizationMemberController {

    private final OrganizationMemberService organizationMemberService;
    private final UserService userService;
    private final TenantContext tenantContext;

    public OrganizationMemberController(
            OrganizationMemberService organizationMemberService, UserService userService, TenantContext tenantContext) {
        this.organizationMemberService = organizationMemberService;
        this.userService = userService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<List<MemberResponse>> listMembers(HttpServletRequest httpRequest) {
        List<OrganizationMember> members = organizationMemberService.listMembers(tenantContext.requireOrganizationId());
        List<MemberResponse> response = members.stream().map(this::toMemberResponse).toList();
        return ApiResponse.ok(response, correlationId(httpRequest));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ResponseEntity<ApiResponse<MemberResponse>> invite(
            @Valid @RequestBody MemberInviteRequest request, HttpServletRequest httpRequest) {
        OrganizationMember member = organizationMemberService.invite(
                tenantContext.requireOrganizationId(),
                tenantContext.requireUserId(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.role());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Member invited", toMemberResponse(member), correlationId(httpRequest)));
    }

    private MemberResponse toMemberResponse(OrganizationMember member) {
        User user = userService.getById(member.getUserId());
        return new MemberResponse(
                member.getId(),
                user.getId(),
                user.getEmail(),
                user.fullName(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt());
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
