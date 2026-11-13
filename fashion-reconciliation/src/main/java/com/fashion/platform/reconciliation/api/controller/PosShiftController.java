package com.fashion.platform.reconciliation.api.controller;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.reconciliation.api.dto.pos.*;
import com.fashion.platform.reconciliation.application.service.PosShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos/shifts")
@RequiredArgsConstructor
public class PosShiftController {

    private final PosShiftService posShiftService;

    @PostMapping("/open")
    public ResponseEntity<PosShiftResponse> openShift(@RequestBody OpenShiftRequest request) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        if (request.getCashierUsername() == null || request.getCashierUsername().isBlank()) {
            request.setCashierUsername(currentUser);
        }
        PosShiftResponse response = posShiftService.openShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PosShiftDetailResponse> closeShift(
            @PathVariable UUID id,
            @RequestBody CloseShiftRequest request) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        if (request.getPerformedBy() == null || request.getPerformedBy().isBlank()) {
            request.setPerformedBy(currentUser);
        }
        PosShiftDetailResponse response = posShiftService.closeShift(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PosShiftResponse>> getShifts(@RequestParam UUID legalEntityId) {
        return ResponseEntity.ok(posShiftService.getShiftsByLegalEntity(legalEntityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosShiftDetailResponse> getShiftDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(posShiftService.getShiftDetail(id));
    }
}
