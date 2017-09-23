/**
 * Copyright (c) 2016, 2017, Gluon Software
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
package com.devoxx.util;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class DevoxxSettings {

    /**
     * boolean option to switch on/off automatic authentication by using a self generated random UUID
     * - true: authentication is performed by generating a unique random UUID for the device. the id will
     *  be persisted and re-used during the installed lifetime of the application. once the app is removed
     *  the id will be gone as well.
     * - false: authentication is done with user interaction.
     */
    public final static boolean AUTO_AUTHENTICATION = false;

    /**
     * boolean option to switch on/off the remote notes and badges
     * - true: notes and badges require authentication and are persisted locally 
     * and on the cloud
     * - false: notes and badges don't require authentication and are persisted 
     * only locally
     * Default: true
     */
    public final static boolean USE_REMOTE_NOTES = true;

    /**
     * boolean option to switch on/off voting
     * Default: false
     */
    public final static boolean VOTING_TESTS = false;

    /**
     * boolean option to switch on/off local notification tests
     * Default: false
     */
    public final static boolean NOTIFICATION_TESTS = false;

    /**
     * Offset in seconds:
     *  12 days from Tue 6th Sep to 18th
     * 
     * A session scheduled for Sun 18th, 11:45 AM - 12:30PM will be notified:
     * - US (Florida) +3h: Tue 6th, at 2:30 PM (start) and 3.28 PM (vote)
     * - Portugal +8h: Tue 6th, at 7:30 PM (start) and 8.28 PM (vote)
     * - Belgium, Spain +9h: Tue 6th, at 8:30 PM (start) and 9.28 PM (vote)
     * - India +12 hours 30 minutes: Wed 7th, at 00:00 AM (start) and 00.58 AM (vote)
     * - New Zealand +19h: Wed 7th, at 6:30 AM (start) and 7.28 AM (vote)
     * 
     * Window for testing: between Tuesday 6th and Saturday 10th
     */
    public final static long NOTIFICATION_OFFSET = 12 * 24 * 60 * 60;

    /**
     * Timeout in seconds to stop any order from the different experiences
     * that hasn't finished yet
     */
    public final static int PROCESSING_TIME_OUT = 15; // seconds

    public final static Locale LOCALE = Locale.getDefault();
    public static final boolean FAV_AND_SCHEDULE_ENABLED = true;
    public static final String SKIP_VIDEO = "SKIP_VIDEO";
    public static final String SKIP_SCH_FAV_DIALOG = "SKIP_SCH_FAV_DIALOG";
    public static final String SIGN_UP = "sign_up";
    public static final String SAVED_CONFERENCE_ID = "devoxx_cfp_id";
    public static final String SAVED_ACCOUNT_ID = "devoxx_cfp_account";
    public static final String SWITCH_TO_NEWS = "SWITCH_TO_NEWS";

    private static final String WEARABLE_DAY_PATTERN  = "MMMM dd, uuuu";
    private static final String TIME_PATTERN = "h:mma";
    private static final String NEWS_PATTERN  = "EEEE, h:mma";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
    public static final DateTimeFormatter WEARABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern(WEARABLE_DAY_PATTERN, LOCALE);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN, LOCALE);
    public static final DateTimeFormatter NEWS_FORMATTER = DateTimeFormatter.ofPattern(NEWS_PATTERN, LOCALE);

    public static final String TWITTER_URL = "https://www.twitter.com/";

    private static String uuid;
    public static String getUserUUID() {
        uuid = Services.get(SettingsService.class)
                .map(s -> s.retrieve("UUID"))
                .orElse(null);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            Services.get(SettingsService.class).ifPresent(s -> s.store("UUID", uuid));
        }
        return uuid;
    }

    private  static final String LAST_VOTE_CAST = "LAST_VOTE_CAST";

    private static Long lastVoteCast = null;
    public static long getLastVoteCast() {
        if (lastVoteCast == null) {
            lastVoteCast = Services.get(SettingsService.class)
                    .flatMap(s -> Optional.ofNullable(s.retrieve(LAST_VOTE_CAST)))
                    .map(Long::parseLong)
                    .orElse(0L);
        }
        return lastVoteCast;
    }

    public static void setLastVoteCast(long updatedLastVoteCast) {
        lastVoteCast = updatedLastVoteCast;
        Services.get(SettingsService.class).ifPresent(s -> s.store(LAST_VOTE_CAST, String.valueOf(lastVoteCast)));
    }
}
