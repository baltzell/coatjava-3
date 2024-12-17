package org.jlab.analysis.nabo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.Schema;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoReader;

public class Efficiency {

    static final List<Integer> NEGATIVES = Arrays.asList(11, -211, -321, -2212);
    static final List<Integer> POSITIVES = Arrays.asList(11, 211, 321, 2212);
    static final List<Integer> NEUTRALS = Arrays.asList(22, 2112);
    static List<Integer> PIDS;
    
    Schema mcGenMatch;
    Schema mcParticle;
    Schema recParticle;
    int[][] caches;

    public static void main(String[] args) {
        Efficiency e = new Efficiency(args);
        System.out.println(e);
    }

    private void init(SchemaFactory schema) {
        PIDS = new ArrayList(NEGATIVES);
        PIDS.addAll(POSITIVES);
        PIDS.addAll(NEUTRALS);
        caches = new int[PIDS.size()][PIDS.size()];
        mcGenMatch = schema.getSchema("MC::GenMatch");
        mcParticle = schema.getSchema("MC::Particle");
        recParticle = schema.getSchema("REC::Particle");
    }

    public Efficiency(SchemaFactory schema) {
        init(schema);
    }

    public Efficiency(String... filenames) {
        HipoReader r = new HipoReader();
        r.open(filenames[0]);
        init(r.getSchemaFactory());
        r.close();
        add(filenames);
    }

    public float get(int mc, int rec) {
        long sum = IntStream.of(caches[PIDS.indexOf(mc)]).sum();
        return sum>0 ? caches[PIDS.indexOf(mc)][PIDS.indexOf(rec)]/sum : 0;
    }

    public float get(int pid) {
        return get(pid,pid);
    }

    @Override
    public String toString() {
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
    
    public void add(int mc, int rec) {
        if (PIDS.contains(mc)) {
            if (!PIDS.contains(rec)) add(mc);
            else caches[PIDS.indexOf(mc)][PIDS.indexOf(rec)]++;
        }
    }

    public void add(int mc) {
        add(mc, 0);
    }

    public void add(Event e) {
        Bank bm = new Bank(mcParticle);
        Bank br = new Bank(recParticle);
        e.read(bm);
        e.read(br);
        TreeMap<Short,Short> good = getMapping(e);
        for (short row=0; row<bm.getRows(); ++row) {
            if (!good.containsKey(row)) add(bm.getInt("pid",row));
            else add(bm.getInt("pid",row), br.getInt("pid",good.get(row)));
        }
    }

    public void add(String... filename) {
        Event e = new Event();
        for (String f : filename) {
            HipoReader r = new HipoReader();
            r.open(f);
            while (r.hasNext()) {
                r.nextEvent(e);
                add(e);
            }
        }
    }
    
    private TreeMap getMapping(Event e) {
        Bank b = new Bank(mcGenMatch);
        e.read(b);
        TreeMap<Short,Short> m = new TreeMap<>();
        for (int row=0; row<b.getRows(); ++row)
            m.put(b.getShort("mcindex", row),b.getShort("pindex",row));
        return m;
    }

}
