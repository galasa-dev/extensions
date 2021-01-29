/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2021.
 */
package dev.galasa.eclipse.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateConverter {

    private final static ZoneId            zoneid = ZoneId.systemDefault();
    private final static DateTimeFormatter dtf    = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);

    public static String visualDate(Instant instant) {
        return visualDate(ZonedDateTime.ofInstant(instant, zoneid));
    }

    public static String visualDate(ZonedDateTime zonedDateTime) {
        ZonedDateTime zdt = ZonedDateTime.now(zoneid);

        if (zdt.toLocalDate().equals(zonedDateTime.toLocalDate())) {
            return "Today, " + zonedDateTime.toLocalTime().toString();
        }

        zdt = zdt.minusDays(1);

        if (zdt.toLocalDate().equals(zonedDateTime.toLocalDate())) {
            return "Yesterday, " + zonedDateTime.toLocalTime().toString();
        }

        return zonedDateTime.format(dtf);
    }

}
