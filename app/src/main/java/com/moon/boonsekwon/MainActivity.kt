package com.moon.boonsekwon

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Toast
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.kongzue.dialog.v2.InputDialog
import com.moon.boonsekwon.const.Const
import com.moon.boonsekwon.data.Location
import com.moon.boonsekwon.data.User
import com.moon.boonsekwon.databinding.ActivityMainBinding
import com.moon.boonsekwon.register.RegisterActivity
import kotlinx.android.synthetic.main.bottom_sheet_persistent.*

import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var gpsTracker: GpsTracker
    private lateinit var persistentBottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var binding: ActivityMainBinding

    private var me: User? = null
    private val GPS_ENABLE_REQUEST_CODE = 2001
    private val PERMISSIONS_REQUEST_CODE = 100
    var REQUIRED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private lateinit var auth: FirebaseAuth
    private var exitDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).let {
            it.getMapAsync(this)
        }

        initView()
        initPersistentBottomSheetBehavior()
        initFirebase()

        MobileAds.initialize(this)
        binding.bottomSheetPersistent.adView.loadAd(AdRequest.Builder().build())
        exitDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.exit_dialog)
        }
        exitDialog?.findViewById<Button>(R.id.review)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        }
        exitDialog?.findViewById<Button>(R.id.exit)?.setOnClickListener {
            finish()
        }
        //Test
