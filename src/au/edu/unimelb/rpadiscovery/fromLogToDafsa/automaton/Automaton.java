package au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.DecodeTandemRepeats;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jbpt.graph.DirectedEdge;
import org.jbpt.graph.MultiDirectedGraph;
import org.jbpt.hypergraph.abs.Vertex;

import com.google.common.collect.BiMap;

/*
 * Copyright © 2009-2017 The Apromore Initiative.
 *
 * This file is part of "Apromore".
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

/**
 * @author Daniel Reissner,
 * @version 1.0, 01.02.2017
 */

public class Automaton {

    private Map<Integer, State> states;
    private BiMap<Integer, String> eventLabels;
    private BiMap<String, Integer> inverseEventLabels;
    private BiMap<Integer, String> globalEventLabels;
    private BiMap<String, Integer> globalInverseLabels;
    private Map<Integer, Transition> transitions;
    private int source;
    private IntHashSet finalStates;
    private Set<IntIntHashMap> finalConfigurations;
    private int skipEvent = -2;
    public FastList<IntArrayList> cases;
    public BiMap<IntArrayList, IntArrayList> caseTracesMapping;
    public Map<IntArrayList, Integer> caseFrequencies = new HashMap<IntArrayList, Integer>();
    private int minNumberOfModelMoves = Integer.MAX_VALUE;
    private Map<IntIntHashMap, List<IntArrayList>> configCaseMapping;
    public Map<Integer, String> caseIDs;
    public IntIntHashMap minimalFinalConfig;
    public int loops = 0;
    public UnifiedMap<IntArrayList, UnifiedSet<DecodeTandemRepeats>> reductions;
    public UnifiedMap<IntIntHashMap, UnifiedSet<DecodeTandemRepeats>> configDecoderMapping;

    public Automaton(Map<Integer, State> states, BiMap<Integer, String> labelMapping, BiMap<String, Integer> inverseLabelMapping, Map<Integer, Transition> transitions,
                     int initialState, IntHashSet FinalStates, BiMap<IntArrayList, IntArrayList> caseTracesMapping, Map<Integer, String> caseIDs) throws IOException {
        this.states = states;
        this.eventLabels = labelMapping;


        this.inverseEventLabels = inverseLabelMapping;
        this.transitions = transitions;
        this.source = initialState;
        this.finalStates = FinalStates;
        this.caseTracesMapping = caseTracesMapping;
        for (IntArrayList trace : caseTracesMapping.keySet())
            caseFrequencies.put(trace, caseTracesMapping.get(trace).size());
        this.caseIDs = caseIDs;
        TransitionComparator tComp = new TransitionComparator();
        for (State st : this.states.values()) {
            st.incomingTransitions().sortThis(tComp);
            st.outgoingTransitions().sortThis(tComp);

        }

        //this.toDot("/Users/daniel/Documents/workspace/dafsa/Road Traffic/Main.dot");
        this.calculateLogFutures();
    }

    public Map<IntArrayList, Integer> getCaseFrequencies() {
        return caseFrequencies;
    }

    public Automaton(Map<Integer, State> states, BiMap<Integer, String> labelMapping, BiMap<String, Integer> inverseLabelMapping, Map<Integer, Transition> transitions,
                     int initialState, IntHashSet FinalStates, BiMap<IntArrayList, IntArrayList> caseTracesMapping, Map<Integer, String> caseIDs, UnifiedMap<IntArrayList, UnifiedSet<DecodeTandemRepeats>> reductions) throws IOException {
        this.states = states;
        this.eventLabels = labelMapping;
        this.inverseEventLabels = inverseLabelMapping;
        this.transitions = transitions;
        this.source = initialState;
        this.finalStates = FinalStates;
        this.caseTracesMapping = caseTracesMapping;
        for (IntArrayList trace : caseTracesMapping.keySet())
            caseFrequencies.put(trace, caseTracesMapping.get(trace).size());
        this.caseIDs = caseIDs;
        this.reductions = reductions;
        TransitionComparator tComp = new TransitionComparator();
        for (State st : this.states.values()) {
            st.incomingTransitions().sortThis(tComp);
            st.outgoingTransitions().sortThis(tComp);
        }

        //this.toDot("/Users/daniel/Documents/workspace/dafsa/Road Traffic/Main.dot");
        this.calculateExpandedLogFutures();
    }

