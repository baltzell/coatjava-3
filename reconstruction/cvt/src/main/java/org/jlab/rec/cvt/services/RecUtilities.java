package org.jlab.rec.cvt.services;

import java.util.ArrayList;
import java.util.List;
import org.jlab.clas.pdg.PDGDatabase;
import java.util.Map;
import java.util.HashMap;
import org.jlab.rec.cvt.trajectory.Helix;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.rec.cvt.Constants;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.hit.Hit;
import org.jlab.rec.cvt.track.Seed;
import org.jlab.rec.cvt.track.Track;
import org.jlab.clas.swimtools.Swim;
import org.jlab.rec.cvt.bmt.BMTType;
import org.jlab.rec.cvt.fit.CosmicFitter;
import org.jlab.rec.cvt.track.StraightTrack;
import org.jlab.rec.cvt.track.StraightTrackSeeder;
import org.jlab.rec.cvt.track.StraightTrackCandListFinder;
import org.jlab.rec.cvt.track.TrackSeeder;
//import org.jlab.rec.cvt.track.TrackSeederCA;
import org.jlab.rec.cvt.track.TrackSeederSVTLinker;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import org.jlab.clas.tracking.kalmanfilter.AKFitter;
import org.jlab.clas.tracking.kalmanfilter.Surface;
import org.jlab.clas.tracking.kalmanfilter.Units;
import org.jlab.clas.tracking.kalmanfilter.helical.KFitter;
import org.jlab.detector.base.DetectorType;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.cvt.Geometry;
import org.jlab.rec.cvt.bmt.BMTGeometry;
import org.jlab.rec.cvt.cross.CrossMaker;
import org.jlab.rec.cvt.fit.CircleFitPars;
import org.jlab.rec.cvt.fit.CircleFitter;
import org.jlab.rec.cvt.measurement.Measurements;
import org.jlab.rec.cvt.svt.SVTGeometry;
import org.jlab.rec.cvt.svt.SVTParameters;
import org.jlab.rec.cvt.trajectory.Ray;
/**
 * Service to return reconstructed TRACKS
 * format
 *
 * @author ziegler
 *
 */
public class RecUtilities {

     public void CleanupSpuriousSVTCrosses(List<Cross> crosses, List<Track> trks) {
        List<Cross> rmCrosses = new ArrayList<>();
        
        for(Cross c : crosses) {
            if(c.getId()>0 && !Geometry.getInstance().getSVT().isInFiducial(c.getCluster1().getLayer(), c.getSector(), c.getPoint()))
                rmCrosses.add(c);
        }
       
        
        for(int j = 0; j<crosses.size(); j++) {
            for(Cross c : rmCrosses) {
                if(crosses.get(j).getId()==c.getId())
                    crosses.remove(j);
            }
        } 
        
       
//        if(trks!=null && rmCrosses!=null) {
//            List<Cross> rmFromTrk = new ArrayList<>();
//            for(Track t:trks) {
//                //boolean rmFlag=false;
//                for(Cross c: rmCrosses) {
//                    if(c!=null && t!=null && c.getAssociatedTrackID()==t.getId())
//                        rmFromTrk.add(c);
//                }
//                t.removeAll(rmFromTrk);
//                //if(rmFlag==true)
//                //    rmFromTrk.add(t);
//            }
//            // RDV why removing the whole track?
//           // trks.removeAll(rmFromTrk);
//        }
    }
    
    
    public void CleanupSpuriousCrosses(List<ArrayList<Cross>> crosses, List<Track> trks) {
        List<Cross> rmCrosses = new ArrayList<>();
        
        for(Cross c : crosses.get(0)) {
            if(c.getId()>0 && !Geometry.getInstance().getSVT().isInFiducial(c.getCluster1().getLayer(), c.getSector(), c.getPoint()))
                rmCrosses.add(c);
        }
       
        
        for(int j = 0; j<crosses.get(0).size(); j++) {
            for(Cross c : rmCrosses) {
                if(crosses.get(0).get(j).getId()==c.getId())
                    crosses.get(0).remove(j);
            }
        } 
        
       
        if(trks!=null && rmCrosses!=null) {
            List<Track> rmTrks = new ArrayList<>();
            for(Track t:trks) {
                boolean rmFlag=false;
                for(Cross c: rmCrosses) {
                    if(c!=null && t!=null && c.getAssociatedTrackID()==t.getId())
                        rmFlag=true;
                }
                if(rmFlag==true)
                    rmTrks.add(t);
            }
            // RDV why removing the whole track?
            trks.removeAll(rmTrks);
        }
    }
    
