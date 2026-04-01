CREATE TABLE payroll_departments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_payroll_departments_name_tenant UNIQUE (name, tenant_id)
);

CREATE TABLE payroll_designations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_payroll_designations_name_tenant UNIQUE (name, tenant_id)
);

CREATE TABLE employee_payroll_profiles (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    user_id UUID NOT NULL,
    employee_code VARCHAR(128) NOT NULL,
    department_id UUID,
    designation_id UUID,
    pay_frequency VARCHAR(32) NOT NULL,
    join_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    CONSTRAINT uk_employee_payroll_profiles_user_tenant UNIQUE (user_id, tenant_id),
    CONSTRAINT uk_employee_payroll_profiles_code_tenant UNIQUE (employee_code, tenant_id),
    CONSTRAINT fk_employee_payroll_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_employee_payroll_profiles_department FOREIGN KEY (department_id) REFERENCES payroll_departments (id),
    CONSTRAINT fk_employee_payroll_profiles_designation FOREIGN KEY (designation_id) REFERENCES payroll_designations (id)
);

CREATE TABLE payroll_components (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    code VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    component_type VARCHAR(32) NOT NULL,
    value_type VARCHAR(32) NOT NULL,
    default_amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    default_rate NUMERIC(19, 6) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    statutory BOOLEAN NOT NULL DEFAULT FALSE,
    editable BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    CONSTRAINT uk_payroll_components_code_tenant UNIQUE (code, tenant_id)
);

CREATE TABLE salary_structures (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    pay_frequency VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_salary_structures_name_tenant UNIQUE (name, tenant_id)
);

CREATE TABLE salary_structure_components (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    salary_structure_id UUID NOT NULL,
    payroll_component_id UUID NOT NULL,
    amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    rate NUMERIC(19, 6) NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_salary_structure_components_structure FOREIGN KEY (salary_structure_id) REFERENCES salary_structures (id) ON DELETE CASCADE,
    CONSTRAINT fk_salary_structure_components_component FOREIGN KEY (payroll_component_id) REFERENCES payroll_components (id),
    CONSTRAINT uk_salary_structure_component_tenant UNIQUE (salary_structure_id, payroll_component_id, tenant_id)
);

CREATE TABLE employee_salary_assignments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    employee_payroll_profile_id UUID NOT NULL,
    salary_structure_id UUID NOT NULL,
    effective_from DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    CONSTRAINT fk_employee_salary_assignments_employee FOREIGN KEY (employee_payroll_profile_id) REFERENCES employee_payroll_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_salary_assignments_structure FOREIGN KEY (salary_structure_id) REFERENCES salary_structures (id)
);

CREATE TABLE attendance_adjustments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    employee_payroll_profile_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    attendance_date DATE,
    source_type VARCHAR(32) NOT NULL,
    source_reference VARCHAR(255),
    device_identifier VARCHAR(255),
    late_minutes INTEGER NOT NULL DEFAULT 0,
    absent_days NUMERIC(12, 3) NOT NULL DEFAULT 0,
    leave_days NUMERIC(12, 3) NOT NULL DEFAULT 0,
    overtime_hours NUMERIC(12, 3) NOT NULL DEFAULT 0,
    manual_allowance NUMERIC(19, 6) NOT NULL DEFAULT 0,
    manual_deduction NUMERIC(19, 6) NOT NULL DEFAULT 0,
    notes TEXT,
    CONSTRAINT fk_attendance_adjustments_employee FOREIGN KEY (employee_payroll_profile_id) REFERENCES employee_payroll_profiles (id) ON DELETE CASCADE
);

