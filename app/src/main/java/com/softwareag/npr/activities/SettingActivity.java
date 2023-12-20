package com.softwareag.npr.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.softwareag.npr.R;
import com.softwareag.npr.utils.Constants;
import com.softwareag.npr.utils.GoogleSheetsUtils;
import com.softwareag.npr.utils.StringUtils;

public class SettingActivity extends AppCompatActivity {
    Button applychanged, loadData;
    String SHARED_PREF_NAME = "user_pref";
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    String token = "";
    EditText nprUrl, nprToken, googleUrl, googleAPIKey, googleAPIToken, googleSheetID, googleSheetName;
    Spinner resizeImageEdit;
    ArrayAdapter<CharSequence> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        pref = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        editor = pref.edit();

        resizeImageEdit = findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        adapter = ArrayAdapter.createFromResource(this,
                R.array.size, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        resizeImageEdit.setAdapter(adapter);


        nprUrl = findViewById(R.id.default_url);
        nprToken = findViewById(R.id.token_Code);
        applychanged = findViewById(R.id.applyChanged);
        loadData = findViewById(R.id.loadData);
        googleUrl = findViewById(R.id.google_url);
        googleAPIKey = findViewById(R.id.google_api_key);
        googleAPIToken = findViewById(R.id.google_api_token);
        googleSheetID = findViewById(R.id.google_sheet_id);
        googleSheetName = findViewById(R.id.google_sheet_name);

        loadValuesFromPreference();

        ImageButton back = findViewById(R.id.backT_btn_type);
        back.setOnClickListener(v -> finish());

        applychanged.setOnClickListener(v -> {
            if (isValidInputs()) {
                saveUserPreference();
            }
        });
        loadData.setOnClickListener(view -> loadVehicleNumbers());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String baseurl = pref.getString(Constants.NPR_URL, Constants.DEFAULT_NPR_API_ENDPOINT);
        nprUrl.setText(baseurl);

        int spinnerValue = pref.getInt(Constants.SPINNER_POSITION, -1);
        if (spinnerValue != -1)
            // set the value of the spinner
            resizeImageEdit.setSelection(spinnerValue);
    }

    private boolean isValidInputs() {
        boolean isValid = true;
        if (StringUtils.isBlank(nprUrl.getText().toString())) {
            nprUrl.setError("Enter url");
            isValid = false;
        } else if (StringUtils.isBlank(nprToken.getText().toString())) {
            nprToken.setError("Enter token");
            isValid = false;
        } else if (StringUtils.isBlank(googleUrl.getText().toString())) {
            googleUrl.setError("Enter url");
            isValid = false;
        } else if (StringUtils.isBlank(googleAPIKey.getText().toString())) {
            googleAPIKey.setError("Enter api key");
            isValid = false;
        } else if (StringUtils.isBlank(googleAPIToken.getText().toString())) {
            googleAPIToken.setError("Enter api token");
            isValid = false;
        } else if (StringUtils.isBlank(googleSheetID.getText().toString())) {
            googleSheetID.setError("Enter sheet id");
            isValid = false;
        } else if (StringUtils.isBlank(googleSheetName.getText().toString())) {
            googleSheetName.setError("Enter sheet name");
            isValid = false;
        }
        return isValid;
    }

    private void loadValuesFromPreference() {
        nprUrl.setText(pref.getString(Constants.NPR_URL, Constants.DEFAULT_NPR_API_ENDPOINT));
        nprToken.setText(pref.getString(Constants.NPR_TOKEN, Constants.DEFAULT_NPR_API_TOKEN));
        googleUrl.setText(pref.getString(Constants.GOOGLE_SHEET_URL, Constants.DEFAULT_GOOGLE_SHEETS_API_ENDPOINT));
        googleAPIKey.setText(pref.getString(Constants.GOOGLE_API_KEY, Constants.DEFAULT_GOOGLE_SHEETS_API_KEY));
        googleAPIToken.setText(pref.getString(Constants.GOOGLE_API_TOKEN, Constants.DEFAULT_GOOGLE_SHEETS_API_TOKEN));
        googleSheetID.setText(pref.getString(Constants.GOOGLE_SHEET_ID, Constants.DEFAULT_GOOGLE_SPREAD_SHEET_ID));
        googleSheetName.setText(pref.getString(Constants.GOOGLE_SHEET_NAME, Constants.DEFAULT_GOOGLE_SPREAD_SHEET_NAME));
    }

    private void saveUserPreference() {
        editor.putString(Constants.NPR_URL, nprUrl.getText().toString()).apply();
        editor.putString(Constants.NPR_TOKEN, nprToken.getText().toString()).apply();

        editor.putInt(Constants.RESIZE_PERCENT, Integer.parseInt(resizeImageEdit.getSelectedItem().toString())).apply();
        editor.putInt(Constants.SPINNER_POSITION, resizeImageEdit.getSelectedItemPosition()).apply();

        editor.putString(Constants.GOOGLE_SHEET_URL, googleUrl.getText().toString()).apply();
        editor.putString(Constants.GOOGLE_API_KEY, googleAPIKey.getText().toString()).apply();
        editor.putString(Constants.GOOGLE_API_TOKEN, googleAPIToken.getText().toString()).apply();
        editor.putString(Constants.GOOGLE_SHEET_ID, googleSheetID.getText().toString()).apply();
        editor.putString(Constants.GOOGLE_SHEET_NAME, googleSheetName.getText().toString()).apply();

        editor.commit();
        finish();
        Toast.makeText(SettingActivity.this, Constants.SAVED_SUCCESSFULLY, Toast.LENGTH_SHORT).show();
    }

    private void loadVehicleNumbers() {
        GoogleSheetsUtils.loadVehicleDetails(getApplicationContext(), pref, editor);
    }


}