    public Automaton(Map<Integer, State> states, BiMap<Integer, String> eventLabels, BiMap<String, Integer> inverseLabelMapping, Map<Integer, Transition> transitions,
                     int initialState, IntHashSet finalStates, int skipEvent) throws IOException {
        this.states = states;
        this.eventLabels = eventLabels;
        this.inverseEventLabels = inverseLabelMapping;
        this.transitions = transitions;
        this.source = initialState;
        this.finalStates = finalStates;
        this.skipEvent = skipEvent;
        //this.toDot("Main.dot");
        TransitionComparator tComp = new TransitionComparator();
        for (State st : this.states.values()) {
            st.incomingTransitions().sortThis(tComp);
            st.outgoingTransitions().sortThis(tComp);
        }
        //this.toDot("/Users/dreissner/Documents/Paper tests/S-Components Paper/TandemRepeatsTest/rg.dot");
        this.discoverFinalConfigurationsForModel();
        //for(int f : this.finalStates.toArray()) {System.out.println("Final Marking : " + this.states.get(f).label());}
    }

    public Automaton(BiMap<Integer, State> states, BiMap<Integer, String> eventLabels, BiMap<String, Integer> inverseEventLabelMapping,
                     BiMap<Integer, Transition> transitions, int initialState, IntHashSet finalStates, int skipEvent, BiMap<Integer, String> globalEventLabels)
            throws FileNotFoundException {
        this.states = states;
        this.eventLabels = eventLabels;
        this.inverseEventLabels = inverseEventLabelMapping;
        this.transitions = transitions;
        this.source = initialState;
        this.finalStates = finalStates;
        this.skipEvent = skipEvent;
        //this.toDot("Main.dot");
        this.setGlobalEventLabels(globalEventLabels);
        this.setGlobalInverseLabels(globalEventLabels.inverse());
        this.discoverFinalConfigurationsForModel();
    }

    public Map<Integer, State> states() {
        return this.states;
    }

    public BiMap<Integer, String> eventLabels() {
        return this.eventLabels;
    }

    public BiMap<String, Integer> inverseEventLabels() {
        return this.inverseEventLabels;
    }

    public Map<Integer, Transition> transitions() {
        return this.transitions;
    }

    public State source() {
        return this.states.get(this.source);
    }

    public int sourceID() {
        return this.source;
    }

    public IntHashSet finalStates() {
        return this.finalStates;
    }

    public Set<IntIntHashMap> finalConfigurations() {
        return this.finalConfigurations;
    }

    public int skipEvent() {
        return this.skipEvent;
    }

    public Map<IntIntHashMap, List<IntArrayList>> configCasesMapping() {
        if (this.configCaseMapping == null)
            this.configCaseMapping = new UnifiedMap<IntIntHashMap, List<IntArrayList>>();
        return this.configCaseMapping;
    }

