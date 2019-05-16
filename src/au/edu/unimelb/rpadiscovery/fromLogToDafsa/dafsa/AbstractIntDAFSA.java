package au.edu.unimelb.rpadiscovery.fromLogToDafsa.dafsa;

//
// Source code recreated attributes a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import gnu.trove.list.array.TLongArrayList;
import name.kazennikov.dafsa.GenericRegister;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public abstract class AbstractIntDAFSA extends IntDaciukAlgoIndexed {
    GenericRegister<State> register = new GenericRegister();
    List<AbstractIntDAFSA.State> states = new ArrayList();
    PriorityQueue<AbstractIntDAFSA.State> free = new PriorityQueue(10, new Comparator<AbstractIntDAFSA.State>() {
        public int compare(AbstractIntDAFSA.State s1, AbstractIntDAFSA.State s2) {
            return s1.number - s2.number;
        }
    });
    int startState;

    public static int decodeLabel(long val) {
        return (int)(val >>> 32);
    }

    public static int decodeDest(long val) {
        return (int)(val & 4294967295L);
    }

    public static long encodeTransition(int input, int next) {
        long k = (long)input;
        k <<= 32;
        k += (long)next;
        return k;
    }

    public void regAdd(int state) {
        if (!((AbstractIntDAFSA.State)this.states.get(state)).registered) {
            ((AbstractIntDAFSA.State)this.states.get(state)).registered = true;
            this.register.add((AbstractIntDAFSA.State)this.states.get(state));
        }
    }

    public int regGet(int state) {
        AbstractIntDAFSA.State s = (AbstractIntDAFSA.State)this.register.get((AbstractIntDAFSA.State)this.states.get(state));
        return s == null ? -1 : s.getNumber();
    }

    public void regRemove(int state) {
        if (((AbstractIntDAFSA.State)this.states.get(state)).registered) {
            ((AbstractIntDAFSA.State)this.states.get(state)).registered = false;
            this.register.remove((AbstractIntDAFSA.State)this.states.get(state));
        }
    }

    public AbstractIntDAFSA() {
        this.initFinals();
        this.startState = this.addState();
    }

    public int getNext(int state, int input) {
        AbstractIntDAFSA.State s = (AbstractIntDAFSA.State)this.states.get(state);
        int next = s.getNext(input);
        return next;
    }

    public boolean isConfluence(int state) {
        return ((AbstractIntDAFSA.State)this.states.get(state)).inbound() > 1;
    }

    public int cloneState(int srcState) {
        AbstractIntDAFSA.State src = (AbstractIntDAFSA.State)this.states.get(srcState);
        int clonedState = this.addState();
        src.assign((AbstractIntDAFSA.State)this.states.get(clonedState));
        ((AbstractIntDAFSA.State)this.states.get(clonedState)).hashCode = src.hashCode;
        ((AbstractIntDAFSA.State)this.states.get(clonedState)).validHashCode = src.validHashCode;
        return clonedState;
    }

    public int addState() {
        if (!this.free.isEmpty()) {
            return ((AbstractIntDAFSA.State)this.free.poll()).getNumber();
        } else {
            AbstractIntDAFSA.State s = new AbstractIntDAFSA.State();
            s.number = this.states.size();
            this.states.add(s);
            this.newFinal(s.number);
            return s.getNumber();
        }
    }

    public boolean setNext(int src, int label, int dest) {
        AbstractIntDAFSA.State s = (AbstractIntDAFSA.State)this.states.get(src);
        s.setNext(label, dest);
        return false;
    }

    public void removeState(int state) {
        AbstractIntDAFSA.State s = (AbstractIntDAFSA.State)this.states.get(state);
        s.reset();
        this.free.add(s);
    }

    public int size() {
        return this.states.size() - this.free.size();
    }

    public int getTransitionCount(int state) {
        return ((AbstractIntDAFSA.State)this.states.get(state)).next.size();
    }

    public int getTransitionInput(int state, int transitionIndex) {
        return decodeLabel(((AbstractIntDAFSA.State)this.states.get(state)).next.get(transitionIndex));
    }

    public int getTransitionNext(int state, int transitionIndex) {
        return decodeDest(((AbstractIntDAFSA.State)this.states.get(state)).next.get(transitionIndex));
    }

    public int getStartState() {
        return this.startState;
    }

    public abstract void initFinals();

    public abstract int finalHash(int var1);

    public abstract boolean finalEquals(int var1, int var2);

    public abstract void finalReset(int var1);

    public abstract void finalAssign(int var1, int var2);

    public abstract void newFinal(int var1);

    public abstract boolean isFinalState(int var1);

    public void toDot(PrintWriter pw) throws IOException {
        pw.println("digraph fsm {");
        pw.println("rankdir=LR;");
        pw.println("node [shape=circle,style=filled, fillcolor=white]");
        Iterator var3 = this.states.iterator();

        while(var3.hasNext()) {
            AbstractIntDAFSA.State n = (AbstractIntDAFSA.State)var3.next();
            if (n.getNumber() == this.startState) {
                pw.printf("%d [fillcolor=\"gray\"];\n", n.getNumber());
            }

            for(int i = 0; i < n.outbound(); ++i) {
                pw.printf("%d -> %d [label=\"%s\"];\n", n.number, decodeDest(n.next.get(i)), "" + decodeLabel(n.next.get(i)));
            }

            if (this.isFinalState(n.getNumber())) {
                pw.printf("%d [shape=doublecircle];\n", n.number);
            }
        }

        pw.println("}");
    }

    public void toDot(String fileName) throws IOException {
        PrintWriter pw = new PrintWriter(fileName);
        this.toDot(pw);
        pw.close();
    }

    public int transitionCount() {
        int count = 0;

        AbstractIntDAFSA.State s;
        for(Iterator var3 = this.states.iterator(); var3.hasNext(); count += s.next.size()) {
            s = (AbstractIntDAFSA.State)var3.next();
        }

        return count;
    }

    public List<AbstractIntDAFSA.State> getStates() {
        return this.states;
    }

    public boolean isSource(int stateNumber) {
        return stateNumber == this.startState;
    }

    public class State {
        public TLongArrayList next = new TLongArrayList();
        int inbound = 0;
        int number;
        int hashCode = 1;
        boolean registered;
        boolean validHashCode = true;

        public State() {
        }

        public void setNumber(int num) {
            this.number = num;
        }

        public int getNumber() {
            return this.number;
        }

        int findIndex(int input) {
            for(int i = 0; i != this.next.size(); ++i) {
                if (AbstractIntDAFSA.decodeLabel(this.next.get(i)) == input) {
                    return i;
                }
            }

            return -1;
        }

        public int getNext(int input) {
            int index = this.findIndex(input);
            return index == -1 ? -1 : AbstractIntDAFSA.decodeDest(this.next.get(index));
        }

        public void setNext(int input, int next) {
            int index = this.findIndex(input);
            AbstractIntDAFSA.State s;
            if (index != -1) {
                s = (AbstractIntDAFSA.State)AbstractIntDAFSA.this.states.get(AbstractIntDAFSA.decodeDest(this.next.get(index)));
                s.removeInbound(input, this);
            }

            if (next != -1) {
                if (index == -1) {
                    this.next.add(AbstractIntDAFSA.encodeTransition(input, next));
                } else {
                    this.next.set(index, AbstractIntDAFSA.encodeTransition(input, next));
                }

                s = (AbstractIntDAFSA.State)AbstractIntDAFSA.this.states.get(next);
                s.addInbound(input, this);
            } else if (index != -1) {
                this.next.removeAt(index);
            }

            this.validHashCode = false;
        }

        public int outbound() {
            return this.next.size();
        }

        public int inbound() {
            return this.inbound;
        }

        public void removeInbound(int input, AbstractIntDAFSA.State node) {
            --this.inbound;
        }

        public void addInbound(int input, AbstractIntDAFSA.State node) {
            ++this.inbound;
        }

        int hc() {
            int result = AbstractIntDAFSA.this.finalHash(this.number);

            for(int i = 0; i != this.next.size(); ++i) {
                result += AbstractIntDAFSA.decodeLabel(this.next.get(i));
                result += AbstractIntDAFSA.decodeDest(this.next.get(i));
            }

            return result;
        }

        public int hashCode() {
            if (!this.validHashCode) {
                this.hashCode = this.hc();
                this.validHashCode = true;
            }

            return this.hashCode;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (!(obj instanceof AbstractIntDAFSA.State)) {
                return false;
            } else {
                AbstractIntDAFSA.State other = (AbstractIntDAFSA.State)obj;
                if (!AbstractIntDAFSA.this.finalEquals(this.number, other.number)) {
                    return false;
                } else if (this.next.size() != other.next.size()) {
                    return false;
                } else {
                    for(int i = 0; i != this.outbound(); ++i) {
                        int otherIndex = other.findIndex(AbstractIntDAFSA.decodeLabel(this.next.get(i)));
                        if (otherIndex == -1) {
                            return false;
                        }

                        if (AbstractIntDAFSA.decodeDest(this.next.get(i)) != AbstractIntDAFSA.decodeDest(other.next.get(otherIndex))) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }

        public void reset() {
            AbstractIntDAFSA.this.finalReset(this.number);

            for(int i = 0; i != this.outbound(); ++i) {
                int input = AbstractIntDAFSA.decodeLabel(this.next.get(i));
                AbstractIntDAFSA.State next = (AbstractIntDAFSA.State)AbstractIntDAFSA.this.states.get(AbstractIntDAFSA.decodeDest(this.next.get(i)));
                next.removeInbound(input, this);
            }

            this.next.clear();
        }

        public AbstractIntDAFSA.State assign(AbstractIntDAFSA.State node) {
            AbstractIntDAFSA.this.finalAssign(node.getNumber(), this.number);

            for(int i = 0; i != this.next.size(); ++i) {
                node.setNext(AbstractIntDAFSA.decodeLabel(this.next.get(i)), AbstractIntDAFSA.decodeDest(this.next.get(i)));
            }

            return node;
        }

        public String toString() {
            return String.format("state=%d", this.number);
        }
    }
}
