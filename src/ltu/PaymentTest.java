package ltu;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Calendar;
import java.util.Date;

public class PaymentTest {

    /**
     * Helper method to create a mock calendar. 
     * This avoids repeating the anonymous inner class code in every test 
     * and lets us lock the system time to a specific date (e.g., in 2016).
     */
    private ICalendar getMockCalendar(int year, int month, int day) {
        return new ICalendar() {
            @Override
            public Date getDate() {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day); // Force the calendar to a specific date
                return cal.getTime();
            }
        };
    }

    // 1. Bugg: Max Value Subsidy (Över 56 år får över 2 miljarder kr istället för 0)
    @Test
    public void testBugOver56GetsZeroSubsidy() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // Född 1959 -> 57 år 2016
        int amount = payment.getMonthlyAmount("19590510-1234", 0, 100, 100);
        // Ska ge 0, men buggiga koden ger Integer.MAX_VALUE
        assertEquals(0, amount);
    }

    // 2. Bugg: 20-åringar nekas bidrag (Off-by-one: > 20 istället för >= 20)
    @Test
    public void testBugExactly20YearsOldGetsSubsidy() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // Född 1996 -> exakt 20 år 2016
        int amount = payment.getMonthlyAmount("19960510-1234", 0, 100, 100);
        // Ska ge fullt lån (7088) + fullt bidrag (2816) = 9904. Buggiga koden ger 7088.
        assertEquals(9904, amount);
    }

    // 3. Bugg: 47-åringar får lån (Off-by-one: <= 47 istället för < 47)
    @Test
    public void testBugExactly47YearsOldNoLoan() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // Född 1969 -> exakt 47 år 2016
        int amount = payment.getMonthlyAmount("19690510-1234", 0, 100, 100);
        // Ska endast ge bidrag (2816). Buggiga koden ger lån + bidrag (9904).
        assertEquals(2816, amount);
    }

    // 4. Bugg: Deltidslånet är felstavat (4564 istället för 3564)
    @Test
    public void testBugHalfTimeLoanAmount() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 50, 100);
        // Ska ge halvt lån (3564) + halvt bidrag (1396) = 4960. Buggiga ger 5960.
        assertEquals(4960, amount);
    }

    // 5. Bugg: Skottårshanteringen för februari är hårdkodad till 28 dagar
    @Test
    public void testBugLeapYearFebruary() throws Exception {
        // Skottår 2016, sätter datumet till någon gång i februari
        ICalendar cal = getMockCalendar(2016, Calendar.FEBRUARY, 15);
        PaymentImpl payment = new PaymentImpl(cal);
        String paymentDay = payment.getNextPaymentDay();
        // Sista dagen i feb 2016 var måndag 29 februari -> "20160229"
        assertEquals("20160229", paymentDay);
    }

    // 6. Bugg: Deltidsinkomst över gränsen ger ändå halvt bidrag
    @Test
    public void testBugPartTimeIncomeExceededYieldsZero() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // Inkomst (128723) är 1 kr över gränsen för deltid
        int amount = payment.getMonthlyAmount("19910510-1234", 128723, 75, 100);
        // Rätt kod ger 0. Buggig kod ger 1396.
        assertEquals(0, amount);
    }

    /**
     * Tests error handling: Passing a null personId should trigger 
     * an IllegalArgumentException. The (expected = ...) attribute tells JUnit 
     * that throwing this exception means the test passes successfully.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullPersonIdThrowsException() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        payment.getMonthlyAmount(null, 0, 100, 100);
    }

    /**
     * Tests error handling: Person ID must be 13 characters (yyyymmdd-nnnn). 
     * Passing a short ID (10 chars) should throw an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPersonIdLengthThrowsException() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        payment.getMonthlyAmount("19910510", 0, 100, 100);
    }

    /**
     * Tests error handling: Income cannot be negative. 
     * Passing -1 should trigger an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIncomeThrowsException() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        payment.getMonthlyAmount("19910510-1234", -1, 100, 100);
    }

    /**
     * Business Rule Test: Age requirement (< 20).
     * A student born in 1997 is 19 years old in 2016, which is under the 
     * minimum age limit. Both loan and subsidy should be 0.
     */
    @Test
    public void testUnderAgeLoanAndSubsidyIsZero() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // 1997 birth year -> Age 19 in 2016
        int amount = payment.getMonthlyAmount("19970510-0000", 0, 100, 100);
        assertEquals(0, amount);
    }

    /**
     * Business Rule Test: Standard full-time student payments.
     * A student born in 1991 is 25 years old in 2016 (fully eligible).
     * Full loan (7088) + Full subsidy (2816) = 9904 SEK.
     */
    @Test
    public void testFullTimeValidStudentStandardAmounts() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 100, 100);
        assertEquals(9904, amount);
    }

    /**
     * Business Rule Test: Part-time studies (75% pace).
     * Half loan (3564) + Half subsidy (1396) = 4960 SEK.
     */
    @Test
    public void testPartTimeValidStudentAmounts() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 75, 100);
        assertEquals(4960, amount);
    }

    /**
     * Business Rule Test: Loan age cutoff limit.
     * A student turning 47 in the target year loses eligibility for loans (0 loan), 
     * but remains eligible for subsidies (2816 SEK) since subsidy cutoff is > 56.
     */
    @Test
    public void testLoanAgeLimitExceeded() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // 1969 birth year -> Age 47 in 2016
        int amount = payment.getMonthlyAmount("19690510-1234", 0, 100, 100);
        assertEquals(2816, amount);
    }

    /**
     * Business Rule Test: Subsidy age cutoff limit.
     * A student turning 57 in 2016 exceeds both the loan limit (>= 47) 
     * and the subsidy limit (> 56), resulting in 0 total payment.
     */
    @Test
    public void testSubsidyAgeLimitExceeded() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        // 1959 birth year -> Age 57 in 2016
        int amount = payment.getMonthlyAmount("19590510-1234", 0, 100, 100);
        assertEquals(0, amount);
    }

    /**
     * Business Rule Test: Income threshold limit.
     * Earning more than the full-time income cap (85813 SEK) results in 0 payment.
     */
    @Test
    public void testFullTimeIncomeLimitExceeded() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 90000, 100, 100);
        assertEquals(0, amount);
    }

    /**
     * Business Rule Test: Study completion ratio requirement.
     * A completion ratio below 50% fails the academic requirement, yielding 0 payment.
     */
    @Test
    public void testLowCompletionRatioResultsInZero() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 100, 40);
        assertEquals(0, amount);
    }

    /**
     * Utility Test: Verifies that `getNextPaymentDay()` successfully calculates 
     * a valid date string and that it matches the expected 8-character format (yyyymmdd).
     */
    @Test
    public void testGetNextPaymentDayFormat() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        String paymentDay = payment.getNextPaymentDay();
        assertNotNull(paymentDay);
        assertEquals(8, paymentDay.length());
    }
}