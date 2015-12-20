package org.processmining.xestools;

import com.google.common.collect.BoundType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.processmining.log.utils.XLogBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Range.closed;
import static com.google.common.collect.Range.upTo;
import static org.junit.Assert.assertTrue;

/**
 * Created by nsitnikov on 20/12/15.
 * Just broke test in two files
 */
public class XESTools2Test {

    @Rule
    public Timeout globalTimeout= new Timeout(10000);

    @Test
    public void filterTest() {
        XLog alog = XLogBuilder.newInstance().startLog("FILTER TEST")
                .addTrace("test 1")
                    .addEvent("event 1")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T10:00:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                    .addEvent("event 2")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T10:10:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                    .addEvent("event 1")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T10:30:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .addTrace("test 2")
                    .addEvent("event 1")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-02T10:00:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "PETROV")
                    .addEvent("event 2")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-02T10:10:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "PETROV")
                    .addEvent("event 1")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-02T10:30:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .addTrace("test 3")
                    .addEvent("event 1")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T19:00:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                    .addEvent("event 3")
                        .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T19:10:00.00Z")))
                        .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .build();

        assertTrue("Log should have 3 traces, got "+ alog.size(), alog.size() == 3);

        XEStools xeStools = new XEStools(alog);
        assertTrue("Log still should be 3 traces, got "+xeStools.getXLogSize(), xeStools.getXLogSize() == 3);

        Map<XEStools.FilterType, Object> filter = Maps.newHashMap();
        filter.put(XEStools.FilterType.EVENT_COUNT_RANGE, upTo(2, BoundType.CLOSED));
        List<FlatXTrace> filtered = xeStools.getFullTraceList(filter);
        assertTrue("Should be only one trace, got " + filtered.size(), filtered.size() == 1);

        filter.clear();
        filter.put(XEStools.FilterType.TRACE_START_RANGE, closed(
                ZonedDateTime.ofInstant(Instant.parse("2015-01-01T00:00:00.00Z"), ZoneId.of("UTC")),
                ZonedDateTime.ofInstant(Instant.parse("2015-01-01T23:59:59.00Z"), ZoneId.of("UTC"))
        ));
        filtered = xeStools.getFullTraceList(filter);
        assertTrue("Should be only two traces, got " + filtered.size(), filtered.size() == 2);

        filter.clear();
        filter.put(XEStools.FilterType.RESOURCE_LIST, Lists.newArrayList("IVANOV"));
        filtered = xeStools.getFullTraceList(filter);
        assertTrue("Should be only two traces, got " + filtered.size(), filtered.size() == 2);

        filter.clear();
        filter.put(XEStools.FilterType.RESOURCE_LIST, Lists.newArrayList("PETROV"));
        filtered = xeStools.getFullTraceList(filter);
        assertTrue("Should be no traces, got " + filtered.size(), filtered.size() == 0);

        filter.clear();
        filter.put(XEStools.FilterType.RESOURCE_LIST, Lists.newArrayList("PETROV", "MULTI"));
        filtered = xeStools.getFullTraceList(filter);
        assertTrue("Should be only one trace, got " + filtered.size(), filtered.size() == 1);

        filter.clear();
        filter.put(XEStools.FilterType.RESOURCE_LIST, Lists.newArrayList("MULTI"));
        filter.put(XEStools.FilterType.EVENT_NAME_LIST, Lists.newArrayList("event 3"));
        filtered = xeStools.getFullTraceList(filter);
        assertTrue("Should be no trace, got " + filtered.size(), filtered.size() == 0);
    }

    @Test
    public void workloadMatrixTest() {
        XLog alog = XLogBuilder.newInstance().startLog("FILTER TEST")
                .addTrace("test 1")
                .addEvent("event 1")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T10:00:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .addEvent("event 2")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T11:10:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "SIDOROV")
                .addEvent("event 1")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T12:30:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .addTrace("test 2")
                .addEvent("event 1")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-02T10:00:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "PETROV")
                .addEvent("event 2")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-02T10:10:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "PETROV")
                .addEvent("event 1")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-02T10:30:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .addTrace("test 3")
                .addEvent("event 1")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T19:00:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .addEvent("event 3")
                .addAttribute(XTimeExtension.KEY_TIMESTAMP, Date.from(Instant.parse("2015-01-01T19:40:00.00Z")))
                .addAttribute(XOrganizationalExtension.KEY_RESOURCE, "IVANOV")
                .build();

        assertTrue("Log should have 3 traces, got "+ alog.size(), alog.size() == 3);

        XEStools xeStools = new XEStools(alog);
        assertTrue("Log still should be 3 traces, got "+xeStools.getXLogSize(), xeStools.getXLogSize() == 3);

        List<Workload> matrix = xeStools.calculateResourceWorkload(null);
        assertTrue("There should be 6 records, got "+ matrix.size(), matrix.size() == 8);
        assertTrue("2nd workload event should be SIDOROV, got " + matrix.get(1).getResource(), matrix.get(1).getResource().equals("SIDOROV"));
        assertTrue("2nd workload event duration should be 3000, got " + matrix.get(1).getWorkload(), matrix.get(1).getWorkload() == 3000);
        assertTrue("2nd workload event should be at 11:00, got " + matrix.get(1).getTimestamp(), matrix.get(1).getTimestamp().equals(ZonedDateTime.ofInstant(Instant.parse("2015-01-01T11:00:00.00Z"), ZoneId.of("UTC"))));
        assertTrue("8th workload event should be PETROV, got " + matrix.get(7).getResource(), matrix.get(7).getResource().equals("PETROV"));
        assertTrue("8th workload event duration should be 1800, got " + matrix.get(7).getWorkload(), matrix.get(7).getWorkload() == 1800);

    }
}
