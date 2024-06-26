package jmri.profile;

import jmri.swing.PreferencesPanelTestBase;
import jmri.util.JUnitUtil;

import org.junit.jupiter.api.*;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
public class ProfilePreferencesPanelTest extends PreferencesPanelTestBase<ProfilePreferencesPanel> {

    @Override
    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        JUnitUtil.resetProfileManager();
        prefsPanel = new ProfilePreferencesPanel();
    }

    // private final static Logger log = LoggerFactory.getLogger(ProfilePreferencesPanelTest.class);

}
