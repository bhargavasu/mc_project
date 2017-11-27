package com.mc.group28.brainnet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    Button btn_auth;
    EditText et_userName;
    Spinner spinner_file;
    ArrayAdapter  adapter;



    String BASE_URL = "http://brainet.herokuapp.com/brainet/api/";

    Retrofit retrofit;

    Api_interface api;
    private String fileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        bindViews();

    }


    private void initViews() {

        btn_auth = (Button) findViewById(R.id.btn_auth);
        et_userName = (EditText) findViewById(R.id.et_username);

        spinner_file = (Spinner) findViewById(R.id.spinner_file);
        //spinner_file.setPrompt("Please select file ");

        adapter = ArrayAdapter.createFromResource(this,R.array.file_names,android.R.layout.simple_spinner_dropdown_item);


        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        retrofit = new Retrofit.Builder().baseUrl(BASE_URL).client(httpClient.build()).addConverterFactory(GsonConverterFactory.create()).build();

        api = retrofit.create(Api_interface.class);

    }


    private void bindViews() {

        spinner_file.setAdapter(adapter);
        btn_auth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileName = spinner_file.getSelectedItem().toString();

                if(et_userName.getText().toString().trim().equals(""))
                    Toast.makeText(MainActivity.this, "Please enter userName to proceed", Toast.LENGTH_SHORT).show();
                else{
                    getReadPermission();

                }


            }
        });


    }

    public void getReadPermission(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else{

            makeAuthCall(fileName,et_userName.getText().toString());

        }



    }


    public interface Api_interface{

        @POST("auth")
        @Multipart
        Call<AuthResponse> authUser (@Part("username") String userName, @Part("eeg_data") RequestBody req);

    }

    private void makeAuthCall(String fileName,String userName){

        MediaType type = MediaType.parse("application/octet-stream");
        File file = new File(Environment.getExternalStorageDirectory()+"/EDFData/"+fileName);
        final RequestBody req = RequestBody.create(type,file);
        Call<AuthResponse> call = api.authUser(userName,req);

       call.enqueue(new Callback<AuthResponse>() {
           @Override
           public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    if(response.body().getAuthenticated())
                        Toast.makeText(MainActivity.this, "Authentication Successful", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
           }

           @Override
           public void onFailure(Call<AuthResponse> call, Throwable t) {
               Toast.makeText(MainActivity.this, "Auth Req failed", Toast.LENGTH_SHORT).show();
           }
       });

    }



}