    private void discoverFinalConfigurationsForModel() throws FileNotFoundException//(boolean discoverPotentialPaths)
    {
		/*DirectedGraph<State, DefaultEdge> dGr = new DefaultDirectedGraph<State, DefaultEdge>(DefaultEdge.class);
		for(State st : this.states.values())
			dGr.addVertex(st);
		for(Transition tr : this.transitions.values())
			dGr.addEdge(tr.source(), tr.target());
		List<List<State>> cycles = new SzwarcfiterLauerSimpleCycles<State, DefaultEdge>(dGr).findSimpleCycles();
		loops = cycles.size();*/
//		System.out.println(cycles.size());
//		for(List<State> cycle : cycles)
//		{
//			System.out.print("Cycle: ");
//			for(int i = 0; i < cycle.size(); i++)
//			{
//				State st = cycle.get(i);
//				System.out.print(st.label());
//				if(i < cycle.size()-1) System.out.print(" -> ");
//			}	
//			System.out.println();
//		}
//		System.out.println();
		
		/*UnifiedSet<IntIntHashMap> oldLoopLabels;
		UnifiedSet<IntIntHashMap> newLoopLabels = null;
		UnifiedSet<IntIntHashMap> elemLoopLabels = new UnifiedSet<IntIntHashMap>();
		for (int i = 0; i < cycles.size(); i++)
		{
			oldLoopLabels = new UnifiedSet<IntIntHashMap>();
			IntIntHashMap init = new IntIntHashMap();
			oldLoopLabels.add(init);
			List<State> cycle = cycles.get(i);
			for (int j = 0; j < cycle.size(); j++) 
			{
				State actState = ((State) cycle.get(j));
				newLoopLabels = new UnifiedSet<IntIntHashMap>();
				actState.isLoopState = true;
				actState.loops().add(i);
				for(IntIntHashMap element : oldLoopLabels)
				{
					int target = j+1;
					if(j==cycle.size()-1) target = 0;
					for(Transition tr : actState.outgoingTransitions())
						if(tr.target().equals((State) cycle.get(target)))
						{
							IntIntHashMap loopLabels = new IntIntHashMap(element);
							loopLabels.addToValue(tr.eventID(), 1);
							newLoopLabels.add(loopLabels);
						}
				}
				oldLoopLabels = newLoopLabels;
			}
			elemLoopLabels.addAll(newLoopLabels);
			for (int j = 0; j < cycle.size(); j++)
				for(IntIntHashMap loopLabels : newLoopLabels)
					((State) cycle.get(j)).loopLabels().add(loopLabels);
		}*/
//		for(State st : this.states.values())
//		{
//			System.out.println(st.label());
//			System.out.println(st.isLoopState);
//			System.out.println(st.loopLabels().size());
//			System.out.println(st.loopLabels());
//		}

        TarjanSCC scc = new TarjanSCC(this);
        int m = scc.count();
        IntArrayList[] components = new IntArrayList[m];
        for (int i = 0; i < m; i++) {
            components[i] = new IntArrayList();
        }
        for (State state : this.states().values()) {
            components[scc.id(state.id())].add(state.id());
            state.setComponent(scc.id(state.id()));
        }
        IntObjectHashMap<List<Transition>> compInArcs = new IntObjectHashMap<List<Transition>>();
        IntObjectHashMap<List<Transition>> compOutArcs = new IntObjectHashMap<List<Transition>>();
        IntObjectHashMap<List<Transition>> compArcs = new IntObjectHashMap<List<Transition>>();
        IntObjectHashMap<IntIntHashMap> cLoopLabels = new IntObjectHashMap<IntIntHashMap>();
        List<Transition> inArcs;
        List<Transition> outArcs;
        List<Transition> cArcs;
        IntIntHashMap cLabels;
        for (IntArrayList component : components) {
            for (int stID : component.toArray()) {
                State state = this.states().get(stID);
//				if(component.size() > 1)
//					state.isLoopState = true;
                if ((inArcs = compInArcs.get(state.component())) == null) {
                    inArcs = new FastList<Transition>();
                    compInArcs.put(state.component(), inArcs);
                }
                for (Transition in : state.incomingTransitions()) {
                    if (component.contains(in.source().id())) {
                        in.explore = false;
                        continue;
                    }
                    inArcs.add(in);
                }
                if ((outArcs = compOutArcs.get(state.component())) == null) {
                    outArcs = new FastList<Transition>();
                    compOutArcs.put(state.component(), outArcs);
                }
                if ((cArcs = compArcs.get(state.component())) == null) {
                    cArcs = new FastList<Transition>();
                    compArcs.put(state.component(), cArcs);
                }
                for (Transition out : state.outgoingTransitions()) {
                    if (component.contains(out.target().id())) {
                        if ((cLabels = cLoopLabels.get(state.component())) == null) {
                            cLabels = new IntIntHashMap();
                            cLoopLabels.put(state.component(), cLabels);
                        }
                        cLabels.put(out.eventID(), 200);
//						if(!state.isLoopState && out.target().equals(state))
//							state.isLoopState = true;
                        cArcs.add(out);
                    } else
                        outArcs.add(out);
                }
            }
        }
//		PrintWriter pw = new PrintWriter("scc.dot");
//		pw.println("digraph fsm {");
//		pw.println("rankdir=LR;");
//		pw.println("node [shape=circle,style=filled, fillcolor=white]");
//		for(int c = 0; c < m; c++)
//		{
//			pw.printf("%d [label=\"%s\"];%n", c, c);
//			for(Synchronization tr : compInArcs.get(c))
//			{
//				pw.printf("%d -> %d [label=\"%s\"];%n", tr.source().component(), c, this.eventLabels().get(tr.eventID()));
//			}
//		}
//		pw.println("}");
//		pw.close();

        //TODO:Implement Component futures -> done
        //visited and toBeVisited for component IDs
        IntArrayList toBeVisited = new IntArrayList();
        IntHashSet visited = new IntHashSet();
        IntIntHashMap possibleFuture;
        if (this.finalConfigurations == null)
            this.finalConfigurations = new UnifiedSet<IntIntHashMap>();
        finalConfigurations.clear();
        this.states().values().forEach(state -> state.possibleFutures().clear());

        for (int finalState : this.finalStates().toArray()) {
            //this.transitions().values().forEach(transition -> transition.explore=true);
            State fState = this.states().get(finalState);
            for (List<Transition> trs : compInArcs.values())
                for (Transition tr : trs)
                    tr.explore = true;
            for (Transition tr : compInArcs.get(fState.component())) {
                tr.explore = false;
                State trSource = tr.source();
				/*if(trSource.isLoopState)
				{
					Set<IntIntHashMap> uniqueCycles = new UnifiedSet<IntIntHashMap>();
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						uniqueCycles.addAll(compState.loopLabels());
					}
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						compState.futureLoops().addAll(uniqueCycles);
					}
				}*/
                possibleFuture = new IntIntHashMap();
                possibleFuture.addToValue(tr.eventID(), 1);
                trSource.possibleFutures().add(possibleFuture);

                boolean compExplore = true;
                for (Transition out : compOutArcs.get(trSource.component())) {
                    if (out.explore)
                        compExplore = false;
                }
                if (compExplore)
                    if (visited.add(trSource.component()))
                        toBeVisited.add(trSource.id());
            }
        }

        while (!toBeVisited.isEmpty()) {
            State state = this.states().get(toBeVisited.removeAtIndex(0));
            //if(!state.loopLabels().isEmpty())
            //state.futureLoops().addAll(state.loopLabels());
            if (components[state.component()].size() > 1) {
                traverseSComponent(compInArcs.get(state.component()), compOutArcs.get(state.component()), compArcs.get(state.component()), state.possibleFutures(), cLoopLabels.get(state.component()), state.component());
            }
            for (Transition tr : compInArcs.get(state.component())) {
                if (tr.explore) {
                    tr.explore = false;
                    State trSource = tr.source();
					/*if(trSource.isLoopState)
					{
						Set<IntIntHashMap> uniqueCycles = new UnifiedSet<IntIntHashMap>();
						for(int stID : components[trSource.component()].toArray())
						{
							State compState = this.states().get(stID);
							uniqueCycles.addAll(compState.loopLabels());
						}
						for(int stID : components[trSource.component()].toArray())
						{
							State compState = this.states().get(stID);
							compState.futureLoops().addAll(uniqueCycles);
						}
					}*/

                    for (IntIntHashMap sourcePossibleFuture : state.possibleFutures()) {
                        possibleFuture = new IntIntHashMap(sourcePossibleFuture);
                        possibleFuture.addToValue(tr.eventID(), 1);
                        trSource.possibleFutures().add(possibleFuture);
                    }

                    if (state.hasLoopFuture())
                        trSource.futureLoops().addAll(state.futureLoops());
                    boolean compExplore = true;
                    for (Transition out : compOutArcs.get(trSource.component())) {
                        if (out.explore) {
                            compExplore = false;
                            break;
                        }
                    }
                    if (compExplore)
                        if (visited.add(trSource.component()))
                            toBeVisited.add(trSource.id());
                }
            }
        }
        this.finalConfigurations().addAll(this.source().possibleFutures());
        for (IntIntHashMap finalConfiguration : this.finalConfigurations) {
            IntIntHashMap test = new IntIntHashMap(finalConfiguration);
            //Main.remove(this.skipEvent);
            for (int key : test.keySet().toArray())
                if (test.get(key) >= 200)
                    test.put(key, test.get(key) % 200);
            if (test.sum() < this.minNumberOfModelMoves()) {
                this.minNumberOfModelMoves = Math.min(this.minNumberOfModelMoves, (int) test.sum());
                this.minimalFinalConfig = test;
            }
        }
    }

