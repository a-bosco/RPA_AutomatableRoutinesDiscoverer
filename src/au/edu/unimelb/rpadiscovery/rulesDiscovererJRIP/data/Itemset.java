package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

import java.util.ArrayList;
import java.util.List;

public class Itemset {
    public List<String> items;
    private Integer frequency;

    public Itemset(List<String> items, Integer frequency){
        this.items = new ArrayList<>(items);
        this.frequency = frequency;
    }

    public boolean isFrequent(Integer threshold){
        return frequency >= threshold;
    }

    public void increaseFrequency(){
        this.frequency += 1;
    }

    public String toString(){
        return "(" + items + "; frequency = " + frequency + ")";
    }
}
