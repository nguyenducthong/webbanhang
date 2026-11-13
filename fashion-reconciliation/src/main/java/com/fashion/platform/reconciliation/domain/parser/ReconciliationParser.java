package com.fashion.platform.reconciliation.domain.parser;

import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;

import java.io.InputStream;
import java.util.List;

public interface ReconciliationParser {
    List<ParsedTransaction> parse(InputStream inputStream);
    default boolean supports(ReconciliationSourceType type) {
        return false;
    }
}
