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
package ghidra.app.plugin.core.decompile.actions;

import docking.action.MenuData;
import ghidra.app.decompiler.component.DecompilerPanel;
import ghidra.app.decompiler.component.TokenHighlights;
import ghidra.app.plugin.core.decompile.DecompilerActionContext;
import ghidra.app.util.HelpTopics;
import ghidra.program.model.listing.Function;
import ghidra.util.HelpLocation;

public class RemoveSecondaryHighlightsAction extends AbstractDecompilerAction {

	public static final String NAME = "Remove Secondary Highlights";

	public RemoveSecondaryHighlightsAction() {
		super(NAME);

		setPopupMenuData(new MenuData(new String[] { NAME }, "Decompile"));
		setHelpLocation(new HelpLocation(HelpTopics.SELECTION, getName()));
	}

	@Override
	protected boolean isEnabledForDecompilerContext(DecompilerActionContext context) {
		return context.hasRealFunction();
	}

	@Override
	protected void decompilerActionPerformed(DecompilerActionContext context) {
		Function function = context.getFunction();
		DecompilerPanel panel = context.getDecompilerPanel();
		TokenHighlights highlightTokens = panel.getHighlightedTokens();
		highlightTokens.remove(function);
	}
}
