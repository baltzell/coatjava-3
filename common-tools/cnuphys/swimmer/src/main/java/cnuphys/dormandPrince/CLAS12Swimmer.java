package cnuphys.dormandPrince;

import java.util.Hashtable;

import cnuphys.magfield.FieldProbe;
import cnuphys.magfield.IMagField;
import cnuphys.magfield.MagneticField;

/*
 * A swimmer using the Dormand-Prince 8(5,3) adaptive step size integrator.
 */
public class CLAS12Swimmer {
	
	// Speed of light in m/s
	public static final double C = 2.99792458e10; // cm/s
	
	/** currently swimming */
	public static final int SWIM_SWIMMING = 88;

	/** The swim was a success */
	public static final int SWIM_SUCCESS = 0;
	
	/**
	 * A target, such as a target rho or z, was not reached before the swim was
	 * stopped for some other reason
	 */
	public static final int SWIM_TARGET_MISSED = -1;
	
	
	public static final Hashtable<Integer, String> resultNames = new Hashtable<Integer, String>();
	static {
		resultNames.put(SWIM_SWIMMING, "SWIMMING");
		resultNames.put(SWIM_SUCCESS, "SWIM_SUCCESS");
		resultNames.put(SWIM_TARGET_MISSED, "SWIM_TARGET_MISSED");
	}

	
	// Minimum integration step size in meters
	public static final double MINSTEPSIZE = 1.0e-7; // cm

