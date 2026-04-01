package com.inventory.system.service;

import com.inventory.system.payload.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface PayrollService {
    PayrollOverviewDto getOverview();
    List<PayrollUserOptionDto> getUserOptions();

    List<PayrollDepartmentDto> getDepartments();
    PayrollDepartmentDto createDepartment(SavePayrollDepartmentRequest request);
    PayrollDepartmentDto updateDepartment(UUID id, SavePayrollDepartmentRequest request);
    void deleteDepartment(UUID id);

    List<PayrollDesignationDto> getDesignations();
    PayrollDesignationDto createDesignation(SavePayrollDesignationRequest request);
    PayrollDesignationDto updateDesignation(UUID id, SavePayrollDesignationRequest request);
    void deleteDesignation(UUID id);

    List<PayrollEmployeeDto> getEmployees();
    PayrollEmployeeDto createEmployee(SavePayrollEmployeeRequest request);
    PayrollEmployeeDto updateEmployee(UUID id, SavePayrollEmployeeRequest request);
    PayrollEmployeeDto archiveEmployee(UUID id);

    List<PayrollComponentDto> getComponents();
    PayrollComponentDto createComponent(SavePayrollComponentRequest request);
    PayrollComponentDto updateComponent(UUID id, SavePayrollComponentRequest request);
    void deleteComponent(UUID id);

    List<SalaryStructureDto> getSalaryStructures();
    SalaryStructureDto createSalaryStructure(SaveSalaryStructureRequest request);
    SalaryStructureDto updateSalaryStructure(UUID id, SaveSalaryStructureRequest request);
    void deleteSalaryStructure(UUID id);

    PayrollEmployeeDto assignSalaryStructure(AssignSalaryStructureRequest request);

    List<AttendanceAdjustmentDto> getAttendanceAdjustments();
    AttendanceAdjustmentDto createAttendanceAdjustment(SaveAttendanceAdjustmentRequest request);
    AttendanceAdjustmentDto updateAttendanceAdjustment(UUID id, SaveAttendanceAdjustmentRequest request);
    void deleteAttendanceAdjustment(UUID id);
    List<AttendanceAdjustmentDto> importAttendanceAdjustments(MultipartFile file) throws IOException;

    PayrollSettingsDto getSettings();
    PayrollSettingsDto saveSettings(SavePayrollSettingsRequest request);

    List<PayrollRunDto> getPayrollRuns();
    PayrollRunDto getPayrollRun(UUID id);
    PayrollRunDto createPayrollRun(CreatePayrollRunRequest request);
    PayrollRunDto updatePayrollRunItem(UUID runId, UUID itemId, UpdatePayrollRunItemRequest request);
    PayrollRunDto approvePayrollRun(UUID id, ApprovePayrollRunRequest request);
    PayrollRunDto markPayrollRunPaid(UUID id, MarkPayrollRunPaidRequest request);

    List<PayrollPayslipDto> getPayslips();
    PayrollPayslipDto getPayslip(UUID id);
}
