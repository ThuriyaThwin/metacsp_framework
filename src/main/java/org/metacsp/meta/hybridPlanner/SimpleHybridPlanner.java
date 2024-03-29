package org.metacsp.meta.hybridPlanner;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Vector;

import org.metacsp.framework.Constraint;
import org.metacsp.framework.ConstraintNetwork;
import org.metacsp.framework.Variable;
import org.metacsp.framework.VariablePrototype;
import org.metacsp.framework.meta.MetaConstraintSolver;
import org.metacsp.framework.meta.MetaVariable;
import org.metacsp.meta.simplePlanner.SimpleDomain;
import org.metacsp.meta.simplePlanner.SimpleDomain.markings;
import org.metacsp.meta.simplePlanner.SimpleOperator;
import org.metacsp.meta.simplePlanner.SimpleReusableResource;
import org.metacsp.multi.activity.SymbolicVariableActivity;
import org.metacsp.multi.activity.ActivityNetworkSolver;
import org.metacsp.multi.allenInterval.AllenIntervalConstraint;
import org.metacsp.multi.spatial.rectangleAlgebra.BoundingBox;
import org.metacsp.multi.spatial.rectangleAlgebra.RectangleConstraint;
import org.metacsp.multi.spatial.rectangleAlgebra.RectangleConstraintSolver;
import org.metacsp.multi.spatial.rectangleAlgebra.RectangularRegion;
import org.metacsp.multi.spatial.rectangleAlgebra.UnaryRectangleConstraint;
import org.metacsp.multi.symbols.SymbolicValueConstraint;
import org.metacsp.multi.temporalRectangleAlgebra.SpatialFluent;
import org.metacsp.multi.temporalRectangleAlgebra.SpatialFluentSolver;
import org.metacsp.spatial.reachability.ReachabilityConstraint;
import org.metacsp.spatial.utility.SpatialAssertionalRelation;

public class SimpleHybridPlanner extends MetaConstraintSolver {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long horizon = 0;
	public Vector<SimpleOperator> operatorsAlongBranch = new Vector<SimpleOperator>();
	public Vector<String> unificationAlongBranch = new  Vector<String>();
	private Vector<SymbolicVariableActivity> goals = new Vector<SymbolicVariableActivity>();//this contains original goals (not sub goal)
	private Vector<SymbolicVariableActivity> varInvolvedInOccupiedMetaConstraints = new Vector<SymbolicVariableActivity>();
	private boolean learningFromfailure = false;

	private HashMap<String, Rectangle> observation = new HashMap<String, Rectangle>();
	private HashMap<String, Integer> conflictRanking = null;

	public SimpleHybridPlanner(long origin, long horizon, long animationTime) {
		super(new Class[] {RectangleConstraint.class, UnaryRectangleConstraint.class, AllenIntervalConstraint.class, SymbolicValueConstraint.class, ReachabilityConstraint.class}, 
				animationTime, new SpatialFluentSolver(origin, horizon)	);
		this.horizon = horizon;
	}



