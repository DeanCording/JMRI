package jmri.jmrix.sprog.pi.pisprognano;

import jmri.jmrix.sprog.SprogSystemConnectionMemo;
import jmri.util.JUnitUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Tests for PiSprogNanoSerialDriverAdapter
 * </P>
 * @author Paul Bender Copyright (C) 2016
 */
public class PiSprogNanoSerialDriverAdapterTest {

   @Test
   public void ConstructorTest(){
       PiSprogNanoSerialDriverAdapter a = new PiSprogNanoSerialDriverAdapter();
       Assert.assertNotNull(a);
 
       // clean up
       ((SprogSystemConnectionMemo)a.getSystemConnectionMemo()).getSprogTrafficController().dispose();
  }

    // The minimal setup for log4J
    @Before
    public void setUp() {
        JUnitUtil.setUp();
    }

    @After
    public void tearDown() {
        JUnitUtil.tearDown();
    }


}
