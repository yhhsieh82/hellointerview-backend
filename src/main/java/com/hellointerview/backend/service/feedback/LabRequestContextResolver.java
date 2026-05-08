package com.hellointerview.backend.service.feedback;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
class LabRequestContextResolver {

    private static final String HEADER_STRATEGY_ID = "X-Lab-Strategy-Id";
    private static final String HEADER_FAULT_MODE = "X-Lab-Fault-Mode";
    private static final String HEADER_FAULT_START_SEC = "X-Lab-Fault-Start-Sec";
    private static final String HEADER_FAULT_END_SEC = "X-Lab-Fault-End-Sec";
    private static final String HEADER_TEST_START_EPOCH_SEC = "X-Lab-Test-Start-Epoch-Sec";

    LabRequestContext resolve() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return LabRequestContext.empty();
        }
        HttpServletRequest request = servletAttributes.getRequest();
        if (request == null) {
            return LabRequestContext.empty();
        }
        return new LabRequestContext(
                trimToNull(request.getHeader(HEADER_STRATEGY_ID)),
                trimToNull(request.getHeader(HEADER_FAULT_MODE)),
                parseLongOrNull(request.getHeader(HEADER_FAULT_START_SEC)),
                parseLongOrNull(request.getHeader(HEADER_FAULT_END_SEC)),
                parseLongOrNull(request.getHeader(HEADER_TEST_START_EPOCH_SEC))
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    record LabRequestContext(
            String strategyId,
            String faultMode,
            Long faultStartSec,
            Long faultEndSec,
            Long testStartEpochSec
    ) {
        static LabRequestContext empty() {
            return new LabRequestContext(null, null, null, null, null);
        }
    }
}
