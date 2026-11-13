package com.fashion.platform.finance.web;

import com.fashion.platform.finance.domain.entity.JournalEntryStatus;
import com.fashion.platform.finance.service.JournalEntryService;
import com.fashion.platform.finance.web.dto.JournalEntryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @PostMapping
    public ResponseEntity<JournalEntryDto.Response> createDraft(
            @RequestParam UUID legalEntityId,
            @Valid @RequestBody JournalEntryDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journalEntryService.createDraft(legalEntityId, request));
    }

    @PostMapping("/{entryId}/post")
    public ResponseEntity<JournalEntryDto.Response> post(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID entryId) {
        return ResponseEntity.ok(journalEntryService.post(legalEntityId, entryId));
    }

    @PostMapping("/{entryId}/reverse")
    public ResponseEntity<JournalEntryDto.Response> reverse(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID entryId,
            @RequestParam String reversalEntryNo,
            @RequestParam UUID currentPeriodId) {
        return ResponseEntity.ok(journalEntryService.reverse(legalEntityId, entryId, reversalEntryNo, currentPeriodId));
    }

    @GetMapping
    public ResponseEntity<Page<JournalEntryDto.Response>> list(
            @RequestParam UUID legalEntityId,
            @RequestParam(required = false) JournalEntryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.list(legalEntityId, status, pageable));
    }
}
