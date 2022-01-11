package com.example.mycurves;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by FrankChoo on 2017/12/29.
 * Email: frankchoochina@gmail.com
 * Version: 1.0
 * Description: 表格自定义View
 */
public class RunTimeCurvesView extends View {


    public class LayerFlag {
        public static final int TOP = 0;
        public static final int BOTTOM = -1;
    }


    private class MCurve {
        int color;
        int layer;
        int id;
        boolean isCoverRegion;
        ConcurrentLinkedQueue<Float> values = new ConcurrentLinkedQueue<Float>();

        public MCurve(int color, boolean isCoverRegion, int id, int layer) {
            this.color = color;
            this.layer = layer;
            this.isCoverRegion = isCoverRegion;
            this.id = id;
        }

        public MCurve(int color, boolean isCoverRegion, int id) {
            this.color = color;
            this.layer = LayerFlag.TOP;
            this.isCoverRegion = isCoverRegion;
            this.id = id;
        }

        public MCurve(int color, boolean isCoverRegion, int id, int layer, float[] values) {
            this.color = color;
            this.layer = layer;
            this.isCoverRegion = isCoverRegion;
            this.id = id;
            for (int i = 0; i < values.length; i++) {
                this.values.add(values[i]);
            }
        }
    }

    //管理曲线的List
    private Map<Integer, MCurve> mCurves;
    private int curvesNumCnt;
    // 坐标轴的数值
    private int mCoordinateYCount;  //Y轴横线数量
    private int mCoordinateXCount;  //X轴竖线数量
    List<Line> xLines = new ArrayList<Line>();
    List<Line> yLines = new ArrayList<Line>();

    // 网格尺寸
    private int mGridWidth, mGridHeight;
    boolean isGridOn = true;


    //一幅图像中点(间隔)的数目
    private int mXDivNum;
    private int mXDivW;
    // 所有曲线中所有数据中的最大值
    private Paint mCoordinatorPaint;
    private Paint mTextPaint;
    private Paint mCurvePaint;

    private float allCurvesMaxValue;
    private float allCurvesMinValue;
    // 坐标轴上描述性文字的空间大小
    private int mTopUnitHeight;// 顶部Y轴单位高度
    private int mBottomTextHeight;
    private int mLeftTextWidth;

    // 坐标的单位
    private String mXUnit;
    private String mYUnit;
    //X轴的刻度值参数
    private String axisXValueType = "int";
    private String axisYValueType = "float";
    private float axisXStartValue = 0;
    private float axisYStartValue = 0;
    private float axisXStepValue = 1;
    private float axisYStepValue = 1;

    boolean initCoordinatorOK = false;

    public RunTimeCurvesView(Context context) {
        this(context, null);
    }

    public RunTimeCurvesView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RunTimeCurvesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化数据集合的容器
        mCurves = new HashMap<Integer, MCurve>();

