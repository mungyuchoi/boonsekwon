package com.moon.boonsekwon.register

import android.os.Bundle
import android.util.Log
import android.view.Menu
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
import com.google.firebase.database.FirebaseDatabase
import com.kongzue.dialog.v2.InputDialog
import com.moon.boonsekwon.GpsTracker
import com.moon.boonsekwon.R
import com.moon.boonsekwon.const.Const
import com.moon.boonsekwon.data.Location
import com.moon.boonsekwon.data.User
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.register, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.name_register -> {
                if (validCheckInfo()) {
                    val pref = applicationContext.getSharedPreferences(
                        "BOONSEKWON",
                        MODE_PRIVATE
                    )
                    val registerRef =
                        FirebaseDatabase.getInstance().reference.child("Location").child("KR")
                            .push()
                    Log.i(TAG, "registerRef:$registerRef")
                    registerRef.setValue(
                        Location(
                            latitude = map.cameraPosition.target.latitude,
                            longitude = map.cameraPosition.target.longitude,
                            title = binding.registerPersistent.title.text.toString(),
                            description = binding.registerPersistent.description.text.toString(),
                            address = null,
                            registerKey = pref.getString("key", null)
                        )
                    )
                } else {
                    Toast.makeText(this, "이름, 내용을 입력해주세요!!", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.location_register -> {
                if (validCheckInfo()) {
                    // TODO EditText두개 넣어서 위도 경도 받고 넣기
                } else {
                    Toast.makeText(this, "이름, 내용을 입력해주세요!!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun validCheckInfo(): Boolean =
        binding.registerPersistent.title.text.isNotEmpty() && binding.registerPersistent.description.text.isNotEmpty()

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
        val latitude = intent.getDoubleExtra(Const.CENTER_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(Const.CENTER_LONGITUDE, 0.0)
        Log.i(TAG, "onMapReady center latitude:$latitude, longitude:$longitude")
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(latitude, longitude),
                15f
            )
        )
    }

    companion object {
        val TAG = "RegisterActivity"
    }
}