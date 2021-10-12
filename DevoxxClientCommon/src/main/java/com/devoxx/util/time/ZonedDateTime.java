/**
 * Copyright (c) 2016, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.devoxx.util.time;

import java.time.ZoneId;
import java.util.Locale;

public class ZonedDateTime {

    private static final ZoneId DEFAULT_CONFERENCE_ZONE_ID = ZoneId.of("Europe/Brussels");

//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    public static java.time.ZonedDateTime ofDate(String date, ZoneId zoneId) {
//        return LocalDate.parse(fromDate, DATE_FORMATTER).atStartOfDay(timezoneId);
        if (date == null || date.isEmpty()) {
            return null;
        }

        String[] d = date.split("-");
        if (d.length < 2) {
            return null;
        }
        try {
            int year = Integer.parseInt(d[0]);
            int month = Integer.parseInt(d[1]);
            int day = Integer.parseInt(d[2]);
            return java.time.ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zoneId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatToDate(java.time.ZonedDateTime zonedDateTime) {
//         DateTimeFormatter formatter = DevoxxSettings.DATE_FORMATTER;
//        return formatter.format(zonedDateTime);

        return "" + camelCase(zonedDateTime.getDayOfWeek().name()) + ", " +
                camelCase(zonedDateTime.getMonth().name()) + " " +
                zonedDateTime.getDayOfMonth() + ", " + zonedDateTime.getYear();
    }

    public static String formatToConferenceDate(String fromDate, String endDate) {
        java.time.ZonedDateTime fromDateTime = ofDate(fromDate, DEFAULT_CONFERENCE_ZONE_ID);
        java.time.ZonedDateTime endDateTime = ofDate(endDate, DEFAULT_CONFERENCE_ZONE_ID);
        if (fromDateTime == null || endDateTime == null) {
            return "";
        }

        if (fromDateTime.equals(endDateTime)) {
//            return LocalDate.parse(fromDateTime).format(DATE_TIME_FORMATTER);
            return "" + camelCase(fromDateTime.getMonth().name()) + " " +
                    String.format("%02d", fromDateTime.getDayOfMonth()) + ", " + fromDateTime.getYear();
        } else {
//            return LocalDate.parse(item.getFromDate()).getDayOfMonth() + " - " +
//                        LocalDate.parse(item.getEndDate()).format(DATE_TIME_FORMATTER);

            return "" + String.format("%02d", fromDateTime.getDayOfMonth()) + " - " +
                    camelCase(endDateTime.getMonth().name()) + " " +
                    String.format("%02d", endDateTime.getDayOfMonth()) + ", " + endDateTime.getYear();
        }
    }

    //    private static final String TIME_PATTERN = "h:mma";
    //    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN, LOCALE);


    public static String ofTime(java.time.ZonedDateTime dateTime) {
//        return DevoxxSettings.TIME_FORMATTER.format(dateTime);
        if (dateTime == null) {
            return null;
        }
        int hour = dateTime.getHour();
        if (hour < 12) {
            return "" + hour + ":" + String.format("%02d", dateTime.getMinute()) + "AM";
        } else if (hour == 12) {
            return "" + hour + ":" + String.format("%02d", dateTime.getMinute()) + "PM";
        } else {
            return "" + (hour - 12) + ":" + String.format("%02d", dateTime.getMinute()) + "PM";
        }
    }

    private static String camelCase(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
