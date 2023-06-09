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
import android.view.Gravity;
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

    private Marker[] binMarkers = new Marker[99999];

    private boolean addMark = false;
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

        //클릭시 핀 추가
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng point){
                MarkerOptions mOptions = new MarkerOptions();
                //마커 타이틀
                mOptions.title("마커 좌표");
                Double latitude = point.latitude; //위도
                Double longtitude = point.longitude; //경도
                // 마커의 스니펫(간단 텍스트) 설정
                mOptions.snippet(latitude.toString() + ", " + longtitude.toString());
                // LatLng : 위도 경도 쌍 나타내기
                mOptions.position(new LatLng(latitude, longtitude));
                // 마커 추가
                if(addMark) { //등록 버튼을 누르면 addMark가 true가 되어서 등록 가능해진다.
                    googleMap.addMarker(mOptions);
                    addMark = false;
                }
            }
        });

        // 마커를 추가하는 곳 공대 5호관 옆에 추가 36.3663, 127.3451
        LatLng bin1 = new LatLng(36.3663, 127.3451);
        LatLng bin2 = new LatLng(36.3661, 127.3455);
        binMarkers[0] = mMap.addMarker(new MarkerOptions().position(bin1).title("Marker1 in 공5"));
        binMarkers[1] = mMap.addMarker(new MarkerOptions().position(bin2).title("Marker2 in 공5"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(bin)); 카메라 이동


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


    //버튼 입력에 대한 반응 등록
    public void button1Activity(View view) {
        //버튼 1 입력시 반응
        Toast toast = Toast.makeText(this,"핀을 등록할 곳을 누르시오",Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP , 0, 300);
        toast.show();

        addMark = true;
//        mMap = googleMap;
//
//        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
//            @Override
//            public void onMapClick(LatLng point){
//                MarkerOptions mOptions = new MarkerOptions();
//                //마커 타이틀
//                mOptions.title("마커 좌표");
//                Double latitude = point.latitude; //위도
//                Double longtitude = point.longitude; //경도
//                // 마커의 스니펫(간단 텍스트) 설정
//                mOptions.snippet(latitude.toString() + ", " + longtitude.toString());
//                // LatLng : 위도 경도 쌍 나타내기
//                mOptions.position(new LatLng(latitude, longtitude));
//                // 마커 추가
//                googleMap.addMarker(mOptions);
//            }
//        });

    }

    public void button2Activity(View view) {
        //버튼 2 입력시 반응 삭제
        Toast toast = Toast.makeText(this,"삭제할 핀을 누르시오",9999);
        toast.setGravity(Gravity.TOP, 0, 300);
        toast.show();

        //binMarker 삭제
        binMarkers[1].remove();

    }
}