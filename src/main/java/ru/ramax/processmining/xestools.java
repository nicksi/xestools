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
import java.util.Map;

/**
 * Created by nsitnikov on 16/12/15.
 */
public class XEStools {

    @Getter
    private XLog xlog;

    // cache for traceStartTime
    private Map<String, LocalDateTime> traceStartTime = Maps.newHashMap();

    public XEStools(@NonNull XLog xlog) {
        this.xlog = xlog;

    }

    public void setXLog(@NonNull XLog xLog) {
        this.xlog = xLog;
        clearCache();
    }

    /***
     * Find the start timestamp of the log
     * @param xTrace
     * @return LocalDateTime
     */
    public LocalDateTime traceStartTime(@NonNull XTrace xTrace) {
        LocalDateTime startTime = LocalDateTime.MIN;

        String index = getIndex(xTrace);
        if ( traceStartTime.containsKey(index) ) {
            startTime = traceStartTime.get(index);
        }
        else {
            if (xTrace.size() > 0) {
                XEvent xEvent = xTrace.get(0);
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

        return startTime;
    }

    public LocalDateTime traceStartTime(@NonNull String index) {
        LocalDateTime startTime = LocalDateTime.MIN;

        Iterator xTraceIterator = xlog.iterator();
        while (xTraceIterator.hasNext()) {
            XTrace xTrace = (XTrace) xTraceIterator.next();

            if (getIndex(xTrace).equals(index)) {
                startTime = traceStartTime(xTrace);
                break;
            }

        }

        return startTime;
    }

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
