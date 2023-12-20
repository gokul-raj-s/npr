package com.softwareag.npr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GoogleSheetsUtils {

    public static void loadVehicleDetails(Context context, SharedPreferences pref, SharedPreferences.Editor editor){

        getVehicleNumbers(pref, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showToast(context, Constants.UNABLE_TO_LOAD_DATA);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            String jsonData = response.body().string();
                            JSONObject output = new JSONObject(jsonData);
                            JSONArray values = output.getJSONArray("values");
                            Map<String, String> vehicleDetails = new HashMap<>();
                            for (int i = 0; i < values.length(); i++) {
                                JSONArray row = values.getJSONArray(i);
                                vehicleDetails.put(row.getString(0), row.getString(1));
                            }
                            Gson gson = new Gson();
                            String vehicleDetailsString = gson.toJson(vehicleDetails);
                            editor.putString(Constants.VEHICLE_DETAILS, vehicleDetailsString).apply();
                            editor.commit();
                            showToast(context, vehicleDetails.size() + Constants.LOADED_SUCCESSFULLY);
                        } catch (JSONException e) {
                            showToast(context, Constants.UNABLE_TO_LOAD_DATA);
                        }
                    }
                } else {
                    showToast(context, Constants.UNABLE_TO_LOAD_DATA);
                }
            }
        });
    }

    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    private static void getVehicleNumbers(SharedPreferences pref, Callback callback) {
        String baseURL = pref.getString(Constants.GOOGLE_SHEET_URL, Constants.DEFAULT_GOOGLE_SHEETS_API_ENDPOINT);
        String sheetID = pref.getString(Constants.GOOGLE_SHEET_ID, Constants.DEFAULT_GOOGLE_SPREAD_SHEET_ID);
        String sheetName = pref.getString(Constants.GOOGLE_SHEET_NAME, Constants.DEFAULT_GOOGLE_SPREAD_SHEET_NAME);
        String googleAPIKey = pref.getString(Constants.GOOGLE_API_KEY, Constants.DEFAULT_GOOGLE_SHEETS_API_KEY);
        String apiToken = pref.getString(Constants.GOOGLE_API_TOKEN, Constants.DEFAULT_GOOGLE_SHEETS_API_TOKEN);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(baseURL + sheetID + "/values/" + sheetName)
                .addHeader(googleAPIKey, apiToken)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }
}
