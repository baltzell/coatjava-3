package org.jlab.analysis.efficiency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.Schema;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.utils.json.JsonArray;
import org.jlab.jnp.utils.json.JsonObject;
import org.jlab.utils.options.OptionParser;

/**
 * Efficiency matrix calculator based solely on the MC::GenMatch truth-matching
 * bank (which is purely hit-based), and a pid assignment match in MC::Particle
 * and REC::Particle.
 * 
 * @author baltzell
 */
public class Truth {
   
    static final int UDF = 0;
    static final List<Integer> NEGATIVES = Arrays.asList(11, -211, -321, -2212);
    static final List<Integer> POSITIVES = Arrays.asList(-11, 211, 321, 2212, 45);
    static final List<Integer> NEUTRALS = Arrays.asList(22, 2112);
    static List<Integer> PIDS;

    Schema mcGenMatch;
    Schema mcParticle;
    Schema recParticle;
    long[][] recTallies;
    long[] mcTallies;

    public static void main(String[] args) {
        OptionParser o = new OptionParser("trutheff");
        o.setRequiresInputList(true);
        o.parse(args);
        Truth t = new Truth(o.getInputList().get(0));
        t.add(o.getInputList());
        System.out.println(t.toTable());
        System.out.println(t.toJson());
    }

    public Truth(SchemaFactory s) {
        init(s);
    }

    public Truth(HipoReader r) {
        init(r.getSchemaFactory());
    }

    public Truth(String filename) {
        HipoReader r = new HipoReader();
        r.open(filename);
        init(r.getSchemaFactory());
    }

    private void init(SchemaFactory schema) {
        PIDS = new ArrayList(NEGATIVES);
        PIDS.addAll(POSITIVES);
        PIDS.addAll(NEUTRALS);
        PIDS.add(UDF);
        mcTallies = new long[PIDS.size()];
        recTallies = new long[PIDS.size()][PIDS.size()];
        mcGenMatch = schema.getSchema("MC::GenMatch");
        mcParticle = schema.getSchema("MC::Particle");
        recParticle = schema.getSchema("REC::Particle");
    }

    /**
     * Get one element of the efficiency matrix.
     * @param truth true PID
     * @param rec reconstructed PID
     * @return probability
     */
    public float get(int truth, int rec) {
        long sum = mcTallies[PIDS.indexOf(truth)];
        return sum>0 ? ((float)recTallies[PIDS.indexOf(truth)][PIDS.indexOf(rec)])/sum : 0;
    }

    /**
     * Add an event in the form of truth and reconstructed particle species.
     * @param truth truth PID
     * @param rec reconstructed PID
     */
    public void add(int truth, int rec) {
	final int t = PIDS.indexOf(truth);
        if (t < 0) return;
	final int r = PIDS.indexOf(rec);
        mcTallies[t]++;
        if (r < 0) recTallies[t][UDF]++;
        else recTallies[t][r]++;
    }

    /**
     * Add a HIPO event.
     * @param e 
     */
    public void add(Event e) {
        Bank bm = new Bank(mcParticle);
        Bank br = new Bank(recParticle);
        e.read(bm);
        e.read(br);
        TreeMap<Short,Short> good = getMapping(e);
        for (short row=0; row<bm.getRows(); ++row) {
            if (!good.containsKey(row)) add(bm.getInt("pid",row), UDF);
            else add(bm.getInt("pid",row), br.getInt("pid",good.get(row)));
        }
    }

    /**
     * Add input HIPO files by path.
     * @param filenames
     */
    public void add(List<String> filenames) {
        Event e = new Event();
        for (String f : filenames) {
            HipoReader r = new HipoReader();
            r.open(f);
            while (r.hasNext()) {
                r.nextEvent(e);
                add(e);
            }
        }
    }
   
    /**
     * Truth-matching banks contain pointers to MC::Particle and REC::Particle,
     * and here we cache that mapping to avoid nested loops.
     */
    private TreeMap getMapping(Event e) {
        Bank b = new Bank(mcGenMatch);
        e.read(b);
        TreeMap<Short,Short> m = new TreeMap<>();
        for (int row=0; row<b.getRows(); ++row)
            m.put(b.getShort("mcindex", row), b.getShort("pindex",row));
        return m;
    }

    /**
     * Get efficiencies as a human-readable table.
     * @return 
     */
    public String toTable() {
        StringBuilder s = new StringBuilder();
        s.append("      ");
        for (int i=0; i<PIDS.size(); ++i) {
            s.append(String.format("%7d",PIDS.get(i)));
            if (PIDS.size()==i+1) s.append("\n");
        }
        for (int i=0; i<PIDS.size(); ++i) {
            s.append(String.format("%6d",PIDS.get(i)));
            for (int j=0; j<PIDS.size(); ++j) {
                s.append(String.format("%7.4f",get(PIDS.get(i),PIDS.get(j))));
                if (PIDS.size()==j+1) s.append("\n");
            }
        }
        return s.toString();
    }

    /**
     * Get efficiencies as a JSON object.
     * @return 
     */
    public JsonObject toJson() {
        JsonObject ret = new JsonObject();
        JsonArray pids = new JsonArray();
        JsonArray effs = new JsonArray();
        for (int i=0; i<PIDS.size(); ++i) {
            pids.add(PIDS.get(i));
            JsonArray a = new JsonArray();
            for (int j=0; j<PIDS.size(); ++j)
                a.add(get(PIDS.get(i),PIDS.get(j)));
            effs.add(a);
        }
        ret.add("pids", pids);
        ret.add("effs", effs);
        return ret;
    }
}
