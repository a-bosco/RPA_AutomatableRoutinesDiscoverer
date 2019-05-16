package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

public class DeclareConstraint {
    public String ruleType;
    public String activation;
    public String target;

    public DeclareConstraint(String ruleType, String activation, String target){
        this.ruleType = ruleType;
        this.activation = activation;
        this.target = target;
    }

    public DeclareConstraint(String rule){
        this.ruleType = rule.substring(0, rule.indexOf("("));
        String[] activities = rule.substring(rule.indexOf("(") + 1, rule.lastIndexOf(")")).split(",");
        if(this.ruleType.equals("precedence") || this.ruleType.equals("chain precedence") || this.ruleType.equals("alternate precedence")){
            this.activation = activities[1];
            this.target = activities[0];
        }
        else{
            this.activation = activities[0];
            this.target = activities[1];
        }
    }

    public String toString() {
        if(this.ruleType.contains("precedence"))
            return this.ruleType + "(" + this.target + ", " + this.activation + ")";
        else
            return this.ruleType + "(" + this.activation + ", " + this.target + ")";
    }
}