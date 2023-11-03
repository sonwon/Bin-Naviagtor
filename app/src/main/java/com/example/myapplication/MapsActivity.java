package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.databinding.LoginBinding;
import com.example.myapplication.databinding.MarkerLayoutBinding;
import com.example.myapplication.databinding.RegisterBinding;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LoginBinding loginBinding;
    private RegisterBinding registerBinding;

    private  MarkerLayoutBinding markerLayoutBinding;
    private static final int PERMISSION_REQUEST_CODE = 1; //위치 권한에 대한 permission code

    private static final int TAKE_PICTURE = 1; //사진 권한에 대한 permission
    private String mCurrentPhotoPath;
    private static final int REQUEST_TAKE_PHOTO = 1;

    private Bitmap addImageBitmap;
    private String ImageBitmapString;
    private ImageView addImageView;

    private boolean addImageBool;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private MarkerOptions markerOptions;
    private Marker currentMarker;

    private Bitmap trashcan_black;
    private Bitmap trashcan_red;

    public Long userId;

    private boolean addMark = false;

    BitmapConverter bitmapConverter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        loginBinding = LoginBinding.inflate(getLayoutInflater());
        registerBinding = RegisterBinding.inflate(getLayoutInflater());
        markerLayoutBinding = MarkerLayoutBinding.inflate(getLayoutInflater());

        setContentView(loginBinding.getRoot());

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
        //카메라 권한 요청
        if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            Log.d("Permission Log", "권한 설정 완료");
        }
        else{
            Log.d("Permission Log", "권한 설정 요청");
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
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

        //로그인 버튼
        LoginButtonClickListener loginButtonClickcListener = new LoginButtonClickListener();
        loginBinding.loginButton.setOnClickListener(loginButtonClickcListener);
        //회원가입 버튼
        RegisterButtonClickListener registerButtonClickListener = new RegisterButtonClickListener();
        loginBinding.registerButton.setOnClickListener(registerButtonClickListener);
        //회원가입 submit 버튼
        RegisterSubmitClickListener registerSubmitClickListener = new RegisterSubmitClickListener();
        registerBinding.registerSubmitButton.setOnClickListener(registerSubmitClickListener);
        //쓰레기통 추가 버튼
        AddBinButtonClickListener addBinButtonClickListener = new AddBinButtonClickListener();
        binding.addBinButton.setOnClickListener(addBinButtonClickListener);

        //분리수거 설명 버튼
        RecycleExplainationButtonClickListener recycleExplainationButtonClickListener = new RecycleExplainationButtonClickListener();
        binding.recycleExlaination.setOnClickListener(recycleExplainationButtonClickListener);

        bitmapConverter = new BitmapConverter();

    }

    public class LoginButtonClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            String getId = "";
            String getPassword = "";
            getId = loginBinding.textId.getText().toString();
            getPassword = loginBinding.textPassword.getText().toString();

            if((getId.length() == 0) || (getPassword.length() == 0)){
                Toast.makeText(MapsActivity.this, "아이디와 패스워드를 입력칸을 채워주세요", Toast.LENGTH_SHORT).show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return;
            }

            String url = "http://34.64.137.18:8080/user/login";

            //json 객체 만들기
            String value = "";
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.accumulate("username", getId);
                jsonObject.accumulate("password", getPassword);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            value = jsonObject.toString();

            NetworkTask networkTask = new NetworkTask(url, value);
            Thread tread = new Thread() {
                @Override
                public void run() {
                    networkTask.Post();
                }
            };
            tread.start();
            try{
                tread.join();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            String result = networkTask.getResult();
            if(result == null){
                Toast.makeText(MapsActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return;
            }

            try{
                JSONObject jsonResult = new JSONObject(result);
                userId = jsonResult.getLong("userId");
            }
            catch (JSONException e){
                e.printStackTrace();
            }

            setContentView(binding.getRoot());
            SetBinIcons();
        }
    }

    public class RegisterButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            setContentView(registerBinding.getRoot());
        }
    }

    public class RegisterSubmitClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            String getId;
            String getPassword;
            getId = registerBinding.registerIdText.getText().toString();
            getPassword = registerBinding.registerPasswordText.getText().toString();

            if(getId.length() == 0 || getPassword.length() == 0){
                Toast.makeText(MapsActivity.this, "아이디와 패스워드를 입력칸을 채워주세요", Toast.LENGTH_SHORT).show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return;
            }

            String url = "http://34.64.137.18:8080/user/add";

            //json 객체 만들기
            String value = "";
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.accumulate("username", getId);
                jsonObject.accumulate("password", getPassword);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            value = jsonObject.toString();

            NetworkTask networkTask = new NetworkTask(url, value);

            Thread tread = new Thread() {
                @Override
                public void run() {
                    networkTask.Post();
                }
            };
            tread.start();
            try{
                tread.join();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            String result = networkTask.getResult();

            if(result == null){
                Toast.makeText(MapsActivity.this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return;
            }

            setContentView(loginBinding.getRoot());
        }
    }

    //Network를 통해 아이콘 받아와서 맵에 출력
    public void SetBinIcons(){
        String url = "http://34.64.137.18:8080/bin/get-all";

        NetworkTask networkTask = new NetworkTask(url, "");
        Thread tread = new Thread() {
            @Override
            public void run() {
                networkTask.Get();
            }
        };
        tread.start();
        try{
            tread.join();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }

        String result = networkTask.getResult();
        try{
            JSONArray jsonArray = new JSONArray(result);
            for(int i=0; i<jsonArray.length(); i++){

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Long getBinId = jsonObject.getLong("binId");
                float getBinLatitude = BigDecimal.valueOf(jsonObject.getDouble("latitude")).floatValue();
                float getBinLongitude = BigDecimal.valueOf(jsonObject.getDouble("longitude")).floatValue();
                boolean getFull = jsonObject.getBoolean("full");
                markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(getBinLatitude, getBinLongitude));
                if(getFull){
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(trashcan_red));
                }
                else{
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(trashcan_black));
                }

                Marker binMarker = mMap.addMarker(markerOptions);
                binMarker.setTag(getBinId);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }

    }

    public class AddBinButtonClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            binding.stateText.setText("쓰레기통을 등록할 위치를 클릭해주세요.");
            binding.stateText.setVisibility(View.VISIBLE);
            addMark = true;
        }
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
        trashcan_black = imageResize(BitmapFactory.decodeResource(getResources(), R.drawable.trashcan_black));
        trashcan_red  = imageResize(BitmapFactory.decodeResource(getResources(), R.drawable.trashcan_red));

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
                if(!addMark){
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                LayoutInflater inflater = getLayoutInflater();

                View dialogView = inflater.inflate(R.layout.marker_add_layout, null);
                builder.setView(dialogView);

                Button addButton = dialogView.findViewById(R.id.add_submit);
                Button cancelButton = dialogView.findViewById(R.id.add_cancel);
                TextView addText = dialogView.findViewById(R.id.add_text);
                addImageView = dialogView.findViewById(R.id.add_image);
                addImageBool = false;
                
                AlertDialog dialog = builder.create();
                dialog.show();
                
                addImageView.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, TAKE_PICTURE);
                    }
                });
                addButton.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        String textValue = addText.getText().toString();
                        if(textValue.length() == 0){
                            Toast.makeText(MapsActivity.this, "쓰레기통 위치 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(!addImageBool){
                            Toast.makeText(MapsActivity.this, "쓰레기통 사진을 찍어주세요", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        MarkerOptions mOptions = new MarkerOptions();

                        Double latitude = point.latitude; //위도
                        Double longtitude = point.longitude; //경도

                        String imageString = bitmapConverter.BitmapToString(addImageBitmap);

                        mOptions.snippet(latitude.toString() + ", " + longtitude.toString());

                        // LatLng : 위도 경도 쌍 나타내기
                        mOptions.position(new LatLng(latitude, longtitude));
                        // 마커의 이미지 변경
                        mOptions.icon(BitmapDescriptorFactory.fromBitmap(trashcan_black));
                        //서버에 전송
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.accumulate("latitude", latitude);
                            jsonObject.accumulate("longitude", longtitude);
                            jsonObject.accumulate("userId", userId);
                            jsonObject.accumulate("information", textValue);
                            jsonObject.accumulate("image", imageString);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        String url = "http://34.64.137.18:8080/bin/add";
                        NetworkTask networkTask = new NetworkTask(url, jsonObject.toString());
                        Thread tread = new Thread() {
                            @Override
                            public void run() {
                                networkTask.Post();
                            }
                        };
                        tread.start();
                        try{
                            tread.join();
                        }
                        catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        //추가된 쓰레기통id 반환
                        String result = networkTask.getResult();

                        if(result == null){
                            return;
                        }
                        Marker binMarker = mMap.addMarker(mOptions);
                        //쓰레기통ID를 태그에 추가
                        binMarker.setTag(Long.parseLong(result));

                        binding.stateText.setVisibility(View.INVISIBLE);
                        addMark = false;
                        dialog.dismiss();

                    }
                });

                cancelButton.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        binding.stateText.setVisibility(View.INVISIBLE);
                        addMark = false;
                        dialog.dismiss();
                    }
                });
            }
        });

        //마커 클릭시 정보표시
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //마커의 아이디 가져오기
                Long str =  (Long) marker.getTag();
                String url = "http://34.64.137.18:8080/bin/get-by-bin?binId="+str;

                NetworkTask networkTask = new NetworkTask(url, "");
                Thread thread = new Thread(){
                    @Override
                    public void run(){
                        networkTask.Get();
                    }
                };
                thread.start();
                try {
                    thread.join();
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
                //binId에 해당하는 마커정보 가져오기
                String result = networkTask.getResult();
                if(result == null){
                    return false;
                }
                JSONObject jsonObject;
                try{
                    jsonObject = new JSONObject(result);
                    //마커 정보 가져오기
                    String information = jsonObject.getString("information");
                    Long binId = jsonObject.getLong("binId");
                    boolean full = jsonObject.getBoolean("full");
                    String imageString = jsonObject.getString("image");
                    markerClick(marker, information, binId, full, imageString);
                }catch (Exception e){
                    e.printStackTrace();
                }
                return false;
            }
        });
    }
    //카메라 권한 요청 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grandtResults){
        super.onRequestPermissionsResult(requestCode, permissions, grandtResults);
        if(grandtResults[0] == PackageManager.PERMISSION_GRANTED && grandtResults[1] == PackageManager.PERMISSION_GRANTED){
            Log.d("Permission Log", "Permission: "+permissions[0]+"was "+grandtResults[0]);
        }
    }
    //카메라로 촬영한 사진을 가져와 addImageBitmap에 저장
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);

        switch(requestCode){
            case TAKE_PICTURE:
                if(resultCode == RESULT_OK && intent.hasExtra("data")){
                    Bitmap bitmap = (Bitmap) intent.getExtras().get("data");
                    if(bitmap != null){
                        addImageBool = true;
                        addImageBitmap = bitmap;
                        addImageView.setImageBitmap(addImageBitmap);
                        ImageBitmapString = bitmapConverter.BitmapToString(addImageBitmap);
                    }
                }
                break;
        }
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

        int newWidth = 100; // 원하는 마커 이미지의 너비
        int newHeight = 80; // 원하는 마커 이미지의 높이

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public class RecycleExplainationButtonClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.recycle_menu, null);
            builder.setView(dialogView);

            ImageButton plasticButton = dialogView.findViewById(R.id.plastic_button);
            ImageButton vinylButton = dialogView.findViewById(R.id.vinyl_button);
            ImageButton aluminumButton = dialogView.findViewById(R.id.aluminum_button);
            ImageButton steelButton = dialogView.findViewById(R.id.steel_button);
            ImageButton glassButton = dialogView.findViewById(R.id.glass_button);
            ImageButton paperButton = dialogView.findViewById(R.id.paper_button);
            Button cancelButton = dialogView.findViewById(R.id.cancelRecycle);

            AlertDialog dialog = builder.create();
            dialog.show();

            RecycleButtonClickListener recycleButtonClickListener = new RecycleButtonClickListener();
            plasticButton.setOnClickListener(recycleButtonClickListener);
            vinylButton.setOnClickListener(recycleButtonClickListener);
            aluminumButton.setOnClickListener(recycleButtonClickListener);
            steelButton.setOnClickListener(recycleButtonClickListener);
            glassButton.setOnClickListener(recycleButtonClickListener);
            paperButton.setOnClickListener(recycleButtonClickListener);

            cancelButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    dialog.dismiss();
                }
            });
        }
    }

    public class RecycleButtonClickListener
            implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.plastic_button:
                    recycleActivity(R.raw.plastic);
                    break;
                case R.id.vinyl_button:
                    recycleActivity(R.raw.vinyl);
                    break;
                case R.id.aluminum_button:
                    recycleActivity(R.raw.aluminum);
                    break;
                case R.id.steel_button:
                    recycleActivity(R.raw.steel);
                    break;
                case R.id.glass_button:
                    recycleActivity(R.raw.glass);
                    break;
                case R.id.paper_button:
                    recycleActivity(R.raw.paper);
                    break;
            }
        }
    }

    public void recycleActivity(int recycleId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.show_tips, null);
        builder.setView(dialogView);

        Button goBack = dialogView.findViewById(R.id.goBack);
        TextView tips = dialogView.findViewById(R.id.tips);

        String recycleText = "";
        try{
            InputStream in = getResources().openRawResource(recycleId);
            byte[] bytes = new byte[in.available()];

            in.read(bytes);
            recycleText = new String(bytes);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        tips.setText(recycleText);
        tips.setMovementMethod(new ScrollingMovementMethod());

        AlertDialog dialog = builder.create();
        dialog.show();

        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }


    public void markerClick(Marker marker, String information, Long binId, boolean full, String imageString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.marker_layout, null);
        builder.setView(dialogView);


        Button cancelButton = dialogView.findViewById(R.id.cancel);
        Button fullButton = dialogView.findViewById(R.id.full);
        Button deleteButton = dialogView.findViewById(R.id.delete);
        TextView textView = dialogView.findViewById(R.id.marker_information);
        textView.setText(information);
        ImageView imageView = dialogView.findViewById(R.id.marker_image);
        Bitmap imageBitmap = bitmapConverter.StringtoBitmap(imageString);
        imageView.setImageBitmap(imageBitmap);

        if(full){//가득 찬 경우
            fullButton.setText("가득 참 신고 해제");
        }
        else{//가득 차지 않은 경우
            fullButton.setText("가득 참 신고");
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 취소 클릭 이벤트 처리
                dialog.dismiss();
            }
        });


        fullButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 가득참 신고 클릭 이벤트 처리
                AlertDialog.Builder dlg = new AlertDialog.Builder(MapsActivity.this);
                dlg.setTitle("확인 메시지");
                dlg.setMessage("신고 하시겠습니까?");
                dlg.setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        String url = "http://34.64.137.18:8080/bin/full?binId="+binId;
                        NetworkTask networkTask = new NetworkTask(url, "");
                        Thread thread = new Thread(){
                            @Override
                            public void run(){
                                networkTask.Put();
                            }
                        };
                        thread.start();
                        try{
                            thread.join();
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        String result = networkTask.getResult();
                        if(result == null){
                            return;
                        }
                        if(full){
                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(trashcan_black));
                        }
                        else{
                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(trashcan_red));
                        }
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

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 삭제 클릭 이벤트 처리
                AlertDialog.Builder dlg = new AlertDialog.Builder(MapsActivity.this);
                dlg.setTitle("확인 메시지");
                dlg.setMessage("삭제 하시겠습니까?");
                dlg.setPositiveButton("확인", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        
                        String url = "http://34.64.137.18:8080/bin/delete";
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.accumulate("binId", binId);
                            jsonObject.accumulate("userId", userId);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        String key = jsonObject.toString();
                        NetworkTask networkTask = new NetworkTask(url, key);
                        Thread thread = new Thread(){
                            @Override
                            public void run(){
                                networkTask.Delete();
                            }
                        };
                        thread.start();
                        try{
                            thread.join();
                        }
                        catch (InterruptedException e){
                            e.printStackTrace();
                            return;
                        }

                        String result = networkTask.getResult();
                        if(result == null){

                            return;
                        }
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