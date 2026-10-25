package com.financewatch.traderapp.ui.markets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * View custom Canvas care deseneaza un chart de linie cu gradient fill.
 * Nu foloseste nicio librarie externa.
 *
 * Caracteristici:
 *  - Linie neteda (cubicTo bezier) pentru tranzitii line
 *  - Gradient fill sub linie (verde pentru pozitiv, rosu pentru negativ)
 *  - Linie orizontala punctata la nivelul pretului de inchidere anterior
 *  - Capete de linie rotunjite
 *  - Tooltip punct final (pretul curent)
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ─── Date ─────────────────────────────────────────────────────────────────
    private var pricePoints: List<Float> = emptyList()
    private var isPositive: Boolean = true

    // ─── Paint-uri ────────────────────────────────────────────────────────────
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style  = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 1.5f
        pathEffect  = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        color       = Color.parseColor("#55FFFFFF")
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // ─── Culori ───────────────────────────────────────────────────────────────
    private val colorPositive = Color.parseColor("#4CAF50")  // verde Material
    private val colorNegative = Color.parseColor("#F44336")  // rosu Material

    // ─── Path refolosit ───────────────────────────────────────────────────────
    private val linePath = Path()
    private val fillPath = Path()

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Seteaza datele de afisat si declanseaza un redraw.
     * @param points  seria de preturi (curatata de null)
     * @param positive true daca pretul a crescut fata de inchiderea anterioara
     */
    fun setData(points: List<Float>, positive: Boolean) {
        pricePoints = points
        isPositive  = positive
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pricePoints.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padH = h * 0.12f   // padding vertical 12%

        val minY = pricePoints.min()
        val maxY = pricePoints.max()
        val range = (maxY - minY).takeIf { it > 0f } ?: 1f

        // Functie de mapare pret → coordonata Y pe ecran
        fun toX(i: Int): Float = i / (pricePoints.size - 1).toFloat() * w
        fun toY(p: Float): Float = padH + (1f - (p - minY) / range) * (h - 2 * padH)

        val lineColor = if (isPositive) colorPositive else colorNegative

        // ── 1. Linie curba ─────────────────────────────────────────────────
        linePath.reset()
        linePath.moveTo(toX(0), toY(pricePoints[0]))
        for (i in 1 until pricePoints.size) {
            val prevX = toX(i - 1); val prevY = toY(pricePoints[i - 1])
            val currX = toX(i);     val currY = toY(pricePoints[i])
            val cpX = (prevX + currX) / 2f
            linePath.cubicTo(cpX, prevY, cpX, currY, currX, currY)
        }

        // ── 2. Fill cu gradient ─────────────────────────────────────────────
        fillPath.set(linePath)
        fillPath.lineTo(toX(pricePoints.size - 1), h)
        fillPath.lineTo(toX(0), h)
        fillPath.close()

        val gradient = LinearGradient(
            0f, padH, 0f, h,
            intArrayOf(
                Color.argb(100, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Color.argb(0,   Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor))
            ),
            null,
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradient
        canvas.drawPath(fillPath, fillPaint)

        // ── 3. Linie propriu-zisa ───────────────────────────────────────────
        linePaint.color = lineColor
        canvas.drawPath(linePath, linePaint)

        // ── 4. Linie punctata la nivelul primului pret (prev close) ─────────
        val baselineY = toY(pricePoints.first())
        canvas.drawLine(0f, baselineY, w, baselineY, baselinePaint)

        // ── 5. Punct final (pretul curent) ───────────────────────────────────
        val lastX = toX(pricePoints.size - 1)
        val lastY = toY(pricePoints.last())

        dotPaint.color = Color.WHITE
        canvas.drawCircle(lastX, lastY, 7f, dotPaint)

        dotPaint.color = lineColor
        canvas.drawCircle(lastX, lastY, 5f, dotPaint)
    }
}
