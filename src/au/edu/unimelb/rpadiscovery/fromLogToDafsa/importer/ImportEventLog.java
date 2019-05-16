package au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer;

import java.io.File;
import java.io.IOException;
import java.util.*;

import au.edu.unimelb.rpadiscovery.Main;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Automaton;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.State;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Transition;
import au.edu.unimelb.rpadiscovery.Utils.Log;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XMxmlGZIPParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.*;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import au.edu.unimelb.rpadiscovery.fromLogToDafsa.dafsa.AbstractIntDAFSA;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.dafsa.IntDAFSAInt;

import static au.edu.unimelb.rpadiscovery.Utils.LogTAG.TAG_IMPORT;

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

public class ImportEventLog {
	private final String conceptname = "concept:name";
	private final String lifecycle = "lifecycle:transition";
	private final String timestamp = "time:timestamp";

	private BiMap<Integer, String> labelMapping;
	private BiMap<String, Integer> inverseLabelMapping;
	private BiMap<Integer, State> stateMapping;
	private BiMap<Integer, Transition> transitionMapping;
	private IntHashSet finalStates;
	private HashMap<IntArrayList, Integer> frequency;

	//private Map<IntArrayList, Boolean> tracesContained;
	private BiMap<IntArrayList, IntArrayList> caseTracesMapping;
	//private IntObjectHashMap<String> traceIDtraceName;
    private Automaton logAutomaton;
    private UnifiedMap<IntArrayList,UnifiedSet<DecodeTandemRepeats>> reductions;
	private HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes;

	public BiMap<Integer, String> getLabelMapping() {
		return labelMapping;
	}

	public HashMap<IntArrayList, Integer> getFrequency() {
		return frequency;
	}

	public HashMap<String, LinkedList<String>> nameActions;

	public HashMap<IntArrayList, ArrayList<EventAttributes>> getTracesEventsAttributes() {
		return tracesEventsAttributes;
	}

	public XLog importEventLog(String fileName) throws Exception
	{
		File xesFileIn = new File(fileName);
		XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
        if (!parser.canParse(xesFileIn)) {
        	parser = new XesXmlGZIPParser();
        	if (!parser.canParse(xesFileIn)) {
        		throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
        	}
        }
        List<XLog> xLogs = parser.parse(xesFileIn);

       return xLogs.remove(0);
	}