CREATE TABLE payroll_settings (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    salary_expense_account_id UUID,
    allowance_expense_account_id UUID,
    deduction_liability_account_id UUID,
    payroll_payable_account_id UUID,
    cash_clearing_account_id UUID,
    monthly_work_days INTEGER NOT NULL DEFAULT 30,
    weekly_work_days INTEGER NOT NULL DEFAULT 7,
    default_currency VARCHAR(3) NOT NULL DEFAULT 'BDT',
    CONSTRAINT uk_payroll_settings_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_payroll_settings_salary_expense FOREIGN KEY (salary_expense_account_id) REFERENCES chart_of_accounts (id),
    CONSTRAINT fk_payroll_settings_allowance_expense FOREIGN KEY (allowance_expense_account_id) REFERENCES chart_of_accounts (id),
    CONSTRAINT fk_payroll_settings_deduction_liability FOREIGN KEY (deduction_liability_account_id) REFERENCES chart_of_accounts (id),
    CONSTRAINT fk_payroll_settings_payroll_payable FOREIGN KEY (payroll_payable_account_id) REFERENCES chart_of_accounts (id),
    CONSTRAINT fk_payroll_settings_cash_clearing FOREIGN KEY (cash_clearing_account_id) REFERENCES chart_of_accounts (id)
);

CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    run_number VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    pay_frequency VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    notes TEXT,
    approved_at TIMESTAMP,
    paid_at TIMESTAMP,
    journal_entry_id UUID,
    CONSTRAINT uk_payroll_runs_number_tenant UNIQUE (run_number, tenant_id),
    CONSTRAINT fk_payroll_runs_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id)
);

CREATE TABLE payroll_run_items (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    payroll_run_id UUID NOT NULL,
    employee_payroll_profile_id UUID NOT NULL,
    employee_salary_assignment_id UUID,
    gross_pay NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_earnings NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(19, 6) NOT NULL DEFAULT 0,
    statutory_deductions NUMERIC(19, 6) NOT NULL DEFAULT 0,
    net_pay NUMERIC(19, 6) NOT NULL DEFAULT 0,
    working_days NUMERIC(12, 3) NOT NULL DEFAULT 0,
    absent_days NUMERIC(12, 3) NOT NULL DEFAULT 0,
    leave_days NUMERIC(12, 3) NOT NULL DEFAULT 0,
    overtime_hours NUMERIC(12, 3) NOT NULL DEFAULT 0,
    manual_allowance NUMERIC(19, 6) NOT NULL DEFAULT 0,
    manual_deduction NUMERIC(19, 6) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    notes TEXT,
    CONSTRAINT fk_payroll_run_items_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_run_items_employee FOREIGN KEY (employee_payroll_profile_id) REFERENCES employee_payroll_profiles (id),
    CONSTRAINT fk_payroll_run_items_assignment FOREIGN KEY (employee_salary_assignment_id) REFERENCES employee_salary_assignments (id),
    CONSTRAINT uk_payroll_run_items_employee_tenant UNIQUE (payroll_run_id, employee_payroll_profile_id, tenant_id)
);

CREATE TABLE payroll_payslips (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    payroll_run_item_id UUID NOT NULL,
    payslip_number VARCHAR(128) NOT NULL,
    generated_at TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_payroll_payslips_run_item FOREIGN KEY (payroll_run_item_id) REFERENCES payroll_run_items (id) ON DELETE CASCADE,
    CONSTRAINT uk_payroll_payslips_run_item_tenant UNIQUE (payroll_run_item_id, tenant_id),
    CONSTRAINT uk_payroll_payslips_number_tenant UNIQUE (payslip_number, tenant_id)
);

CREATE TABLE payroll_payments (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    payroll_run_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    reference VARCHAR(255),
    amount NUMERIC(19, 6) NOT NULL DEFAULT 0,
    notes TEXT,
    CONSTRAINT fk_payroll_payments_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id) ON DELETE CASCADE
);

CREATE INDEX idx_employee_payroll_profiles_department ON employee_payroll_profiles (department_id);
CREATE INDEX idx_employee_payroll_profiles_designation ON employee_payroll_profiles (designation_id);
CREATE INDEX idx_employee_salary_assignments_employee ON employee_salary_assignments (employee_payroll_profile_id);
CREATE INDEX idx_attendance_adjustments_employee_period ON attendance_adjustments (employee_payroll_profile_id, period_start, period_end);
CREATE INDEX idx_payroll_runs_status ON payroll_runs (status);
CREATE INDEX idx_payroll_run_items_run ON payroll_run_items (payroll_run_id);
CREATE INDEX idx_payroll_payslips_run_item ON payroll_payslips (payroll_run_item_id);
