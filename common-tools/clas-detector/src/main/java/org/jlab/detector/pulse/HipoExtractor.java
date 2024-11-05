
package org.jlab.detector.pulse;

import java.util.ArrayList;
import java.util.List;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.utils.groups.NamedEntry;

/**
 * For now, a place to store standard boilerplate for waveform/pulse HIPO
 * manipulations.  No bounds checking regarding number of samples.
 *
 * Here an IndexedTable object from CCDB is used to pass initialization parameters
 * to the extractor.  If that object is null, the @{link org.jlab.detector.pulse.IExtractor.extract}
 * method should know what to do, e.g., hardcoded, channel-independent parameters.
 * 
 * FIXME:  Passing the #samples around is obviously bad, and there's probably a
 * few non-horrible ways that can be addressed without changing bank format.
 * 
 * @author baltzell
 */
public abstract class HipoExtractor implements IExtractor {

    /**
     * @param n number of samples in readout
     * @param it CCDB table containing extraction initialization parameters
     * @param event the event to modify
     * @param schema bank schema factory
     * @param wfBankName name of the input waveform bank
     * @param adcBankName name of the output ADC bank
     */
    public void update(int n, IndexedTable it, Event event, SchemaFactory schema, String wfBankName, String adcBankName) {
        Bank wf = new Bank(schema.getSchema(wfBankName));
        event.read(wf);
        if (wf.getRows() > 0) {
            Bank adc = new Bank(schema.getSchema(adcBankName));
            update(n, it, wf, adc);
            event.remove(schema.getSchema(adcBankName));
            if (adc.getRows() > 0) event.write(adc);
        }
    }

    /**
     * This could be overriden, e.g., for non-standard ADC banks.
     * @param n number of samples in readout
     * @param it CCDB table containing extraction initialization parameters
     * @param event the event to modify
     * @param wfBankName name of the input waveform bank
     * @param adcBankName name of the output ADC bank
     */
    public void update(int n, IndexedTable it, DataEvent event, String wfBankName, String adcBankName) {
        DataBank wf = event.getBank(wfBankName);
        if (wf.rows() > 0) {
            event.removeBank(adcBankName);
            List<Pulse> pulses = getPulses(n, it, wf);
            if (pulses != null && !pulses.isEmpty()) {
                DataBank adc = event.createBank(adcBankName, pulses.size());
                for (int i=0; i<pulses.size(); ++i) {
                    copyIndices(wf, adc, i, i);
                    adc.setInt("ADC", i, (int)pulses.get(i).integral);
                    adc.setFloat("time", i, pulses.get(i).time);
                }
                event.appendBank(adc);
            }
        }
    }

    /**
     * This could be overriden, e.g., for non-standard ADC banks.
     * @param n number of samples in readout
     * @param it CCDB table containing extraction initialization parameters
     * @param wfBank input waveform bank
     * @param adcBank  output ADC bank
     */
    protected void update(int n, IndexedTable it, Bank wfBank, Bank adcBank) {
        if (wfBank.getRows() > 0) {
            List<Pulse> pulses = getPulses(n, it, wfBank);
            adcBank.reset();
            adcBank.setRows(pulses!=null ? pulses.size() : 0);
            if (pulses!=null && !pulses.isEmpty()) {
                for (int i=0; i<pulses.size(); ++i) {
                    copyIndices(wfBank, adcBank, pulses.get(i).id, i);
                    adcBank.putInt("ADC", i, (int)pulses.get(i).integral);
                    adcBank.putFloat("time", i, pulses.get(i).time);
                }
            }
        }
    }

    private static void copyIndices(Bank src, Bank dest, int isrc, int idest) {
        dest.putByte("sector", idest, src.getByte("sector",isrc));
        dest.putByte("layer", idest, src.getByte("layer",isrc));
        dest.putShort("component", idest, src.getShort("component",isrc));
        dest.putByte("order", idest, src.getByte("order",isrc));
        dest.putShort("id", idest, (short)isrc);
    }

    private static void copyIndices(DataBank src, DataBank dest, int isrc, int idest) {
        dest.setByte("sector", idest, src.getByte("sector",isrc));
        dest.setByte("layer", idest, src.getByte("layer",isrc));
        dest.setShort("component", idest, src.getShort("component",isrc));
        dest.setByte("order", idest, src.getByte("order",isrc));
        dest.setShort("id", idest, (short)isrc);
    }

