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
package org.metacsp.meta.simplePlanner;

import org.metacsp.multi.allenInterval.AllenIntervalConstraint;

import java.util.Arrays;

public class SimpleOperator {

	protected String head;
	protected AllenIntervalConstraint[] requirementConstraints;
	protected String[] requirementActivities;
	protected int[] usages;
	protected AllenIntervalConstraint[][] extraConstraints;
	
	static enum ReservedWord {Head, SimpleOperator, PlanningOperator, Resource, Constraint, SimpleDomain, RequiredResource, RequiredState, AchievedState};
	
	public SimpleOperator(String head, AllenIntervalConstraint[] requirementConstraints, String[] requirementActivities, int[] usages) {
		this.head = head;
		if (requirementActivities != null) {
			for (String a : requirementActivities) {
				if (a.equals(head)) throw new InvalidActivityException(a);
			}
		}
		this.requirementConstraints = requirementConstraints;
		this.requirementActivities = requirementActivities;
		this.usages = usages;
		if (requirementConstraints != null) this.extraConstraints = new AllenIntervalConstraint[requirementActivities.length+1][requirementActivities.length+1];
		else this.extraConstraints = new AllenIntervalConstraint[1][1];
	}
	
	public void addConstraint(AllenIntervalConstraint c, int from, int to) {
		extraConstraints[from][to] = c;
	}
	
	public AllenIntervalConstraint[][] getExtraConstraints() {
		return this.extraConstraints;
	}
	
	public String getHead() {
		return head;
	}

	public AllenIntervalConstraint[] getRequirementConstraints() {
		return requirementConstraints;
	}

	public String[] getRequirementActivities() {
		return requirementActivities;
	}

	public int[] getUsages() {
		return usages;
	}

	public String toString() {
		String ret = "";
		String acts = "";
		if (requirementActivities != null) {
			for (int i = 0; i < requirementActivities.length; i++) {
				System.out.println("!! " + this.head + " reqs: " + requirementConstraints[i]);
				System.out.println("?? " + requirementConstraints[i].getBounds());
				acts += head + " --" + Arrays.toString(requirementConstraints[i].getTypes()) + " " + Arrays.toString(requirementConstraints[i].getBounds()) + "--> " + requirementActivities[i];
				if (i != requirementActivities.length-1) acts += "\n";
			}
		}
		if (!acts.trim().equals("")) ret += acts;
		if (usages != null) {
			if (!acts.trim().equals("")) ret += "\n";
			ret += head + " usage: " + Arrays.toString(usages);
		}
		String extraCons = "";
		if (extraConstraints != null) {
			extraCons += "\n";
			for (int i = 0; i < extraConstraints.length; i++) {
				for (int j = 0; j < extraConstraints[i].length; j++) {
					if (extraConstraints[i][j] != null) {
						if (i == 0) extraCons += head;
						else extraCons += requirementActivities[i-1];
						extraCons += " --" + Arrays.toString(extraConstraints[i][j].getTypes()) + " " + Arrays.toString(extraConstraints[i][j].getBounds()) + "--> ";
						if (j == 0) extraCons += head;
						else extraCons += requirementActivities[j-1];
					}
				}				
			}
		}
		if (!extraCons.trim().equals("")) ret += extraCons;
		return ret;
	}
	
	
}