	public XLog createReducedTREventLog(String fileName) throws Exception
	{
		XLog xLog = importEventLog(fileName);

		caseTracesMapping = HashBiMap.create();
		Map<Integer, String> caseIDs = new UnifiedMap<>();
		IntArrayList traces;
		labelMapping = HashBiMap.create();
		inverseLabelMapping = HashBiMap.create();
		reductions = new UnifiedMap<>();
		String eventName;
		String traceID;
		int translation = 1;
		int iTransition = 0;
		IntArrayList tr;
		UnifiedSet<DecodeTandemRepeats> redTr;
		IntDAFSAInt fsa = new IntDAFSAInt();
		Integer key = null;
		UnifiedSet<IntArrayList> visited = new UnifiedSet<>();
		int it = 0;
		IntArrayList reductionStats = new IntArrayList();
		IntArrayList traceLengthStats = new IntArrayList();
		IntArrayList redTraceLengthStats = new IntArrayList();
		IntIntHashMap labelCount = new IntIntHashMap();
		XTrace trace;
		int i,j;
		DecodeTandemRepeats decoder;
		XFactory xFac = new XFactoryNaiveImpl();
		XLog log = xFac.createLog();
		//XExtensionManager xEvMan = XExtensionManager.instance();
		//xEvMan.
		XExtension xtend = XConceptExtension.instance();
		XAttributeLiteral xAttrLiteral = xFac.createAttributeLiteral(conceptname, "", xtend);
		XAttributeLiteral xLc = xFac.createAttributeLiteral(lifecycle, "", xtend);
		for(i = 0; i < xLog.size(); i++)
		{
			trace = xLog.get(i);
			traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
			tr = new IntArrayList(trace.size());
			for (j = 0; j < trace.size(); j++)
			{
				eventName = ((XAttributeLiteral) trace.get(j).getAttributes().get(conceptname)).getValue();//xce.extractName(event);
				if((key = (inverseLabelMapping.get(eventName))) == null)
				{
					//labelMapping.put(translation, eventName);
					inverseLabelMapping.put(eventName, translation);
					key = translation;
					translation++;
				}
				tr.add(key);
			}
			decoder = new DecodeTandemRepeats(tr, 0, tr.size());
			decoder.reduceTandemRepeats();
			XTrace newTr = xFac.createTrace();
			for(j = 0; j < decoder.reducedTrace().size();j++)
			{
				XAttributeMap xAttr = xFac.createAttributeMap();
				xAttrLiteral.setValue(inverseLabelMapping.inverse().get(decoder.reducedTrace().get(j)));
				labelCount.addToValue(decoder.reducedTrace().get(j),1);
				xLc.setValue("" + labelCount.get(decoder.reducedTrace().get(j)));
				xAttr.put(conceptname, xAttrLiteral);
				xAttr.put(lifecycle, xLc);
				XEvent event = xFac.createEvent(xAttr);
				newTr.add(event);
			}
			labelCount.clear();
			log.add(newTr);
			reductionStats.add(decoder.trace.size() - decoder.reducedTrace().size());
			traceLengthStats.add(decoder.trace.size());
			redTraceLengthStats.add(decoder.reducedTrace().size());
			if((redTr = reductions.get(decoder.reducedTrace()))==null)
			{
				redTr = new UnifiedSet<>();
				reductions.put(decoder.reducedTrace(),redTr);
				fsa.addMinWord(decoder.reducedTrace());
			}
			redTr.add(decoder);
			//if(visited.add(decoder.reducedTrace))
			//	fsa.addMinWord(decoder.reducedTrace);
			//caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
			if((traces = caseTracesMapping.get(tr))==null)
			{
				traces = new IntArrayList();
				caseTracesMapping.put(tr, traces);
			}
			traces.add(it);
			caseIDs.put(it, traceID);
			it++;
		}
		labelMapping = inverseLabelMapping.inverse();
		System.out.println("Stats - Avg. : " + reductionStats.average() + "; Max : " + reductionStats.max() + "; Med. : " + reductionStats.median());
		System.out.println("Stats - Avg. : " + traceLengthStats.average() + "; Max : " + traceLengthStats.max() + "; Med. : " + traceLengthStats.median());
		System.out.println("Stats - Avg. : " + redTraceLengthStats.average() + "; Max : " + redTraceLengthStats.max() + "; Med. : " + redTraceLengthStats.median());
		return log;
	}
	
	public Automaton convertLogToAutomatonFromXes(String fileName) throws Exception {
		//long start = System.nanoTime();
		File xesFileIn = new File(fileName);
		System.out.println(xesFileIn.exists());
		XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
		//XMxmlGZIPParser parser = new XMxmlGZIPParser(new XFactoryNaiveImpl());
		if (!parser.canParse(xesFileIn)) {
        	parser = new XesXmlGZIPParser();
			//parser = new XMxmlGZIPParser();
        	if (!parser.canParse(xesFileIn)) {
        		throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
        	}
        }
        List<XLog> xLogs = parser.parse(xesFileIn);
        XLog xLog = xLogs.remove(0);
        /*
        while (xLogs.size() > 0) {
        	xLog.addAll(xLogs.remove(0));
        }
        */
        //long end = System.nanoTime();
        //System.out.println("Log import: " + TimeUnit.SECONDS.convert((end - start), TimeUnit.NANOSECONDS) + "s");
        return this.createDAFSAfromLog(xLog);
	}
	public Automaton convertLogToAutomatonFromXMxml(String fileName) throws Exception {
		//long start = System.nanoTime();
		File xesFileIn = new File(fileName);
		System.out.println(xesFileIn.exists());
		//XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
		XMxmlGZIPParser parser = new XMxmlGZIPParser(new XFactoryNaiveImpl());
		if (!parser.canParse(xesFileIn)) {
			//parser = new XesXmlGZIPParser();
			parser = new XMxmlGZIPParser();
			if (!parser.canParse(xesFileIn)) {
				throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
			}
		}
		List<XLog> xLogs = parser.parse(xesFileIn);
		XLog xLog = xLogs.remove(0);
        /*
        while (xLogs.size() > 0) {
        	xLog.addAll(xLogs.remove(0));
        }
        */
		//long end = System.nanoTime();
		//System.out.println("Log import: " + TimeUnit.SECONDS.convert((end - start), TimeUnit.NANOSECONDS) + "s");
		return this.createDAFSAfromLog(xLog);
	}


