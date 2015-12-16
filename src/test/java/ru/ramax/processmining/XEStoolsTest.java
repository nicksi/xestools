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
        XTimeExtension.instance().assignTimestamp(xEvent1, Instant.parse("2015-01-01T10:00:00.00Z").toEpochMilli());
        xTrace.add(xEvent1);

        XEvent xEvent2 = xFactory.createEvent();
        XConceptExtension.instance().assignName(xEvent2, "event2");
        XTimeExtension.instance().assignTimestamp(xEvent2, Instant.parse("2015-01-01T10:30:00.00Z").toEpochMilli());
        xTrace.add(xEvent2);

        start = xeStools.traceStartTime("test");
        assertTrue("Should match first event", start.equals(LocalDateTime.of(2015,01,01,10,00,00)));

        xeStools.setXLog(emptyLog);
        start = xeStools.traceStartTime("test");
        assertTrue("Should return dummy", start.equals(LocalDateTime.MIN));

    }
}
