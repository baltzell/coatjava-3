/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.swimtools;

import cnuphys.adaptiveSwim.AdaptiveSwimException;
import cnuphys.rk4.IStopper;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.util.Plane;
import cnuphys.swimZ.SwimZException;
import cnuphys.swimZ.SwimZResult;
import cnuphys.swimZ.SwimZStateVector;
import org.apache.commons.math3.util.FastMath;
import org.jlab.geom.prim.Vector3D;
import org.jlab.geom.prim.Point3D;
import cnuphys.adaptiveSwim.AdaptiveSwimResult;
import cnuphys.adaptiveSwim.AdaptiveSwimmer;
import cnuphys.adaptiveSwim.geometry.Cylinder;
import cnuphys.adaptiveSwim.geometry.Line;
import cnuphys.adaptiveSwim.geometry.Point;
import cnuphys.adaptiveSwim.geometry.Vector;
import java.util.ArrayList;
import java.util.List;
import org.jlab.geom.prim.Line3D;
/**
 *
 * @author ziegler
 */

public class Swim {

    private double _x0;
    private double _y0;
    private double _z0;
    private double _phi;
    private double _theta;
    private double _pTot;
    private final double _rMax = 5 + 3; // increase to allow swimming to outer
    // detectors
    private double _maxPathLength = 9;
    private boolean SwimUnPhys = false; //Flag to indicate if track is swimmable
    private int _charge;

    final double SWIMZMINMOM = 0.75; // GeV/c
    final double MINTRKMOM = 0.05; // GeV/c
    double accuracy = 20e-6; // 20 microns
    public double stepSize = 5.00 * 1.e-4; // 500 microns
    public double distanceBetweenSaves= 100*stepSize;
    
    private ProbeCollection PC;
    
    /**
     * Class for swimming to various surfaces.  The input and output units are cm and GeV/c
     */
    public Swim() {
        PC = Swimmer.getProbeCollection(Thread.currentThread());
        if (PC == null) {
            PC = new ProbeCollection();
            Swimmer.put(Thread.currentThread(), PC);
        }
    }

    /**
     *
     * @param direction
     *            +1 for out -1 for in
     * @param x0
     * @param y0
     * @param z0
     * @param thx
     * @param thy
     * @param p
     * @param charge
     */
    public void SetSwimParameters(int direction, double x0, double y0, double z0, double thx, double thy, double p,
                    int charge) {
        
        // x,y,z in m = swimmer units
        _x0 = x0 / 100;
        _y0 = y0 / 100;
        _z0 = z0 / 100;
        this.checkR(_x0, _y0, _z0);
        double pz = direction * p / Math.sqrt(thx * thx + thy * thy + 1);
        double px = thx * pz;
        double py = thy * pz;
        _phi = Math.toDegrees(FastMath.atan2(py, px));
        _pTot = Math.sqrt(px * px + py * py + pz * pz);
        _theta = Math.toDegrees(Math.acos(pz / _pTot));

        _charge = direction * charge;
    }

    /**
     * Sets the parameters used by swimmer based on the input track state vector
     * parameters swimming outwards
     *
     * @param superlayerIdx
     * @param layerIdx
     * @param x0
     * @param y0
     * @param thx
     * @param thy
     * @param p
     * @param charge
     */
    public void SetSwimParameters(int superlayerIdx, int layerIdx, double x0, double y0, double z0, double thx,
                    double thy, double p, int charge) {
        // z at a given DC plane in the tilted coordinate system
        // x,y,z in m = swimmer units
        _x0 = x0 / 100;
        _y0 = y0 / 100;
        _z0 = z0 / 100;
        this.checkR(_x0, _y0, _z0);
        double pz = p / Math.sqrt(thx * thx + thy * thy + 1);
        double px = thx * pz;
        double py = thy * pz;
        _phi = Math.toDegrees(FastMath.atan2(py, px));
        _pTot = Math.sqrt(px * px + py * py + pz * pz);
        _theta = Math.toDegrees(Math.acos(pz / _pTot));

        _charge = charge;

    }

    /**
     * Sets the parameters used by swimmer based on the input track parameters
     *
     * @param x0
     * @param y0
     * @param z0
     * @param px
     * @param py
     * @param pz
     * @param charge
     */
    public void SetSwimParameters(double x0, double y0, double z0, double px, double py, double pz, int charge) {
        _x0 = x0 / 100;
        _y0 = y0 / 100;
        _z0 = z0 / 100;
         this.checkR(_x0, _y0, _z0);
        _phi = Math.toDegrees(FastMath.atan2(py, px));
        _pTot = Math.sqrt(px * px + py * py + pz * pz);
        _theta = Math.toDegrees(Math.acos(pz / _pTot));

        _charge = charge;

    }

