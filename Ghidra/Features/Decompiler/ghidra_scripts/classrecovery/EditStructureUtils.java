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
//DO NOT RUN. THIS IS NOT A SCRIPT! THIS IS A CLASS THAT IS USED BY SCRIPTS. 
package classrecovery;

import java.util.*;

import ghidra.program.model.data.*;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

class EditStructureUtils {

	private EditStructureUtils() {

	}


	/**
	 * Method to determine if the given containing struct has components at the given offset 
	 * that are either the same, all undefined1's or equivalently sized undefineds as the components
	 * in the given possible newInternalStruct
	 * @param containingStruct the given structure to check
	 * @param offset the offset at the containing struct to check for equivalent components
	 * @param newInternalStruct the possible new internal struct to replace at the given offset in the 
	 * given containing struct
	 * @param monitor task monitor
	 * @return true if components in the containing struct are replaceable with the possible new
	 * internal struct, false otherwise
	 * @throws CancelledException if cancelled
	 */
	static boolean hasReplaceableComponentsAtOffset(Structure containingStruct, int offset,
			Structure newInternalStruct, TaskMonitor monitor) throws CancelledException {

		DataTypeComponent[] newStructComponents = newInternalStruct.getComponents();

		for (DataTypeComponent newStructComponent : newStructComponents) {

			monitor.checkCanceled();

			int structOffset = newStructComponent.getOffset();

			DataTypeComponent currentComponentAtOffset =
				containingStruct.getComponentAt(offset + structOffset);

			DataType newStructCompDt = newStructComponent.getDataType();
			DataType containingComDt = currentComponentAtOffset.getDataType();

			// if component dts are equal continue
			if (newStructCompDt.equals(containingComDt)) {
				continue;
			}

			// if containing is all undefined1s at equivalent location then continue
			if (hasEnoughUndefined1sAtOffset(containingStruct, offset + structOffset,
				newStructCompDt.getLength(), monitor)) {
				continue;
			}

			// otherwise if lengths not equal then return false
			if (newStructCompDt.getLength() != containingComDt.getLength()) {
				return false;
			}

			// component lengths are equal if it gets here
			// if containing is not undefined then return false because the components would be
			// incompatible types
			if (!containingComDt.getName().startsWith("undefined")) {
				return false;
			}

		}
		return true;
	}

	/**
	 * Method to determine if there are at least the given length of undefined size 1 components at the given offset in the given structure
	 * @param structure the given structure
	 * @param offset the given offset
	 * @param length the length of undefine size 1 components to check for starting at given offset
	 * @param monitor task monitor
	 * @return true if there are at least length undefined size 1 components at the given offset in the given structure
	 * @throws CancelledException if cancelled
	 */
	static boolean hasEnoughUndefined1sAtOffset(Structure structure, int offset, int length,
			TaskMonitor monitor) throws CancelledException {

		if (structure.getLength() < offset + length) {
			return false;
		}

		for (int i = offset; i < offset + length; i++) {
			monitor.checkCanceled();
			DataTypeComponent component = structure.getComponentAt(i);
			if (component == null) {
				return false;
			}
			DataType dataType = component.getDataType();
			if (dataType.getName().equals("undefined") && dataType.getLength() == 1) {
				continue;
			}
			return false;
		}
		return true;
	}

	/**
	 * Method to check that all components starting with the one at the given offset and encompassing
	 * all up to the given length from the given offset are an undefined data type. If so, clear them all and return true.
	 * If not, do nothing and return false.
	 * @param structure the given structure
	 * @param offset the given offset in the structure
	 * @param length the total length from the offset to hopefully clear
	 * @param monitor task monitor
	 * @return true if successfully cleared from offset to offset+length, false otherwise
	 * @throws CancelledException if cancelled
	 */
	static boolean clearLengthAtOffset(Structure structure, int offset, int length,
			TaskMonitor monitor) throws CancelledException {

		if (structure.getLength() < offset + length) {
			return false;
		}

		List<Integer> offsetsToClear = new ArrayList<Integer>();

		int endOfClear = offset + length;

		while (offset < endOfClear) {

			monitor.checkCanceled();

			DataTypeComponent component = structure.getComponentContaining(offset);

			offsetsToClear.add(component.getOffset());
			offset = component.getOffset() + component.getLength();

		}

		if (offsetsToClear.isEmpty()) {
			return false;
		}

		Iterator<Integer> offsetIterator = offsetsToClear.iterator();
		while (offsetIterator.hasNext()) {
			Integer componentOffset = offsetIterator.next();
			// need to get ordinal from component using offset because after clearing 
			// component size > 1, the index changes
			DataTypeComponent component = structure.getComponentAt(componentOffset);
			int index = component.getOrdinal();
			structure.clearComponent(index);
		}
		return true;
	}

	/**
	 * Method to determine if data type is an undefined size 1 data type
	 * @param dataType the given data type
	 * @return true if given data type is undefined size 1, false otherwise
	 */
	static boolean isUndefined1(DataType dataType) {

		if (Undefined.isUndefined(dataType) && dataType.getLength() == 1) {
			return true;
		}

		return false;
	}

	/**
	 * Method to determine if data type is an undefined data type of any size 
	 * @param dataType the given data type
	 * @return true if given data type is undefined of any size, false otherwise
	 */
	static boolean isUndefined(DataType dataType) {
		if (dataType.getName().contains("undefined")) {
			return true;
		}
		return false;
	}

