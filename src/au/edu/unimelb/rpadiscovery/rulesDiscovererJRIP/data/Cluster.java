package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    String label;
    List<String> rules;
    List<FeatureVector> elements;
    String clusterType;

    public Cluster(String label, List<String> rules, List<FeatureVector> elements, String type){
        this.label = label;
        this.rules = new ArrayList<>(rules);
        this.elements = new ArrayList<>(elements);
        this.clusterType = type;
    }

    Cluster(String label){
        this.label = label;
        this.rules = new ArrayList<>();
        this.elements = new ArrayList<>();
        this.clusterType = null;
    }

    public Cluster(){
        this.label = null;
        this.rules = new ArrayList<>();
        this.elements = new ArrayList<>();
        this.clusterType = null;
    }

    private static Cluster[] InitializeClusters(int ClustersCount)
    {
        Cluster[] clusters = new Cluster[ClustersCount];
        for(int i = 0; i < ClustersCount; i++)
        {
            clusters[i] = new Cluster();
        }
        return clusters;
    }

    public String toString(){
        return "label: " + label + ", elements: " + elements;
    }

    public String getLabel(){
        return this.label;
    }

    public void setLabel(String label){
        this.label = label;
    }

    public List<String> getRules(){
        return this.rules;
    }

    public void setRules(List<String> rules){
        this.rules = rules;
    }

    public void giveLabels(){
        for(FeatureVector element: elements)
            element.label = this.label;
    }

    public List<FeatureVector> getElements(){
        return this.elements;
    }

    public void setElements(List<FeatureVector> elements){
        this.elements = elements;
    }
}