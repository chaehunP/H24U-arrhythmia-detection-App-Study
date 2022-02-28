//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package de.lme.plotview;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
//import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.Toast;
import de.lme.plotview.Plot.PlotChangedListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import junit.framework.Assert;
import android.view.View.OnClickListener;

public class PlotView extends View {
    public static final String TAG = "PlotView";
    protected static final String NEWLINE = System.getProperty("line.separator");
    protected static final int AXIS_PADDING = 16;
    protected static final int AXIS_PIN_LENGTH = 4;
    protected static final int AXIS_PIN_COUNT = 5;
    protected static final float DEFAULT_TEXT_SIZE = 12.0F;
    private ArrayList<Plot> m_plots = new ArrayList();
    private long m_maxRedrawRate = 25L;
    private volatile long m_lastRedrawMillis = -1L;
    private EnumSet<PlotView.Flags> m_plotFlags;
    private Paint m_defaultPaint;
    private Paint m_shader;
    protected static Paint s_mapPaint = null;
    protected static Paint s_overlayPaint = null;
    protected static Paint s_overlayInvPaint = null;
    protected static Paint s_markerPaint = null;
    protected static Paint s_marker2Paint = null;
    protected static Paint s_markerTextPaint = null;
    private GestureDetector m_gestureDetector;
    private ScaleGestureDetector m_scaleGeDet;
    private boolean m_isYScale;
    protected PlotView.PlotSurface m_surface;
    protected PlotView.PlotViewGroup m_group;
    private transient int t_idx;
    private transient int t_size;
    private transient int t_iter;
    private transient int t_drawSize;
    private transient Transformation tAnimTrafo;
    private transient float[] tTrafoMat;

    public PlotView(Context context) {
        super(context);
        this.m_plotFlags = PlotView.Flags.DEFAULT.clone();
        this.m_defaultPaint = null;
        this.m_shader = null;
        this.m_isYScale = false;
        this.m_surface = null;
        this.m_group = null;
        this.t_idx = 0;
        this.t_size = 0;
        this.t_iter = 0;
        this.t_drawSize = 0;
        this.tAnimTrafo = new Transformation();
        this.tTrafoMat = new float[9];
        this.initPlotView(context);
    }

