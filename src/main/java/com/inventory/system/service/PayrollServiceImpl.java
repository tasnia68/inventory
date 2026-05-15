package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.*;
import com.inventory.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final String PAYROLL_JOURNAL_CODE = "PAYROLL";

    private final PayrollDepartmentRepository payrollDepartmentRepository;
    private final PayrollDesignationRepository payrollDesignationRepository;
    private final EmployeePayrollProfileRepository employeePayrollProfileRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeSalaryAssignmentRepository employeeSalaryAssignmentRepository;
    private final AttendanceAdjustmentRepository attendanceAdjustmentRepository;
    private final PayrollSettingsRepository payrollSettingsRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunItemRepository payrollRunItemRepository;
    private final PayrollPayslipRepository payrollPayslipRepository;
    private final PayrollPaymentRepository payrollPaymentRepository;
    private final UserRepository userRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountingJournalRepository accountingJournalRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountingPeriodService accountingPeriodService;

    @Override
    @Transactional(readOnly = true)
    public PayrollOverviewDto getOverview() {
        ensurePresetComponents();
        PayrollOverviewDto dto = new PayrollOverviewDto();
        List<PayrollRun> runs = payrollRunRepository.findAllByOrderByCreatedAtDesc();
        dto.setEmployeeCount(employeePayrollProfileRepository.count());
        dto.setDepartmentCount(payrollDepartmentRepository.count());
        dto.setStructureCount(salaryStructureRepository.count());
        dto.setDraftRuns(runs.stream().filter(run -> run.getStatus() == PayrollRunStatus.DRAFT).count());
        dto.setApprovedRuns(runs.stream().filter(run -> run.getStatus() == PayrollRunStatus.APPROVED).count());
        dto.setPaidRuns(runs.stream().filter(run -> run.getStatus() == PayrollRunStatus.PAID).count());
        dto.setTotalApprovedNetPay(scale(runs.stream()
                .filter(run -> run.getStatus() == PayrollRunStatus.APPROVED || run.getStatus() == PayrollRunStatus.PAID)
                .flatMap(run -> run.getItems().stream())
                .map(PayrollRunItem::getNetPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        dto.setRecentRuns(runs.stream().limit(5).map(this::mapRun).toList());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollUserOptionDto> getUserOptions() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    PayrollUserOptionDto dto = new PayrollUserOptionDto();
                    dto.setId(user.getId());
                    dto.setEmail(user.getEmail());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollDepartmentDto> getDepartments() {
        return payrollDepartmentRepository.findAll().stream()
                .sorted(Comparator.comparing(PayrollDepartment::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapDepartment)
                .toList();
    }

    @Override
    @Transactional
    public PayrollDepartmentDto createDepartment(SavePayrollDepartmentRequest request) {
        PayrollDepartment department = new PayrollDepartment();
        applyDepartment(department, request);
        return mapDepartment(payrollDepartmentRepository.save(department));
    }

    @Override
    @Transactional
    public PayrollDepartmentDto updateDepartment(UUID id, SavePayrollDepartmentRequest request) {
        PayrollDepartment department = payrollDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDepartment", "id", id));
        applyDepartment(department, request);
        return mapDepartment(payrollDepartmentRepository.save(department));
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        PayrollDepartment department = payrollDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDepartment", "id", id));
        boolean inUse = employeePayrollProfileRepository.findAll().stream()
                .anyMatch(profile -> profile.getDepartment() != null && id.equals(profile.getDepartment().getId()));
        if (inUse) {
            throw new BadRequestException("Department is assigned to one or more employees");
        }
        payrollDepartmentRepository.delete(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollDesignationDto> getDesignations() {
        return payrollDesignationRepository.findAll().stream()
                .sorted(Comparator.comparing(PayrollDesignation::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapDesignation)
                .toList();
    }

    @Override
    @Transactional
    public PayrollDesignationDto createDesignation(SavePayrollDesignationRequest request) {
        PayrollDesignation designation = new PayrollDesignation();
        applyDesignation(designation, request);
        return mapDesignation(payrollDesignationRepository.save(designation));
    }

    @Override
    @Transactional
    public PayrollDesignationDto updateDesignation(UUID id, SavePayrollDesignationRequest request) {
        PayrollDesignation designation = payrollDesignationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDesignation", "id", id));
        applyDesignation(designation, request);
        return mapDesignation(payrollDesignationRepository.save(designation));
    }

    @Override
    @Transactional
    public void deleteDesignation(UUID id) {
        PayrollDesignation designation = payrollDesignationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDesignation", "id", id));
        boolean inUse = employeePayrollProfileRepository.findAll().stream()
                .anyMatch(profile -> profile.getDesignation() != null && id.equals(profile.getDesignation().getId()));
        if (inUse) {
            throw new BadRequestException("Designation is assigned to one or more employees");
        }
        payrollDesignationRepository.delete(designation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollEmployeeDto> getEmployees() {
        return employeePayrollProfileRepository.findAll().stream()
                .sorted(Comparator.comparing(EmployeePayrollProfile::getEmployeeCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapEmployee)
                .toList();
    }

    @Override
    @Transactional
    public PayrollEmployeeDto createEmployee(SavePayrollEmployeeRequest request) {
        EmployeePayrollProfile profile = new EmployeePayrollProfile();
        applyEmployee(profile, request);
        return mapEmployee(employeePayrollProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public PayrollEmployeeDto updateEmployee(UUID id, SavePayrollEmployeeRequest request) {
        EmployeePayrollProfile profile = employeePayrollProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeePayrollProfile", "id", id));
        applyEmployee(profile, request);
        return mapEmployee(employeePayrollProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public PayrollEmployeeDto archiveEmployee(UUID id) {
        EmployeePayrollProfile profile = employeePayrollProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeePayrollProfile", "id", id));
        profile.setActive(false);
        employeeSalaryAssignmentRepository.findByEmployeePayrollProfileIdOrderByEffectiveFromDesc(id)
                .forEach(assignment -> {
                    assignment.setActive(false);
                    employeeSalaryAssignmentRepository.save(assignment);
                });
        return mapEmployee(employeePayrollProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollComponentDto> getComponents() {
        ensurePresetComponents();
        return payrollComponentRepository.findAll().stream()
                .sorted(Comparator.comparing(PayrollComponent::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapComponent)
                .toList();
    }

    @Override
    @Transactional
    public PayrollComponentDto createComponent(SavePayrollComponentRequest request) {
        ensurePresetComponents();
        PayrollComponent component = new PayrollComponent();
        applyComponent(component, request);
        return mapComponent(payrollComponentRepository.save(component));
    }

    @Override
    @Transactional
    public PayrollComponentDto updateComponent(UUID id, SavePayrollComponentRequest request) {
        ensurePresetComponents();
        PayrollComponent component = payrollComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollComponent", "id", id));
        applyComponent(component, request);
        return mapComponent(payrollComponentRepository.save(component));
    }

    @Override
    @Transactional
    public void deleteComponent(UUID id) {
        PayrollComponent component = payrollComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollComponent", "id", id));
        boolean inUse = salaryStructureRepository.findAll().stream()
                .flatMap(structure -> structure.getComponents().stream())
                .anyMatch(item -> item.getPayrollComponent() != null && id.equals(item.getPayrollComponent().getId()));
        if (inUse) {
            throw new BadRequestException("Payroll component is used in a salary structure");
        }
        payrollComponentRepository.delete(component);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureDto> getSalaryStructures() {
        return salaryStructureRepository.findAll().stream()
                .sorted(Comparator.comparing(SalaryStructure::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapStructure)
                .toList();
    }

    @Override
    @Transactional
    public SalaryStructureDto createSalaryStructure(SaveSalaryStructureRequest request) {
        SalaryStructure structure = new SalaryStructure();
        applyStructure(structure, request);
        return mapStructure(salaryStructureRepository.save(structure));
    }

    @Override
    @Transactional
    public SalaryStructureDto updateSalaryStructure(UUID id, SaveSalaryStructureRequest request) {
        SalaryStructure structure = salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", id));
        structure.getComponents().clear();
        applyStructure(structure, request);
        return mapStructure(salaryStructureRepository.save(structure));
    }

    @Override
    @Transactional
    public void deleteSalaryStructure(UUID id) {
        SalaryStructure structure = salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", id));
        boolean inUse = employeeSalaryAssignmentRepository.findAll().stream()
                .anyMatch(assignment -> assignment.getSalaryStructure() != null && id.equals(assignment.getSalaryStructure().getId()));
        if (inUse) {
            throw new BadRequestException("Salary structure is assigned to one or more employees");
        }
        salaryStructureRepository.delete(structure);
    }

    @Override
    @Transactional
    public PayrollEmployeeDto assignSalaryStructure(AssignSalaryStructureRequest request) {
        EmployeePayrollProfile profile = employeePayrollProfileRepository.findById(request.getEmployeePayrollProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("EmployeePayrollProfile", "id", request.getEmployeePayrollProfileId()));
        SalaryStructure structure = salaryStructureRepository.findById(request.getSalaryStructureId())
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", request.getSalaryStructureId()));
        employeeSalaryAssignmentRepository.findByEmployeePayrollProfileIdOrderByEffectiveFromDesc(profile.getId())
                .forEach(existing -> {
                    existing.setActive(false);
                    employeeSalaryAssignmentRepository.save(existing);
                });
        EmployeeSalaryAssignment assignment = new EmployeeSalaryAssignment();
        assignment.setEmployeePayrollProfile(profile);
        assignment.setSalaryStructure(structure);
        assignment.setEffectiveFrom(request.getEffectiveFrom());
        assignment.setNotes(blankToNull(request.getNotes()));
        assignment.setActive(true);
        employeeSalaryAssignmentRepository.save(assignment);
        return mapEmployee(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceAdjustmentDto> getAttendanceAdjustments() {
        return attendanceAdjustmentRepository.findAll().stream()
                .sorted(Comparator.comparing(AttendanceAdjustment::getPeriodStart).reversed())
                .map(this::mapAttendanceAdjustment)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceAdjustmentDto createAttendanceAdjustment(SaveAttendanceAdjustmentRequest request) {
        AttendanceAdjustment adjustment = new AttendanceAdjustment();
        applyAttendanceAdjustment(adjustment, request);
        return mapAttendanceAdjustment(attendanceAdjustmentRepository.save(adjustment));
    }

    @Override
    @Transactional
    public AttendanceAdjustmentDto updateAttendanceAdjustment(UUID id, SaveAttendanceAdjustmentRequest request) {
        AttendanceAdjustment adjustment = attendanceAdjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceAdjustment", "id", id));
        applyAttendanceAdjustment(adjustment, request);
        return mapAttendanceAdjustment(attendanceAdjustmentRepository.save(adjustment));
    }

    @Override
    @Transactional
    public void deleteAttendanceAdjustment(UUID id) {
        AttendanceAdjustment adjustment = attendanceAdjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceAdjustment", "id", id));
        attendanceAdjustmentRepository.delete(adjustment);
    }

    @Override
    @Transactional
    public List<AttendanceAdjustmentDto> importAttendanceAdjustments(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required");
        }
        List<AttendanceAdjustmentDto> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return imported;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 10) {
                    continue;
                }
                User user = userRepository.findByEmail(parts[0].trim())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "email", parts[0].trim()));
                EmployeePayrollProfile profile = employeePayrollProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("EmployeePayrollProfile", "userId", user.getId()));
                SaveAttendanceAdjustmentRequest request = new SaveAttendanceAdjustmentRequest();
                request.setEmployeePayrollProfileId(profile.getId());
                request.setPeriodStart(parseDate(parts[1]));
                request.setPeriodEnd(parseDate(parts[2]));
                request.setAttendanceDate(parseDate(parts[3]));
                request.setSourceType(PayrollInputSourceType.CSV_IMPORT);
                request.setSourceReference(parts.length > 4 ? blankToNull(parts[4]) : null);
                request.setDeviceIdentifier(parts.length > 5 ? blankToNull(parts[5]) : null);
                request.setAbsentDays(parseDecimal(parts.length > 6 ? parts[6] : null));
                request.setLeaveDays(parseDecimal(parts.length > 7 ? parts[7] : null));
                request.setOvertimeHours(parseDecimal(parts.length > 8 ? parts[8] : null));
                request.setManualAllowance(parseDecimal(parts.length > 9 ? parts[9] : null));
                request.setManualDeduction(parseDecimal(parts.length > 10 ? parts[10] : null));
                request.setNotes(parts.length > 11 ? blankToNull(parts[11]) : null);
                imported.add(createAttendanceAdjustment(request));
            }
        }
        return imported;
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollSettingsDto getSettings() {
        return mapSettings(getOrCreateSettings());
    }

    @Override
    @Transactional
    public PayrollSettingsDto saveSettings(SavePayrollSettingsRequest request) {
        PayrollSettings settings = getOrCreateSettings();
        settings.setSalaryExpenseAccount(findAccount(request.getSalaryExpenseAccountId()));
        settings.setAllowanceExpenseAccount(findAccount(request.getAllowanceExpenseAccountId()));
        settings.setDeductionLiabilityAccount(findAccount(request.getDeductionLiabilityAccountId()));
        settings.setPayrollPayableAccount(findAccount(request.getPayrollPayableAccountId()));
        settings.setCashClearingAccount(findAccount(request.getCashClearingAccountId()));
        settings.setMonthlyWorkDays(request.getMonthlyWorkDays() == null || request.getMonthlyWorkDays() <= 0 ? 30 : request.getMonthlyWorkDays());
        settings.setWeeklyWorkDays(request.getWeeklyWorkDays() == null || request.getWeeklyWorkDays() <= 0 ? 7 : request.getWeeklyWorkDays());
        settings.setDefaultCurrency(defaultCurrency(request.getDefaultCurrency()));
        return mapSettings(payrollSettingsRepository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRunDto> getPayrollRuns() {
        return payrollRunRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapRun).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunDto getPayrollRun(UUID id) {
        return mapRun(payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id)));
    }

    @Override
    @Transactional
    public PayrollRunDto createPayrollRun(CreatePayrollRunRequest request) {
        if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
            throw new BadRequestException("Period start and end are required");
        }
        if (request.getPayFrequency() == null) {
            throw new BadRequestException("Pay frequency is required");
        }

        PayrollRun run = new PayrollRun();
        run.setRunNumber(generateRunNumber());
        run.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : request.getPayFrequency() + " Payroll");
        run.setPayFrequency(request.getPayFrequency());
        run.setPeriodStart(request.getPeriodStart());
        run.setPeriodEnd(request.getPeriodEnd());
        run.setCurrency(defaultCurrency(request.getCurrency() != null ? request.getCurrency() : getOrCreateSettings().getDefaultCurrency()));
        run.setNotes(blankToNull(request.getNotes()));
        run.setStatus(PayrollRunStatus.DRAFT);

        List<EmployeePayrollProfile> employees = resolveRunEmployees(request);
        for (EmployeePayrollProfile employee : employees) {
            run.getItems().add(buildRunItem(run, employee, request.getPeriodStart(), request.getPeriodEnd(), run.getCurrency()));
        }

        PayrollRun saved = payrollRunRepository.save(run);
        saved.getItems().forEach(item -> ensurePayslip(item));
        return mapRun(saved);
    }

    @Override
    @Transactional
    public PayrollRunDto updatePayrollRunItem(UUID runId, UUID itemId, UpdatePayrollRunItemRequest request) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", runId));
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BadRequestException("Only draft payroll runs can be edited");
        }

        PayrollRunItem item = payrollRunItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRunItem", "id", itemId));
        if (item.getPayrollRun() == null || !runId.equals(item.getPayrollRun().getId())) {
            throw new BadRequestException("Payroll run item does not belong to the selected run");
        }

        item.setAbsentDays(scale3(request.getAbsentDays()));
        item.setLeaveDays(scale3(request.getLeaveDays()));
        item.setOvertimeHours(scale3(request.getOvertimeHours()));
        item.setManualAllowance(scale(request.getManualAllowance()));
        item.setManualDeduction(scale(request.getManualDeduction()));
        item.setNotes(blankToNull(request.getNotes()));

        recalculateRunItem(item);
        payrollRunItemRepository.save(item);
        return mapRun(payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", runId)));
    }

    @Override
    @Transactional
    public PayrollRunDto approvePayrollRun(UUID id, ApprovePayrollRunRequest request) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id));
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BadRequestException("Only draft payroll runs can be approved");
        }
        PayrollSettings settings = getOrCreateSettings();
        if (settings.getSalaryExpenseAccount() == null
                || settings.getAllowanceExpenseAccount() == null
                || settings.getDeductionLiabilityAccount() == null
                || settings.getPayrollPayableAccount() == null) {
            throw new BadRequestException("Payroll settings account mappings must be completed before approval");
        }

        BigDecimal totalGross = run.getItems().stream().map(PayrollRunItem::getGrossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAllowances = run.getItems().stream().map(PayrollRunItem::getManualAllowance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeductions = run.getItems().stream().map(PayrollRunItem::getTotalDeductions).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = run.getItems().stream().map(PayrollRunItem::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add);

        AccountingJournal journal = ensurePayrollJournal();
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalEntryNumber());
        entry.setJournal(journal);
        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setEntryDate(LocalDateTime.now());
        entry.setSourceDocumentType("PAYROLL_RUN");
        entry.setSourceDocumentId(run.getId().toString());
        entry.setSourceDocumentNumber(run.getRunNumber());
        entry.setMemo(blankToNull(request == null ? null : request.getNotes()) == null ? "Payroll run " + run.getRunNumber() : request.getNotes().trim());
        entry.setCurrency(run.getCurrency());

        List<JournalEntryLine> lines = new ArrayList<>();
        lines.add(journalLine(entry, settings.getSalaryExpenseAccount(), 1, "Salary expense", totalGross.subtract(totalAllowances), BigDecimal.ZERO));
        if (totalAllowances.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(journalLine(entry, settings.getAllowanceExpenseAccount(), 2, "Payroll allowances", totalAllowances, BigDecimal.ZERO));
        }
        if (totalDeductions.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(journalLine(entry, settings.getDeductionLiabilityAccount(), 3, "Payroll deductions liability", BigDecimal.ZERO, totalDeductions));
        }
        lines.add(journalLine(entry, settings.getPayrollPayableAccount(), 4, "Payroll payable", BigDecimal.ZERO, totalNet));
        entry.setLines(lines);
        entry.setTotalDebits(scale(lines.stream().map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        entry.setTotalCredits(scale(lines.stream().map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        if (entry.getTotalDebits().compareTo(entry.getTotalCredits()) != 0) {
            throw new BadRequestException("Payroll journal entry is not balanced");
        }
        accountingPeriodService.assertOpen(entry.getEntryDate());
        entry.setPostedAt(LocalDateTime.now());
        journalEntryRepository.save(entry);

        run.setJournalEntry(entry);
        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedAt(LocalDateTime.now());
        return mapRun(payrollRunRepository.save(run));
    }

    @Override
    @Transactional
    public PayrollRunDto markPayrollRunPaid(UUID id, MarkPayrollRunPaidRequest request) {
        PayrollRun run = payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id));
        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new BadRequestException("Only approved payroll runs can be marked paid");
        }
        PayrollSettings settings = getOrCreateSettings();
        if (settings.getPayrollPayableAccount() == null || settings.getCashClearingAccount() == null) {
            throw new BadRequestException(
                    "Payroll settlement requires both payroll-payable and cash/clearing accounts in payroll settings");
        }

        BigDecimal totalNet = scale(run.getItems().stream().map(PayrollRunItem::getNetPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        PayrollPayment payment = new PayrollPayment();
        payment.setPayrollRun(run);
        payment.setPaymentDate(request != null && request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        payment.setReference(blankToNull(request == null ? null : request.getReference()));
        payment.setNotes(blankToNull(request == null ? null : request.getNotes()));
        payment.setAmount(totalNet);
        payrollPaymentRepository.save(payment);

        // Settlement journal entry: drains the payable that was credited at approval time.
        if (totalNet.compareTo(BigDecimal.ZERO) > 0) {
            AccountingJournal journal = ensurePayrollJournal();
            JournalEntry settlement = new JournalEntry();
            settlement.setEntryNumber(generateJournalEntryNumber());
            settlement.setJournal(journal);
            settlement.setStatus(JournalEntryStatus.POSTED);
            settlement.setEntryDate(LocalDateTime.now());
            settlement.setSourceDocumentType("PAYROLL_PAYMENT");
            settlement.setSourceDocumentId(payment.getId().toString());
            settlement.setSourceDocumentNumber(payment.getReference() != null ? payment.getReference() : run.getRunNumber());
            settlement.setMemo("Payroll settlement for " + run.getRunNumber());
            settlement.setCurrency(run.getCurrency());

            List<JournalEntryLine> lines = new ArrayList<>();
            lines.add(journalLine(settlement, settings.getPayrollPayableAccount(), 1,
                    "Settle payroll payable", totalNet, BigDecimal.ZERO));
            lines.add(journalLine(settlement, settings.getCashClearingAccount(), 2,
                    "Cash disbursed for payroll", BigDecimal.ZERO, totalNet));
            settlement.setLines(lines);
            settlement.setTotalDebits(totalNet);
            settlement.setTotalCredits(totalNet);
            accountingPeriodService.assertOpen(settlement.getEntryDate());
            settlement.setPostedAt(LocalDateTime.now());
            journalEntryRepository.save(settlement);
        }

        run.setStatus(PayrollRunStatus.PAID);
        run.setPaidAt(LocalDateTime.now());
        return mapRun(payrollRunRepository.save(run));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollPayslipDto> getPayslips() {
        return payrollPayslipRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapPayslip).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPayslipDto getPayslip(UUID id) {
        return mapPayslip(payrollPayslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollPayslip", "id", id)));
    }

    private void applyDepartment(PayrollDepartment department, SavePayrollDepartmentRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Department name is required");
        }
        department.setName(request.getName().trim());
        department.setDescription(blankToNull(request.getDescription()));
        department.setActive(request.getActive() == null || request.getActive());
    }

    private void applyDesignation(PayrollDesignation designation, SavePayrollDesignationRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Designation name is required");
        }
        designation.setName(request.getName().trim());
        designation.setDescription(blankToNull(request.getDescription()));
        designation.setActive(request.getActive() == null || request.getActive());
    }

    private void applyEmployee(EmployeePayrollProfile profile, SavePayrollEmployeeRequest request) {
        if (request.getUserId() == null) {
            throw new BadRequestException("User is required");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        profile.setUser(user);
        profile.setEmployeeCode(StringUtils.hasText(request.getEmployeeCode()) ? request.getEmployeeCode().trim() : generateEmployeeCode(user));
        profile.setDepartment(findDepartment(request.getDepartmentId()));
        profile.setDesignation(findDesignation(request.getDesignationId()));
        profile.setPayFrequency(request.getPayFrequency() == null ? PayrollPayFrequency.MONTHLY : request.getPayFrequency());
        profile.setJoinDate(request.getJoinDate());
        profile.setActive(request.getActive() == null || request.getActive());
        profile.setNotes(blankToNull(request.getNotes()));
    }

    private void applyComponent(PayrollComponent component, SavePayrollComponentRequest request) {
        if (!StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName()) || request.getComponentType() == null) {
            throw new BadRequestException("Code, name, and component type are required");
        }
        component.setCode(request.getCode().trim().toUpperCase(Locale.ROOT));
        component.setName(request.getName().trim());
        component.setComponentType(request.getComponentType());
        component.setValueType(request.getValueType() == null ? PayrollComponentValueType.FIXED : request.getValueType());
        component.setDefaultAmount(scale(request.getDefaultAmount()));
        component.setDefaultRate(scale(request.getDefaultRate()));
        component.setActive(request.getActive() == null || request.getActive());
        component.setStatutory(request.getStatutory() != null && request.getStatutory());
        component.setEditable(request.getEditable() == null || request.getEditable());
        component.setDescription(blankToNull(request.getDescription()));
    }

    private void applyStructure(SalaryStructure structure, SaveSalaryStructureRequest request) {
        if (!StringUtils.hasText(request.getName()) || request.getPayFrequency() == null) {
            throw new BadRequestException("Name and pay frequency are required");
        }
        structure.setName(request.getName().trim());
        structure.setDescription(blankToNull(request.getDescription()));
        structure.setPayFrequency(request.getPayFrequency());
        structure.setActive(request.getActive() == null || request.getActive());
        if (request.getComponents() != null) {
            for (SaveSalaryStructureRequest.ComponentInput input : request.getComponents()) {
                if (input.getPayrollComponentId() == null) {
                    continue;
                }
                PayrollComponent component = payrollComponentRepository.findById(input.getPayrollComponentId())
                        .orElseThrow(() -> new ResourceNotFoundException("PayrollComponent", "id", input.getPayrollComponentId()));
                SalaryStructureComponent item = new SalaryStructureComponent();
                item.setSalaryStructure(structure);
                item.setPayrollComponent(component);
                item.setAmount(scale(input.getAmount()));
                item.setRate(scale(input.getRate()));
                item.setSortOrder(input.getSortOrder() == null ? 0 : input.getSortOrder());
                structure.getComponents().add(item);
            }
        }
    }

    private void applyAttendanceAdjustment(AttendanceAdjustment adjustment, SaveAttendanceAdjustmentRequest request) {
        if (request.getEmployeePayrollProfileId() == null || request.getPeriodStart() == null || request.getPeriodEnd() == null) {
            throw new BadRequestException("Employee and period are required");
        }
        adjustment.setEmployeePayrollProfile(employeePayrollProfileRepository.findById(request.getEmployeePayrollProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("EmployeePayrollProfile", "id", request.getEmployeePayrollProfileId())));
        adjustment.setPeriodStart(request.getPeriodStart());
        adjustment.setPeriodEnd(request.getPeriodEnd());
        adjustment.setAttendanceDate(request.getAttendanceDate());
        adjustment.setSourceType(request.getSourceType() == null ? PayrollInputSourceType.MANUAL : request.getSourceType());
        adjustment.setSourceReference(blankToNull(request.getSourceReference()));
        adjustment.setDeviceIdentifier(blankToNull(request.getDeviceIdentifier()));
        adjustment.setLateMinutes(request.getLateMinutes() == null ? 0 : request.getLateMinutes());
        adjustment.setAbsentDays(scale3(request.getAbsentDays()));
        adjustment.setLeaveDays(scale3(request.getLeaveDays()));
        adjustment.setOvertimeHours(scale3(request.getOvertimeHours()));
        adjustment.setManualAllowance(scale(request.getManualAllowance()));
        adjustment.setManualDeduction(scale(request.getManualDeduction()));
        adjustment.setNotes(blankToNull(request.getNotes()));
    }

    private List<EmployeePayrollProfile> resolveRunEmployees(CreatePayrollRunRequest request) {
        if (request.getEmployeePayrollProfileIds() != null && !request.getEmployeePayrollProfileIds().isEmpty()) {
            return request.getEmployeePayrollProfileIds().stream()
                    .map(id -> employeePayrollProfileRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("EmployeePayrollProfile", "id", id)))
                    .toList();
        }
        return employeePayrollProfileRepository.findAllByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(profile -> profile.getPayFrequency() == request.getPayFrequency())
                .toList();
    }

    private PayrollRunItem buildRunItem(PayrollRun run, EmployeePayrollProfile employee, LocalDate periodStart, LocalDate periodEnd, String currency) {
        EmployeeSalaryAssignment assignment = employeeSalaryAssignmentRepository
                .findFirstByEmployeePayrollProfileIdAndActiveTrueOrderByEffectiveFromDesc(employee.getId())
                .orElse(null);
        if (assignment == null) {
            throw new BadRequestException("Employee " + employee.getEmployeeCode() + " has no active salary structure assignment");
        }

        PayrollSettings settings = getOrCreateSettings();
        BigDecimal prorationFactor = calculateProrationFactor(employee.getPayFrequency(), periodStart, periodEnd, settings);

        BigDecimal recurringEarnings = BigDecimal.ZERO;
        BigDecimal recurringDeductions = BigDecimal.ZERO;
        BigDecimal recurringStatutory = BigDecimal.ZERO;

        List<SalaryStructureComponent> components = assignment.getSalaryStructure().getComponents().stream()
                .sorted(Comparator.comparing(SalaryStructureComponent::getSortOrder))
                .toList();

        BigDecimal earningBase = BigDecimal.ZERO;
        for (SalaryStructureComponent component : components) {
            BigDecimal amount = resolveComponentAmount(component, earningBase).multiply(prorationFactor);
            amount = scale(amount);
            if (component.getPayrollComponent().getComponentType() == PayrollComponentType.EARNING) {
                recurringEarnings = recurringEarnings.add(amount);
                earningBase = recurringEarnings;
            } else if (component.getPayrollComponent().getComponentType() == PayrollComponentType.STATUTORY) {
                recurringStatutory = recurringStatutory.add(amount);
            } else {
                recurringDeductions = recurringDeductions.add(amount);
            }
        }

        List<AttendanceAdjustment> adjustments = attendanceAdjustmentRepository
                .findByEmployeePayrollProfileIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(employee.getId(), periodStart, periodEnd);
        BigDecimal absentDays = adjustments.stream().map(AttendanceAdjustment::getAbsentDays).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal leaveDays = adjustments.stream().map(AttendanceAdjustment::getLeaveDays).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overtimeHours = adjustments.stream().map(AttendanceAdjustment::getOvertimeHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal manualAllowance = adjustments.stream().map(AttendanceAdjustment::getManualAllowance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal manualDeduction = adjustments.stream().map(AttendanceAdjustment::getManualDeduction).reduce(BigDecimal.ZERO, BigDecimal::add);

        PayrollRunItem item = new PayrollRunItem();
        item.setPayrollRun(run);
        item.setEmployeePayrollProfile(employee);
        item.setEmployeeSalaryAssignment(assignment);
        item.setCurrency(currency);
        item.setAbsentDays(scale3(absentDays));
        item.setLeaveDays(scale3(leaveDays));
        item.setOvertimeHours(scale3(overtimeHours));
        item.setManualAllowance(scale(manualAllowance));
        item.setManualDeduction(scale(manualDeduction));
        item.setNotes("Calculated from salary structure, attendance, and manual adjustments");
        recalculateRunItem(item);
        return item;
    }

    private void recalculateRunItem(PayrollRunItem item) {
        PayrollRun run = item.getPayrollRun();
        EmployeePayrollProfile employee = item.getEmployeePayrollProfile();
        EmployeeSalaryAssignment assignment = item.getEmployeeSalaryAssignment();
        if (assignment == null) {
            assignment = employeeSalaryAssignmentRepository
                    .findFirstByEmployeePayrollProfileIdAndActiveTrueOrderByEffectiveFromDesc(employee.getId())
                    .orElseThrow(() -> new BadRequestException("Employee " + employee.getEmployeeCode() + " has no active salary structure assignment"));
            item.setEmployeeSalaryAssignment(assignment);
        }

        PayrollSettings settings = getOrCreateSettings();
        BigDecimal workDaysBasis = BigDecimal.valueOf(employee.getPayFrequency() == PayrollPayFrequency.WEEKLY
                ? settings.getWeeklyWorkDays()
                : settings.getMonthlyWorkDays());
        BigDecimal prorationFactor = calculateProrationFactor(employee.getPayFrequency(), run.getPeriodStart(), run.getPeriodEnd(), settings);

        BigDecimal recurringEarnings = BigDecimal.ZERO;
        BigDecimal recurringDeductions = BigDecimal.ZERO;
        BigDecimal recurringStatutory = BigDecimal.ZERO;

        List<SalaryStructureComponent> components = assignment.getSalaryStructure().getComponents().stream()
                .sorted(Comparator.comparing(SalaryStructureComponent::getSortOrder))
                .toList();

        BigDecimal earningBase = BigDecimal.ZERO;
        for (SalaryStructureComponent component : components) {
            BigDecimal amount = resolveComponentAmount(component, earningBase).multiply(prorationFactor);
            amount = scale(amount);
            if (component.getPayrollComponent().getComponentType() == PayrollComponentType.EARNING) {
                recurringEarnings = recurringEarnings.add(amount);
                earningBase = recurringEarnings;
            } else if (component.getPayrollComponent().getComponentType() == PayrollComponentType.STATUTORY) {
                recurringStatutory = recurringStatutory.add(amount);
            } else {
                recurringDeductions = recurringDeductions.add(amount);
            }
        }

        BigDecimal absentDays = scale3(item.getAbsentDays());
        BigDecimal leaveDays = scale3(item.getLeaveDays());
        BigDecimal overtimeHours = scale3(item.getOvertimeHours());
        BigDecimal manualAllowance = scale(item.getManualAllowance());
        BigDecimal manualDeduction = scale(item.getManualDeduction());

        BigDecimal dailyRate = workDaysBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : recurringEarnings.divide(workDaysBasis, 6, RoundingMode.HALF_UP);
        BigDecimal hourlyRate = workDaysBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : recurringEarnings.divide(workDaysBasis.multiply(BigDecimal.valueOf(8)), 6, RoundingMode.HALF_UP);
        BigDecimal absenceDeduction = scale(dailyRate.multiply(absentDays.add(leaveDays)));
        BigDecimal overtimeEarnings = scale(hourlyRate.multiply(overtimeHours));

        BigDecimal totalEarnings = scale(recurringEarnings.add(overtimeEarnings).add(manualAllowance));
        BigDecimal totalDeductions = scale(recurringDeductions.add(recurringStatutory).add(absenceDeduction).add(manualDeduction));

        item.setAbsentDays(absentDays);
        item.setLeaveDays(leaveDays);
        item.setOvertimeHours(overtimeHours);
        item.setManualAllowance(manualAllowance);
        item.setManualDeduction(manualDeduction);
        item.setWorkingDays(scale3(workDaysBasis.subtract(absentDays).subtract(leaveDays).max(BigDecimal.ZERO)));
        item.setGrossPay(totalEarnings);
        item.setTotalEarnings(totalEarnings);
        item.setStatutoryDeductions(scale(recurringStatutory));
        item.setTotalDeductions(totalDeductions);
        item.setNetPay(scale(totalEarnings.subtract(totalDeductions).max(BigDecimal.ZERO)));
    }

    private BigDecimal calculateProrationFactor(PayrollPayFrequency frequency, LocalDate periodStart, LocalDate periodEnd, PayrollSettings settings) {
        long daysInclusive = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        if (daysInclusive <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal divisor = BigDecimal.valueOf(frequency == PayrollPayFrequency.WEEKLY ? settings.getWeeklyWorkDays() : settings.getMonthlyWorkDays());
        return BigDecimal.valueOf(daysInclusive).divide(divisor, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveComponentAmount(SalaryStructureComponent component, BigDecimal earningBase) {
        if (component.getPayrollComponent().getValueType() == PayrollComponentValueType.PERCENTAGE) {
            BigDecimal rate = component.getRate() == null || component.getRate().compareTo(BigDecimal.ZERO) == 0
                    ? component.getPayrollComponent().getDefaultRate()
                    : component.getRate();
            return earningBase.multiply(rate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        if (component.getAmount() != null && component.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            return component.getAmount();
        }
        return component.getPayrollComponent().getDefaultAmount();
    }

    private void ensurePresetComponents() {
        if (payrollComponentRepository.count() > 0) {
            return;
        }
        createPresetComponent("BASIC_PAY", "Basic Pay", PayrollComponentType.EARNING, BigDecimal.valueOf(25000), BigDecimal.ZERO, false);
        createPresetComponent("HOUSE_RENT", "House Rent", PayrollComponentType.EARNING, BigDecimal.valueOf(12500), BigDecimal.ZERO, false);
        createPresetComponent("MEDICAL", "Medical Allowance", PayrollComponentType.EARNING, BigDecimal.valueOf(3000), BigDecimal.ZERO, false);
        createPresetComponent("TRANSPORT", "Transport Allowance", PayrollComponentType.EARNING, BigDecimal.valueOf(2000), BigDecimal.ZERO, false);
        createPresetComponent("TAX", "Income Tax", PayrollComponentType.STATUTORY, BigDecimal.ZERO, BigDecimal.valueOf(5), true);
        createPresetComponent("PF", "Provident Fund", PayrollComponentType.STATUTORY, BigDecimal.ZERO, BigDecimal.valueOf(10), true);
        createPresetComponent("LATE_FINE", "Late / Manual Deduction", PayrollComponentType.DEDUCTION, BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    private void createPresetComponent(String code, String name, PayrollComponentType type, BigDecimal amount, BigDecimal rate, boolean statutory) {
        PayrollComponent component = new PayrollComponent();
        component.setCode(code);
        component.setName(name);
        component.setComponentType(type);
        component.setValueType(rate.compareTo(BigDecimal.ZERO) > 0 ? PayrollComponentValueType.PERCENTAGE : PayrollComponentValueType.FIXED);
        component.setDefaultAmount(scale(amount));
        component.setDefaultRate(scale(rate));
        component.setActive(true);
        component.setStatutory(statutory);
        component.setEditable(true);
        payrollComponentRepository.save(component);
    }

    private PayrollSettings getOrCreateSettings() {
        return payrollSettingsRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> payrollSettingsRepository.save(new PayrollSettings()));
    }

    private PayrollDepartment findDepartment(UUID id) {
        return id == null ? null : payrollDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDepartment", "id", id));
    }

    private PayrollDesignation findDesignation(UUID id) {
        return id == null ? null : payrollDesignationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollDesignation", "id", id));
    }

    private ChartOfAccount findAccount(UUID id) {
        return id == null ? null : chartOfAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", "id", id));
    }

    private AccountingJournal ensurePayrollJournal() {
        return accountingJournalRepository.findByJournalCode(PAYROLL_JOURNAL_CODE)
                .orElseGet(() -> {
                    AccountingJournal journal = new AccountingJournal();
                    journal.setJournalCode(PAYROLL_JOURNAL_CODE);
                    journal.setJournalName("Payroll Journal");
                    journal.setDescription("System payroll posting journal");
                    journal.setSystemJournal(true);
                    journal.setActive(true);
                    return accountingJournalRepository.save(journal);
                });
    }

    private JournalEntryLine journalLine(JournalEntry entry, ChartOfAccount account, int lineNumber, String description, BigDecimal debit, BigDecimal credit) {
        JournalEntryLine line = new JournalEntryLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setLineNumber(lineNumber);
        line.setDescription(description);
        line.setDebitAmount(scale(debit));
        line.setCreditAmount(scale(credit));
        return line;
    }

    private PayrollDepartmentDto mapDepartment(PayrollDepartment department) {
        PayrollDepartmentDto dto = new PayrollDepartmentDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setActive(department.isActive());
        return dto;
    }

    private PayrollDesignationDto mapDesignation(PayrollDesignation designation) {
        PayrollDesignationDto dto = new PayrollDesignationDto();
        dto.setId(designation.getId());
        dto.setName(designation.getName());
        dto.setDescription(designation.getDescription());
        dto.setActive(designation.isActive());
        return dto;
    }

    private PayrollEmployeeDto mapEmployee(EmployeePayrollProfile profile) {
        PayrollEmployeeDto dto = new PayrollEmployeeDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser().getId());
        dto.setUserEmail(profile.getUser().getEmail());
        dto.setUserFirstName(profile.getUser().getFirstName());
        dto.setUserLastName(profile.getUser().getLastName());
        dto.setEmployeeCode(profile.getEmployeeCode());
        dto.setPayFrequency(profile.getPayFrequency());
        dto.setJoinDate(profile.getJoinDate());
        dto.setActive(profile.isActive());
        dto.setNotes(profile.getNotes());
        if (profile.getDepartment() != null) {
            dto.setDepartmentId(profile.getDepartment().getId());
            dto.setDepartmentName(profile.getDepartment().getName());
        }
        if (profile.getDesignation() != null) {
            dto.setDesignationId(profile.getDesignation().getId());
            dto.setDesignationName(profile.getDesignation().getName());
        }
        employeeSalaryAssignmentRepository.findFirstByEmployeePayrollProfileIdAndActiveTrueOrderByEffectiveFromDesc(profile.getId())
                .ifPresent(assignment -> {
                    dto.setActiveSalaryStructureId(assignment.getSalaryStructure().getId());
                    dto.setActiveSalaryStructureName(assignment.getSalaryStructure().getName());
                });
        return dto;
    }

    private PayrollComponentDto mapComponent(PayrollComponent component) {
        PayrollComponentDto dto = new PayrollComponentDto();
        dto.setId(component.getId());
        dto.setCode(component.getCode());
        dto.setName(component.getName());
        dto.setComponentType(component.getComponentType());
        dto.setValueType(component.getValueType());
        dto.setDefaultAmount(scale(component.getDefaultAmount()));
        dto.setDefaultRate(scale(component.getDefaultRate()));
        dto.setActive(component.isActive());
        dto.setStatutory(component.isStatutory());
        dto.setEditable(component.isEditable());
        dto.setDescription(component.getDescription());
        return dto;
    }

    private SalaryStructureDto mapStructure(SalaryStructure structure) {
        SalaryStructureDto dto = new SalaryStructureDto();
        dto.setId(structure.getId());
        dto.setName(structure.getName());
        dto.setDescription(structure.getDescription());
        dto.setPayFrequency(structure.getPayFrequency());
        dto.setActive(structure.isActive());
        dto.setComponents(structure.getComponents().stream()
                .sorted(Comparator.comparing(SalaryStructureComponent::getSortOrder))
                .map(component -> {
                    SalaryStructureComponentDto item = new SalaryStructureComponentDto();
                    item.setId(component.getId());
                    item.setPayrollComponentId(component.getPayrollComponent().getId());
                    item.setPayrollComponentCode(component.getPayrollComponent().getCode());
                    item.setPayrollComponentName(component.getPayrollComponent().getName());
                    item.setComponentType(component.getPayrollComponent().getComponentType());
                    item.setValueType(component.getPayrollComponent().getValueType());
                    item.setAmount(scale(component.getAmount()));
                    item.setRate(scale(component.getRate()));
                    item.setSortOrder(component.getSortOrder());
                    return item;
                })
                .toList());
        return dto;
    }

    private AttendanceAdjustmentDto mapAttendanceAdjustment(AttendanceAdjustment adjustment) {
        AttendanceAdjustmentDto dto = new AttendanceAdjustmentDto();
        dto.setId(adjustment.getId());
        dto.setEmployeePayrollProfileId(adjustment.getEmployeePayrollProfile().getId());
        dto.setEmployeeCode(adjustment.getEmployeePayrollProfile().getEmployeeCode());
        dto.setEmployeeName(fullName(adjustment.getEmployeePayrollProfile().getUser()));
        dto.setPeriodStart(adjustment.getPeriodStart());
        dto.setPeriodEnd(adjustment.getPeriodEnd());
        dto.setAttendanceDate(adjustment.getAttendanceDate());
        dto.setSourceType(adjustment.getSourceType());
        dto.setSourceReference(adjustment.getSourceReference());
        dto.setDeviceIdentifier(adjustment.getDeviceIdentifier());
        dto.setLateMinutes(adjustment.getLateMinutes());
        dto.setAbsentDays(scale3(adjustment.getAbsentDays()));
        dto.setLeaveDays(scale3(adjustment.getLeaveDays()));
        dto.setOvertimeHours(scale3(adjustment.getOvertimeHours()));
        dto.setManualAllowance(scale(adjustment.getManualAllowance()));
        dto.setManualDeduction(scale(adjustment.getManualDeduction()));
        dto.setNotes(adjustment.getNotes());
        return dto;
    }

    private PayrollSettingsDto mapSettings(PayrollSettings settings) {
        PayrollSettingsDto dto = new PayrollSettingsDto();
        dto.setId(settings.getId());
        dto.setMonthlyWorkDays(settings.getMonthlyWorkDays());
        dto.setWeeklyWorkDays(settings.getWeeklyWorkDays());
        dto.setDefaultCurrency(settings.getDefaultCurrency());
        if (settings.getSalaryExpenseAccount() != null) {
            dto.setSalaryExpenseAccountId(settings.getSalaryExpenseAccount().getId());
            dto.setSalaryExpenseAccountName(settings.getSalaryExpenseAccount().getAccountCode() + " · " + settings.getSalaryExpenseAccount().getAccountName());
        }
        if (settings.getAllowanceExpenseAccount() != null) {
            dto.setAllowanceExpenseAccountId(settings.getAllowanceExpenseAccount().getId());
            dto.setAllowanceExpenseAccountName(settings.getAllowanceExpenseAccount().getAccountCode() + " · " + settings.getAllowanceExpenseAccount().getAccountName());
        }
        if (settings.getDeductionLiabilityAccount() != null) {
            dto.setDeductionLiabilityAccountId(settings.getDeductionLiabilityAccount().getId());
            dto.setDeductionLiabilityAccountName(settings.getDeductionLiabilityAccount().getAccountCode() + " · " + settings.getDeductionLiabilityAccount().getAccountName());
        }
        if (settings.getPayrollPayableAccount() != null) {
            dto.setPayrollPayableAccountId(settings.getPayrollPayableAccount().getId());
            dto.setPayrollPayableAccountName(settings.getPayrollPayableAccount().getAccountCode() + " · " + settings.getPayrollPayableAccount().getAccountName());
        }
        if (settings.getCashClearingAccount() != null) {
            dto.setCashClearingAccountId(settings.getCashClearingAccount().getId());
            dto.setCashClearingAccountName(settings.getCashClearingAccount().getAccountCode() + " · " + settings.getCashClearingAccount().getAccountName());
        }
        return dto;
    }

    private PayrollRunDto mapRun(PayrollRun run) {
        PayrollRunDto dto = new PayrollRunDto();
        dto.setId(run.getId());
        dto.setRunNumber(run.getRunNumber());
        dto.setTitle(run.getTitle());
        dto.setPayFrequency(run.getPayFrequency());
        dto.setPeriodStart(run.getPeriodStart());
        dto.setPeriodEnd(run.getPeriodEnd());
        dto.setStatus(run.getStatus());
        dto.setCurrency(run.getCurrency());
        dto.setNotes(run.getNotes());
        dto.setApprovedAt(run.getApprovedAt());
        dto.setPaidAt(run.getPaidAt());
        dto.setJournalEntryId(run.getJournalEntry() == null ? null : run.getJournalEntry().getId());
        dto.setItems(run.getItems().stream().map(this::mapRunItem).toList());
        return dto;
    }

    private PayrollRunItemDto mapRunItem(PayrollRunItem item) {
        PayrollRunItemDto dto = new PayrollRunItemDto();
        dto.setId(item.getId());
        dto.setEmployeePayrollProfileId(item.getEmployeePayrollProfile().getId());
        dto.setEmployeeCode(item.getEmployeePayrollProfile().getEmployeeCode());
        dto.setEmployeeName(fullName(item.getEmployeePayrollProfile().getUser()));
        dto.setEmployeeSalaryAssignmentId(item.getEmployeeSalaryAssignment() == null ? null : item.getEmployeeSalaryAssignment().getId());
        dto.setGrossPay(scale(item.getGrossPay()));
        dto.setTotalEarnings(scale(item.getTotalEarnings()));
        dto.setTotalDeductions(scale(item.getTotalDeductions()));
        dto.setStatutoryDeductions(scale(item.getStatutoryDeductions()));
        dto.setNetPay(scale(item.getNetPay()));
        dto.setWorkingDays(scale3(item.getWorkingDays()));
        dto.setAbsentDays(scale3(item.getAbsentDays()));
        dto.setLeaveDays(scale3(item.getLeaveDays()));
        dto.setOvertimeHours(scale3(item.getOvertimeHours()));
        dto.setManualAllowance(scale(item.getManualAllowance()));
        dto.setManualDeduction(scale(item.getManualDeduction()));
        dto.setCurrency(item.getCurrency());
        dto.setNotes(item.getNotes());
        return dto;
    }

    private PayrollPayslipDto mapPayslip(PayrollPayslip payslip) {
        PayrollRunItem item = payslip.getPayrollRunItem();
        PayrollRun run = item.getPayrollRun();
        PayrollPayslipDto dto = new PayrollPayslipDto();
        dto.setId(payslip.getId());
        dto.setPayslipNumber(payslip.getPayslipNumber());
        dto.setPayrollRunId(run.getId());
        dto.setPayrollRunNumber(run.getRunNumber());
        dto.setPeriodStart(run.getPeriodStart());
        dto.setPeriodEnd(run.getPeriodEnd());
        dto.setEmployeePayrollProfileId(item.getEmployeePayrollProfile().getId());
        dto.setEmployeeCode(item.getEmployeePayrollProfile().getEmployeeCode());
        dto.setEmployeeName(fullName(item.getEmployeePayrollProfile().getUser()));
        dto.setGrossPay(scale(item.getGrossPay()));
        dto.setTotalEarnings(scale(item.getTotalEarnings()));
        dto.setTotalDeductions(scale(item.getTotalDeductions()));
        dto.setStatutoryDeductions(scale(item.getStatutoryDeductions()));
        dto.setNetPay(scale(item.getNetPay()));
        dto.setCurrency(item.getCurrency());
        dto.setGeneratedAt(payslip.getGeneratedAt());
        dto.setPublished(payslip.isPublished());
        return dto;
    }

    private void ensurePayslip(PayrollRunItem item) {
        payrollPayslipRepository.findByPayrollRunItemId(item.getId()).orElseGet(() -> {
            PayrollPayslip payslip = new PayrollPayslip();
            payslip.setPayrollRunItem(item);
            payslip.setPayslipNumber(generatePayslipNumber());
            payslip.setGeneratedAt(LocalDateTime.now());
            payslip.setPublished(true);
            return payrollPayslipRepository.save(payslip);
        });
    }

    private String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String combined = (first + " " + last).trim();
        return StringUtils.hasText(combined) ? combined : user.getEmail();
    }

    private String generateEmployeeCode(User user) {
        return "EMP-" + Math.abs(Objects.hash(user.getEmail(), System.nanoTime()));
    }

    private String generateRunNumber() {
        return "PR-" + System.currentTimeMillis();
    }

    private String generatePayslipNumber() {
        return "PS-" + System.currentTimeMillis() + "-" + (payrollPayslipRepository.count() + 1);
    }

    private String generateJournalEntryNumber() {
        return "JE-" + System.currentTimeMillis();
    }

    private String defaultCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return "BDT";
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal scale3(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(3, RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String value) {
        return StringUtils.hasText(value) ? LocalDate.parse(value.trim()) : null;
    }

    private BigDecimal parseDecimal(String value) {
        return StringUtils.hasText(value) ? new BigDecimal(value.trim()) : BigDecimal.ZERO;
    }
}
