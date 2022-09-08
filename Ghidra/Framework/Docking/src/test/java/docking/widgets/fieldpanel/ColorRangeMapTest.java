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
package docking.widgets.fieldpanel;

import static org.junit.Assert.*;

import java.awt.Color;

import org.junit.Test;

import docking.widgets.fieldpanel.internal.ColorRangeMap;
import generic.test.AbstractGenericTest;
import generic.theme.GThemeDefaults.Colors.Palette;

public class ColorRangeMapTest extends AbstractGenericTest {
	public ColorRangeMapTest() {
		super();
	}

	@Test
	public void testPaint1() {
		ColorRangeMap map = new ColorRangeMap();
		map.color(10, 10, Palette.BLUE);
		assertColorsEqual(Palette.WHITE, map.getColor(0, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(9, Palette.WHITE));
		assertColorsEqual(Palette.BLUE, map.getColor(10, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(11, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(100, Palette.WHITE));
	}

	@Test
	public void testCopy() {
		ColorRangeMap map = new ColorRangeMap();
		map.color(10, 10, Palette.BLUE);
		assertColorsEqual(Palette.WHITE, map.getColor(0, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(9, Palette.WHITE));
		assertColorsEqual(Palette.BLUE, map.getColor(10, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(11, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(100, Palette.WHITE));

		map = map.copy();

		assertColorsEqual(Palette.WHITE, map.getColor(0, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(9, Palette.WHITE));
		assertColorsEqual(Palette.BLUE, map.getColor(10, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(11, Palette.WHITE));
		assertColorsEqual(Palette.WHITE, map.getColor(100, Palette.WHITE));
	}

	private void assertColorsEqual(Color c1, Color c2) {
		assertEquals(c1.getRGB(), c2.getRGB());
	}

}
