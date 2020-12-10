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
package ghidra.dbg.sctl.dialect;

public interface HasOptionalPlatform {
	/**
	 * Check if the format contains a platform field
	 * 
	 * @return true if supported, false otherwise
	 */
	boolean supportsPlatform();

	/**
	 * Set the platform
	 * 
	 * @param platform the platform
	 */
	void setPlatform(String platform);

	/**
	 * Get the platform
	 * 
	 * @return the platform
	 */
	String getPlatform();
}