    /**
     * 
     * @param xcm
     * @param ycm
     * @param zcm
     * @param phiDeg
     * @param thetaDeg
     * @param p
     * @param charge
     * @param maxPathLength
     */
    public void SetSwimParameters(double xcm, double ycm, double zcm, double phiDeg, double thetaDeg, double p,
                    int charge, double maxPathLength) {

        _maxPathLength = maxPathLength;
        _charge = charge;
        _phi = phiDeg;
        _theta = thetaDeg;
        _pTot = p;
        _x0 = xcm / 100;
        _y0 = ycm / 100;
        _z0 = zcm / 100;
        this.checkR(_x0, _y0, _z0);
    }

    /**
     * 
     * @param xcm
     * @param ycm
     * @param zcm
     * @param phiDeg
     * @param thetaDeg
     * @param p
     * @param charge
     * @param maxPathLength
     * @param Accuracy
     * @param StepSize
     */
    public void SetSwimParameters(double xcm, double ycm, double zcm, double phiDeg, double thetaDeg, double p,
                    int charge, double maxPathLength, double Accuracy, double StepSize) {

        _maxPathLength = maxPathLength;
         accuracy = Accuracy/100;
         stepSize = StepSize/100;
        _charge = charge;
        _phi = phiDeg;
        _theta = thetaDeg;
        _pTot = p;
        _x0 = xcm / 100;
        _y0 = ycm / 100;
        _z0 = zcm / 100;
        this.checkR(_x0, _y0, _z0);
    }

