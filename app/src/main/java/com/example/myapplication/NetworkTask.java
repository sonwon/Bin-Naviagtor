package com.example.myapplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkTask  {
    private String _result;
    private String _url;
    private String _key;

    public NetworkTask(String url, String key){
        this._url = url;
        this._key = key;
    }

    public void Post(){
        try{
            URL url = new URL(this._url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection != null){
                connection.setConnectTimeout(10000);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type","application/json");
                connection.setDoInput(true);
                connection.setDoOutput(true);

                if(this._key != "") {
                    OutputStream os = connection.getOutputStream();
                    os.write(this._key.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                }

                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    this._result = null;
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                String page = "";
                while((line = reader.readLine()) != null){
                    page += line;
                }
                reader.close();
                connection.disconnect();
                this._result = page;

            }
        }
        catch(IOException e){
            this._result = null;
            e.printStackTrace();
        }
    }

    public void Get(){
        try{
            URL url = new URL(this._url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection != null){
                connection.setConnectTimeout(10000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type","application/json");
                connection.setDoInput(true);

                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    this._result = null;
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                String page = "";
                while((line = reader.readLine()) != null){
                    page += line;
                }
                reader.close();
                connection.disconnect();
                this._result = page;

            }
        }
        catch(IOException e){
            this._result = null;
            e.printStackTrace();
        }
    }
    public void Delete(){
        try{
            URL url = new URL(this._url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection != null){
                connection.setConnectTimeout(10000);
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Content-Type","application/json");
                connection.setDoInput(true);
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(this._key.getBytes("UTF-8"));
                os.flush();
                os.close();

                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    this._result = null;
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                String page = "";
                while((line = reader.readLine()) != null){
                    page += line;
                }
                reader.close();
                connection.disconnect();
                this._result = page;

            }
        }
        catch(IOException e){
            this._result = null;
            e.printStackTrace();
        }
    }

    public void Put(){
        try{
            URL url = new URL(this._url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if(connection != null){
                connection.setConnectTimeout(10000);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type","application/json");
                connection.setDoOutput(true);
                connection.setDoInput(true);


                OutputStream os = connection.getOutputStream();
                os.write(this._key.getBytes("UTF-8"));
                os.flush();
                os.close();

                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    this._result = null;
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                String page = "";
                while((line = reader.readLine()) != null){
                    page += line;
                }
                reader.close();
                connection.disconnect();
                this._result = page;

            }
        }
        catch(IOException e){
            this._result = null;
            e.printStackTrace();
        }
    }

    public String getResult(){
        return this._result;
    }
}

