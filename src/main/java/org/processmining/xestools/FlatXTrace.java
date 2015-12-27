package org.processmining.xestools;

import lombok.*;
import org.deckfour.xes.model.XTrace;

import java.time.ZonedDateTime;

import static org.processmining.log.utils.XUtils.getConceptName;
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
    private String name;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String orgResource;
    private String orgRole;
    private String orgGroup;

    // calculable
    private int eventCount;
    private long duration;
    private int eventRepetitions;

    public FlatXTrace(@NonNull XTrace xTrace) {
        this.name =  getConceptName(xTrace);
        this.duration = getTraceDuration(xTrace);
        this.startTime = traceStartTime(xTrace);
        this.endTime = traceEndTime(xTrace);
        this.eventCount = xTrace.size();
        this.orgResource = getTraceResource(xTrace, "org:resource", null);
        this.orgRole = getTraceResource(xTrace, "org:role", null);
        this.orgGroup = getTraceResource(xTrace, "org:group", null);
        this.eventRepetitions = 0;
    }
}
