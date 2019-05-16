package au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer;

import au.edu.unimelb.rpadiscovery.SubPolygon;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.HashMap;
import java.util.LinkedList;

public class EventAttributes implements Comparable<EventAttributes>{
    private HashMap<String, LinkedList<String>> eventAttributesMap = new HashMap<>();
    private LinkedList<String> timestampList = new LinkedList<>();
    private String activityName;
    //private String polygon;
    private SubPolygon subPolygon;
    private String RPSTEdge;
    private IntArrayList trace;
    //TODO: RPST VALUE


    public SubPolygon getSubPolygon() {
        return subPolygon;
    }

    public void setSubPolygon(SubPolygon subPolygon) {
        this.subPolygon = subPolygon;
    }

    public String getRPSTEdge() {
        return RPSTEdge;
    }

    public void setRPSTEdge(String RPSTEdge) {
        this.RPSTEdge = RPSTEdge;
    }

    public void addAttributes(String key, String value) {
        LinkedList<String> valuesList = eventAttributesMap.get(key);
        if (valuesList == null) {
            valuesList = new LinkedList<String>();
            eventAttributesMap.put(key, valuesList);
        }
        valuesList.add(value);
    }

    public int getNumberInstancesFromAttribute(){
        if(eventAttributesMap.size()==0)
            return 0;
        LinkedList<String> attribute = eventAttributesMap.get(eventAttributesMap.keySet().iterator().next());
        return attribute.size();
    }

    public int getNumberInstancesFromTimestamps(){
       return timestampList.size();
    }

    public void addTimestamp(String value) {
        timestampList.add(value);
    }

    public LinkedList<String> getAttributes(String key) {
        return eventAttributesMap.get(key);
    }

    public String getAttribute(String key, int index) {
        return eventAttributesMap.get(key).get(index);
    }

    public String getTimestamp(int index) {
        return timestampList.get(index);
    }

    public HashMap<String, LinkedList<String>> getEventAttributesMap() {
        return eventAttributesMap;
    }

    public void setEventAttributesMap(HashMap<String, LinkedList<String>> eventAttributesMap) {
        this.eventAttributesMap = eventAttributesMap;
    }

    public LinkedList<String> getTimestampList() {
        return timestampList;
    }

    public void setTimestampList(LinkedList<String> timestampList) {
        this.timestampList = timestampList;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public void add(EventAttributes eventAttributes) {
        for (String ki : eventAttributesMap.keySet()) {
            for (String si : eventAttributes.getAttributes(ki)) {
                this.addAttributes(ki, si);
                //eventAttributesMap.get(ki).add(si);
            }
        }

    }

    public IntArrayList getTrace() {
        return trace;
    }

    public void setTrace(IntArrayList trace) {
        this.trace = trace;
    }

    @Override
    public String toString(){
        return RPSTEdge;
    }

    @Override
    public int compareTo(EventAttributes o) {
        return this.getRPSTEdge().compareTo(o.RPSTEdge);
    }
}
