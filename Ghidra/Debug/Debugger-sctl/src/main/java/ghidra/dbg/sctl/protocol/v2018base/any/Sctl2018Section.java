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
package ghidra.dbg.sctl.protocol.v2018base.any;

import ghidra.comm.packet.fields.PacketField;
import ghidra.dbg.sctl.protocol.common.AbstractSctlSection;
import ghidra.dbg.sctl.protocol.common.SctlString;

public class Sctl2018Section extends AbstractSctlSection {
	@Override
	public void setAddress(long address) {
		this.addr = address;
	}

	@Override
	public long getAddress() {
		return addr;
	}

	@Override
	public void setLength(long length) {
		this.len = length;
	}

	@Override
	public long getLength() {
		return len;
	}

	@Override
	public void setName(String name) {
		this.name = new SctlString(name);
	}

	@Override
	public String getName() {
		return name.str;
	}

	@PacketField
	public long addr;

	@PacketField
	public long len;

	@PacketField
	public SctlString name;
}