    public List<Cluster> findClustersOnTrack(List<Cluster> allClusters, StraightTrack trkcand) {
        List<Cluster> clustersOnTrack = new ArrayList<>();
        Map<Integer, Cluster> clusterMap = new HashMap<>();
        trkcand.sort(Comparator.comparing(Cross::getY).reversed());
        List<Cluster> clsList = new ArrayList<>();
        for (int i = 0; i < trkcand.size(); i++) { //SVT cluster sorting
            if(trkcand.get(i).getDetector()==DetectorType.BST) {
                int sector   = trkcand.get(i).getCluster1().getSector();
                int layertop = 0;
                if(trkcand.get(i).getCluster1()!=null) {
                    layertop = trkcand.get(i).getCluster1().getLayer();
                } else {
                    layertop = trkcand.get(i).getCluster2().getLayer()-1;
                }
                int layerbot = 0;
                if(trkcand.get(i).getCluster2()!=null) {
                    layerbot = trkcand.get(i).getCluster2().getLayer();
                } else {
                    layerbot = trkcand.get(i).getCluster1().getLayer()+1;
                }
                Ray ray = trkcand.getRay();
                Point3D top    = new Point3D();
                Point3D bottom = new Point3D();
                Geometry.getInstance().getSVT().getPlane(layertop, sector).intersection(ray.toLine(), top);
                Geometry.getInstance().getSVT().getPlane(layerbot, sector).intersection(ray.toLine(), bottom);
                
                if(top.y()>bottom.y()) {
                    if(trkcand.get(i).getCluster1()!=null) clsList.add(trkcand.get(i).getCluster1());
                    if(trkcand.get(i).getCluster2()!=null) clsList.add(trkcand.get(i).getCluster2());
                } else {
                    if(trkcand.get(i).getCluster2()!=null) clsList.add(trkcand.get(i).getCluster2());
                    if(trkcand.get(i).getCluster1()!=null) clsList.add(trkcand.get(i).getCluster1());
                }
            }
        } 
        
        
        for(Cluster cluster : clsList) {
            if(cluster.size()<=Constants.getInstance().getSvtmaxclussize()) 
                clusterMap.put(SVTGeometry.getModuleId(cluster.getLayer(), cluster.getSector()), cluster);
        }  
        
        // for each layer
        for (int ilayer = 0; ilayer < SVTGeometry.NLAYERS; ilayer++) {
            int layer = ilayer + 1;
            
            for(int isector=0; isector<SVTGeometry.NSECTORS[ilayer]; isector++) {
                int sector = isector+1;
                
                Ray ray = trkcand.getRay();
                Point3D traj = new Point3D();
                Geometry.getInstance().getSVT().getPlane(layer, sector).intersection(ray.toLine(), traj);
                
                int key = SVTGeometry.getModuleId(layer, sector);
                
                if(traj!=null && Geometry.getInstance().getSVT().isInFiducial(layer, sector, traj)) {
                    double  doca    = Double.POSITIVE_INFINITY;
                    // loop over all clusters in the same sector and layer that are not associated to s track
                    for(Cluster cls : allClusters) {
                        if(cls.getAssociatedTrackID()==-1 && cls.getSector()==sector && cls.getLayer()==layer) {
                            double clsDoca = cls.residual(traj);
                            cls.setTrakInters(traj);
                            // save the ones that have better doca
                            if(Math.abs(clsDoca)<Math.abs(doca) && Math.abs(clsDoca)<10*cls.size()*cls.getSeedStrip().getPitch()/Math.sqrt(12)) {
                                if(clusterMap.containsKey(key) && clusterMap.get(key).getAssociatedTrackID()==-1) {
                                    clusterMap.replace(key, cls);
                                } else {
                                    if(!clusterMap.containsKey(key)) {
                                        clusterMap.put(key, cls); 
                                    }
                                }
                                doca = clsDoca;
                            }                           
                        }
                    }
                }
            }
        }
        // if any lost cluster with doca better than the seed is found, save it
        
        for(Entry<Integer,Cluster> entry : clusterMap.entrySet()) {
            if(entry.getValue().getAssociatedTrackID()==-1) clustersOnTrack.add(entry.getValue());
        }
        return clustersOnTrack;
        
    }
    
    public List<Cluster> findClustersOnTrk(List<Cluster> allClusters, 
            List<Cluster> seedCluster, Track trk, Swim swimmer) { 
        Map<Integer, AKFitter.HitOnTrack> trj = trk.getKFTrajectories();
        //make a map to get the traj at the layers
        Map<Integer, AKFitter.HitOnTrack> trj2 = new HashMap<>();
        for (Map.Entry<Integer, AKFitter.HitOnTrack> entry : trj.entrySet()) {
            if(entry.getKey()>3 && entry.getKey()<10) {
                if(entry.getValue().layer!=0) {
                    trj2.put(entry.getValue().layer, entry.getValue());
                   
                }
            }
        }
        // load SVT clusters that are in the seed
        Map<Integer,Cluster> clusterMap = new HashMap<>();
        for(Cluster cluster : seedCluster) {
            if(cluster.getDetector() == DetectorType.BMT)
                continue;
            if(cluster.size()<=Constants.getInstance().getSvtmaxclussize()) 
                clusterMap.put(SVTGeometry.getModuleId(cluster.getLayer(), cluster.getSector()), cluster);
        }   
        
        // for each layer
        for (int ilayer = 0; ilayer < SVTGeometry.NLAYERS; ilayer++) {
            int layer = ilayer + 1;
            
            for(int isector=0; isector<SVTGeometry.NSECTORS[ilayer]; isector++) {
                int sector = isector+1;
                
                // calculate trajectory
                Point3D trajPoint = null;
                
                
                if(trj2.containsKey(layer))  {
                    if(trj2.get(layer).sector == sector) {
                        trajPoint = new Point3D(trj2.get(layer).x,trj2.get(layer).y,trj2.get(layer).z);
                    }
                }
                
                int key = SVTGeometry.getModuleId(layer, sector);
                
                
                 
                // if trajectory is valid, look for missing clusters
                if(trajPoint!=null && Geometry.getInstance().getSVT().isInFiducial(layer, sector, trajPoint)) {
                    double  doca    = Double.POSITIVE_INFINITY; 
                    //if(clusterMap.containsKey(key)) {
                    //    Cluster cluster = clusterMap.get(key);
                    //    doca = cluster.residual(trajPoint); 
                    //}
                    // loop over all clusters in the same sector and layer that are noy associated to s track
                    for(Cluster cls : allClusters) { 
                        if(cls.getAssociatedTrackID()==-1 && cls.getSector()==sector && cls.getLayer()==layer) { 
                            double clsDoca = cls.residual(trajPoint); 
                            double Nsig = SVTParameters.TOCLUSN;
                            if(trk.getNDF()<3) Nsig*=3;
                            // save the ones that have better doca
                            if(Constants.getInstance().seedingDebugMode) {
                                System.out.println("Check Matching to cluster "+cls.toString()+
                                        " doca "+Math.abs(clsDoca)+"?<"+(Nsig*cls.getSeedStrip().getPitch()/Math.sqrt(12)));
                            }
                            if(Math.abs(clsDoca)<Math.abs(doca) && Math.abs(clsDoca)<Nsig*cls.getSeedStrip().getPitch()/Math.sqrt(12)) {
                                cls.trajPoint=trajPoint;
                                if(clusterMap.containsKey(key) && clusterMap.get(key).getAssociatedTrackID()==-1) {
                                    clusterMap.replace(key, cls); 
                                } else {
                                    if(!clusterMap.containsKey(key)) {
                                        clusterMap.put(key, cls); 
                                    }
                                }
                                doca = clsDoca;
                            }                           
                        }
                    }
                }
            }
        }
        // if any lost cluster with doca better than the seed is found, save it
        List<Cluster> clustersOnTrack = new ArrayList<>();
        for(Entry<Integer,Cluster> entry : clusterMap.entrySet()) {
            if(entry.getValue().getAssociatedTrackID()==-1) clustersOnTrack.add(entry.getValue());
        }
        return clustersOnTrack;
    }
    
