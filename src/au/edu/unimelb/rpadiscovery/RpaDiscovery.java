package au.edu.unimelb.rpadiscovery;

import au.edu.unimelb.rpadiscovery.*;
import au.edu.unimelb.rpadiscovery.Utils.Log;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Automaton;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributes;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributesList;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.ImportEventLog;
import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.JRipClassifier;
import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data.FeatureVector;
import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data.Rule;
import com.google.common.collect.BiMap;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jbpt.algo.tree.rpst.IRPSTNode;

import java.util.*;
import java.util.stream.Collectors;

import static au.edu.unimelb.rpadiscovery.Utils.Functions.*;
import static au.edu.unimelb.rpadiscovery.Utils.LogTAG.*;

public class RpaDiscovery
{
    Log log= Log.getInstance();
    HashMap<String, String> trivialRules ;
    HashMap<String, String> activationRules;

    public  void startProcedureDiscovery(String fileName, String foofahPath, String foofahTimeOut){


        //***************************************IMPORT LOG AND CONSTRUCT DAFSA********************************************
        //
        //

        log.println(TAG_IMPORT, 2, "****STEP: import log...");

        log.println(TAG_IMPORT, 3, "path file: " + fileName);


        ImportEventLog importer = new ImportEventLog();

        Automaton dafsa = null;
        try {
            if (fileName.endsWith("xes.gz") || fileName.endsWith("xes")) {
                dafsa = importer.convertLogToAutomatonFromXes(fileName);
            } else {
                dafsa = importer.convertLogToAutomatonFromXMxml(fileName);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<IntArrayList, Integer> traceFrequencies = dafsa.getCaseFrequencies();

        log.println(TAG_IMPORT, 3, "rename actions: ");
        HashMap<String, LinkedList<String>> nameActions = importer.getNameActions();
        for (String nameAction : nameActions.keySet()) {
            log.println(TAG_IMPORT, 3, "   original name: " + nameAction);
            int i = 0;
            for (String sourceAndAttribute : nameActions.get(nameAction)) {
                if (i == 0) {
                    log.println(TAG_IMPORT, 3, "         name:" + nameAction + " with -> source:" + sourceAndAttribute.substring(0, sourceAndAttribute.indexOf("_")) + "   attributes: " + sourceAndAttribute.substring(sourceAndAttribute.indexOf("_") + 1));
                } else {
                    log.println(TAG_IMPORT, 3, "         name:" + nameAction + i + " with -> source:" + sourceAndAttribute.substring(0, sourceAndAttribute.indexOf("_")) + "   attributes: " + sourceAndAttribute.substring(sourceAndAttribute.indexOf("_") + 1));
                }
                i++;
            }
        }


        BiMap<Integer, String> labelMapping = importer.getLabelMapping();
        HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes = importer.getTracesEventsAttributes();
        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //*******************************************CONSTRUCT RPST FROM DAFSA**********************************************
        //
        //
        log.println(TAG_IMPORT, 2, "****STEP: construct DAFSA...");




        RPSTAdapter rpstAdapter= new RPSTAdapter(dafsa, TAG_RPST);

        HashMap<String, EventAttributesList> actionPayloadMap = rpstAdapter.getActionPayload(tracesEventsAttributes, labelMapping, traceFrequencies);



        log.println(TAG_DAFSA, 3, "DAFSA:");
        log.println(TAG_DAFSA, 3, "\n" + rpstAdapter.getRpst().getGraph().toDOT());
        log.println(TAG_DAFSA, 3, "\n\n");

        log.println(TAG_RPST, 3, "RPST:");
        log.println(TAG_RPST, 3, "\n" + rpstAdapter.getRpst().toDOT());
        log.println(TAG_RPST, 3, "\n\n");
        //log.println(TAG, 5, rpst.getDirectSuccessors(rpst.getRoot()));




        TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap = rpstAdapter.labelRPSDNodeMap();

        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //*******************************************FIND FLAT-POLYGON IN TRACES*********************************************
        //
        //
        log.println(TAG_FLAT_POLYGONS, 2, "****STEP: find flat-plygons..");

        TreeMap<String, LinkedList<SubPolygon>> subPolygonMap = constructSubPolygonMap(labelRPSDNodeMap);
        log.println(TAG_FLAT_POLYGONS, 3, subPolygonMap.keySet());
        log.println(TAG_FLAT_POLYGONS, 3, subPolygonMap);
        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //*******************************************SET FLAT-POLYGON IN TRACES*********************************************
        //
        //
        log.println(TAG_FLAT_POLYGONS_IN_TRACES, 3, "****STEP: flat-polygons in trace...");
        HashMap<IntArrayList, LinkedList<String>> tracePolygonMap = polygonInTrace(tracesEventsAttributes, labelRPSDNodeMap, subPolygonMap);
        log.println(TAG_FLAT_POLYGONS_IN_TRACES, 4, "TRACE POLYGON MAP: " + tracePolygonMap);
        log.println(TAG_FLAT_POLYGONS_IN_TRACES, 4, "POLYGON IN TRACE: " + printPolygonsInTraces(tracesEventsAttributes, tracePolygonMap, labelRPSDNodeMap, TAG_FLAT_POLYGONS_IN_TRACES));

        //System.out.println("\n\n\n");
        //printPolygonsInTraces(tracesEventsAttributes);

        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //*******************************************FIND DEPENDENCIES*********************************************
        //
        //

        log.println(TAG_FIND_DEPENDENCIES, 2, "****STEP: discover dependencies...");

        List<String> discardedActionKeyList = Arrays.asList("insertValue");

        ArrayList<String> discardedAction = new ArrayList<>();
        for (String disc_i : discardedActionKeyList) {
            for (int i = 0; i < nameActions.get(disc_i).size(); i++) {
                if (i == 0) {
                    discardedAction.add(disc_i);
                } else {
                    discardedAction.add(disc_i + i);
                }
            }
        }

        //discardedAction.add("insertValue");

        HashMap<String, LinkedList<Dependency>> dependencyRPSTEdgeMap = discoverDependencies(tracesEventsAttributes, actionPayloadMap, tracePolygonMap, traceFrequencies, discardedAction, foofahPath, foofahTimeOut);
        // System.exit(-1);

        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //*************************************FIND AUTOMATABLE POLYGON CANDIDATE********************************************************
        //
        //

        TreeMap<String, LinkedList<SubPolygon>> automatablePolygonsCandidates = findAutomatablePolygons(actionPayloadMap, labelRPSDNodeMap, subPolygonMap, dependencyRPSTEdgeMap);


        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //*******************************************SET AUTOMATABLE-POLYGON IN TRACES*********************************************
        //
        //
        //System.out.println("automabale polygons candidate: " + automatablePolygonsCandidates);
        log.println(TAG_AUTOMATABLE_POLYGONS_IN_TRACES, 3, "****STEP: flat-polygons in trace...");
        HashMap<IntArrayList, LinkedList<String>> traceAutomatablePolygonMap = polygonInTrace(tracesEventsAttributes, labelRPSDNodeMap, automatablePolygonsCandidates);
        log.println(TAG_AUTOMATABLE_POLYGONS_IN_TRACES, 4, "TRACE POLYGON MAP: " + traceAutomatablePolygonMap);
        printPolygonsInTraces(tracesEventsAttributes, traceAutomatablePolygonMap, labelRPSDNodeMap, TAG_AUTOMATABLE_POLYGONS_IN_TRACES);


        //System.out.println("\n\n\n");
        //printPolygonsInTraces(tracesEventsAttributes);


        //                                                                                                                 *
        //                                                                                                                 *
        //******************************************************************************************************************


        //******************************************************RULES**************************************************************

        log.println(TAG, 1, "OUTPUT: ");

        trivialRules = discoverTrivialRules(labelRPSDNodeMap, automatablePolygonsCandidates, actionPayloadMap, traceAutomatablePolygonMap, tracesEventsAttributes);
        activationRules = discoverRules(traceFrequencies, tracesEventsAttributes, actionPayloadMap, labelRPSDNodeMap, automatablePolygonsCandidates, traceAutomatablePolygonMap);

        for (String polygon : automatablePolygonsCandidates.keySet()) {
            log.println(TAG, 1, polygon + ":");
            log.println(TAG, 1);
            for (SubPolygon subPolygon : automatablePolygonsCandidates.get(polygon)) {
                boolean first = isStarterSubPolygon(tracesEventsAttributes, subPolygon.getName());
                log.println(TAG, 1, "Sequence " + subPolygon.getName());
                log.print(TAG, 1, "\t");
                for (String rpstInSequence : subPolygon.getRPSTNodeSubPolygonList(labelRPSDNodeMap)) {
                    log.simplePrint(TAG, 1, rpstInSequence + actionPayloadMap.get(rpstInSequence).getName() + " ");
                }
                log.println(TAG, 1);
                log.println(TAG, 1, "ACTIVATION RULE: ");
                boolean activation = false;
                if (trivialRules.get(subPolygon.getName()) != null) {
                    if (!trivialRules.get(subPolygon.getName()).contains("PARTIAL"))
                        activation = true;
                    log.println(TAG, 1, " TRIVIAL:");
                    log.println(TAG, 1, trivialRules.get(subPolygon.getName()));
                }

                if (activationRules.get(subPolygon.getName()) != null) {
                    activation = true;
                    log.println(TAG, 1, " ACTIVATION JRIP RULE:");
                    log.println(TAG, 1, activationRules.get(subPolygon.getName()));
                }
                log.println(TAG, 1, "LENGHT: " + subPolygon.getSize());
                if (activation)
                    log.println(TAG, 1, "AUTOMATED EVENTS (automated/automatable): " + subPolygon.getSize() + "/" + subPolygon.getSize());
                else {
                    if (first) {
                        log.println(TAG, 1, "AUTOMATED EVENTS (automated/automatable): STARTER POLYGON ->" + (subPolygon.getSize() - 1) + "/" + (subPolygon.getSize() - 1));
                    } else {
                        log.println(TAG, 1, "AUTOMATED EVENTS (automated/automatable): NO ACTIVATION ->" + (subPolygon.getSize() - 1) + "/" + subPolygon.getSize());
                    }
                }
                log.println(TAG, 1);
            }
        }
        /*System.out.println("6-7");
        System.out.println(eventsAttributesMap.get("[6-7]").getName());
        System.out.println(eventsAttributesMap.get("[6-7]").getPolygon());*/


    }

    private TreeMap<String, LinkedList<SubPolygon>> constructSubPolygonMap(TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap) {
        TreeMap<String, LinkedList<SubPolygon>> outputMap = new TreeMap<>();
        for (String si : labelRPSDNodeMap.keySet()) {
            if (si.charAt(0) == 'P') {
                LinkedList<IRPSTNode> nodesList = labelRPSDNodeMap.get(si);
                LinkedList<SubPolygon> subPolygonsList = new LinkedList();
                IRPSTNode prec = null;
                boolean start = false;
                SubPolygon subPolygon = null;
                int size = 0;
                for (IRPSTNode node : nodesList) {
                    if (!start) {
                        if (node.getLabel().charAt(0) == '[') {
                            start = true;
                            prec = node;
                            subPolygon = new SubPolygon();
                            subPolygon.setStart(node);
                            size = 1;
                            subPolygonsList.add(subPolygon);
                        }
                    } else { //start == true
                        if (node.getLabel().charAt(0) == '[') {
                            size++;
                            prec = node;
                        } else {
                            subPolygon.setFinish(prec);
                            subPolygon.setSize(size);
                            start = false;
                        }
                    }
                }
                if (start) { //if the node list finish with a trivial node
                    subPolygon.setFinish(prec);
                    subPolygon.setSize(size);
                }
                if (subPolygonsList.size() == 1) {
                    subPolygon.setName(si);
                } else {
                    int i = 1;
                    for (SubPolygon sp : subPolygonsList) {
                        sp.setName(si + "-" + i);
                        i++;
                    }
                }
                log.println(TAG_FLAT_POLYGONS, 5, si + ":   " + printNodeLabelInList(nodesList));
                log.println(TAG_FLAT_POLYGONS, 5, subPolygonsList);
                log.println(TAG_FLAT_POLYGONS, 5);
                outputMap.put(si, subPolygonsList);
            }
        }

        return outputMap;
    }
    private static HashMap<IntArrayList, LinkedList<String>> polygonInTrace(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap, TreeMap<String, LinkedList<SubPolygon>> subPolygonMap) {
        HashMap<IntArrayList, LinkedList<String>> tracePolygonMap = new HashMap<>();
        for (IntArrayList trace : tracesEventsAttributes.keySet()) {
            ArrayList<EventAttributes> eventAttributesList = tracesEventsAttributes.get(trace);

            //reset polygon
            for (EventAttributes ea : eventAttributesList)
                ea.setSubPolygon(null);
            //log.println(TAG, 5, eventAttributesList);
            for (String polygon : subPolygonMap.keySet()) {

                //System.out.print(polygon+"   - ");
                boolean containsPolygon = containsPolygon(eventAttributesList, labelRPSDNodeMap.get(polygon), subPolygonMap.get(polygon));
                if (containsPolygon) {
                    //log.println(TAG, 5, "contenuto");
                    LinkedList<String> polygonList = tracePolygonMap.get(trace);
                    if (polygonList == null) {
                        polygonList = new LinkedList<>();
                        tracePolygonMap.put(trace, polygonList);
                    }
                    polygonList.add(polygon);
                    //log.println(TAG, 5, subPolygon.getName());
                    setSubPolygonInTrace(subPolygonMap.get(polygon), eventAttributesList);
                }
            }
        }


        return tracePolygonMap;
    }

    private static boolean containsPolygon(ArrayList<EventAttributes> trace, LinkedList<IRPSTNode> polygon, LinkedList<SubPolygon> subPolygons) {
        //log.println(TAG, 5, subPolygons);
        Iterator<IRPSTNode> polygonIt = polygon.iterator();
        Iterator<EventAttributes> traceIt = trace.iterator();
        SubPolygon subPolygon = null;
        for (SubPolygon spi : subPolygons) {
            subPolygon = spi;
            String labelStart = spi.getStart().getLabel();
            String labelFinish = spi.getFinish().getLabel();
            while (polygonIt.hasNext()) {
                if (polygonIt.next().getLabel().equals(labelStart)) {
                    boolean found = false;
                    while (traceIt.hasNext()) {
                        EventAttributes eventAttributes = traceIt.next();
                        if (eventAttributes.getRPSTEdge().equals(labelStart)) {
                            //TODO: inserire in ogni eventAttribute il poligono corrispondente, nome e subpoligono
                            if (spi.getSize() > 1) {
                                while (true) {
                                    if (polygonIt.hasNext() != traceIt.hasNext())
                                        return false;
                                    if (polygonIt.hasNext()) {
                                        String polygonValue = polygonIt.next().getLabel();
                                        String traceValue = traceIt.next().getRPSTEdge();
                                        if (!polygonValue.equals(traceValue)) {
                                            return false;
                                        }
                                        if (polygonValue.equals(labelFinish))
                                            break;
                                    }
                                }
                            }//if size==1 we already found the polygon
                            found = true;
                            break; //TODO: DO WE NEED BREAK HERE?
                        }
                    }
                    if (!found) {
                        return false;
                    }

                }
            }
        }
        return true;
    }

    private  HashMap<String, LinkedList<Dependency>> discoverDependencies(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, HashMap<String, EventAttributesList> eventsAttributesMap,
                                                                                HashMap<IntArrayList, LinkedList<String>> tracePolygonMap, Map<IntArrayList, Integer> traceFrequencies, ArrayList<String> discardedAction, String foofahPath, String foofahSettings) {

        boolean foofah=( foofahPath!=null && !foofahPath.equals(""))?true:false;

        HashMap<String, LinkedList<Dependency>> rpstEdgeDependencies = new HashMap<>(); //MAP RPST/DEPENDENCY

        //1) same event dependency - constant or increment

        log.println(TAG_FIND_DEPENDENCIES, 4, "discover constants...");
        for (String RPSTEdge : eventsAttributesMap.keySet()) {
            if (!eventsAttributesMap.get(RPSTEdge).isBond()) {
                EventAttributes event = eventsAttributesMap.get(RPSTEdge).iterator().next();
                for (String keyAttribute : event.getEventAttributesMap().keySet()) {
                    String value1 = event.getEventAttributesMap().get(keyAttribute).getFirst();
                    boolean constant = true;
                    for (EventAttributes ea : eventsAttributesMap.get(RPSTEdge)) {
                        LinkedList<String> listValue = ea.getEventAttributesMap().get(keyAttribute);
                        for (String value : listValue) {
                            if (!value.equals(value1)) {
                                constant = false;
                                break;
                            }
                        }
                        if (!constant) //TODO: increment variable
                            break;
                    }
                    if (constant) {
                        LinkedList<Dependency> dependencies = rpstEdgeDependencies.get(RPSTEdge);
                        if (dependencies == null) {
                            dependencies = new LinkedList<>();
                            rpstEdgeDependencies.put(RPSTEdge, dependencies);
                        }
                        Dependency d = new Dependency(event.getRPSTEdge(), event.getRPSTEdge());
                        d.setDependencyAttribute(keyAttribute, keyAttribute);
                        log.println(TAG_FIND_DEPENDENCIES, 5, event.getRPSTEdge() + " - " + event.getActivityName() + " ---> " + keyAttribute + " is a constant");
                        d.setType(Dependency.TYPE_1);
                        dependencies.add(d);
                    }
                }
                //}
            }
        }



        log.println(TAG_FIND_DEPENDENCIES, 4, "discover data dependencies using data transformation...");
        HashMap<String, LinkedList<Dependencies>> polygonDependencies = new HashMap<>();
        HashMap<String, LinkedList<String>> missingDependencyAttributeMap = new HashMap<>();
        LinkedList<String> deleteRpstMissing = new LinkedList<>();

        for (String rpstEdge : eventsAttributesMap.keySet()) {

            if (!eventsAttributesMap.get(rpstEdge).isBond()) {

                EventAttributes toEventAttributesReference = eventsAttributesMap.get(rpstEdge).getFirstEventAttributes();
                //if (!discardedAction.contains(toEventAttributes.getActivityName())) {
                if (toEventAttributesReference.getSubPolygon() != null) {

                    LinkedList<String> missingDependencyAttribute = findMissingDependency(toEventAttributesReference, rpstEdgeDependencies);


                    log.println(TAG_FIND_DEPENDENCIES, 6, "MISSINGG DEPENDENCY " + rpstEdge + ": " + missingDependencyAttribute);

                    if (missingDependencyAttribute.size() > 0) {
                        missingDependencyAttributeMap.put(rpstEdge, missingDependencyAttribute);
                        for (IntArrayList trace : tracesEventsAttributes.keySet()) {

                            if (tracePolygonMap.get(trace).contains(toEventAttributesReference.getSubPolygon().getRPSTPolygonName())) { //  LIST CONTAINS RPST POLYGON, NOT FLAT-POLYGONS!

                                Dependencies dependencies = new Dependencies(trace);
                                LinkedList<Dependencies> linkedListDependencies = polygonDependencies.get(rpstEdge);
                                if (linkedListDependencies == null) {
                                    linkedListDependencies = new LinkedList<>();
                                    polygonDependencies.put(rpstEdge, linkedListDependencies);
                                }
                                linkedListDependencies.add(dependencies);


                                ArrayList<EventAttributes> listEventAttributes = tracesEventsAttributes.get(trace);
                                EventAttributes toEventAttributes = getThisEventAttributes(listEventAttributes, toEventAttributesReference.getRPSTEdge());
                                for (int i = 0; i < listEventAttributes.size(); i++) {
                                    EventAttributes fromEventAttributes = listEventAttributes.get(i);
                                    if (fromEventAttributes.getRPSTEdge().equals(rpstEdge))
                                        break;

                                    Dependency dependency = null;
                                    for (String attributeTo : missingDependencyAttribute) {
                                        for (String attributeFrom : fromEventAttributes.getEventAttributesMap().keySet()) {

                                            if (isTransformation(fromEventAttributes.getEventAttributesMap().get(attributeFrom), toEventAttributes.getEventAttributesMap().get(attributeTo))
                                                    || (foofah
                                                    && FoofahParser.getFoofahTransformation(foofahPath,fromEventAttributes.getEventAttributesMap().get(attributeFrom).subList(0,1), toEventAttributes.getEventAttributesMap().get(attributeTo).subList(0,1), foofahSettings)!=null
                                                    && FoofahParser.getFoofahTransformation(foofahPath,fromEventAttributes.getEventAttributesMap().get(attributeFrom), toEventAttributes.getEventAttributesMap().get(attributeTo), foofahSettings)!=null)
                                            ) {

                                                log.println(TAG_FIND_DEPENDENCIES, 6, "FOUND DEPENDENCY in" + toEventAttributes.getSubPolygon().getName() + " between these variables: " + attributeFrom + ", " + attributeTo + " -->they are respectively in: " + fromEventAttributes.getActivityName() + "-" + fromEventAttributes.getRPSTEdge() + " e " + toEventAttributes.getActivityName() + "-" + toEventAttributes.getRPSTEdge());
                                                log.println(TAG_FIND_DEPENDENCIES, 7, fromEventAttributes.getEventAttributesMap().get(attributeFrom));
                                                log.println(TAG_FIND_DEPENDENCIES, 7, toEventAttributes.getEventAttributesMap().get(attributeTo));
                                                //if(from.getSubPolygon()==null)log.println(TAG, 5, "ERROR: "+from.getRPSTEdge());
                                                log.println(TAG_FIND_DEPENDENCIES, 6, "\n");
                                                dependency = new Dependency(fromEventAttributes.getRPSTEdge(), toEventAttributes.getRPSTEdge());
                                                dependency.setDependencyAttribute(attributeFrom, attributeTo);
                                                dependency.setType(Dependency.TYPE_2);
                                                dependencies.addDependency(dependency);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //}
            }
        }
        log.println(TAG_FIND_DEPENDENCIES, 5, missingDependencyAttributeMap);
        for (String rpstEdge : missingDependencyAttributeMap.keySet()) {
            LinkedList<String> attributeMissing = missingDependencyAttributeMap.get(rpstEdge);
            for (String attribute : attributeMissing) {
                boolean atLeastOneTraceContainsDependency = false;
                boolean allTraceContainsDependency = true;
                for (Dependencies dependencies : polygonDependencies.get(rpstEdge)) {
                    if (dependencies.size() > 0) {
                        for (Dependency dependency : dependencies.getFromToDependenciesList()) {
                            if (dependency.getToAttributes().equals(attribute)) {
                                atLeastOneTraceContainsDependency = true;
                            } else {
                                allTraceContainsDependency = false;
                            }
                            if (atLeastOneTraceContainsDependency && !allTraceContainsDependency)
                                break;
                        }
                        if (atLeastOneTraceContainsDependency && !allTraceContainsDependency)
                            break;
                    } else {
                        allTraceContainsDependency = false;
                    }
                }

                if (allTraceContainsDependency) {

                    log.println(TAG_FIND_DEPENDENCIES, 5, "in " + rpstEdge + "-" + eventsAttributesMap.get(rpstEdge).getName() + " found dependency in attribute: " + attribute);
                    attributeMissing.remove(attribute);
                    if (attributeMissing.size() == 0) {
                        deleteRpstMissing.add(rpstEdge);
                    }



                    for (Dependencies dependencies : polygonDependencies.get(rpstEdge)) {
                        for (Dependency dependency : dependencies.getFromToDependenciesList()) {
                            if (dependency.getToAttributes().equals(attribute)) {

                                LinkedList<Dependency> dependencyList = rpstEdgeDependencies.get(rpstEdge);
                                if (dependencyList == null) {
                                    dependencyList = new LinkedList<>();
                                    rpstEdgeDependencies.put(rpstEdge, dependencyList);
                                }
                                if (!dependencyList.contains(dependency)) {
                                    Dependency d = new Dependency(dependency.getFromRPSTEdge(), dependency.getToRPSTEdge());
                                    d.setDependencyAttribute(dependency.getFromAttributes(), attribute);
                                    d.setType(Dependency.TYPE_2);
                                    dependencyList.add(d);
                                    log.println(TAG_FIND_DEPENDENCIES, 5, rpstEdge + " - " + eventsAttributesMap.get(dependency.getToRPSTEdge()).getName() + " ---> " + attribute + " is a transformation");
                                }
                            }
                        }
                    }

                } else if (atLeastOneTraceContainsDependency) {
                    System.err.println("in discoverDependencies, type2, there is a dependency that not appear in some trace. We can't consider it like a dependency. Maybe it's noise...");
                }
            }
        }

        log.println(TAG_FIND_DEPENDENCIES, 6);
        for (String rpstEdge : deleteRpstMissing) {
            missingDependencyAttributeMap.remove(rpstEdge);
        }
        log.println(TAG_FIND_DEPENDENCIES, 6, "MISSING DEPENDENCIES: " + missingDependencyAttributeMap);


        //3 jrip rules dependency
        log.println(TAG_FIND_DEPENDENCIES, 4, "discover jrip rules for data dependencies");
        for (String rpstEdge : missingDependencyAttributeMap.keySet()) {
            if (!discardedAction.contains(eventsAttributesMap.get(rpstEdge).getName())) {


                LinkedList<String> header = getHeader(tracesEventsAttributes, eventsAttributesMap, tracePolygonMap, rpstEdge);

                HashMap<String, LinkedList<String>> labelValueList = new HashMap<>();
                for (String si : missingDependencyAttributeMap.get(rpstEdge)) {
                    labelValueList.put(si, new LinkedList<>());
                }

                if (header.size() > 0) {
                    List<FeatureVector> featureVectors = new LinkedList<>();
                    for (IntArrayList trace : tracesEventsAttributes.keySet()) {
                        if (tracePolygonMap.get(trace).contains(eventsAttributesMap.get(rpstEdge).getPolygon().getRPSTPolygonName())) {
                            for (int i = 0; i < traceFrequencies.get(trace); i++) {
                                HashMap<String, String> rowTable = new HashMap<>();
                                for (String headerAttribute : header) {
                                    rowTable.put(headerAttribute, getRowValue(headerAttribute, tracesEventsAttributes.get(trace), i));
                                }
                                for (String si : missingDependencyAttributeMap.get(rpstEdge)) {
                                    labelValueList.get(si).add(getRowValue(rpstEdge + ":" + si, tracesEventsAttributes.get(trace), i));
                                }

                                FeatureVector featureVector = new FeatureVector(rowTable);
                                featureVectors.add(featureVector);
                            }

                        }
                    }

                    //CLASSIFICAZIONE
                    log.println(TAG_FIND_DEPENDENCIES, 5, "CLASSIFICATION  " + rpstEdge + "-" + eventsAttributesMap.get(rpstEdge).getName());
                    for (String attribute : labelValueList.keySet()) {
                        log.println(TAG_FIND_DEPENDENCIES, 5, "---->VARIABLE:" + attribute);
                        for (int j = 0; j < featureVectors.size(); j++) {
                            featureVectors.get(j).setLabel(labelValueList.get(attribute).get(j));
                        }

                        log.println(TAG_FIND_DEPENDENCIES, 7, header);
                        for (FeatureVector fv : featureVectors) {
                            for (String ss : header) {
                                //HashMap<String, String> attributes = fv.getAttributes();

                                log.print(TAG_FIND_DEPENDENCIES, 7, fv.attributes.get(ss) + ",");
                            }
                            log.println(TAG_FIND_DEPENDENCIES, 7, " -> " + fv.label);
                        }
                        for (String attr : header) {
                            boolean isANumber = firstValueNotNullIsANumber(featureVectors, attr);
                            for (int i = 0; i < featureVectors.size(); i++) {
                                String oldValue = featureVectors.get(i).getAttributes().get(attr);
                                if (oldValue.equals("-NULL"))
                                    if (isANumber) {
                                        featureVectors.get(i).getAttributes().replace(attr, "" + Integer.MIN_VALUE);
                                    }
                            }
                        }
                        log.println(TAG_FIND_DEPENDENCIES, 6, "jrip");
                        //log.println(TAG, 5, featureVectors);
                        log.println(TAG_FIND_DEPENDENCIES, 6, "RULE: ");

                        JRipClassifier jRipClassifier = new JRipClassifier();
                        LinkedList<Rule> rules = jRipClassifier.classify( featureVectors, rpstEdge + "-" + eventsAttributesMap.get(rpstEdge).getName() + " var: " + attribute, null, 1.0, false);
                        if (rules == null)
                            continue;
                        List<String> labels = featureVectors.stream().map(obj -> obj.label).distinct().collect(Collectors.toList());
                        int[] labelsCount = new int[labels.size()];
                        for (FeatureVector fv : featureVectors) {
                            for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                                if (labels.get(labelIndex).equals(fv.label)) {
                                    labelsCount[labelIndex]++;
                                    break;
                                }
                            }
                        }
                        int[] labelsCountWithConfidence1 = new int[labels.size()];


                        for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                            for (Rule rule : rules) {
                                if (rule.getMissingClassification() == 0) {
                                    if (rule.getLabel().equals(labels.get(labelIndex))) {
                                        labelsCountWithConfidence1[labelIndex] += rule.getTotalNumberOfInstances();
                                        break;
                                    }
                                }
                            }
                        }
                        boolean foundDependency = true;
                        for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                            if (labelsCount[labelIndex] != labelsCountWithConfidence1[labelIndex]) {
                                foundDependency = false;
                            }
                        }
                        log.print(TAG_FIND_DEPENDENCIES, 6, labels);
                        log.println(TAG_FIND_DEPENDENCIES, 6, Arrays.toString(labelsCount));
                        log.println(TAG_FIND_DEPENDENCIES, 6, Arrays.toString(labelsCountWithConfidence1));
                        if (foundDependency) {
                            log.println(TAG_FIND_DEPENDENCIES, 6, "FOUND DEPENDENCY!!!!");
                            missingDependencyAttributeMap.get(rpstEdge).remove(attribute);
                            LinkedList<Dependency> dependencyList = rpstEdgeDependencies.get(rpstEdge);
                            if (dependencyList == null) {
                                dependencyList = new LinkedList<>();
                                rpstEdgeDependencies.put(rpstEdge, dependencyList);
                            }
                            for (Rule rule : rules) {
                                for (String antecedentRule : rule.getRpstAttributeList()) {
                                    //System.out.println(antecedentRule);
                                    try {
                                        String[] splitRpstAtrribute = antecedentRule.split(":");
                                        String fromRpst = splitRpstAtrribute[0];
                                        String attributeFrom = splitRpstAtrribute[1];

                                        Dependency dependency = new Dependency(fromRpst, rpstEdge, attributeFrom, attribute);
                                        //System.out.println("fromRpst: " + fromRpst + "  toRpst: " + rpstEdge + "    attrFrom:" + attributeFrom + "   attrTo:" + attribute);
                                        dependency.setType(Dependency.TYPE_3);
                                        if (rule.getAntecedent().contains("-NULL") || rule.getAntecedent().contains("" + Integer.MIN_VALUE)) {
                                            dependency.setPositionalDependency(true);
                                        }
                                        dependency.setRule(rule);
                                        if (!dependencyList.contains(dependency)) {
                                            //System.out.println("add rule");
                                            dependencyList.add(dependency);
                                            log.println(TAG_FIND_DEPENDENCIES, 5, rpstEdge + " - " + eventsAttributesMap.get(dependency.getToRPSTEdge()).getName() + " ---> " + attribute + " has a rule" + ((dependency.isPositionalDependency()) ? " positional dependency" : "dependency") + ", rule:" + dependency.getRule().getAntecedent() + " => " + dependency.getRule().getLabel());
                                        }
                                    } catch (Exception e) {
                                        continue;
                                    }
                                }
                            }


                        }

                        log.println(TAG_FIND_DEPENDENCIES, 6);


                        log.println(TAG_FIND_DEPENDENCIES, 6, "\n");


                    }
                }
            }

        }

        log.println(TAG_FIND_DEPENDENCIES, 6, "MISSING DEPENDENCIES before: " + missingDependencyAttributeMap);
        log.println(TAG_FIND_DEPENDENCIES, 6);
        deleteRpstMissing = new LinkedList<>();
        for (String rpstEdge : missingDependencyAttributeMap.keySet()) {
            if (missingDependencyAttributeMap.get(rpstEdge).size() == 0)
                deleteRpstMissing.add(rpstEdge);
        }

        for (String rpstEdge : deleteRpstMissing) {
            missingDependencyAttributeMap.remove(rpstEdge);
        }
        log.println(TAG_FIND_DEPENDENCIES, 6, "MISSING DEPENDENCIES now: " + missingDependencyAttributeMap);


        return rpstEdgeDependencies;

    }
    private static LinkedList<String> findMissingDependency(EventAttributes eventAttributes, HashMap<String, LinkedList<Dependency>> rpstEdgeDependencies) {
        LinkedList<String> missingDependency = new LinkedList<>(eventAttributes.getEventAttributesMap().keySet());
        if (rpstEdgeDependencies.get(eventAttributes.getRPSTEdge()) == null)
            return missingDependency;

        for (String attribute : eventAttributes.getEventAttributesMap().keySet()) {
            for (Dependency dependency : rpstEdgeDependencies.get(eventAttributes.getRPSTEdge())) {
                if (dependency.getToAttributes().equals(attribute)) {
                    missingDependency.remove(attribute);
                    break;
                }
            }

        }

        return missingDependency;
    }

    private static boolean isTransformation(LinkedList<String> from, LinkedList<String> to) {

        if (from.size() != to.size()) {
            System.err.println("different size data transformation");
        }
        Iterator<String> itFrom = from.iterator();
        Iterator<String> itTo = to.iterator();
        boolean sameValues = true;
        boolean subString = true;
        while (itFrom.hasNext()) {
            String valueFrom = itFrom.next();
            String valueTo = itTo.next();
            if (!valueFrom.equals(valueTo)) {
                sameValues = false;
            }
            if (!valueFrom.contains(valueTo)) {
                subString = false;
            }
        }
        if (sameValues) {
            //System.out.println("same values");
            return true;
        }

        if (subString) { //TODO: How subString is extracted?
            //System.out.println("subString");
            return true;
        }

        return false;

    }
    private static LinkedList<String> getHeader(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, HashMap<String, EventAttributesList> eventsAttributesMap, HashMap<IntArrayList, LinkedList<String>> tracePolygonMap, String rpstEdge) {
        LinkedList<String> header = new LinkedList<>();

        for (IntArrayList trace : tracesEventsAttributes.keySet()) {
            if (tracePolygonMap.get(trace).contains(eventsAttributesMap.get(rpstEdge).getPolygon().getRPSTPolygonName())) {
                ArrayList<EventAttributes> listEventAttributes = tracesEventsAttributes.get(trace);
                for (int i = 0; i < listEventAttributes.size(); i++) {
                    EventAttributes fromEventAttributes = listEventAttributes.get(i);
                    if (fromEventAttributes.getRPSTEdge().equals(rpstEdge)) {
                        break;
                    }
                    String columnHeader = fromEventAttributes.getRPSTEdge() + fromEventAttributes.getActivityName() + ":";
                    for (String attribute : fromEventAttributes.getEventAttributesMap().keySet()) {
                        String headerValue = columnHeader + attribute;
                        if (!header.contains(headerValue)) {
                            header.add(headerValue);
                        }
                    }
                }
            }
        }
        return header;
    }
    private static String getRowValue(String s, ArrayList<EventAttributes> trace, int i) {
        String rpstEdge = s.substring(0, s.indexOf(']') + 1);

        String name = s.substring(s.indexOf(']') + 1, s.indexOf(':'));
        String attribute = s.substring(s.indexOf(':') + 1);
        for (EventAttributes ea : trace) {
            if (ea.getRPSTEdge().equals(rpstEdge)) {
                if (ea.getEventAttributesMap().get(attribute) != null) {
                    return ea.getEventAttributesMap().get(attribute).get(i);
                } else
                    break;

            }
        }
        return "-NULL";
    }
    private static EventAttributes getThisEventAttributes(ArrayList<EventAttributes> listEventAttributes, String rpstEdge) {
        for (EventAttributes ea : listEventAttributes) {
            if (ea.getRPSTEdge().equals(rpstEdge))
                return ea;
        }
        return null;
    }
    private static void setSubPolygonInTrace(LinkedList<SubPolygon> subPolygonList, ArrayList<EventAttributes> eventAttributesList) {
        for (SubPolygon subPolygon : subPolygonList) {
            //System.out.println("set polygon: "+subPolygon.getName());
            boolean start = false;
            Iterator<EventAttributes> it = eventAttributesList.iterator();
            //System.out.println("we"+eventAttributesList);
            while (true) {
                EventAttributes event = it.next();
                //System.out.println("event "+event.getRPSTEdge()+" value: "+((event.getEventAttributesMap().get("Label")!=null)?event.getEventAttributesMap().get("Label"):""));
                if (!start) {
                    if (subPolygon.getStart().getLabel().equals(event.getRPSTEdge())) {
                        start = true;
                    }
                }
                if (start) {
                    event.setSubPolygon(subPolygon);
                    //System.out.print(event.getRPSTEdge()+" ");
                    if (subPolygon.getFinish().getLabel().equals(event.getRPSTEdge()))
                        break;
                }
            }
        }
    }
    private static String firstValueNotNullNOPol(List<FeatureVector> featureVectors, String attr) {

        for (int i = 0; i < featureVectors.size(); i++) {
            String value = featureVectors.get(i).getAttributes().get(attr);
            if (!value.equals("-NULL"))
                if (featureVectors.get(i).getLabel().equals("NO"))
                    return value;
        }

        if (firstValueNotNullIsANumber(featureVectors, attr))
            return "" + Integer.MIN_VALUE;
        return "-NULL";
    }
    private static boolean firstValueNotNullIsANumber(List<FeatureVector> featureVectors, String attr) {

        for (int i = 0; i < featureVectors.size(); i++) {
            String value = featureVectors.get(i).getAttributes().get(attr);
            if (!value.equals("-NULL"))                if (!tryParseDouble(value))
                return false;
        }
        return true;
    }
    private  TreeMap<String, LinkedList<SubPolygon>> findAutomatablePolygons(HashMap<String, EventAttributesList> eventsAttributesMap, TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap, TreeMap<String, LinkedList<SubPolygon>> subPolygonMap, HashMap<String, LinkedList<Dependency>> dependencyRPSTEdgeMap) {
        TreeMap<String, LinkedList<SubPolygon>> automatablePolygonsCandidates = new TreeMap<>();
        //String[] activityNameWithDependentPrefix = {"openW", "push", "closeW", "click", "minimize", "Push"};
        for (String SESEName : labelRPSDNodeMap.keySet()) {
            if (SESEName.charAt(0) == 'P') {
                log.println(TAG, 5, SESEName);
                Iterator<IRPSTNode> it = labelRPSDNodeMap.get(SESEName).iterator();
                LinkedList<SubPolygon> subPolygonsList = null;
                for (SubPolygon subPolygon : subPolygonMap.get(SESEName)) {
                    boolean start = false;
                    LinkedList<IRPSTNode> subPolygonEdgeList = new LinkedList<>();
                    while (true) {
                        IRPSTNode node = it.next();

                        if (!start) {
                            if (subPolygon.getStart().getLabel().equals(node.getLabel())) {
                                start = true;
                            }
                        }
                        if (start) {

                            subPolygonEdgeList.add(node);
                            if (subPolygon.getFinish().getLabel().equals(node.getLabel()))
                                break;
                        }
                    }

                    boolean dependentSequencyStart = false;
                    SubPolygon automatableSubPolygonCandidate = null;
                    int lenght = 0;

                    for (IRPSTNode RPSTEdge : subPolygonEdgeList) {
                        //System.out.println("dependend? "+RPSTEdge.getLabel());
                        //System.out.println(dependencyRPSTEdgeMap);
                        if (isDependent(RPSTEdge, dependencyRPSTEdgeMap, eventsAttributesMap)) {
                            //System.out.println("YES IT IS!");
                            if (!dependentSequencyStart) {
                                dependentSequencyStart = true;
                                automatableSubPolygonCandidate = new SubPolygon();
                                automatableSubPolygonCandidate.setName(subPolygon.getName());
                                automatableSubPolygonCandidate.setStart(RPSTEdge);
                                lenght = 0;
                                subPolygonsList = automatablePolygonsCandidates.get(SESEName);
                                if (subPolygonsList == null) {
                                    subPolygonsList = new LinkedList<>();
                                    automatablePolygonsCandidates.put(SESEName, subPolygonsList);
                                }
                                subPolygonsList.add(automatableSubPolygonCandidate);
                            }
                            //if it is dependent and sequence is already started
                            lenght++;
                            automatableSubPolygonCandidate.setFinish(RPSTEdge);
                            automatableSubPolygonCandidate.setSize(lenght);

                        } else {
                            dependentSequencyStart = false;
                        }
                    }
                }
                if (subPolygonsList != null) { //name polygon
                    boolean sameList = false;
                    if (subPolygonsList.size() == subPolygonMap.get(SESEName).size()) {
                        sameList = true;
                        for (SubPolygon sp : subPolygonsList) {
                            if (!subPolygonMap.get(SESEName).contains(sp)) {
                                sameList = false;
                                break;
                            }
                        }
                    }
                    if (sameList) {
                        automatablePolygonsCandidates.replace(SESEName, subPolygonMap.get(SESEName));
                    } else {
                        int i = 1;
                        for (SubPolygon sp : subPolygonsList) {
                            sp.setName(sp.getName() + "-" + i);
                            i++;
                        }
                    }

                    //log.println(TAG, 5, SESEName + ":   " + printNodeLabelInList(labelRPSDNodeMap.get(SESEName)));
                    log.println(TAG, 5, SESEName + ":   " + printNodeLabelInListWithName(labelRPSDNodeMap.get(SESEName), eventsAttributesMap));
                    log.println(TAG, 5, subPolygonsList);
                    log.println(TAG, 5);
                }
            }
        }
        return automatablePolygonsCandidates;
    }
    private static boolean isDependent(IRPSTNode rpstEdge, HashMap<String, LinkedList<Dependency>> dependencyRPSTEdgeMap, HashMap<String, EventAttributesList> eventsAttributesMap) {
        if (!dependencyRPSTEdgeMap.containsKey(rpstEdge.getLabel()))
            return false;

        LinkedList<Dependency> dependencyList = dependencyRPSTEdgeMap.get(rpstEdge.getLabel());
        //System.out.println(dependencyList);
        for (String attribute : eventsAttributesMap.get(rpstEdge.getLabel()).getFirstEventAttributes().getEventAttributesMap().keySet()) {
            //System.out.println(attribute);
            if (!dependencyListContainsAttribute(dependencyList, rpstEdge.getLabel(), attribute)) {
                return false;
            }
        }
        return true;
    }

    private static boolean dependencyListContainsAttribute(LinkedList<Dependency> dependencyList, String rpstEdge, String attribute) {
        for (Dependency dependency : dependencyList) {
            if (dependency.getToAttributes().equals(attribute)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStarterSubPolygon(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, String polygonName) {
        for (IntArrayList trace : tracesEventsAttributes.keySet()) {
            for (EventAttributes ea : tracesEventsAttributes.get(trace)) {
                if (ea.getSubPolygon() != null && ea.getSubPolygon().getName().equals(polygonName)) {
                    return true;
                } else
                    break;
            }
        }
        return false;
    }

    private static HashMap<String, String> discoverTrivialRules(TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap, TreeMap<String, LinkedList<SubPolygon>> automatablePolygonsCandidates, HashMap<String, EventAttributesList> eventsAttributesMap, HashMap<IntArrayList, LinkedList<String>> traceAutomatablePolygonMap, HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes) {
        HashMap<String, String> trivialRules = new HashMap<>();
        for (String polygon : automatablePolygonsCandidates.keySet()) {
            int index = 0;
            for (SubPolygon subPolygon : automatablePolygonsCandidates.get(polygon)) {
                //System.out.println("out: "+subPolygon.getName());
                if (index >= 0) {
                    //  System.out.println("in: "+subPolygon.getName());
                    LinkedList<String> eventsBeforeSubPolygon = new LinkedList<>();
                    for (IntArrayList trace : tracesEventsAttributes.keySet()) {
                        String rpstBefore = null;
                        if (traceAutomatablePolygonMap.get(trace).contains(polygon)) {
                            ArrayList<EventAttributes> traceEvents = tracesEventsAttributes.get(trace);
                            for (EventAttributes ea : traceEvents) {

                                if (ea.getRPSTEdge().equals(subPolygon.getStart().getLabel())) {
                                    if (rpstBefore != null && !eventsBeforeSubPolygon.contains(rpstBefore)) {
                                        eventsBeforeSubPolygon.add(rpstBefore);
                                    }
                                    break;
                                }
                                rpstBefore = ea.getRPSTEdge();

                            }
                        }
                    }
                    boolean partial = false;

                    for (IntArrayList trace : tracesEventsAttributes.keySet()) {
                        LinkedList<String> removeRPST = new LinkedList<>();
                        String rpstBefore = null;
                        ArrayList<EventAttributes> traceEvents = tracesEventsAttributes.get(trace);
                        for (String beforeRPSTInList : eventsBeforeSubPolygon) {
                            if (eventAttributesListContainRPST(traceEvents, beforeRPSTInList)) {
                                for (int i = 0; i < traceEvents.size(); i++) {

                                    if (traceEvents.get(i).getRPSTEdge().equals(beforeRPSTInList)) {
                                        if (i + 1 >= traceEvents.size() || !traceEvents.get(i + 1).getRPSTEdge().equals(subPolygon.getStart().getLabel())) {
                                            partial = true;
                                            removeRPST.add(beforeRPSTInList);
                                            break;
                                        }
                                    }

                                }
                            }
                        }
                        if (removeRPST.size() > 0) {
                            eventsBeforeSubPolygon.removeAll(removeRPST);
                        }

                    }

                    if (eventsBeforeSubPolygon.size() > 0) {
                        String rule = (partial) ? "PARTIAL:\n" : "";
                        rule += "after: ";
                        for (String si : eventsBeforeSubPolygon) {
                            rule += si + " ";
                        }
                        rule += "\n";
                        trivialRules.put(subPolygon.getName(), rule);
                    }

                }
                index++;
            }
        }
        return trivialRules;
    }
    private static boolean eventAttributesListContainRPST(ArrayList<EventAttributes> trace, String beforeRPSTInList) {
        for (EventAttributes ea : trace) {
            if (ea.getRPSTEdge().equals(beforeRPSTInList))
                return true;
        }
        return false;
    }

    private  HashMap<String, String> discoverRules(Map<IntArrayList, Integer> traceFrequencies, HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, HashMap<String, EventAttributesList> eventsAttributesMap, TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap, TreeMap<String, LinkedList<SubPolygon>> automatableSubPolygonMap, HashMap<IntArrayList, LinkedList<String>> tracePolygonMap) {
        HashMap<String, String> subPolygonActivationRule = new HashMap<>();
        for (String polygon : automatableSubPolygonMap.keySet()) {

            for (SubPolygon subPolygon : automatableSubPolygonMap.get(polygon)) {
                if (true) {//subPolygon.getStart().getLabel().equals("[7->16]")){
                    LinkedList<String> beforeSubPolygonCommonEdge = new LinkedList<>();
                    boolean firstTrace = true;
                    boolean atLeastOneTimeIsFirstPolygon = false;
                    for (IntArrayList trace : tracePolygonMap.keySet()) {

                        if (tracePolygonMap.get(trace).contains(polygon)) {
                            LinkedList<String> tempCheck = new LinkedList<>();

                            for (EventAttributes eventAttribute : tracesEventsAttributes.get(trace)) {

                                if (eventAttribute.getSubPolygon() != null && eventAttribute.getSubPolygon().getName().equals(subPolygon.getName())) {
                                    if (firstTrace)
                                        firstTrace = false;
                                    else if (tempCheck.size() == 0)
                                        atLeastOneTimeIsFirstPolygon = true;
                                    break;
                                }

                                if (firstTrace) {
                                    beforeSubPolygonCommonEdge.add(eventAttribute.getRPSTEdge());
                                } else {
                                    tempCheck.add(eventAttribute.getRPSTEdge());
                               /* if (!beforeSubPolygonCommonEdge.contains(eventAttribute.getRPSTEdge()))
                                    beforeSubPolygonCommonEdge.remove(eventAttribute.getRPSTEdge());*/
                                }
                            }
                            if (atLeastOneTimeIsFirstPolygon) {
                                log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "polygon " + subPolygon.getName() + " at least once appears like first polygon in the trace");
                                beforeSubPolygonCommonEdge.clear();
                                break;
                            } else if (tempCheck.size() > 0) {

                                for (String si : tempCheck) {
                                    if (!beforeSubPolygonCommonEdge.contains(si))
                                        beforeSubPolygonCommonEdge.add(si);
                                }

                            }

                        }

                    }



                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "***************************************DISCOVER RULES:");
                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "polygon: " + subPolygon.getName());

                    boolean start = false;
                    boolean finish = false;

                    for (IRPSTNode node : labelRPSDNodeMap.get(polygon)) {
                        if (node.getLabel().equals(subPolygon.getFinish().getLabel())) {
                            finish = true;
                        }
                        if (node.getLabel().equals(subPolygon.getStart().getLabel())) {
                            start = true;
                        }
                        if (start) {
                            log.simplePrint(TAG_DISCOVER_ACTIVATION_RULES, 5, node.getLabel() + " ");
                        }
                        if (finish) {
                            break;
                        }
                    }
                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5);
                    start = false;
                    finish = false;

                    for (IRPSTNode node : labelRPSDNodeMap.get(polygon)) {
                        if (node.getLabel().equals(subPolygon.getFinish().getLabel())) {
                            finish = true;
                        }
                        if (node.getLabel().equals(subPolygon.getStart().getLabel())) {
                            start = true;
                        }
                        if (start) {
                            log.simplePrint(TAG_DISCOVER_ACTIVATION_RULES, 5, eventsAttributesMap.get(node.getLabel()).iterator().next().getActivityName() + " ");
                        }
                        if (finish) {
                            break;
                        }
                    }
                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5);

                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "before polygon: ");
                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, beforeSubPolygonCommonEdge);
                    for (String stmp : beforeSubPolygonCommonEdge) {
                        log.simplePrint(TAG_DISCOVER_ACTIVATION_RULES, 5, eventsAttributesMap.get(stmp).iterator().next().getActivityName() + " ");
                    }
                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5);



                    List<FeatureVector> featureVectors = new LinkedList<>();
                    Set<String> header = new HashSet<>();
                    boolean JRipClassification = false;
                    if (beforeSubPolygonCommonEdge.size() > 0) {


                        for (IntArrayList trace : tracesEventsAttributes.keySet()) {
                            String label;
                            if (tracePolygonMap.get(trace).contains(polygon)) {
                                label = subPolygon.getName();
                            } else {
                                label = "NO";
                                JRipClassification = true;
                            }

                            for (int i = 0; i < traceFrequencies.get(trace); i++) {

                                HashMap<String, String> attributes = new HashMap<>();
                                for (EventAttributes eventAttribute : tracesEventsAttributes.get(trace)) {

                                    if (beforeSubPolygonCommonEdge.contains(eventAttribute.getRPSTEdge()) && eventAttribute.getEventAttributesMap().size() > 0) {
                                        String RPSTEdge = eventAttribute.getRPSTEdge() + eventAttribute.getActivityName() + ":";
                                        for (String attribute : eventAttribute.getEventAttributesMap().keySet()) {
                                            String value = eventAttribute.getEventAttributesMap().get(attribute).get(i);
                                            String attributeName = RPSTEdge + attribute;
                                            attributes.put(attributeName, value);
                                            header.add(attributeName);
                                        }
                                    }
                                }

                                if (attributes.size() > 0) {
                                    featureVectors.add(new FeatureVector(attributes, label));
                                }

                            }


                        }
                        for (FeatureVector fv : featureVectors) {
                            HashMap<String, String> attributes = fv.getAttributes();
                            for (String att : header) {
                                String s = attributes.get(att);
                                if (s == null) {
                                    attributes.put(att, "-NULL");
                                }
                            }
                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 6, attributes + "  -> " + fv.label);
                        }
                        log.println(TAG_DISCOVER_ACTIVATION_RULES, 6);
                        //CLASSIFICAZIONE
                        if (JRipClassification) {

                            for (String attr : header) {
                                String newValue = firstValueNotNullNOPol(featureVectors, attr);
                                for (int i = 0; i < featureVectors.size(); i++) {
                                    String oldValue = featureVectors.get(i).getAttributes().get(attr);
                                    if (oldValue.equals("-NULL"))
                                        featureVectors.get(i).getAttributes().replace(attr, newValue);
                                }
                            }
                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "jrip");
                            //log.println(TAG, 5, featureVectors);
                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "RULE: ");

                            JRipClassifier jRipClassifier = new JRipClassifier();
                            LinkedList<Rule> rules = jRipClassifier.classify( featureVectors, subPolygon.getName(), null, 1.0, false);


                            List<String> labels = featureVectors.stream().map(obj -> obj.label).distinct().collect(Collectors.toList());
                            labels.remove("NO");
                            int[] labelsCount = new int[labels.size()];
                            for (FeatureVector fv : featureVectors) {
                                for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                                    if (labels.get(labelIndex).equals(fv.label)) {
                                        labelsCount[labelIndex]++;
                                        break;
                                    }
                                }
                            }
                            int[] labelsCountWithConfidence1 = new int[labels.size()];


                            for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                                for (Rule rule : rules) {
                                    if (rule.getMissingClassification() == 0) {
                                    /*System.out.println("label index: " + labels.get(labelIndex));
                                    System.out.println(rule.getAntecedent() + " => " + rule.getLabel() + "               " + rule.getTotalNumberOfInstances());
                                    System.out.println();*/
                                        if (rule.getLabel().equals(labels.get(labelIndex))) {
                                            labelsCountWithConfidence1[labelIndex] += rule.getTotalNumberOfInstances();
                                            // break;
                                        }
                                    }
                                }
                            }
                            boolean foundDependency = true;
                            for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                                if (labelsCount[labelIndex] != labelsCountWithConfidence1[labelIndex]) {
                                    foundDependency = false;
                                }
                            }
                            log.print(TAG_DISCOVER_ACTIVATION_RULES, 6, labels);
                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 6, Arrays.toString(labelsCount));
                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 6, Arrays.toString(labelsCountWithConfidence1));
                            if (foundDependency) {
                                log.println(TAG_DISCOVER_ACTIVATION_RULES, 6, "FOUND activations RULE:");
                                String ruleString = "";

                                for (Rule rule : rules) {
                                    if (!rule.getLabel().equals("NO")) {
                                        ruleString += rule.getAntecedent() + "    ===>  activation for " + subPolygon.getName() + "     (label: " + rule.getLabel() + ")\n";
                                        log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "     " + rule.getAntecedent() + "    ===>  activation for " + subPolygon.getName() + "     (label: " + rule.getLabel() + ")");
                                    } else {
                                        ruleString += "if there is the opposit of this: " + rule.getAntecedent() + "    ===>  activation for " + subPolygon.getName() + "     (label: " + rule.getLabel() + ")\n";
                                        log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "     if I haven't this: " + rule.getAntecedent() + "    ===>  activation for " + subPolygon.getName() + "     (label: " + rule.getLabel() + ")");
                                    }
                                }

                                subPolygonActivationRule.put(subPolygon.getName(), ruleString);


                            }


                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 5);
                        } else {
                            log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "no jrip");
                        }


                    } else {
                        //NESSUN EVENTATTRIBUTE PRIMA DEL POLIGONO
                    }

                    log.println(TAG_DISCOVER_ACTIVATION_RULES, 5, "\n");
                }
            }

        }
        return subPolygonActivationRule;
    }




}
