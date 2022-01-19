package com.loopytime.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.loopytime.utils.Constants.BASE_URL;

/**
 * Created by hitasoft on 12/3/18.
 */

public class ApiClient {

    private static Retrofit retrofit = null;
    private static Retrofit uploadRetrofit = null;

    public static Retrofit getClient() {
//       HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//        OkHttpClient httpClient = new OkHttpClient.Builder()
//                .callTimeout(60, TimeUnit.MINUTES)
//                .connectTimeout(60, TimeUnit.SECONDS)
//                .addInterceptor(interceptor)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .writeTimeout(60, TimeUnit.SECONDS).build();
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
//                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getUploadClient() {
        if (uploadRetrofit == null) {
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .callTimeout(60, TimeUnit.MINUTES)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS).build();

            uploadRetrofit = new Retrofit.Builder()
                    .client(httpClient)
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return uploadRetrofit;
    }
}
