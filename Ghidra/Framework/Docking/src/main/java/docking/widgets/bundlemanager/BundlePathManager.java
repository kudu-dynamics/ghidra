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
package docking.widgets.bundlemanager;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import docking.widgets.filechooser.GhidraFileChooser;
import docking.widgets.filechooser.GhidraFileChooserMode;
import docking.widgets.table.*;
import generic.jar.ResourceFile;
import ghidra.framework.options.SaveState;
import ghidra.framework.preferences.Preferences;
import ghidra.util.filechooser.GhidraFileChooserModel;
import ghidra.util.filechooser.GhidraFileFilter;
import resources.ResourceManager;

/**
 * Component that has a table to show pathnames; the panel includes buttons to control
 * the order of the paths, and to add and remove paths. The add button brings up a
 * file chooser. Call the setFileChooser() method to control how the file chooser should
 * behave.  If the table entries should not be edited, call setEditingEnabled(false).
 */
public class BundlePathManager {
	private JPanel panel;
	private GTable bundlePathTable;
	private BundlePathManagerModel bundlePathModel;
	private TableModelListener bundlePathModelListener;
	private JButton addButton;
	private JButton removeButton;
	private Color selectionColor;
	private GhidraFileChooser fileChooser;
	private String preferenceForLastSelectedBundle = Preferences.LAST_IMPORT_DIRECTORY;
	private String title = "Select File";
	private GhidraFileFilter filter;
	private ArrayList<BundlePathManagerListener> listeners = new ArrayList<>();

	/**
	 * Construct a new BundlePathManager.
	 * @param paths list of paths to show; may be null
	 * if new paths are to be added to the end of the table
	 */
	public BundlePathManager(List<BundlePath> paths) {
		create(paths);
	}

	/**
	 * Set properties on the file chooser that is displayed when the "Add" button is pressed.
	 * @param title title of the file chooser
	 * @param preferenceForLastSelectedBundle Preference to use as the starting selection in the
	 * file chooser
	 */

	public void setFileChooserProperties(String title, String preferenceForLastSelectedBundle) {
		this.title = title;
		this.preferenceForLastSelectedBundle = preferenceForLastSelectedBundle;
		this.filter = new GhidraFileFilter() {
			@Override
			public String getDescription() {
				return "Source code directory, bundle (*.jar), or bnd script (*.bnd)";
			}

			@Override
			public boolean accept(File path, GhidraFileChooserModel model) {
				return BundlePath.getType(path) != BundlePath.Type.INVALID;
			}
		};
		this.fileChooser = null;
	}

	/**
	 * Return enabled paths in the table.
	 * @return enabled paths
	 */
	public List<BundlePath> getPaths() {
		return bundlePathModel.getPaths();
	}

	/**
	 * (add and) enable a path
	 * @param file path to enable 
	 * @return true if the path is new
	 */
	public boolean enablePath(ResourceFile file) {
		ResourceFile dir = file.isDirectory() ? file : file.getParentFile();
		for (BundlePath path : bundlePathModel.getAllPaths()) {
			if (path.getPath().equals(dir)) {
				if (!path.isEnabled()) {
					path.setEnabled(true);
					bundlePathModel.fireTableDataChanged();
					fireBundlesChanged();
					return true;
				}
				return false;
			}
		}
		BundlePath p = new BundlePath(dir);
		p.setEnabled(true);
		bundlePathModel.addPath(p);
		Preferences.setProperty(preferenceForLastSelectedBundle, dir.getAbsolutePath());
		fireBundlesChanged();
		return true;
	}

	public void setPaths(List<BundlePath> paths) {
		bundlePathModel.setPaths(paths);
	}

	/**
	 * Clear the paths in the table.
	 */
	public void clear() {
		bundlePathModel.clear();
	}

