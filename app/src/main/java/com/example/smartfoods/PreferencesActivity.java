package com.example.smartfoods;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import android.util.Log;

public class PreferencesActivity extends AppCompatActivity {

    CheckBox paraben;
    CheckBox fragrance;
    CheckBox additive;
    CheckBox sulphate;
    CheckBox lanolin;
    CheckBox metal;
    CheckBox pore;
    CheckBox sensitive;
    CheckBox organic;
    CheckBox cruelty;
    Button apply;
    private String preferenceSelection;
    private EditText customAllergenInput;
    private Button addAllergenButton;
    private TextView addedAllergensText;
    private ArrayList<String> customAllergens = new ArrayList<>();

    // Cosmetic ingredient databases
    private static final String COSING_API = "https://ec.europa.eu/growth/tools-databases/cosing/api/";
    private static final String EWG_API = "https://www.ewg.org/skindeep/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        preferenceSelection = getIntent().getExtras().getString("preferences");
        paraben = (CheckBox) findViewById(R.id.Paraben);
        fragrance = (CheckBox) findViewById(R.id.Fragrance);
        additive = (CheckBox) findViewById(R.id.Additive);
        sulphate = (CheckBox) findViewById(R.id.Sulphate);
        lanolin = (CheckBox) findViewById(R.id.Lanolin);
        metal = (CheckBox) findViewById(R.id.Metal);
        pore = (CheckBox) findViewById(R.id.Pore);
        sensitive = (CheckBox) findViewById(R.id.Sensitive);
        organic = (CheckBox) findViewById(R.id.Organic);
        cruelty = (CheckBox) findViewById(R.id.Cruelty);

