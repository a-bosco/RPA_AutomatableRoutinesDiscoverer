package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

import java.util.HashMap;

public class RangesSummary {
    HashMap<String, HashMap<String, Double>> attrMax;
    HashMap<String, HashMap<String, Double>> attrMin;

    public RangesSummary(){
        this.attrMax = new HashMap<>();
        this.attrMin = new HashMap<>();
    }

    public HashMap<String, HashMap<String, Double>> getAttrMax(){
        return this.attrMax;
    }

    public void setAttrMax(HashMap<String, HashMap<String, Double>> attrMax){
        this.attrMax = attrMax;
    }

    public HashMap<String, HashMap<String, Double>> getAttrMin(){
        return this.attrMin;
    }

    public void setAttrMin(HashMap<String, HashMap<String, Double>> attrMin){
        this.attrMin = attrMin;
    }
}