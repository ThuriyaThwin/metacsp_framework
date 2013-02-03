package framework.multi;

import java.util.Arrays;
import java.util.logging.Logger;

import multi.allenInterval.AllenIntervalConstraint;
import utility.logging.MetaCSPLogging;
import framework.Constraint;
import framework.ConstraintSolver;
import framework.Variable;

/**
 * A {@link MultiConstraint} is a constraint among {@link Variable}s that could be {@link MultiVariable}s.
 * Every {@link MultiConstraint} is "implemented" one or more lower-level
 * constraints - see, e.g., the {@link AllenIntervalConstraint}.
 *  
 * @author Federico Pecora
 *
 */
public abstract class MultiConstraint extends Constraint {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2743945338930729256L;

	protected Constraint[] constraints;

	private transient Logger logger = MetaCSPLogging.getLogger(this.getClass());
	
	private boolean propagateImmediately = true;

	/**
	 * Instantiates a {@link MultiConstraint}.  This constructor must be called by the
	 * constructor of the implementing class.
	 */
	public MultiConstraint() {
		this.constraints = null;
	}

	/**
	 * This method must be implemented by the implementing class, and should instantiate
	 * the internal constraints underlying this {@link MultiConstraint}. 
	 * @param variables The {@link Variable}s that are in the scope of this {@link MultiConstraint}.
	 * @return An array of lower-level constraints which "implement" this {@link MultiConstraint}.
	 */
	protected abstract Constraint[] createInternalConstraints(Variable[] variables);

	/**
	 * Get the lower-level constraints underlying of this {@link MultiConstraint}.
	 * @return An array of lower-level constraints underlying of this {@link MultiConstraint}.
	 */
	public Constraint[] getInternalConstraints() {
		if (constraints == null) constraints = this.createInternalConstraints(this.scope);
		logger.finest("Created internal constraints for " + this + ": " + Arrays.toString(constraints));
		return constraints;
	}
	
	/**
	 * A {@link MultiConstraint} must be cloneable.  This is used by the {@link MultiConstraintSolver}
	 * class to instantiate proper constraints to delegate its underlying {@link ConstraintSolver}s.
	 */
	public abstract Object clone();
	
	/**
	 * Delays the propagation of this {@link MultiConstraint}.
	 */
	public void setPropagateLater() {
		propagateImmediately = false;
	}

	/**
	 * Schedules this {@link MultiConstraint} for immediate propagation.
	 */
	public void setPropagateImmediately() {
		propagateImmediately = true;
	}

	/**
	 * A {@link MultiConstraint} can be scheduled for propagation immediately (as soon as it is added)
	 * or later.  This is used internally by the {@link ConstraintSolver} to delay propagation
	 * of a {@link MultiConstraint} in cases where the underlying constraints should be propagated first.
	 * By default, {@link MultiConstraint}s are propagated immediately.
	 * @return <code>true</code> iff this constraint should be propagated as soon as possible.
	 */
	public boolean propagateImmediately() {
		return propagateImmediately;
	}
	
	@Override
	public String getDescription() {
		String ret = this.getClass().getSimpleName() + ": [";
		if (this.getInternalConstraints() != null) {
			for (int i = 0; i < this.getInternalConstraints().length; i++) {
				ret += this.getInternalConstraints()[i].getClass().getSimpleName();
				if (i != this.getInternalConstraints().length-1) ret += ",";
			}
		}
		return ret + "]";
	}
}