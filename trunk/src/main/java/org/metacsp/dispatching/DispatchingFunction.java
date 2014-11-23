package org.metacsp.dispatching;

import org.metacsp.framework.ConstraintNetwork;
import org.metacsp.multi.activity.SymbolicVariableActivity;
import org.metacsp.sensing.ConstraintNetworkAnimator;

public abstract class DispatchingFunction {
	
	protected String component;
	
	protected Dispatcher dis;
	
	public DispatchingFunction(String component) {
		this.component = component;
	}
	
	public void registerDispatcher(Dispatcher dis) {
		this.dis = dis;
	}
	
	public String getComponent() { return component; }
	
	public abstract void dispatch(SymbolicVariableActivity act);
	
	public void finish(SymbolicVariableActivity act) {
		dis.finish(act);
	}
	
	public ConstraintNetwork getConstraintNetwork() {
		return dis.getConstraintNetwork();
	}
	

}
