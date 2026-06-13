package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.AccountingAuditLog;
import com.inventory.system.repository.AccountingAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class AccountingAuditAspect {

    private final AccountingAuditLogRepository accountingAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.inventory.system.service.AccountingService.*(..))")
    public Object auditAccountingMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        if (!isAuditedMutation(methodName)) {
            return joinPoint.proceed();
        }

        Object beforeState = requestSnapshot(signature.getParameterNames(), joinPoint.getArgs());
        Object result = joinPoint.proceed();

        AccountingAuditLog auditLog = new AccountingAuditLog();
        auditLog.setEntityType(entityType(methodName));
        auditLog.setEntityId(entityId(methodName, joinPoint.getArgs(), result));
        auditLog.setAction(actionName(methodName));
        auditLog.setBeforeState(toJson(beforeState));
        auditLog.setAfterState(toJson(result));
        auditLog.setUserId(currentUser());
        auditLog.setOccurredAt(LocalDateTime.now());
        accountingAuditLogRepository.save(auditLog);

        return result;
    }

    private boolean isAuditedMutation(String methodName) {
        return methodName.startsWith("create")
                || methodName.startsWith("record")
                || methodName.startsWith("complete")
                || methodName.startsWith("post")
                || methodName.startsWith("reverse")
                || methodName.startsWith("upload")
                || methodName.startsWith("delete")
                || methodName.startsWith("runDue");
    }

    private Map<String, Object> requestSnapshot(String[] parameterNames, Object[] args) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            snapshot.put(name, sanitize(args[i]));
        }
        return snapshot;
    }

    private Object sanitize(Object value) {
        if (value instanceof MultipartFile file) {
            Map<String, Object> fileState = new LinkedHashMap<>();
            fileState.put("filename", file.getOriginalFilename());
            fileState.put("contentType", file.getContentType());
            fileState.put("size", file.getSize());
            return fileState;
        }
        if (value != null && value.getClass().isArray()) {
            return Arrays.stream((Object[]) value).map(this::sanitize).toList();
        }
        return value;
    }

    private String entityType(String methodName) {
        if (methodName.contains("Attachment")) return "JOURNAL_ENTRY_ATTACHMENT";
        if (methodName.contains("TaxRate")) return "TAX_RATE";
        if (methodName.contains("JournalTemplate")) return "RECURRING_JOURNAL_TEMPLATE";
        if (methodName.contains("Journal") || methodName.contains("Entry") || methodName.startsWith("post")) return "JOURNAL_ENTRY";
        if (methodName.contains("Account")) return "CHART_OF_ACCOUNT";
        if (methodName.contains("Payable")) return "ACCOUNTS_PAYABLE";
        if (methodName.contains("Receivable")) return "ACCOUNTS_RECEIVABLE";
        if (methodName.contains("Treasury")) return "TREASURY";
        return "ACCOUNTING";
    }

    private String actionName(String methodName) {
        return methodName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    private String entityId(String methodName, Object[] args, Object result) {
        Object reflectedId = readId(result);
        if (reflectedId != null) {
            return reflectedId.toString();
        }
        if ((methodName.startsWith("delete") || methodName.startsWith("reverse") || methodName.startsWith("complete"))
                && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }

    private Object readId(Object value) {
        if (value == null || value instanceof Iterable<?>) {
            return null;
        }
        try {
            Method getId = value.getClass().getMethod("getId");
            return getId.invoke(value);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode()
                    .put("serializationError", e.getMessage())
                    .toString();
        }
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return Objects.toString(authentication.getName(), "system");
    }
}
