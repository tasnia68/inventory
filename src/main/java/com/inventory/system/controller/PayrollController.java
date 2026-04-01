package com.inventory.system.controller;

import com.inventory.system.payload.*;
import com.inventory.system.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PayrollOverviewDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getOverview()));
    }

    @GetMapping("/users/options")
    public ResponseEntity<ApiResponse<List<PayrollUserOptionDto>>> getUserOptions() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getUserOptions()));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<PayrollDepartmentDto>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getDepartments()));
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<PayrollDepartmentDto>> createDepartment(@RequestBody SavePayrollDepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createDepartment(request), "Payroll department created"));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<PayrollDepartmentDto>> updateDepartment(@PathVariable UUID id, @RequestBody SavePayrollDepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateDepartment(id, request), "Payroll department updated"));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable UUID id) {
        payrollService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Payroll department deleted"));
    }

    @GetMapping("/designations")
    public ResponseEntity<ApiResponse<List<PayrollDesignationDto>>> getDesignations() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getDesignations()));
    }

    @PostMapping("/designations")
    public ResponseEntity<ApiResponse<PayrollDesignationDto>> createDesignation(@RequestBody SavePayrollDesignationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createDesignation(request), "Payroll designation created"));
    }

    @PutMapping("/designations/{id}")
    public ResponseEntity<ApiResponse<PayrollDesignationDto>> updateDesignation(@PathVariable UUID id, @RequestBody SavePayrollDesignationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateDesignation(id, request), "Payroll designation updated"));
    }

    @DeleteMapping("/designations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDesignation(@PathVariable UUID id) {
        payrollService.deleteDesignation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Payroll designation deleted"));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<PayrollEmployeeDto>>> getEmployees() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getEmployees()));
    }

    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<PayrollEmployeeDto>> createEmployee(@RequestBody SavePayrollEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createEmployee(request), "Payroll employee created"));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<PayrollEmployeeDto>> updateEmployee(@PathVariable UUID id, @RequestBody SavePayrollEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateEmployee(id, request), "Payroll employee updated"));
    }

    @PostMapping("/employees/{id}/archive")
    public ResponseEntity<ApiResponse<PayrollEmployeeDto>> archiveEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.archiveEmployee(id), "Payroll employee archived"));
    }

    @PostMapping("/employees/assignments")
    public ResponseEntity<ApiResponse<PayrollEmployeeDto>> assignSalaryStructure(@RequestBody AssignSalaryStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.assignSalaryStructure(request), "Salary structure assigned"));
    }

    @GetMapping("/components")
    public ResponseEntity<ApiResponse<List<PayrollComponentDto>>> getComponents() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getComponents()));
    }

    @PostMapping("/components")
    public ResponseEntity<ApiResponse<PayrollComponentDto>> createComponent(@RequestBody SavePayrollComponentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createComponent(request), "Payroll component created"));
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<ApiResponse<PayrollComponentDto>> updateComponent(@PathVariable UUID id, @RequestBody SavePayrollComponentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateComponent(id, request), "Payroll component updated"));
    }

    @DeleteMapping("/components/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComponent(@PathVariable UUID id) {
        payrollService.deleteComponent(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Payroll component deleted"));
    }

    @GetMapping("/salary-structures")
    public ResponseEntity<ApiResponse<List<SalaryStructureDto>>> getSalaryStructures() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getSalaryStructures()));
    }

    @PostMapping("/salary-structures")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> createSalaryStructure(@RequestBody SaveSalaryStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createSalaryStructure(request), "Salary structure created"));
    }

    @PutMapping("/salary-structures/{id}")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> updateSalaryStructure(@PathVariable UUID id, @RequestBody SaveSalaryStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateSalaryStructure(id, request), "Salary structure updated"));
    }

    @DeleteMapping("/salary-structures/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSalaryStructure(@PathVariable UUID id) {
        payrollService.deleteSalaryStructure(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Salary structure deleted"));
    }

    @GetMapping("/attendance-adjustments")
    public ResponseEntity<ApiResponse<List<AttendanceAdjustmentDto>>> getAttendanceAdjustments() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getAttendanceAdjustments()));
    }

    @PostMapping("/attendance-adjustments")
    public ResponseEntity<ApiResponse<AttendanceAdjustmentDto>> createAttendanceAdjustment(@RequestBody SaveAttendanceAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createAttendanceAdjustment(request), "Attendance adjustment created"));
    }

    @PutMapping("/attendance-adjustments/{id}")
    public ResponseEntity<ApiResponse<AttendanceAdjustmentDto>> updateAttendanceAdjustment(@PathVariable UUID id, @RequestBody SaveAttendanceAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateAttendanceAdjustment(id, request), "Attendance adjustment updated"));
    }

    @DeleteMapping("/attendance-adjustments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAttendanceAdjustment(@PathVariable UUID id) {
        payrollService.deleteAttendanceAdjustment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Attendance adjustment deleted"));
    }

    @PostMapping(value = "/attendance-adjustments/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<AttendanceAdjustmentDto>>> importAttendanceAdjustments(@RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(payrollService.importAttendanceAdjustments(file), "Attendance adjustments imported"));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<PayrollSettingsDto>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getSettings()));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<PayrollSettingsDto>> saveSettings(@RequestBody SavePayrollSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.saveSettings(request), "Payroll settings updated"));
    }

    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<List<PayrollRunDto>>> getRuns() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollRuns()));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<ApiResponse<PayrollRunDto>> getRun(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayrollRun(id)));
    }

    @PostMapping("/runs")
    public ResponseEntity<ApiResponse<PayrollRunDto>> createRun(@RequestBody CreatePayrollRunRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.createPayrollRun(request), "Payroll run created"));
    }

    @PutMapping("/runs/{runId}/items/{itemId}")
    public ResponseEntity<ApiResponse<PayrollRunDto>> updateRunItem(
            @PathVariable UUID runId,
            @PathVariable UUID itemId,
            @RequestBody UpdatePayrollRunItemRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updatePayrollRunItem(runId, itemId, request), "Payroll run item updated"));
    }

    @PostMapping("/runs/{id}/approve")
    public ResponseEntity<ApiResponse<PayrollRunDto>> approveRun(@PathVariable UUID id, @RequestBody(required = false) ApprovePayrollRunRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.approvePayrollRun(id, request), "Payroll run approved"));
    }

    @PostMapping("/runs/{id}/pay")
    public ResponseEntity<ApiResponse<PayrollRunDto>> markRunPaid(@PathVariable UUID id, @RequestBody(required = false) MarkPayrollRunPaidRequest request) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.markPayrollRunPaid(id, request), "Payroll run marked paid"));
    }

    @GetMapping("/payslips")
    public ResponseEntity<ApiResponse<List<PayrollPayslipDto>>> getPayslips() {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslips()));
    }

    @GetMapping("/payslips/{id}")
    public ResponseEntity<ApiResponse<PayrollPayslipDto>> getPayslip(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslip(id)));
    }
}