    private void calculateExpandedLogFutures() {
        //this.configCasesMapping().clear();
        this.configDecoderMapping = new UnifiedMap<>();
        IntIntHashMap config, expConfig;
        UnifiedSet<DecodeTandemRepeats> decoders = null;
        for (IntArrayList trace : this.reductions.keySet()) {
            config = new IntIntHashMap();
            for (int element : trace.distinct().toArray())
                config.put(element, trace.count(t -> t == element));
            for (DecodeTandemRepeats decoder : reductions.get(trace)) {
                expConfig = new IntIntHashMap(config);
                for (int l : decoder.reducedLabels().keySet().toArray())
                    expConfig.addToValue(l, decoder.reducedLabels().get(l));
                if ((decoders = this.configDecoderMapping.get(expConfig)) == null) {
                    decoders = new UnifiedSet<>();
                    this.configDecoderMapping.put(expConfig, decoders);
                }
                decoders.add(decoder);
            }
        }
    }

    private void calculateLogFutures() {
        this.configCasesMapping().clear();
        IntIntHashMap config;
        List<IntArrayList> cases = null;
        for (IntArrayList trace : this.caseTracesMapping.keySet()) {
            config = new IntIntHashMap();
            for (int element : trace.distinct().toArray())
                config.put(element, trace.count(t -> t == element));
            if ((cases = this.configCasesMapping().get(config)) == null) {
                cases = new FastList<IntArrayList>();
                this.configCasesMapping().put(config, cases);
            }
            cases.add(trace);
        }
    }