    private static int[] getIndices(Bank bank, int row) {
        return new int[] {
            bank.getShort("sector", row),
            bank.getShort("layer", row),
            bank.getShort("component", row),
            bank.getShort("order", row)};
    }

    private static int[] getIndices(DataBank bank, int row) {
        return new int[] {
            bank.getShort("sector", row),
            bank.getShort("layer", row),
            bank.getShort("component", row),
            bank.getShort("order", row)};
    }

    private List<Pulse> getPulses(int n, IndexedTable it, DataBank wfBank) {
        List<Pulse> pulses = null;
        short[] samples = new short[n];
        for (int i=0; i<wfBank.rows(); ++i) {
            for (int j=0; j<n; ++j)
                samples[j] = wfBank.getShort(String.format("s%d",j+1), i);
            List<Pulse> p = it==null ? extract(null, i, samples) :
                extract(it.getNamedEntry(getIndices(wfBank,i)), i, samples);
            if (p!=null && !p.isEmpty()) {
                if (pulses == null) pulses = new ArrayList<>();
                pulses.addAll(p);
            }
        }
        return pulses;
    }

    private List<Pulse> getPulses(int n, IndexedTable it, Bank wfBank) {
        List<Pulse> pulses = null;
        short[] samples = new short[n];
        for (int i=0; i<wfBank.getRows(); ++i) {
            for (int j=0; j<n; ++j)
                samples[j] = wfBank.getShort(String.format("s%d",j+1), i);
                // FIXME:  Can speed this up (but looks like not for DataBank?):
                //samples[j] = wfBank.getShort(String.format(5+j,j+1), i);
            List p = it==null ? extract(null, i, samples) :
                extract(it.getNamedEntry(getIndices(wfBank,i)), i, samples);
            if (p!=null && !p.isEmpty()) {
                if (pulses == null) pulses = new ArrayList<>();
                pulses.addAll(p);
            }
        }
        return pulses;
    }
	
	/**
	 * The following blocks of code implement the extract() method.
	 *
	 * They are inspired by the MVTFitter.java, the previous 
	 * decoding for BMT.
	 *
	 * Can be improved !
	 *
	 * Any questions : @ftouchte
	 */
	
	// Standard attributs to be filled // there are used as calculation intermediaries
	private int binMax; //Bin of the max ADC over the pulse
	private int binOffset; //Offset due to sparse sample
	private float adcMax; //Max value of ADC over the pulse (fitted)
	private float timeMax; //Time of the max ADC over the pulse (fitted)
	private float integral; //Sum of ADCs over the pulse (not fitted)
	private long timestamp;

	private short[] samplesCorr; //Waveform after offset (pedestal) correction
	private int binNumber; //Number of bins in one waveform

	// New attributs to be filled // there are used as calculation intermediaries
	private float timeRiseCFA; // moment when the signal reaches a Constant Fraction of its Amplitude uphill (fitted)
	private float timeFallCFA; // moment when the signal reaches a Constant Fraction of its Amplitude downhill (fitted)
	private float timeOverThresholdCFA; // is equal to (timeFallCFA - timeRiseCFA)
	private float timeCFD; // time extracted using the Constant Fraction Discriminator (CFD) algorithm (fitted)

	// Setting parameters // Should ideally be arguments in the extarct() methods by comparison to MVTFitter.java
	public float samplingTime;
	public int sparseSample;
	public short adcOffset; 
	public long timeStamp;
	public float fineTimeStampResolution;
	public static final short ADC_LIMIT = 4095; // 2^12-1
	public float amplitudeFractionCFA;
	public int binDelayCFD;
	public float fractionCFD;
	
