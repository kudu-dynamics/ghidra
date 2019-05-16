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
import ghidra.pdb.pdbreader.*;

/**
 * This class represents various flavors of Procedure Start IA64 symbol.
 * <P>
 * Note: we do not necessarily understand each of these symbol type classes.  Refer to the
 *  base class for more information.
 */
public abstract class AbstractProcedureStartIa64MsSymbol extends AbstractMsSymbol {

	protected long parentPointer;
	protected long endPointer;
	protected long nextPointer;
	protected long procedureLength;
	protected long debugStartOffset;
	protected long debugEndOffset;
	protected int typeIndex;
	protected long symbolOffset;
	protected int symbolSegment;
	protected int registerIndexContainingReturnValue;
	protected RegisterName registerContainingReturnValue;
	protected ProcedureFlags procedureFlags;
	protected AbstractString name;

	/**
	 * Constructor for this symbol.
	 * @param pdb {@link AbstractPdb} to which this symbol belongs.
	 * @param reader {@link PdbByteReader} from which this symbol is deserialized.
	 * @throws PdbException upon error parsing a field.
	 */
	public AbstractProcedureStartIa64MsSymbol(AbstractPdb pdb, PdbByteReader reader)
			throws PdbException {
		super(pdb, reader);
		name = create();
		parentPointer = reader.parseUnsignedIntVal();
		endPointer = reader.parseUnsignedIntVal();
		nextPointer = reader.parseUnsignedIntVal();
		procedureLength = reader.parseUnsignedIntVal();
		debugStartOffset = reader.parseUnsignedIntVal();
		debugEndOffset = reader.parseUnsignedIntVal();
		typeIndex = reader.parseInt();
		pdb.pushDependencyStack(new CategoryIndex(CategoryIndex.Category.DATA, typeIndex));
		pdb.popDependencyStack();
		symbolOffset = reader.parseUnsignedIntVal();
		symbolSegment = reader.parseUnsignedShortVal();
		registerIndexContainingReturnValue = reader.parseUnsignedShortVal();
		registerContainingReturnValue = new RegisterName(pdb, registerIndexContainingReturnValue);
		procedureFlags = new ProcedureFlags(reader);
		name.parse(reader);
	}

	@Override
	public void emit(StringBuilder builder) {
		builder.append(String.format("%s: [%04X:%08X], Length: %08X, %s: %s, ", getSymbolTypeName(),
			symbolSegment, symbolOffset, procedureLength, getSpecialTypeString(),
			pdb.getTypeRecord(typeIndex)));
		builder.append(name);
		builder.append(String.format("   Parent: %08X, End: %08X, Next: %08X\n", parentPointer,
			endPointer, nextPointer));
		builder.append(String.format("   Debug start: %08X, Debug end: %08X\n", debugStartOffset,
			debugEndOffset));
		builder.append(String.format("   %s\n", procedureFlags));
		builder.append(String.format("   Return Reg: %s\n", registerContainingReturnValue));
	}

	/**
	 * Creates subcomponents for this class, which can be deserialized later.
	 * @return the {@link AbstractString} type necessary for the {@link #name} in the
	 * concrete class.
	 */
	protected abstract AbstractString create();

	/**
	 * Returns the special type string used during Emit.
	 * @return Special type string.
	 */
	protected abstract String getSpecialTypeString();

}