    public void mapAddAll(IntIntHashMap base, IntIntHashMap addition) {
        for (int key : addition.keySet().toArray()) {
            base.addToValue(key, addition.get(key));
        }

    }

    public void mapAddAllSpecial(IntIntHashMap base, IntIntHashMap addition) {
        int count;
        for (int key : addition.keySet().toArray()) {
            count = (base.get(key) % 200) + 200;
            base.put(key, count);
        }
    }

    private void traverseSComponent(List<Transition> compInArcs, List<Transition> compOutArcs, List<Transition> compArcs, Set<IntIntHashMap> baseFutures, IntIntHashMap cLoopLabels, int comp) {
		/*IntArrayList toBeVisited = new IntArrayList();
        IntHashSet visited = new IntHashSet();
        IntIntHashMap possibleFuture;
        compArcs.forEach(t -> t.explore = true);
        for(Transition t : compOutArcs)
        {
        	State source = t.source();
        	for(Transition tr : source.outgoingTransitions())
			{
				if(compOutArcs.contains(tr)) continue;
				tr.explore = false;
			}
        }

        for(Transition t : compOutArcs)
		{
			State source = t.source();
			for(Transition tr : source.incomingTransitions())
			{
				if(compInArcs.contains(tr) || tr.explore==false || tr.source().id() == tr.target().id()) continue;
				tr.explore = false;
				State trSource = tr.source();
				for(IntIntHashMap baseFuture : baseFutures)
				{
					possibleFuture = new IntIntHashMap(baseFuture);
					//possibleFuture.put(tr.eventID(), possibleFuture.get(tr.eventID()) % 200 + 201);
					possibleFuture.addToValue(tr.eventID(), 1);
					trSource.possibleFutures().add(possibleFuture);
				}
				boolean explore = true;
				for(Transition out : trSource.outgoingTransitions())
					if(!compOutArcs.contains(out) && out.explore)
					{explore = false; break;}
				if(explore)
					if(visited.add(trSource.id()))
						toBeVisited.add(trSource.id());
			}
		}
        
        while(!toBeVisited.isEmpty())
        {
        	State st = this.states.get(toBeVisited.removeAtIndex(0));
        	for(Transition tr : st.incomingTransitions())
        	{
        		if(compInArcs.contains(tr) || tr.target().id() == tr.source().id()) continue;
        		tr.explore = false;
        		State trSource = tr.source();
        		for(IntIntHashMap baseFuture : st.possibleFutures())
        		{
        			possibleFuture = new IntIntHashMap(baseFuture);
        			//possibleFuture.put(tr.eventID(), possibleFuture.get(tr.eventID()) % 200 + 201);
        			possibleFuture.addToValue(tr.eventID(), 1);
        			trSource.possibleFutures().add(possibleFuture);
        		}
        		boolean explore = true;
            	for(Transition out : trSource.outgoingTransitions())
            		if(!compOutArcs.contains(out) && out.explore)
            			{explore = false; break;}
            	if(explore)
            		if(visited.add(trSource.id()))
            			toBeVisited.add(trSource.id());
        	}
        }*/
        for (State st : this.states.values())
            if (st.component() == comp)
                for (IntIntHashMap future : st.possibleFutures())
                    for (int label : cLoopLabels.keySet().toArray())
                        future.addToValue(label, 200);
    }

