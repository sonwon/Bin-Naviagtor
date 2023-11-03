package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class BitmapConverter {

    //String을 BitMap으로 변환
    public static Bitmap StringtoBitmap(String encodedString){
        try{
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                byte[] encodedByte = Base64.getDecoder().decode(encodedString);
                Bitmap bitmap = BitmapFactory.decodeByteArray(encodedByte, 0, encodedByte.length);
                return bitmap;
            }
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        return null;
    }

    //Bitmap을 String으로 변환
    public static String BitmapToString(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] bytes = baos.toByteArray();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String temp = Base64.getEncoder().encodeToString(bytes);
            return temp;
        }
        return null;
    }

    public static byte[] BitmapToByteArray(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }
    public static Bitmap ByteArrayToBitmap(byte[] byteArray){
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return bitmap;
    }

}
