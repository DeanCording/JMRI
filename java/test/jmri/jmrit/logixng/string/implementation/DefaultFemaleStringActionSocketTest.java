package jmri.jmrit.logixng.string.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jmri.*;
import jmri.jmrit.logixng.*;
import jmri.jmrit.logixng.string.actions.StringActionMemory;
import jmri.util.JUnitUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test ExpressionTimer
 * 
 * @author Daniel Bergqvist 2018
 */
public class DefaultFemaleStringActionSocketTest extends FemaleSocketTestBase {

    private String _memorySystemName;
    private Memory _memory;
    private MyStringActionMemory _action;
    
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Override
    protected Manager<? extends NamedBean> getManager() {
        return InstanceManager.getDefault(StringActionManager.class);
    }
    
    @Test
    public void testBundleClass() {
        Assert.assertEquals("bundle is correct", "Test Bundle bb aa cc", Bundle.getMessage("TestBundle", "aa", "bb", "cc"));
        Assert.assertEquals("bundle is correct", "Generic", Bundle.getMessage(Locale.US, "SocketTypeGeneric"));
        Assert.assertEquals("bundle is correct", "Test Bundle bb aa cc", Bundle.getMessage(Locale.US, "TestBundle", "aa", "bb", "cc"));
    }
    
    @Test
    public void testGetName() {
        Assert.assertTrue("String matches", "A1".equals(_femaleSocket.getName()));
    }
    
    @Test
    public void testGetDescription() {
        Assert.assertTrue("String matches", "!s".equals(_femaleSocket.getShortDescription()));
        Assert.assertTrue("String matches", "!s A1".equals(_femaleSocket.getLongDescription()));
    }
    
    @Override
    protected FemaleSocket getFemaleSocket(String name) {
        return new DefaultFemaleStringActionSocket(null, new FemaleSocketListener() {
            @Override
            public void connected(FemaleSocket socket) {
            }

            @Override
            public void disconnected(FemaleSocket socket) {
            }
        }, name);
    }
    
    @Override
    protected boolean hasSocketBeenSetup() {
        return _action._hasBeenSetup;
    }
    
    @Test
    public void testSetValue() throws Exception {
        // Every test method should have an assertion
        Assert.assertNotNull("femaleSocket is not null", _femaleSocket);
        Assert.assertFalse("femaleSocket is not connected", _femaleSocket.isConnected());
        // Test setValue() when not connected
        ((DefaultFemaleStringActionSocket)_femaleSocket).setValue("");
    }
    
    @Test
    public void testGetConnectableClasses() {
        Map<Category, List<Class<? extends Base>>> map = new HashMap<>();
        
        List<Class<? extends Base>> classes = new ArrayList<>();
        classes.add(jmri.jmrit.logixng.string.actions.StringActionMemory.class);
        map.put(Category.ITEM, classes);
        
        classes = new ArrayList<>();
        map.put(Category.COMMON, classes);
        
        classes = new ArrayList<>();
//        classes.add(jmri.jmrix.loconet.logixng.StringActionLocoNet_OPC_PEER.class);
        map.put(Category.OTHER, classes);
        
        classes = new ArrayList<>();
        map.put(Category.EXRAVAGANZA, classes);
        
        Assert.assertTrue("maps are equal",
                isConnectionClassesEquals(map, _femaleSocket.getConnectableClasses()));
    }
    
    // The minimal setup for log4J
    @Before
    public void setUp() {
        JUnitUtil.setUp();
        JUnitUtil.resetInstanceManager();
        JUnitUtil.initInternalSensorManager();
        JUnitUtil.initInternalTurnoutManager();
        
        flag = new AtomicBoolean();
        errorFlag = new AtomicBoolean();
        _memorySystemName = "IM1";
        _memory = InstanceManager.getDefault(MemoryManager.class).provide(_memorySystemName);
        _action = new MyStringActionMemory("IQSA321");
        _action.setMemory(_memory);
        StringActionMemory otherAction = new StringActionMemory("IQSA322", null);
        maleSocket = new DefaultMaleStringActionSocket(_action);
        otherMaleSocket = new DefaultMaleStringActionSocket(otherAction);
        _femaleSocket = new DefaultFemaleStringActionSocket(null, new FemaleSocketListener() {
            @Override
            public void connected(FemaleSocket socket) {
                flag.set(true);
            }

            @Override
            public void disconnected(FemaleSocket socket) {
                flag.set(true);
            }
        }, "A1");
    }

    @After
    public void tearDown() {
        JUnitUtil.tearDown();
    }
    
    
    
    private class MyStringActionMemory extends StringActionMemory {
        
        private boolean _hasBeenSetup = false;
        
        public MyStringActionMemory(String systemName) {
            super(systemName, null);
        }
        
        /** {@inheritDoc} */
        @Override
        public void setup() {
            _hasBeenSetup = true;
        }
    }
    
}
