package com.example.myapplication;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
    int markerCount = 0;

    private Bitmap trashcan_black;
    private Bitmap trashcan_red;


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
                        currentMarker.setTag("location");
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
        //검은색 쓰레기통 이미지 생성
        trashcan_black = imageResize(BitmapFactory.decodeResource(getResources(), R.drawable.trashcan_black));
        trashcan_red = imageResize(BitmapFactory.decodeResource(getResources(), R.drawable.trashcan_red));
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
                // 마커의 이미지 변경
                mOptions.icon(BitmapDescriptorFactory.fromBitmap(trashcan_black));
                // 마커 추가
                if(addMark) { //등록 버튼을 누르면 addMark가 true가 되어서 등록 가능해진다.
                    AlertDialog.Builder dlg = new AlertDialog.Builder(MapsActivity.this);
                    dlg.setTitle("확인 메시지");
                    dlg.setMessage("추가 하시겠습니까?");
                    dlg.setPositiveButton("확인", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which){
                            binMarkers[markerCount] = mMap.addMarker(mOptions);
                            binMarkers[markerCount].setTag("binMarker");
                            markerCount++;
                        }
                    });
                    dlg.setNegativeButton("취소", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which){
                        }
                    });
                    dlg.show();
                    addMark = false;
                }
            }
        });

        //마커 클릭시 정보표시
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //Toast.makeText(MapsActivity.this, "marker click", Toast.LENGTH_SHORT).show(); //debug용
                String str = (String) marker.getTag();
                switch(str) {
                    case "location":
                        break;
                    case "binMarker":
                    case "fullBin":
                        markerClick(marker);
                        break;
                }
                return false;
            }
        });

        // 마커를 추가하는 곳 공대 5호관 옆에 추가 36.3663, 127.3451
        LatLng bin1 = new LatLng(36.3663, 127.3451);
        LatLng bin2 = new LatLng(36.3661, 127.3455);
        binMarkers[0] = mMap.addMarker(new MarkerOptions().position(bin1).title("Marker1 in 공5").icon(BitmapDescriptorFactory.fromBitmap(trashcan_black)));
        binMarkers[0].setTag("binMarker");
        markerCount++;
        binMarkers[1] = mMap.addMarker(new MarkerOptions().position(bin2).title("Marker2 in 공5").icon(BitmapDescriptorFactory.fromBitmap(trashcan_black)));
        binMarkers[1].setTag("binMarker");
        markerCount++;
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

    public Bitmap imageResize(Bitmap originalBitmap) { //이미지 크기 resize를 위한 코드
        int width = originalBitmap.getWidth(); // 현재 이미지의 너비
        int height = originalBitmap.getHeight(); // 현재 이미지의 높이

        int newWidth = 80; // 원하는 마커 이미지의 너비
        int newHeight = 50; // 원하는 마커 이미지의 높이

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }


    //버튼 입력에 대한 반응 등록
    public void button1Activity(View view) {
        //버튼 1 입력시 반응
        Toast toast = Toast.makeText(this,"핀을 등록할 곳을 누르시오",Toast.LENGTH_LONG);
        toast.setGravity(Gravity.LEFT , 0, 300);
        toast.show();

        addMark = true;
    }

    public void markerClick(Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.marker_layout, null);
        builder.setView(dialogView);

        Button cancel = dialogView.findViewById(R.id.cancel);
        Button nav = dialogView.findViewById(R.id.nav);
        Button full = dialogView.findViewById(R.id.full);
        Button delete = dialogView.findViewById(R.id.delete);

        TextView titleTextView = dialogView.findViewById(R.id.text2);
        String markerTitle = marker.getTitle();
        titleTextView.setText(markerTitle);

        full.setText(marker.getTag().equals("binMarker") ? "가득참 신고" : "비움 신고");

        AlertDialog dialog = builder.create();
        dialog.show();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 취소 클릭 이벤트 처리
                dialog.dismiss();
            }
        });

        nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 길찾기 클릭 이벤트 처리
                Toast.makeText(MapsActivity.this, "길찾기", Toast.LENGTH_SHORT).show();
            }
        });

        full.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 가득참 신고 클릭 이벤트 처리
                if(marker.getTag().equals("binMarker")) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(trashcan_red));
                    marker.setTag("fullBin");
                }
                else if(marker.getTag().equals("fullBin")) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(trashcan_black));
                    marker.setTag("binMarker");
                }
                dialog.dismiss();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 삭제 클릭 이벤트 처리
                AlertDialog.Builder dlg = new AlertDialog.Builder(MapsActivity.this);
                dlg.setTitle("확인 메시지");
                dlg.setMessage("삭제 하시겠습니까?");
                dlg.setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        marker.remove();
                    }
                });
                dlg.setNegativeButton("취소", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){

                    }
                });
                dlg.show();
                dialog.dismiss();
            }
        });
    }
}