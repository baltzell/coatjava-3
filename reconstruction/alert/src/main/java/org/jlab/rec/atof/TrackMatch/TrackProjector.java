package org.jlab.rec.atof.TrackMatch;

import java.util.ArrayList;
import java.util.List;

import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.clas.tracking.trackrep.Helix;
import org.jlab.clas.tracking.kalmanfilter.Units;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.clas.swimtools.Swim;
import org.jlab.utils.CLASResources;
import cnuphys.magfield.MagneticFields;

/**
 * The {@code TrackProjector} class projects ahdc tracks to the inner surfaces
 * of the bar and wedges of the atof
 *
 * <p>
 * Uses ahdc track bank information (for now position, momentum) Creates a
 * {@link TrackProjection} for each track.
 * </p>
 *
 * <p>
 * TO DO: - replace hardcoded values with database values. - magnetic field ?
 * use swimmer tools? - charge ?
 * </p>
 *
 * @author pilleux
 */
public class TrackProjector {

    /**
     * projections of tracks.
     */
    private List<TrackProjection> Projections;
    
    /**
     * solenoid magnitude
     */
    private Double B;

    /**
     * Default constructor that initializes the list of projections as new empty
     * list and the magnetic field to 5T.
     */
    public TrackProjector() {
        Projections = new ArrayList<TrackProjection>();
        B = 5.0;
    }

    /**
     * Gets the list of track projections.
     *
     * @return a {@link List} of {@link TrackProjection} objects representing
     * the projections.
     */
    public List<TrackProjection> getProjections() {
        return Projections;
    }
    
     /**
     * Gets the solenoid magnitude
     *
     * @return solenoid magnitude
     */
    public Double getB() {
        return B;
    }

    /**
     * Sets the list of track projections.
     *
     * @param Projections a {@link List} of {@link TrackProjection}.
     */
    public void setProjections(List<TrackProjection> Projections) {
        this.Projections = Projections;
    }
    
     /**
     * Sets the solenoid magnitude.
     *
     * @param B a double.
     */
    public void setB(Double B) {
        this.B = B;
    }

    /**
     * Projects the ahdc tracks in the event onto the atof using a {@link Helix}
     * model.
     *
     * @param event the {@link DataEvent} containing track data to be projected.
     */
    public void ProjectTracks(DataEvent event) {//, CalibrationConstantsLoader ccdb) {

        Projections.clear();
        //All of these are in MM
        double bar_innerradius = 77; //NEEDS TO BE REPLACED WITH DB CONSTANTS something like ccdb.INNERRADIUS[0]
        double wedge_innerradius = 80;
        double bar_thickness = 3;
        double wedge_thickness = 20;
        double bar_middle_radius = bar_innerradius + bar_thickness/2;
        double wedge_middle_radius = wedge_innerradius + wedge_thickness/2;   
        
        String track_bank_name = "AHDC::Track";

        if (event == null) { // check if there is an event
            //System.out.print(" no event \n");
        } else if (event.hasBank(track_bank_name) == false) {
            // check if there are ahdc tracks in the event
            //System.out.print("no tracks \n");
        } else {
            DataBank bank = event.getBank(track_bank_name);
            int nt = bank.rows(); // number of tracks 
            TrackProjection projection = new TrackProjection();
            DataBank outputBank = event.createBank("AHDC::Projections", nt);

            for (int i = 0; i < nt; i++) {

                double x = bank.getFloat("x", i);
                double y = bank.getFloat("y", i);
                double z = bank.getFloat("z", i);
                double px = bank.getFloat("px", i);
                double py = bank.getFloat("py", i);
                double pz = bank.getFloat("pz", i);

                int q = 1; //this has to be changed, we need the charge info from tracking

                Units units = Units.MM; //can be MM or CM. 

                double xb = 0;
                double yb = 0;

                Helix helix = new Helix(x, y, z, px, py, pz, q, B, xb, yb, units);

                //Intersection points with the middle of the bar or wedge
                projection.set_BarIntersect(helix.getHelixPointAtR(bar_middle_radius));
                projection.set_WedgeIntersect(helix.getHelixPointAtR(wedge_middle_radius));

                //Path length to the middle of the bar or wedge
                projection.set_BarPathLength((float) helix.getLAtR(bar_middle_radius));
                projection.set_WedgePathLength((float) helix.getLAtR(wedge_middle_radius));
                
                //Path length from the inner radius to the middle radius
                projection.set_BarInPathLength(projection.get_BarPathLength() - (float) helix.getLAtR(bar_innerradius));
                projection.set_WedgeInPathLength(projection.get_WedgePathLength() - (float) helix.getLAtR(wedge_innerradius));

                Projections.add(projection);
                fill_out_bank(outputBank, projection, i);
            }
            event.appendBank(outputBank);
        }
    }

    public static void fill_out_bank(DataBank outputBank, TrackProjection projection, int i) {
        outputBank.setFloat("x_at_bar", i, (float) projection.get_BarIntersect().x());
        outputBank.setFloat("y_at_bar", i, (float) projection.get_BarIntersect().y());
        outputBank.setFloat("z_at_bar", i, (float) projection.get_BarIntersect().z());
        outputBank.setFloat("L_at_bar", i, (float) projection.get_BarPathLength());
        outputBank.setFloat("L_in_bar", i, (float) projection.get_BarInPathLength());
        outputBank.setFloat("x_at_wedge", i, (float) projection.get_WedgeIntersect().x());
        outputBank.setFloat("y_at_wedge", i, (float) projection.get_WedgeIntersect().y());
        outputBank.setFloat("z_at_wedge", i, (float) projection.get_WedgeIntersect().z());
        outputBank.setFloat("L_at_wedge", i, (float) projection.get_WedgePathLength());
        outputBank.setFloat("L_in_wedge", i, (float) projection.get_WedgeInPathLength());
    }

    public static void main(String arg[]) {

        //READING MAG FIELD MAP
        System.setProperty("CLAS12DIR", "../../");

        String mapDir = CLASResources.getResourcePath("etc") + "/data/magfield";
        try {
            MagneticFields.getInstance().initializeMagneticFields(mapDir,
                    "Symm_torus_r2501_phi16_z251_24Apr2018.dat", "Symm_solenoid_r601_phi1_z1201_13June2018.dat");
        } catch (Exception e) {
            e.printStackTrace();
        }

        float[] b = new float[3];
        Swim swimmer = new Swim();
        swimmer.BfieldLab(0, 0, 0, b);
        double B = Math.abs(b[2]);

        //Track Projector Initialisation with B field
        TrackProjector projector = new TrackProjector();
        projector.setB(B);
        
        //Input to be read
        String input = "/Users/npilleux/Desktop/alert/atof-reconstruction/coatjava/reconstruction/alert/src/main/java/org/jlab/rec/atof/Hit/update_protons_to_test_with_tracks.hipo";
        HipoDataSource reader = new HipoDataSource();
        reader.open(input);
        int event_number = 0;
        while (reader.hasEvent()) {
            DataEvent event = (DataEvent) reader.getNextEvent();
            event_number++;

            projector.ProjectTracks(event);
            event.getBank("AHDC::Projections").show();
        }

        System.out.print("Read " + event_number + " events");
    }
}