    public List<Cluster> findBMTClustersOnTrk(List<Cluster> allClusters, List<Cross> seedCrosses, Helix helix, double P, int Q, Swim swimmer) { 
        // initialize swimmer starting from the track vertex
        double maxPathLength = 1; 
        Point3D vertex = helix.getVertex();
        swimmer.SetSwimParameters(vertex.x()/10, vertex.y()/10, vertex.z()/10, 
                     Math.toDegrees(helix.getPhiAtDCA()), Math.toDegrees(Math.acos(helix.cosTheta())),
                     P, Q, maxPathLength) ;
        double[] inters = null;
        // load SVT clusters that are in the seed
        Map<Integer,Cluster> clusterMap = new HashMap<>(); 
        for(Cross cross : seedCrosses) {
            if(cross.getDetector() != DetectorType.BMT)
                continue;
            Cluster cluster = cross.getCluster1(); 
            cluster.setAssociatedTrackID(0);
            clusterMap.put(BMTGeometry.getModuleId(cluster.getLayer(), cluster.getSector()), cluster);
        }   
        
        // for each layer
        for (int ilayer = 0; ilayer < BMTGeometry.NLAYERS; ilayer++) {
            int layer = ilayer + 1;
            double radius  = Geometry.getInstance().getBMT().getRadiusMidDrift(layer);
            // identify the sector the track may be going through (this doesn't account for misalignments
            Point3D helixPoint = helix.getPointAtRadius(radius);
            // reinitilize swimmer from last surface
            if(inters!=null) {
                swimmer.SetSwimParameters(inters[0], inters[1], inters[2], inters[3], inters[4], inters[5], Q);
            }
            
            for(int isector=0; isector<BMTGeometry.NSECTORS; isector++) {
                int sector = isector+1;
                
                // check the angle between the trajectory point and the sector 
                // and skip sectors that are too far (more than the sector angular coverage)
                if(Geometry.getInstance().getBMT().inDetector(layer, sector, helixPoint)==false)
                    continue;
                 
                // calculate trajectory
                Point3D traj = null;
                inters = swimmer.SwimRho(radius/10, Constants.SWIMACCURACYBMT/10);
                
                if(inters!=null) {
                    traj = new Point3D(inters[0]*10, inters[1]*10, inters[2]*10);
                } 
                
                int key = BMTGeometry.getModuleId(layer, sector);
                
               
                // if trajectory is valid, look for missing clusters
                if(traj!=null && Geometry.getInstance().getBMT().inDetector(layer, sector, traj)) {
                    double  doca    = Double.POSITIVE_INFINITY; 
                    // loop over all clusters in the same sector and layer that are not associated to s track
                    for(Cluster cls : allClusters) {
                        if(cls.getAssociatedTrackID()==-1 && cls.getSector()==sector && cls.getLayer()==layer) {
                            double clsDoca = cls.residual(traj); 
                            // save the ones that have better doca
                            if(Math.abs(clsDoca)<Math.abs(doca) && Math.abs(clsDoca)<10*cls.size()*cls.getSeedStrip().getPitch()/Math.sqrt(12)) {
                                if(clusterMap.containsKey(key) && clusterMap.get(key).getAssociatedTrackID()==-1) {
                                    clusterMap.replace(key, cls); 
                                } else {
                                    if(!clusterMap.containsKey(key)) {
                                        clusterMap.put(key, cls); 
                                    }
                                }
                                doca = clsDoca;
                            }                           
                        }
                    }
                }
            }
        }
        // if any lost cluster with doca better than the seed is found, save it
        List<Cluster> clustersOnTrack = new ArrayList<>();
        for(Entry<Integer,Cluster> entry : clusterMap.entrySet()) {
            if(entry.getValue().getAssociatedTrackID()==-1 && entry.getValue().flagForExclusion) clustersOnTrack.add(entry.getValue());
        }
        return clustersOnTrack;
    }
    
    public List<Cross> findCrossesOnBMTTrack(List<Cross> allCrosses, List<Cluster> bmtclsOnTrack) {
         // fill the sorted list
        List<Cross> BMTCrosses = new ArrayList<>();
        for(Cluster cluster : bmtclsOnTrack) {
            for(Cross cross : allCrosses) {
                if(cluster.getId()==cross.getCluster1().getId()) BMTCrosses.add(cross);
            }
        }
        if(BMTCrosses.size()!=bmtclsOnTrack.size()) System.out.println("Error: cross missing from list");
        return BMTCrosses;
    }
    
    @Deprecated
    public void matchTrack2Traj(Seed trkcand, Map<Integer, 
            org.jlab.clas.tracking.kalmanfilter.helical.KFitter.HitOnTrack> traj) {
        
        for (int i = 0; i < trkcand.getClusters().size(); i++) { //SVT
            if(trkcand.getClusters().get(i).getDetector()==DetectorType.BST) {
                Cluster cluster = trkcand.getClusters().get(i);
                int layer  = trkcand.getClusters().get(i).getLayer();
                int sector = trkcand.getClusters().get(i).getSector();
                Point3D p = new Point3D(traj.get(layer).x, traj.get(layer).y, traj.get(layer).z);
                cluster.setCentroidResidual(traj.get(layer).residual);
                cluster.setSeedResidual(p);             
                for (Hit hit : cluster) {
                    double doca1 = hit.residual(p);
                    double sigma1 = Geometry.getInstance().getSVT().getSingleStripResolution(layer, hit.getStrip().getStrip(), traj.get(layer).z);
                    hit.setstripResolutionAtDoca(sigma1);
                    hit.setdocaToTrk(doca1);  
                    if(traj.get(layer).isUsed)
                        hit.setTrkgStatus(1);
                }
            }
        }

        // adding the cross infos
        for (int c = 0; c < trkcand.getCrosses().size(); c++) {
            if (trkcand.getCrosses().get(c).getDetector()==DetectorType.BST) {
                int  layer = trkcand.getCrosses().get(c).getCluster1().getLayer();
                Vector3D d = new Vector3D(traj.get(layer).px, traj.get(layer).py, traj.get(layer).pz).asUnit();
                trkcand.getCrosses().get(c).updateSVTCross(d);
            }
            if (trkcand.getCrosses().get(c).getDetector()==DetectorType.BMT) {
                // update cross position
                int layer = trkcand.getCrosses().get(c).getCluster1().getLayer()+6;
                Point3D  p = new Point3D(traj.get(layer).x, traj.get(layer).y, traj.get(layer).z);
                Vector3D v = new Vector3D(traj.get(layer).px, traj.get(layer).py, traj.get(layer).pz).asUnit();
                trkcand.getCrosses().get(c).updateBMTCross(p, v);
                trkcand.getCrosses().get(c).setDir(v); 
                Cluster cluster = trkcand.getCrosses().get(c).getCluster1();
                if (trkcand.getCrosses().get(c).getType()==BMTType.Z) {
                    cluster.setCentroidResidual(traj.get(layer).residual*cluster.getTile().baseArc().radius());
                }
                else if (trkcand.getCrosses().get(c).getType()==BMTType.C) {
                    cluster.setCentroidResidual(traj.get(layer).residual);
                    cluster.setSeedResidual(p); 
                }
                for (Hit hit : cluster) {
                    hit.setdocaToTrk(hit.residual(p));
                    if(traj.get(layer).isUsed) hit.setTrkgStatus(1);
                }
            }
        }
    }
    