	public Automaton convertLogToAutomatonWithTRFrom(String fileName) throws Exception
	{
		File xesFileIn = new File(fileName);
		XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
		if(!parser.canParse(xesFileIn))
		{
			parser = new XesXmlGZIPParser();
			if(!parser.canParse(xesFileIn))
			{
				throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
			}
		}
		List<XLog> xLogs = parser.parse(xesFileIn);
		XLog xLog = xLogs.get(0);
		return this.createReducedDAFSAfromLog(xLog);
	}


	public Automaton createReducedDAFSAfromLog(XLog xLog) throws IOException
	{
		caseTracesMapping = HashBiMap.create();
		Map<Integer, String> caseIDs = new UnifiedMap<>();
		IntArrayList traces;
		labelMapping = HashBiMap.create();
		inverseLabelMapping = HashBiMap.create();
		reductions = new UnifiedMap<>();
		String eventName;
		String traceID;
		int translation = 1;
		int iTransition = 0;
		IntArrayList tr;
		UnifiedSet<DecodeTandemRepeats> redTr;
		IntDAFSAInt fsa = new IntDAFSAInt();
		Integer key = null;
		UnifiedSet<IntArrayList> visited = new UnifiedSet<>();
		int it = 0;
		IntArrayList reductionStats = new IntArrayList();
		IntArrayList traceLengthStats = new IntArrayList();
		IntArrayList redTraceLengthStats = new IntArrayList();

		XTrace trace;
		int i,j;
		DecodeTandemRepeats decoder;

		for(i = 0; i < xLog.size(); i++)
		{
			trace = xLog.get(i);
			traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
			tr = new IntArrayList(trace.size());
			for (j = 0; j < trace.size(); j++)
			{
				eventName = ((XAttributeLiteral) trace.get(j).getAttributes().get(conceptname)).getValue();//xce.extractName(event);
				if((key = (inverseLabelMapping.get(eventName))) == null)
				{
					//labelMapping.put(translation, eventName);
					inverseLabelMapping.put(eventName, translation);
					key = translation;
					translation++;
				}
				tr.add(key);
			}
			decoder = new DecodeTandemRepeats(tr, 0, tr.size());
			decoder.reduceTandemRepeats();
			reductionStats.add(decoder.trace.size() - decoder.reducedTrace().size());
			traceLengthStats.add(decoder.trace.size());
			redTraceLengthStats.add(decoder.reducedTrace().size());
			if((redTr = reductions.get(decoder.reducedTrace()))==null)
			{
				redTr = new UnifiedSet<>();
				reductions.put(decoder.reducedTrace(),redTr);
				fsa.addMinWord(decoder.reducedTrace());
			}
			redTr.add(decoder);
			//if(visited.add(decoder.reducedTrace))
			//	fsa.addMinWord(decoder.reducedTrace);
			//caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
			if((traces = caseTracesMapping.get(tr))==null)
			{
				traces = new IntArrayList();
				caseTracesMapping.put(tr, traces);
			}
			traces.add(it);
			caseIDs.put(it, traceID);
			it++;
		}
		labelMapping = inverseLabelMapping.inverse();
		System.out.println("Stats - Avg. : " + reductionStats.average() + "; Max : " + reductionStats.max() + "; Med. : " + reductionStats.median());
		System.out.println("Stats - Avg. : " + traceLengthStats.average() + "; Max : " + traceLengthStats.max() + "; Med. : " + traceLengthStats.median());
		System.out.println("Stats - Avg. : " + redTraceLengthStats.average() + "; Max : " + redTraceLengthStats.max() + "; Med. : " + redTraceLengthStats.median());
		return(this.prepareLogAutomaton(fsa, caseIDs));
	}

