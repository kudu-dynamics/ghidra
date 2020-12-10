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
package agent.gdb.manager.reason;

import agent.gdb.manager.parsing.GdbMiParser.GdbMiFieldList;

/**
 * The inferior stopped because it has received a signal
 */
public class GdbSignalReceivedReason implements GdbReason {
	private final String signalName;

	public GdbSignalReceivedReason(GdbMiFieldList info) {
		this.signalName = info.getString("signal-name");
	}

	/**
	 * Get the (POSIX) name of the signal received
	 * 
	 * @return the signal name
	 */
	public String getSignalName() {
		return signalName;
	}

	@Override
	public String desc() {
		return "Signalled with " + getSignalName();
	}
}
