package com.inventory.system.service;

import com.inventory.system.payload.AccountingJournalDto;
import com.inventory.system.payload.AccountsPayableInvoiceDto;
import com.inventory.system.payload.AccountsReceivableInvoiceDto;
import com.inventory.system.payload.AgingSummaryRowDto;
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
import com.inventory.system.payload.TreasuryAccountDto;
import com.inventory.system.payload.TreasuryReconciliationDto;
import com.inventory.system.payload.TrialBalanceRowDto;

import java.util.List;
import java.util.UUID;

public interface AccountingService {
    List<ChartOfAccountDto> getAccounts();
    ChartOfAccountDto createAccount(CreateChartOfAccountRequest request);
    List<AccountingJournalDto> getJournals();
    AccountingJournalDto createJournal(CreateAccountingJournalRequest request);
    List<JournalEntryDto> getJournalEntries();
    List<AccountsPayableInvoiceDto> getAccountsPayableInvoices();
    AccountsPayableInvoiceDto createAccountsPayableInvoice(CreateAccountsPayableInvoiceRequest request);
    AccountsPayableInvoiceDto recordAccountsPayablePayment(UUID invoiceId, RecordAccountsPayablePaymentRequest request);
    List<AgingSummaryRowDto> getAccountsPayableAging();
    List<AccountsReceivableInvoiceDto> getAccountsReceivableInvoices();
    AccountsReceivableInvoiceDto createAccountsReceivableInvoice(CreateAccountsReceivableInvoiceRequest request);
    AccountsReceivableInvoiceDto recordAccountsReceivablePayment(UUID invoiceId, RecordAccountsReceivablePaymentRequest request);
    List<AgingSummaryRowDto> getAccountsReceivableAging();
    List<TreasuryAccountDto> getTreasuryAccounts();
    TreasuryAccountDto createTreasuryAccount(CreateTreasuryAccountRequest request);
    List<TreasuryReconciliationDto> getTreasuryReconciliations();
    TreasuryReconciliationDto createTreasuryReconciliation(CreateTreasuryReconciliationRequest request);
    TreasuryReconciliationDto completeTreasuryReconciliation(UUID reconciliationId, CompleteTreasuryReconciliationRequest request);
    JournalEntryDto createManualJournalEntry(CreateManualJournalEntryRequest request);
    JournalEntryDto postFinancialEvent(UUID financialEventId);
    List<JournalEntryDto> postPendingFinancialEvents();
    JournalEntryDto reverseJournalEntry(UUID journalEntryId, String memo);
    List<TrialBalanceRowDto> getTrialBalance();
    List<FinancialStatementRowDto> getProfitAndLoss();
    List<FinancialStatementRowDto> getBalanceSheet();
}
