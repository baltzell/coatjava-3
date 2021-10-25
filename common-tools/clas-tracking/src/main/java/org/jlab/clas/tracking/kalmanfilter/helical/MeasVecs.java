package org.jlab.clas.tracking.kalmanfilter.helical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jlab.clas.swimtools.Swim;
import org.jlab.clas.tracking.kalmanfilter.Surface;
import org.jlab.clas.tracking.kalmanfilter.Type;
import org.jlab.clas.tracking.kalmanfilter.helical.StateVecs.StateVec;
import org.jlab.clas.tracking.objects.Strip;
import org.jlab.geom.prim.Arc3D;
import org.jlab.geom.prim.Cylindrical3D;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Transformation3D;
import org.jlab.geom.prim.Vector3D;

/**
 *
 * @author ziegler
 */

public class MeasVecs {
   
    public List<MeasVec> measurements = new ArrayList<MeasVec>();

    public void setMeasVecs(List<Surface> measSurfaces) {
        Collections.sort(measSurfaces);
        for(int i = 0; i < measSurfaces.size(); i++) {
            MeasVec mvec = new MeasVec();
            mvec.k = i ;
            mvec.layer = measSurfaces.get(i).getLayer();
            mvec.sector = measSurfaces.get(i).getSector();
            mvec.surface = measSurfaces.get(i);
            if(mvec.surface.getError()!=0)
                mvec.error = mvec.surface.getError();
            mvec.l_over_X0 = mvec.surface.getl_over_X0(); 
            mvec.skip = mvec.surface.notUsedInFit;
            mvec.hemisphere = measSurfaces.get(i).hemisphere;
            measurements.add(mvec);
        }
    }
    
    
    public double dh(int k, StateVec stateVec) {
        
        double value = Double.NaN;
        
        if (stateVec == null|| this.measurements.get(stateVec.k) == null) {
            return value;
        }
        
        if( this.measurements.get(stateVec.k).surface.type == Type.PLANEWITHPOINT || 
                this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHPOINT) {
            Point3D p = new Point3D(this.measurements.get(stateVec.k).surface.refPoint);
            value = p.distance(stateVec.x, stateVec.y, stateVec.z);
        }
        if( this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHARC) {
            double phia = this.measurements.get(stateVec.k).surface.arc.theta();
            value = Math.atan2(stateVec.y, stateVec.x)-phia;
            System.err.println("ARC MEAS. NOT FULLY IMPLEMENTED!!!!!!");
        }
        if( this.measurements.get(stateVec.k).surface.type == Type.PLANEWITHLINE || 
                this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHLINE) {
            Line3D l = new Line3D(this.measurements.get(stateVec.k).surface.lineEndPoint1, 
            this.measurements.get(stateVec.k).surface.lineEndPoint2);
            //value = l.distance(new Point3D(stateVec.x, stateVec.y, stateVec.z)).length();
            Line3D WL = new Line3D();
            WL.copy(l);
            WL.copy(WL.distance(new Point3D(stateVec.x, stateVec.y, stateVec.z)));
            
            value = WL.length();
        }
        if( this.measurements.get(stateVec.k).surface.type == Type.PLANEWITHSTRIP || 
                this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHSTRIP) { 
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.XYZ) {
                Line3D l = new Line3D(this.measurements.get(stateVec.k).surface.lineEndPoint1, 
                this.measurements.get(stateVec.k).surface.lineEndPoint2);
                //value = l.distance(new Point3D(stateVec.x, stateVec.y, stateVec.z)).length(); 
                if(l.direction().z()<0) {
                    l.setEnd(this.measurements.get(stateVec.k).surface.lineEndPoint1);
                    l.setOrigin(this.measurements.get(stateVec.k).surface.lineEndPoint2);
                }
                Line3D WL = new Line3D();
                WL.copy(l);
                Point3D svP = new Point3D(stateVec.x, stateVec.y, stateVec.z);
                WL.copy(WL.distance(svP));
                double sideStrip = -Math.signum(l.direction().cross(WL.direction()).
                        dot(this.measurements.get(stateVec.k).surface.plane.normal()));
                //double sideStrip = Math.signum(l.direction().y()*WL.direction().x()+l.direction().x()*WL.direction().y());
                value = WL.length()*sideStrip; 
            }
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.Z) { 
                Transformation3D toLocal =this.measurements.get(stateVec.k).surface.toLocal();
                Point3D stV = new Point3D(stateVec.x, stateVec.y, stateVec.z); 
                toLocal.apply(stV);
                value = stV.z()-this.measurements.get(stateVec.k).surface.strip.getZ();
            }
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.ARC) {
                Transformation3D toLocal =this.measurements.get(stateVec.k).surface.toLocal();
                Arc3D arc = new Arc3D();
                arc.copy(this.measurements.get(stateVec.k).surface.strip.getArc()); 
                toLocal.apply(arc);
                Point3D stV = new Point3D(stateVec.x, stateVec.y, stateVec.z); 
                toLocal.apply(stV);
                value = stV.z()-arc.center().z(); 
            }
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.PHI) {
                Transformation3D toLocal =this.measurements.get(stateVec.k).surface.toLocal();
                Point3D sv = new Point3D(stateVec.x, stateVec.y, stateVec.z);
                toLocal.apply(sv);
                value = sv.toVector3D().phi()-this.measurements.get(stateVec.k).surface.strip.getPhi();
                if(Math.abs(value)>2*Math.PI) value-=Math.signum(value)*2*Math.PI;
            }
        }
        return value;
    }
    
    
    public double getPhiATZ(StateVec stateVec) {
        Cylindrical3D cyl = this.measurements.get(stateVec.k).surface.cylinder;
        Line3D cln = this.measurements.get(stateVec.k).surface.cylinder.getAxis();
        double v = (stateVec.z-cyl.baseArc().center().z())/cln.direction().z();
        double x = cyl.baseArc().center().x()+v*cln.direction().x();
        double y = cyl.baseArc().center().y()+v*cln.direction().y();
        Vector3D n = new Point3D(x, y, stateVec.z).
                vectorTo(new Point3D(stateVec.x,stateVec.y,stateVec.z)).asUnit();
        return n.phi();
    }
    
    public double getPhi(StateVec stateVec) {
        Point3D sv = new Point3D(stateVec.x, stateVec.y, stateVec.z);
        this.measurements.get(stateVec.k).surface.toLocal().apply(sv);
        return sv.toVector3D().phi();
    }
      
    public double h(int k, StateVec stateVec) {
        
        double value = Double.NaN;
        
        if (stateVec == null|| this.measurements.get(stateVec.k) == null) {
            return value;
        }
        
        if( this.measurements.get(stateVec.k).surface.type == Type.PLANEWITHPOINT || 
                this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHPOINT) {
            Point3D p = new Point3D(this.measurements.get(stateVec.k).surface.refPoint);
            value = p.distance(stateVec.x, stateVec.y, stateVec.z);
        }
        if( this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHARC) {
            double phia = this.measurements.get(stateVec.k).surface.arc.theta();
            value = value = Math.atan2(stateVec.y, stateVec.x);
            System.err.println("ARC MEAS. NOT FULLY IMPLEMENTED!!!!!!");
        }
        if( this.measurements.get(stateVec.k).surface.type == Type.PLANEWITHLINE || 
                this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHLINE) {
            Line3D l = new Line3D(this.measurements.get(stateVec.k).surface.lineEndPoint1, 
            this.measurements.get(stateVec.k).surface.lineEndPoint2);
            value = l.distance(new Point3D(stateVec.x, stateVec.y, stateVec.z)).length();
        }
        if( this.measurements.get(stateVec.k).surface.type == Type.PLANEWITHSTRIP || 
                this.measurements.get(stateVec.k).surface.type == Type.CYLINDERWITHSTRIP) { 
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.XYZ) {
                Line3D l = new Line3D(this.measurements.get(stateVec.k).surface.lineEndPoint1, 
                this.measurements.get(stateVec.k).surface.lineEndPoint2);
                //value = l.distance(new Point3D(stateVec.x, stateVec.y, stateVec.z)).length();
                if(l.direction().z()<0) {
                    l.setEnd(this.measurements.get(stateVec.k).surface.lineEndPoint1);
                    l.setOrigin(this.measurements.get(stateVec.k).surface.lineEndPoint2);
                }
                Line3D WL = new Line3D();
                WL.copy(l);
                Point3D svP = new Point3D(stateVec.x, stateVec.y, stateVec.z);
                WL.copy(WL.distance(svP));
                double sideStrip = -Math.signum(l.direction().cross(WL.direction()).
                        dot(this.measurements.get(stateVec.k).surface.plane.normal())); 
                //double sideStrip = Math.signum(l.direction().y()*WL.direction().x()+l.direction().x()*WL.direction().y());
                value = WL.length()*sideStrip;
                
            }
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.Z) {
               value = stateVec.z;
            }
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.ARC) {
                Transformation3D toLocal =this.measurements.get(stateVec.k).surface.toLocal();
                Point3D stV = new Point3D(stateVec.x, stateVec.y, stateVec.z); 
                toLocal.apply(stV);
                value = stV.z(); 
            }
            if(this.measurements.get(stateVec.k).surface.strip.type == Strip.Type.PHI) {
               //value = Math.atan2(stateVec.y, stateVec.x);
               value = this.getPhi(stateVec);
            }
        }
        return value;
    }

    private double[] delta_d_a = new double[5];//{1, Math.toRadians(0.25),  0.05, 1, 0.01};
    double sqrt_epsilon = Math.sqrt(2.2*1.e-16); //RDV, why so small?
    private double[] Hval = new double[5];
    public double[] H(StateVecs.StateVec stateVec, StateVecs sv, MeasVec mv, Swim swimmer, int dir) {
        StateVecs.StateVec SVplus = null;// = new StateVec(stateVec.k);
        StateVecs.StateVec SVminus = null;// = new StateVec(stateVec.k);
        delta_d_a[0]=2*sqrt_epsilon*(stateVec.d_rho+1);
        delta_d_a[1]=2*sqrt_epsilon*(stateVec.phi0+1);
        delta_d_a[2]=2*sqrt_epsilon*(stateVec.kappa+1);
        delta_d_a[3]=2*sqrt_epsilon*(stateVec.dz+1);
        delta_d_a[4]=2*sqrt_epsilon*(stateVec.tanL+1);
        
        for(int i = 0; i < getHval().length; i++)
            getHval()[i] = 0;
        for(int i = 0; i < getDelta_d_a().length; i++) {
            SVplus = this.reset(SVplus, stateVec, sv);
            SVminus = this.reset(SVminus, stateVec, sv);
            if(i ==0) {
                SVplus.d_rho = stateVec.d_rho + getDelta_d_a()[i] / 2.;
                SVminus.d_rho = stateVec.d_rho - getDelta_d_a()[i] / 2.;
            }
            if(i ==1) {
                SVplus.phi0 = stateVec.phi0 + getDelta_d_a()[i] / 2.;
                SVminus.phi0 = stateVec.phi0 - getDelta_d_a()[i] / 2.;
            }
            if(i ==2) {
                SVplus.kappa = stateVec.kappa + getDelta_d_a()[i] / 2.;
                SVminus.kappa = stateVec.kappa - getDelta_d_a()[i] / 2.;
            }
            if(i ==3) {
                SVplus.dz = stateVec.dz + getDelta_d_a()[i] / 2.;
                SVminus.dz = stateVec.dz - getDelta_d_a()[i] / 2.;
            }
            if(i ==4) {
                SVplus.tanL = stateVec.tanL + getDelta_d_a()[i] / 2.;
                SVminus.tanL = stateVec.tanL - getDelta_d_a()[i] / 2.;
            }
            SVplus = sv.newStateVecAtMeasSite(stateVec.k, SVplus, mv, swimmer, false);
            SVminus = sv.newStateVecAtMeasSite(stateVec.k, SVminus, mv, swimmer, false);
            Hval[i] = (this.h(stateVec.k, SVplus) - this.h(stateVec.k, SVminus)) / getDelta_d_a()[i] ;
        }
       
        return getHval();
    }

    private StateVecs.StateVec reset(StateVecs.StateVec SVplus, StateVecs.StateVec stateVec, StateVecs sv) {
        SVplus = sv.new StateVec(stateVec.k);
        SVplus.d_rho = stateVec.d_rho;
        SVplus.alpha = stateVec.alpha;
        SVplus.phi0 = stateVec.phi0;
        SVplus.kappa = stateVec.kappa;
        SVplus.dz = stateVec.dz;
        SVplus.tanL = stateVec.tanL;
        SVplus.alpha = stateVec.alpha;
        
        return SVplus;
    }
     
    public class MeasVec implements Comparable<MeasVec> {
        public Surface surface;
        public int layer    = -1;
        public int sector   = -1;
        public double error = 1;
        public int k        = -1;
        public boolean skip = false;
        // this is for multiple scattering estimates in track 
        public double l_over_X0;
        //this is for energy loss
        public double Z_over_A_times_l;
        public double hemisphere = 1;


        @Override
        public int compareTo(MeasVec arg) {
            int CompLay = this.layer < arg.layer ? -1 : this.layer == arg.layer ? 0 : 1;
            return CompLay;
        }
    }

    /**
     * @return the delta_d_a
     */
    public double[] getDelta_d_a() {
        return delta_d_a;
    }

    /**
     * @return the Hval
     */
    public double[] getHval() {
        return Hval;
    }

    /**
     * @param Hval the Hval to set
     */
    public void setHval(double[] Hval) {
        this.Hval = Hval;
    }
    
}
