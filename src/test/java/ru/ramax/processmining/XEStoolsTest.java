package ru.ramax.processmining;

import junit.framework.TestCase;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Unit test for simple App.
 */
public class XEStoolsTest extends TestCase
{
    private XLog emptyLog;
    private XFactoryNaiveImpl xFactory;

    public void setUp()
    {
        // create XLOG
        xFactory = new XFactoryNaiveImpl();

        XAttributeMap logAttributes = xFactory.createAttributeMap();
        logAttributes.put("process", xFactory.createAttributeLiteral("concept:name", "Test transaction", XConceptExtension.instance()));
        emptyLog = xFactory.createLog(logAttributes);

        XLifecycleExtension xLifecycleExtension = XLifecycleExtension.instance();
        xLifecycleExtension.assignModel(emptyLog, "standard");

        XTimeExtension xTimeExtension = XTimeExtension.instance();
        XOrganizationalExtension xOrganizationalExtension = XOrganizationalExtension.instance();
        XConceptExtension xConceptExtension = XConceptExtension.instance();

        emptyLog.getGlobalTraceAttributes().add(xFactory.createAttributeLiteral("concept:name", "name", xConceptExtension));

        emptyLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("concept:name", "", xConceptExtension));
        emptyLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("org:resource", "", xOrganizationalExtension));
        emptyLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("time:timestamp", "", xTimeExtension));
        emptyLog.getGlobalEventAttributes().add(xFactory.createAttributeLiteral("lifecycle:transition", "", xLifecycleExtension));

        XEventNameClassifier xEventNameClassifier = new XEventNameClassifier();
        emptyLog.getClassifiers().add(xEventNameClassifier);

        emptyLog.getExtensions().add(xConceptExtension);
        emptyLog.getExtensions().add(xTimeExtension);
        emptyLog.getExtensions().add(xOrganizationalExtension);
        emptyLog.getExtensions().add(xLifecycleExtension);
    }

    public void testConstructor()
    {
        XEStools xeStools = new XEStools(emptyLog);
        assertNotNull("Failed to initilize.", xeStools);

        XLog xLog = xeStools.getXlog();
        assertNotNull("Must be non null", xLog);
        assertTrue("tools must return log", XLog.class.isInstance(xLog));

    }

    public void  testTraceStart() {
        XEStools xeStools = new XEStools(emptyLog);
        assertNotNull("Failed to initilize.", xeStools);

        LocalDateTime start = xeStools.traceStartTime("test");
        assertTrue("Should return dummy", start.equals(LocalDateTime.MIN));

        // add some trace with event and reset utils
        XLog xLog = (XLog) emptyLog.clone();

        XTrace xTrace = xFactory.createTrace();
        XConceptExtension.instance().assignName(xTrace,"test");
        xLog.add(xTrace);
        xeStools.setXLog(xLog);
        start = xeStools.traceStartTime("test");
        assertTrue("Should return dummy", start.equals(LocalDateTime.MIN));

        XEvent xEvent1 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent1, "event1");
        XTimeExtension.instance().assignTimestamp(xEvent1, Instant.parse("2015-01-01T10:30:00.00Z").toEpochMilli());
        xTrace.add(xEvent1);

        XEvent xEvent2 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent2, "event2");
        XTimeExtension.instance().assignTimestamp(xEvent2, Instant.parse("2015-01-01T10:00:00.00Z").toEpochMilli());
        xTrace.add(xEvent2);

        XEvent xEvent3 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent3, "event1");
        XTimeExtension.instance().assignTimestamp(xEvent3, Instant.parse("2015-01-01T10:40:00.00Z").toEpochMilli());
        xTrace.add(xEvent3);

        start = xeStools.traceStartTime("test");
        assertTrue("Should match first event (name). Expected 10:00, got "+ start.toString(), start.equals(LocalDateTime.of(2015,01,01,10,00,00)));

        start = xeStools.traceStartTime(xTrace);
        assertTrue("Should match first event (trace). Expected 10:00, got "+ start.toString(), start.equals(LocalDateTime.of(2015,01,01,10,00,00)));

        start = xeStools.traceStartTime(xTrace, "event1");
        assertTrue("Should match first event (trace) of type event1. Expected 10:30, got "+ start.toString(), start.equals(LocalDateTime.of(2015,01,01,10,30,00)));

        start = xeStools.traceStartTime("test", "event1");
        assertTrue("Should match first event (trace) of type event1. Expected 10:30, got "+ start.toString(), start.equals(LocalDateTime.of(2015,01,01,10,30,00)));

        LocalDateTime end = null;
        end = xeStools.traceEndTime("test");
        assertTrue("Should match last event (name). Expected 10:40, got "+ end.toString(), end.equals(LocalDateTime.of(2015,01,01,10,40,00)));

        end = xeStools.traceEndTime(xTrace);
        assertTrue("Should match last event (trace). Expected 10:40, got "+ end.toString(), end.equals(LocalDateTime.of(2015,01,01,10,40,00)));

        end = xeStools.traceEndTime(xTrace, "event2");
        assertTrue("Should match last event (trace) of type event2. Expected 10:00, got "+ end.toString(), end.equals(LocalDateTime.of(2015,01,01,10,00,00)));

        end = xeStools.traceEndTime("test", "event2");
        assertTrue("Should match last event (trace) of type event2. Expected 10:00, got "+ end.toString(), end.equals(LocalDateTime.of(2015,01,01,10,00,00)));

        end = xeStools.traceStartTime("testNon");
        assertTrue("Should return dummy", end.equals(LocalDateTime.MIN));

        xeStools.setXLog(emptyLog);
        start = xeStools.traceStartTime("test");
        assertTrue("Should return dummy", start.equals(LocalDateTime.MIN));

    }

    public void testDuration() {
        XEStools xeStools = new XEStools(emptyLog);
        assertNotNull("Failed to initilize.", xeStools);

        LocalDateTime start = xeStools.traceStartTime("test");
        assertTrue("Should return dummy", start.equals(LocalDateTime.MIN));

        // add some trace with event and reset utils
        XLog xLog = (XLog) emptyLog.clone();

        XTrace xTrace = xFactory.createTrace();
        XConceptExtension.instance().assignName(xTrace,"test");
        xLog.add(xTrace);
        xeStools.setXLog(xLog);
        start = xeStools.traceStartTime("test");
        assertTrue("Should return dummy", start.equals(LocalDateTime.MIN));

        XEvent xEvent1 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent1, "event1");
        XTimeExtension.instance().assignTimestamp(xEvent1, Instant.parse("2015-01-01T10:30:00.00Z").toEpochMilli());
        xTrace.add(xEvent1);

        XEvent xEvent2 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent2, "event2");
        XTimeExtension.instance().assignTimestamp(xEvent2, Instant.parse("2015-01-01T10:00:00.00Z").toEpochMilli());
        xTrace.add(xEvent2);

        XEvent xEvent3 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent3, "event1");
        XTimeExtension.instance().assignTimestamp(xEvent3, Instant.parse("2015-01-01T10:40:00.00Z").toEpochMilli());
        xTrace.add(xEvent3);

        Long duration = xeStools.getTraceDuration("test");
        assertTrue("Test duration calculation. Expected 2400 got "+duration, duration == 2400);

        duration = xeStools.getTraceDuration(xTrace);
        assertTrue("Test duration calculation. Expected 2400 got "+duration, duration == 2400);

        duration = xeStools.getTraceDuration(xTrace, "event1", "event1");
        assertTrue("Test duration calculation. Expected 2400 got "+duration, duration == 600);

        duration = xeStools.getTraceDuration(xTrace, "event", "event1");
        assertTrue("Test duration calculation. Expected 0 got "+duration, duration == 0);


    }
}
