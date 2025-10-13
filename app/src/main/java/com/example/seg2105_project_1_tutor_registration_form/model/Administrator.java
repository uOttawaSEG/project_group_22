package com.example.seg2105_project_1_tutor_registration_form.model;

public class Administrator extends User {
    private String employeeId;
    private String office;

    public Administrator() {}

    public Administrator(String id, String f, String l, String e, String p, String ph,
                         String employeeId, String office) {
        super(id, f, l, e, p, ph);
        this.employeeId = employeeId;
        this.office = office;
    }

    @Override public Role getRole() { return Role.ADMINISTRATOR; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String v) { this.employeeId = v; }
    public String getOffice() { return office; }
    public void setOffice(String v) { this.office = v; }
}