	@Override
	public void preBacktrack() {

//		HashMap<String, Rectangle> recs = null;
//		for (int j = 0; j < this.metaConstraints.size(); j++){ 
//			if(this.metaConstraints.get(j) instanceof MetaSpatialAdherenceConstraint ){
//				recs = new HashMap<String, Rectangle>();  
//				for (String str : ((RectangleConstraintSolver)((SpatialFluentSolver)this.getConstraintSolvers()[0])
//						.getConstraintSolvers()[0]).extractAllBoundingBoxesFromSTPs().keySet()) {
//					if(str.endsWith("1")){
//						recs.put( str,((RectangleConstraintSolver)((SpatialFluentSolver)this.getConstraintSolvers()[0])
//								.getConstraintSolvers()[0]).extractAllBoundingBoxesFromSTPs().get(str).getAlmostCentreRectangle());
//					}
//				}
//
//				//				System.out.println("recs: " + recs);
//			}
//		}
//
//
//		HashMap<String,  Vector<String>> overlappedPairs = new HashMap<String, Vector<String>>();
//		conflictRanking = new HashMap<String, Integer>();
//
//		//this has to be commented
//		if(observation != null){
//			//			System.out.println("obs: " + observation);
//			for (String recNew : recs.keySet()) {
//				if(recNew.compareTo("at_table1_table1") == 0) continue;
//				if(recs.get(recNew).getWidth() == 0) break; //the bounds are not updated since the spatiak adherence is not called
//				Vector<String> ovr = new Vector<String>();
//				for (String recOld : observation.keySet()) {
//					if(recOld.compareTo("at_table1_table1") == 0) continue;
//					if(recOld.compareTo(recNew) == 0) continue;
//					if(recs.get(recNew).intersects(observation.get(recOld))){
//						ovr.add(recOld);
//					}
//				}
//				//if(ovr.size() > 0)
//				overlappedPairs.put(recNew, ovr);
//			}
//
//			//			System.out.println("overlappedPairs" + overlappedPairs);
//
//			for (String st : overlappedPairs.keySet()) {
//				if(conflictRanking.get(st) == null){
//					conflictRanking.put(st, 1);
//				}
//				for (int i = 0; i < overlappedPairs.get(st).size(); i++) {
//					if(conflictRanking.get(overlappedPairs.get(st).get(i)) != null){
//						int rank = conflictRanking.get(st); 
//						conflictRanking.put(overlappedPairs.get(st).get(i), ++rank);
//					}else{
//						conflictRanking.put(overlappedPairs.get(st).get(i), 1);
//					}					
//				}
//			}
//		}

		//		System.out.println("rank: " + conflictRanking);

	}


	@Override
	public void postBacktrack(MetaVariable mv) {



		if (mv.getMetaConstraint() instanceof FluentBasedSimpleDomain){
			for (Variable v : mv.getConstraintNetwork().getVariables()) {
				v.setMarking(markings.UNJUSTIFIED);
			}
		}

		int armCapacity = 100;
		FluentBasedSimpleDomain causalReasoner = null;
		for (int j = 0; j < this.metaConstraints.size(); j++) {
			if(this.metaConstraints.get(j) instanceof FluentBasedSimpleDomain ){
				causalReasoner = ((FluentBasedSimpleDomain)this.metaConstraints.elementAt(j));
				for (String  resourceName : causalReasoner.getResources().keySet()) {
					if(resourceName.compareTo("arm") == 0)
						armCapacity = causalReasoner.getResources().get(resourceName).getCapacity();						
				}
			}
		}


		if (mv.getMetaConstraint() instanceof MetaOccupiedConstraint){
			for (Variable v : mv.getConstraintNetwork().getVariables()) {
				if(!varInvolvedInOccupiedMetaConstraints.contains((SymbolicVariableActivity)v)){
					//					System.out.println("== occupied constraints == " + (Activity)v);
					varInvolvedInOccupiedMetaConstraints.add((SymbolicVariableActivity)v);	
				}
			}
			if(armCapacity <= varInvolvedInOccupiedMetaConstraints.size()){
				causalReasoner.applyFreeArmHeuristic(varInvolvedInOccupiedMetaConstraints, "tray");
				causalReasoner.activeHeuristic(false);
				learningFromfailure  = true;
				//metaOccupiedConstraint.activeHeuristic(true);
			}
		}




	}

	public boolean learningFromFailure(){
		return learningFromfailure;
	}

