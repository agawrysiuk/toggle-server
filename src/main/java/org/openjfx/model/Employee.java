package org.openjfx.model;

public class Employee {
    private String name;
    private String surname;
    private int employeeID;
    private String email;
    private int employeePIN;
    private boolean online;

    public Employee() {
    }

    public String getName() {
        return surname + " " + name;
    }

    public String getFirstName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public int getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(int employeeID) {
        this.employeeID = employeeID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getEmployeePIN() {
        return employeePIN;
    }

    public void setEmployeePIN(int employeePIN) {
        this.employeePIN = employeePIN;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }
}
