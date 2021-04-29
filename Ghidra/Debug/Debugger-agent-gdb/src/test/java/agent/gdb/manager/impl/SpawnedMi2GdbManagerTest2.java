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
package agent.gdb.manager.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.junit.Ignore;

import agent.gdb.manager.GdbManager;
import agent.gdb.pty.PtyFactory;
import agent.gdb.pty.linux.LinuxPtyFactory;

@Ignore("Need compatible GDB version for CI")
public class SpawnedMi2GdbManagerTest2 extends AbstractGdbManagerTest {
	@Override
	protected CompletableFuture<Void> startManager(GdbManager manager) {
		try {
			manager.start(GdbManager.DEFAULT_GDB_CMD, "-i", "mi2");
			return manager.runRC();
		}
		catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected PtyFactory getPtyFactory() {
		// TODO: Choose by host OS
		return new LinuxPtyFactory();
	}
}
