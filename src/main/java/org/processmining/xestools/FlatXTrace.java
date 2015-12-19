package org.processmining.xestools;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.deckfour.xes.model.XTrace;

import java.time.ZonedDateTime;

import static org.processmining.xestools.XEStools.*;

/**
 * Created by Nikolai Sitnikov on 17.12.15.
 * Class to store flat trace to be used by calling party
 */

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class FlatXTrace {
    private String conceptName;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String orgResource;
    private String orgRole;

    // calculatables
    private int eventCount;
    private long duration;
    private int eventRepetitions;

    public FlatXTrace(XTrace current) {
        this.conceptName = ((String)getAttribute(current, "concept:name"));
        this.duration = getTraceDuration(current);
        this.startTime = traceStartTime(current);
        this.endTime = traceEndTime(current);
        this.eventCount = current.size();
        this.orgResource = getTraceResource(current, "org:resource", null);
        this.orgRole = getTraceResource(current, "org:role", null);
        this.eventRepetitions = 0;
    }
}
