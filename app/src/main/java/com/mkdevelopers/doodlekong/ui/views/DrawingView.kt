package com.mkdevelopers.doodlekong.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.mkdevelopers.doodlekong.data.remote.ws.model.BaseModel
import com.mkdevelopers.doodlekong.data.remote.ws.model.DrawAction
import com.mkdevelopers.doodlekong.data.remote.ws.model.DrawData
import com.mkdevelopers.doodlekong.util.Constants
import java.util.Stack
import kotlin.math.abs

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): View(context, attrs) {

    private var viewWidth: Int? = null
    private var viewHeight: Int? = null
    private var bmp: Bitmap? = null
    private var canvas: Canvas? = null
    private var curX: Float? = null
    private var curY: Float? = null
    var smoothness = 5
    var isDrawing = false

    private var paint = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Constants.DEFAULT_PAINT_THICKNESS
    }

    private var path = Path()
    private var paths = Stack<PathData>()
    private var pathDataChangedListener: ((Stack<PathData>) -> Unit)? = null

    private var startedTouch = false
    var roomName: String? = null
    var isUserDrawing = false
        set(value) {
            isEnabled = value
            field = value
        }

    private var onDrawListener: ((DrawData) -> Unit)? = null

    fun setOnDrawListener(listener: (DrawData) -> Unit) {
        onDrawListener = listener
    }

    fun setPaths(pathData: Stack<PathData>) {
        this.paths = pathData
    }

    fun setPathDataChangedListener(listener: (Stack<PathData>) -> Unit) {
        pathDataChangedListener = listener
    }

    fun update(drawActions: List<BaseModel>) {
        drawActions.forEach { drawAction ->
            when(drawAction) {
                is DrawData -> {
                    when(drawAction.motionEvent) {
                        MotionEvent.ACTION_DOWN -> startedTouchExternally(drawAction)
                        MotionEvent.ACTION_MOVE -> movedTouchExternally(drawAction)
                        MotionEvent.ACTION_UP   -> releasedTouchExternally(drawAction)
                    }
                }
                is DrawAction -> {
                    when(drawAction.action) {
                        DrawAction.ACTION_UNDO -> undo()
                    }
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bmp!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val initialColor = paint.color
        val initialThickness = paint.strokeWidth

        for(pathData in paths) {
            paint.apply {
                color = pathData.color
                strokeWidth = pathData.thickness
            }
            canvas?.drawPath(pathData.path, paint)
        }

        paint.apply {
            color = initialColor
            strokeWidth = initialThickness
        }
        canvas?.drawPath(path, paint)
    }

    fun startedTouchExternally(drawData: DrawData) {
        parseDrawData(drawData).apply {
            paint.color = color
            paint.strokeWidth = thickness
            path.reset()
            path.moveTo(fromX, fromY)
            invalidate()
            startedTouch = true
        }
    }

    fun movedTouchExternally(drawData: DrawData) {
        parseDrawData(drawData).apply {
            val dx = abs(toX - fromX)
            val dy = abs(toY - fromY)

            if(!startedTouch) {
                startedTouchExternally(drawData)
            }

            if(dx >= smoothness || dy >= smoothness) {
                path.quadTo(
                    fromX,
                    fromY,
                    (fromX + toX) / 2f,
                    (fromY + toY) / 2f
                )
                invalidate()
            }
        }
    }

    fun releasedTouchExternally(drawData: DrawData) {
        parseDrawData(drawData).apply {
            path.lineTo(fromX, fromY)
            canvas?.drawPath(path, paint)
            paths.push(
                PathData(path, paint.color, paint.strokeWidth)
            )

            pathDataChangedListener?.invoke(paths)

            path = Path()
            invalidate()
            startedTouch = false
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        path.reset()
        invalidate()
    }

    fun undo() {
        if(paths.isNotEmpty()) {
            paths.pop()
            pathDataChangedListener?.invoke(paths)
            invalidate()
        }
    }

    private fun startedTouch(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        curX = x
        curY = y

        onDrawListener?.let { drawListener ->
            val drawData = createDrawData(
                fromX = x,
                fromY = y,
                toX = x,
                toY = y,
                motionEvent = MotionEvent.ACTION_DOWN
            )
            drawListener(drawData)
        }
        invalidate()
    }

    private fun movedTouch(toX: Float, toY: Float) {
        val dx = abs(toX - (curX ?: return))
        val dy = abs(toY - (curY ?: return))

        if(dx >= smoothness || dy >= smoothness) {
            isDrawing = true
            path.quadTo(curX!!, curY!!, (curX!! + toX) / 2f, (curY!! + toY) / 2f)

            onDrawListener?.let { drawListener ->
                val drawData = createDrawData(
                    fromX = curX!!,
                    fromY = curY!!,
                    toX = toX,
                    toY = toY,
                    motionEvent = MotionEvent.ACTION_MOVE
                )

                drawListener(drawData)
            }

            curX = toX
            curY = toY
            invalidate()
        }
    }

    private fun releasedTouch() {
        isDrawing = false
        path.lineTo(curX ?: return, curY ?: return)
        paths.push(PathData(path, paint.color, paint.strokeWidth))

        pathDataChangedListener?.invoke(paths)

        onDrawListener?.let { drawListener ->
            val drawData = createDrawData(
                fromX = curX!!,
                fromY = curY!!,
                toX = curX!!,
                toY = curY!!,
                motionEvent = MotionEvent.ACTION_UP
            )

            drawListener(drawData)
        }

        path = Path()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(!isEnabled) {
            return false
        }

        val newX = event?.x
        val newY = event?.y

        when(event?.action) {
            MotionEvent.ACTION_DOWN -> startedTouch(newX ?: return false, newY ?: return false)
            MotionEvent.ACTION_MOVE -> movedTouch(newX ?: return false, newY ?: return false)
            MotionEvent.ACTION_UP   -> releasedTouch()
        }

        return true
    }

    private fun parseDrawData(drawData: DrawData): DrawData {
        return drawData.copy(
            fromX = drawData.fromX * viewWidth!!,
            fromY = drawData.fromY * viewHeight!!,
            toX   = drawData.toX * viewWidth!!,
            toY   = drawData.toY * viewHeight!!
        )
    }

    private fun createDrawData(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        motionEvent: Int
    ): DrawData {
        return DrawData(
            roomName    = roomName ?: throw IllegalStateException("Must set the room name in drawing view"),
            color       = paint.color,
            thickness   = paint.strokeWidth,
            fromX       = fromX / viewWidth!!,
            fromY       = fromY / viewHeight!!,
            toX         = toX / viewWidth!!,
            toY         = toY / viewHeight!!,
            motionEvent = motionEvent
        )
    }

    fun finishOffDrawing() {
        isDrawing = false
        path.lineTo(
        curX ?: return,
        curY ?: return
        )
        canvas?.drawPath(path, paint)
        paths.push(
            PathData(
                path = path,
                color = paint.color,
                thickness = paint.strokeWidth
            )
        )
        pathDataChangedListener?.invoke(paths)
        path = Path()
        invalidate()
    }

    fun setThickness(thickness: Float) {
        paint.strokeWidth = thickness
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    fun clear() {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
        paths.clear()
    }

    data class PathData(
        val path: Path,
        val color: Int,
        val thickness: Float
    )
}