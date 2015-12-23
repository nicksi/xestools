package org.processmining.xestools;

import com.google.common.collect.*;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.model.*;
import org.processmining.xeslite.external.XFactoryExternalStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.processmining.log.utils.XUtils.getConceptName;

/**
 * Set of methods to work with XES logs
 * Created by Nikolai Sitnikov on 16/12/15.
 */
public class XEStools {

    static private final ZonedDateTime MINTIME = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());
    static private final ZonedDateTime MAXTIME = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());

    private static final long EVENT_DEFAULT_DURATION = 600L;

    @Getter
    private XLog xlog;

    public enum FilterType {
        EVENT_COUNT_RANGE,
        RESOURCE_LIST,
        ROLE_LIST,
        GROUP_LIST,
        LIFECYCLE_TRANSITION_LIST,
        TRACE_START_RANGE,
        TRACE_END_RANGE,
        EVENT_NAME_LIST,
        TRACE_START_WEEKDAY_LIST,
        TRACE_END_WEEKDAY_LIST,
        TRACE_NAME_LIST
    }

    // cache to search for trace by concept:name
    private Map<String, Integer> name2index = Maps.newHashMap();

    public XEStools() {
        this.xlog = null;
    }

    public XEStools(@NonNull XLog xlog) {
        setXLog(xlog);
    }

    public boolean parseLog(String filename) {
        try {
            XesXmlGZIPParser parser = new XesXmlGZIPParser(new XFactoryExternalStore.MapDBDiskImpl());
            File file = new File(filename);
            if (parser.canParse(file)) {
                InputStream inputStream = new FileInputStream(file);
                this.xlog = parser.parse(inputStream).get(0);
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /***
     * Select log to work with and clear cache
     * @param xLog - new XLog
     */
    public void setXLog(@NonNull XLog xLog) {
        this.xlog = xLog;
        clearCache();
    }

    /***
     * Return number of traces in log
     * @return - number of traces. 0 if none
     */
    public int getXLogSize() {
        return xlog.size();
    }


    /***
     * Find the start timestamp of the trace
     * @param xTrace XTrace object to process
     * @param eventName name of the event to search for, null to consider all events
     * @return ZonedDateTime
     */
    static public ZonedDateTime traceStartTime(@NonNull XTrace xTrace, String eventName) {
        ZonedDateTime startTime = MINTIME;

        if (xTrace.size() > 0) {
            for(XEvent xEvent: xTrace) {
                if (eventName != null && !getAttribute(xEvent, "concept:name").equals(eventName)) continue;
                ZonedDateTime current = getTimeStamp(xEvent);

                if (current != null) {
                    if (startTime.equals(MINTIME) || current.isBefore(startTime)) {
                        startTime = current;
                    }
                }
            }
        }

        return startTime;
    }

    /***
     * Find startTime of the earliest event
     * @param xTrace xTrace object to process
     * @return zoned date time
     */
    static public ZonedDateTime traceStartTime(@NonNull XTrace xTrace) {
        return traceStartTime(xTrace, null);
    }

    /***
     * Find the start timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @return zoned date time
     */
    public ZonedDateTime traceStartTime(@NonNull String index) {
        ZonedDateTime startTime = MINTIME;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            startTime = traceStartTime(xTrace, null);
        }

        return startTime;
    }

    /***
     * Find the first timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @param eventName name of Event to look for or null
     * @return zoned date time
     */
    public ZonedDateTime traceStartTime(@NonNull String index, String eventName) {
        ZonedDateTime startTime = MINTIME;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            startTime = traceStartTime(xTrace, eventName);
        }

        return startTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param xTrace xTrace to process
     * @param eventName name of the event to search for, null to consider all events
     * @return zoned date time
     */
    static public ZonedDateTime traceEndTime(@NonNull XTrace xTrace, String eventName) {
        ZonedDateTime endTime = MAXTIME;

        if (xTrace.size() > 0) {
            for(XEvent xEvent: xTrace) {
                if (eventName != null && !getAttribute(xEvent, "concept:name").equals(eventName)) continue;
                ZonedDateTime current = getTimeStamp(xEvent);

                if (current != null) {
                    if (endTime.equals(MAXTIME) || current.isAfter(endTime)) {
                        endTime = current;
                    }
                }
            }
        }

        return  endTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @return zoned date time
     */
    public ZonedDateTime traceEndTime(@NonNull String index) {
        ZonedDateTime endTime = MAXTIME;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            endTime = traceEndTime(xTrace);
        }

        return endTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param xTrace trace to process
     * @return zoned date time
     */
    static public ZonedDateTime traceEndTime(@NonNull XTrace xTrace) {
        return  traceEndTime(xTrace, null);
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @param eventName name of the event to search for, null to consider all events
     * @return zoned date time
     */
    public ZonedDateTime traceEndTime(@NonNull String index, String eventName) {
        ZonedDateTime endTime = MAXTIME;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            endTime = traceEndTime(xTrace, eventName);
        }

        return endTime;
    }

    /***
     * Find the duration of trace
     * @param xTrace trace to process
     * @return trace duration
     */
    static public Long getTraceDuration(@NonNull XTrace xTrace) {
        return getTraceDuration(xTrace, null, null);
    }


    /***
     * Find the duration of trace based on trace concept:name
     * @param index Concept name to find trace
     * @return trace duration
     */
    public Long getTraceDuration(@NonNull String index) {
        Long duration = 0L;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            duration = getTraceDuration(xTrace, null, null);
        }

        return duration;
    }

    /***
     * Calculate duration of trace segment based on event names limits
     * @param xTrace trace to process
     * @param startEventName start event name in trace. Null to remove limit
     * @param endEventName end event name in trace. Null to remove limit
     * @return trace/segment duration
     */
    static public Long getTraceDuration(@NonNull XTrace xTrace, String startEventName, String endEventName) {
        Long duration = 0L;

        ZonedDateTime start = traceStartTime(xTrace, startEventName);
        ZonedDateTime end = traceEndTime(xTrace, endEventName);
        if (start.isAfter(MINTIME) && end.isBefore(MAXTIME) && end.isAfter(start)) {
            duration = Duration.between(start, end).toMillis()/1000;
        }

        return duration;
    }

    /***
     * Return map of all traces/segments durations limited by event names
     * @param startEvent start event name in trace. Null to remove limit
     * @param endEvent end event name in trace. Null to remove limit
     * @return map of trace/segment durations
     */
    public Map<String, Long> getTraceDurations(String startEvent, String endEvent) {
        Map <String, Long> durations = Maps.newHashMap();

        for(XTrace xTrace: xlog) {
            durations.put(getConceptName(xTrace), getTraceDuration(xTrace, startEvent, endEvent));
        }

        return durations;
    }

    /***
     * Return trace durations for the whole log
     * @return trace duration
     */
    public Map<String, Long> getTraceDurations() {
        return getTraceDurations(null, null);
    }


    /***
     * Returns the list of flat trace segments filtered by filter providers
     * @param filter Map of filters as attribute name = allowed value. Value сan be regex
     * @param startName event name of segment start
     * @param endName event name of segment end
     * @return list of trace segments with calculated statistics
     */
    public List<FlatXTrace> getFullSubTraceList(Map<FilterType, Object> filter, String startName, String endName) {
        List<FlatXTrace> traces = Lists.newArrayList();

        for (XTrace current: xlog) {

            current = trimTrace(current, startName, endName);
            if (current != null && current.size() > 0) {
                if (filterMatch(filter, current)) {
                    FlatXTrace flatXTrace = new FlatXTrace(current);
                    traces.add(flatXTrace);
                }
            }

        }

        return traces;
    }

    /***
     * Copy trace segment as separate trace
     * @param trace trace to process
     * @param startName name of segment starting event
     * @param endName name of segment ending event
     * @return trace or null if start/end combination do not exist in trace
     */
    static public XTrace trimTrace(XTrace trace, String startName, String endName) {
        XTrace trimmed = (XTrace)trace.clone();

        ZonedDateTime startTime = traceStartTime(trimmed, startName);
        ZonedDateTime endTime = traceEndTime(trimmed, endName);
        if (startTime.isAfter(MINTIME) && endTime.isBefore(MAXTIME) && !startTime.isAfter(endTime)) {
            trimmed.removeIf(
                    event -> (
                            getTimeStamp(event).isBefore(startTime) ||
                            getTimeStamp(event).isAfter(endTime)
                    )
            );
        }
        else {
            trimmed = null;
        }

        return trimmed;
    }

    /***
     * Returns the list of flat records filtered by filter providers
     * @param filter Map of filters as attribute name = allowed value. Value сan be regex
     * @return map of traces
     */
    public List<FlatXTrace> getFullTraceList(Map<FilterType, Object> filter) {
        List<FlatXTrace> traces = Lists.newArrayList();

        for(XTrace current: xlog) {
            if (filterMatch(filter, current)) {
                FlatXTrace flatXTrace = new FlatXTrace(current);
                traces.add(flatXTrace);
            }
        }

        return traces;
    }

    /***
     * Return event attribute for trace. "NA" if no org:resource attributes in any event, "MULTI" if multiple resources
     * @param xTrace trace to look into
     * @param attribute name attribute to receive
     * @param eventName event name to check
     * @return attribute value
     */
    static public String getTraceResource(XTrace xTrace, String attribute, String eventName) {
        String resource = "NA";

        Iterator eventIterator = xTrace.iterator();
        while (eventIterator.hasNext()) {
            XEvent xEvent = (XEvent) eventIterator.next();
            if ( eventName != null && !getAttribute(xEvent, "concept:name").equals(eventIterator)) continue;

            String current = (String)getAttribute(xEvent, attribute);
            if ( current == null ) continue;
            else if ( resource.equals("NA") && !current.equals(resource)) resource = current;
            else if ( !current.equals(resource) ) {
                resource = "MULTI";
                break;
            }

        }

        return resource;
    }

    /***
     * Search for trace by it's concept:name
     * @param name name to search for
     * @return trace of null if not found
     */
    public XTrace getXTrace(String name) {
        XTrace xTrace = null;
        int i = 0;

        if(xlog.size() > 0) {

            if (name2index.containsKey(name))
                xTrace = xlog.get(name2index.get(name));
            else {
                for (XTrace currentTrace: xlog) {
                    if (getConceptName(currentTrace).equals(name)) {
                        xTrace = currentTrace;
                        name2index.put(name, i);
                        break;
                    }

                    i++;
                }
            }
        }

        return xTrace;
    }

    /***
     * Get attribute value
     * @param object object with attributes (event or trace)
     * @param name attribute name
     * @return attribute value
     */
    static public Object getAttribute(XAttributable object, String name) {
        return getAttribute(object, name, null);
    }

    /***
     * Get attribute value
     * @param object object with attributes (event or trace)
     * @param name attribute name
     * @param defaultValue value if attribute is missing
     * @return attribute value
     */
    static public Object getAttribute(XAttributable object, String name, Object defaultValue) {
        Object value = null;

        if (object.hasAttributes()) {
            XAttributeMap xAttributeMap = object.getAttributes();
            XAttribute attribute = null;

            if (xAttributeMap.containsKey(name)) {
                attribute = xAttributeMap.get(name);
            }
            else {
                for (XAttribute current : xAttributeMap.values()) {
                    if (current.getKey().equals(name)) {
                        attribute = current;
                        break;
                    }
                }
            }

            if (attribute != null) {
                if (XAttributeLiteral.class.isInstance(attribute)) {
                    value = ((XAttributeLiteral) attribute).getValue();
                }
                else if (attribute.getKey().equals(XTimeExtension.KEY_TIMESTAMP)) {
                    value = ZonedDateTime.ofInstant(((XAttributeTimestamp) attribute).getValue().toInstant(), ZoneId.of("UTC"));
                }
            }
            else {
                value = defaultValue;
            }
        }

        return value;
    }

    /***
     * Calculate average share of event duration in trace on whole log
     * @param filter log filter to apply
     * @param useMedian mode of calculation - mean or median (better if skewed)
     * @return map of event durations
     */
    public Map<String, Double> eventDurationShares(Map<FilterType, Object> filter, boolean useMedian) {
        Map<String, Double> shares = Maps.newHashMap();
        Map<String, DescriptiveStatistics> buffer = Maps.newHashMap();

        for(XTrace xTrace: xlog) {
            if (filterMatch(filter, xTrace)) {
                Map<String, Double> traceShares = eventSharesInTrace(xTrace);
                for (Map.Entry<String, Double> entry : traceShares.entrySet()) {
                    DescriptiveStatistics current = buffer.getOrDefault(entry.getKey(), new DescriptiveStatistics());
                    current.addValue(entry.getValue());
                    buffer.put(
                            entry.getKey(),
                            current
                    );
                }
            }
        }

        // average share
        for (Map.Entry<String, DescriptiveStatistics> entry: buffer.entrySet()) {
            if (useMedian)
                shares.put(entry.getKey(), entry.getValue().getPercentile(50));
            else
                shares.put(entry.getKey(), entry.getValue().getMean());
        }

        return shares;
    }

    /***
     * Calculate event class(unique name) share of trace duration
     * @param xTrace object to process
     * @return map with event name and share
     */
    static public Map<String, Double> eventSharesInTrace(XTrace xTrace) {
        Map<String, Double> shares = eventDurationInTrace(xTrace);

        // divide by total trace duration
        double duration = (double) getTraceDuration(xTrace);
        shares.replaceAll((key, value) -> value / duration);

        return shares;
    }

    /***
     * Calculate event class(unique name) duration in trace
     * @param xTrace object to process
     * @return map with event name and duration
     */
    static public Map<String, Double> eventDurationInTrace(XTrace xTrace) {
        Map<String, Double> durations = Maps.newHashMap();
        Map<XEvent, ZonedDateTime> buffer = Maps.newHashMap();

        if (xTrace.size() > 0) {
            buffer = calculateEventsEndTime(xTrace, 0L);

            // calculate total event durations
            for(Map.Entry<XEvent, ZonedDateTime> entry: buffer.entrySet()) {
                if (getTimeStamp(entry.getKey()).isBefore(entry.getValue())) {
                    durations.put(getConceptName(entry.getKey()),
                            durations.getOrDefault(getConceptName(entry.getKey()), 0D) +
                                    Duration.between(getTimeStamp(entry.getKey()), entry.getValue()).getSeconds());
                }
            }
        }
        return durations;
    }

    /***
     * Get event timestamp or MIN time of not present
     * @param xEvent to process
     * @return Zoned Time Date
     */
    static public ZonedDateTime getTimeStamp(XEvent xEvent) {
        ZonedDateTime stamp = MINTIME;

        if (xEvent.getAttributes().containsKey(XTimeExtension.KEY_TIMESTAMP)) {
            stamp = ZonedDateTime.ofInstant(XTimeExtension.instance().extractTimestamp(xEvent).toInstant(), ZoneId.of("UTC"));
        }

        return stamp;
    }

    /***
     * Calculate workload table (resource name, time, workload in seconds)
     * @param filter
     * @return
     */
    public List<Workload> calculateResourceWorkload(Map <FilterType, Object> filter) {
        Table<String, ZonedDateTime, Workload> workloads = HashBasedTable.create();

        // TODO extend hourly granularity to other levels
        for (XTrace xTrace: xlog) {
            if (filterMatch(filter, xTrace)) {
                // calculate event durations in trace
                Map<XEvent, ZonedDateTime> events = calculateEventsEndTime(xTrace, EVENT_DEFAULT_DURATION);

                for(Map.Entry<XEvent, ZonedDateTime> entry: events.entrySet()) {

                    String resource = (String)getAttribute(entry.getKey(), XOrganizationalExtension.KEY_RESOURCE);
                    if (resource == null ) resource = "NA";

                    String role = (String)getAttribute(entry.getKey(), XOrganizationalExtension.KEY_ROLE);
                    if (role == null ) role = "NA";

                    String group = (String)getAttribute(entry.getKey(), XOrganizationalExtension.KEY_GROUP);
                    if (group == null ) group = "NA";

                    ZonedDateTime stamp = getTimeStamp(entry.getKey());
                    Duration duration = Duration.between(stamp, entry.getValue());

                    // first hour
                    ZonedDateTime hour = getTimeStamp(entry.getKey()).withMinute(0).withSecond(0);
                    Workload workload;
                    if (workloads.contains(resource, hour))
                        workload = workloads.get(resource, hour);
                    else
                        workload = new Workload(resource, role, group, hour, 0L);

                    int firstHour = 3600 - stamp.getMinute()*60 - stamp.getSecond();
                    workload.setWorkload(workload.getWorkload() + Math.min(duration.getSeconds(), firstHour ));
                    workloads.put(resource, hour, workload);

                    duration = duration.minusSeconds(firstHour);

                    while (duration.getSeconds() > 0) {
                        hour = hour.plusHours(1);
                        if (workloads.contains(resource, hour))
                            workload = workloads.get(resource, hour);
                        else
                            workload = new Workload(resource, role, group, hour, 0L);

                        workload.setWorkload(workload.getWorkload() + Math.min(3600, duration.getSeconds() ));
                        workloads.put(resource, hour, workload);

                        duration = duration.minusSeconds(3600);
                    }
                }
            }
        }

        return Lists.newArrayList(workloads.values());
    }

    public List<FlatXEvent> getEventList(Map<FilterType, Object> filter) {
        List<FlatXEvent> events = Lists.newArrayList();

        for (XTrace xTrace: xlog) {
            if (filterMatch(filter, xTrace)) {
                Map<XEvent, ZonedDateTime> eventList = calculateEventsEndTime(xTrace, EVENT_DEFAULT_DURATION);
                for(Map.Entry<XEvent, ZonedDateTime> entry: eventList.entrySet()) {
                    FlatXEvent event = new FlatXEvent();
                    event.setResource((String)getAttribute(entry.getKey(), XOrganizationalExtension.KEY_RESOURCE, "NA"));
                    event.setRole((String)getAttribute(entry.getKey(), XOrganizationalExtension.KEY_ROLE, "NA"));
                    event.setGroup((String)getAttribute(entry.getKey(), XOrganizationalExtension.KEY_GROUP, "NA"));
                    event.setName(getConceptName(entry.getKey()));
                    event.setTrace(getConceptName(xTrace));
                    event.setStart(getTimeStamp(entry.getKey()));
                    event.setEnd(entry.getValue());

                    events.add(event);
                }
            }
        }

        return events;
    }

    /* Private functions */

    /***
     * Clear all cache maps
     */
    private void clearCache() {
        // clear cache
        name2index.clear();
    }

    /***
     * Apply filter to the trace. ALL condition should match. empty filter always tru
     * @param filter list of conditions
     * @param xTrace trace to check
     * @return true if trace match conditions
     */
    @SuppressWarnings("unchecked")
    static private boolean filterMatch(Map<FilterType, Object> filter, XTrace xTrace) {
        boolean verdict = true;
        if (filter != null && filter.size() > 0) {

            for (Map.Entry<FilterType, Object> rule : filter.entrySet()) {
                if (rule.getValue() == null) continue;

                if (rule.getKey().equals(FilterType.TRACE_START_RANGE)) {
                    if (! ((Range)rule.getValue()).contains(traceStartTime(xTrace)) ) {
                        verdict = false;
                        break;
                    }
                }
                if (rule.getKey().equals(FilterType.TRACE_END_RANGE)) {
                    if (! ((Range)rule.getValue()).contains(traceEndTime(xTrace)) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.EVENT_COUNT_RANGE)) {
                    if (! ((Range)rule.getValue()).contains(xTrace.size()) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.RESOURCE_LIST)) {
                    if (! ((List<String>)rule.getValue()).contains(getTraceResource(xTrace, XOrganizationalExtension.KEY_RESOURCE, null)) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.ROLE_LIST)) {
                    if (! ((List<String>)rule.getValue()).contains(getTraceResource(xTrace, XOrganizationalExtension.KEY_ROLE, null)) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.GROUP_LIST)) {
                    if (! ((List<String>)rule.getValue()).contains(getTraceResource(xTrace, XOrganizationalExtension.KEY_GROUP, null)) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.LIFECYCLE_TRANSITION_LIST)) {
                    if (! ((List<String>)rule.getValue()).contains(getTraceResource(xTrace, XLifecycleExtension.KEY_TRANSITION, null)) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.TRACE_NAME_LIST)) {
                    if (! ((List<String>)rule.getValue()).contains(getConceptName(xTrace)) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.EVENT_NAME_LIST)) {
                    verdict = false;
                    for(XEvent event: xTrace) {
                        if (((List<String>) rule.getValue()).contains(getConceptName(event))) {
                            verdict = true;
                            break;
                        }
                    }
                }
                else if (rule.getKey().equals(FilterType.TRACE_START_WEEKDAY_LIST)) {
                    if (! ((List<DayOfWeek>)rule.getValue()).contains(traceStartTime(xTrace).getDayOfWeek()) ) {
                        verdict = false;
                        break;
                    }
                }
                else if (rule.getKey().equals(FilterType.TRACE_END_WEEKDAY_LIST)) {
                    if (! ((List<DayOfWeek>)rule.getValue()).contains(traceEndTime(xTrace).getDayOfWeek()) ) {
                        verdict = false;
                        break;
                    }
                }
            }

        }
        return verdict;
    }


    /***
     * Determine end time for events in trace based on lifecycle or event sequence. Last event in trace
     * (without lifecycle) will have fixed duration
     * @param xTrace trace with events
     * @param defaultDuration default last event duration in seconds
     * @return map of event and event end time
     */
    static private Map<XEvent, ZonedDateTime> calculateEventsEndTime(XTrace xTrace, Long defaultDuration) {
        Map<XEvent, ZonedDateTime> buffer = Maps.newHashMap();

        // let sort events first
        Collections.sort(xTrace,
                (e1, e2) -> XTimeExtension.instance().extractTimestamp(e1)
                        .compareTo(XTimeExtension.instance().extractTimestamp(e2)));

        XEvent lastEvent = null;

        for (XEvent xEvent: xTrace) {
            if (
                    lastEvent != null &&
                    (
                            !lastEvent.getAttributes().containsKey(XLifecycleExtension.KEY_TRANSITION) ||
                            XLifecycleExtension.instance().extractStandardTransition(lastEvent) == XLifecycleExtension.StandardModel.COMPLETE
                    )
            ) {
                buffer.put(lastEvent, getTimeStamp(xEvent));
            }

            lastEvent = xEvent;

            if (xEvent.getAttributes().containsKey(XLifecycleExtension.KEY_TRANSITION)) {
                if (XLifecycleExtension.instance().extractStandardTransition(xEvent) == XLifecycleExtension.StandardModel.START)
                {
                    buffer.put(xEvent, MINTIME);
                }
                else if (
                        XLifecycleExtension.instance().extractStandardTransition(xEvent) == XLifecycleExtension.StandardModel.COMPLETE
                        ) {
                    // we only support START and COMPLETE transitions
                    // go back and update open event. We assume no nested events available, nor same event can run in parallel
                    // TODO improve code to accepts mentioned cases
                    String name = getConceptName(xEvent);
                    boolean success = false;
                    for (XEvent key: buffer.keySet()) {
                        if (
                                getConceptName(key).equals(name) &&
                                XLifecycleExtension.instance().extractStandardTransition(key) == XLifecycleExtension.StandardModel.START &&
                                buffer.get(key).equals(MINTIME)
                            ) {
                            buffer.put(key, getTimeStamp(xEvent));
                            success = true;
                            break;
                        }
                    }

                    if (! success ) {
                        buffer.put(xEvent, MINTIME);
                    }
                    else {
                        lastEvent = null; // skip complete event
                    }
                }

            }
            else {
                buffer.put(xEvent, MINTIME);
            }

        }

        if (defaultDuration > 0 ) {
            for (Map.Entry<XEvent, ZonedDateTime> entry: buffer.entrySet()) {
                if (entry.getValue().equals(MINTIME))
                    buffer.put(entry.getKey(), getTimeStamp(entry.getKey()).plusSeconds(defaultDuration));
            }
        }

        return buffer;
    }
}