	@Override
	protected void retractResolverSub(ConstraintNetwork metaVariable, ConstraintNetwork metaValue) {
		
		if (metaValue.specilizedAnnotation != null && metaValue.specilizedAnnotation instanceof SimpleOperator) {
			this.operatorsAlongBranch.remove(operatorsAlongBranch.size()-1);
			//			System.out.println("-------------------> popped " + metaValue.specilizedAnnotation);
		}

		ActivityNetworkSolver groundSolver = (ActivityNetworkSolver)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getConstraintSolvers()[1];
		Vector<Variable> activityToRemove = new Vector<Variable>();
		Vector<Variable> fluentToRemove = new Vector<Variable>();
		Vector<Variable> rectangleToRemove = new Vector<Variable>();
		
		for (Variable v : metaValue.getVariables()) {
			if (!metaVariable.containsVariable(v)) {
				if (v instanceof VariablePrototype) {
					if(((String)((VariablePrototype)v).getParameters()[1]).contains(manipulationAreaEncoding)){
						Variable vReal = metaValue.getSubstitution((VariablePrototype)v);
						if (vReal != null) {
							fluentToRemove.add(vReal);
						}
					}
					else{
						Variable vReal = metaValue.getSubstitution((VariablePrototype)v);
						if (vReal != null) {
							activityToRemove.add(vReal);
						
						}						
					}
				}
				else if(v instanceof RectangularRegion){
					if(((RectangularRegion)v).getName().contains("placingArea") ||
							((RectangularRegion)v).getName().contains("pickingArea")){						
						rectangleToRemove.add(v);
					}
				}					
			}
		}



		for (int j = 0; j < this.metaConstraints.size(); j++){ 
			if(this.metaConstraints.get(j) instanceof FluentBasedSimpleDomain ){
				FluentBasedSimpleDomain mcc = (FluentBasedSimpleDomain)this.metaConstraints.get(j);
				for (Variable v : fluentToRemove) {
					for (SimpleReusableResource rr : mcc.getCurrentReusableResourcesUsedByActivity((SymbolicVariableActivity)((SpatialFluent)v).getActivity())) {
						rr.removeUsage((SymbolicVariableActivity)((SpatialFluent)v).getActivity());
					}
				}
				for (Variable v : activityToRemove) {
					for (SimpleReusableResource rr : mcc.getCurrentReusableResourcesUsedByActivity((SymbolicVariableActivity)v)) {
						rr.removeUsage((SymbolicVariableActivity)v);
					}
				}
			}
		}


		boolean isRtractingSpatialRelations = false;
		if (metaValue.specilizedAnnotation != null && metaValue.specilizedAnnotation instanceof Integer) {
			isRtractingSpatialRelations = true;
		}
		
		

		if(isRtractingSpatialRelations){
			Vector<SpatialFluent> spatialFluentToBeRemoved = new Vector<SpatialFluent>();
			System.out.println("Meta Value of MetaSpatialConstraint is retracted");

			for (int i = 0; i < this.getConstraintSolvers()[0].getVariables().length; i++) {
				if(((SymbolicVariableActivity)((SpatialFluent)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getVariables()[i]).getActivity()).getTemporalVariable().getEST() == 0 &&
						((SymbolicVariableActivity)((SpatialFluent)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getVariables()[i]).getActivity()).getTemporalVariable().getLST() == horizon){
					spatialFluentToBeRemoved.add((SpatialFluent)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getVariables()[i]);
					//					System.out.println((SpatialFluent)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getVariables()[i]);
				}
			}

			for (int i = 0; i < this.metaConstraints.size(); i++){
				if(this.metaConstraints.get(i) instanceof MetaSpatialAdherenceConstraint ){	
					for (int j = 0; j < ((MetaSpatialAdherenceConstraint)this.metaConstraints.get(i)).getsAssertionalRels().size(); j++) {
						((MetaSpatialAdherenceConstraint)this.metaConstraints.get(i)).getsAssertionalRels().get(j).setUnaryAtRectangleConstraint
						(((MetaSpatialAdherenceConstraint)this.metaConstraints.get(i)).getCurrentAssertionalCons().
								get(((MetaSpatialAdherenceConstraint)this.metaConstraints.get(i)).getsAssertionalRels().get(j).getFrom()));
						//							System.out.println("Assertional Realtion: " + (((SpatialSchedulable)this.metaConstraints.get(i)).getCurrentAssertionalCons().
						//									get(((SpatialSchedulable)this.metaConstraints.get(i)).getsAssertionalRels()[j].getFrom())));
					}			
				}
			}
			System.out.println(spatialFluentToBeRemoved);
			((SpatialFluentSolver)this.getConstraintSolvers()[0]).removeVariables(spatialFluentToBeRemoved.toArray(new Variable[spatialFluentToBeRemoved.size()]));
		}
		
//		System.out.println("fluentToBeRemoved: "+fluentToRemove );
		((SpatialFluentSolver)this.getConstraintSolvers()[0]).removeVariables(fluentToRemove.toArray(new Variable[fluentToRemove.size()]));
		groundSolver.removeVariables(activityToRemove.toArray(new Variable[activityToRemove.size()]));
		((RectangleConstraintSolver)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getConstraintSolvers()[0]).removeVariables(rectangleToRemove.toArray(new Variable[rectangleToRemove.size()]));
		
		
	}