        apply = (Button) findViewById(R.id.Apply);

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                CharSequence text = "Preferences have been saved.";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                preferenceSelection = getPreferenceString();
            }
        });

        // Initialize custom allergen UI elements
        customAllergenInput = findViewById(R.id.customAllergenInput);
        addAllergenButton = findViewById(R.id.addAllergenButton);
        addedAllergensText = findViewById(R.id.addedAllergensText);

        // Set up add allergen button click listener
        addAllergenButton.setOnClickListener(v -> {
            String allergen = customAllergenInput.getText().toString().trim();
            if (!allergen.isEmpty()) {
                if (!customAllergens.contains(allergen)) {
                    customAllergens.add(allergen);
                    updateAddedAllergensText();
                    customAllergenInput.setText("");
                    Toast.makeText(this, "Allergen added: " + allergen, Toast.LENGTH_SHORT).show();
                    
                    // Verify the allergen with APIs in background
                    new VerifyAllergenTask().execute(allergen);
                } else {
                    Toast.makeText(this, "Allergen already added", Toast.LENGTH_SHORT).show();
                }
            }
        });

        checkPreviousPreferences();
    }

    private void checkPreviousPreferences() {
        if (preferenceSelection.charAt(0) == '1') {
            paraben.setChecked(true);
        }

        if (preferenceSelection.charAt(1) == '1') {
            fragrance.setChecked(true);
        }

        if (preferenceSelection.charAt(2) == '1') {
            additive.setChecked(true);
        }

        if (preferenceSelection.charAt(3) == '1') {
            sulphate.setChecked(true);
        }

        if (preferenceSelection.charAt(4) == '1') {
            lanolin.setChecked(true);
        }

        if (preferenceSelection.charAt(5) == '1') {
            metal.setChecked(true);
        }

        if (preferenceSelection.charAt(6) == '1') {
            pore.setChecked(true);
        }

        if (preferenceSelection.charAt(7) == '1') {
            sensitive.setChecked(true);
        }

        if (preferenceSelection.charAt(8) == '1') {
            organic.setChecked(true);
        }

        if (preferenceSelection.charAt(9) == '1') {
            cruelty.setChecked(true);
        }

        // Check for custom allergens after the 10-digit preference string
        if (preferenceSelection.length() > 10 && preferenceSelection.contains("|")) {
            String[] parts = preferenceSelection.split("\\|");
            if (parts.length > 1 && !parts[1].isEmpty()) {
                customAllergens = new ArrayList<>(Arrays.asList(parts[1].split("\\,")));
                updateAddedAllergensText();
            }
        }
    }

    private String getPreferenceString() {
        StringBuilder sb = new StringBuilder();

        if (paraben.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (fragrance.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (additive.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (sulphate.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (lanolin.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (metal.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (pore.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (sensitive.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (organic.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        if (cruelty.isChecked()) {
            sb.append('1');
        } else {
            sb.append('0');
        }

        // Add custom allergens if any
        if (!customAllergens.isEmpty()) {
            sb.append('|');
            sb.append(String.join(",", customAllergens));
        }
        return sb.toString();
    }

    private void updateAddedAllergensText() {
        if (customAllergens.isEmpty()) {
            addedAllergensText.setText("No custom allergens added");
        } else {
            addedAllergensText.setText("Added allergens: " + String.join(", ", customAllergens));
        }
    }

    private class VerifyAllergenTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... allergens) {
            String allergen = allergens[0];
            try {
                // Check CosIng database first
                String cosingInfo = checkCosIngDatabase(allergen);
                if (cosingInfo != null) return cosingInfo;

                // Then check EWG database
                String ewgInfo = checkEWGDatabase(allergen);
                if (ewgInfo != null) return ewgInfo;

                return "No official information found about " + allergen + 
                       ". It will still be checked against ingredients.";
            } catch (Exception e) {
                return "Error verifying allergen: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            new AlertDialog.Builder(PreferencesActivity.this)
                .setTitle("Allergen Verification")
                .setMessage(result)
                .setPositiveButton("OK", null)
                .show();
        }

        private String checkCosIngDatabase(String allergen) throws Exception {
            URL url = new URL(COSING_API + "search?q=" + URLEncoder.encode(allergen, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder response = new StringBuilder();
            try {
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    try {
                        JSONObject json = new JSONObject(response.toString());
                        if (json.has("ingredients") && json.getJSONArray("ingredients").length() > 0) {
                            JSONObject ingredient = json.getJSONArray("ingredients").getJSONObject(0);
                            return "Verified by EU CosIng database:\n" +
                                   "Name: " + ingredient.optString("name", "N/A") + "\n" +
                                   "Function: " + ingredient.optString("function", "N/A") + "\n" +
                                   "Restrictions: " + ingredient.optString("restrictions", "None");
                        }
                    } catch (Exception e) {
                        Log.e("VerifyAllergen", "Error parsing CosIng JSON: " + e.getMessage());
                        return null;
                    }
                }
            } catch (Exception e) {
                Log.e("VerifyAllergen", "Error connecting to CosIng: " + e.getMessage());
                return null;
            } finally {
                conn.disconnect();
            }
            return null;
        }

        private String checkEWGDatabase(String allergen) throws Exception {
            URL url = new URL(EWG_API + "search?q=" + URLEncoder.encode(allergen, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder response = new StringBuilder();
            try {
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    try {
                        JSONObject json = new JSONObject(response.toString());
                        if (json.has("results") && json.getJSONArray("results").length() > 0) {
                            JSONObject result = json.getJSONArray("results").getJSONObject(0);
                            return "Verified by EWG Skin Deep:\n" +
                                   "Name: " + result.optString("name", "N/A") + "\n" +
                                   "Score: " + result.optString("score", "N/A") + "\n" +
                                   "Concerns: " + result.optString("concerns", "None");
                        }
                    } catch (Exception e) {
                        Log.e("VerifyAllergen", "Error parsing EWG JSON: " + e.getMessage());
                        return null;
                    }
                }
            } catch (Exception e) {
                Log.e("VerifyAllergen", "Error connecting to EWG: " + e.getMessage());
                return null;
            } finally {
                conn.disconnect();
            }
            return null;
        }
    }

    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(PreferencesActivity.this, MainActivity.class);
        i.putExtra("preferences", preferenceSelection);
        startActivity(i);
        finish();
    }
}
