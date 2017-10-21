package com.mytestmoudle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Created by lwh on 2017/10/19.
 * 增加点计算触摸
 * 增加文字提示
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
    int outPointNum = 4;
    //    外部点的半径
    int outPointRadius = 10;
    // 设置半径上的分割数量  默认为 1
    int centerToPointStep = 5;
    //     设置初始点的角度位置
    int startFromAngle = 0;
    //    半径长度
    float mRadius = 300;

    Paint pointPaint;

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setTextSize(30);
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
        drawLastOutPointText(canvas);
        calculateInnerPoints();
        lineInnerPoints(canvas);
        drawInnerPoints(canvas);
        canvas.restore();
    }

    /*
    * 连接内部点
    * */
    private void lineInnerPoints(Canvas canvas) {
        int size = mInnerPoints.size();
//      有几条线就花几个点
        Path path = new Path();
        for (int i = 0; i < size; i++) {
            PointF pointF = mInnerPoints.get(i);
            float x = pointF.x;
            float y = pointF.y;
////           移动到某个点
            if (i == 0) {
                path.moveTo(x, y);
            }
            path.lineTo(x, y);

        }

        Paint pt = new Paint(pointPaint);
        pt.setStrokeWidth(3);
        pt.setStyle(Paint.Style.FILL_AND_STROKE);
        pt.setColor(Color.parseColor("#6642A5F5"));
        canvas.drawPath(path, pt);
    }

    /*
    * 绘制内部点的形状
    * */
    private void drawInnerPoints(Canvas canvas) {

        int size = mInnerPoints.size();
        for (int i = 0; i < size; i++) {
            PointF pointF = mInnerPoints.get(i);
            Paint pt = new Paint(pointPaint);

            pt.setColor(Color.parseColor("#E91E63"));
//           这里控制内部点的半径
            canvas.drawCircle(pointF.x, pointF.y, 5, pt);

        }
    }

    SparseArray<PointF> mInnerPoints = new SparseArray<>();

    /*
    * 计算出所有的内部点
    * */
    private void calculateInnerPoints() {
        mInnerPoints.clear();
//  需要计算顶点的个数
        for (int i = 0; i < outPointNum; i++) {
//            获取到内部点的距离
            float distance = 0;
            distance = innerPointDistance[i];

//           测试数据    随机点
//            distance =(float) Math.random();
//            distance = (float) (distance < 0.2 ? 0.2 : distance) * mRadius;


//            求K 也就是斜率
            PointF poutf = mLastOutPointArray.get(i);
            float k = getGradient(poutf);
            double x = 0, y = 0;
            if (k != 0) {
                x = distance / Math.sqrt(Math.pow(k, 2) + 1);
//                确定斜率的正负关系
                if (poutf.x < getCenterX()) {
                    x = -x + getCenterX();
                } else {
                    x = x + getCenterX();
                }
                y = k * (x-getCenterX()) + getCenterY();
            } else {
//                斜率为0 点那个x为重点的时候 求 y  当y为重点的时候求 x
//                 确定x值
                if (poutf.x == getCenterX()){
                    x = getCenterX();


//                    这里需要注意  因为如果以中心为坐标点的话 那么 其坐标的正负和真实的是相反的因此需要进行正负计算
                    if (poutf.y > getCenterY()){
                        y = getCenterY() - distance;
                    }
                    if (poutf.y < getCenterY()){
                        y = getCenterY() + distance;
                    }


                }

//                确定Y值
                if (poutf.y == getCenterY()){
                    y = getCenterY();
                    //                正方向
                    if (poutf.x > getCenterX()){
//                    正方向的点为+距离
                        x = getCenterX() + distance;

                    }
                    if (poutf.x < getCenterX()){
                        x = getCenterX() - distance;

                    }
                }
            }
////           相对于原点的 x
//            判断X的正负

////           相对于原点的 Y
//            double y = k != 0 ? k * x : (poutf.y < getCenterY() ? getCenterY() - poutf.y : poutf.y - getCenterY());
//            存放绝对坐标值
            mInnerPoints.put(i, new PointF((float) x, (float) y ));

        }
    }

    /*
    * 求斜率 其实都是一样的 因为后来对其平方以后还是正 但这么做了一个简单的斜率封装
    * */
    private float getGradient(PointF pointF) {
        float rx = pointF.x;
        float ry = pointF.y;

        float k = 0;
//        第一 三象限
        if (rx > getCenterX() && ry < getCenterY() || rx < getCenterX() && ry > getCenterY()) {

            float dx = rx - getCenterX();
            float dy = ry - getCenterY();
            return -dy / dx;

        }
        //        第2⃣️ 四象限
        if (rx < getCenterX() && ry < getCenterY() || rx > getCenterX() && ry > getCenterY()) {
            float dx = rx - getCenterX();
            float dy = ry - getCenterY();
            return -dy / dx;

        }
//        其余的都是在主轴
        return k;
    }


    String[] outTexts = {
            "Android", "PS", "Object-C", "PHP", "CSS3", "HTML", "JAVA", "JavaScript", "GO", "Nodejs", "MySql",
            "NoSql", "Sqite3", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql",
            "JavaScript", "PHP", "Object-C", "Swift", "CSS3", "HTML", "JAVA", "Android", "GO", "Nodejs", "MySql"

    };
    //    内部点的坐标位置
    /*
     确定距离原点的长度
     计算出响应的xy
     计算出准确的坐标位置
     绘制坐标点
     连接坐标点
     填充颜色
    *
    * */
    float[] innerPointDistance = {
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231,
            255, 190, 10, 299, 50, 200, 110, 90, 25, 190, 231
    };
    float mOutPointToTextDistance = 20;

    /*
    * 绘制最外层的文字 比如战力 血量 攻击
    * */
    private void drawLastOutPointText(Canvas canvas) {
        int size = mLastOutPointArray.size();
        for (int i = 0; i < size; i++) {
            PointF pointF = mLastOutPointArray.get(i);
            if (i < outTexts.length) {
                PointF rp = getRelativePositionByText(outTexts[i], pointF);
//                绘制选中的字体颜色
                if (mCurrentIndex != -1 && (mCurrentIndex) % (outPointNum) == i) {
                    Paint pt = new Paint(pointPaint);
                    pt.setColor(Color.RED);
                    canvas.drawText(outTexts[i] + "-" + i, rp.x, rp.y, pt);

                } else {
                    canvas.drawText(outTexts[i] + "-" + i, rp.x, rp.y, pointPaint);
                }
            }
        }
    }

    /*
    * 根基原来的坐标和文字以及间距计算出应该放置的点的位置
    * 一共八个方向
    *
    * */
    private PointF getRelativePositionByText(String text, PointF pointF) {
//        原来实际的坐标点
        float rx = pointF.x;
        float ry = pointF.y;
//
        PointF reasultPonit = null;
//      文字的总宽度
        float wv = pointPaint.measureText(text);
        Paint.FontMetrics fontMetrics = pointPaint.getFontMetrics();
//        文字的高度
        float tH = fontMetrics.descent - fontMetrics.ascent;


        //      第一象限
        if (rx > getCenterX() && ry < getCenterY()) {
            reasultPonit = new PointF(rx + outPointRadius + mOutPointToTextDistance,
                    ry - outPointRadius - mOutPointToTextDistance
            );
        }
        //      第=象限
        if (rx < getCenterX() && ry < getCenterY()) {
            reasultPonit = new PointF(rx - outPointRadius - mOutPointToTextDistance - wv,
                    ry - outPointRadius - mOutPointToTextDistance
            );
        }
        //      第三象限
        if (rx < getCenterX() && ry > getCenterY()) {
            reasultPonit = new PointF(rx - outPointRadius - mOutPointToTextDistance - wv,
                    ry + outPointRadius + mOutPointToTextDistance
            );
        }
        //      第四象限
        if (rx > getCenterX() && ry > getCenterY()) {
            reasultPonit = new PointF(rx + outPointRadius + mOutPointToTextDistance,
                    ry + outPointRadius + mOutPointToTextDistance
            );
        }
        //        主轴方向
        //        y正
        if (rx == getCenterX() && ry < getCenterY()) {
            reasultPonit = new PointF(rx - wv / 2,
                    ry - outPointRadius - mOutPointToTextDistance
            );
        }
        //        y负
        if (rx == getCenterX() && ry > getCenterY()) {
            reasultPonit = new PointF(rx - wv / 2,
                    ry + outPointRadius + mOutPointToTextDistance + tH / 2
            );
        }
        //        x正
        if (rx > getCenterX() && ry == getCenterY()) {
            reasultPonit = new PointF(rx + outPointRadius + mOutPointToTextDistance,
                    ry + (tH / 4)
            );
        }
        //         x负
        if (rx < getCenterX() && ry == getCenterY()) {
            reasultPonit = new PointF(rx - outPointRadius - mOutPointToTextDistance - wv,
                    ry + tH / 4
            );
        }
        return reasultPonit;
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
        mLastOutPointArray.clear();
//计算角度
        float pnum = 360f / outPointNum;
        int mCount = 0;
//每段的长度
        float istep = mRadius / centerToPointStep;
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
                /*
                * 计算最外部所有的点并且放置在集合中
                * 绘制文字 （大小 颜色 样式）
                * 调整文字的位置
                * */
                if (j == centerToPointStep - 1) {
//                    这里修复了一个问题 源于 文字和应该持有的索引位置不正确
                    calculateOutPointRefer((i) % outPointNum,
                            rx,
                            ry
                    );
                }
                registerOnPointTouchEvent(mCount, rx, ry);
                outPointArray.put((mCount++), new Point((int) rx, (int) ry));
            }
        }
    }

    SparseArray<PointF> mLastOutPointArray = new SparseArray<>();

    /*
    * 计算最外层的文字点的位置
    * */
    private void calculateOutPointRefer(int i, double rx, double ry) {
        mLastOutPointArray.put(i, new PointF((float) rx, (float) ry));

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
                        Toast.makeText(getContext(), outTexts[(mCurrentIndex) % outPointNum] + " index=" + i, Toast.LENGTH_SHORT).show();
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

    @Override
    public void invalidate() {
        super.invalidate();
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

    public void setOutPointRefer(int size) {
        this.pointPaint.setTextSize(size);
        invalidate();
    }
    public void setInnerPointDistance(float... inces){
        this.innerPointDistance = inces;
        invalidate();
    }

    public void setPointPaintReferText(String... texts) {
        outTexts = texts;
        invalidate();
    }
}
