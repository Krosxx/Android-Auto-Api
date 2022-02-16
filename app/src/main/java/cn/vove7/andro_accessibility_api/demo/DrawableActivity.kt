package cn.vove7.andro_accessibility_api.demo

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import me.panavtec.drawableview.DrawableView
import me.panavtec.drawableview.DrawableViewConfig

/**
 * # DrawableActivity
 *
 * Created on 2020/6/10
 * @author Vove
 */
class DrawableActivity : AppCompatActivity() {

    val button_clear by lazy {
        findViewById<Button>(R.id.button_clear)
    }

    val drawable_view by lazy {
        findViewById<DrawableView>(R.id.drawable_view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawable)
        button_clear.setOnClickListener {
            drawable_view.clear()
        }
        drawable_view.post {
            drawable_view.setConfig(DrawableViewConfig().apply {
                strokeColor = Color.BLACK
                isShowCanvasBounds = true
                strokeWidth = 10.0f
                minZoom = 1.0f
                maxZoom = 3.0f
                canvasHeight = drawable_view.height
                canvasWidth = drawable_view.width
            })
        }
    }

}