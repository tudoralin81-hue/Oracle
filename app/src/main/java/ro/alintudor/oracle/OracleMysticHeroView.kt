package ro.alintudor.oracle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View

/** Compatibility shim; B514 Start is implemented by OracleMysticStartView. */
class OracleMysticHeroView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val start = OracleMysticStartView(context, onModule)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(2, 3, 5))
        start.layout(0, 0, width, height)
        start.draw(canvas)
    }
}
