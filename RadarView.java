package com.mytestmoudle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by lwh on 2017/10/19.
 */

/*
确定中心点
确定边界点数量
确定第一个点的位置（这里有个新的思路是 默认使用0角度作为起始点，然后根据偏转角进行旋转即可）
计算平均夹角确定边界点的相对位置
链接每个边界点
确定半径分割数量
计算缩放比例
通过缩放比例计算出所有的线段
*
* */

public class RadarView extends View {
    // 外部点的数量
    int outPointNum = 7;
    //    外部点的半径
    int outPointRadius = 20;
    // 设置半径上的分割数量  默认为 1
    int centerToPointStep = 3;
    //     设置初始点的角度位置
    int startFromAngle = 270;
    //    半径长度
    int mRadius = 300;

    Paint pointPaint;

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setTextSize(100);
        pointPaint.setStrokeWidth(2);
        pointPaint.setColor(Color.parseColor("#42A5F5"));
        pointPaint.setStyle(Paint.Style.FILL);
    }

    // 相对于坐标原点的集合
    SparseArray<Point> outPointArray = new SparseArray<>();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        lineTenAssist(canvas);
        canvas.save();
//        canvas.rotate(startFromAngle, getCenterX(), getCenterY());
//        绘制中心圆心
        canvas.drawCircle(getCenterX(), getCenterY(), 10, pointPaint);
        calculateOutPoints();
        lineOutPoints(canvas);
        circleOutPoints(canvas);

        canvas.restore();
    }

    /*绘制十字辅助线*/
    public void lineTenAssist(Canvas canvas) {
        canvas.save();
//       绘制十字线
//v
        canvas.drawLine(0, getCenterY(), getMeasuredWidth(), getCenterY(), pointPaint);
//        h
        canvas.drawLine(getCenterX(), 0, getCenterX(), getMeasuredHeight(), pointPaint);

        canvas.restore();
    }

    /*
    * 绘制外部点
    * */
    private void circleOutPoints(Canvas canvas) {
        int size = outPointArray.size();
        for (int i = 0; i < size; i++) {
            Point point = outPointArray.get(i);
            if (mCurrentIndex == i) {

                Paint pt = new Paint(pointPaint);
                pt.setColor(Color.RED);
                canvas.drawCircle(point.x, point.y, outPointRadius, pt);

            } else {
                canvas.drawCircle(point.x, point.y, outPointRadius, pointPaint);
            }

//            绘制坐标点的索引
//            Paint paint = new Paint();
//            paint.setColor(Color.RED);
//            paint.setTextSize(40);
//            canvas.drawText("" + i, point.x, point.y, paint);
        }
    }

    /*
     * 计算边界点的X Y坐标
     * */
    private void calculateOutPoints() {
        outPointArray.clear();
        rectFSparseArray.clear();
//计算角度
        float pnum = 360f / outPointNum;
        int mCount = 0;
//每段的长度
        int istep = mRadius / centerToPointStep;
        for (int j = 0; j < centerToPointStep; j++) {
            for (int i = 0; i < outPointNum; i++) {
                float mp = pnum * i;
//              计算偏转角以后的准确坐标
                //X
                double x = Math.cos(getRadian(mp - startFromAngle)) * (j + 1) * istep;
                //Y
                double y = Math.sin(getRadian(mp - startFromAngle)) * (j + 1) * istep;

                double rx = x + getCenterX();
//                得道相对于原点的坐标
                double ry = -y + getCenterY();
//
                registerOnPointTouchEvent(mCount, rx, ry);
                outPointArray.put((mCount++), new Point((int) rx, (int) ry));
            }
        }
    }

    SparseArray<RectF> rectFSparseArray = new SparseArray<>();

    /*
    * 注册点对应的触摸事件
    * */
    private void registerOnPointTouchEvent(int count, double rx, double ry) {
        RectF rectF = new RectF((float) rx - outPointRadius,
                (float) ry - outPointRadius,
                (float) rx + outPointRadius,
                (float) ry + outPointRadius);
        rectFSparseArray.put(count, rectF);

    }

    int mCurrentIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();

        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                long l = System.currentTimeMillis();
                int size = rectFSparseArray.size();
                for (int i = 0; i < size; i++) {
                    RectF rectF = rectFSparseArray.get(i);
                    if (rectF.contains(x, y)) {

                        long la = System.currentTimeMillis() - l;
                        mCurrentIndex = i;
                        invalidate();
//                        Toast.makeText(getContext(), (la)+ "ms x=" + x + " y=" + y, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    /*
       * 链接外部点的链接
       * */
    private void lineOutPoints(Canvas canvas) {
        int size = outPointArray.size();
        for (int i = 1; i <= size; i++) {
            Point p1 = outPointArray.get(i - 1);
            if ((i) % (outPointNum) != 0) {
                Point p2;
//              连接相邻线段的点
                p2 = outPointArray.get(i);
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, pointPaint);

            } else {
//              连接每一层的首尾点
                Point p = outPointArray.get(i - outPointNum);

                canvas.drawLine(p1.x, p1.y, p.x, p.y, pointPaint);
            }

        }
    }


    /*
    * 获取中心X
    * */
    public int getCenterX() {
        return getMeasuredWidth() / 2;
    }

    /*
    * 获取中心Y
    * */
    public int getCenterY() {
        return getMeasuredHeight() / 2;
    }


    /*
    换算角度到弧度
    * */
    public double getRadian(double angle) {
        return (Math.PI / 180) * angle;
    }

    public void setOutPoint(int outPoint) {
        this.outPointNum = outPoint < 3 ? 3 : outPoint;
        invalidate();
    }
}
