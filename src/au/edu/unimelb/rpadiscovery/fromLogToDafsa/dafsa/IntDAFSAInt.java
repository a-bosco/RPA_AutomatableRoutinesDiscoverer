package au.edu.unimelb.rpadiscovery.fromLogToDafsa.dafsa;

//
// Source code recreated attributes a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import com.google.common.base.Objects;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;

import name.kazennikov.fsa.IntFSAObjectEventHandler;

public class IntDAFSAInt extends AbstractIntDAFSA {
    ArrayList<TIntHashSet> finals;
    int finalValue;

    public IntDAFSAInt() {
    }

    public int finalHash(int state) {
        return ((TIntHashSet)this.finals.get(state)).hashCode();
    }

    public boolean finalEquals(int state1, int state2) {
        return Objects.equal(this.finals.get(state1), this.finals.get(state2));
    }

    public void finalReset(int state) {
        ((TIntHashSet)this.finals.get(state)).clear();
        ((State)this.states.get(state)).validHashCode = false;
    }

    public void finalAssign(int destState, int srcState) {
        TIntHashSet dest = (TIntHashSet)this.finals.get(destState);
        TIntHashSet src = (TIntHashSet)this.finals.get(srcState);
        dest.clear();
        dest.addAll(src);
    }

    public void initFinals() {
        this.finals = new ArrayList();
    }

    public void newFinal(int state) {
        this.finals.add(new TIntHashSet(3));
    }

    public void setFinalValue(int finalValue) {
        this.finalValue = finalValue;
    }

    public boolean setFinal(int state) {
        boolean b = ((TIntHashSet)this.finals.get(state)).add(this.finalValue);
        ((State)this.states.get(state)).validHashCode = false;
        return b;
    }

    public boolean hasFinal(int state) {
        return ((TIntHashSet)this.finals.get(state)).contains(this.finalValue);
    }

    public TIntHashSet getFinals(int state) {
        return (TIntHashSet)this.finals.get(state);
    }

    public boolean isFinalState(int state) {
        return !((TIntHashSet)this.finals.get(state)).isEmpty();
    }

    public void emit(IntFSAObjectEventHandler<int[]> events) {
        for(int i = 0; i < this.states.size(); ++i) {
            State s = (State)this.states.get(i);
            events.startState(i);
            events.setFinalValue(((TIntHashSet)this.finals.get(i)).toArray());
            events.setFinal();

            for(int j = 0; j < s.next.size(); ++j) {
                int input = decodeLabel(s.next.get(j));
                int dest = decodeDest(s.next.get(j));
                events.addTransition(input, dest);
            }

            events.endState();
        }

    }
}

