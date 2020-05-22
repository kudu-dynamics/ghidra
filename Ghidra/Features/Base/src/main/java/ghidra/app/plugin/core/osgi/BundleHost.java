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
package ghidra.app.plugin.core.osgi;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.*;

import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.*;
import org.osgi.service.log.*;

import generic.io.NullPrintWriter;
import generic.jar.ResourceFile;
import ghidra.framework.Application;
import ghidra.framework.options.SaveState;
import ghidra.framework.plugintool.PluginTool;
import ghidra.util.Msg;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.*;

public class BundleHost {
	protected static final boolean DUMP_TO_STDERR = false;

	static public String getSymbolicNameFromSourceDir(ResourceFile sourceDir) {
		return Integer.toHexString(sourceDir.getAbsolutePath().hashCode());
	}

	public BundleHost() {
		//
	}

	public void dispose() {
		disposeFramework();
	}

	HashMap<ResourceFile, GhidraBundle> bp2gb = new HashMap<>();
	HashMap<String, GhidraBundle> bl2gb = new HashMap<>();

	public GhidraBundle getExistingGhidraBundle(ResourceFile bundlePath) {
		GhidraBundle r = bp2gb.get(bundlePath);
		if (r == null) {
			Msg.showError(this, null, "ghidra bundle cache",
				"getExistingGhidraBundle before GhidraBundle created: " + bundlePath);
		}
		return r;
	}

	public boolean enablePath(ResourceFile bundlePath) {
		GhidraBundle gb = bp2gb.get(bundlePath);
		if (gb == null) {
			gb = addGhidraBundle(bundlePath, true, false);
			return true;
		}
		return enable(gb);
	}

	public boolean enable(GhidraBundle gbundle) {
		if (!gbundle.isEnabled()) {
			gbundle.setEnabled(true);
			fireBundleEnablementChange(gbundle, true);
			return true;
		}
		return false;
	}

	public boolean disable(GhidraBundle gbundle) {
		if (gbundle.isEnabled()) {
			gbundle.setEnabled(false);
			fireBundleEnablementChange(gbundle, false);
			return true;
		}
		return false;
	}

	public GhidraBundle addGhidraBundle(ResourceFile path, boolean enabled, boolean systemBundle) {
		GhidraBundle gb = newGhidraBundle(this, path, enabled, systemBundle);
		bp2gb.put(path, gb);
		bl2gb.put(gb.getBundleLoc(), gb);
		fireBundleAdded(gb);
		return gb;
	}

	public void addGhidraBundles(List<ResourceFile> paths, boolean enabled, boolean systemBundle) {
		Map<ResourceFile, GhidraBundle> newmap =
			paths.stream().collect(Collectors.toUnmodifiableMap(Function.identity(),
				path -> newGhidraBundle(BundleHost.this, path, enabled, systemBundle)));
		bp2gb.putAll(newmap);
		bl2gb.putAll(newmap.values().stream().collect(
			Collectors.toUnmodifiableMap(GhidraBundle::getBundleLoc, Function.identity())));
		fireBundlesAdded(newmap.values());
	}

	public void add(List<GhidraBundle> gbundles) {
		for (GhidraBundle gb : gbundles) {
			bp2gb.put(gb.getPath(), gb);
			bl2gb.put(gb.getBundleLoc(), gb);
		}
		fireBundlesAdded(gbundles);
	}

	static private GhidraBundle newGhidraBundle(BundleHost bh, ResourceFile path, boolean enabled,
			boolean systemBundle) {
		switch (GhidraBundle.getType(path)) {
			case SourceDir:
				return new GhidraSourceBundle(bh, path, enabled, systemBundle);
			case Jar:
				return new GhidraJarBundle(bh, path, enabled, systemBundle);
			case BndScript:
			default:
				break;
		}
		return new GhidraPlaceholderBundle(bh, path, enabled, systemBundle);
	}

	// XXX consumers must clean up after themselves
	public void removeBundlePath(ResourceFile bundlePath) {
		GhidraBundle gb = bp2gb.remove(bundlePath);
		bl2gb.remove(gb.getBundleLoc());
		fireBundleRemoved(gb);
	}

