package au.edu.unimelb.rpadiscovery.fromLogToDafsa.automaton;

import java.util.Comparator;

public class TransitionComparator implements Comparator<Transition>{

	@Override
	public int compare(Transition t1, Transition t2) {
		// TODO Auto-generated method stub
		return t1.eventID() - t2.eventID();
	}

}