//        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
        val adLoader = AdLoader.Builder(this, "ca-app-pub-8549606613390169/3199270954")
            .forUnifiedNativeAd { ad: UnifiedNativeAd ->
                exitDialog?.findViewById<TemplateView>(R.id.template)?.setNativeAd(ad)
            }
            .withAdListener(object : AdListener() {
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(AdRequest.Builder().build())

        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onBackPressed() {
        exitDialog?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.register -> {
                Log.i(
                    TAG,
                    "center position latitude:${map.cameraPosition.target.latitude}, longitude:${map.cameraPosition.target.latitude}"
                )
                val intent = Intent(this, RegisterActivity::class.java).apply {
                    putExtra(Const.CENTER_LATITUDE, map.cameraPosition.target.latitude)
                    putExtra(Const.CENTER_LONGITUDE, map.cameraPosition.target.longitude)
                }
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUI(user: FirebaseUser?) {
        Log.i(TAG, "updateUI user:$user")
        user?.run {
            loadUserInfo()
            loadLocationInfo()
        }
    }

    fun loadLocationInfo() {
        FirebaseDatabase.getInstance().reference.child("Location").child("KR").apply {
            addValueEventListener(loadLocationListener)
        }
    }

    val loadLocationListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.i("MQ!", "loadLocationListener onDataChange")
            for (location in snapshot.children) {
                val info = location.getValue(Location::class.java)
                map.addMarker(MarkerOptions().apply {
                    position(LatLng(info!!.latitude, info!!.longitude))
                    title(info!!.title)
                    snippet(info!!.description)
                })
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.i("MQ!", "loadLocationListener onCancelled: $error")
        }
    }

    fun loadUserInfo() {
        val pref = application.getSharedPreferences("BOONSEKWON", Context.MODE_PRIVATE)
        val keyMe = pref.getString("key", "")
        if (keyMe == "" || keyMe!!.isEmpty()) {
            return
        }
        FirebaseDatabase.getInstance().reference.child("users").child(keyMe).apply {
            Log.i("MQ!", "addValueEventListener: $loadUserListener")
            addValueEventListener(loadUserListener)
        }
    }

    val loadUserListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val key = snapshot.key
            Log.i("MQ!", "onDataChange user key:$key")
            val pref = application.getSharedPreferences("BOONSEKWON", Context.MODE_PRIVATE)
            val keyMe = pref.getString("key", "")
            if (keyMe == "" || keyMe!!.isEmpty()) {
                return
            }
            if (key == keyMe) {
                val user = snapshot.getValue(User::class.java)
                me = user
                supportActionBar?.title = "붕세권 Point: ${user?.point}"
                return
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.i("MQ!", "loadUserListener onCancelled: $error")
        }
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(application, gso)
        auth = FirebaseAuth.getInstance().apply {
            if (currentUser == null) {
                startActivityForResult(client.signInIntent, RC_SIGN_IN)
            } else {
                FirebaseAnalytics.getInstance(this@MainActivity).logEvent("initFirebase", Bundle().apply {
                    putString("currentUser", "null")
                })
            }
        }
    }

    private fun initView() {
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting()
        } else {
            checkRunTimePermission()
        }

        gpsTracker = GpsTracker(this@MainActivity)

        findViewById<Button>(R.id.my_location).run {
            setOnClickListener {
                gpsTracker.run {
                    val address = getCurrentAddress(latitude, longitude)
                    Toast.makeText(
                        this@MainActivity,
                        "현재위치 \n위도 $latitude\n경도 $longitude\n address: $address",
                        Toast.LENGTH_LONG
                    ).show()
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            15f
                        )
                    )
                }
            }
        }

        binding.bottomSheetPersistent.thumbnail.setOnClickListener {
            Toast.makeText(this, "준비중입니다!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPersistentBottomSheetBehavior() {
        persistentBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_persistent)
        persistentBottomSheetBehavior.run {
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

//        map.addMarker(MarkerOptions().apply {
//            position(LatLng(37.32487, 127.10762))
//            title("죽전역 앞 포장마차 잉어빵")
//            snippet(
//                "잉어빵2개 천원 5개 2천원이다.\n" +
//                        "시간 잘 맞춰서 가면 뜨겁고 바삭바삭한 잉어빵을 먹을 수 있다. 오뎅도 판다.\n" +
//                        "밤 11시에도 열려있었다. 아침에 여는 시간은 잘 모름."
//            )
//        })

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(gpsTracker.latitude, gpsTracker.longitude),
                15f
            )
        )

        map.setOnMarkerClickListener(this)

        map.setOnMapClickListener {
            persistentBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onRequestPermissionsResult(
        permsRequestCode: Int,
        permissions: Array<String?>,
        grandResults: IntArray
    ) {
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.size == REQUIRED_PERMISSIONS.size) {
            var check_result = true
            for (result in grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }
            if (check_result) {

                //위치 값을 가져올 수 있음
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[0]
                    )
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[1]
                    )
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun checkRunTimePermission() {

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음
        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    REQUIRED_PERMISSIONS[0]
                )
            ) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(this@MainActivity, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG)
                    .show()
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this@MainActivity, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                    this@MainActivity, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }


    fun getCurrentAddress(latitude: Double, longitude: Double): String? {

        //지오코더... GPS를 주소로 변환
        val geocoder = Geocoder(this, Locale.getDefault())
        var addresses: MutableList<Address>?
        try {
            addresses = geocoder.getFromLocation(
                latitude,
                longitude,
                7
            )
        } catch (ioException: IOException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show()
            return "지오코더 서비스 사용불가"
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show()
            return "잘못된 GPS 좌표"
        }
        if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show()
            return "주소 미발견"
        }
        val address: Address = addresses[0]
        return address.getAddressLine(0).toString().toString() + "\n"
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private fun showDialogForLocationServiceSetting() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage(
            """
            앱을 사용하기 위해서는 위치 서비스가 필요합니다.
            위치 설정을 수정하실래요?
            """.trimIndent()
        )
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        })
        builder.setNegativeButton("취소",
            DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        builder.create().show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GPS_ENABLE_REQUEST_CODE ->
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음")
                        checkRunTimePermission()
                        return
                    }
                }
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                FirebaseAnalytics.getInstance(this@MainActivity).logEvent("RC_SIGN_IN", Bundle().apply {
                    putString("task", task.toString())
                })
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)
                } catch (e: ApiException) {
                    FirebaseAnalytics.getInstance(this@MainActivity).logEvent("RC_SIGN_IN", Bundle().apply {
                        putString("sign in", "failed")
                    })
                    Log.w(TAG, "Google sign in failed", e)
                    finish()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        FirebaseAnalytics.getInstance(this@MainActivity).logEvent("AUTH_WITH_GOOGLE", Bundle().apply {
            putString("acctid", acct.id!!)
        })
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth?.signInWithCredential(credential)!!
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val pref =
                        applicationContext.getSharedPreferences("BOONSEKWON", Context.MODE_PRIVATE)
                    Log.d(TAG, "name: " + pref.getString("name", "unknown"))
                    if (pref.getString("name", "unknown") == "unknown") {
                        InputDialog.build(
                            this@MainActivity,
                            "별명을 입력해주세요.", "사용할 별명을 입력해주세요", "완료",
                            { dialog, inputText ->
                                dialog.dismiss()
                                val pref = applicationContext.getSharedPreferences(
                                    "BOONSEKWON",
                                    MODE_PRIVATE
                                )
                                val editor = pref.edit()
                                editor.putString("name", auth?.currentUser?.displayName)
                                editor.putString(
                                    "image",
                                    auth?.currentUser?.photoUrl.toString()
                                )
                                var usersRef =
                                    FirebaseDatabase.getInstance().reference.child("users")
                                        .push()
                                editor.putString("key", usersRef.key)
                                editor.commit()
                                var name = inputText
                                if (name == null || name.isEmpty()) {
                                    name = auth?.currentUser?.displayName
                                }
                                Log.d(TAG, "name: $name")
                                usersRef.setValue(
                                    User(
                                        name = name,
                                        imageUrl = auth?.currentUser?.photoUrl.toString(),
                                        point = 0
                                    )
                                )
                                finishAffinity()
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                            }, "취소", { dialog, _ ->
                                dialog.dismiss()
                                finish()
                            }).apply {
                            setDialogStyle(1)
                            setDefaultInputHint(auth?.currentUser?.displayName)
                            showDialog()
                        }
                    }

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    finish()
                }
            }
    }

    fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    companion object {
        const val TAG = "BOONG"
        val RC_SIGN_IN = 9001
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        binding.bottomSheetPersistent.run {
            Log.i("MQ!", "marker Click: ${marker?.title}")
            title.text = marker?.title
            description.text = marker?.snippet
        }
        persistentBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        return false
    }
}