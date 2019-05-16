package au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer;

import au.edu.unimelb.rpadiscovery.SubPolygon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class EventAttributesList implements Iterable<EventAttributes>{
    private LinkedList<EventAttributes> list=null;
    private boolean first=true;
    private String name;
    private int frequency=0;
    private HashMap<String, HashSet<String>> attributesKeySet =new HashMap<>(); //name event on rpstEdge, attributes
    private boolean bond=false;
    private LinkedList<String> otherNames=new LinkedList<>();

    public EventAttributesList(){
        list=new LinkedList<>();
    }
    public EventAttributesList(int frequency){
        list=new LinkedList<>();
        this.frequency=frequency;
    }
    public int size(){
        return list.size();
    }


    public void add(EventAttributes e){
        list.add(e);
        if(first){
            first=false;
            name=e.getActivityName();
            HashSet<String> attributes = new HashSet<>();
            attributes.addAll(e.getEventAttributesMap().keySet());
            attributesKeySet.put(name,attributes);
        }else
        if(!e.getActivityName().equals(name)) {
            System.err.println("ERROR IN EventAttributesList Class, method add: " + e.getActivityName() + "  new: " + e.getActivityName() + ", old: " + name + " rpstEdge:" + e.getRPSTEdge() + "  att:" + e.getEventAttributesMap().keySet());
            bond=true;
            if(attributesKeySet.get(e.getActivityName())==null){
                HashSet<String> attributes = new HashSet<>();
                attributes.addAll(e.getEventAttributesMap().keySet());
                attributesKeySet.put(e.getActivityName(),attributes);
            }

        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Iterator<EventAttributes> iterator() {
        return list.iterator();
    }

    public EventAttributes getFirstEventAttributes(){
        return (list!=null)?list.getFirst():null;
    }

    public SubPolygon getPolygon(){
        EventAttributes ea=getFirstEventAttributes();
        if(ea==null||ea.getSubPolygon()==null)
            return null;
        return ea.getSubPolygon();
    }

    public LinkedList<EventAttributes> getList() {
        return list;
    }

    public void setList(LinkedList<EventAttributes> list) {
        this.list = list;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }


    public boolean isBond() {
        return bond;
    }

    public void setBond(boolean bond) {
        this.bond = bond;
    }


    public HashMap<String, HashSet<String>> getAttributesKeySet() {
        return attributesKeySet;
    }

    public void setAttributesKeySet(HashMap<String, HashSet<String>> attributesKeySet) {
        this.attributesKeySet = attributesKeySet;
    }
}
