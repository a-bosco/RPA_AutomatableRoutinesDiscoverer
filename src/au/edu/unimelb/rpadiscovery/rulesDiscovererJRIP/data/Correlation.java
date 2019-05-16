package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

import java.util.HashMap;
import java.util.List;

public class Correlation {
    public String from;
    public String to;
    public HashMap<String, List<String>> antecedent;
    public HashMap<String, List<String>> consequent;
    public Double support;
    public Double confidence;

    public Correlation(DeclareConstraint constraint, HashMap<String, List<String>> antecedent, HashMap<String, List<String>> consequent){
        this.from = constraint.activation;
        this.to = constraint.target;
        this.antecedent = new HashMap<>(antecedent);
        this.consequent = new HashMap<>(consequent);
        this.support = 0.0;
        this.confidence = 0.0;
    }

    public String toString(){
        return from + ": " + antecedent + " => " + to + ": " + consequent;
    }
}