    public boolean fitSeedLayerExcluded(Seed seed, double xbeam, double ybeam, double bfield)  {
        List<Cluster> clusters = seed.getClusters();
        clusters.sort(Comparator.comparing(Cluster::getTlayer));
        int[] L = new int[clusters.size()];
        for(int l =0; l<clusters.size(); l++) {
            L[l] = clusters.get(l).getTlayer();
        }
        Seed testSeed = new Seed(seed.getCrosses());
        double chi2_Circ = seed.getCircleFitChi2PerNDF();
        double chi2_Line = seed.getLineFitChi2PerNDF();
        int exclLy = -1;
        for(int l =0; l<clusters.size(); l++) {
            boolean fitStatus = testSeed.fit(3, xbeam, ybeam, bfield, L[l], true);
            if(fitStatus) {
                double chi2_Circ_test = testSeed.getCircleFitChi2PerNDF();
                double chi2_Line_test = testSeed.getLineFitChi2PerNDF();
                if(chi2_Circ_test<=chi2_Circ && chi2_Line_test<chi2_Line || 
                        chi2_Circ_test<chi2_Circ && chi2_Line_test<=chi2_Line ) {
                    exclLy = L[l];
                }
            }
        }
        List<Cross> rmCrosses = new ArrayList<>();
        List<Cluster> rmClusters = new ArrayList<>();
        
        for(Cross c : seed.getCrosses()) {
            if(c.getDetector()==DetectorType.BST) {
                if(c.getCluster1()!=null && c.getCluster1().getTlayer()==exclLy) {
                    rmCrosses.add(c);
                    rmClusters.add(c.getCluster1());
                }
                if(c.getCluster2()!=null && c.getCluster2().getTlayer()==exclLy) {
                    rmCrosses.add(c);
                    rmClusters.add(c.getCluster2());
                } 
            }
             if(c.getDetector()==DetectorType.BMT) {
                if(c.getCluster1().getTlayer()==exclLy) {
                    rmCrosses.add(c);
                    rmClusters.add(c.getCluster1());
                }
            }
        }
        
        boolean fitStatus = testSeed.fit(3, xbeam, ybeam, bfield, true);
        
        if(seed.getChi2()>=testSeed.getChi2()+0.5) {
            seed.getCrosses().removeAll(rmCrosses);
            seed.getClusters().removeAll(rmClusters);
        }
        
        
        return fitStatus;
    }
    
    public List<Seed> reFit(List<Seed> seedlist, Swim swimmer,  StraightTrackSeeder trseed, double xb, double yb) {
        List<Seed> filtlist = new ArrayList<>();
        if(seedlist==null)
            return filtlist;
        for (Seed bseed : seedlist) {
            if(bseed == null)
                continue;
            List<Seed>  fseeds = this.reFitSeed(bseed, trseed, xb, yb);
            if(fseeds!=null) {
                filtlist.addAll(fseeds);
            }
        }
        return filtlist;
    }        
    
    public List<Seed> reFitSeed(Seed bseed, StraightTrackSeeder trseed, double xb, double yb) {
        
        List<Cross> refib = new ArrayList<>();
        List<Cross> refi = new ArrayList<>();
        for(Cross c : bseed.getCrosses()) {
            int layr = 0;
            int layr2 = 0;
            if(c.getDetector()==DetectorType.BMT) {
                layr = c.getOrderedRegion()+3;
                if((int)Constants.getInstance().getUsedLayers().get(layr)>0) {
                    c.isInSeed = false;
                    //System.out.println("refit "+c.printInfo());
                    refib.add(c);
                }
            } else {
                layr = c.getCluster1().getLayer();
                layr2 = c.getCluster2().getLayer();
                if((int)Constants.getInstance().getUsedLayers().get(layr)>0 
                        && (int)Constants.getInstance().getUsedLayers().get(layr2)>0) {
                    c.updateSVTCross(null); 
                    c.isInSeed = false;
                    refi.add(c); 
                }
            }
        }
        Collections.sort(refi);
        List<Seed> seedlist = trseed.findSeed(refi, refib, false, xb, yb);
        return seedlist;
    }
    
    public boolean reFitCircle(Seed seed, int iter, double xb, double yb) {
        boolean fitStatus = false;
        
        List<Double> Xs = new ArrayList<>() ;
        List<Double> Ys = new ArrayList<>() ;
        List<Double> Ws = new ArrayList<>() ;
        
        CircleFitter circlefit = new CircleFitter(xb, yb);
        for(int i = 0; i< iter; i++) {
            Xs.clear();
            Ys.clear();
            Ws.clear();
            List<Cross> seedCrosses = seed.getCrosses();
            
            for (int j = 0; j < seedCrosses.size(); j++) {
                if (seedCrosses.get(j).getType() == BMTType.C)
                    continue;
                
                Xs.add(seedCrosses.get(j).getPoint().x());
                Ys.add(seedCrosses.get(j).getPoint().y());
                Ws.add(1. / (seedCrosses.get(j).getPointErr().x()*seedCrosses.get(j).getPointErr().x()
                            +seedCrosses.get(j).getPointErr().y()*seedCrosses.get(j).getPointErr().y()));

            }

            fitStatus = circlefit.fitStatus(Xs, Ys, Ws, Xs.size());

            if(fitStatus) {
                CircleFitPars pars = circlefit.getFit();
                seed.getHelix().setCurvature(pars.rho());           
                seed.getHelix().setDCA(-pars.doca());
                seed.getHelix().setPhiAtDCA(pars.phi());
                seed.update_Crosses(xb,yb);
            }
        }
        return fitStatus;
    }
    