    public double[] SwimToPlaneTiltSecSys(int sector, double z_cm) {
        double z = z_cm / 100; // the magfield method uses meters
        double[] value = new double[8];

        if (_pTot < MINTRKMOM || this.SwimUnPhys==true) // fiducial cut
        {
            return null;
        }

        // use a SwimZResult instead of a trajectory (dph)
        SwimZResult szr = null;

        SwimTrajectory traj = null;
        double hdata[] = new double[3];

        try {

            if (_pTot > SWIMZMINMOM) {

                // use the new z swimmer (dph)
                // NOTE THE DISTANCE, UNITS FOR swimZ are cm, NOT m like the old
                // swimmer (dph)

                double stepSizeCM = stepSize * 100; // convert to cm

                // create the starting SwimZ state vector
                SwimZStateVector start = new SwimZStateVector(_x0 * 100, _y0 * 100, _z0 * 100, _pTot, _theta, _phi);

                try {
                        szr = PC.RCF_z.sectorAdaptiveRK(sector, _charge, _pTot, start, z_cm, stepSizeCM, hdata);
                } catch (SwimZException e) {
                        szr = null;
                        //System.err.println("[WARNING] Tilted SwimZ Failed for p = " + _pTot);
                }
            }

            if (szr != null) {
                double bdl = szr.sectorGetBDL(sector, PC.RCF_z.getProbe());
                double pathLength = szr.getPathLength(); // already in cm

                SwimZStateVector last = szr.last();
                double p3[] = szr.getThreeMomentum(last);

                value[0] = last.x; // xf in cm
                value[1] = last.y; // yz in cm
                value[2] = last.z; // zf in cm
                value[3] = p3[0];
                value[4] = p3[1];
                value[5] = p3[2];
                value[6] = pathLength;
                value[7] = bdl / 10; // convert from kg*cm to T*cm
            } else { // use old swimmer. Either low momentum or SwimZ failed.
                                // (dph)

                traj = PC.RCF.sectorSwim(sector, _charge, _x0, _y0, _z0, _pTot, _theta, _phi, z, accuracy, _rMax,
                                _maxPathLength, stepSize, cnuphys.swim.Swimmer.CLAS_Tolerance, hdata);

                // traj.computeBDL(sector, rprob);
                if(traj==null)
                    return null;
                
                traj.sectorComputeBDL(sector, PC.RCP);
                // traj.computeBDL(rcompositeField);

                double lastY[] = traj.lastElement();
                value[0] = lastY[0] * 100; // convert back to cm
                value[1] = lastY[1] * 100; // convert back to cm
                value[2] = lastY[2] * 100; // convert back to cm
                value[3] = lastY[3] * _pTot;
                value[4] = lastY[4] * _pTot;
                value[5] = lastY[5] * _pTot;
                value[6] = lastY[6] * 100;
                value[7] = lastY[7] * 10;
            } // use old swimmer
        } catch (Exception e) {
                e.printStackTrace();
        }
        return value;

    }
    /**
     * 
     * @param z_cm
     * @return state  x,y,z,px,py,pz, pathlength, iBdl at the plane surface
     */
    public double[] SwimToPlaneLab(double z_cm) {
        double z = z_cm / 100; // the magfield method uses meters
        double[] value = new double[8];

        if (_pTot < MINTRKMOM || this.SwimUnPhys==true) // fiducial cut
        {
                return null;
        }
        SwimTrajectory traj = null;
        double hdata[] = new double[3];

        // use a SwimZResult instead of a trajectory (dph)
        SwimZResult szr = null;

        try {

            if (_pTot > SWIMZMINMOM) {

                // use the new z swimmer (dph)
                // NOTE THE DISTANCE, UNITS FOR swimZ are cm, NOT m like the old
                // swimmer (dph)

                double stepSizeCM = stepSize * 100; // convert to cm

                // create the starting SwimZ state vector
                SwimZStateVector start = new SwimZStateVector(_x0 * 100, _y0 * 100, _z0 * 100, _pTot, _theta, _phi);

                try {
                        szr = PC.CF_z.adaptiveRK(_charge, _pTot, start, z_cm, stepSizeCM, hdata);
                } catch (SwimZException e) {
                        szr = null;
                        //System.err.println("[WARNING] SwimZ Failed for p = " + _pTot);

                }
            }

            if (szr != null) {
                double bdl = szr.getBDL(PC.CF_z.getProbe());
                double pathLength = szr.getPathLength(); // already in cm

                SwimZStateVector last = szr.last();
                double p3[] = szr.getThreeMomentum(last);

                value[0] = last.x; // xf in cm
                value[1] = last.y; // yz in cm
                value[2] = last.z; // zf in cm
                value[3] = p3[0];
                value[4] = p3[1];
                value[5] = p3[2];
                value[6] = pathLength;
                value[7] = bdl / 10; // convert from kg*cm to T*cm
            } else { // use old swimmer. Either low momentum or SwimZ failed.
                                    // (dph)
                traj = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, z, accuracy, _rMax, _maxPathLength,
                                stepSize, cnuphys.swim.Swimmer.CLAS_Tolerance, hdata);
                if(traj==null)
                    return null;
                traj.computeBDL(PC.CP);
                // traj.computeBDL(compositeField);

                double lastY[] = traj.lastElement();

                value[0] = lastY[0] * 100; // convert back to cm
                value[1] = lastY[1] * 100; // convert back to cm
                value[2] = lastY[2] * 100; // convert back to cm
                value[3] = lastY[3] * _pTot;
                value[4] = lastY[4] * _pTot;
                value[5] = lastY[5] * _pTot;
                value[6] = lastY[6] * 100;
                value[7] = lastY[7] * 10;
            } // old swimmer

        } catch (RungeKuttaException e) {
                e.printStackTrace();
        }
        return value;

    }

    private void checkR(double _x0, double _y0, double _z0) {
        this.SwimUnPhys=false;
        if(Math.sqrt(_x0*_x0 + _y0*_y0)>this._rMax || 
                Math.sqrt(_x0*_x0 + _y0*_y0 + _z0*_z0)>this._maxPathLength)
            this.SwimUnPhys=true;
    }
    /**
     * Cylindrical stopper
     */
    private class CylindricalBoundarySwimStopper implements IStopper {

        private double _finalPathLength = Double.NaN;

        private double _Rad;
        //boolean cutOff = false;
        // check that the track can get to R.   Stops at the track radius    
        //float[] b = new float[3];
        //double x0 = _x0*100;
        //double y0 = _y0*100;
        //double z0 = _z0*100;

        double max = -1.0;
        /**
         * A swim stopper that will stop if the boundary of a plane is crossed
         *
         * @param maxR
         *            the max radial coordinate in meters.
         */
        private CylindricalBoundarySwimStopper(double Rad) {
            // DC reconstruction units are cm. Swim units are m. Hence scale by
            // 100
            _Rad = Rad;
            // check if the track will reach the surface of the cylinder.  
            //BfieldLab(x0, y0, z0, b);            
            //double trkR = _pTot*Math.sin(Math.toRadians(_theta))/Math.abs(b[2]*LIGHTVEL);
            //double trkXc = x0 + trkR * Math.sin(Math.toRadians(_phi));
            //if(trkR<(Rad+trkXc) && Math.sqrt(x0*x0+y0*y0)<_Rad) { // check only for swimming inside out.
            //    cutOff=true;
            //}
        }

        @Override
        public boolean stopIntegration(double t, double[] y) {
            
            double r = Math.sqrt(y[0] * y[0] + y[1] * y[1]) * 100.;
//            if(r>max ) 
//                max = r;
//            else System.out.println(r + " " + max + " " + t);
            //if(cutOff) {
                return (r < max || r > _Rad); // stop intergration at closest distance to the cylinder
            //}
            //else {
            //    return (r > _Rad);
            //}
        }

        /**
         * Get the final path length in meters
         *
         * @return the final path length in meters
         */
        @Override
        public double getFinalT() {
                return _finalPathLength;
        }

        /**
         * Set the final path length in meters
         *
         * @param finalPathLength
         *            the final path length in meters
         */
        @Override
        public void setFinalT(double finalPathLength) {
                _finalPathLength = finalPathLength;
        }
    }
    //private final double LIGHTVEL     = 0.000299792458 ;
    
    /**
     * 
     * @param Rad
     * @return state  x,y,z,px,py,pz, pathlength, iBdl at the surface 
     */
    public double[] SwimToCylinder(double Rad) {
        
        double[] value = new double[8];
        if(this.SwimUnPhys)
            return null;
        
        CylindricalBoundarySwimStopper stopper = new CylindricalBoundarySwimStopper(Rad);
        
        SwimTrajectory st = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, stopper, _maxPathLength, stepSize,
                        0.0005);
        if(st==null)
                return null;
        st.computeBDL(PC.CP);
        // st.computeBDL(compositeField);

        double[] lastY = st.lastElement();

        value[0] = lastY[0] * 100; // convert back to cm
        value[1] = lastY[1] * 100; // convert back to cm
        value[2] = lastY[2] * 100; // convert back to cm
        value[3] = lastY[3] * _pTot; // normalized values
        value[4] = lastY[4] * _pTot;
        value[5] = lastY[5] * _pTot;
        value[6] = lastY[6] * 100;
        value[7] = lastY[7] * 10; // Conversion from kG.m to T.cm

        return value;

    }

    /**
     * 
     * @param radius in cm
     * @return state  x,y,z,px,py,pz, pathlength, iBdl at the surface 
     */
    public double[] SwimRho(double radius)  {
        return SwimRho(radius, accuracy*100);
    }
    
    /**
     * 
     * @param radius   in cm
     * @param accuracy in cm 
     * @return state  x,y,z,px,py,pz, pathlength, iBdl at the surface 
     */
    public double[] SwimRho(double radius, double accuracy)  {

        double[] value = null;

        // using adaptive stepsize
        if(this.SwimUnPhys)
            return null;

        try {
        
            AdaptiveSwimResult result = new AdaptiveSwimResult(false);
            
            PC.CF.swimRho(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, radius/100, accuracy/100, _rMax, stepSize, cnuphys.swim.Swimmer.CLAS_Tolerance, result);

            if(result.getStatus()==0) {
                value = new double[8];   
                value[0] = result.getUf()[0] * 100; // convert back to cm
                value[1] = result.getUf()[1] * 100; // convert back to cm
                value[2] = result.getUf()[2] * 100; // convert back to cm
                value[3] = result.getUf()[3] * _pTot; // normalized values
                value[4] = result.getUf()[4] * _pTot;
                value[5] = result.getUf()[5] * _pTot;
                value[6] = result.getFinalS() * 100;
                value[7] = 0; // Conversion from kG.m to T.cm
            }
                    
        } catch (RungeKuttaException e) {
                System.out.println(_charge + " " + _x0 + " " + _y0 + " " + _z0 + " " + _pTot + " " + _theta + " " + _phi);
                e.printStackTrace();
        }
        return value;

    }
    
    /**
     * 
     * @param axisPoint1 in cm
     * @param axisPoint2 in cm 
     * @param radius in cm 
     * @return swam trajectory to the cylinder
     */
    public double[] SwimGenCylinder(Point3D axisPoint1, Point3D axisPoint2, double radius)  {
        return SwimGenCylinder(axisPoint1, axisPoint2, radius, accuracy*100);
    }
    
    /**
     * 
     * @param axisPoint1 in cm
     * @param axisPoint2 in cm 
     * @param radius in cm 
     * @param accuracy in cm
     * @return swam trajectory to the cylinder
     */
    public double[] SwimGenCylinder(Point3D axisPoint1, Point3D axisPoint2, double radius, double accuracy)  {

        double[] value = null;
        double[] p1 = new double[3];
        double[] p2 = new double[3];
        p1[0] = axisPoint1.x()/100;
        p1[1] = axisPoint1.y()/100;
        p1[2] = axisPoint1.z()/100;
        p2[0] = axisPoint2.x()/100;
        p2[1] = axisPoint2.y()/100;
        p2[2] = axisPoint2.z()/100;
        
        Cylinder targCyl = new Cylinder(p1, p2, radius/100);
        // using adaptive stepsize
        if(this.SwimUnPhys)
            return null;

        try {
        
            AdaptiveSwimResult result = new AdaptiveSwimResult(false);
            
            PC.CF.swimCylinder(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, 
                    p1, p2, radius/100, accuracy/100, _rMax, stepSize, cnuphys.swim.Swimmer.CLAS_Tolerance, result);
            
            if(result.getStatus()==0) {
                value = new double[8];            
                value[0] = result.getUf()[0] * 100; // convert back to cm
                value[1] = result.getUf()[1] * 100; // convert back to cm
                value[2] = result.getUf()[2] * 100; // convert back to cm
                value[3] = result.getUf()[3] * _pTot; // normalized values
                value[4] = result.getUf()[4] * _pTot;
                value[5] = result.getUf()[5] * _pTot;
                value[6] = result.getFinalS() * 100;
                value[7] = 0; // Conversion from kG.m to T.cm
            }
                    
        } catch (RungeKuttaException e) {
                System.out.println(_charge + " " + _x0 + " " + _y0 + " " + _z0 + " " + _pTot + " " + _theta + " " + _phi);
                e.printStackTrace();
        }
        return value;

    }

    public double[] SwimPlane(Vector3D n, Point3D p, double accuracy)  {

        double[] value = null;
        
        
        // using adaptive stepsize
        if(this.SwimUnPhys)
            return null;

        try {
        
            AdaptiveSwimResult result = new AdaptiveSwimResult(false);
            
            PC.CF.swimPlane(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, 
                            n.x(),n.y(),n.z(),p.x()/100,p.y()/100,p.z()/100, 
                            accuracy/100, _rMax, stepSize, cnuphys.swim.Swimmer.CLAS_Tolerance, result);
            

            if(result.getStatus()==0) {
                value = new double[8];   
                value[0] = result.getUf()[0] * 100; // convert back to cm
                value[1] = result.getUf()[1] * 100; // convert back to cm
                value[2] = result.getUf()[2] * 100; // convert back to cm
                value[3] = result.getUf()[3] * _pTot; // normalized values
                value[4] = result.getUf()[4] * _pTot;
                value[5] = result.getUf()[5] * _pTot;
                value[6] = result.getFinalS() * 100;
                value[7] = 0; // Conversion from kG.m to T.cm
            }
                    
        } catch (RungeKuttaException e) {
                System.out.println(_charge + " " + _x0 + " " + _y0 + " " + _z0 + " " + _pTot + " " + _theta + " " + _phi);
                e.printStackTrace();
        }
        return value;

    }
    
    
    private class SphericalBoundarySwimStopper implements IStopper {

        private double _finalPathLength = Double.NaN;

        private double _Rad;

        /**
         * A swim stopper that will stop if the boundary of a plane is crossed
         *
         * @param maxR
         *            the max radial coordinate in meters.
         */
        private SphericalBoundarySwimStopper(double Rad) {
                // DC reconstruction units are cm. Swim units are m. Hence scale by
                // 100
                _Rad = Rad;
        }

        @Override
        public boolean stopIntegration(double t, double[] y) {

                double r = Math.sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]) * 100.;

                return (r > _Rad);

        }

        /**
         * Get the final path length in meters
         *
         * @return the final path length in meters
         */
        @Override
        public double getFinalT() {
                return _finalPathLength;
        }

        /**
         * Set the final path length in meters
         *
         * @param finalPathLength
         *            the final path length in meters
         */
        @Override
        public void setFinalT(double finalPathLength) {
                _finalPathLength = finalPathLength;
        }
    }
    /**
     * 
     * @param Rad
     * @return state  x,y,z,px,py,pz, pathlength, iBdl at the surface 
     */
    public double[] SwimToSphere(double Rad) {

        double[] value = new double[8];
        // using adaptive stepsize
        if(this.SwimUnPhys==true)
            return null;
        SphericalBoundarySwimStopper stopper = new SphericalBoundarySwimStopper(Rad);
            
        SwimTrajectory st = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, stopper, _maxPathLength, stepSize,
                        0.0005);
        if(st==null)
            return null;
        st.computeBDL(PC.CP);
        // st.computeBDL(compositeField);

        double[] lastY = st.lastElement();

        value[0] = lastY[0] * 100; // convert back to cm
        value[1] = lastY[1] * 100; // convert back to cm
        value[2] = lastY[2] * 100; // convert back to cm
        value[3] = lastY[3] * _pTot; // normalized values
        value[4] = lastY[4] * _pTot;
        value[5] = lastY[5] * _pTot;
        value[6] = lastY[6] * 100;
        value[7] = lastY[7] * 10; // Conversion from kG.m to T.cm

        return value;

    }

    // added for swimming to outer detectors
    private class PlaneBoundarySwimStopper implements IStopper {

        private double _finalPathLength = Double.NaN;
        private double _d;
        private Vector3D _n;
        private double _dist2plane;
        private int _dir;

        /**
         * A swim stopper that will stop if the boundary of a plane is crossed
         *
         * @param maxR
         *            the max radial coordinate in meters.
         */
        private PlaneBoundarySwimStopper(double d, Vector3D n, int dir) {
                // DC reconstruction units are cm. Swim units are m. Hence scale by
                // 100
                _d = d;
                _n = n;
                _dir = dir;
        }

        @Override
        public boolean stopIntegration(double t, double[] y) {
            double dtrk = y[0] * _n.x() + y[1] * _n.y() + y[2] * _n.z();

            double accuracy = 20e-6; // 20 microns
            // System.out.println(" dist "+dtrk*100+ " state "+y[0]*100+",
            // "+y[1]*100+" , "+y[2]*100);
            if (_dir < 0) {
                    return dtrk < _d;
            } else {
                    return dtrk > _d;
            }

        }

        @Override
        public double getFinalT() {

                return _finalPathLength;
        }

        /**
         * Set the final path length in meters
         *
         * @param finalPathLength
         *            the final path length in meters
         */
        @Override
        public void setFinalT(double finalPathLength) {
                _finalPathLength = finalPathLength;
        }
    }
    /**
     * 
     * @param d_cm
     * @param n
     * @param dir
     * @return return state  x,y,z,px,py,pz, pathlength, iBdl at the plane surface in the lab frame
     */
    public double[] SwimToPlaneBoundary(double d_cm, Vector3D n, int dir) {

        double[] value = new double[8];
        if(this.SwimUnPhys)
            return null;
        double d = d_cm / 100;
        
        double hdata[] = new double[3];
        // using adaptive stepsize

        // the new swim to plane in swimmer
        Plane plane = new Plane(n.x(), n.y(), n.z(), d);
        SwimTrajectory st;
        try {

            st = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, plane, accuracy, _maxPathLength, stepSize,
                            cnuphys.swim.Swimmer.CLAS_Tolerance, hdata);

            st.computeBDL(PC.CP);

            double[] lastY = st.lastElement();
            
            value[0] = lastY[0] * 100; // convert back to cm
            value[1] = lastY[1] * 100; // convert back to cm
            value[2] = lastY[2] * 100; // convert back to cm
            value[3] = lastY[3] * _pTot; // normalized values
            value[4] = lastY[4] * _pTot;
            value[5] = lastY[5] * _pTot;
            value[6] = lastY[6] * 100;
            value[7] = lastY[7] * 10; // Conversion from kG.m to T.cm

            // System.out.println("\nCOMPARE plane swims DIRECTION = " +
            // dir);
            // for (int i = 0; i < 8; i++) {
            // System.out.print(String.format("%-8.5f ", value[i]));
            // }

         
        } catch (RungeKuttaException e) {
                e.printStackTrace();
        }
        return value;

    }

    
    
    private class BeamLineSwimStopper implements IStopper {

        private double _finalPathLength = Double.NaN;

        private double _xB;
        private double _yB;
        double min = Double.POSITIVE_INFINITY;
        double thetaRad = Math.toRadians(_theta);
        double phiRad = Math.toRadians(_phi);
        double pz = _pTot * Math.cos(thetaRad);
        private BeamLineSwimStopper(double xB, double yB) {
                // DC reconstruction units are cm. Swim units are m. Hence scale by
                // 100
                _xB = xB;
                _yB = yB;
        }

        @Override
        public boolean stopIntegration(double t, double[] y) {

                double r = Math.sqrt((_xB-y[0]* 100.) * (_xB-y[0]* 100.) + (_yB-y[1]* 100.) * (_yB-y[1]* 100.));
                if(r<min && y[2]<2.0) //start at about 2 meters before target.  Avoid inbending stopping when P dir changes
                    min = r;
                return (r > min );

        }

        /**
         * Get the final path length in meters
         *
         * @return the final path length in meters
         */
        @Override
        public double getFinalT() {
                return _finalPathLength;
        }

        /**
         * Set the final path length in meters
         *
         * @param finalPathLength
         *            the final path length in meters
         */
        @Override
        public void setFinalT(double finalPathLength) {
                _finalPathLength = finalPathLength;
        }
    }
    
    public double[] SwimToBeamLine(double xB, double yB) {

        double[] value = new double[8];
        
        if(this.SwimUnPhys==true)
            return null;
        BeamLineSwimStopper stopper = new BeamLineSwimStopper(xB, yB);

        SwimTrajectory st = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, stopper, _maxPathLength, stepSize,
                        0.0005);
        if(st==null)
            return null;
        st.computeBDL(PC.CP);
        // st.computeBDL(compositeField);

        double[] lastY = st.lastElement();

        value[0] = lastY[0] * 100; // convert back to cm
        value[1] = lastY[1] * 100; // convert back to cm
        value[2] = lastY[2] * 100; // convert back to cm
        value[3] = lastY[3] * _pTot; // normalized values
        value[4] = lastY[4] * _pTot;
        value[5] = lastY[5] * _pTot;
        value[6] = lastY[6] * 100;
        value[7] = lastY[7] * 10; // Conversion from kG.m to T.cm

        return value;

    }

    
    
    private void printV(String pfx, double v[]) {
            double x = v[0] / 100;
            double y = v[1] / 100;
            double z = v[2] / 100;
            double r = Math.sqrt(x * x + y * y + z * z);
            System.out.println(String.format("%s: (%-8.5f, %-8.5f, %-8.5f) R: %-8.5f", pfx, z, y, z, r));
    }

    /**
     * 
     * @param sector
     * @param x_cm
     * @param y_cm
     * @param z_cm
     * @param result B field components in T in the tilted sector system
     */
    public void Bfield(int sector, double x_cm, double y_cm, double z_cm, float[] result) {

        PC.RCP.field(sector, (float) x_cm, (float) y_cm, (float) z_cm, result);
        // rcompositeField.field((float) x_cm, (float) y_cm, (float) z_cm,
        // result);
        result[0] = result[0] / 10;
        result[1] = result[1] / 10;
        result[2] = result[2] / 10;

    }
    /**
     * 
     * @param x_cm
     * @param y_cm
     * @param z_cm
     * @param result B field components in T in the lab frame
     */
    public void BfieldLab(double x_cm, double y_cm, double z_cm, float[] result) {

        PC.CP.field((float) x_cm, (float) y_cm, (float) z_cm, result);
        result[0] = result[0] / 10;
        result[1] = result[1] / 10;
        result[2] = result[2] / 10;

    }

    
    
    public double[] AdaptiveSwimPlane(double px, double py, double pz, double nx, double ny, double nz, double accuracy)  {
//        System.out.println("Don't use yet");

        double[] value = new double[8];
        
        Vector norm = new Vector(nx,ny,nz);
        Point point = new Point(px/100,py/100,pz/100);
        
        cnuphys.adaptiveSwim.geometry.Plane targetPlane = new cnuphys.adaptiveSwim.geometry.Plane(norm, point);

        
        // using adaptive stepsize
        if(this.SwimUnPhys)
            return null;

        try {
        
            AdaptiveSwimResult result = new AdaptiveSwimResult(false);
            
            PC.AS.swimPlane(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, targetPlane,
                            accuracy/100, _rMax, stepSize, cnuphys.swim.Swimmer.getEps(), result);
            
            if(result.getStatus() == AdaptiveSwimmer.SWIM_SUCCESS) {
                value[0] = result.getUf()[0] * 100; // convert back to cm
                value[1] = result.getUf()[1] * 100; // convert back to cm
                value[2] = result.getUf()[2] * 100; // convert back to cm
                value[3] = result.getUf()[3] * _pTot; // normalized values
                value[4] = result.getUf()[4] * _pTot;
                value[5] = result.getUf()[5] * _pTot;
                value[6] = result.getFinalS() * 100;
                value[7] = 0; // Conversion from kG.m to T.cm
            }
            else {
                return null;
            }
                    
        } catch (AdaptiveSwimException e) {
                e.printStackTrace();
        }        
        return value;

    }
    
    
    public double[] AdaptiveSwimCylinder(double a1x, double a1y, double a1z, double a2x, double a2y, double a2z, double radius, double accuracy)  {
    //    System.out.println("Don't use yet");
        double[] value = new double[8];
        
        radius = radius/100;
        Point a1 = new Point(a1x/100, a1y/100, a1z/100);
        Point a2 = new Point(a2x/100, a2y/100, a2z/100);
        Line centerLine = new Line(a1, a2);
        
        cnuphys.adaptiveSwim.geometry.Cylinder targetCylinder = new cnuphys.adaptiveSwim.geometry.Cylinder(centerLine, radius);

        
        // using adaptive stepsize
        if(this.SwimUnPhys)
            return null;

        try {
        
            AdaptiveSwimResult result = new AdaptiveSwimResult(false);
            
            PC.AS.swimCylinder(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, targetCylinder,
                            accuracy/100, _rMax, stepSize, cnuphys.swim.Swimmer.getEps(), result);

            if(result.getStatus() == AdaptiveSwimmer.SWIM_SUCCESS) {
                value[0] = result.getUf()[0] * 100; // convert back to cm
                value[1] = result.getUf()[1] * 100; // convert back to cm
                value[2] = result.getUf()[2] * 100; // convert back to cm
                value[3] = result.getUf()[3] * _pTot; // normalized values
                value[4] = result.getUf()[4] * _pTot;
                value[5] = result.getUf()[5] * _pTot;
                value[6] = result.getFinalS() * 100;
                value[7] = 0; // Conversion from kG.m to T.cm
            }
            else {
                return null;
            }
                    
        } catch (AdaptiveSwimException e) {
                e.printStackTrace();
        }        
        return value;

    }

    public double[] AdaptiveSwimRho(double radius, double accuracy)  {
        System.out.println("Don't use yet");

        double[] value = new double[8];

        radius = radius/100;
        // using adaptive stepsize
        if(this.SwimUnPhys)
            return null;

        try {
        
            AdaptiveSwimResult result = new AdaptiveSwimResult(false);
            
            PC.AS.swimRho(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, radius, 
                          accuracy/100, _rMax, stepSize, cnuphys.swim.Swimmer.getEps(), result);

            if(result.getStatus() == AdaptiveSwimmer.SWIM_SUCCESS) {
                value[0] = result.getUf()[0] * 100; // convert back to cm
                value[1] = result.getUf()[1] * 100; // convert back to cm
                value[2] = result.getUf()[2] * 100; // convert back to cm
                value[3] = result.getUf()[3] * _pTot; // normalized values
                value[4] = result.getUf()[4] * _pTot;
                value[5] = result.getUf()[5] * _pTot;
                value[6] = result.getFinalS() * 100;
                value[7] = 0; // Conversion from kG.m to T.cm
            }
            else {
                return null;
            }
                    
        } catch (AdaptiveSwimException e) {
                e.printStackTrace();
        }
        return value;

    }

