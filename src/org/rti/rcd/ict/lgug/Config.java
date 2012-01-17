/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rti.rcd.ict.lgug;

public class Config {

// App ID for C2DM server registrations
    public static final String C2DM_ACCOUNT_EXTRA = "account_name";
    public static final String C2DM_MESSAGE_EXTRA = "message";
    public static final String C2DM_MESSAGE_SYNC = "sync";

// Network communication
// For app engine SDK
    //public static final String PUSH_SERVER_URL = "http://192.168.0.3:8080";

	@SuppressWarnings("unchecked")
	public static String makeLogTag(Class cls) {
	    String tag = "Coconut_" + cls.getSimpleName();
	    return (tag.length() > 23) ? tag.substring(0, 23) : tag;
	}
}
