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
package ghidra.app.util.pdb.pdbapplicator;

import java.math.BigInteger;

import ghidra.app.util.bin.format.pdb.*;
import ghidra.program.model.data.DataType;
import ghidra.util.exception.CancelledException;

/**
 * <code>PdbMember</code> convey PDB member information used for datatype
 * reconstruction.
 */
public class Default2PdbMember extends PdbMember {

	private AbstractMsTypeApplier applier;
	private DataType dataType;

	/**
	 * Default PDB member construction
	 * @param applicator {@link PdbApplicator} for which we are working.
	 * @param name member field name.  For bitfields this also conveys the bit-size
	 * and optionally the bit-offset.
	 * @param applier fieldApplier for the field datatype or base datatype associated with the
	 *  bitfield.
	 * @param offset member's byte offset within the root composite.
	 */
	Default2PdbMember(PdbApplicator applicator, String name, AbstractMsTypeApplier applier,
			int offset) {
		super(name, (applier.getDataType()).getName(), offset, null);
		this.applier = applier;
		dataType = null;
	}

	/**
	 * Default PDB member construction
	 * @param applicator {@link PdbApplicator} for which we are working.
	 * @param name member field name.  For bitfields this also conveys the bit-size
	 * and optionally the bit-offset.
	 * @param dataType for the field.
	 * @param offset member's byte offset within the root composite.
	 */
	Default2PdbMember(PdbApplicator applicator, String name, DataType dataType, int offset) {
		super(name, dataType.getName(), offset, null);
		this.applier = null;
		this.dataType = dataType;
	}

	AbstractMsTypeApplier getApplier() {
		return applier;
	}

	private DataType getDataTypeInternal() {
		if (applier != null) {
			return applier.getDataType();
		}
		return dataType;
	}

	@Override
	public String getDataTypeName() {
		return getDataTypeInternal().getName();
	}

	@Override
	public String toString() {
		String str = "name=" + getName();
		DataType dt = getDataTypeInternal();
		if (dt instanceof PdbBitField) {
			PdbBitField bfDt = (PdbBitField) dataType;
			str += ", type=" + bfDt.getBaseDataType().getName();
			str += ", offset=" + getOffset();
			str += ", bitSize=" + bfDt.getDeclaredBitSize() + ", bitOffset=" +
				bfDt.getBitOffsetWithinBase();
		}
		else {
			str += ", type=" + dataType.getName();
			str += ", offset=" + getOffset();
		}
		return str;
	}

	@Override
	protected WrappedDataType getDataType() throws CancelledException {
		DataType dt = getDataTypeInternal();
		if (applier != null && applier instanceof ArrayTypeApplier) {
			if (BigInteger.ZERO.compareTo(applier.getSize()) == 0) {
				return new WrappedDataType(dt, true, false);
			}
		}
		return new WrappedDataType(dt, false, false);
	}

}
