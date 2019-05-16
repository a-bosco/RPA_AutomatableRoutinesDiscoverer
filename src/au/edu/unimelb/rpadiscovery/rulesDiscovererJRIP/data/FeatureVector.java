package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data;

import java.util.HashMap;
import java.util.Objects;

public class FeatureVector {
    public HashMap<String, String> attributes;
    public String label;

    public FeatureVector(Event attributes){
        this.attributes = attributes.payload;
        this.label = null;
    }

    public FeatureVector(HashMap<String, String> attributes){
        this.attributes = new HashMap<>();
        for(String attribute: attributes.keySet())
            this.attributes.put(attribute, attributes.get(attribute));
    }

    public FeatureVector(HashMap<String, String> attributes, String label){
        this.attributes = new HashMap<>();
        for(String attribute: attributes.keySet())
            this.attributes.put(attribute, attributes.get(attribute));
        this.label=label;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString(){
        return attributes + " => "+ " label = " + label;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FeatureVector)) {
            return false;
        }
        FeatureVector fv = (FeatureVector) o;
        return fv.attributes.equals(this.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }
}
