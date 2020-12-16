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
package ghidra.app.plugin.core.debug.gui.thread;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

import docking.ActionContext;
import docking.WindowPosition;
import docking.action.*;
import docking.widgets.EventTrigger;
import docking.widgets.HorizontalTabPanel;
import docking.widgets.HorizontalTabPanel.TabListCellRenderer;
import docking.widgets.table.*;
import ghidra.app.plugin.core.debug.DebuggerCoordinates;
import ghidra.app.plugin.core.debug.DebuggerPluginPackage;
import ghidra.app.plugin.core.debug.gui.DebuggerResources;
import ghidra.app.plugin.core.debug.gui.DebuggerResources.*;
import ghidra.app.plugin.core.debug.gui.DebuggerSnapActionContext;
import ghidra.app.plugin.core.debug.gui.thread.DebuggerThreadsTimelinePanel.VetoableSnapRequestListener;
import ghidra.app.services.*;
import ghidra.app.services.DebuggerTraceManagerService.BooleanChangeAdapter;
import ghidra.framework.model.DomainObject;
import ghidra.framework.plugintool.AutoService;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.annotation.AutoServiceConsumed;
import ghidra.trace.model.Trace;
import ghidra.trace.model.Trace.TraceSnapshotChangeType;
import ghidra.trace.model.Trace.TraceThreadChangeType;
import ghidra.trace.model.TraceDomainObjectListener;
import ghidra.trace.model.thread.TraceThread;
import ghidra.trace.model.thread.TraceThreadManager;
import ghidra.trace.model.time.TraceSnapshot;
import ghidra.util.Swing;
import ghidra.util.datastruct.CollectionChangeListener;
import ghidra.util.table.GhidraTable;
import ghidra.util.table.GhidraTableFilterPanel;
import utilities.util.SuppressableCallback;
import utilities.util.SuppressableCallback.Suppression;

public class DebuggerThreadsProvider extends ComponentProviderAdapter {

	protected static long orZero(Long l) {
		return l == null ? 0 : l;
	}

	protected static boolean sameCoordinates(DebuggerCoordinates a, DebuggerCoordinates b) {
		if (!Objects.equals(a.getTrace(), b.getTrace())) {
			return false;
		}
		if (!Objects.equals(a.getRecorder(), b.getRecorder())) {
			return false; // For live read/writes
		}
		if (!Objects.equals(a.getThread(), b.getThread())) {
			return false;
		}
		if (!Objects.equals(a.getSnap(), b.getSnap())) {
			return false;
		}
		// TODO: Ticks
		return true;
	}

	protected class StepTraceBackwardAction extends AbstractStepTraceBackwardAction {
		public static final String GROUP = DebuggerResources.GROUP_GENERAL;

		public StepTraceBackwardAction() {
			super(plugin);
			setToolBarData(new ToolBarData(ICON, GROUP));
			addLocalAction(this);
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionContext context) {
			traceManager.activateSnap(current.getSnap() - 1);
		}

		@Override
		public boolean isEnabledForContext(ActionContext context) {
			if (current.getTrace() == null) {
				return false;
			}
			// TODO: Can I track minSnap?
			if (current.getSnap() <= 0) {
				return false;
			}
			return true;
		}
	}

	protected class StepTraceForwardAction extends AbstractStepTraceForwardAction {
		public static final String GROUP = DebuggerResources.GROUP_GENERAL;

		public StepTraceForwardAction() {
			super(plugin);
			setToolBarData(new ToolBarData(ICON, GROUP));
			addLocalAction(this);
			setEnabled(false);
		}

		@Override
		public void actionPerformed(ActionContext context) {
			traceManager.activateSnap(current.getSnap() + 1);
		}

		@Override
		public boolean isEnabledForContext(ActionContext context) {
			Trace curTrace = current.getTrace();
			if (curTrace == null) {
				return false;
			}
			Long maxSnap = curTrace.getTimeManager().getMaxSnap();
			if (maxSnap == null || current.getSnap() >= maxSnap) {
				return false;
			}
			return true;
		}
	}

