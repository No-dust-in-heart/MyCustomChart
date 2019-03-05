package com.mplus.ticket.view

import android.R
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView


class LineColumnChart(mcontext: Context?, attributeSet: AttributeSet?, def: Int) : View(mcontext, attributeSet, def) {

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?) : this(context, attrs = null)

    private var xLength: Float = 0.toFloat()
    private var yLength: Float = 0.toFloat()
    private var startPointX: Float = 0.toFloat()
    private var startPointY: Float = 0.toFloat()
    private var xScale: Float = 0.toFloat()
    private var yScale: Float = 0.toFloat()
    private var coordTextSize: Float = 0.toFloat()

    private var xLabel: Array<String>? = null
    private var yLabel: Array<String?>? = null
    private var yLabel2: Array<String?>? = null
    private var data: Array<String>? = null
    private var data2: Array<String>? = null

    private var mDataCoords: Array<FloatArray>? = null//转折点坐标
    private var mDataCoords2: Array<FloatArray>? = null//转折点坐标

    private var mScaleLinePaint: Paint? = null
    private var mDataLinePaint: Paint? = null
    private var mScaleValuePaint: Paint? = null

    var bounds = Rect()
    val ValueHeight = 50f
    val ValueWidth = 100f

    private var isClick: Boolean = false   // 是否点击了数据点
    private var clickIndex = -1            // 被点击的数据点的索引值
    private var mPopWin: PopupWindow? = null
    //控件宽高
    var mwidth = 0
    var mheight = 0

    init {
        this.setBackgroundColor(Color.WHITE)
        // x轴刻度值
        if (xLabel == null && yLabel == null) {
            xLabel = arrayOf("上午场", "下午场", "黄金场", "午夜场")
            yLabel = arrayOf("0", "150", "300", "450")
            yLabel2 = arrayOf("0%", "15%", "30%", "45%")
        }
        // 数据值
        if (data == null && data2 == null) {
            data = arrayOf("75", "100", "160", "60")
            data2 = arrayOf("20%", "35%", "18%", "20%")
        }
        //数据坐标
        mDataCoords = Array(data!!.size) { FloatArray(2) }
        mDataCoords2 = Array(data!!.size) { FloatArray(2) }

        // 新建画笔
        mDataLinePaint = Paint()       // 数据(点和连线)画笔
        mScaleLinePaint = Paint()      // 坐标(刻度线条)轴画笔
        mScaleValuePaint = Paint()      // 坐标值(刻度值)画笔
        // 画笔抗锯齿
        mDataLinePaint!!.isAntiAlias = true
        mScaleLinePaint!!.isAntiAlias = true
        mScaleValuePaint!!.isAntiAlias = true
    }


    //重写onMeasure方法,使得控件支持AT_MOST
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            measureWidth(widthMeasureSpec),
            measureHeight(heightMeasureSpec)
        )
        initParams(mwidth, mheight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawYAxisAndXScaleValue(canvas)    // 绘制y轴和x刻度值
        drawXAxisAndYScaleValue(canvas)    // 绘制x轴和y刻度值
        drawDataLines(canvas)              // 绘制数据连线
        drawVerticalLine(canvas)           // 绘制竖线
        drawText(canvas)                   // 绘制标题文字
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //触摸点坐标
        val touchX = event.x
        val touchY = event.y
        for (i in 0..(data!!.size - 1)) {
            //转折点坐标
            val dataX = mDataCoords!![i][0]
            val dataY = mDataCoords!![i][1]
            val data2X = mDataCoords2!![i][0]
            val data2Y = mDataCoords2!![i][1]
            // 控制触摸/点击的范围，在有效范围内才触发
            if ((Math.abs(touchX - dataX) < xScale / 2 && Math.abs(touchY - dataY) < yScale / 2) ||
                (Math.abs(touchX - data2X) < xScale / 2 && Math.abs(touchY - data2Y) < yScale / 2)
            ) {
                isClick = true
                clickIndex = i
                invalidate()     // 重绘展示数据点小圆圈和竖直线
                showDetails(i)   // 通过PopupWindow展示详细数据信息
                return true
            } else {
                hideDetails()
            }
            clickIndex = -1
            invalidate()
        }
        return super.onTouchEvent(event)
    }

    private fun initParams(width: Int, height: Int) {

        yScale = (height - 5 * ValueHeight) / 3 // y轴刻度单位
        xScale = (width - 2 * ValueWidth) / 5      // x轴刻度单位
        startPointX = ValueWidth - 5       // 开始绘图的x坐标(左上角)
        startPointY = 2.5f * ValueHeight - 10  // 开始UI图的y坐标
        xLength = (xLabel!!.size + 1) * xScale        // x轴长度
        yLength = (yLabel!!.size - 1) * yScale        // y轴长度

        val chartLineStrokeWidth = xScale / 50     // 坐标轴线条的线宽
        coordTextSize = xScale / 5             // 坐标刻度文字的大小
        val dataLineStrodeWidth = xScale / 50      // 数据线条的线宽

        // 设置画笔相关属性
        mScaleLinePaint!!.strokeWidth = chartLineStrokeWidth
        mScaleLinePaint!!.style = Paint.Style.STROKE
        mScaleLinePaint!!.color = Color.LTGRAY

        mScaleValuePaint!!.textSize = coordTextSize
        mScaleValuePaint!!.color = Color.BLACK

        mDataLinePaint!!.strokeWidth = dataLineStrodeWidth
        mDataLinePaint!!.color = Color.RED
        mScaleLinePaint!!.style = Paint.Style.STROKE
        mDataLinePaint!!.strokeCap = Paint.Cap.ROUND
    }

    //画出标题和图表下方文字以及圆点和直线
    private fun drawText(canvas: Canvas) {
        var string = "总场次:${getSum(data!!)}"
        mScaleValuePaint!!.getTextBounds(string, 0, string.length, bounds)
        var x = startPointX + xLength / 2 - bounds.width() / 2
        var y = startPointY + yLength + 2 * bounds.height() + 25
        canvas.drawText(string, x, y, mScaleValuePaint)

        var string2 = "人次占比        场次"
        mScaleValuePaint!!.getTextBounds(string2, 0, string2.length, bounds)
        var x2 = startPointX + xLength / 2 - bounds.width() / 2 - 20
        var y2 = y + bounds.height() + 10
        canvas.drawText(string2, x2, y2, mScaleValuePaint)

        //圆点 线
        var x3 = x2 + bounds.width() / 2 + 18
        var y3 = y2 - bounds.height() / 4
        mDataLinePaint!!.color = Color.RED
        canvas.drawCircle(x3, y3, xScale / 20, mDataLinePaint)
        canvas.drawLine(x3 + xScale / 20, y3, x3 + xScale / 20 + 18, y3, mDataLinePaint)
        mDataLinePaint!!.color = Color.WHITE
        canvas.drawCircle(x3, y3, xScale / 40, mDataLinePaint)
        var x4 = x3 + bounds.width() / 2
        var y4 = y3
        mDataLinePaint!!.color = Color.BLUE
        canvas.drawCircle(x4, y4, xScale / 20, mDataLinePaint)
        canvas.drawLine(x4 + xScale / 20, y3, x4 + xScale / 20 + 18, y3, mDataLinePaint)
        mDataLinePaint!!.color = Color.WHITE
        canvas.drawCircle(x4, y4, xScale / 40, mDataLinePaint)

        //标题文字
        val string3 = "排片分布"
        mScaleValuePaint!!.textSize = 50f
        mScaleValuePaint!!.getTextBounds(string3, 0, string3.length, bounds)
        val x5 = startPointX - bounds.width() / 2 + 15
        val y5 = startPointY - bounds.height() - 10
        canvas.drawText(string3, x5, y5, mScaleValuePaint)
        mScaleValuePaint!!.textSize = coordTextSize
    }

    /*
    * 画Y轴和X轴的刻度值
    * */
    private fun drawYAxisAndXScaleValue(canvas: Canvas) {
        //y轴
        canvas.drawLine(
            startPointX + 15,
            startPointY,
            startPointX + 15,
            startPointY + yLength,
            mScaleLinePaint
        )
        //右边的Y轴
        canvas.drawLine(
            startPointX + xLength - 15,
            startPointY,
            startPointX + xLength - 15,
            startPointY + yLength,
            mScaleLinePaint
        )
        //x轴刻度值
        for (i in 0..(xLabel!!.size - 1)) {
            mScaleValuePaint!!.getTextBounds(xLabel!![i], 0, xLabel!![i].length, bounds)
            canvas.drawText(
                xLabel!![i],
                startPointX + (i + 1) * xScale - bounds.width() / 2,
                startPointY + yLength + bounds.height() + yScale / 15,
                mScaleValuePaint
            )
        }
    }

    /**
     * 绘制x轴和y刻度值
     *
     * @param canvas
     */
    private fun drawXAxisAndYScaleValue(canvas: Canvas) {

        canvas.drawLine(
            startPointX + 15,
            startPointY + yLength,
            startPointX + xLength - 15,
            startPointY + yLength,
            mScaleLinePaint
        )
        for (i in 0..(yLabel!!.size - 1)) {
            //从左上角坐标开始画
            mScaleValuePaint!!.getTextBounds(
                yLabel!![(yLabel!!.size - 1) - i],
                0,
                yLabel!![(yLabel!!.size - 1) - i]!!.length,
                bounds
            )
            canvas.drawText(
                yLabel!![(yLabel!!.size - 1) - i],
                startPointX - bounds.width() - xScale / 15 + 15,
                startPointY + yScale * i + bounds.height() / 2,
                mScaleValuePaint
            )

            canvas.drawText(
                yLabel2!![(yLabel2!!.size - 1) - i],
                startPointX + xScale / 15 + xLength - 15,
                startPointY + yScale * i + bounds.height() / 2,
                mScaleValuePaint
            )

            //虚线
            mScaleLinePaint!!.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            canvas.drawLine(
                startPointX + 15,
                startPointY + i * yScale,
                startPointX + xLength - 15,
                startPointY + i * yScale,
                mScaleLinePaint
            )
            mScaleLinePaint!!.pathEffect = null
        }
    }

    /**
     * 绘制数据线条、柱子、圆点
     *
     * @param canvas
     */
    private fun drawDataLines(canvas: Canvas) {
        //画蓝线（场次）
        getDataRoords()
        mDataLinePaint!!.color = Color.BLUE
        for (i in 0..(data!!.size - 2)) {
            canvas.drawLine(
                mDataCoords!![i][0],
                mDataCoords!![i][1],
                mDataCoords!![i + 1][0],
                mDataCoords!![i + 1][1],
                mDataLinePaint
            )
        }
        //绘制蓝线数据点
        for (i in 0..(data!!.size - 1)) {
            mDataLinePaint!!.color = Color.BLUE
            canvas.drawCircle(mDataCoords!![i][0], mDataCoords!![i][1], xScale / 20, mDataLinePaint)
            mDataLinePaint!!.color = Color.WHITE
            canvas.drawCircle(mDataCoords!![i][0], mDataCoords!![i][1], xScale / 40, mDataLinePaint)
            Log.d("LineColumn", i.toString())
        }
        //画柱子
        val columnPaint = Paint()
        columnPaint.color = Color.GRAY
        columnPaint.style = Paint.Style.FILL
        columnPaint.alpha = 130
        for (i in 0..(data!!.size - 1)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.drawRoundRect(
                    mDataCoords!![i][0] - xScale / 8,
                    mDataCoords!![i][1] + xScale / 20,
                    mDataCoords!![i][0] + xScale / 8,
                    startPointY + yLength,
                    10f, 10f, columnPaint
                )
            } else {
                canvas.drawRect(
                    mDataCoords!![i][0] - xScale / 8,
                    mDataCoords!![i][1] + xScale / 20,
                    mDataCoords!![i][0] + xScale / 8,
                    startPointY + yLength,
                    columnPaint
                )
            }
        }

        //划红线（人次占比）
        mDataLinePaint!!.color = Color.RED
        for (i in 0..(data2!!.size - 2)) {
            canvas.drawLine(
                mDataCoords2!![i][0],
                mDataCoords2!![i][1],
                mDataCoords2!![i + 1][0],
                mDataCoords2!![i + 1][1],
                mDataLinePaint
            )
        }
        //绘制红线数据点
        for (i in 0..(data2!!.size - 1)) {
            mDataLinePaint!!.color = Color.RED
            canvas.drawCircle(mDataCoords2!![i][0], mDataCoords2!![i][1], xScale / 20, mDataLinePaint)
            mDataLinePaint!!.color = Color.WHITE
            canvas.drawCircle(mDataCoords2!![i][0], mDataCoords2!![i][1], xScale / 40, mDataLinePaint)
        }

    }

    /**
     * 绘制竖直线
     *
     * @param canvas
     */
    private fun drawVerticalLine(canvas: Canvas) {
        // 点击后，
        if (isClick && clickIndex > -1) {
            //画竖直线
            mDataLinePaint!!.color = Color.RED
            canvas.drawLine(
                mDataCoords!![clickIndex][0], startPointY + yLength,
                startPointX + xScale * (clickIndex + 1), startPointY + yScale - 35,
                mDataLinePaint
            )
        }
    }


    /**
     * 点击数据点后，展示详细的数据值
     */
    private fun showDetails(index: Int) {
        mPopWin?.dismiss()
        val tv1 = TextView(context)
        tv1.setTextColor(Color.WHITE)
        tv1.textSize = 10f
        tv1.text = "2018年11月18日周一" + "\n" +
                "${xLabel!![index]}:${data!![index]}场" + "\n" +
                "场次占比:${data!![index]}" + "\n" +
                "人次占比:${data2!![index]}"

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layout.setPadding(10, 10, 10, 10)
        layout.setBackgroundResource(com.example.mychart.R.drawable.rect_red_solid)
        layout.addView(tv1)

        mPopWin = PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mPopWin!!.setBackgroundDrawable(ColorDrawable(0))
        mPopWin!!.isFocusable = false
        // 根据坐标点的位置计算弹窗的展示位置
        val string = tv1.text.toString()
        val mpaint = Paint()
        val rect = Rect()
        mpaint.getTextBounds(string, 0, string.length, rect)
        //偏移量
        val xoff = startPointX + xScale * (index + 1) - rect.width() / 2 + 20
        val yoff = -(height + 0.75f * yScale).toInt() + 230

        mPopWin!!.showAsDropDown(this, xoff.toInt(), yoff)
        mPopWin!!.update()
    }

    private fun hideDetails() {
        mPopWin?.dismiss()
    }

    private fun measureWidth(measureSpec: Int): Int {
        var result = 0
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = 1260//默认大小350dp
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize)
            }
        }
        mwidth = result
        return result
    }

    private fun measureHeight(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = 770//默认大小220dp
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize)
            }
        }
        mheight = result
        return result
    }


    /**
     * 获取数据值的坐标点
     *
     * @return 数据点的坐标
     */
    private fun getDataRoords() {
        for (i in 0..(data!!.size - 1)) {//默认Y轴坐标从零开始
            mDataCoords!![i][0] = startPointX + (i + 1) * xScale //x
            mDataCoords!![i][1] = yLength - ((data!![i].toFloat() * yLength) /
                    yLabel!![yLabel!!.size - 1]!!.toFloat()) + startPointY

            mDataCoords2!![i][0] = startPointX + (i + 1) * xScale //x
            mDataCoords2!![i][1] = startPointY + yLength -
                    data2!![i].substring(0, data2!![i].length - 1).toFloat() /
                    yLabel2!![yLabel2!!.size - 1]!!.substring(
                        0,
                        yLabel2!![yLabel2!!.size - 1]!!.length - 1
                    ).toFloat() * yLength
        }
        /*for (i in 0..(data2!!.size - 1)) {//默认Y轴坐标从零开始
            mDataCoords2!![i][0] = startPointX + (i + 1) * xScale //x
            mDataCoords2!![i][1] = startPointY + yLength -
                    data2!![i].substring(0, data2!![i].length - 1).toFloat() /
                    yLabel2!![yLabel2!!.size - 1]!!.substring(
                        0,
                        yLabel2!![yLabel2!!.size - 1]!!.length - 1
                    ).toFloat() * yLength
        }*/
    }

    //求和
    private fun getSum(array: Array<String>): Int {
        var result = 0
        for (i in 0..(array.size - 1)) {
            result += array[i].toInt()
        }
        return result
    }






    //设置x轴刻度值
    fun setxLabel(xLabel: Array<String>) {
        this.xLabel = xLabel
    }

    /**
     * 设置数据
     *
     * @param data 数据值
     */
    fun setData(data: Array<String>) {
        this.data = data
    }


    /**
     * 重新设置x轴刻度、数据、标题后必须刷新重绘
     */
    fun fresh() {
//        init()
        requestLayout()
        postInvalidate()
    }
}