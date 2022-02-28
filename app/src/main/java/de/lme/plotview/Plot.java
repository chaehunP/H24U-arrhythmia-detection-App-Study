//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package de.lme.plotview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.format.Time;
import de.lme.plotview.PlotView.PlotScrollPolicy;
import de.lme.plotview.PlotView.PlotSurface;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Plot {
    protected ReentrantLock m_dataLock = new ReentrantLock();
    public FloatValueList values;
    public BooleanValueList inspectValues;
    private static final int DEFAULT_NUM_MARKERS = 128;
    protected ArrayList<Plot.PlotMarker> m_markers = new ArrayList(128);
    public String plotTitle = "Untitled";
    public Plot.PlotAxis valueAxis = new Plot.PlotAxis();
    public boolean isVisible = true;
    protected Paint m_paint = null;
    public Plot.PlotStyle style;
    public EnumSet<Plot.PlotFlag> flags;
    protected int m_maxCachedEntries;
    protected int m_desiredViewportIdxNum;
    public PlotScrollPolicy scrollHow;
    protected Plot.PlotChangedListener m_plotChangeListener;
    protected String m_file;
    protected RectF m_markerOverlay;
    protected float m_markerLast;
    protected boolean m_markerInvalid;
    protected long m_xAxisMin;
    protected long m_xAxisMax;
    protected float m_yAxisMin;
    protected float m_yAxisMax;
    protected int m_idxStart;
    protected int m_idxEnd;
    protected int m_idxNum;
    protected double m_numIdxPerPixel;
    protected int m_xIdxTrans;
    protected double m_xIdxScale;
    protected float m_yPxTrans;
    protected double m_yPxScale;
    private transient int tNum;
    private transient Plot.PlotMarker tMark;
    private transient int tIdx;
    private transient RectF tRect;
    private transient int tIdxAxis;

    public Plot() {
        this.style = Plot.PlotStyle.LINE;
        this.flags = EnumSet.of(Plot.PlotFlag.LINK_ASPECT);
        this.m_maxCachedEntries = 0;
        this.m_desiredViewportIdxNum = -1;
        this.scrollHow = PlotScrollPolicy.DEFAULT;
        this.m_plotChangeListener = null;
        this.m_file = null;
        this.m_markerOverlay = null;
        this.m_markerLast = 0.0F;
        this.m_markerInvalid = false;
        this.m_xIdxTrans = 0;
        this.m_xIdxScale = 1.0D;
        this.m_yPxTrans = 0.0F;
        this.m_yPxScale = 1.0D;
        this.tMark = null;
        this.tRect = new RectF();
    }

    public Plot(String plotTitle, Paint paint, Plot.PlotStyle style, int maxCache) {
        this.style = Plot.PlotStyle.LINE;
        this.flags = EnumSet.of(Plot.PlotFlag.LINK_ASPECT);
        this.m_maxCachedEntries = 0;
        this.m_desiredViewportIdxNum = -1;
        this.scrollHow = PlotScrollPolicy.DEFAULT;
        this.m_plotChangeListener = null;
        this.m_file = null;
        this.m_markerOverlay = null;
        this.m_markerLast = 0.0F;
        this.m_markerInvalid = false;
        this.m_xIdxTrans = 0;
        this.m_xIdxScale = 1.0D;
        this.m_yPxTrans = 0.0F;
        this.m_yPxScale = 1.0D;
        this.tMark = null;
        this.tRect = new RectF();
        this.plotTitle = plotTitle;
        if (paint != null) {
            this.m_paint = paint;
        } else {
            this.m_paint = generatePlotPaint();
        }

        this.style = style;
        this.m_maxCachedEntries = maxCache;
        this.values = new FloatValueList(this.m_maxCachedEntries, true);
        this.inspectValues = new BooleanValueList(this.m_maxCachedEntries, false);
        Time tt = new Time();
        tt.setToNow();
        this.m_file = "plot_" + tt.format2445() + "_" + this.plotTitle + ".ssd";
    }

    public Plot(String plotTitle, Paint paint, Plot.PlotStyle style, int maxCache, boolean maintainMinMax) {
        this.style = Plot.PlotStyle.LINE;
        this.flags = EnumSet.of(Plot.PlotFlag.LINK_ASPECT);
        this.m_maxCachedEntries = 0;
        this.m_desiredViewportIdxNum = -1;
        this.scrollHow = PlotScrollPolicy.DEFAULT;
        this.m_plotChangeListener = null;
        this.m_file = null;
        this.m_markerOverlay = null;
        this.m_markerLast = 0.0F;
        this.m_markerInvalid = false;
        this.m_xIdxTrans = 0;
        this.m_xIdxScale = 1.0D;
        this.m_yPxTrans = 0.0F;
        this.m_yPxScale = 1.0D;
        this.tMark = null;
        this.tRect = new RectF();
        this.plotTitle = plotTitle;
        if (paint != null) {
            this.m_paint = paint;
        } else {
            this.m_paint = generatePlotPaint();
        }

        this.style = style;
        this.m_maxCachedEntries = maxCache;
        this.values = new FloatValueList(this.m_maxCachedEntries, maintainMinMax);
        this.inspectValues = new BooleanValueList(this.m_maxCachedEntries, false);
        Time tt = new Time();
        tt.setToNow();
        this.m_file = "plot_" + tt.format2445() + "_" + this.plotTitle + ".ssd";
    }

    public void setAxis(String axisTitle, String unitName, float multiplier) {
        this.valueAxis.title = axisTitle;
        this.valueAxis.unitName = unitName;
        this.valueAxis.multiplier = multiplier;
        this.valueAxis.paint = generatePlotPaint(2.0F, 192, 128, 128, 128);
        this.valueAxis.paintText = generatePlotPaint(1.0F, 192, 192, 192, 192);
        this.valueAxis.paintText.setTextAlign(Align.LEFT);
        this.valueAxis.paintText.setStyle(Style.STROKE);
        this.valueAxis.paintText.setTextSize(12.0F);
    }

    public void setPaint(Paint p) {
        if (p != null) {
            this.m_paint = p;
        }

    }

    public void setFile(String newFile) {
        if (newFile != null) {
            this.m_file = newFile;
        }

    }

    public static Paint generatePlotPaint() {
        Paint p = new Paint();
        Random rand = new Random();
        p.setARGB(254, 24 + rand.nextInt(230), 24 + rand.nextInt(230), 24 + rand.nextInt(230));
        p.setAntiAlias(true);
        p.setStrokeWidth(1.0F);
        p.setTextAlign(Align.CENTER);
        p.setStyle(Style.STROKE);
        return p;
    }

    public static Paint generatePlotPaint(float width, int a, int r, int g, int b) {
        Paint p = new Paint();
        p.setARGB(a, r, g, b);
        p.setAntiAlias(true);
        p.setStrokeWidth(width);
        p.setStyle(Style.STROKE);
        p.setTextAlign(Align.CENTER);
        return p;
    }

    public static Paint generatePlotPaint(Style style, int a, int r, int g, int b) {
        Paint p = new Paint();
        p.setARGB(a, r, g, b);
        p.setStyle(style);
        return p;
    }

    protected void plotChanged() {
        if (this.m_plotChangeListener != null) {
            this.m_plotChangeListener.onPlotChanged();
        }

    }

    public void setMarker(Plot.PlotMarker marker) {
        this.setMarker(this.getValueHead(), marker);
    }

    public void setMarker(int entryIdx, Plot.PlotMarker marker) {
        this.m_dataLock.lock();
        if (marker == null) {
            this.tNum = this.m_markers.size();

            for(this.tIdx = 0; this.tIdx < this.tNum; ++this.tIdx) {
                this.tMark = (Plot.PlotMarker)this.m_markers.get(this.tIdx);
                if (this.tMark.m_index == entryIdx) {
                    this.m_markers.remove(this.tIdx);
                    break;
                }
            }

            this.inspectValues.set(entryIdx, false);
        } else {
            this.inspectValues.set(entryIdx, true);
            marker.m_index = entryIdx;
            marker.m_plot = this;
            this.m_markers.add(marker);
        }

        this.m_dataLock.unlock();
    }

    public Plot.PlotMarker getMarker(int entryIdx) {
        this.tNum = this.m_markers.size();

        for(this.tIdx = 0; this.tIdx < this.tNum; ++this.tIdx) {
            this.tMark = (Plot.PlotMarker)this.m_markers.get(this.tIdx);
            if (this.tMark.m_index == entryIdx) {
                return this.tMark;
            }
        }

        return null;
    }

    public void setViewport(int numIdx) {
        this.m_desiredViewportIdxNum = numIdx;
    }

    protected void drawXAxis(Canvas can, PlotSurface surface, Plot.PlotAxis axis, boolean drawGrid) {
        can.drawLine(0.0F, (float)(surface.viewHeight - 16), (float)surface.viewWidth, (float)(surface.viewHeight - 16), axis.paint);
        axis.paintText.setTextAlign(Align.CENTER);

        for(this.tIdxAxis = 1; this.tIdxAxis < 5; ++this.tIdxAxis) {
            this.tRect.left = (float)(16 + this.tIdxAxis * surface.viewWidth / 5);
            this.tRect.right = this.tRect.left;
            this.tRect.bottom = (float)(surface.viewHeight - 16 + 4);
            this.tRect.top = (float)(surface.viewHeight - 16 - 4);
            if (drawGrid) {
                can.drawLine(this.tRect.left, this.tRect.bottom, this.tRect.right, 0.0F, axis.paint);
            }

            can.drawLine(this.tRect.left, this.tRect.bottom, this.tRect.right, this.tRect.top, axis.paint);
            can.drawText(this.formatAxisText(axis, this.tIdxAxis * surface.viewWidth / 5), this.tRect.left, (float)surface.viewHeight - axis.paintText.descent(), axis.paintText);
        }

        axis.paintText.setTextAlign(Align.RIGHT);
        can.drawText(axis.title, (float)(surface.viewWidth - 4), (float)surface.viewHeight - axis.paintText.descent(), axis.paintText);
    }

    protected void drawYAxis(Canvas can, PlotSurface surface, Plot.PlotAxis axis, boolean drawGrid) {
        can.drawLine(16.0F, (float)surface.viewHeight, 16.0F, 0.0F, axis.paint);

        for(this.tIdxAxis = 1; this.tIdxAxis < 5; ++this.tIdxAxis) {
            this.tRect.left = 12.0F;
            this.tRect.right = 20.0F;
            this.tRect.bottom = (float)(surface.viewHeight - 16 - this.tIdxAxis * surface.viewHeight / 5);
            this.tRect.top = this.tRect.bottom;
            if (drawGrid) {
                can.drawLine(this.tRect.left, this.tRect.bottom, (float)surface.viewWidth, this.tRect.top, axis.paint);
            }

            can.drawLine(this.tRect.left, this.tRect.bottom, this.tRect.right, this.tRect.top, axis.paint);
            can.drawText(this.formatAxisText(axis, this.tIdxAxis * surface.viewHeight / 5), 1.0F, this.tRect.bottom, axis.paintText);
        }

        can.drawText(axis.title, 1.0F, 0.0F - axis.paintText.ascent(), axis.paintText);
    }

    public void drawGlobalMarks(Canvas can, PlotSurface surface) {
        this.m_dataLock.lock();
        this.tNum = this.m_markers.size();

        for(this.tIdx = 0; this.tIdx < this.tNum; ++this.tIdx) {
            this.tMark = (Plot.PlotMarker)this.m_markers.get(this.tIdx);
            if (this.tMark.m_index == -1) {
                this.tMark.onDraw(can, surface, 0.0F, 0.0F);
            }
        }

        this.m_dataLock.unlock();
    }

    protected abstract void getViewport(PlotSurface var1);

    protected abstract void draw(Canvas var1, PlotSurface var2);

    protected abstract String formatAxisText(Plot.PlotAxis var1, int var2);

    protected abstract void drawAxis(Canvas var1, PlotSurface var2, boolean var3, boolean var4);

    public int getValueHead() {
        return this.values.head;
    }

    public void clear() {
        this.values.clear();
    }

    public abstract boolean saveToFile(Context var1, String var2, String var3);

    public abstract int loadFromFile(Context var1, InputStream var2);

    protected static class PlotAxis {
        public String title = "n/a";
        public String unitName = "n/a";
        public float multiplier = 1.0F;
        public Paint paint = null;
        public Paint paintText = null;

        public PlotAxis() {
            this.paint = Plot.generatePlotPaint(2.0F, 192, 128, 128, 128);
            this.paintText = Plot.generatePlotPaint(1.0F, 192, 192, 192, 192);
            this.paintText.setTextAlign(Align.LEFT);
            this.paintText.setStyle(Style.STROKE);
            this.paintText.setTextSize(12.0F);
        }
    }

    public static class PlotChangedListener {
        private PlotView mAttachedPlotView = null;

        PlotChangedListener(PlotView v) {
            this.mAttachedPlotView = v;
        }

        protected void onAttach(PlotView v) {
            this.mAttachedPlotView = v;
        }

        public void onPlotChanged() {
            if (this.mAttachedPlotView != null) {
                this.mAttachedPlotView.requestRedraw(false);
            }

        }
    }

    public static enum PlotFlag {
        LINK_ASPECT;

        private PlotFlag() {
        }
    }

    public abstract static class PlotMarker {
        protected int m_index = -1;
        protected Paint m_paint = null;
        protected Plot m_plot = null;

        public PlotMarker() {
        }

        public void onAttach(Plot plot) {
            this.m_plot = plot;
            if (this.m_paint == null) {
                this.m_paint = Plot.generatePlotPaint(1.0F, 164, 128, 128, 128);
            }

        }

        public abstract void onDraw(Canvas var1, PlotSurface var2, float var3, float var4);
    }

    public static class PlotMarkerDefault extends Plot.PlotMarker {
        public Plot.PlotMarkerDefault.DefaultMark m_mark;
        public float m_param;

        public PlotMarkerDefault(Plot.PlotMarkerDefault.DefaultMark mark, Paint paint, float param) {
            this.m_mark = Plot.PlotMarkerDefault.DefaultMark.POINT;
            this.m_param = 1.0F;
            this.m_mark = mark;
            this.m_paint = paint;
            if (this.m_paint == null) {
                this.m_paint = PlotView.s_markerPaint;
            }

            this.m_param = param;
        }

        public void onDraw(Canvas can, PlotSurface surface, float x, float y) {
            if (this.m_index == -1) {
                x = this.m_param;
                y = this.m_param;
            }
            switch(this.m_mark.ordinal()) {
                case 3:
                    can.drawText("*", x, y, PlotView.s_markerTextPaint);
                    break;
                case 4:
                    can.drawText("<=", x, y, PlotView.s_markerTextPaint);
                    break;
                case 5:
                    can.drawCircle(x, y, this.m_param, this.m_paint);
                    break;
                case 6:
                    if (this.m_plot.m_markerOverlay != null) {
                        this.m_plot.m_markerOverlay.top = (float)surface.height;
                        this.m_plot.m_markerOverlay.bottom = 0.0F;
                        this.m_plot.m_markerOverlay.right = x;
                        this.m_plot.m_markerLast = x;
                        can.drawRect(this.m_plot.m_markerOverlay, this.m_paint);
                        this.m_plot.m_markerInvalid = true;
                    }

                    this.m_plot.m_markerOverlay = new RectF();
                    this.m_plot.m_markerOverlay.left = x;
                    this.m_plot.m_markerOverlay.bottom = y;
                    this.m_plot.m_markerOverlay.top = this.m_plot.m_markerOverlay.bottom;
                    break;
                case 7:
                    if (this.m_plot.m_markerOverlay == null) {
                        this.m_plot.m_markerOverlay = new RectF();
                        this.m_plot.m_markerOverlay.left = this.m_plot.m_markerLast;
                        this.m_plot.m_markerOverlay.top = (float)surface.height;
                        this.m_plot.m_markerOverlay.bottom = 0.0F;
                        this.m_plot.m_markerOverlay.right = x;
                        this.m_plot.m_markerLast = x;
                        can.drawRect(this.m_plot.m_markerOverlay, this.m_paint);
                        this.m_plot.m_markerOverlay = null;
                    } else {
                        if (y > this.m_plot.m_markerOverlay.top) {
                            this.m_plot.m_markerOverlay.top = y;
                        } else if (y < this.m_plot.m_markerOverlay.bottom) {
                            this.m_plot.m_markerOverlay.bottom = y;
                        } else {
                            RectF var10000 = this.m_plot.m_markerOverlay;
                            var10000.bottom -= 10.0F;
                        }

                        this.m_plot.m_markerOverlay.top = (float)surface.height;
                        this.m_plot.m_markerOverlay.bottom = 0.0F;
                        this.m_plot.m_markerOverlay.right = x;
                        this.m_plot.m_markerLast = x;
                        if (this.m_plot.m_markerInvalid) {
                            can.drawRect(this.m_plot.m_markerOverlay, PlotView.s_overlayInvPaint);
                            this.m_plot.m_markerInvalid = false;
                        } else {
                            can.drawRect(this.m_plot.m_markerOverlay, this.m_paint);
                        }

                        this.m_plot.m_markerOverlay = null;
                    }
                    break;
                case 8:
                    can.drawLine(x, 0.0F, x, (float)surface.height, this.m_paint);
                    break;
                case 9:
                    can.drawLine(0.0F, y, (float)surface.width, y, this.m_paint);
            }

        }

        public static enum DefaultMark {
            NONE,
            POINT,
            STAR,
            ARROW,
            CIRCLE,
            OVERLAY_BEGIN,
            OVERLAY_END,
            LINE_VERTICAL,
            LINE_HORIZONTAL;

            private DefaultMark() {
            }
        }
    }

    public static enum PlotStyle {
        POINT,
        CIRCLE,
        RECT,
        RECT_VALUE_FILLED,
        LINE,
        STAIR,
        BAR,
        STEM,
        CROSS,
        STAR,
        TEXT;

        private PlotStyle() {
        }
    }
}