	// The magnetic field probe.
	// NOTE: the method of interest in FieldProbe takes a position in cm
	// and returns a field in kG.This swim package works in SI (meters and
	// kG.)
	private FieldProbe _probe;

	
	/**
	 * Create a swimmer using the current active field
	 */
	public CLAS12Swimmer() {
		// make a probe using the current active field
		this(FieldProbe.factory());
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 *
	 * @param magneticField the magnetic field
	 */
	public CLAS12Swimmer(MagneticField magneticField) {
		this(FieldProbe.factory(magneticField));
	}

	/**
	 * Create a swimmer specific to a magnetic field
	 *
	 * @param magneticField the magnetic field
	 */
	public CLAS12Swimmer(IMagField magneticField) {
		this(FieldProbe.factory(magneticField));
	}
	
	/**
	 * Create a swimmer specific to a magnetic field probe
	 *
	 * @param probe the magnetic field probe
	 */
	public CLAS12Swimmer(FieldProbe probe) {
		_probe = probe;
	}
	
	/**
	 * The listener that can stop the integration and stores results
	 */
	protected CLAS12Listener _listener;

	/**
	 * Get the probe being used to swim
	 *
	 * @return the probe
	 */
	public FieldProbe getProbe() {
		return _probe;
	}
	
	
	/**
	 * The basic swim method. The swim is terminated when the particle reaches the
	 * pathlength sMax or if the optional listener terminates the swim.
	 * 
	 * @param ode       the ODE to solve
	 * @param u         the initial state vector (x, y, z, tx, ty, tz) x, y, z in cm
	 * @param sMin      the initial value of the independent variable (pathlength) (usually 0)
	 * @param sMax      the final (max) value of the independent variable
	 *                  (pathlength) unless the integration is terminated by the
	 *                  listener
	 * @param h         the initial stepsize in meters
	 * @param tolerance The desired tolerance. The solver will automatically adjust
	 *                  the step size to meet this tolerance.
	 * @param listener  an optional listener that can terminate the integration
	 * @return the result of the swim
	 */
	protected CLAS12SwimResult baseSwim(CLAS12SwimmerODE ode, double u[], double sMin, double sMax, double h, 
			double tolerance, CLAS12Listener listener) {

		_listener = listener;
		//call the basic solver that swims to sMax
		DormandPrince.solve(ode, u, sMin, sMax, h, tolerance, MINSTEPSIZE, _listener);
		
		return new CLAS12SwimResult(_listener);

	}

	
	public CLAS12SwimResult swimToAccuracy(CLAS12SwimmerODE ode, double u[], double sMin, double sMax, double h,
			double tolerance, CLAS12BoundaryListener listener) {

		double s1 = sMin;
		double s2 = sMax;
		

		CLAS12SwimResult cres = null;
		while (true) {
			cres = baseSwim(ode, u, s1, s2, h, tolerance, listener);

			if (listener.getStatus() == SWIM_SUCCESS) {
				// crossed boundary
				// algorithm:
				// 1. set s2 to the current path length
				// 2. remove last point (on far side of boundary)
				// 3. set s1 to the new total path length

				s2 = _listener.getS();
				double z2 = _listener.getU()[2];
				_listener.getTrajectory().removeLastPoint();
				
				if (listener.accuracyReached(cres.getPathLength(), cres.getFinalU())) {
System.err.println("HEY");
					return cres;
				}

				u = _listener.getU();
				s1 = _listener.getS();
				double z1 = _listener.getU()[2];

	//			System.err.println("s1 = " + s1 + "  s2 = " + s2 + " cm");
				System.err.println("z1 = " + z1 + "  z2 = " + z2 + " cm");

				// remove last point again it will be restored as start of appendend swim
				_listener.getTrajectory().removeLastPoint();

				h = Math.abs(s2-s1)/100;
			} else { // failed, reached max path length
				return cres;
			}
		}

	}

	
	/**
	 * Basic swim method. The swim is terminated when the particle reaches the pathlength sMax
	 * or if the optional listener terminates the swim.
	 * @param charge    in integer units of e
	 * @param xo        the x vertex position in cm
	 * @param yo        the y vertex position in cm
	 * @param zo        the z vertex position in cm
	 * @param momentum  the momentum in GeV/c
	 * @param theta     the initial polar angle in degrees
	 * @param phi       the initial azimuthal angle in degrees
	 * @param sMax      the final (max) value of the independent variable
	 *                  (pathlength) unless the integration is terminated by the
	 *                  listener
	 * @param h         the initial stepsize in meters
	 * @param tolerance The desired tolerance. The solver will automatically adjust
	 *                  the step size to meet this tolerance.
     */
	public CLAS12SwimResult  swim(int charge, double xo, double yo, double zo, double momentum, double theta, double phi,
			double sMax, double h, double tolerance) {

		//create the ODE
		CLAS12SwimmerODE ode = new CLAS12SwimmerODE(charge, momentum, _probe);
		
		//create the initial values
		CLAS12Values ivals = new CLAS12Values(charge, xo, yo, zo, momentum, theta, phi);
		
		CLAS12Listener listener = new CLAS12Listener(ivals, sMax);
		return baseSwim(ode, ivals.getU(), 0, sMax, h, tolerance, listener);
	}
	
	/**
	 * Swim to a target z in cm
	 * @param charge    in integer units of e
	 * @param xo        the x vertex position in cm
	 * @param yo        the y vertex position in cm
	 * @param zo        the z vertex position in cm
	 * @param momentum  the momentum in GeV/c
	 * @param theta     the initial polar angle in degrees
	 * @param phi       the initial azimuthal angle in degrees
	 * @param zTarget   the target z position in cm
	 * @param sMax      the final (max) value of the independent variable
	 *                  (pathlength) unless the integration is terminated by the
	 *                  listener
	 * @param h         the initial stepsize in meters
	 * @param tolerance The desired tolerance. The solver will automatically adjust
	 *                  the step size to meet this tolerance.
     */
	public CLAS12SwimResult  swimZ(int charge, double xo, double yo, double zo, double momentum, double theta, double phi,
			double zTarget, double sMax, double accuracy, double h, double tolerance) {
		

		//create the ODE
		CLAS12SwimmerODE ode = new CLAS12SwimmerODE(charge, momentum, _probe);
		
		//create the initial values
		CLAS12Values ivals = new CLAS12Values(charge, xo, yo, zo, momentum, theta, phi);
		
		CLAS12ZListener listener = new CLAS12ZListener(ivals, zTarget, sMax, accuracy);
		
		return swimToAccuracy(ode, ivals.getU(), 0, sMax, h, tolerance, listener);
//		return baseSwim(ode, ivals.getU(), 0, sMax, h, tolerance, listener);

	}
		
	/**
     * Get the result of the swim
     * @return the result of the swim in the listener
     */
	public CLAS12Listener getListener() {
		return _listener;
    }

}
