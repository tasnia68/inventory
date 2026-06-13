package com.inventory.system.controller;

import com.inventory.system.payload.AccountingJournalDto;
import com.inventory.system.payload.AccountingAuditLogDto;
import com.inventory.system.payload.AccountLedgerDto;
import com.inventory.system.payload.AccountsPayableInvoiceDto;
import com.inventory.system.payload.AccountsReceivableInvoiceDto;
import com.inventory.system.payload.AgingSummaryRowDto;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ChartOfAccountDto;
import com.inventory.system.payload.CompleteTreasuryReconciliationRequest;
import com.inventory.system.payload.CashFlowRowDto;
import com.inventory.system.payload.CreateAccountsPayableInvoiceRequest;
import com.inventory.system.payload.CreateAccountsReceivableInvoiceRequest;
import com.inventory.system.payload.CreateAccountingJournalRequest;
import com.inventory.system.payload.CreateChartOfAccountRequest;
import com.inventory.system.payload.CreateManualJournalEntryRequest;
import com.inventory.system.payload.CreateRecurringJournalTemplateRequest;
import com.inventory.system.payload.CreateTaxRateRequest;
import com.inventory.system.payload.CreateTreasuryAccountRequest;
import com.inventory.system.payload.CreateTreasuryReconciliationRequest;
import com.inventory.system.payload.FinancialStatementRowDto;
import com.inventory.system.payload.FinancialEventDto;
import com.inventory.system.payload.JournalEntryAttachmentDto;
import com.inventory.system.payload.JournalEntryDto;
import com.inventory.system.payload.RecordAccountsPayablePaymentRequest;
import com.inventory.system.payload.RecordAccountsReceivablePaymentRequest;
import com.inventory.system.payload.RecurringJournalTemplateDto;
import com.inventory.system.payload.ReverseJournalEntryRequest;
import com.inventory.system.payload.TaxRateDto;
import com.inventory.system.payload.TreasuryAccountDto;
import com.inventory.system.payload.TreasuryReconciliationDto;
import com.inventory.system.payload.TrialBalanceRowDto;
import com.inventory.system.payload.VatReturnRowDto;
import com.inventory.system.service.AccountingService;
import com.inventory.system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;
    private final FileStorageService fileStorageService;

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountingAuditLogDto>>> getAccountingAuditLog(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccountingAuditLog(limit)));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<ChartOfAccountDto>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccounts()));
    }

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<ChartOfAccountDto>> createAccount(@RequestBody CreateChartOfAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createAccount(request), "Account created successfully"));
    }

    @GetMapping("/tax-rates")
    public ResponseEntity<ApiResponse<List<TaxRateDto>>> getTaxRates() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getTaxRates()));
    }

    @PostMapping("/tax-rates")
    public ResponseEntity<ApiResponse<TaxRateDto>> createTaxRate(@RequestBody CreateTaxRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createTaxRate(request), "Tax rate created successfully"));
    }

    @GetMapping("/accounts/{accountId}/ledger")
    public ResponseEntity<ApiResponse<AccountLedgerDto>> getAccountLedger(@PathVariable UUID accountId,
                                                                          @RequestParam(required = false) LocalDate from,
                                                                          @RequestParam(required = false) LocalDate to,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccountLedger(accountId, from, to, page, size)));
    }

    @GetMapping("/journals")
    public ResponseEntity<ApiResponse<List<AccountingJournalDto>>> getJournals() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getJournals()));
    }

    @PostMapping("/journals")
    public ResponseEntity<ApiResponse<AccountingJournalDto>> createJournal(@RequestBody CreateAccountingJournalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createJournal(request), "Journal created successfully"));
    }

    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> getEntries() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getJournalEntries()));
    }

    @GetMapping("/ap/invoices")
    public ResponseEntity<ApiResponse<List<AccountsPayableInvoiceDto>>> getAccountsPayableInvoices() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccountsPayableInvoices()));
    }

    @PostMapping("/ap/invoices")
    public ResponseEntity<ApiResponse<AccountsPayableInvoiceDto>> createAccountsPayableInvoice(@RequestBody CreateAccountsPayableInvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createAccountsPayableInvoice(request), "Accounts payable invoice created"));
    }

    @PostMapping("/ap/invoices/{invoiceId}/payments")
    public ResponseEntity<ApiResponse<AccountsPayableInvoiceDto>> recordAccountsPayablePayment(@PathVariable UUID invoiceId,
                                                                                               @RequestBody RecordAccountsPayablePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.recordAccountsPayablePayment(invoiceId, request), "Accounts payable payment recorded"));
    }

    @GetMapping("/ap/aging")
    public ResponseEntity<ApiResponse<List<AgingSummaryRowDto>>> getAccountsPayableAging() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccountsPayableAging()));
    }

    @GetMapping("/ar/invoices")
    public ResponseEntity<ApiResponse<List<AccountsReceivableInvoiceDto>>> getAccountsReceivableInvoices() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccountsReceivableInvoices()));
    }

    @PostMapping("/ar/invoices")
    public ResponseEntity<ApiResponse<AccountsReceivableInvoiceDto>> createAccountsReceivableInvoice(@RequestBody CreateAccountsReceivableInvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createAccountsReceivableInvoice(request), "Accounts receivable invoice created"));
    }

    @PostMapping("/ar/invoices/{invoiceId}/payments")
    public ResponseEntity<ApiResponse<AccountsReceivableInvoiceDto>> recordAccountsReceivablePayment(@PathVariable UUID invoiceId,
                                                                                                     @RequestBody RecordAccountsReceivablePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.recordAccountsReceivablePayment(invoiceId, request), "Accounts receivable payment recorded"));
    }

    @GetMapping("/ar/aging")
    public ResponseEntity<ApiResponse<List<AgingSummaryRowDto>>> getAccountsReceivableAging() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccountsReceivableAging()));
    }

    @GetMapping("/treasury/accounts")
    public ResponseEntity<ApiResponse<List<TreasuryAccountDto>>> getTreasuryAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getTreasuryAccounts()));
    }

    @PostMapping("/treasury/accounts")
    public ResponseEntity<ApiResponse<TreasuryAccountDto>> createTreasuryAccount(@RequestBody CreateTreasuryAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createTreasuryAccount(request), "Treasury account created"));
    }

    @GetMapping("/treasury/reconciliations")
    public ResponseEntity<ApiResponse<List<TreasuryReconciliationDto>>> getTreasuryReconciliations() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getTreasuryReconciliations()));
    }

    @PostMapping("/treasury/reconciliations")
    public ResponseEntity<ApiResponse<TreasuryReconciliationDto>> createTreasuryReconciliation(@RequestBody CreateTreasuryReconciliationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createTreasuryReconciliation(request), "Treasury reconciliation created"));
    }

    @PostMapping("/treasury/reconciliations/{reconciliationId}/complete")
    public ResponseEntity<ApiResponse<TreasuryReconciliationDto>> completeTreasuryReconciliation(@PathVariable UUID reconciliationId,
                                                                                                 @RequestBody(required = false) CompleteTreasuryReconciliationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.completeTreasuryReconciliation(reconciliationId, request), "Treasury reconciliation completed"));
    }

    @PostMapping("/entries/manual")
    public ResponseEntity<ApiResponse<JournalEntryDto>> createManualEntry(@RequestBody CreateManualJournalEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createManualJournalEntry(request), "Manual journal entry created"));
    }

    @GetMapping("/entries/{journalEntryId}/attachments")
    public ResponseEntity<ApiResponse<List<JournalEntryAttachmentDto>>> getJournalEntryAttachments(@PathVariable UUID journalEntryId) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getJournalEntryAttachments(journalEntryId)));
    }

    @PostMapping(value = "/entries/{journalEntryId}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<JournalEntryAttachmentDto>> uploadJournalEntryAttachment(
            @PathVariable UUID journalEntryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes) {
        return new ResponseEntity<>(
                ApiResponse.success(accountingService.uploadJournalEntryAttachment(journalEntryId, file, notes), "Journal entry attachment uploaded"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/entries/attachments/{attachmentId}/file")
    public ResponseEntity<InputStreamResource> getJournalEntryAttachmentFile(@PathVariable UUID attachmentId) {
        JournalEntryAttachmentDto attachment = accountingService.getJournalEntryAttachment(attachmentId);
        InputStreamResource resource = new InputStreamResource(fileStorageService.getFile(attachment.getStoragePath()));
        MediaType mediaType = (attachment.getContentType() != null && !attachment.getContentType().isBlank())
                ? MediaType.parseMediaType(attachment.getContentType())
                : MediaTypeFactory.getMediaType(attachment.getFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/entries/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteJournalEntryAttachment(@PathVariable UUID attachmentId) {
        accountingService.deleteJournalEntryAttachment(attachmentId);
        return new ResponseEntity<>(new ApiResponse<>(true, "Journal entry attachment deleted", null), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/recurring-templates")
    public ResponseEntity<ApiResponse<List<RecurringJournalTemplateDto>>> getRecurringJournalTemplates() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getRecurringJournalTemplates()));
    }

    @PostMapping("/recurring-templates")
    public ResponseEntity<ApiResponse<RecurringJournalTemplateDto>> createRecurringJournalTemplate(@RequestBody CreateRecurringJournalTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createRecurringJournalTemplate(request), "Recurring journal template created"));
    }

    @PostMapping("/recurring-templates/run-due")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> runDueRecurringJournalTemplates(@RequestParam(required = false) LocalDate runDate) {
        return ResponseEntity.ok(ApiResponse.success(
                accountingService.runDueRecurringJournalTemplates(runDate == null ? LocalDate.now() : runDate),
                "Due recurring journal templates processed"
        ));
    }

    @PostMapping("/entries/post-financial-event/{financialEventId}")
    public ResponseEntity<ApiResponse<JournalEntryDto>> postFinancialEvent(@PathVariable UUID financialEventId) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.postFinancialEvent(financialEventId), "Financial event posted to journal"));
    }

    @PostMapping("/entries/post-pending")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> postPendingFinancialEvents() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.postPendingFinancialEvents(), "Pending financial events posted"));
    }

    @GetMapping("/events/pending")
    public ResponseEntity<ApiResponse<List<FinancialEventDto>>> getPendingFinancialEvents() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getPendingFinancialEvents()));
    }

    @GetMapping("/events/{financialEventId}/preview")
    public ResponseEntity<ApiResponse<FinancialEventDto>> getFinancialEventPreview(@PathVariable UUID financialEventId) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getFinancialEventPreview(financialEventId)));
    }

    @PostMapping("/events/post-selected")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> postSelectedFinancialEvents(@RequestBody List<UUID> financialEventIds) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.postFinancialEvents(financialEventIds), "Selected financial events processed"));
    }

    @PostMapping("/entries/{journalEntryId}/reverse")
    public ResponseEntity<ApiResponse<JournalEntryDto>> reverseJournalEntry(@PathVariable UUID journalEntryId,
                                                                            @RequestBody(required = false) ReverseJournalEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                accountingService.reverseJournalEntry(journalEntryId, request == null ? null : request.getMemo()),
                "Journal entry reversed"
        ));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<?> getTrialBalance(@RequestParam(required = false) String format) {
        List<TrialBalanceRowDto> rows = accountingService.getTrialBalance();
        if ("csv".equalsIgnoreCase(format)) {
            return csv("trial-balance.csv", csvRows(
                    List.of("Code", "Account", "Type", "Debits", "Credits", "Balance"),
                    rows,
                    row -> List.of(row.getAccountCode(), row.getAccountName(), String.valueOf(row.getAccountType()), row.getTotalDebits(), row.getTotalCredits(), row.getBalance())
            ));
        }
        if ("xlsx".equalsIgnoreCase(format)) {
            return xlsx("trial-balance.xlsx", "Trial Balance",
                    List.of("Code", "Account", "Type", "Debits", "Credits", "Balance"),
                    rows,
                    row -> List.of(row.getAccountCode(), row.getAccountName(), String.valueOf(row.getAccountType()), row.getTotalDebits(), row.getTotalCredits(), row.getBalance())
            );
        }
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @GetMapping("/statements/profit-and-loss")
    public ResponseEntity<?> getProfitAndLoss(@RequestParam(required = false) String format) {
        List<FinancialStatementRowDto> rows = accountingService.getProfitAndLoss();
        if ("csv".equalsIgnoreCase(format)) {
            return csv("profit-and-loss.csv", csvRows(
                    List.of("Code", "Account", "Type", "Amount"),
                    rows,
                    row -> List.of(row.getAccountCode(), row.getAccountName(), String.valueOf(row.getAccountType()), row.getAmount())
            ));
        }
        if ("xlsx".equalsIgnoreCase(format)) {
            return xlsx("profit-and-loss.xlsx", "Profit and Loss",
                    List.of("Code", "Account", "Type", "Amount"),
                    rows,
                    row -> List.of(row.getAccountCode(), row.getAccountName(), String.valueOf(row.getAccountType()), row.getAmount())
            );
        }
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @GetMapping("/statements/balance-sheet")
    public ResponseEntity<?> getBalanceSheet(@RequestParam(required = false) String format) {
        List<FinancialStatementRowDto> rows = accountingService.getBalanceSheet();
        if ("csv".equalsIgnoreCase(format)) {
            return csv("balance-sheet.csv", csvRows(
                    List.of("Code", "Account", "Type", "Amount"),
                    rows,
                    row -> List.of(row.getAccountCode(), row.getAccountName(), String.valueOf(row.getAccountType()), row.getAmount())
            ));
        }
        if ("xlsx".equalsIgnoreCase(format)) {
            return xlsx("balance-sheet.xlsx", "Balance Sheet",
                    List.of("Code", "Account", "Type", "Amount"),
                    rows,
                    row -> List.of(row.getAccountCode(), row.getAccountName(), String.valueOf(row.getAccountType()), row.getAmount())
            );
        }
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @GetMapping("/statements/cash-flow")
    public ResponseEntity<ApiResponse<List<CashFlowRowDto>>> getCashFlow(@RequestParam(required = false) LocalDate from,
                                                                         @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getCashFlow(from, to)));
    }

    @GetMapping("/reports/vat")
    public ResponseEntity<ApiResponse<List<VatReturnRowDto>>> getVatReturn(@RequestParam(required = false) LocalDate from,
                                                                           @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getVatReturn(from, to)));
    }

    private <T> List<List<Object>> csvRows(List<String> headers, List<T> rows, Function<T, List<Object>> mapper) {
        List<List<Object>> output = new ArrayList<>();
        output.add(new ArrayList<>(headers));
        rows.stream().map(mapper).forEach(output::add);
        return output;
    }

    private ResponseEntity<ByteArrayResource> csv(String filename, List<List<Object>> rows) {
        StringBuilder builder = new StringBuilder();
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(csvCell(row.get(i)));
            }
            builder.append('\n');
        }
        byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(bytes));
    }

    private <T> ResponseEntity<ByteArrayResource> xlsx(String filename,
                                                       String sheetName,
                                                       List<String> headers,
                                                       List<T> rows,
                                                       Function<T, List<Object>> mapper) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row sheetRow = sheet.createRow(rowIndex + 1);
                List<Object> values = mapper.apply(rows.get(rowIndex));
                for (int cellIndex = 0; cellIndex < values.size(); cellIndex++) {
                    Object value = values.get(cellIndex);
                    if (value instanceof BigDecimal number) {
                        sheetRow.createCell(cellIndex).setCellValue(number.doubleValue());
                    } else {
                        sheetRow.createCell(cellIndex).setCellValue(value == null ? "" : value.toString());
                    }
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(new ByteArrayResource(outputStream.toByteArray()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not export accounting statement", e);
        }
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
