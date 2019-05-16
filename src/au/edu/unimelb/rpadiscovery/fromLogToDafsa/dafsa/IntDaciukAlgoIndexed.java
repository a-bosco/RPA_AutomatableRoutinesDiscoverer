package au.edu.unimelb.rpadiscovery.fromLogToDafsa.dafsa;



import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public abstract class IntDaciukAlgoIndexed {
    protected int startState;

    public IntDaciukAlgoIndexed() {
    }

    public abstract int getNext(int var1, int var2);

    public abstract boolean isConfluence(int var1);

    public abstract int cloneState(int var1);

    public abstract int addState();

    public abstract boolean setNext(int var1, int var2, int var3);

    public abstract void removeState(int var1);

    public abstract boolean setFinal(int var1);

    public abstract boolean hasFinal(int var1);

    public abstract void regAdd(int var1);

    public abstract int regGet(int var1);

    public abstract void regRemove(int var1);

    protected IntArrayList addSuffix(IntArrayList states, int s, IntArrayList seq, int start, int end) {
        int current = s;
        if (end > start) {
            this.regRemove(s);
        }

        for(int i = start; i < end; ++i) {
            int in = seq.get(i);
            int state = this.addState();
            if (states != null) {
                states.add(state);
            }

            this.setNext(current, in, state);
            current = state;
        }

        if (start == end && !this.hasFinal(current)) {
            this.regRemove(current);
        }

        this.setFinal(current);
        //System.out.println("states suffix: "+states);
        return states;
    }

    IntArrayList commonPrefix(IntArrayList seq) {
        int current = this.startState;
        IntArrayList prefix = new IntArrayList(seq.size() + 1);
        prefix.add(current);

        for(int i = 0; i != seq.size(); ++i) {
            int in = seq.get(i);
            int next = this.getNext(current, in);
            if (next == -1) {
                break;
            }

            current = next;
            prefix.add(next);
        }

        //System.out.println("common prefix: "+prefix);
        return prefix;
    }

    int findConfluence(IntArrayList states) {
        for(int i = 0; i != states.size(); ++i) {
            if (this.isConfluence(states.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public void addMinWord(IntArrayList seq) {
        IntArrayList stateList = this.commonPrefix(seq);
        //System.out.println("COMMON PREFIX LIST: "+stateList);
        int confIdx = this.findConfluence(stateList);
        //System.out.println("confIdx: "+confIdx);
        int stopIdx = confIdx == -1 ? stateList.size() : confIdx;
        //System.out.println("stopIdx: "+stopIdx);
        if (confIdx > -1) {
            int idx = confIdx;
            this.regRemove(stateList.get(confIdx - 1));

            while(idx < stateList.size()) {
                int prev = stateList.get(idx - 1);
                int cloned = this.cloneState(stateList.get(idx));
                stateList.set(idx, cloned);
                this.setNext(prev, seq.get(confIdx - 1), cloned);
                ++idx;
                ++confIdx;
            }
        }
       // System.out.println("STATELIST 2!! : "+stateList);
        //System.out.println("SEQ 2!! : "+seq);
        this.addSuffix(stateList, stateList.get(stateList.size() - 1), seq, stateList.size() - 1, seq.size());
        this.replaceOrRegister(seq, stateList, stopIdx);
        //System.out.println("STATELIST!! : "+stateList);
    }

    protected void replaceOrRegister(IntArrayList input, IntArrayList stateList, int stop) {
        if (stateList.size() >= 2) {
            int stateIdx = stateList.size() - 1;

            for(int inputIdx = input.size() - 1; stateIdx > 0; --stateIdx) {
                int n = stateList.get(stateIdx);
                int regNode = this.regGet(n);
                if (regNode == n) {
                    if (stateIdx < stop) {
                        return;
                    }
                } else if (regNode == -1) {
                    this.regAdd(n);
                } else {
                    int in = input.get(inputIdx);
                    this.regRemove(stateList.get(stateIdx - 1));
                    this.setNext(stateList.get(stateIdx - 1), in, regNode);
                    stateList.set(stateIdx, regNode);
                    this.removeState(n);
                }

                --inputIdx;
            }

        }
    }

    public void add(IntArrayList seq) {
        int current = this.startState;

        int idx;
        int s;
        for(idx = 0; idx < seq.size(); current = s) {
            s = this.getNext(current, seq.get(idx));
            if (s == -1) {
                break;
            }

            ++idx;
        }

        this.addSuffix((IntArrayList)null, current, seq, idx, seq.size());
    }
}