	public void removeBundleLoc(String bundleLoc) {
		GhidraBundle gb = bl2gb.remove(bundleLoc);
		bp2gb.remove(gb.getPath());
		fireBundleRemoved(gb);
	}

	public void remove(GhidraBundle gb) {
		bp2gb.remove(gb.getPath());
		bl2gb.remove(gb.getBundleLoc());
		fireBundleRemoved(gb);
	}

	public void remove(Collection<GhidraBundle> gbundles) {
		for (GhidraBundle gb : gbundles) {
			bp2gb.remove(gb.getPath());
			bl2gb.remove(gb.getBundleLoc());
		}
		fireBundlesRemoved(gbundles);
	}

	/**
	 * parse Import-Package string from a bundle manifest
	 * 
	 * @param imports Import-Package value
	 * @return deduced requirements or null if there was an error
	 * @throws BundleException on parse failure
	 */
	static List<BundleRequirement> parseImports(String imports) throws BundleException {
		// parse it with Felix's ManifestParser to a list of BundleRequirement objects
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put(Constants.IMPORT_PACKAGE, imports);
		ManifestParser mp;
		mp = new ManifestParser(null, null, null, headerMap);
		return mp.getRequirements();
	}

	/**
	 * cache of data corresponding to a source directory that is bound to be an exploded bundle
	 */
	public static class BuildFailure {
		long when = -1;
		StringBuilder message = new StringBuilder();
	}

	String buildExtraPackages() {
		Set<String> packages = new HashSet<>();
		getPackagesFromClasspath(packages);
		return packages.stream().collect(Collectors.joining(","));
	}

	BundleContext bc;
	Framework felix;
	Bundle fileinstall_bundle;

	Bundle installFromPath(Path p) throws GhidraBundleException {
		return installFromLoc("file://" + p.toAbsolutePath().normalize().toString());
	}

	public Bundle installFromLoc(String bundle_loc) throws GhidraBundleException {
		try {
			return bc.installBundle(bundle_loc);
		}
		catch (BundleException e) {
			throw new GhidraBundleException(bundle_loc, "installing from bundle location", e);
		}
	}

	Bundle installAsLoc(String bundle_loc, InputStream contents) throws GhidraBundleException {
		try {
			return bc.installBundle(bundle_loc, contents);
		}
		catch (BundleException e) {
			throw new GhidraBundleException(bundle_loc, "installing as bundle location", e);
		}
	}

	void dumpLoadedBundles() {
		System.err.printf("=== Bundles ===\n");
		for (Bundle bundle : bc.getBundles()) {
			System.err.printf("%s: %s: %s: %s\n", bundle.getBundleId(), bundle.getSymbolicName(),
				bundle.getState(), bundle.getVersion());
		}
	}

	/**
	 * Attempt to resolve a list of BundleRequirements with active Bundle capabilities.
	 * 
	 * @param reqs list of requirements -- satisfied requirements are removed as capabiliites are found
	 * @return the list of BundeWiring objects correpsonding to matching capabilities
	 */
	public List<BundleWiring> resolve(List<BundleRequirement> reqs) {
		// enumerate active bundles, looking for capabilities meeting our requirements
		List<BundleWiring> bundleWirings = new ArrayList<>();
		for (Bundle b : bc.getBundles()) {
			if (b.getState() == Bundle.ACTIVE) {
				BundleWiring bw = b.adapt(BundleWiring.class);
				boolean keeper = false;
				for (BundleCapability cap : bw.getCapabilities(null)) {
					Iterator<BundleRequirement> it = reqs.iterator();
					while (it.hasNext()) {
						BundleRequirement req = it.next();
						if (req.matches(cap)) {
							it.remove();
							keeper = true;
						}
					}
				}
				if (keeper) {
					bundleWirings.add(bw);
				}
			}
		}
		return bundleWirings;
	}

	public boolean canResolveAll(Collection<BundleRequirement> reqs) {
		LinkedList<BundleRequirement> tmp = new LinkedList<>(reqs);
		resolve(tmp);
		return tmp.isEmpty();
	}