	@Override
	protected boolean addResolverSub(ConstraintNetwork metaVariable, ConstraintNetwork metaValue) {

		if (metaValue.specilizedAnnotation != null && metaValue.specilizedAnnotation instanceof SimpleOperator) {
			if (operatorsAlongBranch.contains((metaValue.specilizedAnnotation))) {
				return false;					
			}
			operatorsAlongBranch.add((SimpleOperator)metaValue.specilizedAnnotation);
		}

//		if (metaValue.specilizedAnnotation != null && metaValue.specilizedAnnotation instanceof SimpleOperator) {
//			if (operatorsAlongBranch.contains((metaValue.specilizedAnnotation))) {
//				return false;					
//			}
//			operatorsAlongBranch.add((SimpleOperator)metaValue.specilizedAnnotation);
//		}

		
		//this if handles the cases when the controllables are not unified and there is no operators which can be activated
		//then we annotated as false to force it to be failed rather than return null constraint network 
		if (metaValue.specilizedAnnotation != null && metaValue.specilizedAnnotation instanceof Boolean) {
//			System.out.println("Annotation: " + (Boolean)metaValue.getSpecilizedAnnotation());
			if (!(Boolean)metaValue.getSpecilizedAnnotation()) {
				System.out.println(">>>>>>>>>>>>>>>>>");
				return false;
			}
		}

		ActivityNetworkSolver groundSolver = (ActivityNetworkSolver)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getConstraintSolvers()[1];

		Vector<SymbolicVariableActivity> manAreaActs = new Vector<SymbolicVariableActivity>();

		//Make real variables from variable prototypes
		for (Variable v :  metaValue.getVariables()) {
			if (v instanceof VariablePrototype) {
				// 	Parameters for real instantiation: the first is the component itself, the second is
				//	the symbol of the Activity to be instantiated
				String component = (String)((VariablePrototype) v).getParameters()[0];
				String symbol = (String)((VariablePrototype) v).getParameters()[1];

				if(symbol.contains(manipulationAreaEncoding)){
					SpatialFluent sf = (SpatialFluent)((SpatialFluentSolver)this.getConstraintSolvers()[0]).createVariable(component);
					sf.setName(symbol);
					((RectangularRegion)sf.getInternalVariables()[0]).setName(symbol);
					((SymbolicVariableActivity)sf.getInternalVariables()[1]).setSymbolicDomain(symbol);
					((SymbolicVariableActivity)sf.getInternalVariables()[1]).setMarking((SimpleDomain.markings)v.getMarking());
					metaValue.addSubstitution((VariablePrototype)v, sf);
				}
				else{				
					SymbolicVariableActivity tailActivity = null;
					tailActivity = (SymbolicVariableActivity)groundSolver.createVariable(component);
					tailActivity.setSymbolicDomain(symbol);
					tailActivity.setMarking(v.getMarking());
					metaValue.addSubstitution((VariablePrototype)v, tailActivity);
				}

			}
		}


		//the idea behind this if is: if it is a constraint including manipulationArea, the rectangleConstaint has to be between two rectangle not between two fluent 
		//Involve real variables in the constraints
		for (Constraint con : metaValue.getConstraints()) {
			Constraint clonedConstraint = (Constraint)con.clone();  
			Variable[] oldScope = con.getScope();
			Variable[] newScope = new Variable[oldScope.length];			
			if(con instanceof AllenIntervalConstraint){
				for (int i = 0; i < oldScope.length; i++) {
					if (oldScope[i] instanceof VariablePrototype) {
						if(((String)((VariablePrototype) oldScope[i]).getParameters()[1]).contains(manipulationAreaEncoding))
							newScope[i] = (SymbolicVariableActivity)((SpatialFluent)metaValue.getSubstitution((VariablePrototype)oldScope[i])).getActivity();
						else
							newScope[i] = metaValue.getSubstitution((VariablePrototype)oldScope[i]);
					}
					else{
						if (oldScope[i] instanceof SpatialFluent) {
							newScope[i] = ((SpatialFluent)oldScope[i]).getActivity();
						}
						else
							newScope[i] = oldScope[i];
					}
				}
	
			}
			else{ //if it is Rectangle Constraint
				for (int i = 0; i < oldScope.length; i++) {
					if (oldScope[i] instanceof VariablePrototype) {
						if(((String)((VariablePrototype) oldScope[i]).getParameters()[1]).contains(manipulationAreaEncoding))
							newScope[i] = (RectangularRegion)((SpatialFluent)metaValue.getSubstitution((VariablePrototype)oldScope[i])).getRectangularRegion();
						else
							newScope[i] = metaValue.getSubstitution((VariablePrototype)oldScope[i]);
					}
					else{
						if (oldScope[i] instanceof SpatialFluent) {
							newScope[i] = ((SpatialFluent)oldScope[i]).getRectangularRegion();
						}
						else
							newScope[i] = oldScope[i];
					}
				}				
			}
			clonedConstraint.setScope(newScope);
			metaValue.removeConstraint(con);
//			System.out.println("clonedConstraint: " + clonedConstraint);
			metaValue.addConstraint(clonedConstraint);				
		}





		for (Variable v : metaValue.getVariables()) {
			for (int j = 0; j < this.metaConstraints.size(); j++) {
				if(this.metaConstraints.get(j) instanceof FluentBasedSimpleDomain ){
					FluentBasedSimpleDomain metaCausalConatraint = (FluentBasedSimpleDomain)this.metaConstraints.elementAt(j);
					for (SimpleReusableResource rr : metaCausalConatraint.getCurrentReusableResourcesUsedByActivity(v)) {
						rr.setUsage((SymbolicVariableActivity)v);
					}
				}
			}
		}



		return true;
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

	public HashMap<String, BoundingBox> getOldRectangularRegion(){

		for (int j = 0; j < this.metaConstraints.size(); j++){ 
			if(this.metaConstraints.get(j) instanceof MetaSpatialAdherenceConstraint ){
				return ((MetaSpatialAdherenceConstraint)this.metaConstraints.get(j)).getOldRectangularRegion();
			}
		}
		return null;
	}


	public void addGoal(SymbolicVariableActivity act) {
		goals.add(act);
	}

	public Vector<SymbolicVariableActivity> getGoals(){
		return goals;
	}



	public void addObservation(HashMap<String, Rectangle> observation) {
		this.observation = observation;
	}

	private Vector<SpatialFluent> observedSpatialFluents = new Vector<SpatialFluent>();
	public void addObservedSpatialFluents(SpatialFluent observedSpatialFluent) {
		observedSpatialFluents.add(observedSpatialFluent);
	}

	public Vector<SpatialFluent> getObservedSpatialFluents(){
		return observedSpatialFluents;
	}

	public HashMap<String, Integer> getConflictRanking(){
		return conflictRanking;
	}

	private String manipulationAreaEncoding = "";
	public void setManipulationAreasEncoding(String manipulationAreaEncoding){
		this.manipulationAreaEncoding = manipulationAreaEncoding;
	}
	
	public String getManipulationAreaEncoding(){
		return manipulationAreaEncoding;
	}
	
	public void setObstacles(Vector<SpatialAssertionalRelation> sAssertionalRels){
		Vector<BoundingBox> bbs = new Vector<BoundingBox>();
		for (int j = 0; j < sAssertionalRels.size(); j++) {
			if(sAssertionalRels.get(j).getOntologicalProp().isObstacle()){
				BoundingBox bb = new BoundingBox(sAssertionalRels.get(j).getUnaryAtRectangleConstraint().getBounds()[0], 
						sAssertionalRels.get(j).getUnaryAtRectangleConstraint().getBounds()[1],
						sAssertionalRels.get(j).getUnaryAtRectangleConstraint().getBounds()[2],
						sAssertionalRels.get(j).getUnaryAtRectangleConstraint().getBounds()[3]);
				bbs.add(bb);
			}
		}				
		((RectangleConstraintSolver)((SpatialFluentSolver)this.getConstraintSolvers()[0]).getConstraintSolvers()[0]).setFilteringArea(bbs);
	}
}
