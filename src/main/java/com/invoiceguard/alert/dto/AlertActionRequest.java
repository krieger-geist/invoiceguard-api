package com.invoiceguard.alert.dto;

import jakarta.validation.constraints.Size;

public record AlertActionRequest(@Size(max = 500) String comments) {}
