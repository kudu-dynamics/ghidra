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
package ghidra.pdb.pdbreader.type;

import ghidra.pdb.PdbByteReader;
import ghidra.pdb.PdbException;
import ghidra.pdb.pdbreader.*;

public class UserDefinedTypeModuleSourceAndLineMsType extends AbstractMsType {

	public static final int PDB_ID = 0x1607;

	private AbstractTypeIndex udtTypeIndex;
	private AbstractTypeIndex sourceFileNameStringOffset;
	private int lineNumber;
	private int module;

	// TODO: For continued work with hash.
	//public static Set<Integer> sourceIdIndexList = new HashSet<>();

	/**
	 * Constructor for this type.
	 * @param pdb {@link AbstractPdb} to which this type belongs.
	 * @param reader {@link PdbByteReader} from which this type is deserialized.
	 * @throws PdbException Upon not enough data left to parse.
	 */
	public UserDefinedTypeModuleSourceAndLineMsType(AbstractPdb pdb, PdbByteReader reader)
			throws PdbException {
		super(pdb, reader);
		udtTypeIndex = new TypeIndex32();
		sourceFileNameStringOffset = new TypeIndex32();

		udtTypeIndex.parse(reader);
		pdb.pushDependencyStack(new CategoryIndex(CategoryIndex.Category.DATA, udtTypeIndex.get()));
		pdb.popDependencyStack();
		sourceFileNameStringOffset.parse(reader);
		lineNumber = reader.parseInt();
		module = reader.parseUnsignedShortVal();
		reader.skipPadding();
	}

	@Override
	public int getPdbId() {
		return PDB_ID;
	}

	@Override
	public void emit(StringBuilder builder, Bind bind) {
		// No good API.
		// TODO: Think their hash stuff might be superfluous... will see...
		// TODO: output module # for now.  Might need name lookup later.
		builder.append(UserDefinedTypeModuleSourceAndLineMsType.class.getSimpleName());
		builder.append(", module: ");
		builder.append(module);
		builder.append(", line: ");
		builder.append(lineNumber);
		builder.append(", sourceFileName: ");
		// I believe the following is correct source of string.  API unclear.
		builder.append(pdb.getNameStringFromOffset(sourceFileNameStringOffset.get()));
		builder.append(", type: ");
		builder.append(pdb.getTypeRecord(udtTypeIndex.get()));
	}

}
