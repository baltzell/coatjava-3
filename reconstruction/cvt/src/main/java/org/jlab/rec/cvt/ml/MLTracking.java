/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jlab.rec.cvt.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.cvt.banks.RecoBankWriter;
import org.jlab.rec.cvt.cluster.Cluster;
import org.jlab.rec.cvt.cross.Cross;
import org.jlab.rec.cvt.services.TracksFromTargetRec;
import org.jlab.rec.cvt.track.Seed;
import org.jlab.rec.cvt.track.Track;

/**
 *
 * @author ziegler
 */
public class MLTracking extends CVTInitializer {
   
    @Override
    public boolean processDataEvent(DataEvent event) {
        super.processDataEvent(event);
        return true;
    }

    @Override
    public boolean init() {
        super.setOutputBankPrefix("Rec");
        double[] aistatus = new double[]{10};
        super.setAistatus(aistatus);
        super.init();
        return true;
    }
    
    
    
}