	protected class SeekTracePresentAction extends AbstractSeekTracePresentAction
			implements BooleanChangeAdapter {
		public static final String GROUP = DebuggerResources.GROUP_GENERAL;

		public SeekTracePresentAction() {
			super(plugin);
			setToolBarData(new ToolBarData(ICON, GROUP));
			addLocalAction(this);
			setSelected(traceManager == null ? false : traceManager.isAutoActivatePresent());
			traceManager.addAutoActivatePresentChangeListener(this);
		}

		@Override
		public boolean isEnabledForContext(ActionContext context) {
			return traceManager != null;
		}

		@Override
		public void actionPerformed(ActionContext context) {
			if (traceManager == null) {
				return;
			}
			traceManager.setAutoActivatePresent(isSelected());
		}

		@Override
		public void changed(Boolean value) {
			if (isSelected() == value) {
				return;
			}
			setSelected(value);
		}
	}

	private class ThreadsListener extends TraceDomainObjectListener {
		public ThreadsListener() {
			listenForUntyped(DomainObject.DO_OBJECT_RESTORED, e -> objectRestored());

			listenFor(TraceThreadChangeType.ADDED, this::threadAdded);
			listenFor(TraceThreadChangeType.CHANGED, this::threadChanged);
			listenFor(TraceThreadChangeType.LIFESPAN_CHANGED, this::threadChanged);
			listenFor(TraceThreadChangeType.DELETED, this::threadDeleted);

			listenFor(TraceSnapshotChangeType.ADDED, this::snapAdded);
			listenFor(TraceSnapshotChangeType.DELETED, this::snapDeleted);
		}

		private void objectRestored() {
			loadThreads();
		}

		private void threadAdded(TraceThread thread) {
			threadMap.computeIfAbsent(thread, t -> {
				ThreadRow tr = new ThreadRow(modelService, t);
				threadTableModel.add(tr);
				doSetThread(thread);
				return tr;
			});
		}

		private void threadChanged(TraceThread thread) {
			threadTableModel.notifyUpdatedWith(row -> row.getThread() == thread);
		}

		private void threadDeleted(TraceThread thread) {
			ThreadRow tr = threadMap.remove(thread);
			if (tr != null) {
				threadTableModel.delete(tr);
			}
		}

		private void snapAdded(TraceSnapshot snapshot) {
			updateTimelineMax();
			contextChanged();
		}

		private void snapDeleted() {
			updateTimelineMax();
		}
	}

	private class RecordersChangeListener implements CollectionChangeListener<TraceRecorder> {
		@Override
		public void elementAdded(TraceRecorder element) {
			Swing.runIfSwingOrRunLater(() -> traceTabs.repaint());
		}

		@Override
		public void elementModified(TraceRecorder element) {
			Swing.runIfSwingOrRunLater(() -> traceTabs.repaint());
		}

		@Override
		public void elementRemoved(TraceRecorder element) {
			Swing.runIfSwingOrRunLater(() -> traceTabs.repaint());
		}
	}

	private final DebuggerThreadsPlugin plugin;

	// @AutoServiceConsumed  by method
	private DebuggerModelService modelService;
	@AutoServiceConsumed // NB, also by method
	private DebuggerTraceManagerService traceManager;
	@SuppressWarnings("unused")
	private final AutoService.Wiring autoWiring;

	DebuggerCoordinates current = DebuggerCoordinates.NOWHERE;
	private Trace currentTrace; // Copy for transition
	private final SuppressableCallback<Void> cbCoordinateActivation = new SuppressableCallback<>();

	private final ThreadsListener threadsListener = new ThreadsListener();
	private final VetoableSnapRequestListener snapListener = this::snapRequested;
	private final CollectionChangeListener<TraceRecorder> recordersListener =
		new RecordersChangeListener();

