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
package docking;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import docking.action.DockingActionIf;
import docking.actions.KeyBindingUtils;

/**
 * A class that can be used as an interface for using actions associated with keybindings.  This
 * class is meant to only by used by internal Ghidra key event processing.
 */
public class DockingKeyBindingAction extends AbstractAction {

	private DockingActionIf docakbleAction;

	protected KeyStroke keyStroke;
	protected final DockingWindowManager winMgr;

	public DockingKeyBindingAction(DockingWindowManager winMgr, DockingActionIf action,
			KeyStroke keyStroke) {
		super(KeyBindingUtils.parseKeyStroke(keyStroke));
		this.winMgr = winMgr;
		this.docakbleAction = action;
		this.keyStroke = keyStroke;
	}

	KeyStroke getKeyStroke() {
		return keyStroke;
	}

	@Override
	public boolean isEnabled() {
		// always enable; this is a reserved binding and cannot be disabled
		return true;
	}

	public boolean isReservedKeybindingPrecedence() {
		return getKeyBindingPrecedence() == KeyBindingPrecedence.ReservedActionsLevel;
	}

	public KeyBindingPrecedence getKeyBindingPrecedence() {
		return KeyBindingPrecedence.ReservedActionsLevel;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		winMgr.setStatusText("");
		ComponentProvider provider = winMgr.getActiveComponentProvider();
		ActionContext context = getLocalContext(provider);
		context.setSource(e.getSource());
		docakbleAction.actionPerformed(context);
	}

	protected ActionContext getLocalContext(ComponentProvider localProvider) {
		if (localProvider == null) {
			return new ActionContext();
		}

		ActionContext actionContext = localProvider.getActionContext(null);
		if (actionContext != null) {
			return actionContext;
		}

		return new ActionContext(localProvider, null);
	}
}
