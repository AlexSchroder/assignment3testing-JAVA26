package ltu;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Calendar;
import java.util.Date;

public class PaymentTest {

    private ICalendar getMockCalendar(int year, int month, int day) {
        return new ICalendar() {
            @Override
            public Date getDate() {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day); 
                return cal.getTime();
            }
        };
    }

    // ====================================================================
    // 1. EXCEPTION TESTS
    // ====================================================================

    @Test(expected = IllegalArgumentException.class)
    public void testNullPersonId() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        payment.getMonthlyAmount(null, 0, 100, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidIdLength() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        payment.getMonthlyAmount("19910510", 0, 100, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIncome() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        payment.getMonthlyAmount("19910510-1234", -1, 100, 100);
    }

    // ====================================================================
    // 2. AGE TESTS
    // ====================================================================

    /**
     * Requirement [ID: 101]
     * Bug: Buggy code uses > 20 instead of >= 20.
     */
    @Test
    public void testAge20() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19960510-1234", 0, 100, 100);
        assertEquals(9904, amount);
    }

    /**
     * Requirement [ID: 101] limit test.
     */
    @Test
    public void testAge19() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19970510-0000", 0, 100, 100);
        assertEquals(0, amount);
    }

    /**
     * Requirement [ID: 103]
     * Bug: Buggy code allows loans for 47-year-olds (<= 47).
     */
    @Test
    public void testAge47() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19690510-1234", 0, 100, 100);
        assertEquals(2816, amount);
    }

    /**
     * Requirement [ID: 102]
     * Bug: Buggy code returns Integer.MAX_VALUE for > 56.
     */
    @Test
    public void testAge57() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19590510-1234", 0, 100, 100);
        assertEquals(0, amount);
    }

    // ====================================================================
    // 3. STUDY PACE TESTS
    // ====================================================================

    /**
     * Requirement [ID: 501, 502]
     */
    @Test
    public void testFullTimeAmounts() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 100, 100);
        assertEquals(9904, amount);
    }

    /**
     * Requirement [ID: 503, 504]
     * Bug: Buggy code assigns 4564 for part-time loan instead of 3564.
     */
    @Test
    public void testPartTimeAmounts() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 75, 100);
        assertEquals(4960, amount);
    }

    // ====================================================================
    // 4. INCOME & COMPLETION TESTS
    // ====================================================================

    /**
     * Requirement [ID: 301]
     */
    @Test
    public void testHighIncomeFullTime() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 85814, 100, 100);
        assertEquals(0, amount);
    }

    /**
     * Requirement [ID: 302]
     * Bug: Buggy code awards half subsidy (1396) even if limit is exceeded.
     */
    @Test
    public void testHighIncomePartTime() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 128723, 75, 100);
        assertEquals(0, amount);
    }

    /**
     * Requirement [ID: 401]
     */
    @Test
    public void testLowCompletionRatio() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 10);
        PaymentImpl payment = new PaymentImpl(cal);
        int amount = payment.getMonthlyAmount("19910510-1234", 0, 100, 49);
        assertEquals(0, amount);
    }

    // ====================================================================
    // 5. DATE TESTS
    // ====================================================================

    /**
     * Requirement [ID: 506]
     * Bug: Buggy code hardcodes February to 28 days.
     */
    @Test
    public void testLeapYearFeb() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.FEBRUARY, 15);
        PaymentImpl payment = new PaymentImpl(cal);
        String paymentDay = payment.getNextPaymentDay();
        assertEquals("20160229", paymentDay);
    }

    @Test
    public void testPaymentDayFormat() throws Exception {
        ICalendar cal = getMockCalendar(2016, Calendar.MAY, 15);
        PaymentImpl payment = new PaymentImpl(cal);
        String paymentDay = payment.getNextPaymentDay();
        assertNotNull(paymentDay);
        assertEquals(8, paymentDay.length());
    }
}