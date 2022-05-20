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
package ghidra.pcode.exec;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ghidra.app.plugin.processors.sleigh.SleighLanguage;
import ghidra.program.model.lang.LanguageID;
import ghidra.test.AbstractGhidraHeadlessIntegrationTest;

public class PcodeFrameTest extends AbstractGhidraHeadlessIntegrationTest {
	static final List<String> SAMPLE_ADD = List.of(
		"r0 = r0 + r1;");
	static final List<String> SAMPLE_ADD2 = List.of(
		"r0 = r0 + r1 + r2;");
	static final List<String> SAMPLE_IF = List.of(
		"if (r0 == r1) goto <skip>;",
		"r2 = r2 + 1;",
		"<skip>");
	static final List<String> SAMPLE_LOOP = List.of(
		"<loop>",
		"r0 = r0 + 1;",
		"if (r0 == r1) goto <loop>;");
	static final List<String> SAMPLE_BRANCH = List.of(
		"goto 0x1234;");
	static final List<String> SAMPLE_LOAD = List.of(
		"r0 = *:8 r1;");
	static final List<String> SAMPLE_LANG_USEROP = List.of(
		"pcodeop_one(r0);");
	static final List<String> SAMPLE_LIB_USEROP = List.of(
		"__lib_userop(r0);");

	static class MyLib extends AnnotatedPcodeUseropLibrary<Void> {
		@PcodeUserop
		void __lib_userop() {
		}
	}

	SleighLanguage language;
	MyLib library = new MyLib();

	@Before
	public void setUp() throws Exception {
		language =
			(SleighLanguage) getLanguageService().getLanguage(new LanguageID("Toy:BE:64:default"));
	}

	private PcodeProgram compile(List<String> sample) {
		return SleighProgramCompiler.compileProgram(language, getName(), sample, library);
	}

	private PcodeFrame frame(List<String> sample) {
		PcodeProgram program = compile(sample);
		return new PcodeFrame(language, program.code, program.useropNames);
	}

	@Test
	public void testProgramToStringAdd() throws Exception {
		PcodeProgram program = compile(SAMPLE_ADD);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"  r0 = INT_ADD r0, r1\n" +
			">",
			program.toString());
	}

	@Test
	public void testProgramToStringAdd2() throws Exception {
		PcodeProgram program = compile(SAMPLE_ADD2);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"  $U2000:8 = INT_ADD r0, r1\n" +
			"  r0 = INT_ADD $U2000:8, r2\n" +
			">",
			program.toString());
	}

	@Test
	public void testProgramToStringIf() throws Exception {
		PcodeProgram program = compile(SAMPLE_IF);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"  $U2000:1 = INT_EQUAL r0, r1\n" +
			"  CBRANCH <0>, $U2000:1\n" +
			"  r2 = INT_ADD r2, 1:8\n" +
			"<0>\n" +
			">",
			program.toString());
	}

	@Test
	public void testProgramToStringLoop() throws Exception {
		PcodeProgram program = compile(SAMPLE_LOOP);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"<0>\n" +
			"  r0 = INT_ADD r0, 1:8\n" +
			"  $U2080:1 = INT_EQUAL r0, r1\n" +
			"  CBRANCH <0>, $U2080:1\n" +
			">",
			program.toString());
	}

	@Test
	public void testProgramToStringLoad() throws Exception {
		PcodeProgram program = compile(SAMPLE_LOAD);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"  r0 = LOAD ram(r1)\n" +
			">",
			program.toString());
	}

	@Test
	public void testProgramToStringLangUserop() throws Exception {
		PcodeProgram program = compile(SAMPLE_LANG_USEROP);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"  CALLOTHER \"pcodeop_one\", r0\n" +
			">",
			program.toString());
	}

	@Test
	public void testProgramToStringLibUserop() throws Exception {
		PcodeProgram program = compile(SAMPLE_LIB_USEROP);
		assertEquals("" +
			"<PcodeProgram:\n" +
			"  CALLOTHER \"__lib_userop\", r0\n" +
			">",
			program.toString());
	}

	@Test
	public void testFrameToStringAdd() throws Exception {
		PcodeFrame frame = frame(SAMPLE_ADD);
		assertEquals("" +
			"<p-code frame: index=0 {\n" +
			" -> r0 = INT_ADD r0, r1\n" +
			"}>",
			frame.toString());

		frame.advance();
		assertEquals("" +
			"<p-code frame: index=1 {\n" +
			"    r0 = INT_ADD r0, r1\n" +
			" *> fall-through\n" +
			"}>",
			frame.toString());
	}

	@Test
	public void testFrameToStringIf() throws Exception {
		PcodeFrame frame = frame(SAMPLE_IF);
		assertEquals("" +
			"<p-code frame: index=0 {\n" +
			" -> $U2000:1 = INT_EQUAL r0, r1\n" +
			"    CBRANCH <0>, $U2000:1\n" +
			"    r2 = INT_ADD r2, 1:8\n" +
			"  <0>\n" +
			"}>",
			frame.toString());

		frame.advance();
		frame.advance();
		frame.advance();
		assertEquals("" +
			"<p-code frame: index=3 {\n" +
			"    $U2000:1 = INT_EQUAL r0, r1\n" +
			"    CBRANCH <0>, $U2000:1\n" +
			"    r2 = INT_ADD r2, 1:8\n" +
			"  <0>\n" +
			" *> fall-through\n" +
			"}>",
			frame.toString());
	}

	@Test
	public void testFrameToStringLoop() throws Exception {
		PcodeFrame frame = frame(SAMPLE_LOOP);
		assertEquals("" +
			"<p-code frame: index=0 {\n" +
			"  <0>\n" +
			" -> r0 = INT_ADD r0, 1:8\n" +
			"    $U2080:1 = INT_EQUAL r0, r1\n" +
			"    CBRANCH <0>, $U2080:1\n" +
			"}>",
			frame.toString());
	}

	@Test
	public void testFrameToStringBranch() throws Exception {
		PcodeFrame frame = frame(SAMPLE_BRANCH);
		assertEquals("" +
			"<p-code frame: index=0 {\n" +
			" -> BRANCH *[ram]0x1234:8\n" +
			"}>",
			frame.toString());

		frame.advance();
		frame.finishAsBranch();
		assertEquals("" +
			"<p-code frame: index=-1 branched=0 {\n" +
			" *> BRANCH *[ram]0x1234:8\n" +
			"}>",
			frame.toString());
	}

	@Test
	public void testFrameToStringLangUserop() throws Exception {
		PcodeFrame frame = frame(SAMPLE_LANG_USEROP);
		assertEquals("" +
			"<p-code frame: index=0 {\n" +
			" -> CALLOTHER \"pcodeop_one\", r0\n" +
			"}>",
			frame.toString());
	}

	@Test
	public void testFrameToStringLibUserop() throws Exception {
		PcodeFrame frame = frame(SAMPLE_LIB_USEROP);
		assertEquals("" +
			"<p-code frame: index=0 {\n" +
			" -> CALLOTHER \"__lib_userop\", r0\n" +
			"}>",
			frame.toString());
	}
}