	protected final Map<TraceThread, ThreadRow> threadMap = new HashMap<>();
	protected final EnumeratedColumnTableModel<ThreadRow> threadTableModel =
		new DefaultEnumeratedColumnTableModel<>("Threads", ThreadTableColumns.class);

	private JPanel mainPanel;
	private JSplitPane splitPane;

	HorizontalTabPanel<Trace> traceTabs;
	GTable threadTable;
	GhidraTableFilterPanel<ThreadRow> threadFilterPanel;
	DebuggerThreadsTimelinePanel threadTimeline;
	JPopupMenu traceTabPopupMenu;

	private ActionContext myActionContext;

	DockingAction actionSaveTrace;
	StepTraceBackwardAction actionStepTraceBackward;
	StepTraceForwardAction actionStepTraceForward;
	SeekTracePresentAction actionSeekTracePresent;
	ToggleDockingAction actionSyncFocus;
	Set<Object> strongRefs = new HashSet<>(); // Eww

	public DebuggerThreadsProvider(final DebuggerThreadsPlugin plugin) {
		super(plugin.getTool(), DebuggerResources.TITLE_PROVIDER_THREADS, plugin.getName());
		this.plugin = plugin;

		this.autoWiring = AutoService.wireServicesConsumed(plugin, this);

		setIcon(DebuggerResources.ICON_PROVIDER_THREADS);
		setHelpLocation(DebuggerResources.HELP_PROVIDER_THREADS);
		setWindowMenuGroup(DebuggerPluginPackage.NAME);

		buildMainPanel();

		// TODO: Consider a custom cell renderer in the table instead of a timeline widget?
		// TODO: Should I receive clicks on that renderer to seek to a given snap?
		setDefaultWindowPosition(WindowPosition.BOTTOM);

		myActionContext = new DebuggerSnapActionContext(0);
		createActions();
		contextChanged();

		setVisible(true);
	}

	private <T> T strongRef(T t) {
		strongRefs.add(t);
		return t;
	}

	@AutoServiceConsumed
	public void setModelService(DebuggerModelService modelService) {
		if (this.modelService != null) {
			this.modelService.removeTraceRecordersChangedListener(recordersListener);
		}
		this.modelService = modelService;
		if (this.modelService != null) {
			this.modelService.addTraceRecordersChangedListener(recordersListener);
		}
	}

	@AutoServiceConsumed
	public void setTraceManager(DebuggerTraceManagerService traceManager) {
		if (traceManager != null && actionSeekTracePresent != null) {
			actionSeekTracePresent.setSelected(traceManager.isAutoActivatePresent());
			actionSyncFocus.setSelected(traceManager.isSynchronizeFocus());
		}
		contextChanged();
	}

	private void removeOldListeners() {
		if (currentTrace == null) {
			return;
		}
		currentTrace.removeListener(threadsListener);
	}

	private void addNewListeners() {
		if (currentTrace == null) {
			return;
		}
		currentTrace.addListener(threadsListener);
	}

	private void doSetTrace(Trace trace) {
		if (currentTrace == trace) {
			return;
		}
		removeOldListeners();
		currentTrace = trace;
		addNewListeners();

		try (Suppression supp = cbCoordinateActivation.suppress(null)) {
			traceTabs.setSelectedItem(trace);
		}
		loadThreads();
	}

	private void doSetThread(TraceThread thread) {
		ThreadRow row = threadFilterPanel.getSelectedItem();
		TraceThread curThread = row == null ? null : row.getThread();
		if (curThread == thread) {
			return;
		}
		try (Suppression supp = cbCoordinateActivation.suppress(null)) {
			if (thread != null) {
				threadFilterPanel.setSelectedItem(threadMap.get(thread));
			}
			else {
				threadTable.clearSelection();
			}
		}
	}

	private void doSetSnap(long snap) {
		if (threadTimeline.getSnap() == snap) {
			return;
		}
		threadTimeline.setSnap(snap);
	}