    public PlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.m_plotFlags = PlotView.Flags.DEFAULT.clone();
        this.m_defaultPaint = null;
        this.m_shader = null;
        this.m_isYScale = false;
        this.m_surface = null;
        this.m_group = null;
        this.t_idx = 0;
        this.t_size = 0;
        this.t_iter = 0;
        this.t_drawSize = 0;
        this.tAnimTrafo = new Transformation();
        this.tTrafoMat = new float[9];
        this.initPlotView(context);
    }

    public PlotView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.m_plotFlags = PlotView.Flags.DEFAULT.clone();
        this.m_defaultPaint = null;
        this.m_shader = null;
        this.m_isYScale = false;
        this.m_surface = null;
        this.m_group = null;
        this.t_idx = 0;
        this.t_size = 0;
        this.t_iter = 0;
        this.t_drawSize = 0;
        this.tAnimTrafo = new Transformation();
        this.tTrafoMat = new float[9];
        this.initPlotView(context);
    }

    private void initPlotView(Context context) {
        this.m_lastRedrawMillis = -1L;
        this.m_gestureDetector = new GestureDetector(new PlotView.GestureListener());
        this.m_scaleGeDet = new PlotView.ScaleGestDetector(context, new PlotView.ScaleListener());
        this.m_surface = new PlotView.PlotSurface();
    }

    public void addFlag(PlotView.Flags flag) {
        this.m_plotFlags.add(flag);
    }

    public int attachPlot(Plot plot) {
        if (plot == null) {
            return -1;
        } else {
            if (plot.m_plotChangeListener == null) {
                plot.m_plotChangeListener = new PlotChangedListener(this);
            } else {
                plot.m_plotChangeListener.onAttach(this);
            }

            this.m_plots.add(plot);
            this.requestRedraw(false);
            return this.m_plots.size() - 1;
        }
    }

    public int attachPlot(Plot plot, PlotChangedListener changeListener) {
        if (plot == null) {
            return -1;
        } else {
            if (changeListener != null) {
                plot.m_plotChangeListener = changeListener;
                plot.m_plotChangeListener.onAttach(this);
            } else {
                plot.m_plotChangeListener = null;
            }

            this.m_plots.add(plot);
            this.requestRedraw(false);
            return this.m_plots.size() - 1;
        }
    }

    public Plot getFirstVisiblePlot() {
        this.t_size = this.m_plots.size();

        for(this.t_idx = 0; this.t_idx < this.t_size; ++this.t_idx) {
            if (((Plot)this.m_plots.get(this.t_idx)).isVisible) {
                return (Plot)this.m_plots.get(this.t_idx);
            }
        }

        return null;
    }

    public long getMaxRedrawRate() {
        return this.m_maxRedrawRate;
    }

    public int getNumPlots() {
        return this.m_plots.size();
    }

    public Plot getPlot(int idx) {
        Assert.assertTrue(idx >= 0 && idx < this.m_plots.size());
        return (Plot)this.m_plots.get(idx);
    }

    public boolean hasFlag(PlotView.Flags flag) {
        return this.m_plotFlags.contains(flag);
    }

    public void requestRedraw(boolean usePost) {
        if (this.m_maxRedrawRate <= 0L) {
            if (usePost) {
                this.postInvalidate();
            } else {
                this.invalidate();
            }
        } else {
            Plot firstVisPlot = this.getFirstVisiblePlot();
            if (firstVisPlot != null && this.isEnabled() && this.m_lastRedrawMillis + this.m_maxRedrawRate <= System.currentTimeMillis()) {
                if (usePost) {
                    this.postInvalidate();
                } else {
                    this.invalidate();
                }
            }
        }

    }

    private void checkPaint() {
        if (this.m_defaultPaint == null) {
            this.m_defaultPaint = new Paint();
            this.m_defaultPaint.setARGB(192, 128, 128, 138);
            this.m_defaultPaint.setAntiAlias(true);
            this.m_defaultPaint.setStyle(Style.FILL_AND_STROKE);
            this.m_defaultPaint.setStrokeWidth(1.0F);
        }

        if (this.m_shader == null) {
            int[] col = new int[]{Color.argb(64, 128, 164, 128), Color.argb(128, 64, 92, 64)};
            this.m_shader = new Paint();
            this.m_shader.setShader(new LinearGradient(0.0F, 0.0F, 0.0F, (float)this.getHeight(), col, (float[])null, TileMode.MIRROR));
        }

        if (s_mapPaint == null) {
            s_mapPaint = new Paint();
            s_mapPaint.setStyle(Style.FILL_AND_STROKE);
            s_mapPaint.setColor(800113886);
            s_mapPaint.setAntiAlias(true);
        }

        if (s_overlayPaint == null) {
            s_overlayPaint = new Paint();
            s_overlayPaint.setStyle(Style.FILL_AND_STROKE);
            s_overlayPaint.setARGB(128, 239, 238, 220);
        }

        if (s_overlayInvPaint == null) {
            s_overlayInvPaint = new Paint();
            s_overlayInvPaint.setStyle(Style.FILL_AND_STROKE);
            s_overlayInvPaint.setARGB(128, 128, 128, 128);
        }

        float[] intervals;
        if (s_markerPaint == null) {
            s_markerPaint = new Paint();
            s_markerPaint.setARGB(192, 143, 188, 143);
            s_markerPaint.setAntiAlias(true);
            s_markerPaint.setStyle(Style.FILL_AND_STROKE);
            s_markerPaint.setStrokeWidth(3.0F);
            intervals = new float[]{8.0F, 2.0F};
            s_markerPaint.setPathEffect(new DashPathEffect(intervals, 0.0F));
        }

        if (s_marker2Paint == null) {
            s_marker2Paint = new Paint();
            s_marker2Paint.setARGB(192, 143, 188, 143);
            s_marker2Paint.setAntiAlias(true);
            s_marker2Paint.setStyle(Style.STROKE);
            s_marker2Paint.setStrokeWidth(2.0F);
            intervals = new float[]{8.0F, 2.0F};
            s_marker2Paint.setPathEffect(new DashPathEffect(intervals, 0.0F));
        }

        if (s_markerTextPaint == null) {
            s_markerTextPaint = new Paint();
            s_markerTextPaint.setARGB(192, 143, 188, 143);
            s_markerTextPaint.setAntiAlias(true);
            s_markerTextPaint.setStyle(Style.FILL_AND_STROKE);
            s_markerTextPaint.setStrokeWidth(2.0F);
            s_markerTextPaint.setTextSize(32.0F);
            s_markerTextPaint.setTextAlign(Align.LEFT);
        }

    }

    protected void onDraw(Canvas canvas) {
        if ((this.isEnabled() && (this.getVisibility()==View.VISIBLE))) {  // isEnabled는 View.class에서 boolean형이고 getVisibility는 View.class에서 int형
            this.m_lastRedrawMillis = System.currentTimeMillis();
            this.checkPaint();
            this.m_surface.viewHeight = this.getHeight();
            this.m_surface.viewWidth = this.getWidth();
            this.m_surface.width = this.m_surface.viewWidth - 16 - this.getPaddingRight() - this.getPaddingLeft();
            this.m_surface.height = this.m_surface.viewHeight - 16 - this.getPaddingTop() - this.getPaddingBottom();
            this.m_surface.masterPlot = this.getFirstVisiblePlot();
            if (this.m_surface.masterPlot == null) {
                canvas.drawText("n/a", 30.0F, (float)(this.m_surface.viewHeight - 30), this.m_defaultPaint);
                canvas.drawLine(-1000.0F, (float)this.m_surface.viewHeight - 5.0F, 1000.0F, (float)this.m_surface.viewHeight - 5.0F, this.m_defaultPaint);
                canvas.drawLine(5.0F, -1000.0F, 5.0F, 1000.0F, this.m_defaultPaint);
            } else {
                this.m_surface.masterPlot.getViewport(this.m_surface);
                this.m_surface.xScrollAmp = this.m_surface.masterPlot.m_numIdxPerPixel;
                if (this.hasFlag(PlotView.Flags.DISABLE_Y_USERSCROLL)) {
                    this.m_surface.yTrans = 0.0F;
                    this.m_surface.yScrollAmp = 1.0D;
                } else {
                    this.m_surface.yScrollAmp = 1.0D / this.m_surface.masterPlot.m_yPxScale;
                }

                if (this.m_plotFlags.contains(PlotView.Flags.DRAW_AXES)) {
                    canvas.drawText(this.m_surface.masterPlot.plotTitle, 80.0F, 30.0F, this.m_defaultPaint);
                    this.m_surface.masterPlot.drawAxis(canvas, this.m_surface, this.m_plotFlags.contains(PlotView.Flags.DRAW_GRID), this.m_plotFlags.contains(PlotView.Flags.DRAW_MAP));
                }

                canvas.save();
                canvas.translate(0.0F, (float)this.m_surface.viewHeight);
                canvas.scale(1.0F, -1.0F);
                canvas.translate((float)(16 + this.getPaddingLeft()), (float)(16 + this.getPaddingBottom()));
                this.t_drawSize = this.m_plots.size();

                for(this.t_iter = 0; this.t_iter < this.t_drawSize; ++this.t_iter) {
                    if (this.m_plots.get(this.t_iter) != this.m_surface.masterPlot && ((Plot)this.m_plots.get(this.t_iter)).isVisible) {
                        ((Plot)this.m_plots.get(this.t_iter)).draw(canvas, this.m_surface);
                        ((Plot)this.m_plots.get(this.t_iter)).drawGlobalMarks(canvas, this.m_surface);
                    }
                }

                this.m_surface.masterPlot.draw(canvas, this.m_surface);
                this.m_surface.masterPlot.drawGlobalMarks(canvas, this.m_surface);
                canvas.restore();
                if (this.m_surface.xFlinger != null && !this.m_surface.xFlinger.hasEnded()) {
                    this.m_surface.xFlinger.getTransformation(AnimationUtils.currentAnimationTimeMillis(), this.tAnimTrafo);
                    this.tAnimTrafo.getMatrix().getValues(this.tTrafoMat);
                    this.m_surface.xTrans = this.tTrafoMat[2];
                    this.invalidate();
                }

                if (this.m_surface.xAnimScale != null && !this.m_surface.xAnimScale.hasEnded()) {
                    this.m_surface.xAnimScale.getTransformation(AnimationUtils.currentAnimationTimeMillis(), this.tAnimTrafo);
                    this.tAnimTrafo.getMatrix().getValues(this.tTrafoMat);
                    this.m_surface.xScale = this.tTrafoMat[0];
                    this.invalidate();
                }

                if (this.m_maxRedrawRate <= 0L) {
                    this.invalidate();
                }

            }
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.m_shader = null;
        s_overlayPaint = null;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.m_gestureDetector.onTouchEvent(event)) {
            return true;
        } else {
            return this.m_scaleGeDet.onTouchEvent(event);
        }
    }

    public void removeFlag(PlotView.Flags flag) {
        this.m_plotFlags.remove(flag);
        this.requestRedraw(false);
    }

    public void removePlot(int idx) {
        if (idx < 0) {
            this.m_plots.clear();
        } else {
            this.m_plots.remove(idx);
        }

        this.requestRedraw(false);
    }

    public void setMaxRedrawRate(long maxRedrawRate) {
        this.m_maxRedrawRate = maxRedrawRate;
        this.requestRedraw(false);
    }

    protected static void log(String str) {
        Log.d("PlotView", str);
    }

    public static enum Flags {
        DRAW_AXES,
        DRAW_GRID,
        DRAW_MAP,
        ENABLE_GESTURES,
        DISABLE_Y_USERSCROLL,
        ENABLE_AUTO_SCROLL,
        ENABLE_AUTO_ZOOM_Y,
        ENABLE_AUTO_ZOOM_X,
        ENABLE_AUTO_RESET;

        public static final EnumSet<PlotView.Flags> DEFAULT = EnumSet.of(DRAW_AXES, DRAW_MAP, ENABLE_GESTURES, ENABLE_AUTO_SCROLL, ENABLE_AUTO_ZOOM_Y);

        private Flags() {
        }
    }

    final class GestureListener extends SimpleOnGestureListener {
        GestureListener() {
        }

        public boolean onDoubleTapEvent(MotionEvent ev) {
            return false;
        }

        public boolean onDoubleTap(MotionEvent ev) {
            PlotView.this.m_surface.reset(false);
            PlotView.this.invalidate();
            return true;
        }

        public boolean onDown(MotionEvent ev) {
            return true;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(e2.getX() - e1.getX()) > 70.0F) {
                PlotView.this.m_surface.animateXTrans(PlotView.this.m_surface.xTrans, (float)((double)PlotView.this.m_surface.xTrans - (double)velocityX * PlotView.this.m_surface.xScrollAmp), 5000L);
                PlotView.this.invalidate();
            } else if (Math.abs(velocityY) > Math.abs(velocityX) && Math.abs(e2.getY() - e1.getY()) > 35.0F) {
                if (e2.getY() > e1.getY()) {
                    PlotView.this.m_surface.animateXScale(PlotView.this.m_surface.xScale, PlotView.this.m_surface.xScale * 2.0F, 1500L);
                } else {
                    PlotView.this.m_surface.animateXScale(PlotView.this.m_surface.xScale, PlotView.this.m_surface.xScale / 2.0F, 1500L);
                }

                PlotView.this.invalidate();
            }

            return true;
        }


        public void onLongPress(MotionEvent ev) {
            PlotView.this.performHapticFeedback(0);
            Toast.makeText(PlotView.this.getContext(), "Double-tap to reset view. Fling up/down to scale.",Toast.LENGTH_SHORT).show();
            if (PlotView.this.m_surface.masterPlot != null) {
                Builder intBuilder = new Builder(PlotView.this.getContext());
                intBuilder.setTitle("Warp Me");
                intBuilder.setMessage("Enter the index to jump to.");
                final EditText input = new EditText(PlotView.this.getContext());
                input.setInputType(2);
                intBuilder.setView(input);
                input.setHint(Integer.toString(PlotView.this.m_surface.masterPlot.values.num + PlotView.this.m_surface.masterPlot.m_xIdxTrans));
                intBuilder.setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int whichButton) {
                        PlotView.this.m_surface.xTrans = (float)(-PlotView.this.m_surface.masterPlot.values.num) + Float.parseFloat(input.getText().toString());
                        PlotView.this.invalidate();
                    }
                });
                intBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int whichButton) {
                    }
                });
                intBuilder.show();
            }

        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1.getPointerCount() <= 1 && e2.getPointerCount() <= 1) {
                if (PlotView.this.m_surface.xFlinger != null && !PlotView.this.m_surface.xFlinger.hasEnded()) {
                    PlotView.this.m_surface.xFlinger.cancel();
                    return true;
                } else {
                    if (PlotView.this.m_surface.masterPlot != null) {
                        PlotView.this.m_surface.yTrans = PlotView.this.m_surface.masterPlot.m_yPxTrans;
                    }

                    PlotView.PlotSurface var10000 = PlotView.this.m_surface;
                    var10000.xTrans = (float)((double)var10000.xTrans + (double)distanceX * PlotView.this.m_surface.xScrollAmp);
                    var10000 = PlotView.this.m_surface;
                    var10000.yTrans = (float)((double)var10000.yTrans + (double)distanceY * PlotView.this.m_surface.yScrollAmp);
                    if (PlotView.this.m_group != null) {
                        PlotView.this.m_group.propagateTrans(PlotView.this, PlotView.this.m_surface.xTrans, PlotView.this.m_surface.yTrans);
                    }

                    PlotView.this.invalidate();
                    return true;
                }
            } else {
                return false;
            }
        }

        public void onShowPress(MotionEvent ev) {
            if (PlotView.this.m_surface.xFlinger != null && !PlotView.this.m_surface.xFlinger.hasEnded()) {
                PlotView.this.m_surface.xFlinger.cancel();
                PlotView.this.invalidate();
            }

        }

        public boolean onSingleTapUp(MotionEvent ev) {
            return true;
        }

    }

    public interface PlotProgressListener {
        void onUpdateProgress(int var1);

        void onSetMaxProgress(int var1);

        boolean isCancelled();
    }

    static enum PlotScrollPolicy {
        DEFAULT,
        OVERRUN,
        DROP,
        GAP;

        private PlotScrollPolicy() {
        }
    }

    protected class PlotSurface {
        public int viewWidth;
        public int viewHeight;
        public int width;
        public int height;
        public float xTrans = 0.0F;
        public float yTrans = 0.0F;
        public float xScale = 1.0F;
        public float yScale = 1.0F;
        public double xScrollAmp = 1.0D;
        public double yScrollAmp = 1.0D;
        public TranslateAnimation xFlinger = null;
        public ScaleAnimation xAnimScale = null;
        public Plot masterPlot = null;
        private DecelerateInterpolator m_decInterpolator = new DecelerateInterpolator(2.0F);
        private LinearInterpolator m_linInterpolator = new LinearInterpolator();

        protected PlotSurface() {
        }

        public void reset(boolean noFancyStuff) {
            this.yTrans = 0.0F;
            this.yScale = 1.0F;
            this.xScrollAmp = 1.0D;
            this.yScrollAmp = 1.0D;
            if (this.xFlinger != null) {
                this.xFlinger.cancel();
            }

            if (this.xAnimScale != null) {
                this.xAnimScale.cancel();
            }

            if (noFancyStuff) {
                this.xTrans = 0.0F;
                this.xScale = 1.0F;
                this.xFlinger = null;
                this.xAnimScale = null;
            } else {
                this.animateXTrans(PlotView.this.m_surface.xTrans, 0.0F, 2500L);
                this.animateXScale(PlotView.this.m_surface.xScale, 1.0F, 1500L);
            }

            PlotView.this.invalidate();
        }

        public void animateXTrans(float fromXDelta, float toXDelta, long duration) {
            this.xFlinger = new TranslateAnimation(fromXDelta, toXDelta, 0.0F, 0.0F);
            this.xFlinger.initialize(1, 1, 20000, 10);
            this.xFlinger.setFillEnabled(false);
            this.xFlinger.setDuration(duration);
            this.xFlinger.setInterpolator(this.m_decInterpolator);
            this.xFlinger.setRepeatCount(0);
            this.xFlinger.startNow();
        }

        public void animateXScale(float fromX, float toX, long duration) {
            this.xAnimScale = new ScaleAnimation(fromX, toX, 0.0F, 0.0F);
            this.xAnimScale.initialize(1000, 1000, 100000, 100000);
            this.xAnimScale.setFillEnabled(false);
            this.xAnimScale.setDuration(duration);
            this.xAnimScale.setInterpolator(this.m_linInterpolator);
            this.xAnimScale.setRepeatCount(0);
            this.xAnimScale.startNow();
        }
    }

    public static class PlotViewGroup {
        private ArrayList<PlotView> m_links = new ArrayList(8);
        private boolean m_linkY = true;

        public PlotViewGroup() {
        }

        public void addView(PlotView view, boolean linkY) {
            this.m_linkY = linkY;
            if (!this.m_links.contains(view)) {
                this.m_links.add(view);
                view.m_group = this;
            }

        }

        public void propagateScale(PlotView propagator, float scale, boolean isY) {
            PlotView v;
            for(Iterator var5 = this.m_links.iterator(); var5.hasNext(); v.invalidate()) {
                v = (PlotView)var5.next();
                PlotView.PlotSurface var10000;
                if (isY) {
                    var10000 = v.m_surface;
                    var10000.yScale *= scale;
                } else {
                    var10000 = v.m_surface;
                    var10000.xScale /= scale;
                }
            }

        }

        public void propagateTrans(PlotView propagator, float xTrans, float yTrans) {
            PlotView v;
            for(Iterator var5 = this.m_links.iterator(); var5.hasNext(); v.invalidate()) {
                v = (PlotView)var5.next();
                v.m_surface.xTrans = xTrans;
                if (this.m_linkY) {
                    v.m_surface.yTrans = yTrans;
                }
            }

        }
    }

    final class ScaleGestDetector extends ScaleGestureDetector {
        private float m_xRange;
        private float m_yRange;

        public ScaleGestDetector(Context context, OnScaleGestureListener listener) {
            super(context, listener);
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (event.getPointerCount() > 1) {
                this.m_xRange = event.getX(event.getPointerId(0)) - event.getX(event.getPointerId(1));
                this.m_yRange = event.getY(event.getPointerId(0)) - event.getY(event.getPointerId(1));
                if (Math.abs(this.m_xRange) > Math.abs(this.m_yRange)) {
                    PlotView.this.m_isYScale = false;
                } else {
                    PlotView.this.m_isYScale = true;
                }
            }

            return super.onTouchEvent(event);
        }
    }

    final class ScaleListener extends SimpleOnScaleGestureListener {
        private float m_lastScaleFactor = 1.0F;

        ScaleListener() {
        }

        public boolean onScale(ScaleGestureDetector detector) {
            this.m_lastScaleFactor = detector.getScaleFactor();
            if (this.m_lastScaleFactor != 0.0F) {
                PlotView.PlotSurface var10000;
                if (PlotView.this.m_isYScale) {
                    var10000 = PlotView.this.m_surface;
                    var10000.yScale *= this.m_lastScaleFactor;
                    if (PlotView.this.m_group != null) {
                        PlotView.this.m_group.propagateScale(PlotView.this, this.m_lastScaleFactor, true);
                    }
                } else {
                    var10000 = PlotView.this.m_surface;
                    var10000.xScale /= this.m_lastScaleFactor;
                    if (PlotView.this.m_surface.xScale > 10000.0F) {
                        PlotView.this.m_surface.xScale = 10000.0F;
                    } else if (PlotView.this.m_surface.xScale < 1.0E-4F) {
                        PlotView.this.m_surface.xScale = 1.0E-4F;
                    }

                    if (PlotView.this.m_group != null) {
                        PlotView.this.m_group.propagateScale(PlotView.this, this.m_lastScaleFactor, false);
                    }
                }

                PlotView.this.invalidate();
            }

            return true;
        }
    }
}