	class FelixStderrLogger extends org.apache.felix.framework.Logger {
		@Override
		protected void doLog(int level, String msg, Throwable throwable) {
			System.err.printf("felixlogger: %s %s\n", msg, throwable);
		}

		@Override
		protected void doLogOut(int level, String msg, Throwable throwable) {
			System.err.printf("felixlogger: %s %s\n", msg, throwable);
		}

		@SuppressWarnings("rawtypes")

		@Override
		protected void doLog(final Bundle bundle, final ServiceReference sr, final int level,
				final String msg, final Throwable throwable) {
			System.err.printf("felixlogger: %s %s %s\n", bundle, msg, throwable);
		}

	}

	static String getEventTypeString(BundleEvent e) {
		switch (e.getType()) {
			case BundleEvent.INSTALLED:
				return "INSTALLED";
			case BundleEvent.RESOLVED:
				return "RESOLVED";
			case BundleEvent.LAZY_ACTIVATION:
				return "LAZY_ACTIVATION";
			case BundleEvent.STARTING:
				return "STARTING";
			case BundleEvent.STARTED:
				return "STARTED";
			case BundleEvent.STOPPING:
				return "STOPPING";
			case BundleEvent.STOPPED:
				return "STOPPED";
			case BundleEvent.UPDATED:
				return "UPDATED";
			case BundleEvent.UNRESOLVED:
				return "UNRESOLVED";
			case BundleEvent.UNINSTALLED:
				return "UNINSTALLED";
			default:
				return "???";
		}
	}

	/**
	 * start the framework
	 * 
	 * @throws OSGiException framework failures
	 * @throws IOException filesystem setup
	 */
	public void startFramework() throws OSGiException, IOException {

		Properties config = new Properties();

		// allow multiple bundles w/ the same symbolic name -- location can distinguish
		config.setProperty(Constants.FRAMEWORK_BSNVERSION, Constants.FRAMEWORK_BSNVERSION_MULTIPLE);
		// use the default, inferred from environment
		// config.setProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES,"osgi.ee; osgi.ee=\"JavaSE\";version:List=\"...\"");

		// compute and add everything in the class path.  extra packages have lower precedence than imports,
		// so an Import-Package / @importpackage will override the "living off the land" default
		config.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, buildExtraPackages());

		// only clean on first startup, o/w keep our storage around
		config.setProperty(Constants.FRAMEWORK_STORAGE_CLEAN,
			Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

		// setup the cache path
		config.setProperty(Constants.FRAMEWORK_STORAGE, makeCacheDir());

		config.put(FelixConstants.LOG_LEVEL_PROP, "1");
		if (DUMP_TO_STDERR) {
			config.put(FelixConstants.LOG_LEVEL_PROP, "999");
			config.put(FelixConstants.LOG_LOGGER_PROP, new FelixStderrLogger());
		}

		FrameworkFactory factory = new FrameworkFactory();
		felix = factory.newFramework(config);

		try {
			felix.init();
		}
		catch (BundleException e) {
			throw new OSGiException("initializing felix OSGi framework", e);
		}
		bc = felix.getBundleContext();

		ServiceReference<LogReaderService> ref = bc.getServiceReference(LogReaderService.class);
		if (ref != null) {
			LogReaderService reader = bc.getService(ref);
			reader.addLogListener(new LogListener() {

				@Override
				public void logged(LogEntry entry) {
					// plugin.printf("%s: %s\n", entry.getBundle(), entry.getMessage());
				}
			});
		}
		else {
			// plugin.printf("no logreaderservice in felix!\n");
		}

		if (DUMP_TO_STDERR) {
			bc.addFrameworkListener(new FrameworkListener() {
				@Override
				public void frameworkEvent(FrameworkEvent event) {
					System.err.printf("%s %s\n", event.getBundle(), event);
				}
			});
			bc.addServiceListener(new ServiceListener() {
				@Override
				public void serviceChanged(ServiceEvent event) {

					String type = "?";
					if (event.getType() == ServiceEvent.REGISTERED) {
						type = "registered";
					}
					else if (event.getType() == ServiceEvent.UNREGISTERING) {
						type = "unregistering";
					}

					System.err.printf("%s %s from %s\n", event.getSource(), type,
						event.getServiceReference().getBundle().getLocation());

				}
			});
		}

		final Bundle systemBundle = bc.getBundle();
		bc.addBundleListener(new BundleListener() {
			@Override
			public void bundleChanged(BundleEvent event) {
				Bundle b = event.getBundle();

				if (b == systemBundle) {
					return;
				}
				if (DUMP_TO_STDERR) {
					String n = b.getSymbolicName();
					String l = b.getLocation();
					System.err.printf("%s %s from %s\n", getEventTypeString(event), n, l);
				}
				GhidraBundle gb;
				switch (event.getType()) {
					case BundleEvent.STARTED:
						gb = bl2gb.get(b.getLocation());
						if (gb != null) {
							fireBundleActivationChange(gb, true);
						}
						else {
							Msg.error(this,
								String.format("not a GhidraBundle: %s\n", b.getLocation()));
						}
						break;
					case BundleEvent.UNINSTALLED:
						gb = bl2gb.get(b.getLocation());
						if (gb != null) {
							fireBundleActivationChange(gb, false);
						}
						else {
							Msg.error(this,
								String.format("not a GhidraBundle: %s\n", b.getLocation()));
						}
						break;
					default:
						break;
				}
			}
		});

		try {
			felix.start();
		}
		catch (BundleException e) {
			throw new OSGiException("starting felix OSGi framework", e);
		}
	}

