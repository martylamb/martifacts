package com.martiansoftware.time;

import com.martiansoftware.time.DateRange;
import java.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mlamb
 */
public class DateRangeTest {
    
    public DateRangeTest() {
    }
    
    private LocalDate ld(String s) { return LocalDate.parse(s); }
    
    @Test public void testSimpleDateQuery() throws Exception {
        DateRange dr = DateRange.forQuery("2015-10-09");
        LocalDate ld = ld("2015-10-09");
        assertEquals(ld, dr.from());
        assertEquals(ld, dr.to());
    }

    @Test public void testAgoQuery() throws Exception {
        DateRange dr = DateRange.forQuery("-2w3d");
        assertEquals(LocalDate.now().minusWeeks(2).minusDays(3), dr.from());
        assertEquals(LocalDate.now(), dr.to());
    }
    
    @Test public void testPlusMinusQuery() throws Exception {
        DateRange dr = DateRange.forQuery("2015-10-10+-1y2m3w4d");
        assertEquals(ld("2014-07-16"), dr.from());
        assertEquals(ld("2017-01-04"), dr.to());        
    }

    @Test public void testSimpleRange() throws Exception {
        DateRange dr = DateRange.forQuery("2015-10-10-2015-10-01");
        assertEquals(ld("2015-10-01"), dr.from());
        assertEquals(ld("2015-10-10"), dr.to());
    }
    
    @Test public void testPlusQuery() throws Exception {
        DateRange dr = DateRange.forQuery("2015-10-10+3d");
        assertEquals(ld("2015-10-10"), dr.from());
        assertEquals(ld("2015-10-13"), dr.to());
    }
    
    @Test public void testEquivalentQueries() throws Exception {
        DateRange dr1 = DateRange.forQuery("2015-10-10+1w1d");
        DateRange dr2 = DateRange.forQuery("2015-10-18-1w1d");
        assertEquals(dr1, dr2);
        assertEquals(dr1.hashCode(), dr2.hashCode());
        DateRange dr3 = DateRange.forQuery("2015-10-18-1w2d");
        assertFalse(dr2.equals(dr3));
        assertFalse(dr2.hashCode() == dr3.hashCode());
    }
    
    @Test public void testBareModifier() throws Exception {
        DateRange dr = DateRange.forQuery("1d");
        assertEquals(LocalDate.now().minusDays(1), dr.from());
        assertEquals(LocalDate.now(), dr.to());
    }
    
    /**
     * Test of toString method, of class DateRange.
     */
    @Test
    public void testToString() throws Exception {
        DateRange dr = DateRange.forQuery("2015-10-10+-1y2m3w4d");
        assertEquals("[2014-07-16, 2017-01-04]", dr.toString());
    }
}
