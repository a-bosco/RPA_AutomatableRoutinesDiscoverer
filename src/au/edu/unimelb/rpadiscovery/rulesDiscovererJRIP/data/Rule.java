package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

import java.util.LinkedList;

public class Rule {
    String rule;
    String antecedent;
    String label;
    double totalNumberOfInstances;
    double missingClassification;
    Double support;
    Double confidence;
    Double ruleLength;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule1 = (Rule) o;

        if (Double.compare(rule1.totalNumberOfInstances, totalNumberOfInstances) != 0) return false;
        if (Double.compare(rule1.missingClassification, missingClassification) != 0) return false;
        if (rule != null ? !rule.equals(rule1.rule) : rule1.rule != null) return false;
        if (!antecedent.equals(rule1.antecedent)) return false;
        if (!label.equals(rule1.label)) return false;
        if (support != null ? !support.equals(rule1.support) : rule1.support != null) return false;
        return confidence != null ? confidence.equals(rule1.confidence) : rule1.confidence == null;
    }



    public Rule(String rule, Double support, Double confidence, Double ruleLength){
        this.rule = rule.replace(" => Class=", " => ").replaceAll("\\s\\([\\d\\.]+/[\\d\\.]+\\)", "");
        this.support = support;
        this.confidence = confidence;
        this.ruleLength = ruleLength;
    }

    public Rule(String antecedent, String className, double firstValue, double secondValue){
        this.antecedent=antecedent;
        this.label=className;
        this.totalNumberOfInstances=firstValue;
        this.missingClassification=secondValue;
    }

    public String toString(){
        return this.rule + "   (sup = " + String.format("%.2f", this.support) + ", conf = " + String.format("%.2f", this.confidence) + ", ruleLength = " + String.format("%.2f", this.ruleLength) + ")";
    }

    public Double getSupport(){
        return this.support;
    }

    public Double getConfidence(){
        return this.confidence;
    }

    public Double getRuleLength(){
        return this.ruleLength;
    }

    public String getAntecedent() {
        return antecedent;
    }

    public void setAntecedent(String antecedent) {
        this.antecedent = antecedent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getTotalNumberOfInstances() {
        return totalNumberOfInstances;
    }

    public void setTotalNumberOfInstances(double totalNumberOfInstances) {
        this.totalNumberOfInstances = totalNumberOfInstances;
    }

    public double getMissingClassification() {
        return missingClassification;
    }

    public void setMissingClassification(double missingClassification) {
        this.missingClassification = missingClassification;
    }

    public LinkedList<String> getRpstAttributeList(){
        //System.out.println(antecedent);
        LinkedList<String> list = new LinkedList<>();
        int start=0;
        int lastIndex=antecedent.lastIndexOf('[');
        while(true){
            start=antecedent.indexOf('[',start);
            //System.out.println(start);
            char sign='=';
            int indexSign=antecedent.indexOf('=', start);
            if(indexSign<0){
                indexSign=antecedent.indexOf('>', start);
                sign='>';
            }
            if(indexSign<0){
                indexSign=antecedent.indexOf('<', start);
                sign='<';
            }
            String value=null;
            if(antecedent.charAt(antecedent.indexOf(sign, start)-1)!=' '){
                value = antecedent.substring(start, antecedent.indexOf(sign, start) - 2);
            }else {
                value = antecedent.substring(start, antecedent.indexOf(sign, start) - 1);
            }
            if(!list.contains(value)) {
                list.add(value);
            }
            if(start==lastIndex)
                break;
            start++;
        }
        return list;
    }
}
