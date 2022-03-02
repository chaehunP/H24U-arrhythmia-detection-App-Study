/**
 * DataSource.java
 * Copyright (C) 2012 Pattern Recognition Lab, University Erlangen-Nuremberg.
 *  
 * Licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License (the "License");
 * you may not use this file except in compliance with the License.
 * To view a copy of this License, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on 
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 * 
 * 
 * This file is part of the "Hearty" Android Application. It was released as supplementary material related to the publication [1]:
 * [1] S. Gradl, P. Kugler, C. Lohmüller, and B. Eskofier, “Real-time ECG monitoring and arrhythmia detection using Android-based mobile devices,” in 34th Annual International Conference of the IEEE EMBS, 2012, pp. 2452–2455.
 * 
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * If you reuse this code you have to keep or cite this comment.
 *
 */
package de.lme.heartnhealth4u;

import android.text.TextUtils;
import android.util.SparseIntArray;
import android.util.Log;
import junit.framework.Assert;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import de.lme.plotview.FloatValueList;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.Plot1D;
import de.lme.plotview.PlotView;

/**
 * Handles all supported data sources.
 * 
 * @author Stefan Gradl; Patrick Kugler
 * 
 */
public class DataSource {

    public static class wfdbEcgSignal {
	public float sampleInterval;
	public FloatValueList ml2;
	// public FloatValueList v5;
	public String recordName;
	public SparseIntArray labels = new SparseIntArray(8192);

	/**
	 * Loads a WFDB ECG csv converted signal and annotation file.
	 * 
	 * @param signalFile
	 *            The csv-converted signal file. May be gzipped.
	 * @param annotationFile
	 *            The csv-converted annotation file.
	 * @param maxSamples
	 *            The maximal number of samples to load. Setting this to
	 *            <code>0</code> loads all samples.
	 * @param leadColumn
	 *            The column number (starting at 1) of the desired lead.
	 * @param scale
	 *            The scaling factor to multiply each sample with. Default
	 *            is 1. Setting this to 0 will result in a
	 *            InvalidParameterException.
	 */
	public int load(String signalFile, String annotationFile,
		int maxSamples, int leadColumn, int scale) {
	    try {
		File fs = new File(signalFile);
		File fa = new File(annotationFile);

		float val1;
		int sample = 0;
		int currentColumn = 0;
		ArrayList<Float> vals1;
		BufferedReader reader;
		long size;
		int counter = 0;

		sampleInterval = -1;

		// is gzip m_file?
		if (signalFile.endsWith(".gz")) {
		    reader = new BufferedReader(new InputStreamReader(
			    new GZIPInputStream(new BufferedInputStream(
				    new FileInputStream(fs)))));
		    // estimate the sizes
		    size = fs.length() / 3;
		} else {
		    reader = new BufferedReader(new FileReader(fs));
		    // estimate the sizes
		    size = fs.length() >> 3;
		}

		recordName = signalFile;

		// preallocate value arrays
		vals1 = new ArrayList<Float>((int) size);

		// initialize string (line) splitter
		TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(
			',');
		String line = null;

		// skip header lines
		reader.readLine();

		// read sampling interval
		line = reader.readLine();
		splitter.setString(line);
		if (splitter.hasNext()) {
		    String str = splitter.next();
		    if (str != null && str.startsWith("'")) {
			int i = str.indexOf(' ');
			if (i != -1) {
			    str = str.substring(1, i);
			    try {
				sampleInterval = Float.parseFloat(str);
			    } catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    }
			}
		    }
		}

		// if invalid, reset to default
		if (sampleInterval < 0)
		    sampleInterval = 0.00277778f;

		// ==============> read lines
		while ((line = reader.readLine()) != null) {
		    // split
		    splitter.setString(line);

		    try {
			// ==============> iterate columns
			currentColumn = 1;
			for (String split : splitter) {
			    if (currentColumn == leadColumn) {
				val1 = Float.parseFloat(split);

				// scale data?
				if (scale != 1)
				    val1 *= scale;

				vals1.add(val1);

				++counter;
			    }

			    // if (currentColumn == secondColumn)
			    // {
			    // val2 = Float.valueOf( split );
			    // vals2.add( val2 );
			    // // we have both values, break column iterator
			    // break;
			    // }

			    ++currentColumn;
			}
			// <=============
		    } catch (NumberFormatException e) {
			e.printStackTrace();
		    }

		    if (maxSamples > 0) {
			if (counter >= maxSamples)
			    break;
		    }
		}
		// <=============

		//Log.d(PlotView.TAG,
		//	String.format("Plot1D.create loaded %d values.",
		//		vals1.size()));

		// create plot
		// plot = new Plot1D( f.getName(), null, PlotStyle.LINE,
		// vals1.size() );
		// plot.setAxis( "t", "s", 1f, "a", "g", 1f );

		ml2 = new FloatValueList(vals1.size());
		ml2.copy(vals1);

		reader.close();

		// load annotations
		reader = new BufferedReader(new FileReader(fa));

		// initialize string (line) splitter
		splitter = new TextUtils.SimpleStringSplitter(' ');
		line = null;

		// skip header lines
		reader.readLine();

		// ==============> read lines
		while ((line = reader.readLine()) != null) {
		    // split
		    splitter.setString(line);

		    try {
			// ==============> iterate columns, 2nd is sample #, 3rd
			// is Type
			currentColumn = 1;
			for (String split : splitter) {
			    if (split.length() < 1)
				continue;

			    if (currentColumn == 2) {
				sample = Integer.parseInt(split.trim());
			    }

			    if (currentColumn == 3) {
				labels.append(sample, (int) split.trim()
					.charAt(0));
			    }

			    ++currentColumn;
			}
			// <=============
		    } catch (NumberFormatException e) {
			e.printStackTrace();
		    }
		}

		reader.close();

	    } catch (FileNotFoundException e) {
		e.printStackTrace();
		return 1;
	    } catch (IOException e) {
		e.printStackTrace();
		return 2;
	    } catch (OutOfMemoryError e) {
		e.printStackTrace();
		return 3;
	    }

