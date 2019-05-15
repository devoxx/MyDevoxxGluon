/**
 * Copyright (c) 2019, Gluon Software
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

import com.devoxx.model.Floor;
import com.devoxx.model.SessionType;
import com.devoxx.model.Talk;

public class JBcnBundle {

	/**
	 * Returns i18n version of the track name depending on the default locale
	 * 
	 * @param trackName
	 *            track name to internationalize
	 * @return i18n version of the track name depending on the default locale
	 */
	public static String getTrackName(String trackName) {
		return getI18NValue("OTN.SESSION." + trackName.replaceAll(" ", "").toUpperCase(), trackName);
	}

	/**
	 * Returns i18n version of the talk type depending on the default locale
	 * 
	 * @param talk
	 *            talk
	 * @return i18n version of the talk type depending on the default locale
	 */
	public static String getTalkType(Talk talk) {
		return getI18NValue("OTN.SESSION.TALK_TYPE." + talk.getTalkType().toUpperCase(), talk.getTalkType());
	}

	/**
	 * Returns i18n version of the session type depending on the default locale
	 * 
	 * @param sessionType
	 *            session type
	 * @return i18n version of the session type depending on the default locale
	 */
	public static String getSessionType(SessionType sessionType) {
		return getI18NValue("OTN.SESSION.TALK_TYPE." + sessionType.getName().toUpperCase(), sessionType.getName());
	}

	/**
	 * Returns i18n version of the floor name depending on the default locale
	 * 
	 * @param floor
	 *            floor
	 * @return i18n version of the floor name depending on the default locale
	 */
	public static String getFloorName(Floor floor) {
		String floorName = floor.getName();
		return getI18NValue("OTN.VIEW.EXHIBITIONMAPS." + floorName.trim().toUpperCase().replaceAll(" ", "\\."),
				floorName);
	}
	
	
	public static String getCity(String city) {
		if ("barcelona".equalsIgnoreCase(city.trim())) {
			return getI18NValue("OTN.VIEW.VENUE.CITY."+city.toUpperCase().replaceAll(" ", "\\."), city);
		} else {
			return city;
		}
	}
	
	public static String getCountry(String country) {
		if ("spain".equalsIgnoreCase(country.trim())) {
			return getI18NValue("OTN.VIEW.VENUE.COUNTRY."+country.toUpperCase().replaceAll(" ", "\\."), country);
		} else {
			return country;
		}
	}

	private static String getI18NValue(String key, String defaultValue) {
		String i18nVal = "";
		i18nVal = DevoxxBundle.getString(key);
		if (i18nVal == null || i18nVal.trim().length() == 0)
			i18nVal = defaultValue;
		return i18nVal;
	}

}
