package ru.ramax.processmining;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

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
}
