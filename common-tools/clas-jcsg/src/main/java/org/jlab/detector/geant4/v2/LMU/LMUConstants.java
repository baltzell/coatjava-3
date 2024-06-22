package org.jlab.detector.geant4.v2.LMU;


import org.jlab.detector.calib.utils.DatabaseConstantProvider; 


public class LMUConstants {

    private final static String CCDBPATH = "/geometry/urwell/";
    
    public final static int NREGIONS    = 6;    //number of regions 
    public final static int NLAYERS     = 2;    //number of layers per region (SHOULD BE AN ARRAY)

    public final static double XSIZE = 10;
    public final static double YSIZE = 10;
    
    public final static double XENLARGEMENT = 0.1; // cm
    public final static double YENLARGEMENT = 0.1;  // cm
    public final static double ZENLARGEMENT = 0.1; // cm
   
    // Chamber volumes  and materials (units are cm)
    public final static double[] CHAMBERVOLUMESTHICKNESS = {0.0025, 0.0005,0.3,                                // window
                                                            0.0025, 0.0005,0.4,                                // cathode
                                                            0.0005, 0.005, 0.0005,                             // uRWell + DlC
                                                            0.0005, 0.005, 0.0005,                             // Capacitive sharing layer1
                                                            0.0005, 0.005, 0.0005,                             // Capacitive sharing layer2
                                                            0.005,  0.0005,0.005, 0.005,  0.0005,0.005, 0.005, // Readout
                                                            0.0127, 0.3, 0.0125};                              // support
    public final static String[] CHAMBERVOLUMESNAME = {"window_kapton", "window_Al", "window_gas",
           "cathode_kapton", "cathode_Al", "cathode_gas",
           "muRwell_Cu", "muRwell_kapton", "muRwell_dlc", 
           "capa_sharing_layer1_glue","capa_sharing_layer1_Cr","capa_sharing_layer1_kapton",
           "capa_sharing_layer2_glue","capa_sharing_layer2_Cr","capa_sharing_layer2_kapton",
           "readout1_glue", "readout1_Cu", "readout1_kapton", "readout2_glue", "readout2_Cu", "readout2_kapton", "readout3_glue",
           "support_skin1_g10", "support_honeycomb_nomex", "support_skin2_g10"};

    // URWELL position in the CLAS12 frame 
    public final static double DIST2TGT[] = new double[NREGIONS];
    
  //  public final static double DIST2TGT   = (TGT2DC0-URWELL2DC0);
   // public final static double W2TGT = DIST2TGT/Math.cos(Math.toRadians(THTILT-THMIN));
  //  public final static double YMIN = W2TGT*Math.sin(Math.toRadians(THMIN)); // distance from the base chamber1 and beamline
  //  public final static double ZMIN = W2TGT*Math.cos(Math.toRadians(THMIN));   
    public final static double PITCH = 0.1 ;       // cm
    public final static double[] STEREOANGLE = {0, 90, 45};   // deg
    
    
    
    /*
     * @return String a path to a directory in CCDB of the format {@code "/geometry/detector/"}
     */
    public static String getCcdbPath()
    {
            return CCDBPATH;
    }



     /**
     * Loads the the necessary tables for the URWELL geometry for a given DatabaseConstantProvider.
     * 
     * @return DatabaseConstantProvider the same thing
     */
    public static DatabaseConstantProvider connect( DatabaseConstantProvider cp )
    {
          //  cp.loadTable( CCDBPATH +"RWELL");

            load(cp  );
            return cp;
    }

    /**
     * Reads all the necessary constants from CCDB into static variables.
     * Please use a DatabaseConstantProvider to access CCDB and load the following tables:
     * @param cp a ConstantProvider that has loaded the necessary tables
     */
    
    public static synchronized void load( DatabaseConstantProvider cp )
    {
            // read constants from svt table
//            NREGIONS = cp.getInteger( CCDBPATH+"svt/nRegions", 0 );

             for (int i=0; i<NREGIONS; i++){
                
                DIST2TGT[i] = 10 + 10*i;  
  
            }

    
    }


}
