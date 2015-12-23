package org.processmining.xestools;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

/**
 * Created by nsitnikov on 22/12/15.
 */
@Getter
@Setter
@NoArgsConstructor
public class FlatXEvent {
    private String trace;
    private String name;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private String resource;
    private String role;
    private String group;
}
