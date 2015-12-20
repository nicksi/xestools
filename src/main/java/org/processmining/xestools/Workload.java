package org.processmining.xestools;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

/**
 * Created by nsitnikov on 20/12/15.
 * Class to store workload calculation results
 */
@Getter
@Setter
@NoArgsConstructor
public class Workload {
    private String resource;
    private String role;
    private String group;
    private ZonedDateTime timestamp;
    private Long workload; //in seconds

    public Workload (String resource, String role, String group, ZonedDateTime hour, Long workload) {
        this.resource = resource;
        this.role = role;
        this.group = group;
        this.timestamp = hour;
        this.workload = workload;
    }
}
