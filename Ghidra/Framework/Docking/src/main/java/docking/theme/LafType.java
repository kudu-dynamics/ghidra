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
package docking.theme;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import docking.theme.laf.*;
import ghidra.framework.OperatingSystem;
import ghidra.framework.Platform;
import ghidra.util.exception.AssertException;

public enum LafType {
	METAL("Metal", false),
	NIMBUS("Nimbus", false),
	GTK("GTK+", false),
	MOTIF("CDE/Motif", false),
	FLAT_LIGHT("Flat Light", false),
	FLAT_DARK("Flat Dark", true),
	FLAT_DARCULA("Flat Darcula", true),
	WINDOWS("Windows", false),
	WINDOWS_CLASSIC("Windows Classic", false),
	MAC("Mac OS X", false),
	SYSTEM("System", false);

	private String name;
	private boolean isDark;

	private LafType(String name, boolean isDark) {
		this.name = name;
		this.isDark = isDark;
	}

	public String getName() {
		return name;
	}

	public boolean isDark() {
		return isDark;
	}

	public static LafType fromName(String name) {
		for (LafType type : values()) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		return null;
	}

	private static LookAndFeelInstaller getSystemLookAndFeelInstaller() {
		OperatingSystem OS = Platform.CURRENT_PLATFORM.getOperatingSystem();
		if (OS == OperatingSystem.LINUX) {
			return getInstaller(NIMBUS);
		}
		else if (OS == OperatingSystem.MAC_OS_X) {
			return getInstaller(MAC);
		}
		else if (OS == OperatingSystem.WINDOWS) {
			return getInstaller(WINDOWS);
		}
		return getInstaller(NIMBUS);
	}

	public boolean isSupported() {
		if (this == SYSTEM) {
			return true;
		}
		LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
		for (LookAndFeelInfo info : installedLookAndFeels) {
			if (name.equals(info.getName())) {
				return true;
			}
		}
		return false;
	}

	public void install() throws Exception {
		getInstaller(this).install();
	}

	private static LookAndFeelInstaller getInstaller(LafType lookAndFeel) {
		switch (lookAndFeel) {
			case FLAT_DARCULA:
				return new FlatLookAndFeelInstaller(FLAT_DARCULA);
			case FLAT_DARK:
				return new FlatLookAndFeelInstaller(FLAT_DARK);
			case FLAT_LIGHT:
				return new FlatLookAndFeelInstaller(FLAT_LIGHT);
			case GTK:
				return new GTKLookAndFeelInstaller();
			case MAC:
				return new LookAndFeelInstaller(MAC);
			case METAL:
				return new LookAndFeelInstaller(METAL);
			case MOTIF:
				return new MotifLookAndFeelInstaller();  // Motif has some specific ui fix ups
			case NIMBUS:
				return new NimbusLookAndFeelInstaller(); // Nimbus installs a special way
			case SYSTEM:
				return getSystemLookAndFeelInstaller();
			case WINDOWS:
				return new LookAndFeelInstaller(WINDOWS);
			case WINDOWS_CLASSIC:
				return new LookAndFeelInstaller(WINDOWS_CLASSIC);
			default:
				throw new AssertException("No lookAndFeelInstaller defined for " + lookAndFeel);
		}
	}
}
