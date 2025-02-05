package org.jlab.service.mltn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jlab.clas.reco.ReconstructionEngine;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.CompositeBank;

/**
 * This service, will save the minimal information from DC tracking that can be
 * used to train AI models and run validations.
 * @author gavalian
 */
public class MLDCEngine extends ReconstructionEngine {
    
    
    public MLDCEngine(){
        super("MLDC","gavalian","1.0");
    }
    
    @Override
    public boolean init() {
        return true;
    }
    
    @Override
    public boolean processDataEvent(DataEvent de) {
        
        List<ByteBuffer> trackBuffers = new ArrayList<>();
        
        if(de.hasBank("TimeBasedTrkg::TBTracks")&&de.hasBank("TimeBasedTrkg::TBClusters")){
            DataBank   tbt = de.getBank("TimeBasedTrkg::TBTracks");
            DataBank   tbc = de.getBank("TimeBasedTrkg::TBClusters");
            ByteBuffer bb = getTracks(tbt,tbc,10);
            trackBuffers.add(bb);
        }
        
        if(de.hasBank("TimeBasedTrkg::AITracks")&&de.hasBank("TimeBasedTrkg::AIClusters")){
            DataBank   tbt = de.getBank("TimeBasedTrkg::AITracks");
            DataBank   tbc = de.getBank("TimeBasedTrkg::AIClusters");
            ByteBuffer bb = getTracks(tbt,tbc,20);
            trackBuffers.add(bb);
        }
        
        if(trackBuffers.size()>0){
            int[] lengths = new int[trackBuffers.size()];
            int[] offsets = new int[trackBuffers.size()];
            int size = 0;
            for(int k = 0; k < offsets.length; k++){                
                lengths[k] = trackBuffers.get(k).array().length;
                offsets[k] = size;
                size += trackBuffers.get(k).array().length;
            }
            byte[] buffer = new byte[size];
            for(int k = 0; k < trackBuffers.size(); k++){
                System.arraycopy(trackBuffers.get(k).array(), 0, buffer, offsets[k], lengths[k]);
            }
            DataBank output = de.createBank("MLDC::tracks", buffer.length);
            for(int j = 0; j < buffer.length; j++)
                output.setByte("bytes", j,buffer[j]);
            de.appendBank(output);
        }
        
        if(de.hasBank("DC::tdc")==true){
            DataBank   bank = de.getBank("DC::tdc");
            DataBank output = de.createBank("MLDC::dc", bank.rows());
            
            HipoDataBank hbank = (HipoDataBank) bank;
            byte[]  sector = hbank.getByte("sector");
            byte[]   layer = hbank.getByte("layer");
            short[]   wire = hbank.getShort("component");
            int[]      tdc = hbank.getInt("TDC");
            
            for(int j = 0; j < bank.rows(); j++){
                int id = (sector[j]-1)*(112*36) + (layer[j]-1)*112 + (wire[j]-1);
                output.setShort("id", j, (short) id);
                output.setShort("value", j, (short) tdc[j]);
            }          
            de.appendBank(output);
        }
        return true;
    }
    
    public static Map<Integer,Integer> getMap(DataBank bank){
        Map<Integer,Integer> map = new HashMap<>();
        int[] ids = bank.getInt("id");
        for(int j = 0; j < ids.length; j++)
            map.put(ids[j], j);
        return map;
    }
    
    
    public static ByteBuffer getTracks(DataBank trkg, DataBank clusters, int status){
        Map<Integer,Integer> map = getMap(clusters);
        int size = trkg.rows();
        int bsize = 110;
        byte[] bytes = new byte[size*bsize];
        ByteBuffer b = ByteBuffer.wrap(bytes);
        b.order(ByteOrder.LITTLE_ENDIAN);
        HipoDataBank ht = (HipoDataBank) trkg;
        HipoDataBank hc = (HipoDataBank) clusters;
        int[] cid = new int[6];
        for(int j = 0; j < size; j++){
            int offset = j*bsize;
            int charge = (int) ht.getByte("q",j);
            int     ps = status + (charge<0?2:1);
            b.putShort(offset+0, (short) ps);
            b.putFloat(offset+2, 0.0f);
            b.putShort(offset+6, (short) ht.getByte("sector",j));
            b.putShort(offset+8, (short) ht.getByte("q",j));
            b.putFloat(offset+10, ht.getFloat("chi2", j));
            b.putFloat(offset+14, ht.getFloat("p0_x", j));
            b.putFloat(offset+18, ht.getFloat("p0_y", j));
            b.putFloat(offset+22, ht.getFloat("p0_z", j));
            
            b.putFloat(offset+26, ht.getFloat("Vtx0_x", j));
            b.putFloat(offset+30, ht.getFloat("Vtx0_y", j));
            b.putFloat(offset+34, ht.getFloat("Vtx0_z", j));
            
            cid[0] = ht.getShort("Cluster1_ID", j);
            cid[1] = ht.getShort("Cluster2_ID", j);
            cid[2] = ht.getShort("Cluster3_ID", j);
            cid[3] = ht.getShort("Cluster4_ID", j);
            cid[4] = ht.getShort("Cluster5_ID", j);
            cid[5] = ht.getShort("Cluster6_ID", j);
            
            b.putInt(offset+38, cid[0]);
            b.putInt(offset+42, cid[1]);
            b.putInt(offset+46, cid[2]);
            b.putInt(offset+50, cid[3]);
            b.putInt(offset+54, cid[4]);
            b.putInt(offset+58, cid[5]);

            
            for(int i = 0; i < 6; i++){
                if(map.containsKey(cid[i])==true){
                    float avg = hc.getFloat("avgWire", map.get(cid[i]));
                    b.putFloat(offset+i*4+62, avg);
                    b.putFloat(offset+i*4+62+4*6, avg);
                } else { 
                    b.putFloat(offset+i*4+62, 0.0f);
                    b.putFloat(offset+i*4+62+4*6, 0.0f);
                }
            }
        }
        return b;
    }
}
