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
package com.devoxx;

import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.*;
import com.gluonhq.charm.glisten.afterburner.AppView;
import com.gluonhq.charm.glisten.afterburner.AppViewRegistry;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;

import java.util.Locale;

import static com.gluonhq.charm.glisten.afterburner.AppView.Flag.*;

public class DevoxxView {

    static final AppViewRegistry REGISTRY = new AppViewRegistry();

    public static final AppView NEWS            = view( NewsPresenter.class,           MaterialDesignIcon.SMS,                SHOW_IN_DRAWER);
    public static final AppView SESSIONS        = view( SessionsPresenter.class,       MaterialDesignIcon.DASHBOARD,          SHOW_IN_DRAWER);
    public static final AppView SESSION         = view( SessionPresenter.class,        MaterialDesignIcon.RECORD_VOICE_OVER);
    public static final AppView SPEAKERS        = view( SpeakersPresenter.class,       MaterialDesignIcon.SPEAKER,            SHOW_IN_DRAWER);
    public static final AppView SPEAKER         = view( SpeakerPresenter.class,        MaterialDesignIcon.SPEAKER);
    public static final AppView EXHIBITORS      = view( ExhibitorsPresenter.class,     MaterialDesignIcon.PLACE/*,              SHOW_IN_DRAWER*/);
    public static final AppView EXHIBITOR       = view( ExhibitorPresenter.class,      MaterialDesignIcon.PLACE);
    public static final AppView EXHIBITION_MAPS = view( ExhibitionMapsPresenter.class, MaterialDesignIcon.MAP,                SHOW_IN_DRAWER);
    public static final AppView EXHIBITION_MAP  = view( ExhibitionMapPresenter.class,  MaterialDesignIcon.MAP);
    public static final AppView SPONSORS        = view( SponsorsPresenter.class,       MaterialDesignIcon.PAYMENT/*,            SHOW_IN_DRAWER*/);
    public static final AppView SPONSOR         = view( SponsorPresenter.class,        MaterialDesignIcon.PAYMENT);
    public static final AppView VENUE           = view( VenuePresenter.class,          MaterialDesignIcon.ACCESSIBILITY,      SHOW_IN_DRAWER);
    public static final AppView NOTES           = view( NotesPresenter.class,          MaterialDesignIcon.EVENT_NOTE,         SHOW_IN_DRAWER);
    public static final AppView BADGES          = view( BadgesPresenter.class,         MaterialDesignIcon.CONTACTS/*,            SHOW_IN_DRAWER*/);
    public static final AppView BADGE           = view( BadgePresenter.class,          MaterialDesignIcon.CONTACTS);
    public static final AppView SEARCH          = view( SearchPresenter.class,         MaterialDesignIcon.SEARCH);
    public static final AppView ABOUT           = view( AboutPresenter.class,          MaterialDesignIcon.AC_UNIT,            SHOW_IN_DRAWER);
    public static final AppView CONF_SELECTOR   = view( ConfSelectorPresenter.class,   MaterialDesignIcon.SELECT_ALL,         SHOW_IN_DRAWER, HOME_VIEW, SKIP_VIEW_STACK);

    private static AppView view(Class<? extends GluonPresenter<?>> presenterClass, MaterialDesignIcon menuIcon, AppView.Flag... flags ) {
        return REGISTRY.createView( name(presenterClass),
                                    DevoxxBundle.getString( "OTN.VIEW." + name(presenterClass)),
                                    presenterClass,
                                    menuIcon,
                                    flags);
    }

    private static String name(Class<? extends GluonPresenter<?>> presenterClass) {
        return presenterClass.getSimpleName().toUpperCase(Locale.ROOT).replace("PRESENTER", "");
    }

}
