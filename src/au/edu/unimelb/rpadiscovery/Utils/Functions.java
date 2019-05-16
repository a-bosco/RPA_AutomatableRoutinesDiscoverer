package au.edu.unimelb.rpadiscovery.Utils;

import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributes;
import au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer.EventAttributesList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jbpt.algo.tree.rpst.IRPSTNode;

import java.util.*;

public class Functions {
    public static boolean isLong(String a)
    {

        try {

            Long.valueOf(a);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }

    }

    public static boolean isDouble(String a)
    {

        try {

            Double.valueOf(a);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }

    }
    public static boolean tryParseDouble(String value){
        try{
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    public static String printNodeLabelInListWithName(List<IRPSTNode> list, HashMap<String, EventAttributesList> eventsAttributesMap) {
        String s = "";
        for (IRPSTNode ni : list) {
            if (eventsAttributesMap.get(ni.getLabel()) == null) {
                s += "<**" + ni.getLabel() + "**>, ";
            } else
                s += "<" + ni.getLabel() + " - " + eventsAttributesMap.get(ni.getLabel()).getName() + ">, ";
        }
        return s;
    }
    public static String printNodeLabelInList(List<IRPSTNode> list) {
        String s = "";
        for (IRPSTNode ni : list) {
            s += ni.getLabel() + ", ";
        }
        return s;
    }
    public static String printPolygonsInTraces(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes) {
        String s="\n";
        for (IntArrayList trace : tracesEventsAttributes.keySet()) {
            ArrayList<EventAttributes> traceEA = tracesEventsAttributes.get(trace);
            for (EventAttributes ea : traceEA)
                if (ea.getSubPolygon() != null)
                   s+=ea.getSubPolygon().getName() + "  ";
            s+="\n";
        }
        return s;
    }
    public static String printPolygonsInTraces(HashMap<IntArrayList, ArrayList<EventAttributes>> tracesEventsAttributes, HashMap<IntArrayList, LinkedList<String>> tracePolygonMap, TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap, String tag) {
        String output="";
        for (IntArrayList trace : tracesEventsAttributes.keySet()) {
            String s = "";
            for (EventAttributes event : tracesEventsAttributes.get(trace)) {
                s += event.getRPSTEdge() + ", ";
            }
            s += "\n";
            for (String polygon : tracePolygonMap.get(trace)) {
                s += polygon + " ---- " + printNodeLabelInList(labelRPSDNodeMap.get(polygon)) + "\n";
            }
            s += "\n";
            output+=s+"\n";
        }
        return output;
    }
}
