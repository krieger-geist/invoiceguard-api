package com.invoiceguard.approval.mapper;

import com.invoiceguard.approval.dto.ApprovalRequestResponse;
import com.invoiceguard.approval.dto.ApprovalStepResponse;
import com.invoiceguard.approval.entity.ApprovalRequest;
import com.invoiceguard.approval.entity.ApprovalStep;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApprovalMapper {

    ApprovalStepResponse toResponse(ApprovalStep step);

    default ApprovalRequestResponse toResponse(ApprovalRequest request, List<ApprovalStep> steps) {
        List<ApprovalStepResponse> stepResponses = steps.stream().map(this::toResponse).toList();
        return new ApprovalRequestResponse(
                request.getId(),
                request.getInvoiceId(),
                request.getStatus(),
                request.getRequiredApprovals(),
                request.getApprovalsReceived(),
                request.getSubmittedBy(),
                request.getDecidedAt(),
                stepResponses);
    }
}
