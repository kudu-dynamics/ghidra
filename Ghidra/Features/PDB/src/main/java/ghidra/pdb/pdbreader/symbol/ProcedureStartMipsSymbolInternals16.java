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
package ghidra.pdb.pdbreader.symbol;

import ghidra.pdb.PdbByteReader;
import ghidra.pdb.PdbException;
import ghidra.pdb.pdbreader.AbstractPdb;
import ghidra.pdb.pdbreader.StringUtf8St;

/**
 * This class represents <B>16</B> Internals of the Procedure Start MIPS symbol.
 * <P>
 * Note: we do not necessarily understand each of these symbol type classes.  Refer to the
 *  base class for more information.
 */
public class ProcedureStartMipsSymbolInternals16 extends AbstractProcedureStartMipsSymbolInternals {

	/**
	 * Constructor for this symbol internals.
	 * @param pdb {@link AbstractPdb} to which this symbol belongs.
	 */
	public ProcedureStartMipsSymbolInternals16(AbstractPdb pdb) {
		super(pdb);
	}

	@Override
	protected void create() {
		name = new StringUtf8St();
	}

	@Override
	protected void parseTypeIndexAndSymbolSegmentOffset(PdbByteReader reader) throws PdbException {
		// Note parsing order and sizes.
		symbolOffset = reader.parseUnsignedIntVal();
		symbolSegment = reader.parseUnsignedShortVal();
		typeIndex = reader.parseUnsignedShortVal();
	}

}
