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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.StrongConnectivityInspector;

/**
* Find all simple cycles of a directed graph using the 
* Schwarcfiter and Lauer's algorithm.
* <p/>
* See:<br/>
* J.L.Szwarcfiter and P.E.Lauer, Finding the elementary 
* cycles of a directed graph in O(n + m) per cycle, 
* Technical Report Series, #60, May 1974, Univ. of 
* Newcastle upon Tyne, Newcastle upon Tyne, England.
* 
* @author Nikolay Ognyanov
*
* @param <V> - the vertex type.
* @param <E> - the edge type.
*/
public class SzwarcfiterLauerSimpleCycles<V, E>
implements DirectedSimpleCycles<V, E>
{
// The graph.
private DirectedGraph<V, E> graph;

// The state of the algorithm.
private List<List<V>>       cycles        = null;
private V[]                 iToV          = null;
private Map<V, Integer>     vToI          = null;
private Map<V, Set<V>>      bSets         = null;
private Stack<V>            stack         = null;
private Set<V>              marked        = null;
private Map<V, Set<V>>      removed       = null;
private int[]               position      = null;
private boolean[]           reach         = null;
private List<V>             startVertices = null;

/**
 * Create a simple cycle finder with an unspecified graph.
 */
public SzwarcfiterLauerSimpleCycles()
{
}

/**
 * Create a simple cycle finder for the specified graph.
 * 
 * @param graph - the DirectedGraph in which to find cycles.
 * @throws IllegalArgumentException if the graph argument is
 *         <code>null</code>.
 */
public SzwarcfiterLauerSimpleCycles(DirectedGraph<V, E> graph)
{
    if (graph == null) {
        throw new IllegalArgumentException("Null graph argument.");
    }
    this.graph = graph;
}

/**
  * {@inheritDoc}
  */
@Override
public DirectedGraph<V, E> getGraph()
{
    return graph;
}

/**
 * {@inheritDoc}
 */
@Override
public void setGraph(DirectedGraph<V, E> graph)
{
    if (graph == null) {
        throw new IllegalArgumentException("Null graph argument.");
    }
    this.graph = graph;
}

/**
 * {@inheritDoc}
 */
@Override
public List<List<V>> findSimpleCycles()
{
    // Just a straightforward implementation of
    // the algorithm.
    if (graph == null) {
        throw new IllegalArgumentException("Null graph.");
    }
    initState();
    StrongConnectivityInspector<V, E> inspector =
        new StrongConnectivityInspector<V, E>(graph);
    List<Set<V>> sccs = inspector.stronglyConnectedSets();
    for (Set<V> scc : sccs) {
        int maxInDegree = -1;
        V startVertex = null;
        for (V v : scc) {
            int inDegree = graph.inDegreeOf(v);
            if (inDegree > maxInDegree) {
                maxInDegree = inDegree;
                startVertex = v;
            }
        }
        startVertices.add(startVertex);
    }

    for (V vertex : startVertices) {
        cycle(toI(vertex), 0);
    }

    List<List<V>> result = cycles;
    clearState();
    return result;
}

private boolean cycle(int v, int q)
{
    boolean foundCycle = false;
    V vV = toV(v);
    marked.add(vV);
    stack.push(vV);
    int t = stack.size();
    position[v] = t;
    if (!reach[v]) {
        q = t;
    }
    Set<V> avRemoved = getRemoved(vV);
    Set<E> edgeSet = graph.outgoingEdgesOf(vV);
    Iterator<E> avIt = edgeSet.iterator();
    while (avIt.hasNext()) {
        E e = avIt.next();
        V wV = graph.getEdgeTarget(e);
        if (avRemoved.contains(wV)) {
            continue;
        }
        int w = toI(wV);
        if (!marked.contains(wV)) {
            boolean gotCycle = cycle(w, q);
            if (gotCycle) {
                foundCycle = gotCycle;
            }
            else {
                noCycle(v, w);
            }
        }
        else if (position[w] <= q) {
            foundCycle = true;
            int vIndex = stack.indexOf(vV);
            int wIndex = stack.indexOf(wV);
            List<V> cycle = new ArrayList<V>();
            for (int i = wIndex; i <= vIndex; i++) {
                cycle.add(stack.elementAt(i));
            }
            cycles.add(cycle);
        }
        else {
            noCycle(v, w);
        }
    }
    stack.pop();
    if (foundCycle) {
        unmark(v);
    }
    reach[v] = true;
    position[v] = graph.vertexSet().size();
    return foundCycle;
}

private void noCycle(int x, int y)
{
    V xV = toV(x);
    V yV = toV(y);

    Set<V> by = getBSet(yV);
    Set<V> axRemoved = getRemoved(xV);

    by.add(xV);
    axRemoved.add(yV);
}

private void unmark(int x)
{
    V xV = toV(x);
    marked.remove(xV);
    Set<V> bx = getBSet(xV);
    for (V yV : bx) {
        Set<V> ayRemoved = getRemoved(yV);
        ayRemoved.remove(xV);
        if (marked.contains(yV)) {
            unmark(toI(yV));
        }
    }
    bx.clear();
}

@SuppressWarnings("unchecked")
private void initState()
{
    cycles = new ArrayList<List<V>>();
    iToV = (V[]) graph.vertexSet().toArray();
    vToI = new HashMap<V, Integer>();
    bSets = new HashMap<V, Set<V>>();
    stack = new Stack<V>();
    marked = new HashSet<V>();
    removed = new HashMap<V, Set<V>>();
    int size = graph.vertexSet().size();
    position = new int[size];
    reach = new boolean[size];
    startVertices = new ArrayList<V>();

    for (int i = 0; i < iToV.length; i++) {
        vToI.put(iToV[i], i);
    }
}

private void clearState()
{
    cycles = null;
    iToV = null;
    vToI = null;
    bSets = null;
    stack = null;
    marked = null;
    removed = null;
    position = null;
    reach = null;
    startVertices = null;
}

private Integer toI(V v)
{
    return vToI.get(v);
}

private V toV(int i)
{
    return iToV[i];
}

private Set<V> getBSet(V v)
{
    // B sets are typically not all
    // needed, so instantiate lazily.
    Set<V> result = bSets.get(v);
    if (result == null) {
        result = new HashSet<V>();
        bSets.put(v, result);
    }
    return result;
}

private Set<V> getRemoved(V v)
{
    // Removed sets typically not all
    // needed, so instantiate lazily.
    Set<V> result = removed.get(v);
    if (result == null) {
        result = new HashSet<V>();
        removed.put(v, result);
    }
    return result;
}
}