    //RDV: to be checked: causes track crosses to be overwritten
    @Deprecated
    public List<Seed> reFit(List<Seed> seedlist, Swim swimmer,  TrackSeederSVTLinker trseed,  TrackSeeder trseed2, double xb, double yb) {
        trseed = new TrackSeederSVTLinker(swimmer, xb, yb);
        trseed2 = new TrackSeeder(swimmer, xb, yb);
        List<Seed> filtlist = new ArrayList<>();
        if(seedlist==null)
            return filtlist;
        for (Seed bseed : seedlist) {
            if(bseed == null)
                continue;
            List<Seed>  fseeds = this.reFitSeed(bseed, swimmer, trseed, trseed2);
            if(fseeds!=null) {
                filtlist.addAll(fseeds);
            }
        }
        return filtlist;
    }
    
    public List<Seed> reFitSeed(Seed bseed, Swim swimmer,  TrackSeederSVTLinker trseed,  TrackSeeder trseed2) {
        boolean pass = true;

        List<Cross> refib = new ArrayList<>();
        List<Cross> refi = new ArrayList<>();
        for(Cross c : bseed.getCrosses()) {
            int layr = 0;
            int layr2 = 0;
            c.setAssociatedTrackID(-1);
            if(c.getDetector()==DetectorType.BMT) {
                layr = c.getOrderedRegion()+3;
                if((int)Constants.getInstance().getUsedLayers().get(layr)>0) {
                    c.isInSeed = false;
                    refib.add(c);
                }
            } else {
                layr = c.getCluster1().getLayer();
                layr2 = c.getCluster2().getLayer();
                if((int)Constants.getInstance().getUsedLayers().get(layr)>0 
                        && (int)Constants.getInstance().getUsedLayers().get(layr2)>0) {
                    c.updateSVTCross(null);
                    c.isInSeed = false;
                   // System.out.println("refit "+c.printInfo());
                    refi.add(c); 
                }
            }
        }
        Collections.sort(refi);
        List<Seed> seedlist = trseed.findSeed(refi, refib);
        
        trseed2.unUsedHitsOnly = true;
        seedlist.addAll( trseed2.findSeed(refi, refib)); 
        
        return seedlist;
    }
    
    public List<StraightTrack> reFit(List<StraightTrack> seedlist, CosmicFitter fitTrk,  StraightTrackCandListFinder trkfindr) {
        fitTrk = new CosmicFitter();
        trkfindr = new StraightTrackCandListFinder();
        List<StraightTrack> filtlist = new ArrayList<>();
        if(seedlist==null)
            return filtlist;
        for (StraightTrack bseed : seedlist) {
            if(bseed == null)
                continue;
            List<StraightTrack> fseeds = this.reFitSeed(bseed, fitTrk, trkfindr);
            if(fseeds!=null) {
                filtlist.addAll(fseeds);
            }
        }
        return filtlist;
    }
    
    
    public List<StraightTrack> reFitSeed(StraightTrack cand, CosmicFitter fitTrk,StraightTrackCandListFinder trkfindr) {
        boolean pass = true;

        List<StraightTrack> seedlist = new ArrayList<>();
        List<Cross> refib = new ArrayList<>();
        List<Cross> refi = new ArrayList<>();
        for(Cross c : cand) {
            int layr = 0;
            int layr2 = 0;
            if(c.getDetector()==DetectorType.BMT) {
                layr = c.getOrderedRegion()+3;
                if((int)Constants.getInstance().getUsedLayers().get(layr)>0) {
                    c.isInSeed = false;
                //    System.out.println("refit "+c.printInfo());
                    refib.add(c);
                }
            } else {
                layr = c.getCluster1().getLayer();
                layr2 = c.getCluster2().getLayer();
                if((int)Constants.getInstance().getUsedLayers().get(layr)>0 
                        && (int)Constants.getInstance().getUsedLayers().get(layr2)>0) {
                    c.updateSVTCross(null);
                    c.isInSeed = false;
                   // System.out.println("refit "+c.printInfo());
                    refi.add(c); 
                }
            }
        }
        if(refi.size()>=3) {
            StraightTrackCandListFinder.RayMeasurements NewMeasArrays = trkfindr.
                getRayMeasurementsArrays((ArrayList<Cross>) refi, false, false, true);
            fitTrk.fit(NewMeasArrays._X, NewMeasArrays._Y, NewMeasArrays._Z,
                    NewMeasArrays._Y_prime, NewMeasArrays._ErrRt, 
                    NewMeasArrays._ErrY_prime, NewMeasArrays._ErrZ);
            if(fitTrk.getray()!=null) {
                cand = new StraightTrack(fitTrk.getray());
                cand.addAll(refi);
                //refit with the SVT included to determine the z profile
                NewMeasArrays = trkfindr.
                getRayMeasurementsArrays((ArrayList<Cross>) refi, false, false, false);
                fitTrk.fit(NewMeasArrays._X, NewMeasArrays._Y, NewMeasArrays._Z, 
                        NewMeasArrays._Y_prime, NewMeasArrays._ErrRt, NewMeasArrays._ErrY_prime, NewMeasArrays._ErrZ);
                cand = new StraightTrack(fitTrk.getray()); 
                cand.addAll(refi);
                seedlist.add(cand);
            }
        }
        return seedlist;
    }
    
    public double[] mcTrackPars(DataEvent event) {
        double[] value = new double[6];
        if (event.hasBank("MC::Particle") == false) {
            return value;
        }
        DataBank bank = event.getBank("MC::Particle");
        
        // fills the arrays corresponding to the variables
        if(bank!=null) {
            value[0] = (double) bank.getFloat("vx", 0)*10;
            value[1] = (double) bank.getFloat("vy", 0)*10;
            value[2] = (double) bank.getFloat("vz", 0)*10;
            value[3] = (double) bank.getFloat("px", 0);
            value[4] = (double) bank.getFloat("py", 0);
            value[5] = (double) bank.getFloat("pz", 0);
        }
        return value;
    }