	public static Path getCompiledBundlesDir() {
		return getOsgiDir().resolve("compiled-bundles");
	}

	public static Path getOsgiDir() {
		Path usersettings = Application.getUserSettingsDirectory().toPath();
		return usersettings.resolve("osgi");
	}

	static public Path getCacheDir() {
		return BundleHost.getOsgiDir().resolve("felixcache");
	}

	private String makeCacheDir() throws IOException {
		Path cache_dir = getCacheDir();
		Files.createDirectories(cache_dir);
		return cache_dir.toAbsolutePath().toString();
	}

	public Bundle getBundle(String bundleLoc) {
		return bc.getBundle(bundleLoc);
	}

	static private boolean oneOf(Bundle b, int... bundle_states) {
		Integer s = b.getState();
		return IntStream.of(bundle_states).anyMatch(s::equals);
	}

	static private void waitFor(Bundle b, int... bundle_states) throws InterruptedException {
		while (true) {
			if (oneOf(b, bundle_states)) {
				return;
			}
			Thread.sleep(500);
		}
	}

	public void activateSynchronously(Bundle b) throws InterruptedException, GhidraBundleException {
		if (b.getState() == Bundle.ACTIVE) {
			return;
		}
		try {
			b.start();
		}
		catch (BundleException e) {
			GhidraBundleException gbe = new GhidraBundleException(b, "activating bundle", e);
			fireBundleException(gbe);
			throw gbe;
		}
		waitFor(b, Bundle.ACTIVE);
	}

	public void activateSynchronously(String bundleLoc)
			throws GhidraBundleException, InterruptedException {
		Bundle bundle = getBundle(bundleLoc);
		if (bundle == null) {
			bundle = installFromLoc(bundleLoc);
		}
		activateSynchronously(bundle);
	}

	public void deactivateSynchronously(Bundle b)
			throws InterruptedException, GhidraBundleException {
		if (b.getState() == Bundle.UNINSTALLED) {
			return;
		}
		FrameworkWiring fw = felix.adapt(FrameworkWiring.class);
		LinkedList<Bundle> dependents =
			new LinkedList<Bundle>(fw.getDependencyClosure(Collections.singleton(b)));
		while (!dependents.isEmpty()) {
			b = dependents.pop();
			try {
				b.uninstall();
				fw.refreshBundles(dependents);
			}
			catch (BundleException e) {
				GhidraBundleException gbe = new GhidraBundleException(b, "deactivating bundle", e);
				fireBundleException(gbe);
				throw gbe;
			}
			waitFor(b, Bundle.UNINSTALLED);
		}
	}

	public void deactivateSynchronously(String bundleLoc)
			throws GhidraBundleException, InterruptedException {
		Bundle b = getBundle(bundleLoc);
		if (b != null) {
			deactivateSynchronously(b);
		}
	}

