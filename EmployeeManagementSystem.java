import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * EmployeeManagementSystem
 *
 * Implements the scenario for organization X: managing employees,
 * processing their payments, and generating payroll reports.
 * A console menu in main() drives the system interactively.
 *
 * Business rules:
 *   - Overtime is paid at 1.5x the employee's derived hourly rate
 *     (base salary / 160 standard hours).
 *   - Employees in the Management department receive a 10% bonus on gross pay.
 *   - Gross pay at or above 5000 is taxed at 25%; below 5000 it is taxed at 15%.
 *   - Employee ID, name, and salary are validated on registration;
 *     duplicate IDs are rejected (case-insensitively; see DEF-02).
 *   - Department values are trimmed, so " Management " receives the
 *     Management bonus (see DEF-01).
 */
public class EmployeeManagementSystem {

    /** Standard monthly working hours used to derive the hourly rate. */
    public static final double STANDARD_HOURS = 160.0;
    /** Overtime is paid at 1.5x the hourly rate. */
    public static final double OVERTIME_MULTIPLIER = 1.5;
    /** Gross pay at or above this threshold is taxed at the high rate. */
    public static final double HIGH_TAX_THRESHOLD = 5000.0;
    public static final double HIGH_TAX_RATE = 0.25;
    public static final double LOW_TAX_RATE = 0.15;
    /** Management department employees receive a 10% bonus on gross pay. */
    public static final double MANAGEMENT_BONUS_RATE = 0.10;

    /** A single employee record. */
    static class Employee {
        final String id;
        final String name;
        final String department;
        final double baseSalary;

        Employee(String id, String name, String department, double baseSalary) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.baseSalary = baseSalary;
        }

        double hourlyRate() {
            return baseSalary / STANDARD_HOURS;
        }

