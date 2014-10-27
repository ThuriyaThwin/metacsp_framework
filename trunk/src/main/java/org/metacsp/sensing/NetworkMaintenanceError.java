package org.metacsp.sensing;

import org.metacsp.multi.allenInterval.AllenIntervalConstraint;

import cern.colt.Arrays;

public class NetworkMaintenanceError extends Error {

	private static final long serialVersionUID = -5616267862743820271L;

	public NetworkMaintenanceError(AllenIntervalConstraint ... con) {
		super("Cannot add maintenance constraint(s) " + Arrays.toString(con));
	}

}