    public double[][] getCovMatInTrackRep(Track trk) {
        double[][] tCov = new double[6][6];
        double [][] hCov = trk.getHelix().getCovMatrix();
        
    //error matrix (assuming that the circle fit and line fit parameters are uncorrelated)
    // | d_dca*d_dca                   d_dca*d_phi_at_dca            d_dca*d_curvature        0            0             |
    // | d_phi_at_dca*d_dca     d_phi_at_dca*d_phi_at_dca     d_phi_at_dca*d_curvature        0            0             |
    // | d_curvature*d_dca	    d_curvature*d_phi_at_dca      d_curvature*d_curvature     0            0             |
    // | 0                              0                             0                    d_Z0*d_Z0                     |
    // | 0                              0                             0                       0        d_tandip*d_tandip |
    // 
    
    
        double pt = trk.getPt();
        double rho = trk.getHelix().getCurvature();
        double c = Constants.LIGHTVEL;
        double Bz = pt/(c*trk.getHelix().radius());
        double d0 = trk.getHelix().getDCA();
        double phi0 = trk.getHelix().getPhiAtDCA();
        double tandip = trk.getHelix().getTanDip();

        double delxdeld0 = -Math.sin(phi0);
        double delxdelphi0 = -d0*Math.cos(phi0);
        double delydeld0 = Math.cos(phi0);
        double delydelphi0 = -d0*Math.sin(phi0);
        
        double delzdelz0 = 1;
        
        double delpxdelphi0 = -pt*Math.sin(phi0);   
        double delpxdelrho = -pt*Math.cos(phi0)/rho;    
        double delpydelphi0 = pt*Math.cos(phi0);
        double delpydelrho = -pt*Math.sin(phi0)/rho;    
        
        double delpzdelrho = -pt*tandip/rho;    
        
        double delpzdeltandip = pt;
        
        tCov[0][0] = (hCov[0][0]*delxdeld0+hCov[0][1]*delxdelphi0)*delxdeld0
                    +(hCov[1][0]*delxdeld0+hCov[1][1]*delxdelphi0)*delxdelphi0;
        tCov[0][1] = (hCov[0][0]*delydeld0+hCov[0][1]*delydelphi0)*delxdeld0
                    +(hCov[1][0]*delydeld0+hCov[1][1]*delydelphi0)*delxdelphi0;
        tCov[0][2] =  hCov[0][3]*delxdeld0+hCov[1][3]*delxdelphi0;
        tCov[0][3] = (hCov[0][1]*delpxdelphi0+hCov[0][2]*delpxdelrho)*delxdeld0
                    +(hCov[1][1]*delpxdelphi0+hCov[1][2]*delpxdelrho)*delxdelphi0;
        tCov[0][4] = (hCov[0][1]*delpydelphi0+hCov[0][2]*delpydelrho)*delxdeld0
                    +(hCov[1][1]*delpydelphi0+hCov[1][2]*delpydelrho)*delxdelphi0;
        tCov[0][5] = (hCov[0][2]*delpzdelrho+hCov[0][4]*delpzdeltandip)*delxdeld0
                    +(hCov[1][2]*delpzdelrho+hCov[1][4]*delpzdeltandip)*delxdelphi0;
        
        
        tCov[1][0] = (hCov[0][0]*delxdeld0+hCov[0][1]*delxdelphi0)*delydeld0
                    +(hCov[1][0]*delxdeld0+hCov[1][1]*delxdelphi0)*delydelphi0;
        tCov[1][1] = (hCov[0][0]*delydeld0+hCov[0][1]*delydelphi0)*delydeld0
                    +(hCov[1][0]*delydeld0+hCov[1][1]*delydelphi0)*delydelphi0;
        tCov[1][2] = (hCov[0][3]*delydeld0+hCov[1][3]*delydelphi0);
        tCov[1][3] = (hCov[0][1]*delpxdelphi0+hCov[0][2]*delpxdelrho)*delydeld0
                    +(hCov[1][1]*delpxdelphi0+hCov[1][2]*delpxdelrho)*delydelphi0;
        tCov[1][4] = (hCov[0][1]*delpydelphi0+hCov[0][2]*delpydelrho)*delydeld0
                    +(hCov[1][1]*delpydelphi0+hCov[1][2]*delpydelrho)*delydelphi0;
        tCov[1][5] = (hCov[0][2]*delpzdelrho+hCov[0][4]*delpzdeltandip)*delydeld0
                    +(hCov[1][2]*delpzdelrho+hCov[1][4]*delpzdeltandip)*delydelphi0;
        
        
        tCov[2][0] = (hCov[3][0]*delxdeld0+hCov[3][1]*delxdelphi0);
        tCov[2][1] = (hCov[3][0]*delydeld0+hCov[3][1]*delydelphi0);
        tCov[2][2] =  hCov[3][3];
        tCov[2][3] = (hCov[3][1]*delpxdelphi0+hCov[3][2]*delpxdelrho);
        tCov[2][4] = (hCov[3][1]*delpydelphi0+hCov[3][2]*delpydelrho);
        tCov[2][5] = (hCov[3][2]*delpzdelrho+hCov[3][4]*delpzdeltandip);


        tCov[3][0] = (hCov[1][0]*delxdeld0+hCov[1][1]*delxdelphi0)*delpxdelphi0
                    +(hCov[2][0]*delxdeld0+hCov[2][1]*delxdelphi0)*delpxdelrho;
        tCov[3][1] = (hCov[1][0]*delydeld0+hCov[1][1]*delydelphi0)*delpxdelphi0
                    +(hCov[2][0]*delydeld0+hCov[2][1]*delydelphi0)*delpxdelrho;
        tCov[3][2] =  hCov[1][3]*delpxdelphi0+hCov[2][3]*delpxdelrho;
        tCov[3][3] = (hCov[1][1]*delpxdelphi0+hCov[1][2]*delpxdelrho)*delpxdelphi0
                    +(hCov[2][1]*delpxdelphi0+hCov[2][2]*delpxdelrho)*delpxdelrho;
        tCov[3][4] = (hCov[1][1]*delpydelphi0+hCov[1][2]*delpydelrho)*delpxdelphi0
                    +(hCov[2][1]*delpydelphi0+hCov[2][2]*delpydelrho)*delpxdelrho;
        tCov[3][5] = (hCov[1][2]*delpzdelrho+hCov[1][4]*delpzdeltandip)*delpxdelphi0
                    +(hCov[2][2]*delpzdelrho+hCov[2][4]*delpzdeltandip)*delpxdelrho;
                
        
        tCov[4][0] = (hCov[1][0]*delxdeld0+hCov[1][1]*delxdelphi0)*delpydelphi0
                    +(hCov[2][0]*delxdeld0+hCov[2][1]*delxdelphi0)*delpydelrho;
        tCov[4][1] = (hCov[1][0]*delydeld0+hCov[1][1]*delydelphi0)*delpydelphi0
                    +(hCov[2][0]*delydeld0+hCov[2][1]*delydelphi0)*delpydelrho;
        tCov[4][2] =  hCov[1][3]*delpydelphi0+hCov[2][3]*delpydelrho;
        tCov[4][3] = (hCov[1][1]*delpxdelphi0+hCov[1][2]*delpxdelrho)*delpydelphi0
                    +(hCov[2][1]*delpxdelphi0+hCov[2][2]*delpxdelrho)*delpydelrho;
        tCov[4][4] = (hCov[1][1]*delpydelphi0+hCov[1][2]*delpydelrho)*delpydelphi0
                    +(hCov[2][1]*delpydelphi0+hCov[2][2]*delpydelrho)*delpydelrho;
        tCov[4][5] = (hCov[1][2]*delpzdelrho+hCov[1][4]*delpzdeltandip)*delpydelphi0
                    +(hCov[2][2]*delpzdelrho+hCov[2][4]*delpzdeltandip)*delpydelrho;
              
        
        tCov[5][0] = (hCov[2][0]*delxdeld0+hCov[2][1]*delxdelphi0)*delpzdelrho
                    +(hCov[4][0]*delxdeld0+hCov[4][1]*delxdelphi0)*delpzdeltandip;
        tCov[5][1] = (hCov[2][0]*delydeld0+hCov[2][1]*delydelphi0)*delpzdelrho
                    +(hCov[4][0]*delydeld0+hCov[4][1]*delydelphi0)*delpzdeltandip;        
        tCov[5][2] =  hCov[2][3]*delpzdelrho+hCov[4][3]*delpzdeltandip;        
        tCov[5][3] = (hCov[2][1]*delpxdelphi0+hCov[2][2]*delpxdelrho)*delpzdelrho
                    +(hCov[4][1]*delpxdelphi0+hCov[4][2]*delpxdelrho)*delpzdeltandip;        
        tCov[5][4] = (hCov[2][1]*delpydelphi0+hCov[2][2]*delpydelrho)*delpzdelrho
                    +(hCov[4][1]*delpydelphi0+hCov[4][2]*delpydelrho)*delpzdeltandip;
        tCov[5][5] = (hCov[2][2]*delpzdelrho+hCov[2][4]*delpzdeltandip)*delpzdelrho
                    +(hCov[4][2]*delpzdelrho+hCov[4][4]*delpzdeltandip)*delpzdeltandip;
        
        //for (int k = 0; k < 6; k++) {
        //    System.out.println(tCov[k][0]+"	"+tCov[k][1]+"	"+tCov[k][2]+"	"+tCov[k][3]+"	"+tCov[k][4]+"	"+tCov[k][5]);
        //}
        //System.out.println("    ");
        
        return tCov;
    }
    public Cross createPseudoCross(Cluster cl, int counter) {
        Cross this_cross = new Cross(DetectorType.BST, BMTType.UNDEFINED, cl.getSector(), cl.getRegion(), -(counter+1));
        this_cross.setOrderedRegion(cl.getRegion());
        if(cl.getLayer()%2==0) {
            this_cross.setCluster1(cl);
        } else {
            this_cross.setCluster2(cl);
        }
        cl.setAssociatedCrossID(this_cross.getId());  
        this_cross.setPoint0(cl.trajPoint); 
        this_cross.setPoint(cl.trajPoint);
        this_cross.setPointErr0(new Point3D(0,0,0));
        this_cross.setPointErr(new Point3D(0,0,0));
        if(Constants.getInstance().seedingDebugMode)
            System.out.println("Created pseudo-cross "+this_cross.printInfo());
        return this_cross;
    }
    public List<Cross> findCrossesFromClustersOnTrk(List<Cross> allCrosses, 
            List<Cluster> clsOnTrack, Track track, double xb, double yb) {
        CrossMaker cm = new CrossMaker();
        List<Cross> crosses = new ArrayList<>();
        for (Cluster cl1 : clsOnTrack) {//inner layer
            if(cl1.getLayer()%2==0) 
                continue;
            //mke crosses using these clusters
            for (Cluster cl2 : clsOnTrack) { //outer layer
                if(cl2.getLayer()%2==1) 
                    continue;
                
                if (cl1.getRegion() == cl2.getRegion() && cl1.getSector() == cl2.getSector()
                        && cl1.getMinStrip() + cl2.getMinStrip() > SVTParameters.MINSTRIPSUM
                        && cl1.getMaxStrip() + cl2.getMaxStrip() < SVTParameters.MAXSTRIPSUM) {
                    Cross this_cross = null;
                    for(Cross c : allCrosses) {
                        if(c.getCluster1()!=null && c.getCluster2()!=null)
                            if(c.getCluster1().getId()==cl1.getId() && c.getCluster2().getId()==cl2.getId())
                                this_cross = c;
                    }
                    if(this_cross==null) {
                        //System.out.print("Found NNNNNNNNNNNNNNNNNNNNNNew cross");
                        // define new cross 
                        this_cross = new Cross(DetectorType.BST, BMTType.UNDEFINED, cl1.getSector(), cl1.getRegion(), allCrosses.size()+1);
                        this_cross.setOrderedRegion(cl1.getRegion());
                        cl1.setAssociatedCrossID(this_cross.getId());
                        cl2.setAssociatedCrossID(this_cross.getId());
                        this_cross.setCluster1(cl1);
                        this_cross.setCluster2(cl2);
                        // sets the cross parameters (point3D and associated error) from the SVT geometry
                        this_cross.updateSVTCross(null);
                        // the uncorrected point obtained from default estimate that the track is at 90 deg wrt the module should not be null
                        if (this_cross.getPoint0() != null) {     
                            this_cross.setDetector(DetectorType.BST);
                            cm.calcCentErr(this_cross, this_cross.getCluster1());
                            cm.calcCentErr(this_cross, this_cross.getCluster2());
                        }
                        allCrosses.add(this_cross);
                    }
                    if (track.getHelix() != null && track.getHelix().getCurvature() != 0) {
                        double Cx = this_cross.getPoint().x()-xb;
                        double Cy = this_cross.getPoint().y()-yb;
                        double R = Math.sqrt(Cx * Cx + Cy * Cy);
                        this_cross.update(track.getHelix().getPointAtRadius(R), track.getHelix().getTrackDirectionAtRadius(R));
                        this_cross.setAssociatedTrackID(track.getSeed().getClusters().get(0).getAssociatedTrackID());
                    }
                    this_cross.setAssociatedTrackID(track.getSeed().getClusters().get(0).getAssociatedTrackID());
                    crosses.add(this_cross); 
                }
            }
        }
        return crosses;
    }

