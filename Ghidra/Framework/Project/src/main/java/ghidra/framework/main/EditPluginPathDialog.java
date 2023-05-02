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
/* Generated by Together */

package ghidra.framework.main;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import docking.DialogComponentProvider;
import docking.widgets.button.GButton;
import docking.widgets.filechooser.GhidraFileChooser;
import docking.widgets.filechooser.GhidraFileChooserMode;
import docking.widgets.label.GDLabel;
import docking.widgets.list.GListCellRenderer;
import generic.theme.GThemeDefaults.Colors.Messages;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.preferences.Preferences;
import ghidra.util.HelpLocation;
import ghidra.util.Msg;
import ghidra.util.filechooser.ExtensionFileFilter;
import ghidra.util.filechooser.GhidraFileFilter;
import resources.Icons;

/**
 * Dialog for editing the Plugin path and Jar directory path preferences.
 *
 * <p>The Plugin Path and Jar directory path are locations where Ghidra searches
 * for plugins to load. The Plugin Path is specified exactly as a Java Classpath
 * is specified.  When changes are made to these fields in the dialog, the
 * preferences file is updated and written to disk. The preferences file is
 * located in the .ghidra directory in the user's home directory.
 *
 */
class EditPluginPathDialog extends DialogComponentProvider {

	static final String ADD_DIR_BUTTON_TEXT = "Add Dir ...";
	static final String ADD_JAR_BUTTON_TEXT = "Add Jar ...";
	private final static Color STATUS_MESSAGE_COLOR = Messages.NORMAL;
	final static String EMPTY_STATUS = " ";

	private ExtensionFileFilter JAR_FILTER =
		new ExtensionFileFilter(new String[] { "jar", "zip" }, "Plugin Jar Files");

	// codes used when handling actions
	private final static byte UP = (byte) 0;
	private final static byte DOWN = (byte) 1;
	private final static byte REMOVE = (byte) 2;

	// state data
	private DefaultListModel<String> listModel; // paths to search for finding plugins
	private boolean pluginPathsChanged = false;

	// gui members needed for dis/enabling and other state-dependent actions
	private JScrollPane scrollPane; // need for preferred size when resizing
	private JList<String> pluginPathsList;

	private JButton upButton;
	private JButton downButton;
	private JButton removeButton;
	private List<String> selectedInList;
	private JLabel statusMessage;
	private JPanel mainPanel;
	private String errorMsg;

	/**
	 * Creates a non-modal dialog with OK, Apply, Cancel buttons.
	 * The OK and Apply buttons will be enabled when user makes unapplied
	 * changes to the UserPluginPath or UserPluginJarDirectory property values.
	 */
	EditPluginPathDialog() {
		super("Edit Plugin Path", true, false, true, false);
		setHelpLocation(new HelpLocation("FrontEndPlugin", "Edit_Plugin_Path"));
		addWorkPanel(buildMainPanel());
		addOKButton();
		addApplyButton();
		addCancelButton();
		// set model after the pack() so the dialog is sized properly
		pluginPathsList.setModel(listModel);
	}

	/**
	 * Define the Main panel for the dialog here.
	 * @return JPanel the completed <CODE>Main Panel</CODE>
	 */
	protected JPanel buildMainPanel() {
		// give base class the panel it needs to complete its construction
		// and then finish building after base class is done
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		listModel = new DefaultListModel<>();
		setPluginPathsListData(Preferences.getPluginPaths());

		// construct the bottom error message panel
		JPanel statusMessagePanel = new JPanel();
		statusMessage = new GDLabel("Ready to set User Plugin Paths");
		statusMessage.setName("statusLabel");

		statusMessage.setForeground(STATUS_MESSAGE_COLOR);
		statusMessagePanel.add(statusMessage);

		// put the main panel together
		// make sure to construct the plugin paths panel, since that
		// creates the scroll pane we use for sizing the text fields on
		// subsequent panels
		mainPanel.add(buildPluginPathsPanel());
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(Box.createVerticalGlue());
		mainPanel.add(statusMessagePanel);
		mainPanel.invalidate();

		// dialog sensitivity setup
		enableButtons(false);
		setApplyEnabled(false);

		return mainPanel;
	}

	@Override
	protected void applyCallback() {
		handleApply();
	}

	@Override
	protected void cancelCallback() {
		close();

		// reset original state of dialog for next display of dialog
		enableButtons(false);
		setStatusMessage(EMPTY_STATUS);
		setApplyEnabled(false);
		errorMsg = null;
	}

	/**
	 * Gets called when the user selects Ok
	 */
	@Override
	protected void okCallback() {
		if (isApplyEnabled()) {
			applyCallback();
		}
		if (errorMsg == null) {
			cancelCallback();
		}
	}

	/**
	 * Reset the list of paths each time the dialog is shown
	 * @param tool the tool
	 */
	public void show(PluginTool tool) {
		setPluginPathsListData(Preferences.getPluginPaths());
		setApplyEnabled(pluginPathsChanged);
		setStatusMessage(EMPTY_STATUS);

		// setting the path enables the apply, but we know we haven't
		// made any changes yet, so disable
		setApplyEnabled(false);
		tool.showDialog(this);
	}