	public void traceOpened(Trace trace) {
		traceTabs.addItem(trace);
	}

	public void traceClosed(Trace trace) {
		traceTabs.removeItem(trace);
		// manager will issue activate-null event if current trace is closed
	}

	public void coordinatesActivated(DebuggerCoordinates coordinates) {
		if (sameCoordinates(current, coordinates)) {
			current = coordinates;
			return;
		}

		current = coordinates;

		doSetTrace(current.getTrace());
		doSetThread(current.getThread());
		doSetSnap(current.getSnap());
		contextChanged();
	}

	protected void loadThreads() {
		threadMap.clear();
		threadTableModel.clear();
		Trace curTrace = current.getTrace();
		if (curTrace == null) {
			return;
		}
		TraceThreadManager manager = curTrace.getThreadManager();
		for (TraceThread thread : manager.getAllThreads()) {
			threadMap.computeIfAbsent(thread, t -> {
				ThreadRow tr = new ThreadRow(modelService, t);
				threadTableModel.add(tr);
				return tr;
			});
		}
		updateTimelineMax();
	}

	protected void updateTimelineMax() {
		threadTimeline.setMaxSnapAtLeast(orZero(current.getTrace().getTimeManager().getMaxSnap()));
	}

	@Override
	public void addLocalAction(DockingActionIf action) {
		super.addLocalAction(action);
	}

	@Override
	public ActionContext getActionContext(MouseEvent event) {
		if (myActionContext == null) {
			return super.getActionContext(event);
		}
		return myActionContext;
	}

	private void snapRequested(long req, EventTrigger trigger) {
		long snap = req;
		if (snap < 0) {
			snap = 0;
		}
		if (current.getTrace() == null) {
			snap = 0;
		}
		/*else {
			Long maxSnap = currentTrace.getTimeManager().getMaxSnap();
			if (maxSnap == null) {
				maxSnap = 0L;
			}
			if (snap > maxSnap) {
				snap = maxSnap;
			}
		}*/
		if (trigger == EventTrigger.GUI_ACTION) {
			traceManager.activateSnap(snap);
		}
		myActionContext = new DebuggerSnapActionContext(snap);
		contextChanged();
	}

