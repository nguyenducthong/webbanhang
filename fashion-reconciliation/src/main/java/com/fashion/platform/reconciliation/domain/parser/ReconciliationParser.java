package com.fashion.platform.reconciliation.domain.parser;

import java.io.InputStream;
import java.util.List;

public interface ReconciliationParser {
    List<ParsedTransaction> parse(InputStream inputStream);
}
