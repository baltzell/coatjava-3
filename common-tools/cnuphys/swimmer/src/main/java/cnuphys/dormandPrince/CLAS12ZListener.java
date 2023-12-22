package cnuphys.dormandPrince;

import java.io.File;
import java.io.FileNotFoundException;

import cnuphys.magfield.FastMath;
import cnuphys.magfield.MagneticFieldInitializationException;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.MagneticFields.FieldType;

public class CLAS12ZListener extends CLAS12BoundaryListener {
	
	//the target z (cm)
	private double _zTarget;
	
	//the starting sign. When this changes we have crossed.
	private double _startSign;

	/**
	 * Create a CLAS12 boundary target Z listener, for swimming to a fixed z
	 * 
	 * @param ivals           the initial values of the swim
	 * @param zTarget         the target z (cm)
	 * @param sFinal          the final or max path length (cm)
	 * @param accuracy        the desired accuracy (cm)
	 */
	public CLAS12ZListener(CLAS12Values ivals, double zTarget, double sFinal, double accuracy) {
		super(ivals, sFinal, accuracy);
		_zTarget = zTarget;
		_startSign = sign(ivals.z);
	}

	@Override
	public boolean crossedBoundary(double newS, double[] newU) {
		double newZ = newU[2];
		int sign = sign(newZ);
		
		if (sign != _startSign) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean accuracyReached(double newS, double[] newU) {
		double dZ = Math.abs(newU[2] - _zTarget);
		return dZ < _accuracy;
	}

	// left or right of the target Z?
	private int sign(double z) {
		return (z < _zTarget) ? -1 : 1;
	}
	

	// used for testing
	public static void main(String arg[]) {
		final MagneticFields mf = MagneticFields.getInstance();
		File mfdir = new File(System.getProperty("user.home"), "magfield");
		System.out.println("mfdir exists: " + (mfdir.exists() && mfdir.isDirectory()));
		try {
			mf.initializeMagneticFields(mfdir.getPath(), "Symm_torus_r2501_phi16_z251_24Apr2018.dat",
					"Symm_solenoid_r601_phi1_z1201_13June2018.dat");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (MagneticFieldInitializationException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Active Field Description: " + MagneticFields.getInstance().getActiveFieldDescription());

//		int q = 1;
//		double p = 2.369457106003491;
//		double theta = 32.985153708129516;
//		double phi = 196.23998861894339;
//		double xo = -0.2988817577588326;
//		double yo = 0.1723213641208851;
//		double zo = -0.3404871441880286;
//		double ztarget = 549.1227330916572;
//		double accuracy = 0.001; //cm
//		double stepsizeAdaptive = 1; // starting stepsize in cm
//		double maxS = 1000; // cm
//		double eps = 9.999999999999999E-5;

		
		int q = -1;
		double p = 2.0;
		double theta = 15;
		double phi = 5;
		double xo = 0.01;
		double yo = 0.02;
		double zo = -0.01;
		double ztarget = 575.00067;
		double accuracy = 0.0001; //cm
		double stepsizeAdaptive = 0.0001; // starting stepsize in cm
		double maxS = 800; // cm
		double eps = 1.0e-7;

		
		MagneticFields.getInstance().setActiveField(FieldType.COMPOSITE);
		CLAS12Swimmer clas12Swimmer = new CLAS12Swimmer(); //new
		

		CLAS12SwimResult c12res = clas12Swimmer.swimZ(q, xo, yo, zo, p, theta, phi, ztarget, maxS, accuracy, stepsizeAdaptive, eps);
		System.out.println("DP result:  " + c12res.toString() + "\n");


		
	}



}
