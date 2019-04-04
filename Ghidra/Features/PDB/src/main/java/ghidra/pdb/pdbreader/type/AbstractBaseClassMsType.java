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

import java.math.BigInteger;

import ghidra.pdb.PdbByteReader;
import ghidra.pdb.PdbException;
import ghidra.pdb.pdbreader.*;

public abstract class AbstractBaseClassMsType extends AbstractMsType {

	protected AbstractTypeIndex baseClassIndex;
	protected ClassFieldMsAttributes attribute;
	protected BigInteger offset;

	/**
	 * Constructor for this type.
	 * @param pdb {@link AbstractPdb} to which this type belongs.
	 * @param reader {@link PdbByteReader} from which this type is deserialized.
	 * @throws PdbException Upon not enough data left to parse.
	 */
	public AbstractBaseClassMsType(AbstractPdb pdb, PdbByteReader reader) throws PdbException {
		super(pdb, reader);
		create();
		parseFields(reader);
		pdb.pushDependencyStack(
			new CategoryIndex(CategoryIndex.Category.DATA, baseClassIndex.get()));
		pdb.popDependencyStack();
	}

	@Override
	public void emit(StringBuilder builder, Bind bind) {
		attribute.emit(builder);
		builder.append(":");
		builder.append(pdb.getTypeRecord(baseClassIndex.get()));
		builder.append("<@");
		builder.append(offset);
		builder.append(">");
	}

	/**
	 * Creates subcomponents for this class, which can be deserialized later.
	 * <P>
	 * Implementing class must initialize {@link #baseClassIndex}.
	 */
	protected abstract void create();

	/**
	 * Parses the fields for this type.
	 * <P>
	 * Implementing class must, in the appropriate order pertinent to itself, allocate/parse
	 * {@link #attribute}; also parse {@link #baseClassIndex} and {@link #offset}.
	 * @param reader {@link PdbByteReader} from which data is parsed.
	 * @throws PdbException Upon not enough data left to parse.
	 */
	protected abstract void parseFields(PdbByteReader reader) throws PdbException;

}
