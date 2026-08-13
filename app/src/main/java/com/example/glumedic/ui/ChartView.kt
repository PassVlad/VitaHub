package com.example.glumedic.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class DataPoint(
    val timestamp: Long,   // System.currentTimeMillis()
    val value: Int,
    val value2: Int? = null
)

/**
 * График в стиле веб-версии (Recharts ComposedChart):
 * - плавная (монотонная) линия #A78BFA и градиентная заливка 35% -> 0
 * - пунктирная сетка rgba(148,163,184,0.1) со штрихом 4/6
 * - авто-домен по Y с паддингом: pad = max((s-o)*0.15, s*0.05, 1)
 * - полоса нормы (ReferenceArea min..max, #A78BFA 7%)
 * - пунктирные референс-линии min (#60A5FA) и max (#F87171)
 * - точки r5 с обводкой #060A13, для второй серии r4 (#E879F9)
 * - тултип по тапу: дата, значение, статус «в норме / вне нормы»
 */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<DataPoint>()

    // Масштаб по Y: значение точки делится на divisor при отображении
    // (глюкоза хранится в десятых долях: 8.3 ммоль/л -> value=83, divisor=10;
    //  давление хранится в целых мм рт. ст.: value=120, divisor=1)
    private var valueDivisor = 1
    private var unitLabel = ""
    private var decimals = 1

    // Пороги нормы (в единицах отображения, напр. ммоль/л)
    private var metricMin: Double? = null
    private var metricMax: Double? = null
    private var metricMin2: Double? = null
    private var metricMax2: Double? = null

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    // Максимальная крутизна сглаживающей кривой (в пикселях на пиксель),
    // чтобы близкие по времени точки не давали выбросов за пределы графика
    private val MAX_TANGENT = 3f

    // ---------- Кисти ----------

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A78BFA")
        strokeWidth = dp(2.5f)
        style = Paint.Style.STROKE
    }

    private val secondLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E879F9")
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f)
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A78BFA")
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#060A13")
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
    }

    private val secondPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E879F9")
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(26, 148, 163, 184) // rgba(148,163,184,0.1)
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(6f)), 0f)
    }

    private val minLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(102, 96, 165, 250) // #60A5FA opacity 0.4
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(4f)), 0f)
    }

    private val maxLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(102, 248, 113, 113) // #F87171 opacity 0.4
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(4f), dp(4f)), 0f)
    }

    private val normBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(18, 167, 139, 250) // #A78BFA fillOpacity 0.07
        style = Paint.Style.FILL
    }

    // Текст меток (цвет и размер как на сайте: #77718F, 11px)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#77718F")
        textSize = dp(11f)
        textAlign = Paint.Align.CENTER
    }

    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#77718F")
        textSize = dp(11f)
        textAlign = Paint.Align.RIGHT
    }

    // Тултип
    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#131D33")
        style = Paint.Style.FILL
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(51, 148, 163, 184) // rgba(148,163,184,0.2)
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }

    private val tooltipTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = dp(13f)
        textAlign = Paint.Align.LEFT
    }

    private val tooltipValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EFE9FF")
        textSize = dp(14f)
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }

    private val tooltipOkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34D399")
        textSize = dp(13f)
        textAlign = Paint.Align.LEFT
    }

    private val tooltipBadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F87171")
        textSize = dp(13f)
        textAlign = Paint.Align.LEFT
    }

    private val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

    // Выбранная точка для тултипа
    private var selectedIndex: Int = -1

    // Текущий домен по Y (для расчёта референс-линий вне onDraw)
    private var domainLow = 0.0
    private var domainRange = 1.0
    private var domainPadTop = 0f
    private var domainPlotH = 0f

    fun setValueScale(divisor: Int, unit: String) {
        valueDivisor = if (divisor > 0) divisor else 1
        decimals = if (valueDivisor == 1) 0 else 1
        unitLabel = unit
        invalidate()
    }

    fun setDecimals(d: Int) {
        decimals = if (d >= 0) d else 1
        invalidate()
    }

    fun setMetricLimits(min: Double?, max: Double?) {
        metricMin = min
        metricMax = max
        invalidate()
    }

    fun setMetricLimits2(min2: Double?, max2: Double?) {
        metricMin2 = min2
        metricMax2 = max2
        invalidate()
    }

    fun setPoints(points: List<DataPoint>) {
        dataPoints.clear()
        dataPoints.addAll(points)
        selectedIndex = -1
        invalidate()
    }

    fun clear() {
        dataPoints.clear()
        selectedIndex = -1
        invalidate()
    }

    private fun formatValue(raw: Int): String {
        val display = raw.toDouble() / valueDivisor
        return String.format(Locale.US, "%.${decimals}f", display)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Фильтруем: только последние 7 дней
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val visiblePoints = dataPoints.filter { it.timestamp >= weekAgo }

        val w = width.toFloat()
        val h = height.toFloat()

        val padLeft = dp(50f)
        val padRight = dp(12f)
        val padTop = dp(16f)
        val padBottom = dp(46f)

        val plotW = w - padLeft - padRight
        val plotH = h - padTop - padBottom

        if (visiblePoints.isEmpty() || plotW <= 0f || plotH <= 0f) return

        // Домен по Y как на сайте: pad = max((s-o)*0.15, s*0.05, 1)
        val displayValues = visiblePoints.flatMap { listOfNotNull(it.value, it.value2) }
            .map { it.toDouble() / valueDivisor }
        val s = displayValues.max()
        val o = displayValues.min()
        val pad = max(max((s - o) * 0.15, s * 0.05), 1.0)
        val low = floor((o - pad) * 10) / 10
        val high = ceil((s + pad) * 10) / 10
        val range = (high - low).coerceAtLeast(0.1)

        domainLow = low
        domainRange = range
        domainPadTop = padTop
        domainPlotH = plotH

        // Диапазон времени
        val minTime = visiblePoints.first().timestamp
        val maxTime = visiblePoints.last().timestamp
        val timeRange = (maxTime - minTime).coerceAtLeast(1L).toFloat()

        fun xFromTimestamp(ts: Long): Float {
            val fraction = if (visiblePoints.size <= 1) 0.5f else (ts - minTime) / timeRange
            return padLeft + plotW * fraction
        }

        fun yFromValue(raw: Int): Float {
            val fraction = ((raw.toDouble() / valueDivisor - low) / range).toFloat()
            return padTop + plotH * (1f - fraction)
        }

        val baselineY = padTop + plotH

        // Полоса нормы + референс-линии
        val minD = metricMin
        val maxD = metricMax
        if (minD != null && maxD != null && maxD > minD) {
            val yTop = yFromRawDisplay(maxD)
            val yBottom = yFromRawDisplay(minD)
            val top = yTop.coerceIn(padTop, baselineY)
            val bottom = yBottom.coerceIn(padTop, baselineY)
            if (bottom > top) {
                canvas.drawRect(padLeft, top, w - padRight, bottom, normBandPaint)
            }
            if (minD in low..high) {
                val y = yFromRawDisplay(minD)
                canvas.drawLine(padLeft, y, w - padRight, y, minLinePaint)
            }
            if (maxD in low..high) {
                val y = yFromRawDisplay(maxD)
                canvas.drawLine(padLeft, y, w - padRight, y, maxLinePaint)
            }
        }

        // Сетка (горизонтальные линии) с подписями по Y
        val gridCount = 5
        for (i in 0..gridCount) {
            val value = low + range * i / gridCount
            val y = padTop + plotH * (1f - i / gridCount.toFloat())
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint)
            canvas.drawText(
                String.format(Locale.US, "%.${decimals}f", value),
                padLeft - dp(8f),
                y + yLabelPaint.textSize / 3f,
                yLabelPaint
            )
        }

        // Вертикальная пунктирная сетка по точкам
        for (dp in visiblePoints) {
            val x = xFromTimestamp(dp.timestamp)
            if (x > padLeft && x < w - padRight) {
                canvas.drawLine(x, padTop, x, baselineY, gridPaint)
            }
        }

        // Вторая серия (например, диастолическое давление)
        val secondSeries = visiblePoints.mapNotNull { dp ->
            dp.value2?.let { DataPoint(dp.timestamp, it) }
        }

        // Линия и заливка клипятся областью графика (как clipPath в recharts),
        // чтобы сглаживающая кривая не выходила за пределы окна
        canvas.save()
        canvas.clipRect(padLeft, padTop, w - padRight, baselineY)

        // Градиентная заливка + линия первой серии
        if (visiblePoints.size >= 2) {
            val path = smoothPath(
                visiblePoints.map { PointF(xFromTimestamp(it.timestamp), yFromValue(it.value)) }
            )
            val fillPath = Path(path)
            fillPath.lineTo(xFromTimestamp(visiblePoints.last().timestamp), baselineY)
            fillPath.lineTo(xFromTimestamp(visiblePoints.first().timestamp), baselineY)
            fillPath.close()
            fillPaint.shader = LinearGradient(
                0f, padTop, 0f, baselineY,
                Color.argb(89, 167, 139, 250), // #A78BFA 35%
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(path, linePaint)
        }

        // Линия второй серии
        if (secondSeries.size >= 2) {
            val path2 = smoothPath(
                secondSeries.map { PointF(xFromTimestamp(it.timestamp), yFromValue(it.value)) }
            )
            canvas.drawPath(path2, secondLinePaint)
        }

        canvas.restore()

        // Точки + метки дат
        for (dp in visiblePoints) {
            val x = xFromTimestamp(dp.timestamp)
            val y = yFromValue(dp.value)

            canvas.drawCircle(x, y, dp(5f), pointPaint)
            canvas.drawCircle(x, y, dp(5f), pointStrokePaint)

            val label = shortDateFormat.format(Date(dp.timestamp))
            canvas.drawText(label, x, y + dp(28f), labelPaint)

            dp.value2?.let {
                val y2 = yFromValue(it)
                canvas.drawCircle(x, y2, dp(4f), secondPointPaint)
                canvas.drawCircle(x, y2, dp(4f), pointStrokePaint)
            }
        }

        // Тултип
        if (selectedIndex in visiblePoints.indices) {
            val dp = visiblePoints[selectedIndex]
            drawTooltip(canvas, dp, xFromTimestamp(dp.timestamp), yFromValue(dp.value))
        }
    }

    private fun yFromRawDisplay(display: Double): Float {
        val fraction = ((display - domainLow) / domainRange).toFloat()
        return domainPadTop + domainPlotH * (1f - fraction)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val visible = dataPoints.filter { it.timestamp >= weekAgo }
            if (visible.isEmpty()) return true

            val padLeft = dp(50f)
            val plotW = (width - padLeft - dp(12f)).coerceAtLeast(1f)
            val minTime = visible.first().timestamp
            val maxTime = visible.last().timestamp
            val timeRange = (maxTime - minTime).coerceAtLeast(1L).toFloat()

            fun xFor(ts: Long): Float {
                val fraction = if (visible.size <= 1) 0.5f else (ts - minTime) / timeRange
                return padLeft + plotW * fraction
            }

            val nearest = visible.indices.minByOrNull { kotlin.math.abs(xFor(visible[it].timestamp) - x) }
            val dist = nearest?.let { kotlin.math.abs(xFor(visible[it].timestamp) - x) } ?: Float.MAX_VALUE
            val prev = selectedIndex
            selectedIndex = if (nearest != null && dist <= dp(48f)) nearest else -1
            if (prev != selectedIndex) invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun drawTooltip(canvas: Canvas, dp: DataPoint, x: Float, y: Float) {
        val minD = metricMin
        val maxD = metricMax
        val min2 = metricMin2
        val max2 = metricMax2
        val v = dp.value.toDouble() / valueDivisor
        val v2 = dp.value2?.toDouble()?.div(valueDivisor)
        val hasLimits = minD != null && maxD != null
        val inRange = !hasLimits ||
            (v in minD!!..maxD!! && (v2 == null || (min2 == null || max2 == null || v2 in min2..max2)))

        val valueText = buildString {
            append(formatValue(dp.value))
            dp.value2?.let { append("/").append(formatValue(it)) }
            if (unitLabel.isNotEmpty()) append(" ").append(unitLabel)
        }
        val statusText = if (hasLimits) {
            if (inRange) "В норме: показатели в норме." else "Вне нормы: показатели вне нормы."
        } else null

        val title = dateFormat.format(Date(dp.timestamp))

        val pad = dp(12f)
        val lineGap = dp(4f)
        val textW = maxOf(
            tooltipTitlePaint.measureText(title),
            tooltipValuePaint.measureText(valueText),
            statusText?.let { tooltipOkPaint.measureText(it) } ?: 0f
        )
        val cardW = textW + pad * 2
        val titleH = tooltipTitlePaint.textSize
        val valueH = tooltipValuePaint.textSize
        val statusH = if (statusText != null) tooltipOkPaint.textSize + lineGap else 0f
        val cardH = pad + titleH + lineGap + valueH + (if (statusText != null) lineGap + statusH else 0f) + pad

        var left = (x - cardW / 2).coerceIn(dp(4f), width - cardW - dp(4f))
        var top = y - cardH - dp(14f)
        if (top < dp(4f)) top = y + dp(14f)
        if (top + cardH > height - dp(4f)) top = height - cardH - dp(4f)
        left = left.coerceAtLeast(dp(4f))
        top = top.coerceAtLeast(dp(4f))

        val rect = RectF(left, top, left + cardW, top + cardH)
        canvas.drawRoundRect(rect, dp(12f), dp(12f), tooltipBgPaint)
        canvas.drawRoundRect(rect, dp(12f), dp(12f), tooltipBorderPaint)

        var ty = top + pad + titleH
        canvas.drawText(title, left + pad, ty, tooltipTitlePaint)
        ty += lineGap + valueH
        canvas.drawText(valueText, left + pad, ty, tooltipValuePaint)
        if (statusText != null) {
            ty += lineGap + tooltipOkPaint.textSize
            val paint = if (inRange) tooltipOkPaint else tooltipBadPaint
            canvas.drawText(statusText, left + pad, ty, paint)
        }
    }

    /** Монотонная кубическая интерполяция (как curveMonotoneX в recharts). */
    private fun smoothPath(points: List<PointF>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x, points[0].y)
        if (points.size < 2) return path

        val n = points.size
        val dx = FloatArray(n - 1)
        val dy = FloatArray(n - 1)
        val m = FloatArray(n - 1)
        for (i in 0 until n - 1) {
            dx[i] = points[i + 1].x - points[i].x
            dy[i] = points[i + 1].y - points[i].y
            m[i] = if (dx[i] != 0f) dy[i] / dx[i] else 0f
        }

        val t = FloatArray(n)
        t[0] = m[0].coerceIn(-MAX_TANGENT, MAX_TANGENT)
        t[n - 1] = m[n - 2].coerceIn(-MAX_TANGENT, MAX_TANGENT)
        for (i in 1 until n - 1) {
            if (m[i - 1] == 0f || m[i] == 0f) {
                t[i] = 0f
            } else {
                val w1 = 2f * dx[i] + dx[i - 1]
                val w2 = dx[i] + 2f * dx[i - 1]
                val raw = (w1 + w2) / (w1 / m[i - 1] + w2 / m[i])
                t[i] = raw.coerceIn(-MAX_TANGENT, MAX_TANGENT)
            }
        }

        for (i in 0 until n - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val h = dx[i]
            // Почти вертикальный участок (точки вплотную по времени) —
            // соединяем прямой, чтобы линия не «стреляла» за пределы графика
            if (kotlin.math.abs(h) < 1f) {
                path.lineTo(p1.x, p1.y)
            } else {
                path.cubicTo(
                    p0.x + h / 3f, p0.y + h / 3f * t[i],
                    p1.x - h / 3f, p1.y - h / 3f * t[i + 1],
                    p1.x, p1.y
                )
            }
        }
        return path
    }
}
