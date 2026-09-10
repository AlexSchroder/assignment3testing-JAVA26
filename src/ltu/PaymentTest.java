package ltu;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

import org.junit.Test;




public class PaymentTest
{
    private static class FixedCalendar implements ICalendar
    {
        public Date getDate(){
            return new Date ("25/05/2016");
        }
    }
 

    @Test
    public void testUnder20GetsNoPayment() throws IOException
    {
        PaymentImpl payment = new PaymentImpl(new FixedCalendar());

        assertEquals(0, payment.getMonthlyAmount("19970202-0000", 0, 100, 100));
    }

    @Test
    public void test20AndOlderGetsPayment() throws IOException
    {
        PaymentImpl payment = new PaymentImpl(new FixedCalendar());
        
        assertEquals(9904, payment.getMonthlyAmount("19960202-0000", 0, 100, 100));
    }

    @Test
    public void test49PercentCompltetion() throws IOException
    {
        PaymentImpl payment = new PaymentImpl(new FixedCalendar());
        
        assertEquals(0, payment.getMonthlyAmount("19960202-0000", 0, 100, 49));
    }
    @Test
    public void test50PercentCompltetion() throws IOException
    {
        PaymentImpl payment = new PaymentImpl(new FixedCalendar());
        
        assertEquals(9904, payment.getMonthlyAmount("19960202-0000", 0, 100, 50));
    }

}
