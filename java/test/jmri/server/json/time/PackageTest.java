package jmri.server.json.time;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 *
 * @author Randall Wood (C) 2016
 */
@RunWith(Suite.class)
@SuiteClasses({
    BundleTest.class,
    JsonTimeHttpServiceTest.class,
    JsonTimeSocketServiceTest.class
})
public class PackageTest {
}