        // 坐标系的单位
        mBottomTextHeight = dp2px(40);// X轴底部字体的高度
        mLeftTextWidth = mBottomTextHeight;// Y轴左边字体的宽度
        mTopUnitHeight = dp2px(30);// 顶部Y轴的单位
        // 初始化坐标轴Paint
        mCoordinatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mCoordinatorPaint.setColor(Color.LTGRAY);
        // 初始化文本Paint
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mTextPaint.setColor(Color.GRAY);
        mTextPaint.setTextSize(sp2px(12));
        // 初始化曲线Paint
        mCurvePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    }

    public void setPathEffect(PathEffect effect){
        mCurvePaint.setPathEffect(effect);
    }

    public void setCoordinator(String xUnit, String yUnit, int mCoordinateXCount, int mCoordinateYCount) {
        mXUnit = xUnit;
        mYUnit = yUnit;
        this.mCoordinateYCount = mCoordinateYCount; //
        this.mCoordinateXCount = mCoordinateXCount;
    }

    public void setCoordinator(String xUnit, String yUnit, int mCoordinateXCount, int mCoordinateYCount, String axisXValueType, String axisYValueType) {
        mXUnit = xUnit;
        mYUnit = yUnit;
        this.mCoordinateXCount = mCoordinateXCount;
        this.mCoordinateYCount = mCoordinateYCount;

        this.axisXValueType = axisXValueType;
        this.axisYValueType = axisYValueType;

        mXDivNum = mCoordinateXCount;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (!initCoordinatorOK) {
            initCoordinator(canvas);
        }


        drawYCoordinateMetric(canvas);
        drawCurves(canvas);
    }

    private void initCoordinator(Canvas canvas) {
        calCoordinateGridParam();
        calCureDrawParam();
        if (isGridOn) {
            drawCoordinateGrid(canvas);
        } else {
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC);
            drawCoordinateXY(canvas);
        }

        drawXYCoordinateUnit(canvas);
        drawXCoordinateMetric(canvas);
        drawYCoordinateMetric(canvas);
    }

    private void calCoordinateGridParam() {
        //获取网格宽高
        mGridHeight = (getHeight() - getPaddingTop() - getPaddingBottom() - mBottomTextHeight - mTopUnitHeight) / (mCoordinateYCount - 1);
        mGridWidth = (getWidth() - getPaddingLeft() - getPaddingRight() - mLeftTextWidth) / (mCoordinateXCount - 1);

        //获取横向网格线条的起点和终点
        for (int i = 0; i < mCoordinateYCount; i++) {
            Point start = new Point();
            Point end = new Point();
            start.x = getPaddingLeft() + mLeftTextWidth;
            start.y = getHeight() - getPaddingBottom() - mBottomTextHeight - mGridHeight * i;
            end.x = getRight() - getPaddingRight();
            end.y = start.y;
            xLines.add(new Line(start, end));
        }

        //获取纵向网格线条的起点和终点
        for (int i = 0; i < mCoordinateXCount; i++) {
            Point start = new Point();
            Point end = new Point();
            start.x = getPaddingLeft() + mLeftTextWidth + mGridWidth * i;
            start.y = getPaddingTop() + mTopUnitHeight;
            end.x = start.x;
            end.y = getHeight() - mBottomTextHeight - getPaddingBottom();
            yLines.add(new Line(start, end));
        }
    }

    public void setXDivNum(int divNum) {
        mXDivNum = divNum;
        initCoordinatorOK = false;
    }

    private void calCurvesParam(float currentValue) {

        allCurvesMinValue = allCurvesMaxValue = currentValue;

        for (MCurve curve : mCurves.values()) {


            for (float value : curve.values) {

                allCurvesMaxValue = Math.max(allCurvesMaxValue, value);
                allCurvesMinValue = Math.min(allCurvesMinValue, value);
            }
        }

        axisYStepValue = (allCurvesMaxValue - allCurvesMinValue) / (mCoordinateYCount - 1);


    }

    private void calCureDrawParam() {
        mXDivW = (getWidth() - getPaddingLeft() - getPaddingRight() - mLeftTextWidth) / (mXDivNum - 1);
    }

    private void drawCoordinateGrid(Canvas canvas) {

        Point start = null, end = null;

        // 1. 绘制横轴线
        for (int i = 0; i < mCoordinateYCount; i++) {
            start = xLines.get(i).start;
            end = xLines.get(i).end;
            // 绘制横轴线
            canvas.drawLine(start.x, start.y, end.x, end.y, mCoordinatorPaint);
        }

        // 2. 绘制纵轴线
        for (int i = 0; i < mCoordinateXCount; i++) {
            start = yLines.get(i).start;
            end = yLines.get(i).end;
            // 绘制纵轴线
            canvas.drawLine(start.x, start.y, end.x, end.y, mCoordinatorPaint);
        }
    }

    private void drawCoordinateXY(Canvas canvas) {

        Point start = null, end = null;
        start = xLines.get(0).start;
        end = xLines.get(0).end;
        canvas.drawLine(start.x, start.y, end.x, end.y, mCoordinatorPaint);

        start = yLines.get(0).start;
        end = yLines.get(0).end;
        // 绘制纵轴线
        canvas.drawLine(start.x, start.y, end.x, end.y, mCoordinatorPaint);

    }


    private void drawXYCoordinateUnit(Canvas canvas) {

        // 绘制Y轴单位
        float y_text_x = getPaddingLeft() + mLeftTextWidth / 2 - mTextPaint.measureText(mYUnit) / 2;
        float y_text_y = getPaddingTop() + mTopUnitHeight / 2;
        canvas.drawText(mYUnit, y_text_x, y_text_y, mTextPaint);

        // 绘制X轴单位
        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
        float offsetY = ((fontMetrics.bottom - fontMetrics.top) / 2 + fontMetrics.bottom) / 2;
        float x_text_y = getHeight() - getPaddingBottom() - mBottomTextHeight / 2 + offsetY;
        float x_text_x = getRight() - mTextPaint.measureText(mXUnit);
        canvas.drawText(mXUnit, x_text_x, x_text_y, mTextPaint);
    }


    private String getTextOfXCoordinateValues(int i) {
        String text = "";
        if (axisXValueType.equals("float")) {
            Float value = i * axisXStepValue + axisXStartValue;
            //TODO 控制String的长度
            text = value.toString();
        }

        if (axisXValueType.equals("int")) {
            Integer value = (int) (i * axisXStepValue + axisXStartValue);
            text = value.toString();
        }
        return text;
    }

    private String getTextOfYCoordinateValues(int i) {
        String text = "";
        if (axisYValueType.equals("float")) {
            Float value = i * axisYStepValue + axisYStartValue;
            //TODO 控制String的长度
            text = value.toString();
        }


        if (axisYValueType.equals("int")) {
            Integer value = (int) (i * axisYStepValue + axisYStartValue);
            text = value.toString();
        }

        return text;
    }

    private void drawYCoordinateMetric(Canvas canvas) {
        Point start = null;
        //绘制绘制纵坐标刻度值
        for (int i = 0; i < mCoordinateYCount; i++) {
            start = xLines.get(i).start;
            String drawText = getTextOfYCoordinateValues(i);
            Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
            float offsetY = ((fontMetrics.bottom - fontMetrics.top) / 2 + fontMetrics.bottom) / 2;
            float baseLine = start.y + offsetY;
            float left = getPaddingLeft() + mLeftTextWidth / 2 - mTextPaint.measureText(drawText) / 2;
            canvas.drawText(drawText, left, baseLine, mTextPaint);
        }
    }

    private void drawXCoordinateMetric(Canvas canvas) {
        Point start = null;
        //绘制横坐标刻度值
        for (int i = 0; i < mCoordinateXCount; i++) {
            start = yLines.get(i).start;
            String drawText = getTextOfXCoordinateValues(i);
            Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
            float offsetY = ((fontMetrics.bottom - fontMetrics.top) / 2 + fontMetrics.bottom) / 2;
            float baseLine = getHeight() - getPaddingBottom() - mBottomTextHeight / 2 + offsetY;
            float left = start.x - mTextPaint.measureText(drawText) / 2;
            canvas.drawText(drawText, left, baseLine, mTextPaint);
        }
    }


    /**
     * 绘制曲线
     */
    private void drawCurves(Canvas canvas) {

//        int paddingLeft = getPaddingLeft();
//        int paddingRight = getPaddingRight();
//        int paddingTop = getPaddingTop();
//        int paddingBottom = getPaddingBottom();
//        //获取绘制的View的宽度
//        int width = getWidth() - paddingLeft - paddingRight;
//        //获取绘制的View的高度
//        int height = getHeight() - paddingTop - paddingBottom;
//        //绘制View，左上角坐标（0+paddingLeft,0+paddingTop），右下角坐标（width+paddingLeft,height+paddingTop）
//        canvas.drawRect(0 + paddingLeft, 0 + paddingTop, 10 + paddingLeft, 10 + paddingTop, mCurvePaint);


        Point origin = xLines.get(0).start; //获取原点在View中的坐标
        float yHeight = mGridHeight * (mCoordinateYCount - 1);
        for (MCurve curve : mCurves.values()) {
            Path path = new Path();
            if (!curve.values.isEmpty()) {
                path.moveTo(origin.x, origin.y - yHeight * (curve.values.peek() / allCurvesMaxValue));
            }

            int index = 0;
//            for(Iterator<Float> it = curve.values.iterator(); it.hasNext();){
//                float value = it.next();
//                float a = origin.x + mGridWidth * index;
//                     float b=   origin.y - yHeight * (value / allCurvesMaxValue);
//
//                index++;
//            }

//            for (int i = 0; i < curve.values.size(); i++) {
//                float a = origin.x + mGridWidth * index;
//                float b = origin.y - yHeight * (1 / allCurvesMaxValue);
//
//
//                path.lineTo(
//                        1,
//                        11
//                );
//            }
            for (float value : curve.values) {
                path.lineTo(
                        origin.x + mXDivW * index,
                        origin.y - yHeight * (value / allCurvesMaxValue)
                );
                index++;
            }

            if (curve.isCoverRegion) {
                mCurvePaint.setStyle(Paint.Style.FILL);
                path.lineTo(getRight() - getPaddingRight(), getHeight());
                path.close();
            } else {
                mCurvePaint.setStyle(Paint.Style.STROKE);
                mCurvePaint.setStrokeWidth(10);
            }

            mCurvePaint.setColor(curve.color);
            canvas.drawPath(path, mCurvePaint);
        }
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                sp, getResources().getDisplayMetrics());
    }


    public int createCurve(int color, boolean isCoverRegion) {
        mCurves.put(curvesNumCnt, new MCurve(color, isCoverRegion, curvesNumCnt));
        return curvesNumCnt++;
    }

    public boolean push2Curve(int id, float data) {
        if (mCurves.keySet().contains(id)) {
            ConcurrentLinkedQueue<Float> values = mCurves.get(id).values;
            if (values.size() >= mXDivNum) {
//                values.removeFirst();
                values.poll();
            }
            values.offer(data);
            calCurvesParam(data);
            postInvalidate();

            return true;
        } else {
            return false;
        }
    }

    public boolean isCurveEmpty(int id) {
        if (mCurves.keySet().contains(id)) {
            return mCurves.get(id).values.isEmpty();
        } else {
            return false;
        }
    }


    public void gridOn(boolean isGridOn) {
        this.isGridOn = isGridOn;
        initCoordinatorOK = false;
    }
}


