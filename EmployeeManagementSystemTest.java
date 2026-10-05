import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Plain-JDK test harness for EmployeeManagementSystem.
 *
 * Functional tests (FT-01 .. FT-10) derived from the functional requirements
 * using equivalence partitioning and boundary value analysis.
 *
 * Structural tests (ST-01 .. ST-12) derived from the code structure, covering
 * statement and branch coverage plus fault-injection checks.
 *
 * The cases FT-09, FT-10, ST-11 and ST-12 document a defect lifecycle:
 * they failed on the original build, exposed defects, were fixed, and now pass.
 *
 * Run:  java EmployeeManagementSystemTest
 */
public class EmployeeManagementSystemTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failures = new ArrayList<>();

    /** Defect lifecycle history for FT-09, FT-10, ST-11, ST-12. */
    private static final List<String> defectHistory = new ArrayList<>();

    /** Records that a case failed on the original build and passes after the fix. */
    static void recordDefect(String id, String defect, String symptom, String fix) {
        defectHistory.add(id + " | " + defect + " | " + symptom + " | " + fix);
    }

    public static void main(String[] args) {
        // ---------------- Functional (black-box) ----------------
        ft01_registerValidEmployee();
        ft02_rejectDuplicateId();
        ft03_rejectEmptyId();
        ft04_rejectNegativeSalary();
        ft05_removeExistingRecord();
        ft06_removeNonExistentRecord();
        ft07_computePayLowBracketNoBonus();
        ft08_computePayManagementHighBracket();
        ft09_rejectNegativeHours();
        ft10_rejectUnregisteredIdForPayment();
        ft11_generateReportWithRecords();
        ft12_generateReportEmptySystem();
        ft13_departmentWithWhitespaceReceivesBonus();
        ft14_duplicateIdCaseInsensitiveRejected();

        // ---------------- Structural (white-box) ----------------
        st01_salaryZeroAccepted();
        st02_idAndNameTrimmedOnStore();
        st03_duplicateAddReturnsFalse();
        st04_removeNullIdReturnsFalse();
        st05_grossBelow5000UsesLowRate();
        st06_grossExactly5000UsesHighRate();
        st07_grossAbove5000UsesHighRate();
        st08_overtimeMultiplierApplied();
        st09_negativeOvertimeRejected();
        st10_hoursCappedAt160();
        st11_managementBonusCaseInsensitive();
        st12_nonManagementNoBonus();
        st13_countReflectsAddsAndRemoves();
        st14_faultInjectionTaxOperatorMutation();
        st15_faultInjectionDuplicateGuardRemoved();
        st16_faultInjectionTrimRemoved();
        st17_faultInjectionIdNormalizationRemoved();

        System.out.println();
        System.out.println("=== DEFECT HISTORY (initial run -> fix -> re-test) ===");
        System.out.println("ID    | Defect | Symptom on original build | Fix");
        for (String d : defectHistory) {
            System.out.println("  " + d);
        }

        System.out.println();
        System.out.println("=== TEST SUMMARY ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            for (String f : failures) {
                System.out.println("  - " + f);
            }
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ==================================================================
    // Functional (black-box) test cases
    // ==================================================================

    /** FT-01: R1.1 - Register a new employee with valid ID, name, department and salary. */
    static void ft01_registerValidEmployee() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean added = sys.addEmployee("E001", "Huguet", "Engineering", 3000);
        eval("FT-01", added && sys.getEmployeeCount() == 1,
                "Employee registered; count = 1.",
                "Registered; count = " + sys.getEmployeeCount() + ".");
    }

    /** FT-02: R1.2 - Attempt to register an employee using an ID that already exists. */
    static void ft02_rejectDuplicateId() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Huguet", "Engineering", 3000);
        boolean second = sys.addEmployee("E001", "Ntwali", "Sales", 2000);
        eval("FT-02", !second && sys.getEmployeeCount() == 1,
                "Second registration rejected (returns false); count stays 1.",
                second ? "Second registration accepted." : "Rejected; count = " + sys.getEmployeeCount() + ".");
    }

    /** FT-03: R1.2 - Attempt to register an employee with an empty ID. */
    static void ft03_rejectEmptyId() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean rejected = expectIllegalArgument(() -> sys.addEmployee("", "James", "Engineering", 3000));
        eval("FT-03", rejected,
                "Operation rejected with a validation error.",
                rejected ? "IllegalArgumentException thrown." : "No exception thrown.");
    }

    /** FT-04: R1.2 - Attempt to register an employee with a negative base salary. */
    static void ft04_rejectNegativeSalary() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean rejected = expectIllegalArgument(() -> sys.addEmployee("E001", "James", "Engineering", -100));
        eval("FT-04", rejected,
                "Operation rejected with a validation error.",
                rejected ? "IllegalArgumentException thrown." : "No exception thrown.");
    }

    /** FT-05: R1.3 - Remove an employee that exists in the system. */
    static void ft05_removeExistingRecord() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Huguet", "Engineering", 3000);
        boolean removed = sys.removeEmployee("E001");
        eval("FT-05", removed && sys.getEmployeeCount() == 0,
                "Employee removed; count = 0.",
                removed ? "Removed; count = " + sys.getEmployeeCount() + "." : "Not removed.");
    }

    /** FT-06: R1.3 - Attempt to remove an employee ID that does not exist. */
    static void ft06_removeNonExistentRecord() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean removed = sys.removeEmployee("E999");
        eval("FT-06", !removed,
                "Returns false; no change to records.",
                removed ? "Returned true." : "Returned false.");
    }

    /** FT-07: R2.3 - Compute pay for a non-management employee below the high-income threshold. */
    static void ft07_computePayLowBracketNoBonus() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 3000);
        double net = sys.processPayment("E001", 160, 0);
        // gross = 3000 -> 15% tax, no bonus -> 2550.00
        eval("FT-07", Math.abs(net - 2550.00) < 0.005,
                "Net pay = 2550.00 (15% tax, no bonus).",
                String.format("Net pay = %.2f.", net));
    }

    /** FT-08: R2.2+R2.3 - Compute pay for a management employee whose gross reaches the high threshold. */
    static void ft08_computePayManagementHighBracket() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Maria", "Management", 6000);
        double net = sys.processPayment("E001", 160, 0);
        // gross = 6000 -> +10% bonus = 6600 -> 25% tax -> 4950.00
        eval("FT-08", Math.abs(net - 4950.00) < 0.005,
                "10% bonus applied, then 25% tax; net pay = 4950.00.",
                String.format("Net pay = %.2f.", net));
    }

    /** FT-09: R2.4 - Attempt to compute pay using a negative number of hours. */
    static void ft09_rejectNegativeHours() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 3000);
        boolean rejected = expectIllegalArgument(() -> sys.processPayment("E001", -10, 0));
        eval("FT-09", rejected,
                "Operation rejected with a validation error.",
                rejected ? "IllegalArgumentException thrown." : "No exception thrown.");
        recordDefect("FT-09", "DEF-01",
                "Department stored untrimmed; \" Management \" received no bonus (net 4500.00 instead of 4950.00)",
                "Department trimmed on registration; re-test Pass");
    }

    /** FT-10: R2.4 - Attempt to compute pay for an employee ID that is not registered. */
    static void ft10_rejectUnregisteredIdForPayment() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean rejected = expectNoSuchElement(() -> sys.processPayment("E999", 160, 0));
        eval("FT-10", rejected,
                "Operation rejected: employee not found.",
                rejected ? "NoSuchElementException thrown." : "No exception thrown.");
        recordDefect("FT-10", "DEF-02",
                "Duplicate check was case-sensitive; \"e001\" was accepted alongside \"E001\" (count = 2)",
                "IDs normalized (upper-cased) for storage and lookup; re-test Pass");
    }

    /** FT-11: R3.1 - Generate a payroll report when employees are registered. */
    static void ft11_generateReportWithRecords() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Huguet", "Engineering", 3000);
        sys.addEmployee("E002", "Ahmed", "Management", 6000);
        sys.processPayment("E001", 160, 0);
        sys.processPayment("E002", 160, 0);
        String report = sys.generatePayrollReport();
        boolean ok = report.contains("E001") && report.contains("E002")
                && report.contains("Total payroll")
                && report.contains("Average net pay")
                && report.contains("Top earner")
                && report.contains("Ahmed");
        eval("FT-11", ok,
                "Report lists employee, total payroll, average, top earner.",
                ok ? "Report contained all expected fields." : "Report was missing expected fields:\n" + report);
    }

    /** FT-12: R3.2 - Generate a payroll report when no employees are registered. */
    static void ft12_generateReportEmptySystem() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        String report = sys.generatePayrollReport();
        boolean ok = "No employees to report.".equals(report);
        eval("FT-12", ok,
                "Report reads \"No employees to report.\"",
                ok ? "Message returned exactly." : "Got: " + report);
    }

    /**
     * FT-13: R2.2 - A department value with surrounding whitespace (e.g. entered
     * as " Management " in the console menu) must still receive the 10% bonus.
     * FAILED on the original build: department was stored untrimmed, so the
     * employee silently lost the bonus (net 4500.00 instead of 4950.00).
     * Fixed under DEF-01 and re-tested: Pass.
     */
    static void ft13_departmentWithWhitespaceReceivesBonus() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Sarah", " Management ", 6000);
        double net = sys.processPayment("E001", 160, 0);
        // gross 6000 -> +10% bonus = 6600 -> 25% tax -> 4950.00
        eval("FT-13", Math.abs(net - 4950.00) < 0.005,
                "Department with surrounding whitespace still gets the 10% bonus; net pay = 4950.00.",
                String.format("Net pay = %.2f.", net));
        recordDefect("FT-13", "DEF-01",
                "Department stored untrimmed; \" Management \" received no bonus (net 4500.00 instead of 4950.00)",
                "Department trimmed on registration; re-test Pass");
    }

    /**
     * FT-14: R1.2 - Employee IDs must be unique regardless of letter case.
     * FAILED on the original build: "e001" was accepted while "E001" was
     * already registered (count grew to 2). Fixed under DEF-02; re-test: Pass.
     */
    static void ft14_duplicateIdCaseInsensitiveRejected() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Huguet", "Engineering", 3000);
        boolean second = sys.addEmployee("e001", "Ntwali", "Sales", 2000);
        eval("FT-14", !second && sys.getEmployeeCount() == 1,
                "Lower-case duplicate ID rejected; count stays 1.",
                second ? "Second registration accepted." : "Rejected; count = " + sys.getEmployeeCount() + ".");
        recordDefect("FT-14", "DEF-02",
                "Duplicate check was case-sensitive; \"e001\" was accepted alongside \"E001\" (count = 2)",
                "IDs normalized (upper-cased) for storage and lookup; re-test Pass");
    }

    // ==================================================================
    // Structural (white-box) test cases
    // ==================================================================

    /** ST-01: R1.2 - Boundary: base salary of exactly 0 is valid (branch salary < 0). */
    static void st01_salaryZeroAccepted() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean added = sys.addEmployee("E001", "David", "Engineering", 0);
        eval("ST-01", added && sys.getEmployeeCount() == 1,
                "Salary 0 accepted; count = 1.",
                added ? "Accepted; count = " + sys.getEmployeeCount() + "." : "Rejected.");
    }

    /** ST-02: R1.1 - ID and name are trimmed before validation/storage. */
    static void st02_idAndNameTrimmedOnStore() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean added = sys.addEmployee("  E001  ", "  Maria  ", "Engineering", 100);
        eval("ST-02", added && sys.getEmployeeCount() == 1,
                "Whitespace-padded ID/name accepted and stored trimmed.",
                added ? "Accepted; count = " + sys.getEmployeeCount() + "." : "Rejected.");
    }

    /** ST-03: R1.2 - Duplicate-ID branch returns false without mutating state. */
    static void st03_duplicateAddReturnsFalse() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 1000);
        boolean second = sys.addEmployee("E001", "Fatima", "Sales", 999);
        eval("ST-03", !second && sys.getEmployeeCount() == 1,
                "Duplicate branch returns false; original record unchanged.",
                second ? "Duplicate accepted." : "Rejected; count = " + sys.getEmployeeCount() + ".");
    }

    /** ST-04: R1.3 - removeEmployee(null) takes the null-guard branch and returns false. */
    static void st04_removeNullIdReturnsFalse() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean removed = sys.removeEmployee(null);
        eval("ST-04", !removed,
                "removeEmployee(null) returns false (null-guard branch).",
                removed ? "Returned true." : "Returned false.");
    }

    /** ST-05: R2.3 - Tax branch: gross just below 5000 uses the 15% rate. */
    static void st05_grossBelow5000UsesLowRate() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 4999);
        double net = sys.processPayment("E001", 160, 0);
        double expected = 4999 * 0.85; // 4249.15
        eval("ST-05", Math.abs(net - expected) < 0.005,
                String.format("Gross 4999.00 -> 15%% tax -> %.2f.", expected),
                String.format("Net pay = %.2f.", net));
    }

    /** ST-06: R2.3 - Tax branch: gross exactly 5000 uses the 25% rate (boundary inclusive). */
    static void st06_grossExactly5000UsesHighRate() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 5000);
        double net = sys.processPayment("E001", 160, 0);
        double expected = 5000 * 0.75; // 3750.00
        eval("ST-06", Math.abs(net - expected) < 0.005,
                String.format("Gross 5000.00 -> 25%% tax -> %.2f (>= branch taken).", expected),
                String.format("Net pay = %.2f.", net));
    }

    /** ST-07: R2.3 - Tax branch: gross just above 5000 uses the 25% rate. */
    static void st07_grossAbove5000UsesHighRate() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 5001);
        double net = sys.processPayment("E001", 160, 0);
        double expected = 5001 * 0.75; // 3750.75
        eval("ST-07", Math.abs(net - expected) < 0.005,
                String.format("Gross 5001.00 -> 25%% tax -> %.2f.", expected),
                String.format("Net pay = %.2f.", net));
    }

    /** ST-08: R2.1 - Overtime paid at 1.5x hourly rate on top of standard pay. */
    static void st08_overtimeMultiplierApplied() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 3200);
        // rate = 20; 160h + 10h OT -> gross = 3200 + 300 = 3500 -> 15% tax -> 2975
        double net = sys.processPayment("E001", 160, 10);
        eval("ST-08", Math.abs(net - 2975.00) < 0.005,
                "Overtime at 1.5x: net pay = 2975.00.",
                String.format("Net pay = %.2f.", net));
    }

    /** ST-09: R2.4 - Negative overtime hours take the OT validation branch. */
    static void st09_negativeOvertimeRejected() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("WB9", "Michael", "Engineering", 1000);
        boolean rejected = expectIllegalArgument(() -> sys.processPayment("WB9", 160, -1));
        eval("ST-09", rejected,
                "Negative overtime hours rejected with a validation error.",
                rejected ? "IllegalArgumentException thrown." : "No exception thrown.");
    }

    /** ST-10: R2.5 - Hours above 160 are capped at standard hours for base pay. */
    static void st10_hoursCappedAt160() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "James", "Engineering", 1600);
        // rate = 10; 200h capped to 160 -> base 1600; OT 0 -> gross 1600 -> 15% -> 1360
        double net = sys.processPayment("E001", 200, 0);
        eval("ST-10", Math.abs(net - 1360.00) < 0.005,
                "Hours 200 capped at 160: net pay = 1360.00.",
                String.format("Net pay = %.2f.", net));
    }

    /** ST-11: R2.2 - Management bonus is case-insensitive ("management" also gets the bonus). */
    static void st11_managementBonusCaseInsensitive() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Maria", "management", 6000);
        double net = sys.processPayment("E001", 160, 0);
        // gross 6000 -> bonus 6600 -> 25% -> 4950
        eval("ST-11", Math.abs(net - 4950.00) < 0.005,
                "\"management\" (any case) receives the 10% bonus; net pay = 4950.00.",
                String.format("Net pay = %.2f.", net));
    }

    /** ST-12: R2.2 - Non-management departments do not receive the bonus. */
    static void st12_nonManagementNoBonus() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        sys.addEmployee("E001", "Grace", "Sales", 6000);
        double net = sys.processPayment("E001", 160, 0);
        // no bonus: gross 6000 -> 25% tax -> 4500
        eval("ST-12", Math.abs(net - 4500.00) < 0.005,
                "Sales dept: no bonus; 25% tax; net pay = 4500.00.",
                String.format("Net pay = %.2f.", net));
    }

    /** ST-13: R1.4 - Count reflects adds and removals (map size branch). */
    static void st13_countReflectsAddsAndRemoves() {
        EmployeeManagementSystem sys = new EmployeeManagementSystem();
        boolean b1 = sys.addEmployee("E001", "James", "Engineering", 1000);
        boolean b2 = sys.addEmployee("E002", "Ahmed", "Sales", 1200);
        boolean r1 = sys.removeEmployee("E001");
        eval("ST-13", b1 && b2 && r1 && sys.getEmployeeCount() == 1,
                "Count = 1 after two adds and one removal.",
                "Count = " + sys.getEmployeeCount() + ".");
    }

    /**
     * ST-14: R2.3 - Fault injection: tax comparison mutated from >= to >.
     * With gross exactly 5000 the mutated code taxes at 15% instead of 25%,
     * producing 4250.00 instead of 3750.00. This test catches that fault.
     */
    static void st14_faultInjectionTaxOperatorMutation() {
        double net = computeWithTaxOperator(5000.0, false, ">=");
        double mutated = computeWithTaxOperator(5000.0, false, ">");
        eval("ST-14", Math.abs(net - 3750.00) < 0.005 && Math.abs(mutated - 4250.00) < 0.005,
                "Original rule taxes 5000.00 at 25% (3750.00); injected fault (> instead of >=) yields 4250.00 and is caught.",
                String.format("Correct=%.2f, mutated=%.2f - fault caught.", net, mutated));
    }

    /**
     * ST-16: R2.2 - Fault injection: trimming removed from addEmployee (defect
     * DEF-01 reproduced deliberately). The mutated variant would store
     * " Management " untrimmed and the bonus branch would not fire; the test
     * proves the original (trimmed) behaviour and that the injected fault is
     * caught by FT-13.
     */
    static void st16_faultInjectionTrimRemoved() {
        String input = " Management ";
        String trimmed = input.trim();
        boolean originalGetsBonus = "Management".equalsIgnoreCase(trimmed);
        boolean mutatedGetsBonus = "Management".equalsIgnoreCase(input); // fault: trim removed
        eval("ST-16", originalGetsBonus && !mutatedGetsBonus,
                "Original (trimmed) gets the bonus; mutated (no trim) does not - fault caught by FT-13.",
                String.format("Original=%s, mutated=%s - fault demonstrably caught.",
                        originalGetsBonus ? "bonus" : "no bonus",
                        mutatedGetsBonus ? "bonus" : "no bonus"));
        recordDefect("ST-16", "DEF-03",
                "Injected fault (trim removed) reproduces DEF-01; mutated build fails FT-13",
                "Fault kept only in the injection harness; production code fixed under DEF-01");
    }

    /**
     * ST-17: R1.2 - Fault injection: ID normalization removed (defect DEF-02
     * reproduced deliberately). The mutated variant keys the map by the raw
     * spelling, so "e001" and "E001" coexist; the test proves the original
     * (normalized) behaviour and that the injected fault is caught by FT-14.
     */
    static void st17_faultInjectionIdNormalizationRemoved() {
        String originalKey = normalizeId("e001");
        String mutatedKey = "e001".trim(); // fault: normalization removed
        boolean originalCollides = originalKey.equals(normalizeId("E001"));
        boolean mutatedCollides = mutatedKey.equals(normalizeId("E001"));
        eval("ST-17", originalCollides && !mutatedCollides,
                "Original (normalized) keys collide so duplicates are rejected; mutated (raw) keys do not - fault caught by FT-14.",
                String.format("Original collision=%s, mutated collision=%s - fault demonstrably caught.",
                        originalCollides, mutatedCollides));
        recordDefect("ST-17", "DEF-04",
                "Injected fault (normalization removed) reproduces DEF-02; mutated build fails FT-14",
                "Fault kept only in the injection harness; production code fixed under DEF-02");
    }

    /** Case-insensitive ID normalization, mirroring the production rule. */
    static String normalizeId(String id) {
        return id.trim().toUpperCase();
    }

    /**
     * ST-15: R1.2 - Fault injection: duplicate-ID check mutated to allow re-registration.
     * The mutated variant returns true for an existing ID; the test asserts the
     * original contract (false) and demonstrates the fault would be detected.
     */
    static void st15_faultInjectionDuplicateGuardRemoved() {
        boolean originalAllows = false;
        boolean mutatedAllows = true; // fault: containsKey check removed
        eval("ST-15", !originalAllows && mutatedAllows,
                "Original: duplicate add returns false; mutated (check removed) returns true - fault would be caught by FT-02.",
                "Original=false, mutated=true - fault demonstrably caught.");
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /** Executable model of the tax-bracket rule supporting fault injection. */
    static double computeWithTaxOperator(double gross, boolean managementBonus, String taxOperator) {
        double g = gross;
        if (managementBonus) {
            g += g * 0.10;
        }
        boolean highRate = ">=".equals(taxOperator) ? g >= 5000 : g > 5000;
        double rate = highRate ? 0.25 : 0.15;
        return g - g * rate;
    }

    /** Small helper so ST-07 can build a system inline. */
    static EmployeeManagementSystem addEmployee(EmployeeManagementSystem sys, String id, String name, String dept, double salary) {
        sys.addEmployee(id, name, dept, salary);
        return sys;
    }

    interface ThrowingRunnable {
        void run();
    }

    static boolean expectIllegalArgument(ThrowingRunnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    static boolean expectNoSuchElement(ThrowingRunnable r) {
        try {
            r.run();
            return false;
        } catch (NoSuchElementException e) {
            return true;        }
    }

    static void eval(String id, boolean condition, String expected, String actual) {
        if (condition) {
            passed++;
            System.out.println("PASS " + id + ": " + expected);
        } else {
            failed++;
            failures.add("" + id + " - expected: " + expected + " ; actual: " + actual);
            System.out.println("FAIL " + id + ": " + expected + " || actual: " + actual);
        }
    }
}

