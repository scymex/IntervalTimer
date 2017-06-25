package verseczi.intervaltimer

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.MenuItem
import android.support.v4.app.NavUtils
import android.widget.FrameLayout
import android.widget.RelativeLayout
import verseczi.intervaltimer.data.Database

class GetCoordinates : AppCompatActivity() {
    private val UI_ANIMATION_DELAY: Long = 300
    private val mHideHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_coordinates)

        val db = Database(this)
        val actionBar = supportActionBar
        var mContentView = findViewById(R.id.fullscreen_content)
        var mHidePart2Runnable = Runnable { mContentView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION }

        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.hide()

        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY)

        changeCoords(db.coordinateX, db.coordinateY)

        mContentView.setOnTouchListener { v, event ->
            changeCoords(Math.round(event.x), Math.round(event.y))

            if (event.action == MotionEvent.ACTION_UP) {
                val returnIntent = Intent()
                returnIntent.putExtra("coordx", Math.round(event.x))
                returnIntent.putExtra("coordy", Math.round(event.y))
                setResult(1, returnIntent)
                finish()
            }
            true
        }
    }

    /**
     * Change the position of the lines (margins)
     */
    fun changeCoords(coordx: Int, coordy: Int) {
        val coordliney = findViewById(R.id.coordliney) as RelativeLayout
        val head_params = coordliney.layoutParams as FrameLayout.LayoutParams
        head_params.setMargins(coordx, 0, 0, 0) //substitute parameters for left, top, right, bottom
        coordliney.layoutParams = head_params

        val coordlinex = findViewById(R.id.coordlinex) as RelativeLayout
        val head_params2 = coordlinex.layoutParams as FrameLayout.LayoutParams
        head_params2.setMargins(0, coordy, 0, 0) //substitute parameters for left, top, right, bottom
        coordlinex.layoutParams = head_params2
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
