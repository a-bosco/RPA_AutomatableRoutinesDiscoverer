package au.edu.unimelb.rpadiscovery;

import au.edu.unimelb.rpadiscovery.Utils.Log;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton.Automaton;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributes;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributesList;
import com.google.common.collect.BiMap;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.graph.DirectedEdge;
import org.jbpt.graph.MultiDirectedGraph;
import org.jbpt.hypergraph.abs.Vertex;

import java.util.*;

import static au.edu.unimelb.rpadiscovery.Utils.Functions.printNodeLabelInList;

public class RPSTAdapter {
    private static String TAG_RPST;
    private Log log= Log.getInstance();
    private RPST<DirectedEdge, Vertex> rpst;
    private Automaton dafsa;
    private MultiDirectedGraph g;

    public RPSTAdapter (Automaton dafsa, String TAG){
        try {
            g = dafsa.toMultiDirectedGraph();
        }catch (Exception e){
            e.printStackTrace();
        }
        rpst = new RPST<DirectedEdge, Vertex>(g);
        this.dafsa=dafsa;
        TAG_RPST=TAG;
        log.println(TAG_RPST, 2, "construct RPST..");


    }

    public HashMap<String, EventAttributesList> getActionPayload(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, BiMap<Integer, String> labelMapping, Map<IntArrayList, Integer> traceFrequencies){
        HashMap<String, EventAttributesList> actionPayloadMap = new HashMap<>();
        Vertex source = getVertex(g.getVertices(), dafsa.source().label());
        int count = 0;
        for (IntArrayList tr : tracesEventsAttributes.keySet()) {
            Vertex vertex = source;
            for (int i = 0; i < tr.size(); i++) {
                String label = labelMapping.get(tr.get(i));
                DirectedEdge edge = chooseNextEdgeFrom(g.getEdgesWithSource(vertex), label);
                Vertex nextVertex = edge.getTarget();
                String RPSTLabel = "[" + vertex.getLabel() + "->" + nextVertex.getLabel() + "]";

                EventAttributes eventAttributes = tracesEventsAttributes.get(tr).get(i);
                eventAttributes.setRPSTEdge(RPSTLabel);

                EventAttributesList eventAttributesList = actionPayloadMap.get(RPSTLabel);
                if (eventAttributesList == null) {
                    eventAttributesList = new EventAttributesList(traceFrequencies.get(tr));
                    actionPayloadMap.put(RPSTLabel, eventAttributesList);
                }
                eventAttributesList.add(tracesEventsAttributes.get(tr).get(i));

                vertex = nextVertex;
            }
            count++;
        }
        return actionPayloadMap;
    }
    private static Vertex getVertex(Collection<Vertex> list, String label) {
        for (Vertex vi : list) {
            if (vi.getLabel().equals(label))
                return vi;
        }
        return null;
    }
    private static DirectedEdge chooseNextEdgeFrom(Collection<DirectedEdge> edgesWithSource, String label) {
        for (DirectedEdge di : edgesWithSource) {
            if (di.getLabel().equals(label)) {
                return di;
            }
        }
        return null;
    }

    public RPST<DirectedEdge, Vertex> getRpst() {
        return rpst;
    }

    public TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap(){
        TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap = new TreeMap<>();
        IRPSTNode root = rpst.getRoot();
        log.println(TAG_RPST, 5, "Single Enter - Single Exit sequences from RPST:");
        labelRPSDNodeMap.put(root.getLabel(), constructMap(rpst, root, labelRPSDNodeMap, TAG_RPST));



        return labelRPSDNodeMap;
    }

    private  LinkedList<IRPSTNode> constructMap(RPST<DirectedEdge, Vertex> rpst, IRPSTNode node, TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap, String tag) {
        LinkedList<IRPSTNode> list = new LinkedList<>();
        for (Object no : rpst.getDirectSuccessors(node)) {
            IRPSTNode ni = (IRPSTNode) no;
            if (ni.getLabel().charAt(0) == '[') {
                list.add(ni);
            } else {
                list.add(ni);
                //System.out.println("order " + ni);
                labelRPSDNodeMap.put(ni.getLabel(), constructMap(rpst, ni, labelRPSDNodeMap, tag));
            }
        }
        //log.println(TAG, 5, "BO: "+printNodeLabelInList(list));
        //TODO: ITA modificato alle 4 di notte..stare attenti a questo if, dice che se abbiamo un Rigid, è inutile ordinarlo
        if (node.getLabel().charAt(0) != 'R') {
            sorterList(list, labelRPSDNodeMap);
        }
        log.println(tag, 5, "\n" + node.getLabel() + "  --->  ");
        log.println(tag, 5, printNodeLabelInList(list));
        log.println(tag, 5, "\n\n");
        return list;
    }

    private static void sorterList(LinkedList<IRPSTNode> list, TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap) {
        //log.println(TAG, 5, );
        IRPSTNode first = null;
        int index = -1;
        for (final ListIterator<IRPSTNode> niIterator = list.listIterator(); niIterator.hasNext(); ) {
            final IRPSTNode ni = niIterator.next();
            index++;
            boolean found = true;

            String niEntry = ni.getEntry().getName();
            for (IRPSTNode nii : list) {
                String niiExit = nii.getExit().getName();
                if (niEntry.equals(niiExit)) {
                    found = false;
                    break;
                }

            }
            if (found) {
                first = ni;
                break;
            }

        }
        //log.println(TAG, 5, "LISTA DA ORDINARE: "+printNodeLabelInList(list));
       /* list.set(index, list.get(0));
        list.set(0, first);*/

        //log.println(TAG, 5, first.getLabel());
        for (int i = 0; i < list.size() - 1; i++) {
            list.set(index, list.get(i));
            list.set(i, first);
            // log.println(TAG, 5, "ORDINATO "+i+"-esimo: "+printNodeLabelInList(list));
            index = 0;
            for (IRPSTNode ni : list) {
                if (ni.getEntry().getName().equals(first.getExit().getName()) || (ni != first && ni.getEntry().getName().equals(first.getEntry().getName()))) {
                    first = ni;
                    break;
                }
                index++;
            }

        }

        /*Collections.sort(list,
                new Comparator<IRPSTNode>() {
                    @Override
                    public int compare(IRPSTNode n1, IRPSTNode n2) {

                        return Integer.parseInt(n1.getEntry().getName()) - Integer.parseInt(n2.getEntry().getName());

                    }
                });*/
    }





}
