package com.mc.group28.brainnet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by bhargav on 11/27/17.
 */

public class SettingsActivity extends AppCompatActivity {

    EditText etLocalIp;
    Button btnGetBw;
    TextView tvRemoteBw;
    TextView tvFogBw;
    EditText etRemoteComp;
    EditText etFogComp;
    Button btnSave;

    String REMOTE_BASE_URL = "http://brainet.herokuapp.com/brainet/api/";

    Retrofit retrofit;

    Api_interface api;

    double fogbw = 0;
    double remotebw = 0;

    double fileSize = 100.0;
    private String FOG_BASE_URL = "";

    private  String BASE_URL = "";


    boolean fogServer = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        bindViews();

    }

    private void initViews() {

        etLocalIp = (EditText) findViewById(R.id.et_localip);
        btnGetBw = (Button) findViewById(R.id.btn_get_bw);
        btnSave = (Button) findViewById(R.id.btn_save);

        tvRemoteBw = (TextView) findViewById(R.id.tv_remote_bw);
        tvFogBw = (TextView) findViewById(R.id.tv_fog_bw);

        etFogComp = (EditText) findViewById(R.id.et_fog_comp);
        etRemoteComp = (EditText) findViewById(R.id.et_remote_comp);


//        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//
//        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
//        httpClient.addInterceptor(logging);
//
//        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).client(httpClient.build()).addConverterFactory(GsonConverterFactory.create()).build();
//
//        api = retrofit.create(Api_interface.class);

        setupRetrofitInstance(REMOTE_BASE_URL);


    }


    public  void setupRetrofitInstance(String newApiBaseUrl) {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);


        String BASE_URL = newApiBaseUrl;

        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).client(httpClient.build()).addConverterFactory(GsonConverterFactory.create()).build();

        api = retrofit.create(Api_interface.class);
    }





    public interface Api_interface{

        @GET("bandwidth")
        Call<ResponseBody> getFile ();

    }




    private void bindViews() {

        btnGetBw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(etLocalIp.getText().toString().trim().equals(""))
                    FOG_BASE_URL = REMOTE_BASE_URL;
                else
                    FOG_BASE_URL = etLocalIp.getText().toString();

                Call<ResponseBody> call = api.getFile();

                call.enqueue(new Callback<ResponseBody>() {

                    long startTime = System.currentTimeMillis();


                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        boolean writtenToDisk = writeResponseBodyToDisk(response.body());
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        remotebw = (fileSize / elapsedTime)*1000;
                        tvRemoteBw.setText("Remote Server BW: "+String.valueOf(remotebw)+"KB/S");

                        Toast.makeText(SettingsActivity.this, "Remote BW Updated", Toast.LENGTH_SHORT).show();

                    }


                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });


                setupRetrofitInstance(FOG_BASE_URL);

                Call<ResponseBody> fogCall = api.getFile();

                final long fogStartTime = System.currentTimeMillis();

                fogCall.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        boolean writtenToDisk = writeResponseBodyToDisk(response.body());
                        long elapsedTime = System.currentTimeMillis() - fogStartTime;
                        fogbw = (fileSize / elapsedTime)*1000;
                        tvFogBw.setText("Fog Server BW: "+String.valueOf(fogbw)+"KB/S");

                        Toast.makeText(SettingsActivity.this, "Remote BW Updated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });


            }


        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseServer();

                Intent in = new Intent(SettingsActivity.this,MainActivity.class);
                in.putExtra("Url",BASE_URL);
                startActivity(in);

            }
        });



    }


    public void chooseServer(){

            double remoteTime = Integer.parseInt(etRemoteComp.getText().toString()) + (1.3*1024)/remotebw;
            double fogTime = Integer.parseInt(etFogComp.getText().toString()) + (1.3*1024)/fogbw;

            if(remoteTime > fogTime) {
                BASE_URL = FOG_BASE_URL;
                Toast.makeText(this, "Choosing Fog Server", Toast.LENGTH_SHORT).show();

            } else{

                BASE_URL = REMOTE_BASE_URL;
                Toast.makeText(this, "Choosing Remote Server", Toast.LENGTH_SHORT).show();
            }

    }


    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(Environment.getExternalStorageDirectory()+ "BWSampleFile.txt");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d("File downloaded", "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }


}