	void disposeFramework() {
		if (felix != null) {
			try {
				felix.stop();
				felix.waitForStop(5000);
				felix = null;
			}
			catch (BundleException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void forceStopFrameworkTask() {
		Task t = new Task("killing OSGi framework", false, false, true, true) {
			@Override
			public void run(TaskMonitor monitor) throws CancelledException {
				disposeFramework();
			}

		};
		new TaskLauncher(t, null);
	}

	public Framework getHostFramework() {
		return felix;
	}

	// from https://dzone.com/articles/locate-jar-classpath-given
	static String findJarForClass(Class<?> c) {
		final URL location;
		final String classLocation = c.getName().replace('.', '/') + ".class";
		final ClassLoader loader = c.getClassLoader();
		if (loader == null) {
			location = ClassLoader.getSystemResource(classLocation);
		}
		else {
			location = loader.getResource(classLocation);
		}
		if (location != null) {
			Pattern p = Pattern.compile("^.*:(.*)!.*$");
			Matcher m = p.matcher(location.toString());
			if (m.find()) {
				return m.group(1);
			}
			return null; // not loaded from jar?
		}
		return null;
	}

	static void getPackagesFromClasspath(Set<String> s) {
		getClasspathElements().forEach(p -> {
			if (Files.isDirectory(p)) {
				collectPackagesFromDirectory(p, s);
			}
			else if (p.toString().endsWith(".jar")) {
				collectPackagesFromJar(p, s);
			}
		});
	}

	static Stream<Path> getClasspathElements() {
		String classpathStr = System.getProperty("java.class.path");
		return Collections.list(new StringTokenizer(classpathStr, File.pathSeparator)).stream().map(
			String.class::cast).map(Paths::get).map(Path::normalize);
	}

	static void collectPackagesFromDirectory(Path dirPath, Set<String> s) {
		try {
			Files.walk(dirPath).filter(p -> p.toString().endsWith(".class")).forEach(p -> {
				String n = dirPath.relativize(p).toString();
				int lastSlash = n.lastIndexOf(File.separatorChar);
				s.add(lastSlash > 0 ? n.substring(0, lastSlash).replace(File.separatorChar, '.')
						: "");
			});

		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void collectPackagesFromJar(Path jarPath, Set<String> s) {
		try {
			try (JarFile j = new JarFile(jarPath.toFile())) {
				j.stream().filter(je -> je.getName().endsWith(".class")).forEach(je -> {
					String n = je.getName();
					int lastSlash = n.lastIndexOf('/');
					s.add(lastSlash > 0 ? n.substring(0, lastSlash).replace('/', '.') : "");
				});
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	List<BundleHostListener> listeners = new ArrayList<>();

	void fireBundleBuilt(GhidraBundle gb, String summary) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundleBuilt(gb, summary);
			}
		}
	}

	void fireBundleEnablementChange(GhidraBundle gb, boolean newEnablement) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundleEnablementChange(gb, newEnablement);
			}
		}
	}

	void fireBundleActivationChange(GhidraBundle gb, boolean newEnablement) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundleActivationChange(gb, newEnablement);
			}
		}
	}

	private void fireBundleAdded(GhidraBundle gb) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundleAdded(gb);
			}
		}
	}

	private void fireBundleRemoved(GhidraBundle gb) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundleRemoved(gb);
			}
		}
	}

	private void fireBundlesAdded(Collection<GhidraBundle> gbundles) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundlesAdded(gbundles);
			}
		}
	}

	private void fireBundlesRemoved(Collection<GhidraBundle> gbundles) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundlesRemoved(gbundles);
			}
		}
	}

	private void fireBundleException(GhidraBundleException gbe) {
		synchronized (listeners) {
			for (BundleHostListener l : listeners) {
				l.bundleException(gbe);
			}
		}
	}

	public void addListener(BundleHostListener bundleHostListener) {
		synchronized (listeners) {
			listeners.add(bundleHostListener);
		}
	}

	public void removeListener(BundleHostListener bundleHostListener) {
		synchronized (listeners) {
			listeners.remove(bundleHostListener);
		}
	}

	public void removeAllListeners() {
		synchronized (listeners) {
			listeners.clear();
		}
	}

	public Collection<GhidraBundle> getGhidraBundles() {
		return bp2gb.values();
	}

	public Collection<ResourceFile> getBundlePaths() {
		return bp2gb.keySet();
	}

	/*
	 * this is done at most once, and it's done AFTER system bundles have been added.
	 */
	public void restoreStateAndActivate(SaveState ss, PluginTool tool) {
		// XXX lock bundlehost operations
		String[] pathArr = ss.getStrings("BundleHost_PATH", new String[0]);

		if (pathArr.length == 0) {
			return;
		}

		boolean[] enableArr = ss.getBooleans("BundleHost_ENABLE", new boolean[pathArr.length]);
		boolean[] activeArr = ss.getBooleans("BundleHost_ACTIVE", new boolean[pathArr.length]);
		boolean[] systemArr = ss.getBooleans("BundleHost_SYSTEM", new boolean[pathArr.length]);

		List<GhidraBundle> added = new ArrayList<>();
		List<GhidraBundle> need_activation = new ArrayList<>();
		for (int i = 0; i < pathArr.length; i++) {

			ResourceFile bp = generic.util.Path.fromPathString(pathArr[i]);
			boolean en = enableArr[i];
			boolean act = activeArr[i];
			boolean sys = systemArr[i];
			GhidraBundle gb = bp2gb.get(bp);
			if (gb != null) {
				if (en != gb.isEnabled()) {
					gb.setEnabled(en);
					fireBundleEnablementChange(gb, en);
				}
				if (sys != gb.isSystemBundle()) {
					gb.systemBundle = sys;
					Msg.error(this, String.format("%s went from %system to %system", bp,
						sys ? "not " : "", sys ? "" : "not "));
				}
			}
			else if (sys) {
				// stored system bundles that weren't already initialized must be old, drop 'm.
			}
			else {
				added.add(gb = newGhidraBundle(this, bp, en, sys));
			}
			if (gb != null && act) {
				need_activation.add(gb);
			}
		}
		add(added);

		new TaskLauncher(new Task("restoring bundle state", true, true, false) {
			@Override
			public void run(TaskMonitor monitor) throws CancelledException {
				activateAll(need_activation, monitor, new NullPrintWriter());
			}
		});
	}

	public void saveState(SaveState ss) {
		int n = bp2gb.size();
		String[] pathArr = new String[n];
		boolean[] enableArr = new boolean[n];
		boolean[] activeArr = new boolean[n];
		boolean[] systemArr = new boolean[n];

		int index = 0;
		for (Entry<ResourceFile, GhidraBundle> e : bp2gb.entrySet()) {
			GhidraBundle gb = e.getValue();
			pathArr[index] = generic.util.Path.toPathString(gb.getPath());
			enableArr[index] = gb.isEnabled();
			activeArr[index] = gb.isActive();
			systemArr[index] = gb.isSystemBundle();
			++index;
		}

		ss.putStrings("BundleHost_PATH", pathArr);
		ss.putBooleans("BundleHost_ENABLE", enableArr);
		ss.putBooleans("BundleHost_ACTIVE", activeArr);
		ss.putBooleans("BundleHost_SYSTEM", systemArr);
	}

	protected void activateAll(List<GhidraBundle> gbs, TaskMonitor monitor, PrintWriter console) {

		monitor.setMaximum(gbs.size());
		while (!gbs.isEmpty() && !monitor.isCancelled()) {
			List<GhidraBundle> l =
				gbs.stream().filter(gb -> canResolveAll(gb.getAllReqs())).collect(
					Collectors.toList());
			if (l.isEmpty()) {
				// final round
				l = gbs;
				gbs = Collections.emptyList();
			}
			else {
				gbs.removeAll(l);
			}

			for (GhidraBundle gb : l) {
				if (monitor.isCancelled()) {
					break;
				}
				try {
					gb.build(console);
					activateSynchronously(gb.getBundleLoc());
				}
				catch (Exception e) {
					e.printStackTrace(console);
				}
				monitor.incrementProgress(1);
			}
		}
	}

}
