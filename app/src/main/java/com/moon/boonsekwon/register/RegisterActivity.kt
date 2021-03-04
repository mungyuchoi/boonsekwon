package com.moon.boonsekwon.register

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.moon.boonsekwon.GpsTracker
import com.moon.boonsekwon.R
import com.moon.boonsekwon.databinding.ActivityRegisterBinding
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var gpsTracker: GpsTracker
    private lateinit var persistentBottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        supportActionBar?.run {
            title = "등록"
            setDisplayHomeAsUpEnabled(true)
        }
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).let {
            it.getMapAsync(this)
        }

        gpsTracker = GpsTracker(this)
        initView()
        initPersistentBottomSheetBehavior()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        binding.registerPersistent.thumbnail.setOnClickListener {
            Toast.makeText(this, "준비중입니다!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPersistentBottomSheetBehavior() {
        persistentBottomSheetBehavior = BottomSheetBehavior.from(register_persistent)
        persistentBottomSheetBehavior.run {
            state = BottomSheetBehavior.STATE_EXPANDED
            setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(p0: View, state: Int) {
                    when (state) {
                        BottomSheetBehavior.STATE_EXPANDED -> {

                        }
                    }
                }

                override fun onSlide(p0: View, p1: Float) {
                }
            })
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(gpsTracker.latitude, gpsTracker.longitude),
                15f
            )
        )
    }
}