	    return 0;
	}
    }

    public DataSource() {
    }

    /**
     * Creates a new DataSource from the given path.
     * 
     * @param path
     */
    public DataSource(String path) {

    }

    /**
     * Creates a Plot1D by loading the given text (gzipped) m_file, separating
     * at delimiter and using the given columns.
     * 
     * @param filePath
     *            File to load. Must be a textfile but can be gzipped.
     * @param delimiter
     *            Character at which to separate the columns.
     * @param firstColumn
     *            Index of the first column to use for the x values (starting
     *            from 1)
     * @param secondColumn
     *            Index of the second column to use for the Plot values
     *            (starting from 2, must be > firstColumn)
     * @param numHeaderLines
     *            Number of header lines to skip.
     * @param progressListener
     *            (AsyncTask) object listener the progress is updated to
     * @return A Plot1D object ready to use.
     */
    public static Plot1D create(String filePath, char delimiter,
								int firstColumn, int secondColumn, int numHeaderLines,
								PlotView.PlotProgressListener progressListener) {
	Plot1D plot = null;

	Assert.assertTrue(firstColumn < secondColumn);

	try {
	    File f = new File(filePath);

	    long val1;
	    float val2;
	    int currentColumn = 0;
	    int counter = 0;
	    ArrayList<Long> vals1;
	    ArrayList<Float> vals2;
	    BufferedReader reader;
	    long size;

	    // is gzip m_file?
	    if (filePath.endsWith(".gz")) {
		reader = new BufferedReader(new InputStreamReader(
			new GZIPInputStream(new BufferedInputStream(
				new FileInputStream(f)))));
		// estimate the sizes
		size = f.length() >> 1;
	    } else {
		reader = new BufferedReader(new FileReader(f));
		// estimate the sizes
		size = f.length() >> 3;
	    }

	    // no m_file size
	    if (size <= 0) {
		return null;
	    }

	    // preallocate value arrays
	    vals1 = new ArrayList<Long>((int) size);
	    vals2 = new ArrayList<Float>((int) size);

	    // initialize string (line) splitter
	    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(
		    delimiter);
	    String line = null;

	    // skip header lines
	    while (numHeaderLines > 0) {
		reader.readLine();
		--numHeaderLines;
	    }

	    if (progressListener != null)
		progressListener.onSetMaxProgress((int) size + 10);

	    // ==============> read lines
	    while ((line = reader.readLine()) != null) {
		// split
		splitter.setString(line);

		try {
		    // ==============> iterate columns
		    currentColumn = 1;
		    for (String split : splitter) {
			if (currentColumn == firstColumn) {
			    val1 = Long.parseLong(split);
			    vals1.add(val1);
			}

			if (currentColumn == secondColumn) {
			    val2 = Float.valueOf(split);
			    vals2.add(val2);
			    // we have both values, break column iterator
			    break;
			}

			++currentColumn;
		    }
		    // <=============
		} catch (NumberFormatException e) {
		    e.printStackTrace();
		}

		++counter;

		if (progressListener != null && counter % 256 == 0) {
		    if (progressListener.isCancelled()) {
			reader.close();
			return null;
		    }

		    progressListener.onUpdateProgress(counter);
		}
	    }
	    // <=============

		 Log.d( PlotView.TAG,
		 String.format( "Plot1D.create loaded %d and %d values.",
		 vals1.size(),
		 vals2.size() ) );
	    if (progressListener != null)
		progressListener.onUpdateProgress((int) (size + 3));

	    // create plot
	    plot = new Plot1D(f.getName(), null, PlotStyle.LINE, vals1.size());
	    plot.setAxis("t", "s", 1f, "a", "g", 1f);

	    plot.x.copy(vals1);

	    if (progressListener != null)
		progressListener.onUpdateProgress((int) (size + 7));

	    plot.values.copy(vals2);

	    if (progressListener != null)
		progressListener.onUpdateProgress((int) (size + 10));

	    reader.close();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (OutOfMemoryError e) {
	    return null;
	}

	return plot;
    }
}