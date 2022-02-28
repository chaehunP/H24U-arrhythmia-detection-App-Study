//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package de.lme.plotview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.format.Time;
import android.util.Log;
import de.lme.plotview.Plot.PlotAxis;
import de.lme.plotview.Plot.PlotMarker;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.PlotView.PlotProgressListener;
import de.lme.plotview.PlotView.PlotScrollPolicy;
import de.lme.plotview.PlotView.PlotSurface;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import junit.framework.Assert;

import org.apache.commons.lang3.BooleanUtils;

public class Plot1D extends Plot {
    public LongValueList x;
    public PlotAxis xAxis = new PlotAxis();
    private transient int tIdx = 0;
    private transient int tPixelIdx = 0;
    private transient int tRealIdx = 0;
    private transient float tppValue = 0.0F;
    private transient float tppValueMax = 0.0F;
    private transient float tppValueMin = 0.0F;
    private transient PlotMarker tMarker = null;
    private transient Path tPath = new Path();
    private transient int tppIdxMin;
    private transient int tppIdxMax;
    private transient Time tTime = new Time();
    private transient RectF tRect = new RectF();

    public Plot1D(String plotTitle, Paint paint, PlotStyle style, int maxCache) {
        super(plotTitle, paint, style, maxCache);
        this.m_dataLock.lock();
        this.x = new LongValueList(this.m_maxCachedEntries, true);
        this.m_dataLock.unlock();
    }

    public Plot1D(String plotTitle, Paint paint, PlotStyle style, int maxCache, boolean maintainMinMax) {
        super(plotTitle, paint, style, maxCache, maintainMinMax);
        this.m_dataLock.lock();
        this.x = new LongValueList(this.m_maxCachedEntries, maintainMinMax);
        this.m_dataLock.unlock();
    }

    public void setAxis(String xTitle, String xUnit, float xMultiplier, String valueTitle, String valueUnit, float valueMultiplier) {
        super.setAxis(valueTitle, valueUnit, valueMultiplier);
        this.xAxis.title = xTitle;
        this.xAxis.unitName = xUnit;
        this.xAxis.multiplier = xMultiplier;
        this.xAxis.paint = Plot.generatePlotPaint(2.0F, 192, 128, 128, 128);
        this.xAxis.paintText = Plot.generatePlotPaint(1.0F, 192, 192, 192, 192);
        this.xAxis.paintText.setTextAlign(Align.CENTER);
        this.xAxis.paintText.setStyle(Style.STROKE);
        this.xAxis.paintText.setTextSize(12.0F);
    }

    public void addValueFast(long value, long x) {
        this.x.add(x);
        this.values.add(value);
        this.inspectValues.add(false);
    }

    public void addValueFast(float value, long x) {
        this.x.add(x);
        this.values.add(value);
        this.inspectValues.add(false);
    }

    public void addValue(long value, long x) {
        this.m_dataLock.lock();
        this.addValueFast(value, x);
        this.setMarker(this.values.head, (PlotMarker)null);
        this.m_dataLock.unlock();
        this.plotChanged();
    }

    public void addValue(float value, long x) {
        this.m_dataLock.lock();
        this.addValueFast(value, x);
        this.setMarker(this.values.head, (PlotMarker)null);
        this.m_dataLock.unlock();
        this.plotChanged();
    }

    public void addValue(long value, long x, PlotMarker marker) {
        this.m_dataLock.lock();
        this.x.add(x);
        this.values.add(value);
        if (marker != null) {
            this.inspectValues.add(true);
        } else {
            this.inspectValues.add(false);
        }

        this.setMarker(this.x.head, marker);
        this.m_dataLock.unlock();
        this.plotChanged();
    }

    public void clear() {
        super.clear();
        this.x.clear();
    }

