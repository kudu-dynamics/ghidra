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
package ghidra.server;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

import javax.security.auth.x500.X500Principal;

import generic.jar.ResourceFile;
import ghidra.GhidraApplicationLayout;
import ghidra.GhidraLaunchable;
import ghidra.framework.Application;
import ghidra.framework.ApplicationConfiguration;
import ghidra.util.Msg;
import ghidra.util.NamingUtilities;

public class ServerAdmin implements GhidraLaunchable {

	private static final String CONFIG_FILE_PROPERTY = "UserAdmin.config";

	// property name defined within the sever.conf file which specifies
	// server repositories directory
	private static final String SERVER_DIR_CONFIG_PROPERTY = "ghidra.repositories.dir";

	private static final String INVOCATION_NAME_PROPERTY = "UserAdmin.invocation";

	// Immediate commands
	private static final String LIST_COMMAND = "-list";
	private static final String USERS_COMMAND = "-users";

	// Delayed commands
	private static final String MIGRATE_COMMAND = "-migrate";
	private static final String MIGRATE_ALL_COMMAND = "-migrate-all";

	private boolean propertyUsed = false;

	/**
	 * Main method for running the UserAdmin Application.
	 * The following properties may be set:
	 * <pre>
	 *   UserAdmin.invocation - identifies the name of the application used when displaying usage text.
	 *   UserAdmin.serverDir - identifies the server directory instead of passing on command line.
	 * </pre>
	 * @param args command line arguments
	 */
	@Override
	public void launch(GhidraApplicationLayout layout, String[] args) {

		// Perform static initializations if not already initialized
		// Some tests invoke main method directly which have already initialized Application
		if (!Application.isInitialized()) {
			ApplicationConfiguration configuration = new ApplicationConfiguration();
			configuration.setInitializeLogging(false);
			Application.initializeApplication(layout, configuration);
		}

		File serverDir = null;

		int ix = 0;
		if (args.length != 0 && !args[0].startsWith("-")) {
			serverDir = new File(args[ix++]);
		}
		else {
			serverDir = getServerDirFromConfig();
		}

		if (serverDir == null || (args.length - ix) == 0) {
			displayUsage("");
			System.exit(-1);
			return;
		}

		try {
			serverDir = serverDir.getCanonicalFile();
		}
		catch (IOException e1) {
			System.err.println("Failed to resolve server directory: " + serverDir);
			System.exit(-1);
		}

		if (propertyUsed) {
			System.out.println("Using server directory: " + serverDir);
		}

		File userFile = new File(serverDir, UserManager.USER_PASSWORD_FILE);
		if (!serverDir.isDirectory() || !userFile.isFile()) {
			System.err.println("Invalid Ghidra server directory specified: " + serverDir);
			System.exit(-1);
		}

		File cmdDir = new File(serverDir, UserAdmin.ADMIN_CMD_DIR);
		if (!cmdDir.exists()) {
			System.err.println("Insufficient privilege or server not started.");
			System.exit(-1);
		}
		if (!cmdDir.isDirectory()) {
			System.err.println("Bad server directory: " + serverDir);
			System.exit(-1);
		}

		// Process command line
		boolean listRepositories = false;
		boolean listUsers = false;
		boolean migrationConfirmed = false;
		boolean migrationAbort = false;
		ArrayList<String> cmdList = new ArrayList<>();
		int cmdLen = 1;
		for (; ix < args.length; ix += cmdLen) {
			boolean queueCmd = true;
			if (UserAdmin.ADD_USER_COMMAND.equals(args[ix])) {  // add user
				cmdLen = 2;
				validateSID(args, ix + 1);
			}
			else if (UserAdmin.REMOVE_USER_COMMAND.equals(args[ix])) { // remove user
				cmdLen = 2;
				validateSID(args, ix + 1);
			}
			else if (UserAdmin.RESET_USER_COMMAND.equals(args[ix])) { // reset user
				cmdLen = 2;
				validateSID(args, ix + 1);
			}
			else if (UserAdmin.SET_USER_DN_COMMAND.equals(args[ix])) { // set/add user with DN for PKI
				cmdLen = 3;
				validateSID(args, ix + 1);
				validateDN(args, ix + 2);
			}
			else if (UserAdmin.SET_ADMIN_COMMAND.equals(args[ix])) { // set/add repository admin
				cmdLen = 3;
				validateSID(args, ix + 1);
				validateRepName(args, ix + 2, serverDir);
			}
			else if (LIST_COMMAND.equals(args[ix])) { // list repositories
				cmdLen = 1;
				queueCmd = false;
				listRepositories = true;
			}
			else if (USERS_COMMAND.equals(args[ix])) { // list users (also affects listRepositories)
				cmdLen = 1;
				queueCmd = false;
				listUsers = true;
			}
			else if (MIGRATE_ALL_COMMAND.equals(args[ix])) { // list repositories
				cmdLen = 1;
				queueCmd = false;
				if (!migrationConfirmed && !confirmMigration()) {
					migrationAbort = true;
				}
				migrationConfirmed = true;
				if (!migrationAbort) {
					RepositoryManager.markAllRepositoriesForIndexMigration(serverDir);
				}
			}
			else if (MIGRATE_COMMAND.equals(args[ix])) { // list repositories
				cmdLen = 2;
				queueCmd = false;
				if (ix == (args.length - 1)) {
					System.err.println("Missing " + MIGRATE_COMMAND + " repository name argument");
				}
				else {
					String repositoryName = args[ix + 1];
					if (!migrationConfirmed && !confirmMigration()) {
						migrationAbort = true;
					}
					migrationConfirmed = true;
					if (!migrationAbort) {
						Repository.markRepositoryForIndexMigration(serverDir, repositoryName,
							false);
					}
				}
			}
			else {
				displayUsage("Invalid usage!");
				System.exit(-1);
			}
			if (queueCmd) {
				addCommand(cmdList, args, ix, cmdLen);
			}
		}

		try {
			UserAdmin.writeCommands(cmdList, cmdDir);
		}
		catch (IOException e) {
			System.err.println("Failed to queue commands: " + e.toString());
			System.exit(-1);
		}
		System.out.println(cmdList.size() + " command(s) queued.");

		if (listUsers) {
			UserManager.listUsers(serverDir);
		}
		if (listRepositories) {
			RepositoryManager.listRepositories(serverDir, listUsers);
		}
		System.out.println();
	}

