package com.mplus.ticket.view

import android.R
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView


class LineChart(mcontext: Context?, attributeSet: AttributeSet?, def: Int) : View(mcontext, attributeSet, def) {

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
    private var data: Array<String>? = null

    private var mDataCoords: Array<FloatArray>? = null//转折点坐标

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
        if (xLabel == null) {
            xLabel = arrayOf("12/11", "12/12", "12/13", "12/14", "12/15", "12/16", "12/17")
        }
        // 数据值
        if (data == null) {
            data = arrayOf("1500", "3000", "3000", "1000", "1800", "4000", "4500")
        }

        mDataCoords = Array(data!!.size) { FloatArray(2) }
        yLabel = arrayOf("0", "1500", "3000", "4500")

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
        drawDataPoints(canvas)             // 绘制数据点
    }

    /*
    * 画Y轴和X轴的刻度值
    * */
    private fun drawYAxisAndXScaleValue(canvas: Canvas) {
        //y轴
        canvas.drawLine(startPointX,//+ i * xScale
                startPointY,
                startPointX,//+ i * xScale
                startPointY + yLength,
                mScaleLinePaint)
        //右边的Y轴
        canvas.drawLine(startPointX + xLength,
                startPointY,
                startPointX + xLength,
                startPointY + yLength,
                mScaleLinePaint)
        //x轴刻度值
        for (i in 0..(xLabel!!.size - 1)) {
            mScaleValuePaint!!.getTextBounds(xLabel!![i], 0, xLabel!![i].length, bounds)//用于获取坐标值文字的宽高
            if (i == 0) {
                canvas.drawText(xLabel!![i],
                        startPointX,
                        startPointY + yLength + bounds.height() + yScale / 15,
                        mScaleValuePaint)
            } else {
                canvas.drawText(xLabel!![i],
                        startPointX + i * xScale - bounds.width() / 2,
                        startPointY + yLength + bounds.height() + yScale / 15,
                        mScaleValuePaint)
            }
        }
    }

    /**
     * 绘制x轴和y刻度值
     *
     * @param canvas
     */
    private fun drawXAxisAndYScaleValue(canvas: Canvas) {

        canvas.drawLine(startPointX,
                startPointY + yLength,
                startPointX + xLength,
                startPointY + yLength,
                mScaleLinePaint)
        for (i in 0..(yLabel!!.size - 1)) {
            //从左上角坐标开始画
            mScaleValuePaint!!.getTextBounds(yLabel!![(yLabel!!.size - 1) - i], 0, yLabel!![(yLabel!!.size - 1) - i]!!.length, bounds)
            canvas.drawText(yLabel!![(yLabel!!.size - 1) - i],
                    startPointX - bounds.width() - xScale / 15,
                    startPointY + yScale * i + bounds.height() / 2,
                    mScaleValuePaint)
            //虚线
            mScaleLinePaint!!.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            canvas.drawLine(startPointX,
                    startPointY + i * yScale,
                    startPointX + xLength,
                    startPointY + i * yScale,
                    mScaleLinePaint)
            mScaleLinePaint!!.pathEffect = null
        }
    }

    /**
     * 绘制数据线条
     *
     * @param canvas
     */
    private fun drawDataLines(canvas: Canvas) {

        getDataRoords()
        for (i in 0..(data!!.size - 2)) {
            canvas.drawLine(mDataCoords!![i][0], mDataCoords!![i][1], mDataCoords!![i + 1][0], mDataCoords!![i + 1][1], mDataLinePaint)
        }

        //画阴影
        val path = Path()
        mDataLinePaint!!.alpha = 80
        path.moveTo(startPointX, startPointY + yLength)//原点
        var flag = 0
        for (i in 0..(data!!.size - 1)) {
            path.lineTo(startPointX + xScale * i, yLength - ((data!![i].toFloat() * yLength) / yLabel!![yLabel!!.size - 1]!!.toFloat()) + startPointY)
            flag = i
        }
        path.lineTo(startPointX + (xScale * flag), startPointY + yLength)
        Log.d("LineChart", flag.toString())
        canvas.drawPath(path, mDataLinePaint)
        mDataLinePaint!!.alpha = 255
    }

    /**
     * 绘制数据点和直线
     *
     * @param canvas
     */
    private fun drawDataPoints(canvas: Canvas) {
        // 点击后，
        if (isClick && clickIndex > -1) {
            //绘制数据点
            mDataLinePaint!!.color = resources.getColor(R.color.holo_orange_light)
            canvas.drawCircle(mDataCoords!![clickIndex][0], mDataCoords!![clickIndex][1], xScale / 10, mDataLinePaint)
            mDataLinePaint!!.color = Color.WHITE
            canvas.drawCircle(mDataCoords!![clickIndex][0], mDataCoords!![clickIndex][1], xScale / 20, mDataLinePaint)

            //画竖直线
            mDataLinePaint!!.color = resources.getColor(R.color.holo_orange_light)
            if (mDataCoords!![clickIndex][1] - xScale / 20 >= startPointY + yScale - 10) {
                canvas.drawLine(
                    mDataCoords!![clickIndex][0], mDataCoords!![clickIndex][1] - xScale / 20 - 5,
                    startPointX + xScale * clickIndex, startPointY + yScale - 15,
                    mDataLinePaint
                )
            } else {
                canvas.drawLine(
                    mDataCoords!![clickIndex][0], mDataCoords!![clickIndex][1] + xScale / 20 + 5,
                    startPointX + xScale * clickIndex, startPointY + yScale - 15,
                    mDataLinePaint
                )
            }
        }


    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //触摸点坐标
        val touchX = event.x
        val touchY = event.y
        for (i in 0..(data!!.size - 1)) {
            //转折点坐标
            val dataX = mDataCoords!![i][0]
            val dataY = mDataCoords!![i][1]
            // 控制触摸/点击的范围，在有效范围内才触发
            if (Math.abs(touchX - dataX) < xScale / 2 && Math.abs(touchY - dataY) < yScale / 2) {
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

    /**
     * 点击数据点后，展示详细的数据值
     */
    private fun showDetails(index: Int) {
        mPopWin?.dismiss()
        val tv1 = TextView(context)
        tv1.setTextColor(Color.WHITE)
        tv1.textSize = 10f
        tv1.text = "2018年11月18日周一" + "\n" + "总场次:${data!![index]}" + "\n" + "场次总占比:${data!![index]}"

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layout.setPadding(10, 10, 10, 10)
        layout.setBackgroundResource(com.example.mychart.R.drawable.rect_orange_solid)
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
        val xoff = startPointX + xScale * index - (rect.width() + 90) / 2
        val yoff = -(height + 0.75f * yScale).toInt() + 260

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
            result = 1225//默认大小350dp
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

    private fun initParams(width: Int, height: Int) {

        yScale = (height - 1.5f * ValueHeight) / 3f - 30         // y轴刻度单位
        xScale = (width - 2f * ValueWidth) / 6f      // x轴刻度单位
        startPointX = (ValueWidth + 1) + 10      // 开始绘图的x坐标(左上角)
        startPointY = 0.5f * ValueHeight + 30   // 开始UI图的y坐标
        xLength = (xLabel!!.size - 1) * xScale        // x轴长度
        yLength = (yLabel!!.size - 1) * yScale        // y轴长度

        val chartLineStrokeWidth = xScale / 50     // 坐标轴线条的线宽
        coordTextSize = xScale / 5             // 坐标刻度文字的大小
        val dataLineStrodeWidth = xScale / 30      // 数据线条的线宽

        // 设置画笔相关属性
        mScaleLinePaint!!.strokeWidth = chartLineStrokeWidth
        mScaleLinePaint!!.style = Paint.Style.STROKE
        mScaleLinePaint!!.color = Color.GRAY

        mScaleValuePaint!!.textSize = coordTextSize
        mScaleValuePaint!!.color = Color.GRAY

        mDataLinePaint!!.style=Paint.Style.FILL
        mDataLinePaint!!.strokeWidth = dataLineStrodeWidth
        mDataLinePaint!!.color = resources.getColor(R.color.holo_orange_light)
        mScaleLinePaint!!.style = Paint.Style.STROKE
        mDataLinePaint!!.strokeCap = Paint.Cap.ROUND
    }


    /**
     * 获取数据值的坐标点
     *
     * @return 数据点的坐标
     */
    private fun getDataRoords() {
        for (i in 0..(data!!.size - 1)) {//默认Y轴坐标从零开始
            mDataCoords!![i][0] = startPointX + i * xScale //x
            mDataCoords!![i][1] = yLength - ((data!![i].toFloat() * yLength) / yLabel!![yLabel!!.size - 1]!!.toFloat()) + startPointY
        }
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