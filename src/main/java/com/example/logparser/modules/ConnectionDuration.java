package com.example.logparser.modules;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class ConnectionDuration {
    private ZonedDateTime start;
    private ZonedDateTime end;
    private long duration;
    private final DateTimeFormatter formatter;

    public ConnectionDuration(String timePattern,String stringDate) {
        this.formatter = DateTimeFormatter.ofPattern(timePattern);
        compareThisDate(stringDate);
    }

    public long getDuration() {
        calculateDuration();
        return duration;
    }

    public void calculateDuration() {
        this.duration = timeDifference(this.start,this.end);
    }

    private long timeDifference(ZonedDateTime start, ZonedDateTime end) {
        if (start == null || end == null) {
            return -1;
        }
        return ChronoUnit.MILLIS.between(start,end);
    }

    public void compareThisDate(String stringDate) {
        try {
            ZonedDateTime compareDate = ZonedDateTime.parse(stringDate,formatter);

            if (start == null) {
                start = compareDate;
                end = compareDate;
            } else {
                if (compareDate.isBefore(start)) {
                    start = compareDate;
                } else if (compareDate.isAfter(end)) {
                    end = compareDate;
                }
            }
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            System.out.println("The value was: " + stringDate);
        }

    }
}