	/**
	 * This method extracts relevant informations from the digitized signal
	 * (the samples) and store them in a Pulse
	 *
	 * The above private attributs are used as calculation intermediaries
	 * @param samples : waveform samples stored in an array of short
	 * @param id : to be filled
	 * @param pars : to be filled
	 */
	public List<Pulse> extract(NamedEntry pars, int id, short... samples){
		waveformCorrection(adcOffset,samplingTime,samples,sparseSample);
		fitAverage(samplingTime);
		computeTimeAtConstantFractionAmplitude(samplingTime,amplitudeFractionCFA);
		computeTimeUsingConstantFractionDiscriminator(samplingTime,fractionCFD,binDelayCFD);
		fineTimeStampCorrection(timeStamp,fineTimeStampResolution);
		// output
		Pulse pulse = new Pulse();
		pulse.adcMax = adcMax;
		pulse.time = timeMax;
		pulse.timestamp = timestamp;
		pulse.integral = integral;
		pulse.timeRiseCFA = timeRiseCFA;
		pulse.timeFallCFA = timeFallCFA;
		pulse.timeOverThresholdCFA = timeOverThresholdCFA;
		pulse.timeCFD = timeCFD;
		pulse.binMax = binMax;
		pulse.binOffset = binOffset;
		pulse.pedestal = adcOffset;
		List<Pulse> output = new ArrayList<>();
		output.add(pulse);
		return output;
	}

	/**
	* This method subtracts the pedestal (noise) from samples and stores it in : samplesCorr
	* It also computes a first value for : adcMax, binMax, timeMax and integral
	* This code is inspired by the one of MVTFitter.java
	* @param adcOffset : pedestal or noise level
	* @param samplingTime : time between two adc bins
	* @param sparseSample : to define binOffset
	*/
    private void waveformCorrection(short adcOffset, float samplingTime, short[] samples, int sparseSample){
        binNumber = samples.length;
        binMax = 0;
        adcMax = (short) (samples[0] - adcOffset);
        integral = 0;
        samplesCorr = new short[binNumber];
        for (int bin = 0; bin < binNumber; bin++){
                samplesCorr[bin] = (short) (samples[bin] - adcOffset);
                if (adcMax < samplesCorr[bin]){
                        adcMax = samplesCorr[bin];
                        binMax = bin;
                }
                integral += samplesCorr[bin];
        }
        /**
         * If adcMax == ADC_LIMIT, that means there is saturation
         * In that case, binMax is the middle of the first plateau
         * This convention can be changed
         */
        if ((short) adcMax == ADC_LIMIT) {
                int binMax2 = binMax;
                for (int bin = binMax; bin < binNumber; bin++){
                        if (samplesCorr[bin] == ADC_LIMIT) {
                                binMax2 = bin;
                        }
                        else {
                                break;
                        }
                }
                binMax = (binMax + binMax2)/2;
        }
        binOffset = sparseSample*binMax;
        timeMax = (binMax + binOffset)*samplingTime;
	}

	/**
	 * This method gives a more precise value of the max of the waveform by computing the average of five points around the binMax
	 * It is an alternative to fitParabolic()
	 * The suitability of one of these fits can be the subject of a study
	 * Remark : This method updates adcMax but doesn't change timeMax
	 * @param samplingTime : time between 2 ADC bins
	 */
	private void fitAverage(float samplingTime){
			if ((binMax - 2 >= 0) && (binMax + 2 <= binNumber - 1)){
					adcMax = 0;
					for (int bin = binMax - 2; bin <= binMax + 2; bin++){
							adcMax += samplesCorr[bin];
					}
					adcMax = adcMax/5;
			}
	}

	/**
	 * Fit the max of the pulse using parabolic fit, this method updates the timeMax and adcMax values
	 * @param samplingTime : time between 2 ADC bins
	 */
	private void fitParabolic(float samplingTime) {

	}

	/**
	 * From MVTFitter.java
	 * Make fine timestamp correction (using dream (=electronic chip) clock)
	 * @param timeStamp : timing informations (used to make fine corrections)
	 * @param fineTimeStampResolution : precision of dream clock (usually 8)
	 */
	private void fineTimeStampCorrection (long timeStamp, float fineTimeStampResolution) {
			this.timestamp = timeStamp;
			String binaryTimeStamp = Long.toBinaryString(timeStamp); //get 64 bit timestamp in binary format
			if (binaryTimeStamp.length()>=3){
					byte fineTimeStamp = Byte.parseByte(binaryTimeStamp.substring(binaryTimeStamp.length()-3,binaryTimeStamp.length()),2); //fineTimeStamp : keep and convert last 3 bits of binary timestamp
					timeMax += (float) ((fineTimeStamp+0.5) * fineTimeStampResolution); //fineTimeStampCorrection
					// Question : I wonder if I have to do the same thing of all time quantities that the extract() methods compute. 
			}
	}

