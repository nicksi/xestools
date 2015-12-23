package org.processmining.xestools;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.model.XTrace;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by nsitnikov on 17/12/15.
 * Attempt to read real life log
 */
public class RealLogTest {
    @Test
    public void logLoadAndTraceStatistics() throws Exception {
        XesXmlGZIPParser parser = new XesXmlGZIPParser();

        URL url = RealLogTest.class.getClassLoader().getResource("out.xes.gz");
        URI uri = url.toURI();
        assertNotNull("File not present", uri);
        File file = new File(uri);
        assertNotNull("File should be present", file);
        assertTrue("File should be parsable", parser.canParse(file));

        XEStools parsed = new XEStools();
        boolean result = parsed.parseLog(Paths.get(url.toURI()).toString());
        assertTrue("Log should be parsed", result);

        XTrace xTrace = parsed.getXTrace("610705");
        assertNotNull("Trace 610705 should be present", xTrace);
        assertTrue("Trace has 4 events, count is" + xTrace.size(), xTrace.size() == 4);

        boolean lessThen3 = false;
        Iterator iterator = parsed.getXlog().iterator();
        while (iterator.hasNext()) {
            XTrace current = (XTrace) iterator.next();
            if (current.size() < 3) {
                lessThen3 = true;
                break;
            }
        }
        assertTrue("All traces has at least 3 events", !lessThen3);

        //String name = "709340";
        String name = "710400";
        xTrace = parsed.getXTrace(name);
        assertNotNull("Trace " + name + " should be present", xTrace);
        assertTrue("Trace has 122 events, count is " + xTrace.size(), xTrace.size() == 122);
        assertTrue("Trace start time should be before end time", XEStools.traceStartTime(xTrace).isBefore(XEStools.traceEndTime(xTrace)));

        Map<XEStools.FilterType, Object> filter = Maps.newHashMap();
        filter.put(XEStools.FilterType.TRACE_NAME_LIST, Lists.newArrayList(name));

        List<FlatXTrace> trace = parsed.getFullTraceList(filter);
        assertTrue("Trace " + name + " should be present", trace.size() == 1);
        assertTrue("Start dates should match", XEStools.traceStartTime(xTrace).equals(trace.get(0).getStartTime()));
        assertTrue("End dates should match", XEStools.traceEndTime(xTrace).equals(trace.get(0).getEndTime()));

        List<FlatXEvent> eventList = parsed.getEventList(filter);
        assertTrue("Expected number of events 62, got " + eventList.size(), eventList.size()==62);

    }
}
