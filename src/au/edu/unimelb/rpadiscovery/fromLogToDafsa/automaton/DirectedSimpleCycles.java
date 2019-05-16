package au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton;

/*=============================================================================

    Copyright(©) 2013 Nikolay Ognyanov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

=============================================================================*/

import java.util.List;

import org.jgrapht.DirectedGraph;

/**
 * A common interface for classes implementing algorithms
 * for enumeration of the simple cycles of a directed graph.
 *
 * @author Nikolay Ognyanov
 *
 * @param <V> - the vertex type.
 */
public interface DirectedSimpleCycles<V, E>
{
    /**
     * Returns the graph on which the simple cycle
     * search algorithm is executed by this object.
     *
     * @return The graph.
     */
    DirectedGraph<V, E> getGraph();

    /**
     * Sets the graph on which the simple cycle
     * search algorithm is executed by this object.
     *
     * @throws IllegalArgumentException if the
     *         argument is <code>null</code>.
     */
    void setGraph(DirectedGraph<V, E> graph);

    /**
     * Finds the simple cycles of the graph.<br/>
     * Note that the full algorithm is executed on
     * every call since the graph may have changed
     * between calls.
     *
     * @return The list of all simple cycles.
     * Possibly empty but never <code>null</code>.
     * @throws IllegalArgumentException if the
     * current graph is null.
     */
    List<List<V>> findSimpleCycles();
}