    protected void draw(Canvas can, PlotSurface surface) {
        this.getViewport(surface);
        this.m_dataLock.lock();
        if (this.m_idxNum >= 1 && this.m_numIdxPerPixel != 0.0D) {
            if (surface.masterPlot != this) {
                this.m_yPxScale = surface.masterPlot.m_yPxScale;
                this.m_yPxTrans = surface.masterPlot.m_yPxTrans;
            }

            this.m_markerOverlay = null;
            this.m_markerLast = 0.0F;
            this.m_markerInvalid = false;
            can.save();

            try {
                this.tPath.reset();
                this.tppValue = (float)((double)(this.values.getIndirect(this.m_idxStart) + this.m_yPxTrans) * this.m_yPxScale);
                this.tPath.moveTo(0.0F, this.tppValue);
                this.tIdx = 0;

                for(this.tPixelIdx = 1; this.tIdx < this.m_idxNum; ++this.tPixelIdx) {
                    for(this.tppIdxMin = -1; (double)this.tIdx < (double)this.tPixelIdx * this.m_numIdxPerPixel && this.tIdx < this.m_idxNum; ++this.tIdx) {
                        this.tRealIdx = this.values.normIdx(this.m_idxStart + this.tIdx);
                        this.tppValue = (float)((double)(this.values.values[this.tRealIdx] + this.m_yPxTrans) * this.m_yPxScale);
                        if (this.m_numIdxPerPixel > 1.0D) {
                            if (this.tppIdxMin == -1) {
                                this.tppValueMin = this.tppValueMax = this.tppValue;
                                this.tppIdxMin = this.tppIdxMax = this.tIdx;
                            } else if (this.tppValue < this.tppValueMin) {
                                this.tppValueMin = this.tppValue;
                                this.tppIdxMin = this.tIdx;
                            } else if (this.tppValue > this.tppValueMax) {
                                this.tppValueMax = this.tppValue;
                                this.tppIdxMax = this.tIdx;
                            }
                        } else {
                            this.tppIdxMin = this.tRealIdx;
                        }

                        if (this.inspectValues.values[this.tRealIdx]) {
                            this.tMarker = this.getMarker(this.tRealIdx);
                            if (this.tMarker != null) {
                                this.tMarker.onDraw(can, surface, (float)this.tPixelIdx, this.tppValue);
                            }
                        }
                    }

                    if (this.tppIdxMin != -1) {
                        if (this.m_numIdxPerPixel > 1.0D) {
                            if (this.tppIdxMin <= this.tppIdxMax) {
                                this.tPath.lineTo((float)this.tPixelIdx, this.tppValueMin);
                                this.tPath.lineTo((float)this.tPixelIdx, this.tppValueMax);
                            } else {
                                this.tPath.lineTo((float)this.tPixelIdx, this.tppValueMax);
                                this.tPath.lineTo((float)this.tPixelIdx, this.tppValueMin);
                            }
                        } else {
                            this.tPath.lineTo((float)this.tPixelIdx, this.tppValue);
                        }
                    }
                }

                this.m_dataLock.unlock();
                can.drawPath(this.tPath, this.m_paint);
            } catch (Exception var4) {
                this.m_dataLock.unlock();
                var4.printStackTrace();
            }

            can.restore();
        } else {
            this.m_dataLock.unlock();
        }
    }