	protected void buildMainPanel() {
		traceTabPopupMenu = new JPopupMenu("Trace");

		mainPanel = new JPanel(new BorderLayout());
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setContinuousLayout(true);

		JPanel tablePanel = new JPanel(new BorderLayout());
		threadTable = new GhidraTable(threadTableModel);
		threadTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tablePanel.add(new JScrollPane(threadTable));
		threadFilterPanel = new GhidraTableFilterPanel<>(threadTable, threadTableModel);
		tablePanel.add(threadFilterPanel, BorderLayout.SOUTH);
		splitPane.setLeftComponent(tablePanel);

		threadTimeline = new DebuggerThreadsTimelinePanel(threadFilterPanel.getTableFilterModel());
		splitPane.setRightComponent(threadTimeline);

		splitPane.setResizeWeight(0.4);

		threadTable.getSelectionModel().addListSelectionListener(this::threadRowSelected);
		threadTimeline.setSelectionModel(threadTable.getSelectionModel());
		threadTimeline.addSnapRequestedListener(snapListener);

		mainPanel.add(splitPane, BorderLayout.CENTER);

		traceTabs = new HorizontalTabPanel<>();
		traceTabs.getList().setCellRenderer(new TabListCellRenderer<>() {
			protected String getText(Trace value) {
				return value.getName();
			}

			protected Icon getIcon(Trace value) {
				if (modelService == null) {
					return super.getIcon(value);
				}
				TraceRecorder recorder = modelService.getRecorder(value);
				if (recorder == null || !recorder.isRecording()) {
					return super.getIcon(value);
				}
				return DebuggerResources.ICON_RECORD;
			}
		});
		JList<Trace> list = traceTabs.getList();
		list.getSelectionModel().addListSelectionListener(this::traceTabSelected);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				checkTraceTabPopupViaMouse(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				checkTraceTabPopupViaMouse(e);
			}
		});
		// TODO: The popup key? Only seems to have rawCode=0x93 (147) in Swing
		mainPanel.add(traceTabs, BorderLayout.NORTH);
	}

	private void checkTraceTabPopupViaMouse(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		JList<Trace> list = traceTabs.getList();
		int i = list.locationToIndex(e.getPoint());
		if (i < 0) {
			return;
		}
		Rectangle cell = list.getCellBounds(i, i);
		if (!cell.contains(e.getPoint())) {
			return;
		}
		showTraceTabPopup(e.getComponent(), e.getPoint(), i);
	}

	private void showTraceTabPopup(Component comp, Point p, int i) {
		// TODO: Make this use action contexts and docking actions instead
		traceTabPopupMenu.removeAll();
		final Trace trace = traceTabs.getItem(i);
		JMenuItem closeItem =
			new JMenuItem("Close " + trace.getName(), DebuggerResources.ICON_CLOSE);
		closeItem.addActionListener(evt -> {
			traceManager.closeTrace(trace);
		});
		JMenuItem closeOthers =
			new JMenuItem("Close Others", DebuggerResources.ICON_CLOSE);
		closeOthers.addActionListener(evt -> {
			traceManager.closeOtherTraces(trace);
		});
		JMenuItem closeDead =
			new JMenuItem("Close Dead", DebuggerResources.ICON_CLOSE);
		closeDead.addActionListener(evt -> {
			traceManager.closeDeadTraces();
		});
		JMenuItem closeAll =
			new JMenuItem("Close All", DebuggerResources.ICON_CLOSE);
		closeAll.addActionListener(evt -> {
			for (Trace t : List.copyOf(traceManager.getOpenTraces())) {
				traceManager.closeTrace(t);
			}
		});
		traceTabPopupMenu.add(closeItem);
		traceTabPopupMenu.addSeparator();
		traceTabPopupMenu.add(closeOthers);
		traceTabPopupMenu.add(closeDead);
		traceTabPopupMenu.add(closeAll);
		traceTabPopupMenu.show(comp, p.x, p.y);
	}

	protected void createActions() {
		// TODO: Make other actions use builder?
		actionStepTraceBackward = new StepTraceBackwardAction();
		actionStepTraceForward = new StepTraceForwardAction();
		actionSeekTracePresent = new SeekTracePresentAction();
		actionSyncFocus = SynchronizeFocusAction.builder(plugin)
				.selected(traceManager != null && traceManager.isSynchronizeFocus())
				.enabledWhen(c -> traceManager != null)
				.onAction(c -> toggleSyncFocus(actionSyncFocus.isSelected()))
				.buildAndInstallLocal(this);
		traceManager.addSynchronizeFocusChangeListener(
			strongRef(new ToToggleSelectionListener(actionSyncFocus)));
	}

	private void toggleSyncFocus(boolean enabled) {
		if (traceManager == null) {
			return;
		}
		traceManager.setSynchronizeFocus(enabled);
	}

	private void traceTabSelected(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}
		Trace newTrace = traceTabs.getSelectedItem();
		myActionContext = new DebuggerThreadActionContext(newTrace, null);
		contextChanged();
		cbCoordinateActivation.invoke(() -> traceManager.activateTrace(newTrace));
	}

	private void threadRowSelected(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}
		ThreadRow row = threadFilterPanel.getSelectedItem();
		myActionContext = new DebuggerThreadActionContext(current.getTrace(),
			row == null ? null : row.getThread());
		contextChanged();
		if (row != null && traceManager != null) {
			cbCoordinateActivation.invoke(() -> traceManager.activateThread(row.getThread()));
		}
	}

	@Override
	public JComponent getComponent() {
		return mainPanel;
	}
}