	/**
	 * This method determines the moment when the signal reaches a Constant Fraction of its Amplitude (i.e fraction*adcMax)
	 * It fills the attributs : timeRiseCFA, timeFallCFA, timeOverThresholdCFA
	 * @param samplingTime : time between 2 ADC bins
	 * @param amplitudeFraction : a float between 0 and 1
	 */
	private void computeTimeAtConstantFractionAmplitude(float samplingTime, float amplitudeFractionCFA){
			float threshold = amplitudeFractionCFA*adcMax;
			// timeRiseCFA
			int binRise = 0;
			for (int bin = 0; bin < binMax; bin++){
					if (samplesCorr[bin] < threshold)
							binRise = bin;  // last pass below threshold and before adcMax
			} // at this stage : binRise < timeRiseCFA/samplingTime <= binRise + 1 // timeRiseCFA is determined by assuming a linear fit between binRise and binRise + 1
			float slopeRise = 0;
			if (binRise + 1 <= binNumber-1)
					slopeRise = samplesCorr[binRise+1] - samplesCorr[binRise];
			float fittedBinRise = (slopeRise == 0) ? binRise : binRise + (threshold - samplesCorr[binRise])/slopeRise;
			timeRiseCFA = (fittedBinRise + binOffset)*samplingTime; // binOffset is determined in wavefromCorrection() // must be the same for all time ? // or must be defined using fittedBinRise*sparseSample

			// timeFallCFA
			int binFall = binMax;
			for (int bin = binMax; bin < binNumber; bin++){
					if (samplesCorr[bin] > threshold){
							binFall = bin;
					}
					else {
							binFall = bin;
							break; // first pass below the threshold
					}
			} // at this stage : binFall - 1 <= timeRiseCFA/samplingTime < binFall // timeFallCFA is determined by assuming a linear fit between binFall - 1 and binFall
			float slopeFall = 0;
			if (binFall - 1 >= 0)
					slopeFall = samplesCorr[binFall] - samplesCorr[binFall-1];
			float fittedBinFall = (slopeFall == 0) ? binFall : binFall + (threshold - samplesCorr[binFall-1])/slopeFall;
			timeFallCFA = (fittedBinFall + binOffset)*samplingTime;

			// timeOverThreshold
			timeOverThresholdCFA = timeFallCFA - timeRiseCFA;
	}

	/**
	 * This methods extracts a time using the Constant Fraction Discriminator (CFD) algorithm
	 * It fills the attribut : timeCFD
	 * @param samplingTime : time between 2 ADC bins
	 * @param amplitudeFraction : a float between 0 and 1, CFD fraction parameter
	 * @param binDelay : CFD delay parameter
	 */
	private void computeTimeUsingConstantFractionDiscriminator(float samplingTime, float fractionCFD, int binDelayCFD){
			float[] signal = new float[binNumber];
			// signal generation
			for (int bin = 0; bin < binNumber; bin++){
					signal[bin] = (1 - fractionCFD)*samplesCorr[bin]; // we fill it with a fraction of the original signal
					if (bin < binNumber - binDelayCFD)
							signal[bin] += -1*fractionCFD*samplesCorr[bin + binDelayCFD]; // we advance and invert a complementary fraction of the original signal and superimpose it to the previous signal
			}
			// determine the two humps
			int binHumpSup = 0;
			int binHumpInf = 0;
			for (int bin = 0; bin < binNumber; bin++){
					if (signal[bin] > signal[binHumpSup])
							binHumpSup = bin;
			}
			for (int bin = 0; bin < binHumpSup; bin++){ // this loop has been added to be sure : binHumpInf < binHumpSup
					if (signal[bin] < signal[binHumpInf])
							binHumpInf = bin;
			}
			// research for zero
			int binZero = 0;
			for (int bin = binHumpInf; bin <= binHumpSup; bin++){
					if (signal[bin] < 0)
							binZero = bin; // last pass below zero
			} // at this stage : binZero < timeCFD/samplingTime <= binZero + 1 // timeCFD is determined by assuming a linear fit between binZero and binZero + 1
			float slopeCFD = 0;
			if (binZero + 1 <= binNumber)
					slopeCFD = signal[binZero+1] - signal[binZero];
			float fittedBinZero = (slopeCFD == 0) ? binZero : binZero + (0 - signal[binZero])/slopeCFD;
			timeCFD = (fittedBinZero + binOffset)*samplingTime;

	}


}