    public void getViewport(PlotSurface surface) {
        this.m_dataLock.lock();
        if (this.values.num >= this.m_desiredViewportIdxNum && this.m_desiredViewportIdxNum > 0) {
            this.m_idxNum = this.m_desiredViewportIdxNum;
        } else {
            this.m_idxNum = this.values.num;
        }

        this.m_idxNum = (int)((float)this.m_idxNum * surface.xScale);
        if (this.m_idxNum <= 0) {
            this.m_dataLock.unlock();
        } else {
            if (this.m_idxNum > this.values.num) {
                this.m_idxNum = this.values.num;
                surface.xScale = (float)this.m_xIdxScale;
                surface.xTrans = 0.0F;
            } else {
                this.m_xIdxScale = (double)surface.xScale;
            }

            if (this.m_idxNum <= surface.width) {
                this.m_numIdxPerPixel = 1.0D;
                if (this.values.num > surface.width) {
                    this.m_idxNum = surface.width;
                    if (this.values.num != 0) {
                        surface.xScale = (float)((double)surface.width / (double)this.values.num);
                    }
                } else {
                    this.m_idxNum = this.values.num;
                }
            } else {
                this.m_numIdxPerPixel = (double)this.m_idxNum / (double)(surface.width + 2);
            }

            if (this.scrollHow == PlotScrollPolicy.DEFAULT) {
                this.m_xIdxTrans = (int)surface.xTrans;
                if (this.m_xIdxTrans <= this.m_idxNum - this.values.num) {
                    this.m_xIdxTrans = this.m_idxNum - this.values.num;
                    surface.xTrans = (float)this.m_xIdxTrans;
                }

                if (this.m_xIdxTrans > 0) {
                    this.m_xIdxTrans = 0;
                    surface.xTrans = 0.0F;
                }

                this.m_idxStart = this.values.normIdx(this.values.head - this.m_idxNum + 1 + this.m_xIdxTrans);
                this.m_idxEnd = this.values.normIdx(this.values.head + (this.m_xIdxTrans < 0 ? this.m_xIdxTrans : 0));
            } else if (this.scrollHow == PlotScrollPolicy.OVERRUN) {
                this.m_idxEnd = this.values.normIdx(this.m_idxNum - 1);
                this.m_idxStart = 0;
            }

            if (this.values.rangeMinMax != 0.0F) {
                this.m_yPxScale = (double)((float)(surface.height - 32) / this.values.rangeMinMax);
            } else {
                this.m_yPxScale = 1.0D;
            }

            this.m_yPxScale *= (double)surface.yScale;
            if (this.m_yPxScale == 0.0D) {
                this.m_yPxScale = 1.0D;
            }

            if (surface.yTrans == 0.0F) {
                this.m_yPxTrans = (float)((double)(-this.values.minValue) + 18.0D / this.m_yPxScale);
            } else {
                this.m_yPxTrans = surface.yTrans;
            }

            this.m_xAxisMax = this.x.getIndirect(this.m_idxEnd);
            this.m_xAxisMin = this.x.getIndirect(this.m_idxStart);
            this.m_yAxisMax = this.values.maxValue;
            this.m_yAxisMin = this.values.minValue;
            this.m_dataLock.unlock();
        }
    }

    protected String formatAxisText(PlotAxis axis, int pt) {
        if (axis == this.xAxis) {
            if ((double)this.m_idxStart + this.m_numIdxPerPixel * (double)pt >= (double)this.x.num) {
                return "n/a";
            } else {
                this.tTime.set(this.x.getIndirect((int)((double)this.m_idxStart + this.m_numIdxPerPixel * (double)pt)));
                return this.tTime.format("%H:%M:%S");
            }
        } else if (axis == this.valueAxis && this.m_yPxScale != 0.0D) {
            return (double)pt / this.m_yPxScale > 10.0D ? String.format("%.0f", (this.m_yAxisMin + (float)((long)((double)pt / this.m_yPxScale))) * axis.multiplier) : String.format("%.1f", (this.m_yAxisMin + (float)((long)((double)pt / this.m_yPxScale))) * axis.multiplier);
        } else {
            return "n/a";
        }
    }

    protected void drawAxis(Canvas can, PlotSurface surface, boolean drawGrid, boolean drawMap) {
        if (drawMap && this.values.num > 0) {
            this.tRect.left = (float)this.x.tailDistance(this.m_idxStart) * ((float)surface.viewWidth / (float)this.values.num);
            this.tRect.right = (float)((double)this.tRect.left + (double)this.m_idxNum / (double)this.x.num * (double)surface.viewWidth);
            this.tRect.bottom = (float)(surface.viewHeight - 16);
            this.tRect.top = (float)surface.viewHeight;
            this.tRect.sort();
            can.drawRect(this.tRect, PlotView.s_mapPaint);
        }

        this.drawXAxis(can, surface, this.xAxis, drawGrid);
        this.drawYAxis(can, surface, this.valueAxis, drawGrid);
    }

