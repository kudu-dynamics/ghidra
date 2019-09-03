/* ###
 * IP: GHIDRA
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
package ghidra.server.security;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;

import ghidra.server.UserManager;

public interface AuthenticationModule {

	public static final String USERNAME_CALLBACK_PROMPT = "User ID";
	public static final String PASSWORD_CALLBACK_PROMPT = "Password";

	default void ensureConfig() {
		// default nothing
	}

	/**
	 * Complete the authentication process.
	 * <p>
	 * Note to AuthenticationModule implementors:
	 * <ul>
	 * <li>The authentication callback objects are not guaranteed to be the same
	 * instances as those returned by the {@link #getAuthenticationCallbacks()}.<br>
	 * (they may have been cloned or duplicated or copied in some manner)</li>
	 * <li>The authentication callback array may contain callback instances other than
	 * the ones your module specified in its {@link #getAuthenticationCallbacks()}</li>
	 * </ul>
	 * <p>
	 *
	 * <p>
	 * @param userMgr Ghidra server user manager
	 * @param subject unauthenticated user ID (must be used if name callback not provided/allowed)
	 * @param callbacks authentication callbacks
	 * @return authenticated user ID (may come from callbacks)
	 * @throws LoginException
	 */
	String authenticate(UserManager userMgr, Subject subject, Callback[] callbacks)
			throws LoginException;

	/**
	 * Returns authentication callbacks needed to authenticate a user.
	 */
	Callback[] getAuthenticationCallbacks();

	/**
	 * Allows an AuthenticationModule to deny default anonymous login steps.
	 * <p>
	 * @return true if a separate AnonymousCallback is allowed and may be
	 * added to the array returned by getAuthenticationCallbacks.
	 * @see #getAuthenticationCallbacks()
	 */
	boolean anonymousCallbacksAllowed();

	/**
	 * @return true if NameCallback is allowed
	 */
	boolean isNameCallbackAllowed();

	static Callback[] createSimpleNamePasswordCallbacks(boolean allowUserToSpecifyName) {
		PasswordCallback passCb = new PasswordCallback(PASSWORD_CALLBACK_PROMPT + ":", false);
		if (allowUserToSpecifyName) {
			NameCallback nameCb = new NameCallback(USERNAME_CALLBACK_PROMPT + ":");
			return new Callback[] { nameCb, passCb };
		}
		return new Callback[] { passCb };
	}

	static <T extends Callback> T getFirstCallbackOfType(Class<T> callbackClass,
			Callback[] callbackArray) {
		if (callbackArray == null) {
			return null;
		}

		// dunno if this approach is warranted. the second loop with its isInstance() may be fine.
		for (Callback cb : callbackArray) {
			if (callbackClass == cb.getClass()) {
				return callbackClass.cast(cb);
			}
		}
		for (Callback cb : callbackArray) {
			if (callbackClass.isInstance(cb.getClass())) {
				return callbackClass.cast(cb);
			}
		}
		return null;
	}
}
