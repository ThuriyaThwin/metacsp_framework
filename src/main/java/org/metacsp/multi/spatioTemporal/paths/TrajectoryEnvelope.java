package org.metacsp.multi.spatioTemporal.paths;

import java.util.ArrayList;

import org.metacsp.framework.Constraint;
import org.metacsp.framework.ConstraintSolver;
import org.metacsp.framework.Domain;
import org.metacsp.framework.Variable;
import org.metacsp.framework.multi.MultiVariable;
import org.metacsp.multi.activity.Activity;
import org.metacsp.multi.allenInterval.AllenInterval;
import org.metacsp.multi.allenInterval.AllenIntervalConstraint;
import org.metacsp.multi.spatial.DE9IM.DE9IMRelation;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeDomain;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeVariable;
import org.metacsp.multi.spatial.DE9IM.LineStringDomain;
import org.metacsp.multi.spatial.DE9IM.PolygonalDomain;
import org.metacsp.time.APSPSolver;
import org.metacsp.time.Bounds;

import com.vividsolutions.jts.JTSVersion;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;

/**
 * A {@link TrajectoryEnvelope} is a {@link MultiVariable} composed of an {@link AllenInterval} (temporal part)
 * and a {@link GeometricShapeVariable} (spatial part). Constraints of type {@link AllenIntervalConstraint} and
 * {@link DE9IMRelation} can be added to {@link TrajectoryEnvelope}s.
 * 
 * @author Federico Pecora
 */
public class TrajectoryEnvelope extends MultiVariable implements Activity {

	private static final long serialVersionUID = 183736569434737103L;
	private double width = 1.3;
	private double length= 3.5;
	private double deltaW = 0.0;
	private double deltaL = 0.0;
	private Trajectory trajectory = null;
	
	public TrajectoryEnvelope(ConstraintSolver cs, int id, ConstraintSolver[] internalSolvers, Variable[] internalVars) {
		super(cs, id, internalSolvers, internalVars);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public int compareTo(Variable o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected Constraint[] createInternalConstraints(Variable[] variables) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setProfile(double width, double length, double deltaW, double deltaL) {
		this.width = width;
		this.length = length;
		this.deltaW = deltaW;
		this.deltaL = deltaL;
	}
	
//	private Coordinate[] createEnvelope() {
//		ArrayList<Coordinate> ret = new ArrayList<Coordinate>();
//		for (PoseSteering ps : this.path.getPoseSteering()) {
//			GeometricShapeFactory gsf = new GeometricShapeFactory();
//			gsf.setHeight(width);
//			gsf.setWidth(length);
//			gsf.setCentre(new Coordinate(ps.getX(),ps.getY()));
//			gsf.setRotation(ps.getTheta());
//			Polygon rect = gsf.createRectangle();
//			for (Coordinate coor : rect.getCoordinates()) ret.add(coor);
//		}
//		return ret.toArray(new Coordinate[ret.size()]);
//	}

//	private Coordinate[] createEnvelope() {
//		ArrayList<Polygon> ret = new ArrayList<Polygon>();
//		for (PoseSteering ps : this.path.getPoseSteering()) {
//			GeometricShapeFactory gsf = new GeometricShapeFactory();
//			gsf.setHeight(width);
//			gsf.setWidth(length);
//			gsf.setCentre(new Coordinate(ps.getX(),ps.getY()));
//			gsf.setRotation(ps.getTheta());
//			Polygon rect = gsf.createRectangle();
//			ret.add(rect);
//		}
//		GeometryFactory gf = new GeometryFactory();
//		MultiPolygon combined = new MultiPolygon(ret.toArray(new Polygon[ret.size()]), gf);
//		return combined.getCoordinates();
//	}


//	private Coordinate[] createEnvelope() {
//		Geometry onePoly = null;
//		for (PoseSteering ps : this.path.getPoseSteering()) {
//			GeometricShapeFactory gsf = new GeometricShapeFactory();
//			gsf.setHeight(width);
//			gsf.setWidth(length);
//			gsf.setCentre(new Coordinate(ps.getX(),ps.getY()));
//			gsf.setRotation(ps.getTheta());
//			Polygon rect = gsf.createRectangle();
//			if (onePoly == null) onePoly = rect;
//			else onePoly = onePoly.union(rect);
//		}
//		return onePoly.getCoordinates();
//	}

	private Coordinate[] createEnvelope() {
		Geometry onePoly = null;
		Geometry prevPoly = null;
		for (PoseSteering ps : this.trajectory.getPoseSteering()) {
			GeometricShapeFactory gsf = new GeometricShapeFactory();
			gsf.setHeight(width);
			gsf.setWidth(length);
			gsf.setCentre(new Coordinate(ps.getX(),ps.getY()));
			gsf.setRotation(ps.getTheta());
			Polygon rect = gsf.createRectangle();
			if (onePoly == null) {
				onePoly = rect;
				prevPoly = rect;
			}
			else {
				Geometry auxPoly = prevPoly.union(rect);
				onePoly = onePoly.union(auxPoly.convexHull());
				prevPoly = rect;
			}
		}
		return onePoly.getCoordinates();
	}

	public void setTrajectory(Trajectory traj) {
		this.trajectory = traj;
		this.setDomain(new LineStringDomain(this,traj.getPositions()));
		PolygonalDomain env = new PolygonalDomain(null,createEnvelope());
//		PolygonalDomain newEnv = new PolygonalDomain(this, env.getGeometry().convexHull().getCoordinates());
		this.setDomain(env);
	}
	
	public Trajectory getTrajectory() {
		return trajectory;
	}
	
	public AllenIntervalConstraint getDurationConstriant() {
		AllenIntervalConstraint ret = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Duration, new Bounds((long)(this.getTrajectory().getMinTraversalTime()*1000.0), APSPSolver.INF));
		ret.setFrom(this);
		ret.setTo(this);
		return ret;
	}
	
	public int getPathLength() {
		return this.trajectory.getPoseSteering().length;
	}
	
	@Override
	public void setDomain(Domain d) {
		if (d instanceof LineStringDomain) {
			this.getInternalVariables()[1].setDomain(d);
		}
		else if (d instanceof PolygonalDomain) {
			this.getInternalVariables()[2].setDomain(d);
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.id+"";
	}
	
	/**
	 * Returns the temporal part of this {@link TrajectoryEnvelope}.
	 * @return An {@link AllenInterval} representing the temporal part of this {@link TrajectoryEnvelope}.
	 */
	public AllenInterval getTemporalVariable() {
		return (AllenInterval)this.getInternalVariables()[0];
	}

	/**
	 * Returns the spatial part of this {@link TrajectoryEnvelope}.
	 * @return A {@link GeometricShapeVariable} representing the spatial part of this {@link TrajectoryEnvelope}.
	 */
	public GeometricShapeVariable getReferencePathVariable() {
		return (GeometricShapeVariable)this.getInternalVariables()[1];
	}

	/**
	 * Returns the spatial part of this {@link TrajectoryEnvelope}.
	 * @return A {@link GeometricShapeVariable} representing the spatial part of this {@link TrajectoryEnvelope}.
	 */
	public GeometricShapeVariable getEnvelopeVariable() {
		return (GeometricShapeVariable)this.getInternalVariables()[2];
	}

	@Override
	public String[] getSymbols() {
		return new String[] {((GeometricShapeDomain)getEnvelopeVariable().getDomain()).getGeometry().getCoordinates().length+""};
	}

	@Override
	public Variable getVariable() {
		return this;
	}

}