	private void setStatusMessage(String msg) {
		if (msg == null || msg.length() == 0) {
			msg = EMPTY_STATUS;
		}
		statusMessage.setText(msg);
		statusMessage.invalidate();
	}

	private void addJarCallback() {

		setStatusMessage(EditPluginPathDialog.EMPTY_STATUS);

		GhidraFileChooser fileChooser = new GhidraFileChooser(getComponent());
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		fileChooser.setFileSelectionMode(GhidraFileChooserMode.FILES_ONLY);
		fileChooser.setFileFilter(JAR_FILTER);
		fileChooser.setApproveButtonToolTipText("Choose Plugin Jar File");
		fileChooser.setApproveButtonText("Add Jar File");

		fileChooser.setLastDirectoryPreference(Preferences.LAST_PATH_DIRECTORY);

		File dir = fileChooser.getSelectedFile();
		if (dir != null) {
			try {
				String dirPath = dir.getCanonicalPath();
				if (!listModel.contains(dirPath)) {
					listModel.addElement(dirPath);
					pluginPathsChanged = true;
					setApplyEnabled(true);
				}
				else {
					setStatusMessage(dirPath + " is already in the list.");
				}
			}
			catch (IOException e) {
				setStatusMessage(e.getMessage());
			}
		}

		fileChooser.dispose();
	}

	private void addDirCallback() {

		setStatusMessage(EditPluginPathDialog.EMPTY_STATUS);

		GhidraFileChooser fileChooser = new GhidraFileChooser(getComponent());
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		fileChooser.setFileSelectionMode(GhidraFileChooserMode.DIRECTORIES_ONLY);
		fileChooser.setFileFilter(GhidraFileFilter.ALL);
		fileChooser.setApproveButtonToolTipText("Choose Directory with Plugin class Files");
		fileChooser.setApproveButtonText("Add Directory");

		File dir = fileChooser.getSelectedFile();
		if (dir != null) {
			try {
				String dirPath = dir.getCanonicalPath();
				if (!listModel.contains(dirPath)) {
					listModel.addElement(dirPath);
					pluginPathsChanged = true;
					setApplyEnabled(true);
				}
				else {
					setStatusMessage(dirPath + " is already in the list.");
				}
			}
			catch (IOException e) {
				setStatusMessage(e.getMessage());
				Msg.error(this, "Unexpected Exception: " + e.getMessage(), e);
			}
		}
		fileChooser.dispose();
	}

	private String[] getUserPluginPaths() {
		String[] pluginsArray = new String[listModel.size()];
		listModel.copyInto(pluginsArray);
		return pluginsArray;
	}

	private JPanel buildPluginPathsPanel() {
		// create the UP and DOWN arrows panel
		upButton = new GButton(Icons.UP_ICON);
		upButton.setName("UpArrow");
		upButton.addActionListener(e -> handleSelection(UP));

		downButton = new GButton(Icons.DOWN_ICON);
		downButton.setName("DownArrow");
		downButton.addActionListener(e -> handleSelection(DOWN));
		JPanel arrowButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		arrowButtonsPanel.add(upButton);
		arrowButtonsPanel.add(downButton);

		// create the Add and Remove panel
		JButton addJarButton = new GButton(ADD_JAR_BUTTON_TEXT);
		addJarButton.addActionListener(e -> addJarCallback());
		JButton addDirButton = new GButton(ADD_DIR_BUTTON_TEXT);
		addDirButton.addActionListener(e -> addDirCallback());
		removeButton = new GButton("Remove");
		removeButton.addActionListener(e -> handleSelection(REMOVE));
		Dimension d = addJarButton.getPreferredSize();
		addDirButton.setPreferredSize(d);
		removeButton.setPreferredSize(d);

		//
		// Button panel for adding and removing jar files
		//
		JPanel otherButtonsPanel = new JPanel();
		JPanel subPanel = new JPanel();
		otherButtonsPanel.add(subPanel);
		int buttonGap = 10;
		subPanel.setLayout(new GridLayout(0, 1, 0, buttonGap));

		int top = 8;
		int side = 5;
		Border inside = BorderFactory.createEmptyBorder(top, side, top, side);
		subPanel.setBorder(inside);

		subPanel.add(addJarButton);
		subPanel.add(addDirButton);
		subPanel.add(removeButton);

		// put the right-side buttons panel together
		JPanel listButtonPanel = new JPanel(new BorderLayout(0, 0));
		listButtonPanel.add(arrowButtonsPanel, BorderLayout.NORTH);
		listButtonPanel.add(otherButtonsPanel, BorderLayout.CENTER);

		//
		// construct the plugin paths list
		//
		JPanel scrollListPanel = new JPanel(new BorderLayout(10, 15));
		pluginPathsList = new JList<>();
		pluginPathsList.addListSelectionListener(new PathListSelectionListener());
		pluginPathsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		// give the list a custom cell renderer that shows invalid paths
		// in red (may be preference paths set previously that are no longer
		// available
		pluginPathsList.setCellRenderer(new PluginPathRenderer());
		// component used for sizing all the text fields on the main panel
		scrollPane = new JScrollPane(pluginPathsList);
		scrollPane.setPreferredSize(new Dimension(250, 150));
		scrollListPanel.add(scrollPane, BorderLayout.CENTER);

		//
		// construct the plugin text panel
		//
		JPanel pluginPathListPanel = new JPanel(new BorderLayout(0, 0));
		pluginPathListPanel.add(scrollListPanel, BorderLayout.CENTER);
		pluginPathListPanel.add(listButtonPanel, BorderLayout.EAST);

		pluginPathListPanel.setBorder(new TitledBorder("User Plugin Paths"));

		// set tooltips after adding all components to get around swing
		// tooltip text problem where the text is obscured by a component
		// added after tooltip has been added
		//
		upButton.setToolTipText("Changes the order of search for plugins");
		downButton.setToolTipText("Changes the order of search for plugins");

		pluginPathListPanel.validate();
		return pluginPathListPanel;
	}

