package com.softwareag.npr.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cardview.widget.CardView;
import androidx.core.text.HtmlCompat;

import cz.msebera.android.httpclient.Header;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.softwareag.npr.R;
import com.softwareag.npr.network.WebRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.softwareag.npr.utils.Constants;
import com.softwareag.npr.utils.GoogleSheetsUtils;
import com.softwareag.npr.utils.StringUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.enums.EPickType;
import com.vansuita.pickimage.listeners.IPickResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements IPickResult, View.OnClickListener {

    ImageView imageView, camera, settings;
    TextView region_txt, vehicle_txt, company_name, app_desc;

    EditText plate_txt;
    Context context;
    Button nextImage;
    ProgressBar progressBar;
    SharedPreferences sharedPreferences;

    SharedPreferences.Editor editor;

    String SHARED_PREF_NAME = "user_pref";
    String token = "";
    Date date;
    DateFormat df;
    String timeStamp = "";
    CardView plateCard, regionCard, vihicalCard;
    FloatingActionButton floatingActionButton;
    String imagepath;

    LinearLayout check_again;

    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        builder = new AlertDialog.Builder(this, R.style.appAlertDialog);
        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        date = new Date();
        df = new SimpleDateFormat("MM/dd/");
        // Use London time zone to format the date in
        df.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        nextImage = findViewById(R.id.next_image);
        nextImage.setOnClickListener(this);
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);
        progressBar = findViewById(R.id.homeprogress);
        plate_txt = findViewById(R.id.car_plate);
        region_txt = findViewById(R.id.region_code);
        vehicle_txt = findViewById(R.id.vihicle_type);
        settings = findViewById(R.id.settings);
        settings.setOnClickListener(this);
        camera = findViewById(R.id.camera);
        camera.setOnClickListener(this);
        plateCard = findViewById(R.id.cardView);
        vihicalCard = findViewById(R.id.cardView3);
        regionCard = findViewById(R.id.cardView2);
        imageView = findViewById(R.id.imageView);
        check_again = findViewById(R.id.check_again);
        check_again.setOnClickListener(this);
        company_name = findViewById(R.id.company_name);
        app_desc = findViewById(R.id.app_desc);

        loadVehicleNumbers();
    }

    private void loadVehicleNumbers() {
        GoogleSheetsUtils.loadVehicleDetails(getApplicationContext(), sharedPreferences, editor);
    }

    //dialoag box setup
    @SuppressLint("WrongConstant")
    PickSetup setup = new PickSetup()
            .setTitle("Choose")
            .setCancelText("Cancel")
            .setFlip(true)
            .setMaxSize(50)
            .setWidth(50)
            .setHeight(50)
            .setProgressText("Loading Image")
            .setPickTypes(EPickType.GALLERY, EPickType.CAMERA)
            .setCameraButtonText("Camera")
            .setGalleryButtonText("Gallery")
            .setIconGravity(Gravity.TOP)
            .setButtonOrientation(LinearLayoutCompat.HORIZONTAL)
            .setSystemDialog(false)
            .setGalleryIcon(R.drawable.photo)
            .setCameraIcon(R.drawable.cam);

    @Override
    protected void onResume() {
        super.onResume();
        token = sharedPreferences.getString(Constants.NPR_TOKEN, "");
        if (token.equals("")) {
            token = Constants.DEFAULT_NPR_API_TOKEN;
            editor.putString(Constants.NPR_TOKEN, token).apply();
            editor.commit();
            Toast.makeText(context, "Setting default token", Toast.LENGTH_SHORT).show();
        } else {
            WebRequest.client.addHeader("Authorization", "Token " + token);
        }
    }

    //pick result method to get image after getting image form gallary or camera
    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            RequestParams params = new RequestParams();
            String file = r.getPath();
            String compressed = compressImage(file);
            String baseurl = sharedPreferences.getString(Constants.NPR_URL, Constants.DEFAULT_NPR_API_ENDPOINT);

            Log.d("response", "filepath: " + file + " ");
            try {
                params.put("upload", new File(compressed));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            params.put("regions","IN");
            Log.d("response", "image to upload: " + params + " ");
            WebRequest.post(context, baseurl, params, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    region_txt.setText(null);
                    plate_txt.setText(null);
                    vehicle_txt.setText(null);
                    imageView.setImageResource(R.drawable.upload);

                    Log.d("response", "onStart: ");
                    super.onStart();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);

                    Log.d("response ", response.toString() + " ");
                    try {
                        //image path
                        imagepath = "https://us-east-1.linodeobjects.com/platerec-api/uploads/" + df.format(date) + response.getString("filename");
                        //json array or results
                        JSONArray Jsresults = response.getJSONArray("results");
                        if (Jsresults.length() > 0) {
                            for (int i = 0; i < Jsresults.length(); i++) {
                                JSONObject tabObj = Jsresults.getJSONObject(i);
                                String vehicleNumber = tabObj.getString("plate");
                                plate_txt.setText(vehicleNumber.toUpperCase());
                                region_txt.setText(tabObj.getJSONObject("region").getString("code").toUpperCase());
                                vehicle_txt.setText(tabObj.getJSONObject("vehicle").getString("type"));

                                checkVehicleNumber(vehicleNumber.toUpperCase());

                                timeStamp = response.getString("timestamp");
                                Picasso.with(context)
                                        .load(imagepath)
                                        .into(imageView, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                                progressBar.setVisibility(View.GONE);
                                            }

                                            @Override
                                            public void onError() {

                                            }
                                        });
                                setScanPageVisible();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.d("response1", "onFailure: " + errorResponse + " ");
                    setLandingPageVisible();
                    Toast.makeText(MainActivity.this, errorResponse + "", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.d("response2", "onFailure: " + errorResponse + " ");
                   setLandingPageVisible();
                    Toast.makeText(MainActivity.this, errorResponse.toString() + "", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Log.d("response3", "onFailure: " + responseString + " ");
                    setLandingPageVisible();
                    Toast.makeText(MainActivity.this, responseString + "No Internet Connection", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            //Handle possible errors
            //TODO: do what you have to do with r.getError();
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLandingPageVisible(){
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        regionCard.setVisibility(View.GONE);
        plateCard.setVisibility(View.GONE);
        vihicalCard.setVisibility(View.GONE);
        nextImage.setVisibility(View.GONE);
        floatingActionButton.setVisibility(View.GONE);
        camera.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
    }

    private void setScanPageVisible(){
        imageView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        regionCard.setVisibility(View.VISIBLE);
        plateCard.setVisibility(View.VISIBLE);
        vihicalCard.setVisibility(View.VISIBLE);
        nextImage.setVisibility(View.VISIBLE);
        floatingActionButton.setVisibility(View.VISIBLE);
        camera.setVisibility(View.GONE);
        settings.setVisibility(View.GONE);
    }

    private void checkVehicleNumber(String vehicleNumber) {
        String vehicleDetailsString = sharedPreferences.getString(Constants.VEHICLE_DETAILS, "");
        if (StringUtils.isBlank(vehicleDetailsString)) {
            GoogleSheetsUtils.loadVehicleDetails(getApplicationContext(), sharedPreferences, editor);
        }
        java.lang.reflect.Type type = new TypeToken<HashMap<String, String>>(){}.getType();
        Gson gson = new Gson();
        HashMap<String, String> vehicleDetails = gson.fromJson(vehicleDetailsString, type);
        if (vehicleDetails.containsKey(vehicleNumber)) {
            openAlertDialog(HtmlCompat.fromHtml("<b><p style='color:#008000'>Name: " + vehicleDetails.get(vehicleNumber) + "</p></br>" +
                    "<p style='color:#008000'>Vehicle: " + vehicleNumber + "</p></font></b>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        } else {
            openAlertDialog(HtmlCompat.fromHtml("<b><font color='#C70039'>" + "Vehicle No: " + vehicleNumber + " not found."+ "</font></b>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
    }

    private void openAlertDialog(Spanned message){
        builder.setMessage(message)
                .setTitle("Software AG")
                .setIcon(R.drawable.logo)
                .setCancelable(true)
                .setPositiveButton("Ok", (dialog, id) -> {});
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //check if request code is for inserting new list then perform insertion
        if (requestCode == 123 && resultCode == RESULT_OK) {
            String plate = data.getStringExtra("car_plate");
            String region = data.getStringExtra("region_code");
            String car = data.getStringExtra("car_type");
            Log.d("response", "onActivityResult: " + plate + " ");
            plate_txt.setText(plate);
            region_txt.setText(region);
            vehicle_txt.setText(car);
            Toast.makeText(this, "Results saved", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int TIME_INTERVAL = 2000;
    private long mBackPressed;

    @Override
    public void onBackPressed() {
        if (imageView.getVisibility() == View.VISIBLE) {
            setLandingPageVisible();
            return;
        }
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        mBackPressed = System.currentTimeMillis();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.camera) {
            if (token.isEmpty()) {
                Toast.makeText(this, "Go to Settings to Set Your Token", Toast.LENGTH_LONG).show();
                return;
            }
            PickImageDialog.build(setup).show(MainActivity.this);
        }
        if (v.getId() == R.id.settings) {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        }
        if (v.getId() == R.id.next_image) {
            PickImageDialog.build(setup).show(MainActivity.this);
        }
        if (v.getId() == R.id.fab) {
            String plate = plate_txt.getText().toString();
            String region = region_txt.getText().toString();
            String type = vehicle_txt.getText().toString();
            //Uri bmpUri = getLocalBitmapUri(imageView);
            //set it as current date.
            String share = "Date & TimeStamp: " + timeStamp + "\nVehicle No: " + plate + "\nRegion Code: " + region + "\nVehicle Type: " + type;
            Log.d("response", "onActivityResult: " + plate + " ");
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, share);
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, "send"));
        }
        if(v.getId() == R.id.check_again){
            checkVehicleNumber(plate_txt.getText().toString().toUpperCase());
        }
    }

    public String compressImage(String filePath) {

        int resized = sharedPreferences.getInt(Constants.RESIZE_PERCENT,50);

        Bitmap scaledBitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        float maxHeight = resized * 7.0f;
        float maxWidth = resized * 12.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;

            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;
            }
        }
        options.inSampleSize = calculateInSampleSize(options, actualWidth,
                actualHeight);
        //      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;
        //      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];
        try {
            //          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth,
                    actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        //      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        String filename = getFilename(this);
        try {
            out = new FileOutputStream(filename);
            //          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, resized, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return filename;
    }

    public static String getFilename(Context context) {
        File file = new File(context.getFilesDir().getPath(), ".Foldername/PlateRecognizerHistory");

        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");

        return uriSting;

    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;

        final int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int heightRatio = Math.round((float) height / (float)
                    reqHeight);

            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

        }
        final float totalPixels = width * height;

        final float totalReqPixelsCap = reqWidth * reqHeight * 2;

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }
        return inSampleSize;
    }
}