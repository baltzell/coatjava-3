package org.jlab.detector.pulse;

import org.jlab.detector.base.DetectorDescriptor;

/**
 * Just a dumb data container
 */
public class Pulse {

	// standard information
    public DetectorDescriptor descriptor;
    public float pedestal;
    public long flags;
    public int id;

    // Standard information
	public int binMax; //Bin of the max ADC over the pulse
	public int binOffset; //Offset due to sparse sample
	public float adcMax; //Max value of ADC over the pulse (fitted)
	public float time; //Time of the max ADC over the pulse (fitted) // I would like to rename it timeMax
	public float integral; //Sum of ADCs over the pulse (not fitted)
	public long timestamp;

	// New information
	public float timeRiseCFA; // moment when the signal reaches a Constant Fraction of its Amplitude uphill (fitted)
	public float timeFallCFA; // moment when the signal reaches a Constant Fraction of its Amplitude downhill (fitted)
	public float timeOverThresholdCFA; // is equal to (timeFallCFA - timeRiseCFA)
	public float timeCFD; // time extracted using the Constant Fraction Discriminator (CFD) algorithm (fitted)

    /**
     * Units are the same as the raw units of the samples.
     * @param integral pulse integral, pedestal-subtracted
     * @param time pulse time
     * @param flags user flags
     * @param id link to row in source bank
     */
    public Pulse(float integral, float time, long flags, int id) {
        this.integral = integral;
        this.time = time;
        this.flags = flags;
        this.id = id;
    }
	
	public Pulse(){}

    @Override
    public String toString() {
        return String.format("pulse: integral=%f time=%f flags=%d id=%d",
                integral, time, flags, id);
    }

}    