    public boolean saveToFile(Context con, String filePath, String header) {
        File f = null;

        try {
            if (filePath == null) {
                if (this.m_file.charAt(0) == File.separatorChar) {
                    f = new File(this.m_file);
                } else {
                    f = new File(con.getExternalFilesDir((String)null), this.m_file);
                }
            } else {
                f = new File(filePath);
            }

            FileWriter fw = new FileWriter(f, true);
            if (header != null && f.length() < 5L) {
                fw.write(header);
            }

            StringBuilder sb = new StringBuilder(128);

            for(this.tIdx = 0; this.tIdx < this.values.num; ++this.tIdx) {
                sb.setLength(0);
                sb.append(this.x.values[this.tIdx]).append(" ").append(this.values.values[this.tIdx]).append(PlotView.NEWLINE);
                fw.write(sb.toString());
            }

            fw.close();
            return true;
        } catch (IOException var7) {
            if (f != null) {
                Log.w("PlotView", "Error writing " + f.getAbsolutePath(), var7);
            } else {
                Log.w("PlotView", "Error writing " + filePath, var7);
            }

            return false;
        }
    }

    public static Plot1D create(String filePath, char delimiter, int firstColumn, int secondColumn, int numHeaderLines, PlotProgressListener progressListener) {
        Plot1D plot = null;
        Assert.assertTrue(firstColumn < secondColumn);

        try {
            File f = new File(filePath);
            boolean currentColumn = false;

            int currentColumn1 = BooleanUtils.toInteger(currentColumn);
            int counter = 0;
            BufferedReader reader;
            long size;
            if (filePath.endsWith(".gz")) {
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)))));
                size = f.length() >> 1;
            } else {
                reader = new BufferedReader(new FileReader(f));
                size = f.length() >> 3;
            }

            if (size <= 0L) {
                return null;
            }

            ArrayList<Long> vals1 = new ArrayList((int)size);
            ArrayList<Float> vals2 = new ArrayList((int)size);
            SimpleStringSplitter splitter = new SimpleStringSplitter(delimiter);

            String line;
            for(line = null; numHeaderLines > 0; --numHeaderLines) {
                reader.readLine();
            }

            if (progressListener != null) {
                progressListener.onSetMaxProgress((int)size + 10);
            }

            while((line = reader.readLine()) != null) {
                splitter.setString(line);

                try {
                    if (currentColumn1 == 1) {

                        for (Iterator var21 = splitter.iterator(); var21.hasNext(); ++currentColumn1) {
                            String split = (String) var21.next();
                            if (currentColumn1 == firstColumn) {
                                long val1 = Long.parseLong(split);
                                vals1.add(val1);
                            }

                            if (currentColumn1 == secondColumn) {
                                float val2 = Float.valueOf(split);
                                vals2.add(val2);
                                break;
                            }
                        }
                    }
                }catch (NumberFormatException var22) {
                    var22.printStackTrace();
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

            if (progressListener != null) {
                progressListener.onUpdateProgress((int)(size + 3L));
            }

            plot = new Plot1D(f.getName(), (Paint)null, PlotStyle.LINE, vals1.size());
            plot.setAxis("t", "s", 1.0F, "a", "g", 1.0F);
            plot.x.copy(vals1);
            if (progressListener != null) {
                progressListener.onUpdateProgress((int)(size + 7L));
            }

            plot.values.copy(vals2);
            if (progressListener != null) {
                progressListener.onUpdateProgress((int)(size + 10L));
            }

            reader.close();
        } catch (FileNotFoundException var23) {
            var23.printStackTrace();
        } catch (IOException var24) {
            var24.printStackTrace();
        } catch (OutOfMemoryError var25) {
            return null;
        }

        return plot;
    }

    public int loadFromFile(Context con, InputStream streamIn) {
        int count = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(streamIn));

        try {
            long curtime = System.currentTimeMillis();
            SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
            String line = null;

            while((line = reader.readLine()) != null) {
                splitter.setString(line);
                if (splitter.hasNext()) {
                    long rval = Long.parseLong(splitter.next());
                    if (splitter.hasNext()) {
                        long rx = Long.parseLong(splitter.next());
                        this.addValueFast(rval, rx);
                    } else {
                        this.addValueFast(rval, curtime + (long)(5 * count++));
                    }
                }
            }

            reader.close();
        } catch (NumberFormatException var13) {
            var13.printStackTrace();
        } catch (IOException var14) {
        }

        return count;
    }
}