	private Automaton prepareLogAutomaton(IntDAFSAInt fsa, Map<Integer, String> caseIDs) throws IOException {
		int i;
		int iTransition = 0;
		int idest=0;
		int ilabel=0;
		int initialState = 0;
		stateMapping = HashBiMap.create();
		transitionMapping = HashBiMap.create();
		finalStates = new IntHashSet();
		for(AbstractIntDAFSA.State n : fsa.getStates())
		{
			if(!(n.outbound()==0 && (!fsa.isFinalState(n.getNumber()))))
			{
				if(!stateMapping.containsKey(n.getNumber()))
					stateMapping.put(n.getNumber(), new State(n.getNumber(), fsa.isSource(n.getNumber()), fsa.isFinalState(n.getNumber())));
				if(initialState !=0 && fsa.isSource(n.getNumber())){initialState = n.getNumber();}
				if(fsa.isFinalState(n.getNumber())){finalStates.add(n.getNumber());}
				for(i = 0; i < n.outbound(); i++)
				{
					idest = AbstractIntDAFSA.decodeDest(n.next.get(i));
					//System.out.println(idest);

					ilabel = AbstractIntDAFSA.decodeLabel(n.next.get(i));
					/*System.out.println(ilabel);
					System.out.println("****");*/
					if (!stateMapping.containsKey(idest))
						stateMapping.put(idest, new State(idest, fsa.isSource(idest), fsa.isFinalState(AbstractIntDAFSA.decodeDest(n.next.get(i)))));
					iTransition++;
					Transition t = new Transition(stateMapping.get(n.getNumber()), stateMapping.get(idest), ilabel);
					transitionMapping.put(iTransition, t);
					stateMapping.get(n.getNumber()).outgoingTransitions().add(t);
					stateMapping.get(idest).incomingTransitions().add(t);
				}
			}
		}

		//System.out.println("value: "+transitionMapping.values().iterator().next().eventID());
		logAutomaton = new Automaton(stateMapping, labelMapping, inverseLabelMapping, transitionMapping, initialState, finalStates, caseTracesMapping, caseIDs);//, concurrencyOracle);
		//long conversion = System.nanoTime();
		//System.out.println("Log Automaton creation: " + TimeUnit.MILLISECONDS.convert((automaton - start), TimeUnit.NANOSECONDS) + "ms");
		//System.out.println("Log Automaton conversion: " + TimeUnit.MILLISECONDS.convert((conversion - automaton), TimeUnit.NANOSECONDS) + "ms");
		return logAutomaton;
	}

