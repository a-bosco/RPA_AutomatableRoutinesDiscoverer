package org.jbpt.pm.epc;

import org.jbpt.pm.DataNode;

/**
 * EPC document implementation
 * 
 * @author Artem Polyvyanyy, Cindy Fhnrich, Tobias Hoppe
 */
public class Document extends DataNode implements IDocument {

	public Document() {
		super();
	}

	public Document(String name, String desc) {
		super(name, desc);
	}

	public Document(String name) {
		super(name);
	}
}