    public static void getUniqueSeedList(List<Seed> seeds) {
        List<Seed> dupl = new ArrayList<>();
        Map<Double, Seed> seedmap = new HashMap<>();
        for(Seed s : seeds) {
            double key = getTrackKey(s); 
            if(Double.isNaN(key)) {
                dupl.add(s);
            } else {
                if(seedmap.containsKey(key)) {
                    dupl.add(s); 
                } else {
                    seedmap.put(key, s);
                }
            }
        }
        seeds.removeAll(dupl);
    }

    public static List<Seed> getOverlappingSeedList(List<Seed> seeds) {
        List<Seed> dupl = new ArrayList<>();
        Map<Double, Seed> seedmap = new HashMap<>();
        for(Seed s : seeds) {
            double key = getTrackKey(s); 
            if(Double.isNaN(key)) {
                dupl.add(s);
            } else {
                if(seedmap.containsKey(key)) {
                    dupl.add(s); 
                } else {
                    seedmap.put(key, s);
                }
            }
        }
        return dupl;
    }
    
    static double getTrackKey(Seed s) {
        List<Cross> crs = s.getCrosses();
        double x = 0;
        double y = 0;
        double z = 0;
        for(Cross c : crs) {
            if(!Double.isNaN(c.getPoint0().x()))
                x+=c.getPoint0().x();
            if(!Double.isNaN(c.getPoint0().y()))
                y+=c.getPoint0().y();
            if(!Double.isNaN(c.getPoint0().z()))
                z+=c.getPoint0().z();
        }
        x/=crs.size();
        y/=crs.size();
        z/=crs.size();
        return new Vector3D(x,y,z).mag();
    }

