package au.edu.unimelb.rpadiscovery;

import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data.Rule;

public class Dependency { //TODO:  change this name!
    /*private String fromPolygon;*/
    private String fromRPSTEdge;
    private String toRPSTEdge;
    private String fromAttributes;
    private String toAttributes;
    private Rule rule;
    private boolean positionalDependency = false;

    private int type;
    public static final int TYPE_1 = 1; //constant
    public static final int TYPE_2 = 2; //transformation
    public static final int TYPE_3 = 3;


    public Dependency(String fromRPSTEdge, String toRPSTEdge, String startAttributes, String toAttributes) {
        this.fromRPSTEdge = fromRPSTEdge;
        this.toRPSTEdge = toRPSTEdge;
        this.fromAttributes = startAttributes;
        this.toAttributes = toAttributes;
    }

    public Dependency(String fromRPSTEdge, String toRPSTEdge) {
        this.fromRPSTEdge = fromRPSTEdge;
        this.toRPSTEdge = toRPSTEdge;

    }

    public Dependency() {

    }

    public void setDependencyAttribute(String fromAttribute, String toAttribute) {
        fromAttributes = fromAttribute;
        toAttributes = toAttribute;
    }
   /* public String getFromPolygon() {
        return fromPolygon;
    }

    public void setFromPolygon(String fromPolygon) {
        this.fromPolygon = fromPolygon;
    }*/

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFromRPSTEdge() {
        return fromRPSTEdge;
    }

    public void setFromRPSTEdge(String fromRPSTEdge) {
        this.fromRPSTEdge = fromRPSTEdge;
    }

    public String getToRPSTEdge() {
        return toRPSTEdge;
    }

    public void setToRPSTEdge(String toRPSTEdge) {
        this.toRPSTEdge = toRPSTEdge;
    }

    public String getFromAttributes() {
        return fromAttributes;
    }

    public void setFromAttributes(String fromAttributes) {
        this.fromAttributes = fromAttributes;
    }

    public String getToAttributes() {
        return toAttributes;
    }

    public void setToAttributes(String toAttributes) {
        this.toAttributes = toAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dependency that = (Dependency) o;

        if (type != that.type) {
            //System.out.println("C 1");
            return false;
        }
        if (!fromRPSTEdge.equals(that.fromRPSTEdge)) {
            //System.out.println("C 2");
            return false;
        }
        if (!toRPSTEdge.equals(that.toRPSTEdge)) {
            //System.out.println("C 3");
            return false;
        }
        if (!fromAttributes.equals(that.fromAttributes)) {
            //System.out.println("C 4 ");
            return false;
        }
        if (rule == null && that.rule != null) {
            //System.out.println("C 5");
            return false;
        }
        if (rule != null && !rule.equals(that.rule)) {
            //System.out.println("C 6");
            return false;
        }
        if (positionalDependency != that.positionalDependency) {
            //System.out.println("C 7");
            return false;
        }

        //System.out.println("C 8");

        return toAttributes.equals(that.toAttributes);
    }

    @Override
    public int hashCode() {
        int result = fromRPSTEdge.hashCode();
        result = 31 * result + toRPSTEdge.hashCode();
        result = 31 * result + fromAttributes.hashCode();
        result = 31 * result + toAttributes.hashCode();
        result = 31 * result + type;
        return result;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /* private boolean equalsList(LinkedList<String> list1, LinkedList<String> list2) {
        if(list1==list2)
            return true;
        if(list1.size()!=list2.size())
            return false;
        for(String s: list1){
            int contList1=0;
            int contList2=0;
            for(String s1: list1){
                if(s1.equals(s))
                    contList1++;
            }
            for(String s2:list2){
                if(s2.equals(s))
                    contList2++;
            }
            if(contList1!=contList2)
                return false;
        }
        return true;
    }*/

    public boolean isPositionalDependency() {
        return positionalDependency;
    }

    public void setPositionalDependency(boolean positionalDependency) {
        this.positionalDependency = positionalDependency;
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "fromRPSTEdge='" + fromRPSTEdge + '\'' +
                ", toRPSTEdge='" + toRPSTEdge + '\'' +
                ", fromAttributes='" + fromAttributes + '\'' +
                ", toAttributes='" + toAttributes + '\'' +
                '}'+"\n";
    }
}