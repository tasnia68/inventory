package com.inventory.system.controller;

import com.inventory.system.payload.AccountingJournalDto;
import com.inventory.system.payload.AccountsPayableInvoiceDto;
import com.inventory.system.payload.AccountsReceivableInvoiceDto;
import com.inventory.system.payload.AgingSummaryRowDto;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ChartOfAccountDto;
import com.inventory.system.payload.CompleteTreasuryReconciliationRequest;
import com.inventory.system.payload.CreateAccountsPayableInvoiceRequest;
import com.inventory.system.payload.CreateAccountsReceivableInvoiceRequest;
import com.inventory.system.payload.CreateAccountingJournalRequest;
import com.inventory.system.payload.CreateChartOfAccountRequest;
import com.inventory.system.payload.CreateManualJournalEntryRequest;
import com.inventory.system.payload.CreateTreasuryAccountRequest;
import com.inventory.system.payload.CreateTreasuryReconciliationRequest;
import com.inventory.system.payload.FinancialStatementRowDto;
import com.inventory.system.payload.JournalEntryDto;
import com.inventory.system.payload.RecordAccountsPayablePaymentRequest;
import com.inventory.system.payload.RecordAccountsReceivablePaymentRequest;
import com.inventory.system.payload.ReverseJournalEntryRequest;
import com.inventory.system.payload.TreasuryAccountDto;
import com.inventory.system.payload.TreasuryReconciliationDto;
import com.inventory.system.payload.TrialBalanceRowDto;
import com.inventory.system.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<ChartOfAccountDto>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getAccounts()));
    }

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<ChartOfAccountDto>> createAccount(@RequestBody CreateChartOfAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.createAccount(request), "Account created successfully"));
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

    @PostMapping("/entries/post-financial-event/{financialEventId}")
    public ResponseEntity<ApiResponse<JournalEntryDto>> postFinancialEvent(@PathVariable UUID financialEventId) {
        return ResponseEntity.ok(ApiResponse.success(accountingService.postFinancialEvent(financialEventId), "Financial event posted to journal"));
    }

    @PostMapping("/entries/post-pending")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> postPendingFinancialEvents() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.postPendingFinancialEvents(), "Pending financial events posted"));
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
    public ResponseEntity<ApiResponse<List<TrialBalanceRowDto>>> getTrialBalance() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getTrialBalance()));
    }

    @GetMapping("/statements/profit-and-loss")
    public ResponseEntity<ApiResponse<List<FinancialStatementRowDto>>> getProfitAndLoss() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getProfitAndLoss()));
    }

    @GetMapping("/statements/balance-sheet")
    public ResponseEntity<ApiResponse<List<FinancialStatementRowDto>>> getBalanceSheet() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getBalanceSheet()));
    }
}
