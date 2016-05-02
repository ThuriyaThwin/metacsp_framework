/*******************************************************************************
 * Copyright (c) 2010-2013 Federico Pecora <federico.pecora@oru.se>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package org.metacsp.meta.spatioTemporal.paths;

import java.util.ArrayList;
import java.util.HashMap;

import org.metacsp.framework.ConstraintNetwork;
import org.metacsp.framework.Variable;
import org.metacsp.framework.meta.MetaConstraintSolver;
import org.metacsp.framework.meta.MetaVariable;
import org.metacsp.multi.activity.ActivityNetworkSolver;
import org.metacsp.multi.allenInterval.AllenIntervalConstraint;
import org.metacsp.multi.spatial.DE9IM.DE9IMRelation;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeDomain;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeVariable;
import org.metacsp.multi.spatioTemporal.paths.PoseSteering;
import org.metacsp.multi.spatioTemporal.paths.Trajectory;
import org.metacsp.multi.spatioTemporal.paths.TrajectoryEnvelope;
import org.metacsp.multi.spatioTemporal.paths.TrajectoryEnvelopeSolver;
import org.metacsp.multi.symbols.SymbolicValueConstraint;
import org.metacsp.time.APSPSolver;
import org.metacsp.time.Bounds;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class TrajectoryEnvelopeScheduler extends MetaConstraintSolver {

	private ArrayList<TrajectoryEnvelope> envelopesForScheduling = new ArrayList<TrajectoryEnvelope>();
	private static final long serialVersionUID = 8551829132754804513L;
	private HashMap<TrajectoryEnvelope,ArrayList<TrajectoryEnvelope>> refinedWith = new HashMap<TrajectoryEnvelope, ArrayList<TrajectoryEnvelope>>();

	public TrajectoryEnvelopeScheduler(long origin, long horizon, long animationTime) {
		super(new Class[] {AllenIntervalConstraint.class, DE9IMRelation.class}, animationTime, new TrajectoryEnvelopeSolver(origin, horizon));
	}

	public TrajectoryEnvelopeScheduler(long origin, long horizon) {
		super(new Class[] {AllenIntervalConstraint.class, DE9IMRelation.class}, 0, new TrajectoryEnvelopeSolver(origin, horizon));
	}

	@Override
	public void preBacktrack() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void retractResolverSub(ConstraintNetwork metaVariable, ConstraintNetwork metaValue) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean addResolverSub(ConstraintNetwork metaVariable,
			ConstraintNetwork metaValue) {
		return true;

	}

	@Override
	public void postBacktrack(MetaVariable mv) {
		// TODO Auto-generated method stub

	}

	@Override
	protected double getUpperBound() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void setUpperBound() {
		// TODO Auto-generated method stub

	}

	@Override
	protected double getLowerBound() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void setLowerBound() {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean hasConflictClause(ConstraintNetwork metaValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void resetFalseClause() {
		// TODO Auto-generated method stub

	}	

	public ConstraintNetwork refineTrajectoryEnvelopes() {
		ConstraintNetwork ret = new ConstraintNetwork(null);
		
		boolean done = false;
		while (!done) {
			done = true;
			Variable[] varsOneIteration = this.getConstraintSolvers()[0].getVariables();
			for (int i = 0; i < varsOneIteration.length-1; i++) {
				for (int j = i+1; j < varsOneIteration.length; j++) {

					//Get TEs
					TrajectoryEnvelope te1 = (TrajectoryEnvelope)varsOneIteration[i];
					TrajectoryEnvelope te2 = (TrajectoryEnvelope)varsOneIteration[j];

					//Init data structures
					if (!refinedWith.containsKey(te1)) refinedWith.put(te1,new ArrayList<TrajectoryEnvelope>());
					if (!refinedWith.containsKey(te2)) refinedWith.put(te2,new ArrayList<TrajectoryEnvelope>());

					// If != robots
					boolean te1HasSub = te1.hasSubEnvelopes();
					boolean te2HasSub = te2.hasSubEnvelopes();
					if (te1.getRobotID() != te2.getRobotID()) {
						//if they intersect
						GeometricShapeVariable poly1 = te1.getEnvelopeVariable();
						GeometricShapeVariable poly2 = te2.getEnvelopeVariable();
						Geometry shape1 = ((GeometricShapeDomain)poly1.getDomain()).getGeometry();
						Geometry shape2 = ((GeometricShapeDomain)poly2.getDomain()).getGeometry();
						if (shape1.intersects(shape2)) {
							if (!te2HasSub && te1.getRefinable() && !refinedWith.get(te1).contains(te2)) {
								ConstraintNetwork ref1 = refineTrajectoryEnvelopes(te1, te2);
								refinedWith.get(te1).add(te2);
								ret.join(ref1);
								done = false;
							}
							if (!te1HasSub && te2.getRefinable() && !refinedWith.get(te2).contains(te1)) {
								ConstraintNetwork ref2 = refineTrajectoryEnvelopes(te2, te1);
								refinedWith.get(te2).add(te1);
								ret.join(ref2);
								done = false;
							}
						}
					}
				}
			}			
		}
		//recompute usages
		for (TrajectoryEnvelope te : envelopesForScheduling) {
			((Map)this.getMetaConstraints()[0]).removeUsage(te);		
		}
		for (Variable v : this.getConstraintSolvers()[0].getVariables()) {
			TrajectoryEnvelope te = (TrajectoryEnvelope)v;
			if (!te.hasSuperEnvelope()) {
				for (TrajectoryEnvelope gte : te.getGroundEnvelopes()) {
					((Map)this.getMetaConstraints()[0]).setUsage(gte);
				}
			}
		}
		return ret;
	}

	private ConstraintNetwork refineTrajectoryEnvelopes(TrajectoryEnvelope var1, TrajectoryEnvelope var2) {
		logger.fine("Refining " + var1 + " with " + var2);
		TrajectoryEnvelopeSolver solver = (TrajectoryEnvelopeSolver)this.getConstraintSolvers()[0];
		ConstraintNetwork toReturn = new ConstraintNetwork(null);
		GeometryFactory gf = new GeometryFactory();
		Geometry se1 = ((GeometricShapeDomain)var1.getEnvelopeVariable().getDomain()).getGeometry();
		Geometry se2 = ((GeometricShapeDomain)var2.getEnvelopeVariable().getDomain()).getGeometry();
		Geometry intersectionse1se2 = se1.intersection(se2);

		ArrayList<PoseSteering> var1sec1 = new ArrayList<PoseSteering>();
		ArrayList<PoseSteering> var1sec2 = new ArrayList<PoseSteering>();
		ArrayList<PoseSteering> var1sec3 = new ArrayList<PoseSteering>();
		for (int i = 0; i < var1.getPathLength(); i++) {
			Coordinate coord = var1.getTrajectory().getPositions()[i];
			PoseSteering ps = var1.getTrajectory().getPoseSteering()[i];
			Point point = gf.createPoint(coord);
			if (!intersectionse1se2.contains(point) && var1sec2.isEmpty()) {
				var1sec1.add(ps);
			}
			else if (intersectionse1se2.contains(point)) {
				var1sec2.add(ps);
			}
			else if (!intersectionse1se2.contains(point) && !var1sec2.isEmpty()) {
				var1sec3.add(ps);
			}
		}

		boolean skipSec1 = false;
		boolean skipSec3 = false;
		
		//Add to start
		boolean done = false;
		while (!done) {
			try {
				Geometry lastPolySec1 = var1.makeFootprint(var1sec1.get(var1sec1.size()-1));
				if (lastPolySec1.disjoint(se2)) done = true;
				else {
					var1sec2.add(0,var1sec1.get(var1sec1.size()-1));
					var1sec1.remove(var1sec1.size()-1);
					logger.info("Added to start... (1)");
				}
			} catch (IndexOutOfBoundsException e) { skipSec1 = true; done = true; }
		}
		//If sec1 emptied, remove it
		if (var1sec1.size() == 1) {
			var1sec2.add(0,var1sec1.get(var1sec1.size()-1));
			var1sec1.remove(var1sec1.size()-1);
			skipSec1 = true;
			System.out.println("REDUCED SEC 1");
		}

		//Add to end
		done = false;
		while (!done) {
			try {
				Geometry firstPolySec3 = var1.makeFootprint(var1sec3.get(0));
				if (firstPolySec3.disjoint(se2)) done = true;
				else {
					var1sec2.add(var1sec3.get(0));
					var1sec3.remove(0);
					logger.info("Added to end... (1)");
				}
			} catch (IndexOutOfBoundsException e) { skipSec3 = true; done = true; }
		}
		//If sec3 emptied, remove it
		if (var1sec3.size() == 1) {
			var1sec2.add(var1sec3.get(0));
			var1sec3.remove(0);
			skipSec3 = true;
			System.out.println("REDUCED SEC 3");
		}
		
		if (var1sec2.size() < 2) {
			if (var1sec1.size() > 2) {
				var1sec2.add(0,var1sec1.get(var1sec1.size()-1));
				var1sec1.remove(var1sec1.size()-1);
				logger.info("Added to start... (2)");
			}
			else if (var1sec3.size() > 2) {
				var1sec2.add(var1sec3.get(0));
				var1sec3.remove(0);				
				logger.info("Added to end... (2)");
			}
		}

		if ((skipSec1 && skipSec3) || var1sec2.size() < 2) {
			System.out.println("NOTHING TO DO");
			return toReturn;
		}

		var1.setRefinable(false);
		ArrayList<Trajectory> newTrajectories = new ArrayList<Trajectory>();
		ArrayList<TrajectoryEnvelope> newTrajectoryEnvelopes = new ArrayList<TrajectoryEnvelope>();
				
		System.out.println("var1sec1.size() = " + var1sec1.size());
		System.out.println("var1sec2.size() = " + var1sec2.size());
		System.out.println("var1sec3.size() = " + var1sec3.size());
		System.out.println("TOT: " + var1.getTrajectory().getPoseSteering().length);
		if (!skipSec1) {
			newTrajectories.add(new Trajectory(var1sec1.toArray(new PoseSteering[var1sec1.size()]),var1.getTrajectory().getDts(0, var1sec1.size())));
			newTrajectories.add(new Trajectory(var1sec2.toArray(new PoseSteering[var1sec2.size()]),var1.getTrajectory().getDts(var1sec1.size(), var1sec1.size()+var1sec2.size())));
			if (!skipSec3) {
				newTrajectories.add(new Trajectory(var1sec3.toArray(new PoseSteering[var1sec3.size()]),var1.getTrajectory().getDts(var1sec1.size()+var1sec2.size(),var1.getTrajectory().getPoseSteering().length)));
			}
		}
		else {
			newTrajectories.add(new Trajectory(var1sec2.toArray(new PoseSteering[var1sec2.size()]),var1.getTrajectory().getDts(0, var1sec2.size())));
			if (!skipSec3) {
				newTrajectories.add(new Trajectory(var1sec3.toArray(new PoseSteering[var1sec3.size()]),var1.getTrajectory().getDts(var1sec2.size(),var1.getTrajectory().getPoseSteering().length)));
			}			
		}

		Variable[] newVars = solver.createVariables(newTrajectories.size());
		for (int i = 0; i < newVars.length; i++) {
			TrajectoryEnvelope te = (TrajectoryEnvelope)newVars[i];
			//Only for second!
			if ((!skipSec1 && i == 1) || (skipSec1 && i == 0)) {
				te.setRefinable(false);
				refinedWith.get(var2).add(te);
			}
			System.out.println("doing i = " + i + " skipsec1: " + skipSec1 + " skipsec3: " + skipSec3);
			te.setTrajectory(newTrajectories.get(i));
			te.setSuperEnvelope(var1);
			te.setRobotID(var1.getRobotID());
			var1.addSubEnvelope(te);
//			((Map)this.getMetaConstraints()[0]).setUsage(te);
			newTrajectoryEnvelopes.add(te);			
		}

		System.out.println("REFINEMENT (w/ " + var2 + "): " + var1 + " --> " + newTrajectoryEnvelopes);

		AllenIntervalConstraint starts = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Starts);
		starts.setFrom(newTrajectoryEnvelopes.get(0));
		starts.setTo(var1);
		toReturn.addConstraint(starts);

		AllenIntervalConstraint finishes = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Finishes);
		finishes.setFrom(newTrajectoryEnvelopes.get(newTrajectoryEnvelopes.size()-1));
		finishes.setTo(var1);
		toReturn.addConstraint(finishes);

		long minTimeToTransition12 = (long)(TrajectoryEnvelope.RESOLUTION*(newTrajectoryEnvelopes.get(1).getTrajectory().getDTs()[0]-newTrajectoryEnvelopes.get(0).getTrajectory().getDTs()[newTrajectoryEnvelopes.get(0).getTrajectory().getDTs().length-1]));
		AllenIntervalConstraint before1 = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Before, new Bounds(minTimeToTransition12,APSPSolver.INF));
		before1.setFrom(newTrajectoryEnvelopes.get(0));
		before1.setTo(newTrajectoryEnvelopes.get(1));
		toReturn.addConstraint(before1);

		if (newTrajectoryEnvelopes.size() > 2) {
			long minTimeToTransition23 = (long)(TrajectoryEnvelope.RESOLUTION*(newTrajectoryEnvelopes.get(2).getTrajectory().getDTs()[0]-newTrajectoryEnvelopes.get(1).getTrajectory().getDTs()[newTrajectoryEnvelopes.get(1).getTrajectory().getDTs().length-1]));
			AllenIntervalConstraint before2 = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Before, new Bounds(minTimeToTransition23,APSPSolver.INF));
			before2.setFrom(newTrajectoryEnvelopes.get(1));
			before2.setTo(newTrajectoryEnvelopes.get(2));
			toReturn.addConstraint(before2);
		}

		solver.addConstraints(toReturn.getConstraints());
		return toReturn;
	}

}