    public void toDot(PrintWriter pw) throws IOException {
        pw.println("digraph fsm {");
        pw.println("rankdir=LR;");
        pw.println("node [shape=circle,style=filled, fillcolor=white]");

        for (State n : this.states.values()) {
            if (n.isSource()) {
                //pw.printf("%d [label=\"%s\", fillcolor=\"gray\"];%n", n.id(), n.label());
                pw.printf("%d [label=\"%d\", fillcolor=\"gray\"];%n", n.id(), n.id());
            } else {
                //pw.printf("%d [label=\"%s\"];%n", n.id(), n.label());
                pw.printf("%d [label=\"%d\"];%n", n.id(), n.id());
            }

            for (Transition t : n.outgoingTransitions()) {
                //System.out.println(n.id());
                //System.out.println(t.target().id());
                //int frequencyValue = frequency.get(""+n.id()+","+t.target().id());
                //System.out.println("value"+frequencyValue);
                pw.printf("%d -> %d [label=\"%s\"];%n", n.id(), t.target().id(), this.eventLabels().get(t.eventID()));
            }

            if (n.isFinal()) {
                String comment = "";
				/*for(Set<Integer> finalConfiguration: this.finalConfigurations().get(n.id()))
				{
					comment = comment + "<br/>Final Configuration: ";
					for(int event : finalConfiguration)
						comment = comment + this.getEvents().get(event).label() + ", ";
					comment = comment.substring(0, comment.length() -2);
				}*/
                pw.printf("%d [label=<%s%s>, shape=doublecircle];%n", n.id(), n.label(), comment);
                //pw.printf("%d [label=\"%d\", shape=doublecircle];%n", n.id(), n.id());
            }
        }
        pw.println("}");
    }

    public MultiDirectedGraph toMultiDirectedGraph() throws IOException {
        MultiDirectedGraph g = new MultiDirectedGraph();
        HashMap<String, Vertex> vertices = new HashMap<>();
        String id;
        Vertex source=null;
        for (State n : this.states.values()) {
            id = "" + n.id();
            vertices.put(id, new Vertex(id));
        }

        for (State n : this.states.values()) {
            Vertex i = vertices.get("" + n.id());
			if(n.isSource()) {
				source=i;
			}
            for (Transition t : n.outgoingTransitions()) {
                Vertex o = vertices.get("" + t.target().id());
                DirectedEdge edge = g.getEdge(i, o);
                if (edge == null || !edge.getName().equals(this.eventLabels().get(t.eventID()))) {
                    g.addEdge(i, o).setName(this.eventLabels().get(t.eventID()));
                    g.setFrequency(0);
                }
            }
        }
        Vertex vertex;
        String label=null;
        for(IntArrayList c: caseFrequencies.keySet()){
            vertex=source;
            for(int index=0; index<c.size(); index++){
                label=eventLabels.get(c.get(index));
                for(DirectedEdge edge: g.getOutgoingEdges(vertex)){
                    if(edge.getName().equals(label)){
                        edge.setFrequency(edge.getFrequency()+caseFrequencies.get(c));
                        vertex=edge.getTarget();
                        break;
                    }
                }
            }

        }
        //System.out.println(g);
        return g;
    }

    public void toDot(String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(fileName);
        toDot(pw);
        pw.close();
    }

    public int minNumberOfModelMoves() {
        return this.minNumberOfModelMoves;
    }

    public BiMap<Integer, String> getGlobalEventLabels() {
        return globalEventLabels;
    }

    public void setGlobalEventLabels(BiMap<Integer, String> globalEventLabels) {
        this.globalEventLabels = globalEventLabels;
    }

    public BiMap<String, Integer> getGlobalInverseLabels() {
        return globalInverseLabels;
    }

    public void setGlobalInverseLabels(BiMap<String, Integer> globalInverseLabels) {
        this.globalInverseLabels = globalInverseLabels;
    }
}