	/**
	 * @param serverDir
	 * @param args
	 * @param i
	 */
	private static void addCommand(ArrayList<String> cmdList, String[] args, int argOffset,
			int argCnt) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < argCnt; i++) {
			if (i > 0) {
				buf.append(' ');
			}
			buf.append(args[argOffset + i]);
		}
		cmdList.add(buf.toString());
	}

	private static boolean confirmMigration() {
		System.out.print("\nWARNING!  Please confirm the requested migration of one or more\n" +
			"Ghidra Server repositories.  Once migrated to indexed storage,\n" +
			"any attempt to use these server repositories with a Ghidra Server\n" +
			"older than version 5.5 will corrupt the data storage.\n" +
			"\nWould you like to continue? [y/n]: ");
		try {
			if ('y' == System.in.read()) {
				System.out.println();
				return true;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("\nAll repository data migration(s) has been aborted.");
		return false;
	}

	/**
	 * Validate properly formatted Distinguished Name
	 * Example:  'CN=Doe John, OU=X, OU=Y, OU=DoD, O=U.S. Government, C=US'
	 * @param args
	 * @param i argument index
	 */
	private void validateDN(String[] args, int i) {
		if (args.length < (i + 1)) {
			displayUsage("Invalid usage!");
			System.exit(-1);
		}
		String dn = args[i];
		try {
			X500Principal x500User = new X500Principal(dn);
			args[i] = "\"" + x500User.getName() + "\"";
		}
		catch (Exception e) {
			Msg.error(UserAdmin.class, "Invalid DN: " + dn);
			System.exit(-1);
		}
	}

	/**
	 * Validate username/sid
	 * @param args
	 * @param i argument index
	 */
	private void validateSID(String[] args, int i) {
		if (args.length < (i + 1)) {
			displayUsage("Invalid usage!");
			System.exit(-1);
		}
		String sid = args[i];
		if (!NamingUtilities.isValidName(sid) || sid.indexOf(' ') >= 0) {
			Msg.error(UserAdmin.class, "Invalid username/sid: " + sid);
			System.exit(-1);
		}
	}

	/**
	 * Validate username/sid
	 * @param args
	 * @param i argument index
	 */
	private void validateRepName(String[] args, int i, File rootDirFile) {
		if (args.length < (i + 1)) {
			displayUsage("Invalid usage!");
			System.exit(-1);
		}
		String repName = args[i];
		File f = new File(rootDirFile, NamingUtilities.mangle(repName));
		if (!f.isDirectory()) {
			Msg.error(UserAdmin.class, "Repository not found: " + repName);
			System.exit(-1);
		}
	}

	private File getServerDirFromConfig() {
		String p = System.getProperty(CONFIG_FILE_PROPERTY);
		if (p == null) {
			return null;
		}
		propertyUsed = true;
		File configFile = new File(p);

		if (!configFile.exists()) {
			System.out.println("Config file not found: " + configFile.getAbsolutePath());
		}

		Properties config = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream(configFile);
			config.load(in);
		}
		catch (IOException e) {
			System.out.println("Failed to read " + configFile.getName() + ": " + e.getMessage());
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
					// ignore
				}
			}
		}

		p = config.getProperty(SERVER_DIR_CONFIG_PROPERTY);
		if (p == null) {
			return null;
		}
		File dir = new File(p);
		if (!dir.isAbsolute()) {
			// Make relative repositories dir relative to installation root
			ResourceFile installRoot = Application.getInstallationDirectory();
			if (installRoot == null || installRoot.getFile(false) == null) {
				System.out.println("Failed to resolve installation root directory!");
				return null;
			}
			dir = new File(installRoot.getFile(false), p);
		}
		return dir;
	}

	/**
	 * Display an optional message followed by usage syntax.
	 * @param msg
	 */
	private void displayUsage(String msg) {
		if (msg != null) {
			System.err.println(msg);
		}
		String invocationName = System.getProperty(INVOCATION_NAME_PROPERTY);
		System.err.println("Usage: " +
			(invocationName != null ? invocationName : "java " + UserAdmin.class.getName()) +
			(propertyUsed ? "" : " <serverPath>") + " [<command>] [<command>] ...");
		System.err.println("\nSupported commands:");
		System.err.println("  -add <sid>");
		System.err.println("      Add a new user to the server identified by their sid identifier");
		System.err.println("  -remove <sid>");
		System.err.println("      Remove the specified user from the server's user list");
		System.err.println("  -reset <sid>");
		System.err.println("      Reset the specified user's server login password");
		System.err.println("  -dn <sid> \"<dname>\"");
		System.err.println(
			"      When PKI authentication is used, add the specified X500 Distinguished Name for a user");
		System.err.println("  -admin <sid> \"<repository-name>\"");
		System.err.println(
			"      Grant ADMIN privilege to the specified user with the specified repository");
		System.err.println("  -list [-users]");
		System.err.println(
			"      Output list of repositories to the console (user access list will be included with -users)");
		System.err.println("  -users");
		System.err.println("      Output list of users to console which have server access");
		System.err.println("  -migrate \"<repository-name>\"");
		System.err.println(
			"      Migrate the specified repository to the latest file system storage schema (see svrREADME.html)");
		System.err.println("  -migrate-all");
		System.err.println(
			"      Migrate the all repositories to the latest file system storage schema (see svrREADME.html)");
		System.err.println();
	}
}
