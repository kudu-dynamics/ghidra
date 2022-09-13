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
package docking.theme.gui;

import java.awt.Component;
import java.awt.Font;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JLabel;

import docking.widgets.table.*;
import generic.theme.*;
import ghidra.docking.settings.Settings;
import ghidra.framework.plugintool.ServiceProvider;
import ghidra.framework.plugintool.ServiceProviderStub;
import ghidra.util.table.column.AbstractGColumnRenderer;
import ghidra.util.table.column.GColumnRenderer;

/**
 * Table model for theme fonts
 */
public class ThemeFontTableModel extends GDynamicColumnTableModel<FontValue, Object> {
	private List<FontValue> fonts;
	private GThemeValueMap currentValues;
	private GThemeValueMap themeValues;
	private GThemeValueMap defaultValues;

	public ThemeFontTableModel() {
		super(new ServiceProviderStub());
		load();
	}

	/**
	 * Reloads the just the current values shown in the table. Called whenever a font changes.
	 */
	public void reloadCurrent() {
		currentValues = Gui.getAllValues();
		fonts = currentValues.getFonts();
		fireTableDataChanged();
	}

	/**
	 * Reloads all the current values and all the default values in the table. Called when the
	 * theme changes or the application defaults have been forced to reload.
	 */
	public void reloadAll() {
		load();
		fireTableDataChanged();
	}

	private void load() {
		currentValues = Gui.getAllValues();
		fonts = currentValues.getFonts();
		themeValues = new GThemeValueMap(currentValues);
		defaultValues = Gui.getDefaults();
	}

	@Override
	public String getName() {
		return "Fonts";
	}

	@Override
	public List<FontValue> getModelData() {
		return fonts;
	}

	@Override
	protected TableColumnDescriptor<FontValue> createTableColumnDescriptor() {
		TableColumnDescriptor<FontValue> descriptor = new TableColumnDescriptor<>();
		descriptor.addVisibleColumn(new IdColumn());
		descriptor.addVisibleColumn(new FontValueColumn("Current Font", () -> currentValues));
		descriptor.addVisibleColumn(new FontValueColumn("Theme Font", () -> themeValues));
		descriptor.addVisibleColumn(new FontValueColumn("Default Font", () -> defaultValues));
		return descriptor;
	}

	@Override
	public Object getDataSource() {
		return null;
	}

	private String getValueText(FontValue fontValue) {
		if (fontValue == null) {
			return "<No Value>";
		}
		if (fontValue.getReferenceId() != null) {
			return "[" + fontValue.getReferenceId() + "]";
		}

		Font font = fontValue.getRawValue();
		return FontValue.fontToString(font);
	}

	class IdColumn extends AbstractDynamicTableColumn<FontValue, String, Object> {

		@Override
		public String getColumnName() {
			return "Id";
		}

		@Override
		public String getValue(FontValue fontValue, Settings settings, Object data,
				ServiceProvider provider) throws IllegalArgumentException {
			return fontValue.getId();
		}

		@Override
		public int getColumnPreferredWidth() {
			return 300;
		}
	}

	class FontValueColumn extends AbstractDynamicTableColumn<FontValue, FontValue, Object> {
		private ThemeFontRenderer renderer;
		private String name;
		private Supplier<GThemeValueMap> valueSupplier;

		FontValueColumn(String name, Supplier<GThemeValueMap> supplier) {
			this.name = name;
			this.valueSupplier = supplier;
			renderer = new ThemeFontRenderer();
		}

		@Override
		public String getColumnName() {
			return name;
		}

		@Override
		public FontValue getValue(FontValue fontValue, Settings settings, Object data,
				ServiceProvider provider) throws IllegalArgumentException {
			GThemeValueMap valueMap = valueSupplier.get();
			String id = fontValue.getId();
			return valueMap.getFont(id);
		}

		@Override
		public GColumnRenderer<FontValue> getColumnRenderer() {
			return renderer;
		}

		public Comparator<FontValue> getComparator() {
			return (v1, v2) -> {
				if (v1 == null && v2 == null) {
					return 0;
				}
				if (v1 == null) {
					return 1;
				}
				if (v2 == null) {
					return -1;
				}
				return getValueText(v1).compareTo(getValueText(v2));
			};
		}

		@Override
		public int getColumnPreferredWidth() {
			return 300;
		}

	}

	private class ThemeFontRenderer extends AbstractGColumnRenderer<FontValue> {

		@Override
		public Component getTableCellRendererComponent(GTableCellRenderingData data) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(data);
			FontValue fontValue = (FontValue) data.getValue();

			String text = getValueText(fontValue);
			label.setText(text);
			label.setOpaque(true);
			return label;
		}

		@Override
		public String getFilterString(FontValue fontValue, Settings settings) {
			return getValueText(fontValue);
		}

	}

	/**
	 * Returns the original value for the id as defined by the current theme
	 * @param id the resource id to get a font value for
	 * @return  the original value for the id as defined by the current theme
	 */
	public FontValue getThemeValue(String id) {
		return themeValues.getFont(id);
	}

}