        boolean isManagement() {
            return "Management".equalsIgnoreCase(department);
        }
    }

    /** Registered employees, kept in insertion order, keyed by normalized (upper-cased) ID. */
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // FR1 / FR2: registration and validation
    // ------------------------------------------------------------------

    /**
     * Normalizes an employee ID for storage and lookup so that IDs are
     * matched case-insensitively (DEF-02 fix). The original spelling is
     * kept in the record for display.
     */
    private static String normalizeId(String id) {
        return id.trim().toUpperCase();
    }

    /**
     * Adds a new employee record.
     *
     * @return true if the employee was added; false if the ID is already in use.
     * @throws IllegalArgumentException if the ID or name is missing/empty,
     *                                  or the base salary is negative.
     */
    public boolean addEmployee(String id, String name, String department, double baseSalary) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID must not be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name must not be empty.");
        }
        if (baseSalary < 0) {
            throw new IllegalArgumentException("Base salary must not be negative.");
        }
        String key = normalizeId(id);
        if (employees.containsKey(key)) {
            return false; // duplicate ID rejected (case-insensitive)
        }
        employees.put(key, new Employee(id.trim(), name.trim(),
                department == null ? "" : department.trim(), baseSalary));
        return true;
    }

    // ------------------------------------------------------------------
    // FR3: removal
    // ------------------------------------------------------------------

    /**
     * Removes an existing employee record by ID.
     *
     * @return true if the employee was removed; false if the ID does not exist.
     */
    public boolean removeEmployee(String id) {
        if (id == null) {
            return false;
        }
        return employees.remove(normalizeId(id)) != null;
    }

    // ------------------------------------------------------------------
    // FR4 / FR5: payment processing
    // ------------------------------------------------------------------

    /**
     * Computes an employee's net payment.
     *
     * @param id            the registered employee ID
     * @param hoursWorked   standard hours worked (must be >= 0)
     * @param overtimeHours overtime hours worked (must be >= 0)
     * @return the net pay after bonus (Management) and tax
     * @throws IllegalArgumentException if hours or overtime hours are negative.
     * @throws NoSuchElementException   if the employee ID is not registered.
     */
    public double processPayment(String id, double hoursWorked, double overtimeHours) {
        if (hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked must not be negative.");
        }
        if (overtimeHours < 0) {
            throw new IllegalArgumentException("Overtime hours must not be negative.");
        }
        Employee emp = employees.get(id == null ? "" : normalizeId(id));
        if (emp == null) {
            throw new NoSuchElementException("No employee registered with ID " + id + ".");
        }

        double hours = Math.min(hoursWorked, STANDARD_HOURS);
        double basePay = hours * emp.hourlyRate();
        double overtimePay = overtimeHours * emp.hourlyRate() * OVERTIME_MULTIPLIER;
        double grossPay = basePay + overtimePay;

        if (emp.isManagement()) {
            grossPay += grossPay * MANAGEMENT_BONUS_RATE;
        }

        double taxRate = grossPay >= HIGH_TAX_THRESHOLD ? HIGH_TAX_RATE : LOW_TAX_RATE;
        return grossPay - grossPay * taxRate;
    }

    // ------------------------------------------------------------------
    // FR6: payroll reporting
    // ------------------------------------------------------------------

    /**
     * Generates a payroll report listing each employee's net pay, the total
     * payroll, the average net pay, and the top earner. States clearly when
     * there are no employees to report.
     */
    public String generatePayrollReport() {
        if (employees.isEmpty()) {
            return "No employees to report.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-20s %-14s %12s%n", "ID", "Name", "Department", "Net Pay"));
        double total = 0.0;
        Employee top = null;
        double topNet = Double.NEGATIVE_INFINITY;
        for (Employee emp : employees.values()) {
            double net = processPayment(emp.id, STANDARD_HOURS, 0);
            total += net;
            if (net > topNet) {
                topNet = net;
                top = emp;
            }
            sb.append(String.format("%-8s %-20s %-14s %12.2f%n",
                    emp.id, emp.name, emp.department, net));
        }
        double average = total / employees.size();
        sb.append(String.format("%nTotal payroll: %.2f%n", total));
        sb.append(String.format("Average net pay: %.2f%n", average));
        sb.append(String.format("Top earner: %s (%.2f)%n", top.name, topNet));
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // FR7: employee count
    // ------------------------------------------------------------------

    /** Returns the current number of registered employees. */
    public int getEmployeeCount() {
        return employees.size();
    }

    // ------------------------------------------------------------------
    // Console menu (no GUI)
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        EmployeeManagementSystem system = new EmployeeManagementSystem();
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== Employee Management System ===");
            System.out.println("1. Add employee");
            System.out.println("2. Remove employee");
            System.out.println("3. Process payment");
            System.out.println("4. Generate payroll report");
            System.out.println("5. Employee count");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("ID: ");
                        String id = in.nextLine();
                        System.out.print("Name: ");
                        String name = in.nextLine();
                        System.out.print("Department: ");
                        String dept = in.nextLine();
                        System.out.print("Base salary: ");
                        double salary = Double.parseDouble(in.nextLine().trim());
                        boolean added = system.addEmployee(id, name, dept, salary);
                        System.out.println(added ? "Employee added." : "Duplicate ID rejected.");
                    }
                    case "2" -> {
                        System.out.print("ID to remove: ");
                        String id = in.nextLine();
                        boolean removed = system.removeEmployee(id);
                        System.out.println(removed ? "Employee removed." : "ID does not exist.");
                    }
                    case "3" -> {
                        System.out.print("Employee ID: ");
                        String id = in.nextLine();
                        System.out.print("Hours worked: ");
                        double hours = Double.parseDouble(in.nextLine().trim());
                        System.out.print("Overtime hours: ");
                        double ot = Double.parseDouble(in.nextLine().trim());
                        System.out.printf("Net pay: %.2f%n", system.processPayment(id, hours, ot));
                    }
                    case "4" -> System.out.println(system.generatePayrollReport());
                    case "5" -> System.out.println("Registered employees: " + system.getEmployeeCount());
                    case "6" -> {
                        System.out.println("Goodbye.");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number entered.");
            } catch (IllegalArgumentException | NoSuchElementException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
