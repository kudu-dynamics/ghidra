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
package ghidra.service.graph;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import docking.action.DockingAction;
import docking.widgets.EventTrigger;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

/**
 * Interface for objects that display (or consume) graphs.  Normally, a graph display represents
 * a visual component for displaying and interacting with a graph.  Some implementation may not
 * be a visual component, but instead consumes/processes the graph (i.e. graph exporter). In this
 * case, there is no interactive element and once the graph has been set on the display, it is 
 * closed.
 */
public interface GraphDisplay {
	public static final int ALIGN_LEFT = 0;  // aligns graph text to the left
	public static final int ALIGN_CENTER = 1; // aligns graph text to the center
	public static final int ALIGN_RIGHT = 2; // aligns graph text to the right
	public static final int ALIGN_TOP = 3; // aligns graph text to the right
	public static final int ALIGN_BOTTOM = 4; // aligns graph text to the right

	/**
	 * Sets a {@link GraphDisplayListener} to be notified when the user changes the vertex focus
	 * or selects one or more nodes in a graph window
	 * 
	 * @param listener the listener to be notified
	 */
	public void setGraphDisplayListener(GraphDisplayListener listener);

	/**
	 * Tells the graph display window to focus the vertex with the given id.
	 * 
	 * @param vertex the vertex to focus
	 * @param eventTrigger Provides a hint to the GraphDisplay as to why we are updating the
	 * graph location so that the GraphDisplay can decide if it should send out a notification via
	 * the {@link GraphDisplayListener#locationFocusChanged(AttributedVertex)}. For example, if we
	 * are updating the the location due to an event from the main application, we don't want to
	 * notify the application the graph changed to avoid event cycles. See {@link EventTrigger} for
	 * more information.
	 */
	public void setFocusedVertex(AttributedVertex vertex, EventTrigger eventTrigger);

	/**
	 * Returns the graph for this display
	 * @return the graph for this display
	 */
	public AttributedGraph getGraph();

	/**
	 * Returns the currently focused vertex or null if no vertex is focused
	 * @return  the currently focused vertex or null if no vertex is focused.
	 */
	public AttributedVertex getFocusedVertex();

	/**
	 * Tells the graph display window to select the vertices with the given ids
	 * 
	 * @param vertexSet the set of vertices to select
	 * @param eventTrigger Provides a hint to the GraphDisplay as to why we are updating the
	 * graph location so that the GraphDisplay can decide if it should send out a notification via
	 * the {@link GraphDisplayListener#selectionChanged(Set)}. For example, if we are updating
	 * the the location due to an event from the main application, we don't want to notify the 
	 * application the graph changed to avoid event cycles. See {@link EventTrigger} for more
	 * information.
	 */
	public void selectVertices(Set<AttributedVertex> vertexSet, EventTrigger eventTrigger);

	/**
	 * Returns a set of vertex ids for all the currently selected vertices
	 * @return  a set of vertex ids for all the currently selected vertices
	 */
	public Set<AttributedVertex> getSelectedVertices();

	/**
	 * Closes this graph display window.
	 */
	public void close();

	/**
	 * Defines a vertex attribute type for this graph window
	 * 
	 * @param name the name of the attribute which may be attached to vertices.
	 */
	public void defineVertexAttribute(String name);

	/**
	 * Defines an edge attribute type for this graph window
	 * 
	 * @param name the name of the attribute which may be attached to edges.
	 */
	public void defineEdgeAttribute(String name);

	/**
	 * Sets the name of the attribute which should be used as the primary vertex label in the display.
	 * @param attributeName the name of the attribute to use as the display label for vertices.
	 * @param alignment (ALIGN_LEFT, ALIGN_RIGHT, ALIGN_CENTER, ALIGN_TOP, or ALIGN_BOTTOM)
	 * @param size the font size to use for the display label
	 * @param monospace true if the font should be monospaced
	 * @param maxLines the maximum number lines to display in the vertex labels
	 */
	public void setVertexLabel(String attributeName, int alignment, int size, boolean monospace,
			int maxLines);

	/**
	 * Sets the graph to be displayed or consumed by this graph display
	 * @param graph the graph to display or consume
	 * @param title a title for the graph
	 * @param monitor a {@link TaskMonitor} which can be used to cancel the graphing operation
	 * @param append if true, append the new graph to any existing graph.
	 * @throws CancelledException thrown if the graphing operation was cancelled
	 */
	public void setGraph(AttributedGraph graph, String title, boolean append,
			TaskMonitor monitor)
			throws CancelledException;

	/**
	 * Clears all graph vertices and edges from this graph display
	 */
	public void clear();

	/**
	 * Updates a vertex to a new name
	 * @param vertex the vertex to rename
	 * @param newName the new name for the vertex
	 */
	public void updateVertexName(AttributedVertex vertex, String newName);

	/**
	 * Returns the title of the current graph
	 * @return the title of the current graph
	 */
	public String getGraphTitle();

	/**
	 * Adds the action to the graph display. Not all GraphDisplays support adding custom
	 * actions, so this may have no effect.
	 * @param action the action to add.
	 */
	public void addAction(DockingAction action);

	default void setProperty(String key, String value) {
	}

	default String getValue(String key) {
		return null;
	}

	default Map<String, String> getProperties() {
		return Collections.emptyMap();
	}

}
