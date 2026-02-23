package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.DashboardSummary;
import com.ims.service.DashboardService;
import com.ims.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard and KPI operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "KPIs: revenue, sales, stock value, debts")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary(
            @RequestParam(required = false) Long branchId
    ) {
        Long effectiveBranchId = securityUtils.resolveBranchId(branchId);
        DashboardSummary summary = dashboardService.getDashboardSummary(effectiveBranchId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
