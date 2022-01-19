package com.loopytime.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.loopytime.im.ApplicationClass;
import com.loopytime.im.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by jack on 18/10/2016.
 */

public class ExecuteServices {
    OnServiceExecute mOnServiceExecute;

    public ExecuteServices() {

    }

    public interface OnServiceExecute {
        void onServiceExecutedResponse(String response);

        void onServiceExecutedFailed(String message);
    }

    public boolean isConnected( Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public void execute(String url, final OnServiceExecute mOnServiceExecute) {
        if (!isConnected(ApplicationClass.getInstance())) {
            mOnServiceExecute.onServiceExecutedFailed(ApplicationClass.getInstance().getString(R.string.check_connection));
            return;

        }
        System.out.println("xxxurl " + url);
        this.mOnServiceExecute = mOnServiceExecute;
        Request request = new Request.Builder()
                .url(url)
                .build();

        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {

                                 mOnServiceExecute.onServiceExecutedFailed(ApplicationClass.getInstance().getString(R.string.check_connection));
                             }

                             @Override
                             public void onResponse(Call call, Response response) {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     mOnServiceExecute.onServiceExecutedResponse(res);
                                 } catch (IOException e) {
                                     mOnServiceExecute.onServiceExecutedFailed(ApplicationClass.getInstance().getString(R.string.check_connection));
                                     e.printStackTrace();
                                 }

                             }
                         }
                );
    }
    public void executePost( String url,  final OnServiceExecute mOnServiceExecute, RequestBody params)  {
        if(!isConnected(ApplicationClass.getInstance())){
            mOnServiceExecute.onServiceExecutedFailed(ApplicationClass.getInstance().getString(R.string.check_connection));
            return;

        }
      /*  RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart("name", name_)
                .addFormDataPart("email", email_)
                .addFormDataPart("id", id)
                .addFormDataPart("contact", contact__)
                .addFormDataPart("dob", birthdate)
                .addFormDataPart("blood_group", "" + bloodgroup_)
                .addFormDataPart("last_donation", "" + lastDateOfDonation)
                .addFormDataPart("donation_count", noOfDonation)
                .build();*/
        this.mOnServiceExecute=mOnServiceExecute;
        Request request = new Request.Builder()
                .url(url)
                .post(params)
                .build();

        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {

                                 mOnServiceExecute.onServiceExecutedFailed(e.getMessage());//ApplicationClass.getInstance().getString(R.string.network_error));
                             }

                             @Override
                             public void onResponse(Call call,  Response response)  {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     mOnServiceExecute.onServiceExecutedResponse(res);
                                 } catch (IOException e) {
                                     mOnServiceExecute.onServiceExecutedFailed("*"+e.getMessage());//ApplicationClass.getInstance().getString(R.string.network_error));
                                     e.printStackTrace();
                                 }

                             }
                         }
                );}

    public void uploadImage( String url,  final OnServiceExecute mOnServiceExecute, RequestBody params)  {
        if(!isConnected(ApplicationClass.getInstance())){
            mOnServiceExecute.onServiceExecutedFailed(ApplicationClass.getInstance().getString(R.string.check_connection));
            return;

        }
         MediaType MEDIA_TYPE_JPG = MediaType.parse("*/*");
      /*  requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart("api", "saveprofile")
                .addFormDataPart("username", number_)
                .addFormDataPart("sessionid", session_id)

                .addFormDataPart("img", new File(pending_fileName).getName(),
                        RequestBody.create(MEDIA_TYPE_JPG, new File(pending_fileName)))
                .build();*/
        this.mOnServiceExecute=mOnServiceExecute;
        Request request = new Request.Builder()
                .url(url)
                .post(params)
                .build();

        ApplicationClass.getInstance().httpClient.newCall(request)
                .enqueue(new Callback() {
                             @Override
                             public void onFailure(Call call, IOException e) {

                                 mOnServiceExecute.onServiceExecutedFailed(e.getMessage());
                             }

                             @Override
                             public void onResponse(Call call,  Response response)  {
                                 String res = null;
                                 try {
                                     res = response.body().string();
                                     mOnServiceExecute.onServiceExecutedResponse(res);
                                 } catch (IOException e) {
                                     mOnServiceExecute.onServiceExecutedFailed(ApplicationClass.getInstance().getString(R.string.check_connection));
                                     e.printStackTrace();
                                 }

                             }
                         }
                );}

}
