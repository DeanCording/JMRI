package jmri.jmrix.openlcb.swing.eventtable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import jmri.*;
import jmri.jmrix.can.CanSystemConnectionMemo;

import org.openlcb.*;


/**
 * Pane for displaying a table of relationships of nodes, producers and consumers
 * <p>
 *
 * @author Bob Jacobsen Copyright (C) 2023
 */
public class EventTablePane extends jmri.util.swing.JmriPanel
        implements jmri.jmrix.can.swing.CanPanelInterface {

    protected CanSystemConnectionMemo memo;
    Connection connection;
    NodeID nid;

    MimicNodeStore store;
    EventTableDataModel model;

    JCheckBox showRequiresLabel;
    JCheckBox showRequiresMatch; // requires at least one consumer and one producer exist

    private transient TableRowSorter<EventTableDataModel> sorter;

    public String getTitle(String menuTitle) {
        return Bundle.getMessage("TitleEventTable");
    }

    @Override
    public void initComponents(CanSystemConnectionMemo memo) {
        this.memo = memo;
        this.connection = memo.get(Connection.class);
        this.nid = memo.get(NodeID.class);

        store = memo.get(MimicNodeStore.class);

        model = new EventTableDataModel(store);
        sorter = new TableRowSorter<>(model);


        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add to GUI here

        var table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(sorter);

        var scrollPane = new JScrollPane(table);
        add(scrollPane);

        var buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        add(buttonPanel);

        var updateButton = new JButton(Bundle.getMessage("ButtonUpdate"));
        updateButton.addActionListener(this::sendRequestEvents);
        buttonPanel.add(updateButton);

        showRequiresLabel = new JCheckBox(Bundle.getMessage("BoxShowRequiresLabel"));
        showRequiresLabel.addActionListener((ActionEvent e) -> {
            filter();
        });
        buttonPanel.add(showRequiresLabel);

        showRequiresMatch = new JCheckBox(Bundle.getMessage("BoxShowRequiresMatch"));
        showRequiresMatch.addActionListener((ActionEvent e) -> {
            filter();
        });
        buttonPanel.add(showRequiresMatch);
        buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());

        // hook up to receive traffic
        memo.get(OlcbInterface.class).registerMessageListener(new Monitor(model));
    }

    public EventTablePane() {
    }

    @Override
    public String getHelpTarget() {
        return "package.jmri.jmrix.openlcb.swing.eventtable.EventTablePane";
    }

    @Override
    public String getTitle() {
        if (memo != null) {
            return (memo.getUserName() + " Event Table");
        }
        return getTitle(Bundle.getMessage("TitleEventTable"));
    }

    public void sendRequestEvents(java.awt.event.ActionEvent e) {
        model.clear();
        final int DELAY = 75; // msec between operations - 64 events at speed
        int nextDelay = 0;

        for (var memo : store.getNodeMemos()) {

            jmri.util.ThreadingUtil.runOnLayoutDelayed(() -> {
                var destNodeID = memo.getNodeID();
                log.trace("sendRequestEvents {} {}", nid, destNodeID);
                Message m = new IdentifyEventsAddressedMessage(nid, destNodeID);
                connection.put(m, null);
            }, nextDelay);
            nextDelay += DELAY;
        }
    }

    /**
     * Set up filtering of displayed rows
     */
    private void filter() {
        RowFilter<EventTableDataModel, Integer> rf = new RowFilter<EventTableDataModel, Integer>() {
            /**
             * @return true if row is to be displayed
             */
            @Override
            public boolean include(RowFilter.Entry<? extends EventTableDataModel, ? extends Integer> entry) {
                // default filter is IN-USE and regular systems slot
                // the default is whatever the person last closed it with

                int row = entry.getIdentifier();

                var name = model.getValueAt(row, EventTableDataModel.COL_EVENTNAME);
                if ( showRequiresLabel.isSelected() && (name == null || name.toString().isEmpty()) ) return false;

                if ( showRequiresMatch.isSelected()) {
                    var memo = model.getTripleMemo(row);

                    if (memo.producer == null && !model.producerPresent(memo.consumer, memo.eventID)) {
                        // no matching producer
                        return false;
                    }

                    if (memo.consumer == null && !model.consumerPresent(memo.producer, memo.eventID)) {
                        // no matching producer
                        return false;
                    }
                }

                return true;
            }
        };
        sorter.setRowFilter(rf);
    }


    /**
     * Nested class to hold data model
     */
    protected static class EventTableDataModel extends AbstractTableModel {

        EventTableDataModel(MimicNodeStore store) {
            this.store = store;
            tagManager = InstanceManager.getDefault(IdTagManager.class);
            if (tagManager == null) log.error("no TagManager for persisting events");
        }

        static final int COL_EVENTID = 0;
        static final int COL_EVENTNAME = 1;
        static final int COL_PRODUCER_NODE = 2;
        static final int COL_PRODUCER_NAME = 3;
        static final int COL_CONSUMER_NODE = 4;
        static final int COL_CONSUMER_NAME = 5;
        static final int COL_CONTEXT_INFO = 6; // TODO:  This is just a test column, not shown if COUNT == 6
        static final int COL_COUNT = 6;

        MimicNodeStore store;
        IdTagManager tagManager;

        TripleMemo getTripleMemo(int row) {
            if (row >= memos.size()) {
                log.warn("request out of range: {} greater than {}", row, memos.size());
                return null;
            }
            return memos.get(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= memos.size()) {
                log.warn("request out of range: {} greater than {}", row, memos.size());
                return "Illegal col "+row+" "+col;
            }
            var memo = memos.get(row);
            switch (col) {
                case COL_EVENTID: return memo.eventID.toShortString();
                case COL_EVENTNAME:
                    var tag = tagManager.getIdTag(tagPrefix+memo.eventID.toShortString());
                    if (tag == null) return "";
                    return tag.getUserName();
                    // return memo.eventName != null ? memo.eventName.toString() : "";
                case COL_PRODUCER_NODE:
                    return memo.producer != null ? memo.producer.toString() : "";
                case COL_PRODUCER_NAME: return memo.producerName;
                case COL_CONSUMER_NODE:
                    return memo.consumer != null ? memo.consumer.toString() : "";
                case COL_CONSUMER_NAME: return memo.consumerName;
                case COL_CONTEXT_INFO:
                    return new String[]{"foo", "bar", "biff"};
                default: return "Illegal row "+row+" "+col;
            }
        }

        static final String tagPrefix = "ID_OpenLCB_";  // Prefix of IdTag system name

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col != COL_EVENTNAME) return;
            if (row >= memos.size()) {
                log.warn("request out of range: {} greater than {}", row, memos.size());
                return;
            }
            var memo = memos.get(row);
            var tag = tagManager.provideIdTag("ID_OpenLCB_"+memo.eventID.toShortString());
            tag.setUserName(memo.eventName);
        }

        @Override
        public int getColumnCount() {
            return COL_COUNT;
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case COL_EVENTID:       return "Event ID";
                case COL_EVENTNAME:     return "Event Name";
                case COL_PRODUCER_NODE: return "Producer Node";
                case COL_PRODUCER_NAME: return "Producer Node Name";
                case COL_CONSUMER_NODE: return "Consumer Node";
                case COL_CONSUMER_NAME: return "Consumer Node Name";
                case COL_CONTEXT_INFO:  return "";
                default: return "ERROR "+col;
            }
        }

        // TODO: Why is this not an @Override?  Doing nothing? See SlotMonDataModel
        public int getPreferredWidth(int col) {
            switch (col) {
                case COL_EVENTID:
                    return new JTextField(23+1).getPreferredSize().width;
                case COL_PRODUCER_NODE:
                case COL_CONSUMER_NODE:
                    return new JTextField(17+1).getPreferredSize().width;
                default:
                    return 75; // default value from JavaDoc
            }
        }

        @Override
        public int getRowCount() {
            return memos.size();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == COL_EVENTNAME;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == COL_CONTEXT_INFO) return String[].class;
            else return String.class;
        }

        /**
         * Remove all existing data, generally just in advance of an update
         */
        void clear() {
            memos = new ArrayList<>();
        }

        ArrayList<TripleMemo> memos = new ArrayList<>();

        /**
         * Record an event-producer pair
         */
        void recordProducer(EventID eventID, NodeID nodeID) {
            log.trace("recordProducer of {} in {}", eventID, nodeID);

            var nodeMemo = store.findNode(nodeID);
            String name = "";
            if (nodeMemo != null) {
                var ident = nodeMemo.getSimpleNodeIdent();
                    if (ident != null) {
                        name = ident.getUserName();
                    }
            }


            // if this already exists, skip storing it
            // if you can, find a matching memo with an empty consumer value
            TripleMemo empty = null;
            TripleMemo bestEmpty = null;
            for (var memo : memos) {
                if (memo.eventID.equals(eventID) ) {
                    // if matches, ignore
                    if (nodeID.equals(memo.producer)) {
                        return;
                    }
                    // if empty producer slot, remember it
                    if (memo.producer == null) {
                        empty = memo;
                        // best empty has matching consumer
                        if (nodeID.equals(memo.consumer)) bestEmpty = memo;
                    }
                }
            }

            // can we use the bestEmpty?
            if (bestEmpty != null) {
                // yes
                log.trace("   use bestEmpty");
                bestEmpty.producer = nodeID;
                bestEmpty.producerName = name;
                fireTableDataChanged();
                return;
            }

            // can we use the empty?
            if (empty != null) {
                // yes
                log.trace("   reuse empty");
                empty.producer = nodeID;
                empty.producerName = name;
                fireTableDataChanged();
                return;
            }

            // have to make a new one
            var memo = new TripleMemo(
                            eventID,
                            "",
                            nodeID,
                            name,
                            null,
                            ""
                        );
            memos.add(memo);
            fireTableStructureChanged();
        }

        /**
         * Record an event-consumer pair
         */
        void recordConsumer(EventID eventID, NodeID nodeID) {
            log.trace("recordConsumer of {} in {}", eventID, nodeID);

            var nodeMemo = store.findNode(nodeID);
            String name = "";
            if (nodeMemo != null) {
                var ident = nodeMemo.getSimpleNodeIdent();
                    if (ident != null) {
                        name = ident.getUserName();
                    }
            }

            // if this already exists, skip storing it
            // if you can, find a matching memo with an empty consumer value
            TripleMemo empty = null;
            TripleMemo bestEmpty = null;
            for (var memo : memos) {
                if (memo.eventID.equals(eventID) ) {
                    // if matches, ignore
                    if (nodeID.equals(memo.consumer)) {
                        return;
                    }
                    // if empty consumer slot, remember it
                    if (memo.consumer == null) {
                        empty = memo;
                        // best empty has matching producer
                        if (nodeID.equals(memo.producer)) bestEmpty = memo;
                    }
                }
            }

            // can we use the best empty?
            if (bestEmpty != null) {
                // yes
                log.trace("   use bestEmpty");
                bestEmpty.consumer = nodeID;
                bestEmpty.consumerName = name;
                fireTableDataChanged();
                return;
            }

            // can we use the empty?
            if (empty != null) {
                // yes
                log.trace("   reuse empty");
                empty.consumer = nodeID;
                empty.consumerName = name;
                fireTableDataChanged();
                return;
            }

            // have to make a new one
            var memo = new TripleMemo(
                            eventID,
                            "",
                            null,
                            "",
                            nodeID,
                            name
                        );
            memos.add(memo);
            fireTableStructureChanged();
        }

        boolean consumerPresent(NodeID nodeID, EventID eventID) {
            for (var memo : memos) {
                if (memo.eventID.equals(eventID) ) {
                    if (nodeID.equals(memo.consumer)) return true;
                }
            }
            return false;
        }

        boolean producerPresent(NodeID nodeID, EventID eventID) {
            for (var memo : memos) {
                if (memo.eventID.equals(eventID) ) {
                    if (nodeID.equals(memo.producer)) return true;
                }
            }
            return false;
        }

        static class TripleMemo {
            EventID eventID;
            // Event name is stored as an IdTag
            NodeID producer;
            String producerName;
            NodeID consumer;
            String consumerName;

            TripleMemo(EventID eventID, String eventName, NodeID producer, String producerName,
                        NodeID consumer, String consumerName) {
                this.eventID = eventID;
                this.eventName = eventName;
                this.producer = producer;
                this.producerName = producerName;
                this.consumer = consumer;
                this.consumerName = consumerName;
            }
        }
    }

    /**
     * Internal class to watch OpenLCB traffic
     */

    static class Monitor extends MessageDecoder {

        Monitor(EventTableDataModel model) {
            this.model = model;
        }

        EventTableDataModel model;

        /**
         * Handle "Producer/Consumer Event Report" message
         * @param msg       message to handle
         * @param sender    connection where it came from
         */
        public void handleProducerConsumerEventReport(ProducerConsumerEventReportMessage msg, Connection sender){
            var nodeID = msg.getSourceNodeID();
            var eventID = msg.getEventID();
            model.recordProducer(eventID, nodeID);
        }

        /**
         * Handle "Consumer Identified" message
         * @param msg       message to handle
         * @param sender    connection where it came from
         */
        public void handleConsumerIdentified(ConsumerIdentifiedMessage msg, Connection sender){
            var nodeID = msg.getSourceNodeID();
            var eventID = msg.getEventID();
            model.recordConsumer(eventID, nodeID);
        }

        /**
         * Handle "Producer Identified" message
         * @param msg       message to handle
         * @param sender    connection where it came from
         */
        public void handleProducerIdentified(ProducerIdentifiedMessage msg, Connection sender){
            var nodeID = msg.getSourceNodeID();
            var eventID = msg.getEventID();
            model.recordProducer(eventID, nodeID);
        }
    }

    /**
     * Nested class to create one of these using old-style defaults
     */
    public static class Default extends jmri.jmrix.can.swing.CanNamedPaneAction {

        public Default() {
            super("Openlcb Event Table",
                    new jmri.util.swing.sdi.JmriJFrameInterface(),
                    EventTablePane.class.getName(),
                    jmri.InstanceManager.getDefault(jmri.jmrix.can.CanSystemConnectionMemo.class));
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EventTablePane.class);
}