    Track recovTrkMisClusSearch(Seed seed, org.jlab.clas.tracking.trackrep.Helix hlx, double[][] cov, KFitter kf2, KFitter kf, int pid, 
            List<Surface> surfaces, double xb, double yb, List<Cluster> SVTclusters, List<Cross> SVTcrosses, 
            Swim swimmer, double solenoidScale, double solenoidValue, Measurements measure) {

        if(Constants.getInstance().seedingDebugMode) System.out.println("TRACK RECOVERY");
        kf2.init(hlx, cov, xb, yb, 0, surfaces, PDGDatabase.getParticleMass(pid));
        kf2.runFitter();
        if(kf2.getHelix()==null)  
            return null;
        Track fittedTrack = new Track(seed, kf2, pid); 
//        fittedTrack.setStatus(-1);
        if(Constants.getInstance().seedingDebugMode) System.out.println("RECOVERED..."+fittedTrack.toString());
        for(Cross c : fittedTrack) { 
            if(c.getDetector()==DetectorType.BST) {
                if(c.getCluster1()!=null) c.getCluster1().setAssociatedTrackID(0);
                if(c.getCluster2()!=null) c.getCluster2().setAssociatedTrackID(0);
            }
        }
        //refit adding missing clusters
        List<Cluster> clsOnTrack = this.findClustersOnTrk(SVTclusters, seed.getClusters(), fittedTrack, swimmer); //VZ: finds missing clusters; RDV fix 0 error
        List<Cross> crsOnTrack = this.findCrossesFromClustersOnTrk(SVTcrosses, clsOnTrack, fittedTrack, xb, yb);

        if(clsOnTrack.size()>0) {
            seed.add_Clusters(clsOnTrack);
        }
        seed.getClusters().sort(Comparator.comparing(Cluster::getTlayer));
        if(crsOnTrack.size()>0) {
            seed.add_Crosses(crsOnTrack);
        }

        //Collections.sort(seed.getClusters());
        //Collections.sort(seed.getCrosses());

        seed.fit(3, xb, yb, solenoidValue, true);

        //reset pars
        Point3D v = seed.getHelix().getVertex();
        Vector3D p = seed.getHelix().getPXYZ(solenoidValue);

        int charge = (int) (Math.signum(solenoidScale)*seed.getHelix().getCharge());
        if(solenoidValue<0.001)
            charge = 1;

        org.jlab.clas.tracking.trackrep.Helix hlx2 = new org.jlab.clas.tracking.trackrep.Helix(v.x(),v.y(),v.z(),p.x(),p.y(),p.z(), charge,
                        solenoidValue, xb, yb, Units.MM);
        surfaces.clear();
        surfaces = measure.getMeasurements(seed); 
        kf.init(hlx2, cov, xb, yb, 0, surfaces, PDGDatabase.getParticleMass(pid)) ;
        kf.runFitter();
        if(Constants.getInstance().seedingDebugMode) System.out.println("Seed with searched clusters "+seed.toString());
        if(Constants.getInstance().seedingDebugMode)
        System.out.println("KF status ... failed "+kf.setFitFailed+" ndf "+kf.NDF+" helix "+kf.getHelix());
        if (kf.setFitFailed == false && kf.NDF>0 && kf.getHelix()!=null) { 
            fittedTrack = new Track(seed, kf, pid);
            if(Constants.getInstance().seedingDebugMode) System.out.println("RECOVERED..."+fittedTrack.toString());
            for(Cross c : fittedTrack) { 
                if(c.getDetector()==DetectorType.BST) {
                    if(c.getCluster1()!=null) c.getCluster1().setAssociatedTrackID(0);
                    if(c.getCluster2()!=null) c.getCluster2().setAssociatedTrackID(0);
                }
            }
            //fittedTrack.setStatus(1); 
        } else {
            if(fittedTrack!=null) {
                //fittedTrack.setStatus(-1); 
                if(Constants.getInstance().seedingDebugMode) 
                    System.out.println("RECOVERED...with negative status "+fittedTrack.toString());
                
            }
        }
        return fittedTrack;
    }
}