	public void addListener(BundlePathManagerListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(BundlePathManagerListener listener) {
		listeners.remove(listener);
	}

	public List<BundlePathManagerListener> getListeners() {
		return new ArrayList<>(listeners);
	}

	private void fireBundlesChanged() {
		for (BundlePathManagerListener listener : listeners) {
			listener.bundlesChanged();
		}
	}

	private void create(List<BundlePath> paths) {
		panel = new JPanel(new BorderLayout(5, 5));

		selectionColor = new Color(204, 204, 255);

		addButton = new JButton(ResourceManager.loadImage("images/Plus.png"));
		addButton.setName("AddBundle");
		addButton.setToolTipText("Display file chooser to add bundles to list");
		addButton.addActionListener(e -> add());
		addButton.setFocusable(false);

		removeButton = new JButton(ResourceManager.loadImage("images/edit-delete.png"));
		removeButton.setName("RemoveBundle");
		removeButton.setToolTipText("Remove selected bundle(s) from list");
		removeButton.addActionListener(e -> remove());
		removeButton.setFocusable(false);

		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridx = 0;
		gbc.gridy = 0;

		buttonPanel.add(addButton, gbc);
		++gbc.gridy;
		buttonPanel.add(removeButton, gbc);

		bundlePathModelListener = e -> fireBundlesChanged();

		bundlePathModel = new BundlePathManagerModel(this, paths);
		bundlePathModel.addTableModelListener(bundlePathModelListener);

		bundlePathTable = new GTable(bundlePathModel);
		bundlePathTable.setName("BUNDLEPATH_TABLE");
		bundlePathTable.setSelectionBackground(selectionColor);
		bundlePathTable.setSelectionForeground(Color.BLACK);
		bundlePathTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		int skinnyWidth = 50;

		TableColumn column =
			bundlePathTable.getColumnModel().getColumn(BundlePathManagerModel.COLUMN.Enabled.index);
		column.setPreferredWidth(skinnyWidth);
		column.setMinWidth(skinnyWidth);
		column.setMaxWidth(skinnyWidth);
		column.setWidth(skinnyWidth);

		column =
			bundlePathTable.getColumnModel().getColumn(BundlePathManagerModel.COLUMN.Active.index);
		column.setPreferredWidth(skinnyWidth);
		column.setMinWidth(skinnyWidth);
		column.setMaxWidth(skinnyWidth);
		column.setWidth(skinnyWidth);

		column =
			bundlePathTable.getColumnModel().getColumn(BundlePathManagerModel.COLUMN.Type.index);

		FontMetrics fontmetrics = panel.getFontMetrics(panel.getFont());
		column.setMaxWidth(10 +
			SwingUtilities.computeStringWidth(fontmetrics, BundlePath.Type.SourceDir.toString()));

		column =
			bundlePathTable.getColumnModel().getColumn(BundlePathManagerModel.COLUMN.Path.index);
		column.setCellRenderer(new GTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(GTableCellRenderingData data) {
				JLabel renderer = (JLabel) super.getTableCellRendererComponent(data);

				BundlePath path = (BundlePath) data.getValue();
				if (!path.exists()) {
					renderer.setForeground(Color.RED);
				}
				return renderer;
			}
		});

		GTableFilterPanel<BundlePath> filterPanel =
			new GTableFilterPanel<>(bundlePathTable, bundlePathModel);

		JScrollPane scrollPane = new JScrollPane(bundlePathTable);
		scrollPane.getViewport().setBackground(bundlePathTable.getBackground());

		ListSelectionModel selModel = bundlePathTable.getSelectionModel();
		selModel.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting()) {
				return;
			}
			updateButtonsEnabled();
		});
		updateButtonsEnabled();

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(scrollPane, BorderLayout.CENTER);
		centerPanel.add(filterPanel, BorderLayout.SOUTH);
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.EAST);
		panel.setPreferredSize(new Dimension(800, 400));
	}

	private void updateButtonsEnabled() {
		int[] rows = bundlePathTable.getSelectedRows();
		removeButton.setEnabled(rows.length > 0);
	}

	private void remove() {
		int[] selectedRows = bundlePathTable.getSelectedRows();
		if (selectedRows == null) {
			return;
		}
		bundlePathModel.remove(selectedRows);

		// select the next row based on what was selected
		Arrays.sort(selectedRows);
		int row = selectedRows[selectedRows.length - 1] + 1 - selectedRows.length;
		int count = bundlePathModel.getRowCount();
		if (row >= count) {
			row = count - 1;
		}
		if (row >= 0) {
			bundlePathTable.setRowSelectionInterval(row, row);
		}
		updateButtonsEnabled();
	}

	private void add() {
		if (fileChooser == null) {
			fileChooser = new GhidraFileChooser(panel);
			// XXX bad behavior w/ text box when multiselection is enabled
			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.setFileSelectionMode(GhidraFileChooserMode.FILES_AND_DIRECTORIES);
			fileChooser.setTitle(title);
			// fileChooser.setApproveButtonToolTipText(title);
			if (filter != null) {
				fileChooser.addFileFilter(new GhidraFileFilter() {
					@Override
					public String getDescription() {
						return filter.getDescription();
					}

					@Override
					public boolean accept(File f, GhidraFileChooserModel l_model) {
						return filter.accept(f, l_model);
					}
				});
			}
			String lastSelected = Preferences.getProperty(preferenceForLastSelectedBundle);
			if (lastSelected != null) {
				File f = new File(lastSelected);
				fileChooser.setSelectedFile(f);
			}
		}
		else {
			String lastSelected = Preferences.getProperty(preferenceForLastSelectedBundle);
			if (lastSelected != null) {
				File f = new File(lastSelected);
				fileChooser.setSelectedFile(f);
			}
			fileChooser.rescanCurrentDirectory();
		}

		List<File> files = fileChooser.getSelectedFiles();
		if (!files.isEmpty()) {
			Preferences.setProperty(preferenceForLastSelectedBundle,
				files.get(0).getAbsolutePath());
			for (File element : files) {
				BundlePath p = new BundlePath(element);
				bundlePathModel.addPath(p);
			}
		}
	}

	/**
	 * Returns the GUI component for the path manager.
	 * @return the GUI component for the path manager
	 */
	public JComponent getComponent() {
		return panel;
	}

	/**
	 * Saves the paths to the specified SaveState object.
	 * @param ss the SaveState object
	 */
	public void saveState(SaveState ss) {
		List<BundlePath> paths = bundlePathModel.getAllPaths();

		String[] pathArr = new String[paths.size()];
		boolean[] enableArr = new boolean[paths.size()];
		boolean[] editArr = new boolean[paths.size()];
		boolean[] readArr = new boolean[paths.size()];

		int index = 0;
		for (BundlePath path : paths) {
			pathArr[index] = path.getPathAsString();
			enableArr[index] = path.isEnabled();
			editArr[index] = path.isEditable();
			readArr[index] = path.isReadOnly();
			++index;
		}

		ss.putStrings("BundleManagerPanel_PATH", pathArr);
		ss.putBooleans("BundleManagerPanel_ENABLE", enableArr);
		ss.putBooleans("BundleManagerPanel_EDIT", editArr);
		ss.putBooleans("BundleManagerPanel_READ", readArr);
	}

	/**
	 * Restores the paths from the specified SaveState object.
	 * @param ss the SaveState object
	 */
	public void restoreState(SaveState ss) {
		String[] pathArr = ss.getStrings("BundleManagerPanel_PATH", new String[0]);

		if (pathArr.length == 0) {
			return;
		}

		/*
		 * Temporarily remove the listener to prevent too many
		 * notifications from being sent.
		 */
		bundlePathModel.removeTableModelListener(bundlePathModelListener);

		boolean[] enableArr =
			ss.getBooleans("BundleManagerPanel_ENABLE", new boolean[pathArr.length]);
		boolean[] editArr = ss.getBooleans("BundleManagerPanel_EDIT", new boolean[pathArr.length]);
		boolean[] readArr = ss.getBooleans("BundleManagerPanel_READ", new boolean[pathArr.length]);

		List<BundlePath> oldPaths = bundlePathModel.getAllPaths();
		bundlePathModel.clear();

		for (int i = 0; i < pathArr.length; i++) {
			BundlePath path = new BundlePath(pathArr[i], enableArr[i], editArr[i], readArr[i]);
			BundlePath oldPath = getPath(path.getPathAsString(), oldPaths);
			if (oldPath != null) {
				if (!oldPath.isEditable()) {
					boolean enabled = path.isEnabled();
					path = oldPath;
					path.setEnabled(enabled);
				}
				oldPaths.remove(oldPath);
			}
			else if (path.isReadOnly()) {
				// skip read-only paths which are not present in the current config
				// This is needed to thin-out old default entries
				continue;
			}
			bundlePathModel.addPath(path);
		}

		for (BundlePath path : oldPaths) {
			if (!path.isEditable()) {
				bundlePathModel.addPath(path);
			}
		}

		/*
		 * Reinstall the listener then fire the update.
		 */
		bundlePathModel.addTableModelListener(bundlePathModelListener);
		fireBundlesChanged();
	}

	private static BundlePath getPath(String filepath, List<BundlePath> paths) {
		for (BundlePath path : paths) {
			if (filepath.equals(path.getPathAsString())) {
				return path;
			}
		}
		return null;
	}

	public void dispose() {
		bundlePathTable.dispose();
	}
}
