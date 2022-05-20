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
package ghidra.pcode.emu;

import java.util.*;

import ghidra.app.plugin.processors.sleigh.SleighLanguage;
import ghidra.pcode.exec.*;
import ghidra.program.model.address.Address;
import ghidra.program.model.lang.Language;
import ghidra.util.classfinder.ClassSearcher;

/**
 * An abstract implementation of {@link PcodeMachine} suitable as a base for most implementations
 * 
 * <p>
 * For a complete example of a p-code emulator, see {@link PcodeEmulator}.
 */
public abstract class AbstractPcodeMachine<T> implements PcodeMachine<T> {
	protected final SleighLanguage language;
	protected final PcodeArithmetic<T> arithmetic;
	protected final PcodeUseropLibrary<T> library;

	protected final PcodeUseropLibrary<T> stubLibrary;

	/* for abstract thread access */ PcodeStateInitializer initializer;
	private PcodeExecutorState<T> sharedState;
	protected final Map<String, PcodeThread<T>> threads = new LinkedHashMap<>();
	protected final Collection<PcodeThread<T>> threadsView =
		Collections.unmodifiableCollection(threads.values());

	protected final Map<Address, PcodeProgram> injects = new HashMap<>();

	/**
	 * Construct a p-code machine with the given language and arithmetic
	 * 
	 * @param language the processor language to be emulated
	 * @param arithmetic the definition of arithmetic p-code ops to be used in emulation
	 */
	public AbstractPcodeMachine(SleighLanguage language, PcodeArithmetic<T> arithmetic) {
		this.language = language;
		this.arithmetic = arithmetic;

		this.library = createUseropLibrary();
		this.stubLibrary = createThreadStubLibrary().compose(library);

		/**
		 * NOTE: Do not initialize memoryState here, since createMemoryState may depend on fields
		 * initialized in a sub-constructor
		 */

		this.initializer = getPluggableInitializer(language);
	}

	/**
	 * A factory method to create the userop library shared by all threads in this machine
	 * 
	 * @return the library
	 */
	protected abstract PcodeUseropLibrary<T> createUseropLibrary();

	@Override
	public SleighLanguage getLanguage() {
		return language;
	}

	@Override
	public PcodeArithmetic<T> getArithmetic() {
		return arithmetic;
	}

	@Override
	public PcodeUseropLibrary<T> getUseropLibrary() {
		return library;
	}

	@Override
	public PcodeUseropLibrary<T> getStubUseropLibrary() {
		return stubLibrary;
	}

	/**
	 * A factory method to create the (memory) state shared by all threads in this machine
	 * 
	 * @return the shared state
	 */
	protected abstract PcodeExecutorState<T> createSharedState();

	/**
	 * A factory method to create the (register) state local to the given thread
	 * 
	 * @param thread the thread
	 * @return the thread-local state
	 */
	protected abstract PcodeExecutorState<T> createLocalState(PcodeThread<T> thread);

	/**
	 * A factory method to create a stub library for compiling thread-local SLEIGH source
	 * 
	 * <p>
	 * Because threads may introduce p-code userops using libraries unique to that thread, it
	 * becomes necessary to at least export stub symbols, so that p-code programs can be compiled
	 * from SLEIGH source before the thread has necessarily been created. A side effect of this
	 * strategy is that all threads, though they may have independent libraries, must export
	 * identically-named symbols.
	 * 
	 * @return the stub library for all threads
	 */
	protected PcodeUseropLibrary<T> createThreadStubLibrary() {
		return new DefaultPcodeThread.PcodeEmulationLibrary<T>(null);
	}

	/**
	 * A factory method to create a new thread in this machine
	 * 
	 * @see #newThread(String)
	 * @param name the name of the new thread
	 * @return the new thread
	 */
	protected PcodeThread<T> createThread(String name) {
		return new DefaultPcodeThread<>(name, this);
	}

