/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jlab.clas.reco;

import j4np.data.base.DataEvent;
import j4np.data.base.DataSource;
import j4np.data.base.DataWorker;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.utils.ClaraYaml;
import org.jlab.utils.options.OptionParser;
import org.json.JSONObject;

/**
 *
 * @author gavalian
 */
public class EngineStreamWorker extends DataWorker {
    //private EngineConsumer  engineConsumer = null;
    private static final Logger LOGGER = Logger.getLogger(EngineStreamWorker.class.getPackage().getName());
    private final Map<String,ReconstructionEngine>  processorEngines = new LinkedHashMap<>();
   
    
    public int threads = 4;
    public int  frames = 64;
    public int  maxEvents = -1;
    
    public String inputSteamFileName  = "input.h5";
    public String outputSteamFileName = "output.h5";
    public SchemaFactory factory = null;
  
    public EngineStreamWorker(){
        factory = new SchemaFactory();
         String env = System.getenv("CLAS12DIR");
        factory.initFromDirectory( env +  "/etc/bankdefs/hipo4");
        
    }
    
    public void addEngine(String name, String clazz, String jsonConf) {
            Class c;
            try {
                c = Class.forName(clazz);
                if( ReconstructionEngine.class.isAssignableFrom(c)==true){
                    ReconstructionEngine engine = (ReconstructionEngine) c.newInstance();
                    if(jsonConf != null && !jsonConf.equals("null")) {
                        EngineData input = new EngineData();
                        input.setData(EngineDataType.JSON.mimeType(), jsonConf);
                        engine.configure(input);
                    }
                    else {
                        engine.init();
                    }
                    this.processorEngines.put(name == null ? engine.getName() : name, engine);
                } else {
                    LOGGER.log(Level.SEVERE, ">>>> ERROR: class is not a reconstruction engine : {0}", clazz);
                }

            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    
        private void setBanksToKeep(String schemaDirectory) {
            if (!Files.isDirectory((new File(schemaDirectory)).toPath())) {
                LOGGER.log(Level.SEVERE, "Invalid schema directory, aborting:  "+schemaDirectory);
                System.exit(1);
            }
            LOGGER.log(Level.INFO, "Using schema directory:  "+schemaDirectory);
            //banksToKeep = new SchemaFactory();
            //banksToKeep.initFromDirectory(schemaDirectory);
        }
        
       
        
        public void addEngine(String name, String clazz) {
            this.addEngine(name, clazz, null);
        }
        /*
        public void addMonitor(DataWorker w){
            this.dataMonitor.add(w);
        }
        public EngineStream setDataSource(DataSource src){
            this.dataSource = src; return this;
        }*/
        /*
        public EngineStream setDataSync(DataSync sync){
            this.dataSync = sync; return this;
        }*/
        
        public void initYAML(String yamlFile){

            ClaraYaml yaml = new ClaraYaml(yamlFile);
            if (yaml.schemaDirectory() != null) {
                this.setBanksToKeep(yaml.schemaDirectory());
            }
            for (JSONObject service : yaml.services()) {
                JSONObject cfg = yaml.filter(service.getString("name"));
                System.out.printf("----- include --- : %s %s\n",
                        service.getString("name"),service.getString("class"));
                if (cfg.length() > 0) {
                    this.addEngine(service.getString("name"),service.getString("class"),cfg.toString());
                } else {
                    addEngine(service.getString("name"),service.getString("class"));
                }
            }
        }
        /*
        public void initDictionary(){
            org.jlab.jnp.hipo4.data.SchemaFactory  engineDictionary = new org.jlab.jnp.hipo4.data.SchemaFactory();
            String env = System.getenv("CLAS12DIR");
            engineDictionary.initFromDirectory( env +  "/etc/bankdefs/hipo4");
            engineDictionary.show();
            for(Map.Entry<String,ReconstructionEngine> entry : this.processorEngines.entrySet()){
                entry.getValue().setEngineDictionary(engineDictionary);
            }
        }*/
        
        
        @Override
        public boolean init(DataSource src) {
            return true;
        }

        @Override
        public void execute(DataEvent e) {
            j4np.hipo5.data.Event event = (j4np.hipo5.data.Event) e;
            int size = event.getEventBufferSize();
            HipoDataEvent hipoEvent = new HipoDataEvent(event.getBuffer().array(),factory);
            for(Map.Entry<String,ReconstructionEngine> engine : this.processorEngines.entrySet()){
                try {
                    engine.getValue().processDataEvent(hipoEvent);
                } catch (Exception ex){
                    LOGGER.log(Level.SEVERE, "[Exception] >>>>> engine : {0}\n\n", engine.getKey());
                    ex.printStackTrace();
                }
            }
            
            event.initFrom(hipoEvent.getHipoEvent().getEventBuffer().array());
        }
        
        
        public static void main(String[] args){
            
            EngineStreamWorker worker = new EngineStreamWorker();
            worker.initYAML("data-cv.yaml");
            /*OptionParser parser = new OptionParser("recon-stream");
            parser.addRequired("-o","output.hipo");
            parser.addRequired("-i","input.hipo");
            parser.setRequiresInputList(false);
            parser.addOption("-c","0","use default configuration [0 - no, 1 - yes/default, 2 - all services] ");
            parser.addOption("-s","-1","number of events to skip");
            parser.addOption("-n","-1","number of events to process");
            parser.addOption("-t","4","number of threads");
            parser.addOption("-y","0","yaml file");
            
            parser.parse(args);
            EngineStreamWorker stream = new EngineStreamWorker();
            
            stream.inputSteamFileName = parser.getOption("-i").stringValue();
            stream.outputSteamFileName = parser.getOption("-o").stringValue();
            stream.initYAML(parser.getOption("-y").stringValue());
            */
            
            //stream.initDecoder();
            
            //Evio2HipoSource source = new Evio2HipoSource();
            //source.open(stream.inputSteamFileName);    
            //HipoWriter sync = HipoWriter.withDictionary("CLAS12DIR", "etc/bankdefs/hipo4");
            //sync.open(stream.outputSteamFileName);
            //stream.setDataSource(source).setDataSync(sync);
            //stream.process(parser);
            
        }

    
}
