/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jlab.clas.reco;

import j4np.data.base.DataActor;
import j4np.data.base.DataActorStream;
import j4np.data.base.DataFrame;
import j4np.data.base.DataWorker;
import j4np.hipo5.data.Event;
import j4np.hipo5.io.HipoReader;
import j4np.hipo5.io.HipoWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author gavalian
 */
public class EngineStream {
    public static DataFrame createFrames(int count){
        DataFrame<Event>  frame = new DataFrame<>();        
        for(int i = 0; i < count; i++) frame.addEvent(new Event());
        return frame;
    }
    
    public static List<DataActor>  createActors(int nactors, int nframes, List<DataWorker> workers){
        List<DataActor> actors = new ArrayList<>();
        for(int a = 0; a < nactors; a++){
            DataActor actor = new DataActor();
            DataFrame frame = EngineStream.createFrames(nframes);
            actor.setWorkes(workers);
            actor.setDataFrame(frame);
            actors.add(actor);
        }
        return actors;
    }

    public static void main(String[] args){
        EngineStreamWorker worker = new EngineStreamWorker();
        worker.initYAML("data-cv.yaml");
        
        String output = "cooked.h5";
        String  input = args[0];
        
        List<DataActor> actors = EngineStream.createActors(6, 16, Arrays.asList(worker));
        
        HipoReader r = new HipoReader(input);
        HipoWriter w = HipoWriter.create(output, r);
        
        DataActorStream stream = new DataActorStream();
        
        stream.addActor(actors);
        stream.setSource(r).setSync(w);
        
        stream.run();
    }
}
