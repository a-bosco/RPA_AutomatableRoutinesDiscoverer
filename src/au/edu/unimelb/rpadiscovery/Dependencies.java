package au.edu.unimelb.rpadiscovery;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.LinkedList;

public class Dependencies {
    private IntArrayList trace;
    private LinkedList<Dependency> fromToDependenciesList; //for each eventAttributes, which attributes are not independent


    public Dependencies(IntArrayList trace) {
        this.trace = trace;
        fromToDependenciesList=new LinkedList<>();
    }

   /* public void addDependency(EventAttributes start, EventAttributes to, LinkedList<String> startAttributes, LinkedList<String> toAttributes) {
        Dependency fromToDependency = new Dependency(start, to, startAttributes, toAttributes);
    }*/

    public IntArrayList getTrace() {
        return trace;
    }

    public void setTrace(IntArrayList trace) {
        this.trace = trace;
    }

    public LinkedList<Dependency> getFromToDependenciesList() {
        return fromToDependenciesList;
    }

    public void setFromToDependenciesList(LinkedList<Dependency> fromToDependenciesList) {
        this.fromToDependenciesList = fromToDependenciesList;
    }

    public void addDependency(Dependency dependency) {
        fromToDependenciesList.add(dependency);
    }
    public int size(){
        return fromToDependenciesList.size();
    }
}