/**
     * 
     * @param Z
     * @return state  x,y,z,px,py,pz, pathlength, iBdl at the surface 
     */
    public double[] SwimToZ(double Z, int dir) {
        
        double[] value = new double[8];
        //if(this.SwimUnPhys)
        //    return null;
        
        ZSwimStopper stopper = new ZSwimStopper(Z, dir);
        
        SwimTrajectory st = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, stopper, _maxPathLength, stepSize,
                        distanceBetweenSaves);
        if(st==null)
                return null;
        st.computeBDL(PC.CP);
        // st.computeBDL(compositeField);
        this.setSwimTraj(st);
        double[] lastY = st.lastElement();

        value[0] = lastY[0] * 100; // convert back to cm
        value[1] = lastY[1] * 100; // convert back to cm
        value[2] = lastY[2] * 100; // convert back to cm
        value[3] = lastY[3] * _pTot; // normalized values
        value[4] = lastY[4] * _pTot;
        value[5] = lastY[5] * _pTot;
        value[6] = lastY[6] * 100;
        value[7] = lastY[7] * 10; // Conversion from kG.m to T.cm

        return value;

    }

    private SwimTrajectory swimTraj; 
    
    private class ZSwimStopper implements IStopper {

        private double _finalPathLength = Double.NaN;

        private double _Z;
        private int _dir;
        
        private ZSwimStopper(double Z, int dir) {
            // The reconstruction units are cm. Swim units are m. Hence scale by
            // 100
            _Z = Z;
           _dir = dir;
        }

        @Override
        public boolean stopIntegration(double t, double[] y) {
            
            double z = y[2] * 100.;
            if(_dir>0) {
                return (z > _Z);
            } else {
                return (z<_Z);
            }
        }

        /**
         * Get the final path length in meters
         *
         * @return the final path length in meters
         */
        @Override
        public double getFinalT() {
                return _finalPathLength;
        }

        /**
         * Set the final path length in meters
         *
         * @param finalPathLength
         *            the final path length in meters
         */
        @Override
        public void setFinalT(double finalPathLength) {
                _finalPathLength = finalPathLength;
        }
    }
    /**
     * @return the swimTraj
     */
    public SwimTrajectory getSwimTraj() {
        return swimTraj;
    }

    /**
     * @param swimTraj the swimTraj to set
     */
    public void setSwimTraj(SwimTrajectory swimTraj) {
        this.swimTraj = swimTraj;
    }
    
    private  class DCASwimStopper implements IStopper {

        public DCASwimStopper(SwimTrajectory swimTraj) { 
            _swimTraj = swimTraj;
            for(int i = 0; i < _swimTraj.size()-1; i++) { 
                polylines.add(new Line3D(_swimTraj.get(i)[0],_swimTraj.get(i)[1],_swimTraj.get(i)[2],
                        _swimTraj.get(i+1)[0],_swimTraj.get(i+1)[1],_swimTraj.get(i+1)[2]));
                
            }
        }

        private List<Line3D> polylines = new ArrayList<>();
        private SwimTrajectory _swimTraj;
        private double _finalPathLength = Double.NaN;
        private double _doca = Double.POSITIVE_INFINITY;
        
        @Override
        public boolean stopIntegration(double t, double[] y) {
           
            Point3D dcaCand = new Point3D(y[0],y[1],y[2]); 
            double maxDoca = Double.POSITIVE_INFINITY;
            
            for(Line3D l : polylines) { 
                if(l.distance(dcaCand).length()<maxDoca) {
                    maxDoca=l.distance(dcaCand).length();
                } 
            }
            if(maxDoca<_doca) {
                _doca = maxDoca; 
                return false;
            }
            return true;
            
        }

        /**
         * Get the final path length in meters
         *
         * @return the final path length in meters
         */
        @Override
        public double getFinalT() {
                return _finalPathLength;
        }

        /**
         * Set the final path length in meters
         *
         * @param finalPathLength
         *            the final path length in meters
         */
        @Override
        public void setFinalT(double finalPathLength) {
                _finalPathLength = finalPathLength;
        }
    
    }
    public double[] SwimToDCA(SwimTrajectory trk2) { //use for both traj to get doca for each track
        
         double[] value = new double[6];
        //if(this.SwimUnPhys)
        //    return null;
        
        DCASwimStopper stopper = new DCASwimStopper(trk2);
        
        SwimTrajectory st = PC.CF.swim(_charge, _x0, _y0, _z0, _pTot, _theta, _phi, stopper, _maxPathLength, stepSize,
                        0.0005);
        if(st==null)
                return null;
       
        double[] lastY = st.lastElement();

        value[0] = lastY[0] * 100; // convert back to cm
        value[1] = lastY[1] * 100; // convert back to cm
        value[2] = lastY[2] * 100; // convert back to cm
        value[3] = lastY[3] * _pTot; // normalized values
        value[4] = lastY[4] * _pTot;
        value[5] = lastY[5] * _pTot;
        

        return value;
        
        
        
    }
}