	public Automaton createDAFSAfromLog(XLog xLog) throws IOException
	{
		nameActions=new HashMap<>();


		//long start = System.nanoTime();
		//tracesContained = new UnifiedMap<IntArrayList, Boolean>();
		caseTracesMapping = HashBiMap.create();
		Map<Integer, String> caseIDs = new UnifiedMap<Integer, String>();
		IntArrayList traces;
		//traceIDtraceName = new IntObjectHashMap<String>();
		labelMapping = HashBiMap.create();
		inverseLabelMapping = HashBiMap.create();
		frequency= new HashMap<IntArrayList, Integer>();
		String eventName;
		String traceID;
		int translation = 0;
		int iTransition = 0;
		IntArrayList tr;
		IntDAFSAInt fsa = new IntDAFSAInt();
		Integer key = null;
		UnifiedSet<IntArrayList> visited = new UnifiedSet<IntArrayList>();
		int it = 0;
		
		XTrace trace;
		int i, j;
		tracesEventsAttributes = new HashMap<>();
		int numberOfActions=0;
		int numberOfTraces=0;
		int numberOfRoutines=0;
		int numberOfRoutines2=0;
		for (i = 0; i < xLog.size(); i++)
		{
			trace = xLog.get(i);
			traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();

			/*System.out.println();
			System.out.println(traceID);
*/

			tr = new IntArrayList(trace.size());
			ArrayList<EventAttributes> eventAttributesArrayList = new ArrayList<>(trace.size());
			EventAttributes eventAttributesTMP;
			EventAttributes eventAttributes;
			numberOfActions+=trace.size();
			numberOfTraces++;
			for (j = 0; j < trace.size(); j++)
			{
				eventName = ((XAttributeLiteral) trace.get(j).getAttributes().get(conceptname)).getValue();//xce.extractName(event);
				eventAttributesTMP=new EventAttributes();
				eventAttributesTMP.setTrace(tr);
				String nameAction=null;
				String source=null;
				LinkedList<String> attributesList=new LinkedList<>();
				for(String s: trace.get(j).getAttributes().keySet()){
					//TODO: ITA eliminare attributi ridondanti come il nome
					switch(s){
						case timestamp:
							eventAttributesTMP.addTimestamp( trace.get(j).getAttributes().get(s).toString());
							break;
						case conceptname:
							nameAction =  trace.get(j).getAttributes().get(s).toString();
							//System.out.println("name actionnnnnnnnn: "+nameAction);
							eventAttributesTMP.setActivityName(nameAction);
							break;
						case lifecycle:
							break;
						default:
							if(s.equals("source"))
								source=trace.get(j).getAttributes().get(s).toString();
							attributesList.add(s);
							eventAttributesTMP.addAttributes(s, trace.get(j).getAttributes().get(s).toString());

					}

					//System.out.println(s+" -> "+ trace.get(j).getAttributes().get(s).toString());
				}


				LinkedList<String> nameList=nameActions.get(nameAction);
				if(nameList==null){
					nameList=new LinkedList<>();
					nameActions.put(nameAction, nameList);
				}
				String temp=source+"_";
				Collections.sort(attributesList);
				for(String si: attributesList){
					temp+=si+",";
				}
				temp=temp.substring(0, temp.length()-1);
				if(!nameList.contains(temp)){
					nameList.add(temp);
				}
				int index=nameList.indexOf(temp);
				if(index!=0){
					eventAttributesTMP.setActivityName(nameAction+index);
					eventName=eventName+index;
				}
				eventAttributesArrayList.add(j,eventAttributesTMP);

				/*System.out.println("keys: "+inverseLabelMapping.keySet());
				System.out.println("value: "+inverseLabelMapping.values());
				System.out.println(eventName);*/
				if((key = (inverseLabelMapping.get(eventName))) == null)
				{
					//labelMapping.put(translation, eventName);
					inverseLabelMapping.put(eventName, translation);
					//System.out.println("event name: "+eventName+", translation: "+translation);
					key = translation;
					translation++;
				}
				tr.add(key);

			}

			//System.out.println("******* "+tr);
			/*Integer freqTrace = frequency.get(tr);
			if(freqTrace==null){
				fsa.addMinWord(tr);
				frequency.put(tr, 1);
			}else{
				frequency.replace(tr, freqTrace+1);
			}
			System.out.println("frequency "+frequency.get(tr));*/
			Log.getInstance().println(TAG_IMPORT, 8, tr);
			//System.out.println(tr);
			if(visited.add(tr)) {
				numberOfRoutines++;
				fsa.addMinWord(tr);
				tracesEventsAttributes.put(tr, eventAttributesArrayList);
			}else{
				ArrayList<EventAttributes> eventAttributesArrayListOriginal= tracesEventsAttributes.get(tr);
				if(eventAttributesArrayListOriginal.size()!=eventAttributesArrayList.size()){
					//TODO: CANCEL THIS
					System.err.println("ERROR ImportEventLog ****************************");
				}
				for(int z=0; z<eventAttributesArrayListOriginal.size(); z++){
					eventAttributesArrayListOriginal.get(z).add(eventAttributesArrayList.get(z));
				}
			}



			//caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
			if((traces = caseTracesMapping.get(tr))==null)
			{
				traces = new IntArrayList();
				caseTracesMapping.put(tr, traces);
				numberOfRoutines2++;
			}


			traces.add(it);
			caseIDs.put(it, traceID);
			it++;
			//listTraces.add(traceLabels);
			
//			if((traces = tracesLabelsMapping.get(traceLabels))==null)
//			{
//				traces = new IntArrayList();
//				tracesLabelsMapping.put(traceLabels, traces);
//			}
//			traces.add(it);
		}
		labelMapping = inverseLabelMapping.inverse();

		System.out.println("LOG:");
		System.out.println("#Actions: "+numberOfActions);
		System.out.println("#Traces: "+numberOfTraces);
		System.out.println("#Routines1: "+numberOfRoutines);
		System.out.println("#Routines2: "+numberOfRoutines2);
		System.out.println("\n\n");

		return this.prepareLogAutomaton(fsa, caseIDs);
	}

