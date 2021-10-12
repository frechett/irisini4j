/*
 * Copyright 2005,2009 Ivan SZKIBA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ini4j;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class IniPreferencesFactory implements PreferencesFactory {
	/* Key for the system preferences * */
	public static final String KEY_SYSTEM = "org.ini4j.prefs.system";
	/** Key for the user preferences */
	public static final String KEY_USER = "org.ini4j.prefs.user";
	/** Ini4j properties */
	public static final String PROPERTIES = "ini4j.properties";

	@Override
	public synchronized Preferences systemRoot() {
		return IniPreferences.getSystemRoot();
	}

	@Override
	public synchronized Preferences userRoot() {
		return IniPreferences.getUserRoot();
	}
}