	private void enableButtons(boolean enabled) {
		upButton.setEnabled(enabled);
		downButton.setEnabled(enabled);
		removeButton.setEnabled(enabled);
	}

	/**
	 * done here so can be handled in a separate thread
	 */
	private void handleApply() {

		// get the data model as an array of strings
		String[] userPluginPaths = getUserPluginPaths();

		// update Ghidra Preferences with new paths
		Preferences.setPluginPaths(userPluginPaths);

		errorMsg = null;
		// save the new values
		if (Preferences.store()) {
			setStatusMessage("Saved plugin paths successfully!");
			// indicate to user all changes have been applied
			setApplyEnabled(false);

			Msg.showInfo(getClass(), rootPanel, "Restart Ghidra",
				"You must restart Ghidra in order\n" + "for path changes to take effect.");
		}
		else {
			setStatusMessage("");
			Msg.showError(this, rootPanel, "Error Saving Plugin Paths",
				"Failed to update user preferences (see log for details)");
		}
	}

	private void handleSelection(byte whichAction) {
		// if nothing selected, nothing to do
		if (selectedInList == null) {
			enableButtons(false);
			return;
		}

		// confirm removal of plugin path entries, since this may cause
		// previously saved tools to not be able to load their plugins
		if (whichAction == REMOVE) {

			// for each selected entry, do the specified action
			List<String> tempList = new ArrayList<>(selectedInList);
			for (String pathName : tempList) {
				int index = listModel.indexOf(pathName);
				if (index >= 0) {
					listModel.remove(index);
				}
			}
		}
		else {
			int newIndex = -1;
			int selIndex = pluginPathsList.getSelectedIndex();
			int size = listModel.size();
			String path = listModel.remove(selIndex);
			if (selIndex == 0) {
				// if UP, then place this index last,
				if (whichAction == UP) {
					listModel.add(listModel.size(), path);
					newIndex = size - 1;
				}
				else {
					// 	else place it 2nd
					listModel.add(1, path);
					newIndex = 1;
				}
			}
			else {
				// if UP, place selectedIndex at previous.
				if (whichAction == UP) {
					listModel.add(selIndex - 1, path);
					newIndex = selIndex - 1;
				}
				else {
					if (selIndex == size - 1) {
						// place this item first
						listModel.add(0, path);
						newIndex = 0;
					}
					else {
						// else place selected Index at next
						listModel.add(selIndex + 1, path);
						newIndex = selIndex + 1;
					}
				}
			}
			if (newIndex >= 0) {
				pluginPathsList.setSelectedIndex(newIndex);
			}
		}

		// alert user that something changed
		setApplyEnabled(true);

		if (whichAction == REMOVE) {
			enableButtons(false);
		}

	}

	/**
	 * ListCellRenderer that renders the path values in the list,
	 * coloring paths that are no longer readable in red.
	 */
	private class PluginPathRenderer extends GListCellRenderer<String> {

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value,
				int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			boolean pathOK = new File(value).canRead();
			if (!pathOK) {
				setForeground(getErrorForegroundColor(isSelected));
			}

			return this;
		}
	}

	private void setPluginPathsListData(String[] pluginPathNames) {
		listModel.clear();
		for (String pluginPathName : pluginPathNames) {
			listModel.addElement(pluginPathName);
		}
	}

	private class PathListSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) {
				return;
			}

			// reset state initially
			selectedInList = null;
			enableButtons(false);

			List<String> selectedValues = pluginPathsList.getSelectedValuesList();
			if (selectedValues.isEmpty()) {
				String path = pluginPathsList.getSelectedValue();
				if (path != null) {
					selectedValues = Collections.singletonList(path);
				}
			}

			removeButton.setEnabled(false);
			if (selectedValues != null) {
				int numSelected = selectedValues.size();
				selectedInList = new ArrayList<>(selectedValues);
				if (numSelected == 1) {
					// only if there are more than 1 paths in list
					// do we enable the UP and DOWN arrows
					if (listModel.size() > 1) {
						enableButtons(true);
					}
					else {
						removeButton.setEnabled(true);
					}
				}
				else if (numSelected > 0) {
					removeButton.setEnabled(true);
				}
			}
		}
	}
}
