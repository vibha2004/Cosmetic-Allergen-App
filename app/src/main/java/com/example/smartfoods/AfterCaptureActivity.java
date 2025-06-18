package com.example.smartfoods;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfoods.ocr.OcrCaptureActivity;
import com.example.smartfoods.TextParser;

import java.util.ArrayList;
import java.util.Locale;

public class AfterCaptureActivity extends AppCompatActivity {

    ArrayList<String> itemList;
    Button anotherPicture;
    Button textToSpeechButton;
    ImageView icon;
    TextView titleText;
    TextParser parser = new TextParser();
    LinearLayout badIngredientsBox;

    private void displayNegativeNested(ArrayList<ArrayList<String>> result) {
        // Add section header
        TextView sectionHeader = new TextView(this);
        sectionHeader.setText("Allergen Warnings");
        sectionHeader.setTextColor(Color.rgb(209, 89, 98));
        sectionHeader.setTextSize(18);
        sectionHeader.setTypeface(null, Typeface.BOLD);
        sectionHeader.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        sectionHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        sectionHeader.setPadding(0, 0, 0, 16);
        badIngredientsBox.addView(sectionHeader);

        // Rest of your existing code with improved item styling
        for (int i = 0; i < result.size() - 1; i++) {
            for (int j = 0; j < result.get(i).size(); j++) {
                String[] parts = result.get(i).get(j).split("\\|", 2);
                String ingredient = parts[0];
                String explanation = parts.length > 1 ? parts[1] : "";

                // Create a horizontal layout for each allergen item
                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                itemLayout.setPadding(0, 8, 0, 8);

                // Add warning icon
                ImageView icon = new ImageView(this);
                icon.setImageResource(R.drawable.ic_warning);
                icon.setLayoutParams(new LinearLayout.LayoutParams(
                        24, 24));
                icon.setPadding(0, 0, 16, 0);
                itemLayout.addView(icon);

                // Add ingredient text
                TextView text = new TextView(this);
                text.setText(ingredient);
                text.setTextColor(Color.rgb(209, 89, 98));
                text.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                text.setGravity(Gravity.START);
                text.setTextSize(16);
                text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                // Add click listener
                text.setOnClickListener(v -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Allergen Warning");
                    builder.setMessage(explanation);
                    builder.setPositiveButton("OK", null);
                    builder.show();
                });

                itemLayout.addView(text);
                badIngredientsBox.addView(itemLayout);
            }
        }
    }
    Drawable check;
    Drawable negative;
    String preferences;
    TextToSpeech ts;
    StringBuilder speechText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_capture);

        anotherPicture = (Button) findViewById(R.id.AnotherPicture);
        preferences = getIntent().getExtras().getString("preferences");
        Log.i("Prefs:", "In the after capture act " + preferences);

        itemList = (ArrayList<String>) getIntent().getSerializableExtra("ING-LIST");
        icon = (ImageView) findViewById(R.id.icon);
        titleText = (TextView) findViewById(R.id.TitleText);
        badIngredientsBox = (LinearLayout) findViewById(R.id.BadIngredientsBox);
        textToSpeechButton = (Button) findViewById(R.id.TextToSpeech);

        parser.setUserPreferences(preferences);

        check = getResources().getDrawable(R.drawable.check);
        negative = getResources().getDrawable(R.drawable.negative);

        ts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    ts.setLanguage(Locale.US);
                }
            }
        });

        ts.setSpeechRate(0.9f);

        for (int i = 0; i < itemList.size(); i++) {
            Log.i("ITEM " + i, itemList.get(i));
        }

        // Get allergen checks
        ArrayList<ArrayList<String>> allergenItems = parser.checkAllergens(itemList);
        ArrayList<String> lactoseItems = parser.checkPore(itemList);
        ArrayList<String> veganItems = parser.checkSensitive(itemList);
        ArrayList<String> vegetarianItems = parser.checkOrganic(itemList);
        ArrayList<String> glutenItems = parser.checkCruelty(itemList);

        // Log actual contents of lists
        Log.i("Allergen Check", "Allergen items: " + allergenItems.get(0).size());
        Log.i("Allergen Check", "Allergen message: " + allergenItems.get(1).size());
        Log.i("Allergen Check", "Lactose items: " + lactoseItems.size());
        Log.i("Allergen Check", "Vegan items: " + veganItems.size());
        Log.i("Allergen Check", "Vegetarian items: " + vegetarianItems.size());
        Log.i("Allergen Check", "Gluten items: " + glutenItems.size());

        // Check if any actual allergens were found
        boolean hasAllergens = !allergenItems.get(0).isEmpty();
        boolean hasLactose = !lactoseItems.isEmpty();
        boolean hasVegan = !veganItems.isEmpty();
        boolean hasVegetarian = !vegetarianItems.isEmpty();
        boolean hasGluten = !glutenItems.isEmpty();

        if (!hasAllergens && !hasLactose && !hasVegan && !hasVegetarian && !hasGluten) {
            Log.i("OK", "its a");
            
            // Set title text and color
            titleText.setText("Product is Safe!");
            titleText.setTextColor(Color.rgb(102, 187, 106)); // Green color
            
            // Show checkmark icon
            icon.setImageDrawable(check);
            
            // Clear any previous views and add safe message
            badIngredientsBox.removeAllViews();
            
            // Add "Product is Safe" message with check icon
            TextView okMessage = new TextView(this);
            okMessage.setText("✓ All ingredients are safe based on your preferences.");
            okMessage.setTextColor(Color.rgb(102, 187, 106)); // Green color
            okMessage.setTextSize(18); // Slightly larger text
            okMessage.setTypeface(null, Typeface.BOLD); // Make it bold
            okMessage.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
            okMessage.setGravity(Gravity.CENTER_HORIZONTAL);
            okMessage.setPadding(0, 16, 0, 16); // Add some padding
            
            badIngredientsBox.addView(okMessage);
        } else {
            Log.i("OK", "its n");
            speechText.append("The ingredients are not okay, ");
            titleText.setText("Ingredients are not OK. ");
            titleText.setTextColor(Color.rgb(209, 89, 98));
            icon.setImageDrawable(negative);

            if (allergenItems.size() > 0) {
                displayNegativeNested(allergenItems);
            }

            if (lactoseItems.size() > 0) {
                displayNegative(lactoseItems);
            }

            if (veganItems.size() > 0) {
                displayNegative(veganItems);
            }

            if (vegetarianItems.size() > 0) {
                displayNegative(vegetarianItems);
            }

            if (glutenItems.size() > 0) {
                displayNegative(glutenItems);
            }
        }

        anotherPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterCaptureActivity.this, OcrCaptureActivity.class);
                // Pass the full preference string including custom allergens
                intent.putExtra("preferences", preferences);
                startActivity(intent);
            }
        });

        textToSpeechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ts.speak(speechText.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    ts.speak(speechText.toString(), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    private boolean noBadIngredients(ArrayList<ArrayList<String>> allergenItems,
                                     ArrayList<String> lactoseItems,
                                     ArrayList<String> veganItems,
                                     ArrayList<String> vegetarianItems,
                                     ArrayList<String> glutenItems) {
        // Only check the first list in allergenItems for actual allergens
        boolean hasAllergens = !allergenItems.get(0).isEmpty();
        boolean hasLactose = !lactoseItems.isEmpty();
        boolean hasVegan = !veganItems.isEmpty();
        boolean hasVegetarian = !vegetarianItems.isEmpty();
        boolean hasGluten = !glutenItems.isEmpty();
        
        Log.i("Allergen Check", "Has allergens: " + hasAllergens);
        Log.i("Allergen Check", "Has lactose: " + hasLactose);
        Log.i("Allergen Check", "Has vegan: " + hasVegan);
        Log.i("Allergen Check", "Has vegetarian: " + hasVegetarian);
        Log.i("Allergen Check", "Has gluten: " + hasGluten);
        
        return !(hasAllergens || hasLactose || hasVegan || hasVegetarian || hasGluten);
    }

    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(AfterCaptureActivity.this, MainActivity.class);
        i.putExtra("preferences", preferences);
        startActivity(i);
        finish();
    }


    private void displayNegative(ArrayList<String> result) {
        Log.i("Cheese", "in" + result.size());
        for (int i = 0; i < result.size() - 1; i++) {
            Log.i("Cheese", result.get(i));
        }

        for (int i = 0; i < result.size() - 1; i++) {
            Log.i("OK", result.get(i));
            TextView text = new TextView(this);
            text.setText(result.get(i));
            text.setTextColor(Color.rgb(209, 89, 98));
            text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            text.setGravity(Gravity.CENTER_HORIZONTAL);
            badIngredientsBox.addView(text);
        }
        speechText.append(result.get(result.size() - 1));
        speechText.append(" ");
    }
}
