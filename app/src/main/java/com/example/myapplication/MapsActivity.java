package com.example.myapplication;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap; //지도 데이터 및 뷰에 대한 액세스 권한 제공
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment; //앱 UI의 상위 요소
import com.google.android.gms.maps.model.BitmapDescriptor; //마커의 모양
import com.google.android.gms.maps.model.BitmapDescriptorFactory; //마커의 모양
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.myapplication.databinding.ActivityMapsBinding;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private static final int PERMISSION_REQUEST_CODE = 1; //위치 권한에 대한 permission code

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private MarkerOptions markerOptions;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        //FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 실시간 update를 위한 LocationRequest 생성 및 설정 수정해야됨
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000) // 위치 업데이트 간격 (1초)
                .setFastestInterval(1000); // 가장 빠른 위치 업데이트 간격 (1초)

        // LocationCallback 초기화 이 코드가 locationRequest에 설정한 업데이트 간격대로 실행
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                markerOptions = new MarkerOptions();
                BitmapDescriptor markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // 위치 업데이트 시마다 호출되는 콜백 메서드
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if(currentMarker == null) {
                        markerOptions.position(currentLocation);
                        markerOptions.icon(markerIcon);
                        currentMarker = mMap.addMarker(markerOptions);
                    }
                    else{
                        // marker의 위치를 자연스럽게 이동하기 위해 Animator 사용
                        ObjectAnimator animator = ObjectAnimator.ofObject(currentMarker, "position", new LatLngEvaluator(), currentMarker.getPosition(), currentLocation);
                        animator.setDuration(1000); // 애니메이션의 지속 시간을 1초 설정
                        animator.start(); // 애니메이션
                        currentMarker.setPosition(currentLocation);
                    }

                }
            }
        };

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    public class LatLngEvaluator implements TypeEvaluator<LatLng> {
        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            double lat = (endValue.latitude - startValue.latitude) * fraction + startValue.latitude;
            double lng = (endValue.longitude - startValue.longitude) * fraction + startValue.longitude;
            return new LatLng(lat, lng);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // 현재 위치를 가져와서 LatLng 객체로 변환
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        LatLng seoul = new LatLng(37, 127);
        float zoom_level = 18.0f;

        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                // 위치 정보 사용
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom_level));
            }
        } catch (SecurityException e) { // 위치 권한이 거부된 경우 현재위치 서울로 설정
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, zoom_level));
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        try {
            Location location = fusedLocationClient.getLastLocation().getResult();
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                currentMarker.setPosition(currentLocation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        currentMarker.remove();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    //버튼 입력에 대한 반응
    public void button1Activity(View view) {
        //버튼 1 입력시 반응
        Toast.makeText(this,"버튼1 입력",Toast.LENGTH_SHORT).show();
    }

    public void button2Activity(View view) {
        //버튼 2 입력시 반응
        Toast.makeText(this,"버튼2 입력",Toast.LENGTH_SHORT).show();
    }
}