package jmri.jmrix.ieee802154.xbee;

/**
 * Listener interface to be notified about XBee traffic
 *
 * @author Bob Jacobsen Copyright (C) 2001, 2006, 2007, 2008
 */
interface XBeeListener extends jmri.jmrix.AbstractMRListener {

    void message(XBeeMessage m);

    void reply(XBeeReply m);
}


