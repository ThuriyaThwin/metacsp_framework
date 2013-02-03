package multi.activity;

import edu.uci.ics.jung.graph.ObservableGraph;
import framework.Constraint;
import framework.ConstraintNetwork;
import framework.ConstraintSolver;
import framework.Variable;

public class ActivityNetwork extends ConstraintNetwork {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1628887638592525043L;

	public ActivityNetwork(ConstraintSolver sol) {
		super(sol);
		// TODO Auto-generated constructor stub
	}
		
	public long getOrigin() {
		return ((ActivityNetworkSolver)this.solver).getOrigin();
	}
	
	public long getHorizon() {
		return ((ActivityNetworkSolver)this.solver).getHorizon();
	}
	
	public ActivityNetwork clone() {
		ActivityNetwork c = new ActivityNetwork(super.solver);
		
		for ( Variable v : super.g.getVertices() ) {
			c.g.addVertex(v);
		}
		for ( Constraint e : super.g.getEdges() ) {
			c.g.addEdge(e, g.getEndpoints(e));
		}
		return c;
	}
}