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
import org.deckfour.xes.out.XesXmlGZIPSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Unit test for simple App.
 */
public class XEStoolsTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private XLog emptyLog;
    private XFactoryNaiveImpl xFactory;

    @Before
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

    @Test
    public void XEStoolsConstructor()
    {
        XEStools xeStools = new XEStools(emptyLog);
        assertNotNull("Failed to initilize.", xeStools);

        XLog xLog = xeStools.getXlog();
        assertNotNull("Must be non null", xLog);
        assertTrue("tools must return log", XLog.class.isInstance(xLog));

    }

    @Test
    public void xesParser() throws Exception {
        XLog xLog = (XLog) emptyLog.clone();

        XTrace xTrace = xFactory.createTrace();
        XConceptExtension.instance().assignName(xTrace,"test");
        xLog.add(xTrace);

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

        String filename = "test.xes.gz";
        File log = tempFolder.newFile(filename);
        OutputStream outputStream = new FileOutputStream(log);
        XesXmlGZIPSerializer serializer = new XesXmlGZIPSerializer();
        serializer.serialize(xLog, outputStream);
        outputStream.flush();
        outputStream.close();

        XEStools parsed = new XEStools();
        boolean result = parsed.parseLog(tempFolder.getRoot().toString() + "/123");
        assertTrue("Parsing shoudl fail", !result);
        result = parsed.parseLog(tempFolder.getRoot().toString() + "/" + filename);
        assertTrue("Result should be true", result);
        assertNotNull("Log should be parsed", parsed.getXlog());
        assertTrue("Parsed log contains one trace, got "+ parsed.getXLogSize(), parsed.getXLogSize() == 1);
        assertNotNull("Parsed log has test trace", parsed.getXTrace("test"));


    }

    @Test
    public void  traceStartEndTime() {
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

    @Test
    public void traceDuration() {
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

    @Test
    public void logDurations() {
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
        XOrganizationalExtension.instance().assignResource(xEvent1, "RES001");
        xTrace.add(xEvent1);

        XEvent xEvent2 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent2, "event2");
        XOrganizationalExtension.instance().assignResource(xEvent1, "RES001");
        XTimeExtension.instance().assignTimestamp(xEvent2, Instant.parse("2015-01-01T10:00:00.00Z").toEpochMilli());
        xTrace.add(xEvent2);

        XEvent xEvent3 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent3, "event1");
        XOrganizationalExtension.instance().assignResource(xEvent1, "RES003");
        XTimeExtension.instance().assignTimestamp(xEvent3, Instant.parse("2015-01-01T10:40:00.00Z").toEpochMilli());
        xTrace.add(xEvent3);

        XTrace xTrace2 = xFactory.createTrace();
        XConceptExtension.instance().assignName(xTrace2,"test2");
        xLog.add(xTrace2);
        XEvent xEvent4 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent4, "event1");
        XTimeExtension.instance().assignTimestamp(xEvent4, Instant.parse("2015-01-01T10:30:00.00Z").toEpochMilli());
        XOrganizationalExtension.instance().assignResource(xEvent4, "RES001");
        xTrace2.add(xEvent4);

        XEvent xEvent5 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent5, "event2");
        XOrganizationalExtension.instance().assignResource(xEvent4, "RES001");
        XTimeExtension.instance().assignTimestamp(xEvent5, Instant.parse("2015-01-01T10:00:00.00Z").toEpochMilli());
        xTrace2.add(xEvent5);

        XEvent xEvent6 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent6, "event1");
        XOrganizationalExtension.instance().assignResource(xEvent4, "RES003");
        XTimeExtension.instance().assignTimestamp(xEvent6, Instant.parse("2015-01-01T10:40:00.00Z").toEpochMilli());
        xTrace2.add(xEvent6);

        Map<String, Long> result = xeStools.getTraceDurations();
        assertTrue("We expect two traces, got "+result.size(), result.size() == 2);
        assertTrue("Test2 trace should have duration of 2400 seconds, got " + result.get("test2"), result.get("test2") == 2400);

        Map<String, Long> result2 = xeStools.getTraceDurations("event1", "event1");
        assertTrue("We expect two traces, got "+result2.size(), result2.size() == 2);
        assertTrue("Test2 trace should have duration of 600 seconds, got " + result2.get("test2"), result2.get("test2") == 600);


    }
}