	/**
	 * Method to determine if there are at least the given length of undefined (any size) components 
	 * at the given offset in the given structure. This is only valid for non-packed structures.
	 * @param structure the given structure
	 * @param offset the given offset
	 * @param length the total length of undefined components to check for starting at given offset
	 * @param monitor task monitor
	 * @return true if there are at least total length of undefined components at the given offset in the given structure
	 * @throws CancelledException if cancelled
	 * @throws IllegalArgumentException if a packed structure is passed in
	 */
	static boolean hasEnoughUndefinedsOfAnyLengthAtOffset(Structure structure, int offset,
			int length, TaskMonitor monitor) throws CancelledException {

		if (structure.isPackingEnabled()) {
			throw new IllegalArgumentException(
				"Packed structures are not supported by this method");
		}

		int endOfRange = offset + length;

		if (offset < 0 || length <= 0 || structure.getLength() < endOfRange) {
			return false;
		}

		while (offset < endOfRange) {

			monitor.checkCanceled();

			DataTypeComponent component = structure.getComponentContaining(offset);

			DataType dataType = component.getDataType();

			if (!Undefined.isUndefined(dataType)) {
				return false;
			}

			offset = component.getOffset() + component.getLength();

		}

		return true;
	}

	/**
	 * Method to add a field to the given structure. If the structure already has data 
	 * at the given offset, don't replace. If there is undefined data there then replace 
	 * it with the data type. If the structure empty, insert the data type at the given offset. 
	 * If the structure is not big enough and not-empty, grow it so there is room to replace.
	 * @param structure the given structure
	 * @param offset the offset to add a field
	 * @param dataType the data type to add to the field at the given offset
	 * @param fieldName the name of field
	 * @param monitor task monitor
	 * @return the updated structure data type
	 * @throws IllegalArgumentException if issue inserting data type into structure
	 * @throws CancelledException if cancelled
	 */
	static Structure addDataTypeToStructure(Structure structure, int offset,
			DataType dataType, String fieldName, TaskMonitor monitor)
			throws CancelledException, IllegalArgumentException {

		int dataTypeLength = dataType.getLength();

		int endOfDataTypeInStruct = offset + dataTypeLength;

		int roomForData = structure.getLength() - endOfDataTypeInStruct;
		
		// FIXME: This will not worked for structures where packing is enabled - not sure how to handle

		// if structure isn't defined insert
		if (structure.isNotYetDefined()) {
			structure.insertAtOffset(offset, dataType, dataTypeLength, fieldName, null);
			return structure;
		}

		// if not enough room, grow the structure
		if (roomForData < 0) {
			structure.growStructure(0 - roomForData);
		}

		// else replace only if data already there are enough undefined data types at 
		// that offset to fit the new data type
		if (hasEnoughUndefined1sAtOffset(structure, offset, dataTypeLength, monitor)) {
			structure.replaceAtOffset(offset, dataType, dataTypeLength, fieldName, null);
		}

		return structure;
	}

	/**
	 * Method to determine if the given structure has room at the given offset to have a component of the given length added to it
	 * @param structureDataType the given structure
	 * @param offset the offset to check for available room
	 * @param lengthToAdd the length of bytes wanted to add at the offset
	 * @param monitor task monitor
	 * @return true if the given structure has room at the given offset to have a component of the given length added to it
	 * @throws CancelledException if cancelled
	 */
	static boolean canAdd(Structure structureDataType, int offset, int lengthToAdd,
			TaskMonitor monitor)
			throws CancelledException {

		// not big enough so return true so it can be grown
		DataTypeComponent component = structureDataType.getComponentAt(offset);
		if (component == null) {
			return true;
		}

		// no matter what size, if the data type at the offset is defined, return false
		// so it is not replaced
		if (!component.getDataType().getName().equals("undefined")) {
			return false;
		}

		// if structure isn't big enough but what is there is all undefined
		// return true to grow it
		int structLen = structureDataType.getLength();
		int spaceAvailable = structLen - (offset + lengthToAdd);

		if (spaceAvailable < 0) {
			int overflow = 0 - spaceAvailable;
			return hasEnoughUndefined1sAtOffset(structureDataType, offset, structLen - overflow,
				monitor);
		}

		// if structure is big enough and there is room at the offset return true
		return hasEnoughUndefined1sAtOffset(structureDataType, offset, lengthToAdd, monitor);

	}

	/**
	 * Method to retrieve the number of undefined size 1 components in the given structure before the given offset
	 * @param structure the given structure
	 * @param offset the given offset
	 * @param monitor task monitor
	 * @return the number of undefined size 1 components in the given structure before the given offset
	 * @throws CancelledException if cancelled
	 */
	static int getNumberOfUndefinedsBeforeOffset(Structure structure, int offset,
			TaskMonitor monitor) throws CancelledException {

		if (structure.getNumComponents() == 0) {
			return -1;
		}

		int numUndefineds = 0;
		int index = offset - 1;

		while (index >= 0) {
			monitor.checkCanceled();
			DataTypeComponent component = structure.getComponentAt(index);
			if (component != null && component.getDataType() == DataType.DEFAULT) {
				index--;
				numUndefineds++;
			}
			else {
				return numUndefineds;
			}
		}
		return numUndefineds;
	}

	/**
	 * Method to retrieve the number of undefined size 1 components starting at the given offset in the given structure
	 * @param structure the given structure
	 * @param offset the given offset
	 * @param monitor task monitor
	 * @return the number of undefined size 1 components starting at the given offset in the given structure
	 * @throws CancelledException if cancelled
	 */
	static int getNumberOfUndefinedsStartingAtOffset(Structure structure, int offset,
			TaskMonitor monitor) throws CancelledException {

		int numUndefineds = 0;
		int index = offset;

		while (index < structure.getLength()) {
			monitor.checkCanceled();
			DataTypeComponent component = structure.getComponentAt(index);
			if (component.getDataType().getName().equals("undefined") &&
				component.getLength() == 1) {
				index++;
				numUndefineds++;
			}
			else {
				return numUndefineds;
			}
		}
		return numUndefineds;
	}


}