	public Automaton createDAFSAfromLog(XLog xLog, BiMap<String,Integer> inverseLabelMapping) throws IOException
	{
		//long start = System.nanoTime();
		//tracesContained = new UnifiedMap<IntArrayList, Boolean>();
		caseTracesMapping = HashBiMap.create();
		Map<Integer, String> caseIDs = new UnifiedMap<Integer, String>();
		IntArrayList traces;
		//traceIDtraceName = new IntObjectHashMap<String>();
		labelMapping = HashBiMap.create();
		this.inverseLabelMapping = HashBiMap.create(inverseLabelMapping);
		String eventName;
		String traceID;
		int translation = this.inverseLabelMapping.size();
		int iTransition = 0;
		IntArrayList tr;
		IntDAFSAInt fsa = new IntDAFSAInt();
		Integer key = null;
		UnifiedSet<IntArrayList> visited = new UnifiedSet<IntArrayList>();
		int it = 0;

		XTrace trace;
		int i, j;
		for (i = 0; i < xLog.size(); i++)
		{
			trace = xLog.get(i);
			traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
			tr = new IntArrayList(trace.size());
			for (j = 0; j < trace.size(); j++)
			{
				eventName = ((XAttributeLiteral) trace.get(j).getAttributes().get(conceptname)).getValue();//xce.extractName(event);
				if((key = (inverseLabelMapping.get(eventName))) == null)
				{
					//labelMapping.put(translation, eventName);
					inverseLabelMapping.put(eventName, translation);
					key = translation;
					translation++;
				}
				tr.add(key);
			}

			if(visited.add(tr))
				fsa.addMinWord(tr);
			//caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
			if((traces = caseTracesMapping.get(tr))==null)
			{
				traces = new IntArrayList();
				caseTracesMapping.put(tr, traces);
			}
			traces.add(it);
			caseIDs.put(it, traceID);
			it++;
			//listTraces.add(traceLabels);

//			if((traces = tracesLabelsMapping.get(traceLabels))==null)
//			{
//				traces = new IntArrayList();
//				tracesLabelsMapping.put(traceLabels, traces);
//			}
//			traces.add(it);
		}
		labelMapping = inverseLabelMapping.inverse();
		return this.prepareLogAutomaton(fsa, caseIDs);
	}

	public List<Automaton> convertLogToAutomatonFrom(String fileName, List<Map<Integer, String>> projectionLabels) throws Exception {
		//long start = System.nanoTime();
		File xesFileIn = new File(fileName);
		XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
        if (!parser.canParse(xesFileIn)) {
        	parser = new XesXmlGZIPParser();
        	if (!parser.canParse(xesFileIn)) {
        		throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
        	}
        }
        List<XLog> xLogs = parser.parse(xesFileIn);
        XLog xLog = xLogs.remove(0);
        /*
        while (xLogs.size() > 0) {
        	xLog.addAll(xLogs.remove(0));
        }
        */
        //long end = System.nanoTime();
        //System.out.println("Log import: " + TimeUnit.SECONDS.convert((end - start), TimeUnit.NANOSECONDS) + "s");
        List<XLog> projectedLogs = projectModelLabelsOn(xLog, projectionLabels);
        List<Automaton> projectedDafsas = new FastList<Automaton>();
        for(XLog pLog : projectedLogs)
        {
        	projectedDafsas.add(this.createDAFSAfromLog(pLog));
        }
        return projectedDafsas;
	}
	
	public List<XLog> projectModelLabelsOn(XLog xLog, List<Map<Integer,String>> projectionLabels)
	{
		List<XLog> projectedLogs = new FastList<XLog>(projectionLabels.size());
		Map<Integer, String> caseIDs = new UnifiedMap<Integer, String>();
		IntArrayList traces;
		String eventName;
		String traceID;
		int translation = 0;
		int iTransition = 0;
		IntArrayList tr;
		IntDAFSAInt fsa = new IntDAFSAInt();
		Integer key = null;
		UnifiedSet<IntArrayList> visited = new UnifiedSet<IntArrayList>();
		int it = 0;
		XTrace trace;
		int i, j;
		for (i = 0; i < xLog.size(); i++)
		{
			trace = xLog.get(i);
			traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
			tr = new IntArrayList(trace.size());
			for (j = 0; j < trace.size(); j++)
			{
				eventName = ((XAttributeLiteral) trace.get(j).getAttributes().get(conceptname)).getValue();//xce.extractName(event);
				if((key = (inverseLabelMapping.get(eventName))) == null)
				{
					//labelMapping.put(translation, eventName);
					inverseLabelMapping.put(eventName, translation);
					key = translation;
					translation++;
				}
				tr.add(key);
			}
			
			if(visited.add(tr))
				fsa.addMinWord(tr);
			//caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
			if((traces = caseTracesMapping.get(tr))==null)
			{
				traces = new IntArrayList();
				caseTracesMapping.put(tr, traces);
			}
			traces.add(it);
			caseIDs.put(it, traceID);
			it++;
			//listTraces.add(traceLabels);
			
//			if((traces = tracesLabelsMapping.get(traceLabels))==null)
//			{
//				traces = new IntArrayList();
//				tracesLabelsMapping.put(traceLabels, traces);
//			}
//			traces.add(it);
		}
		labelMapping = inverseLabelMapping.inverse();
		return projectedLogs;
	}

	public HashMap<String, LinkedList<String>> getNameActions() {
		return nameActions;
	}
}