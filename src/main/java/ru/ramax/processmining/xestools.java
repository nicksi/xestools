package ru.ramax.processmining;

import com.google.common.collect.Maps;
import com.sun.org.apache.xpath.internal.objects.XObject;
import lombok.Getter;
import lombok.NonNull;
import org.deckfour.xes.model.*;
import org.deckfour.xes.model.buffered.XTraceIterator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by nsitnikov on 16/12/15.
 */
public class XEStools {

    @Getter
    private XLog xlog;

    // cache for traceStartTime
    private Map<String, LocalDateTime> traceStartTime = Maps.newHashMap();
    // cache to search for trace by concept:name
    private Map<String, Integer> name2index = Maps.newHashMap();

    public XEStools(@NonNull XLog xlog) {
        this.xlog = xlog;

    }

    /***
     * Select log to work with and clear cache
     * @param xLog
     */
    public void setXLog(@NonNull XLog xLog) {
        this.xlog = xLog;
        clearCache();
    }

    /***
     * Find the start timestamp of the trace
     * @param xTrace
     * @return LocalDateTime
     */
    public LocalDateTime traceStartTime(@NonNull XTrace xTrace, String eventName) {
        LocalDateTime startTime = LocalDateTime.MIN;

        String index = getIndex(xTrace);
        if ( traceStartTime.containsKey(index) ) {
            startTime = traceStartTime.get(index);
        }
        else {
            if (xTrace.size() > 0) {
                ListIterator events = xTrace.listIterator();
                while (events.hasNext()) {
                    XEvent xEvent = xTrace.get(0); // TODO switch to minimal date time, not first
                    if (xEvent != null && xEvent.hasAttributes()) {
                        XAttributeMap xAttributeMap = xEvent.getAttributes();
                        for (XAttribute attribute : xAttributeMap.values()) {
                            if (attribute.getKey().equals("time:timestamp")) {
                                startTime = LocalDateTime.ofEpochSecond(((XAttributeTimestamp) attribute).getValueMillis() / 1000, 0, ZoneOffset.ofHours(0));
                                traceStartTime.put(index, startTime);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return startTime;
    }

    /***
     * Find the start timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @return
     */
    public LocalDateTime traceStartTime(@NonNull String index) {
        LocalDateTime startTime = LocalDateTime.MIN;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            startTime = traceStartTime(xTrace);
        }

        return startTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param xTrace
     * @return
     */
    public LocalDateTime traceEndTime(@NonNull XTrace xTrace) {
        LocalDateTime endTime = LocalDateTime.MAX;

        return  endTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @return
     */
    public LocalDateTime traceEndTime(@NonNull String index) {
        LocalDateTime endTime = LocalDateTime.MAX;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            endTime = traceEndTime(xTrace);
        }

        return endTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param xTrace
     * @return
     */
    public LocalDateTime traceEndTime(@NonNull XTrace xTrace, String eventName) {
        LocalDateTime endTime = LocalDateTime.MAX;

        return  endTime;
    }

    /***
     * Find the last timestamp of the trace based on trace concept:name
     * @param index Concept name to find
     * @return
     */
    public LocalDateTime traceEndTime(@NonNull String index, String eventName) {
        LocalDateTime endTime = LocalDateTime.MAX;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            endTime = traceEndTime(xTrace);
        }

        return endTime;
    }

    /***
     * Find the duration of trace
     * @param xTrace
     * @return
     */
    public Long getTraceDuration(@NonNull XTrace xTrace) {
        Long duration = 0L;

        LocalDateTime start = traceEndTime(xTrace);
        LocalDateTime end = traceStartTime(xTrace);

        return duration;
    }


    /***
     * Find the duration of trace based on trace concept:name
     * @param index Concept name to find trace
     * @return
     */
    public Long getTraceDuration(@NonNull String index) {
        Long duration = 0L;

        XTrace xTrace = getXTrace(index);
        if (xTrace != null)
        {
            duration = getTraceDuration(xTrace);
        }

        return duration;
    }

    public Map<String, Long> getTraceDurations() {

    }

    public Map<String, Long> getTraceDurationsWithResource(String filter) {

    }

    public Map<String, Long> getTraceDurationsWithResource(String filter) {

    }

    public Map<String, Long> getTraceDurationsWithResource(String filter) {

    }


    /***
     * Search for trace by it's concept:name
     * @param name
     * @return
     */
    public XTrace getXTrace(String name) {
        XTrace xTrace = null;
        int i = 0;

        if (name2index.containsKey(name))
            xTrace = xlog.get(name2index.get(name));
        else {
            Iterator xTraceIterator = xlog.iterator();
            while (xTraceIterator.hasNext()) {
                XTrace currentTrace = (XTrace) xTraceIterator.next();

                if (getIndex(currentTrace).equals(name)) {
                    // TODO not first but minimal
                    xTrace = currentTrace;
                    name2index.put(name, i);
                    break;
                }

                i++;
            }
        }

        return xTrace;
    }

    /* Private functions */

    /***
     * Clear all cache maps
     */
    private void clearCache() {
        // clear cache
        traceStartTime.clear();
    }

    private String getIndex(XAttributable object) {
        String index = "";

        if (XTrace.class.isInstance(object) || XEvent.class.isInstance(object)) {
            XAttributeMap xAttributeMap = object.getAttributes();

            for (XAttribute attribute: xAttributeMap.values()) {
                if (attribute.getKey().equals("concept:name")) {
                    index = ((XAttributeLiteral)attribute).getValue();
                    break;
                }
            }
        }

        return index;
    }
}