	/**
	 * Search the classpath for an applicable state initializer
	 * 
	 * <p>
	 * If found, the initializer is executed immediately upon creating this machine's shared state
	 * and upon creating each thread.
	 * 
	 * <p>
	 * TODO: This isn't really being used. At one point in development it was used to initialize
	 * x86's FS_OFFSET and GS_OFFSET registers. Those only exist in p-code, not the real processor,
	 * and replace what might have been {@code segment(FS)}. There seems more utility in detecting
	 * when those registers are uninitialized, requiring the user to initialize them, than it is to
	 * silently initialize them to 0. Unless we find utility in this, it will likely be removed in
	 * the near future.
	 * 
	 * @see #doPluggableInitialization()
	 * @see DefaultPcodeThread#doPluggableInitialization()
	 * @param language the language requiring pluggable initialization
	 * @return the initializer
	 */
	protected static PcodeStateInitializer getPluggableInitializer(Language language) {
		for (PcodeStateInitializer init : ClassSearcher.getInstances(PcodeStateInitializer.class)) {
			if (init.isApplicable(language)) {
				return init;
			}
		}
		return null;
	}

	/**
	 * Execute the initializer upon this machine, if applicable
	 * 
	 * @see #getPluggableInitializer(Language)
	 */
	protected void doPluggableInitialization() {
		if (initializer != null) {
			initializer.initializeMachine(this);
		}
	}

	@Override
	public PcodeThread<T> newThread() {
		return newThread("Thread " + threads.size());
	}

	@Override
	public PcodeThread<T> newThread(String name) {
		if (threads.containsKey(name)) {
			throw new IllegalStateException("Thread with name '" + name + "' already exists");
		}
		PcodeThread<T> thread = createThread(name);
		threads.put(name, thread);
		return thread;
	}

	@Override
	public PcodeThread<T> getThread(String name, boolean createIfAbsent) {
		PcodeThread<T> thread = threads.get(name);
		if (thread == null && createIfAbsent) {
			thread = newThread(name);
		}
		return thread;
	}

	@Override
	public Collection<? extends PcodeThread<T>> getAllThreads() {
		return threadsView;
	}

	@Override
	public PcodeExecutorState<T> getSharedState() {
		if (sharedState == null) {
			sharedState = createSharedState();
			doPluggableInitialization();
		}
		return sharedState;
	}

	/**
	 * Check for a p-code injection (override) at the given address
	 * 
	 * @param address the address, usually the program counter
	 * @return the injected program, most likely {@code null}
	 */
	protected PcodeProgram getInject(Address address) {
		return injects.get(address);
	}

	@Override
	public PcodeProgram compileSleigh(String sourceName, List<String> lines) {
		return SleighProgramCompiler.compileProgram(language, sourceName, lines, stubLibrary);
	}

	@Override
	public void inject(Address address, List<String> sleigh) {
		/**
		 * TODO: Can I compile the template and build as if the inject were a
		 * instruction:^instruction constructor? This would require me to delay that build until
		 * execution, or at least check for instruction modification, if I do want to cache the
		 * built p-code.
		 */
		PcodeProgram pcode = compileSleigh("machine_inject:" + address, sleigh);
		injects.put(address, pcode);
	}

	@Override
	public void clearInject(Address address) {
		injects.remove(address);
	}

	@Override
	public void clearAllInjects() {
		injects.clear();
	}

	@Override
	public void addBreakpoint(Address address, String sleighCondition) {
		/**
		 * TODO: The template build idea is probably more pertinent here. If a user places a
		 * breakpoint with the purpose of single-stepping the p-code of that instruction, it won't
		 * work, because that p-code is occluded by emu_exec_decoded(). I suppose this could also be
		 * addressed by formalizing and better exposing the notion of p-code stacks (of p-code
		 * frames)
		 */
		PcodeProgram pcode = compileSleigh("breakpoint:" + address, List.of(
			"if (!(" + sleighCondition + ")) goto <nobreak>;",
			"    emu_swi();",
			"<nobreak>",
			"    emu_exec_decoded();"));
		injects.put(address, pcode);
	}
}
