package com.example.smartfoods;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextParser {

    // Cosmetic ingredient databases
    private static final String COSING_API = "https://ec.europa.eu/growth/tools-databases/cosing/api/";
    private static final String EWG_API = "https://www.ewg.org/skindeep/api/";
    private static final String PETA_CRUELTY_FREE_API = "https://api.peta.org/cruelty-free/";

    public ArrayList<ArrayList<String>> allAllergens;
    public ArrayList<String> userAllergens = new ArrayList<>();
    public ArrayList<String> allPore;
    public ArrayList<String> allSensitive;
    public ArrayList<String> allOrganic;
    public ArrayList<String> allCruelty;
    public ArrayList<String> customAllergens = new ArrayList<>();

    public TextParser() {
        this.allAllergens = this.fillInAllergens();
        this.allPore = new ArrayList<>(Arrays.asList(
                "lanolin", "coconut", "cocoa", "shea", "wheat",
                "soybean", "isopropyl", "petrolatum", "sodium",
                "myristyl", "palmitate", "hexadecyl", "palm", "cetearyl"
        ));
        this.allSensitive = new ArrayList<>(Arrays.asList(
                "fragrance", "alcohol", "sls", "sles", "parabens",
                "benzoyl", "retinoids", "salicylic", "aha", "bha",
                "colorants", "lavender", "peppermint", "oxybenzone",
                "avobenzone", "formaldehyde", "lanolin"
        ));
        this.allOrganic = new ArrayList<>(Arrays.asList(
                "synthetic", "parabens", "phthalates", "sulfates",
                "fragrance", "silicone", "petroleum", "mineraloil",
                "triclosan", "phenoxyethanol", "peg", "polyethylene",
                "dyes", "preservatives", "artificial"
        ));
        this.allCruelty = new ArrayList<>(Arrays.asList("animal", "testing"));
        this.customAllergens = new ArrayList<>();
    }

    public ArrayList<ArrayList<String>> fillInAllergens() {
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();

        // Initialize empty lists for each category
        for (int i = 0; i < 6; i++) {
            returnList.add(new ArrayList<>());
        }

        // Fetch from multiple sources
        try {
            // 1. EU CosIng Database (Official EU cosmetic ingredient database)
            Map<String, List<String>> cosingData = fetchCosIngData();
            returnList.get(0).addAll(cosingData.get("parabens"));
            returnList.get(1).addAll(cosingData.get("fragrances"));
            returnList.get(2).addAll(cosingData.get("additives"));
            returnList.get(3).addAll(cosingData.get("sulfates"));
            returnList.get(4).addAll(cosingData.get("lanolins"));
            returnList.get(5).addAll(cosingData.get("metals"));

            // 2. EWG Skin Deep Database (Environmental Working Group)
            List<String> ewgAllergens = fetchEWGData();
            for (String allergen : ewgAllergens) {
                if (allergen.contains("paraben")) {
                    if (!returnList.get(0).contains(allergen)) {
                        returnList.get(0).add(allergen);
                    }
                } else if (allergen.contains("fragrance")) {
                    if (!returnList.get(1).contains(allergen)) {
                        returnList.get(1).add(allergen);
                    }
                } else if (allergen.contains("sulfate") || allergen.contains("sulfonate")) {
                    if (!returnList.get(3).contains(allergen)) {
                        returnList.get(3).add(allergen);
                    }
                } else if (allergen.contains("lanolin")) {
                    if (!returnList.get(4).contains(allergen)) {
                        returnList.get(4).add(allergen);
                    }
                } else if (allergen.matches(".*\\b(nickel|cobalt|chromium|mercury|gold|silver|lead|zinc|aluminum)\\b.*")) {
                    if (!returnList.get(5).contains(allergen)) {
                        returnList.get(5).add(allergen);
                    }
                }
            }

            // 3. PETA Cruelty-Free Database
            List<String> petaData = fetchPetaData();
            this.allCruelty.addAll(petaData);

        } catch (Exception e) {
            Log.e("TextParser", "Error fetching external data: " + e.getMessage());
            // Fallback to hardcoded data if API calls fail
            return getHardcodedAllergens();
        }

        return returnList;
    }

    private Map<String, List<String>> fetchCosIngData() throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        URL url = new URL(COSING_API + "allergens");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray allergens = json.getJSONArray("allergens");

            for (int i = 0; i < allergens.length(); i++) {
                JSONObject allergen = allergens.getJSONObject(i);
                String type = allergen.getString("type");
                String name = allergen.getString("name");

                if (!result.containsKey(type)) {
                    result.put(type, new ArrayList<>());
                }
                result.get(type).add(name.toLowerCase());
            }
        }
        conn.disconnect();
        return result;
    }

    private List<String> fetchEWGData() throws Exception {
        List<String> allergens = new ArrayList<>();
        URL url = new URL(EWG_API + "ingredients/allergens");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONArray json = new JSONArray(response.toString());
            for (int i = 0; i < json.length(); i++) {
                allergens.add(json.getString(i).toLowerCase());
            }
        }
        conn.disconnect();
        return allergens;
    }

    private List<String> fetchPetaData() throws Exception {
        List<String> cruelIngredients = new ArrayList<>();
        URL url = new URL(PETA_CRUELTY_FREE_API + "ingredients");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray ingredients = json.getJSONArray("ingredients");
            for (int i = 0; i < ingredients.length(); i++) {
                cruelIngredients.add(ingredients.getString(i).toLowerCase());
            }
        }
        conn.disconnect();
        return cruelIngredients;
    }

    private ArrayList<ArrayList<String>> getHardcodedAllergens() {
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();

        // paraben allergens
        returnList.add(new ArrayList<>(Arrays.asList(
                "methylparaben", "ethylparaben", "propylparaben", "butylparaben",
                "isobutylparaben", "isopropylparaben", "benzylparaben", "paraben"
        )));

        // fragrance allergens (EU's 26 recognized fragrance allergens)
        returnList.add(new ArrayList<>(Arrays.asList(
                "amyl cinnamal", "amylcinnamyl alcohol", "anisyl alcohol", "benzyl alcohol",
                "benzyl benzoate", "benzyl cinnamate", "benzyl salicylate", "butylphenyl methylpropional",
                "cinnamal", "cinnamyl alcohol", "citral", "citronellol", "coumarin", "eugenol",
                "farnesol", "geraniol", "hexyl cinnamal", "hydroxycitronellal", "hydroxyisohexyl 3-cyclohexene carboxaldehyde",
                "isoeugenol", "limonene", "linalool", "methyl 2-octynoate", "alpha-isomethyl ionone",
                "evernia prunastri extract", "evernia furfuracea extract"
        )));

        // additive allergens
        returnList.add(new ArrayList<>(Arrays.asList(
                "tartrazine", "carmine", "amaranth", "annatto", "azodyes",
                "cochineal", "phenylenediamine", "fd&c", "d&c", "ci "
        )));

        // sulphate allergens
        returnList.add(new ArrayList<>(Arrays.asList(
                "sodium lauryl sulfate", "sodium laureth sulfate", "ammonium lauryl sulfate",
                "ammonium laureth sulfate", "magnesium lauryl sulfate", "magnesium laureth sulfate",
                "sulphate", "sulfonate"
        )));

        // lanolin allergens
        returnList.add(new ArrayList<>(Arrays.asList(
                "lanolin alcohol", "wool alcohols", "acetylated lanolin alcohol", "lanolin",
                "isopropyl lanolate", "ethoxylated lanolin", "peg-75 lanolin", "peg-100 lanolin",
                "peg-16 lanolin", "hydrogenated lanolin", "laneth-10", "laneth-15",
                "laneth-20", "laneth-25", "lanolin oil"
        )));

        // metal allergens
        returnList.add(new ArrayList<>(Arrays.asList(
                "nickel", "cobalt", "chromium", "mercury", "gold",
                "silver", "lead", "zinc", "aluminum", "iron oxides",
                "titanium dioxide", "zinc oxide"
        )));

        return returnList;
    }

    public ArrayList<String> processInput(ArrayList<String> ingredients) {
        ArrayList<String> allIngredients = new ArrayList<>();
        for (String str : ingredients) {
            String line = str.toLowerCase();
            String[] linePieces = line.split(" ");
            for (String str2 : linePieces) {
                allIngredients.add(str2.replaceAll("[^a-zA-Z]", ""));
            }
        }
        return allIngredients;
    }

    private String getUserSelectedAllergenCategories() {
        StringBuilder categories = new StringBuilder();
        String[] categoryNames = {"parabens", "fragrances", "additives", "sulfates",
                "lanolin", "metals", "pore-clogging ingredients",
                "sensitive skin irritants", "non-organic ingredients",
                "animal-tested ingredients"};

        for (int i = 0; i < userAllergens.size() && i < categoryNames.length; i++) {
            if (userAllergens.get(i).equals("1")) {
                if (categories.length() > 0) categories.append(", ");
                categories.append(categoryNames[i]);
            }
        }

        if (customAllergens.size() > 0) {
            if (categories.length() > 0) categories.append(", ");
            categories.append("custom allergens (");
            categories.append(String.join(", ", customAllergens));
            categories.append(")");
        }

        return categories.toString();
    }

    public ArrayList<ArrayList<String>> checkAllergens(ArrayList<String> ingredients) {
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();
        ArrayList<String> detectedAllergens = new ArrayList<>();
        StringBuilder message = new StringBuilder();

        String userAllergenCategories = getUserSelectedAllergenCategories();
        ArrayList<String> allIngredients = this.processInput(ingredients);
        boolean foundAllergens = false;

        // Check custom allergens first
        for (String allergen : this.customAllergens) {
            for (String ingredient : allIngredients) {
                if (ingredient.toLowerCase().contains(allergen.toLowerCase())) {
                    foundAllergens = true;
                    String warning = "Custom allergen detected!\n" +
                            "You specified you are allergic to '" + allergen +
                            "' and this product contains '" + ingredient + "'.\n" +
                            "Source: User-specified allergen";
                    detectedAllergens.add(ingredient + "|" + warning);
                    break;
                }
            }
        }

        // Check standard allergens
        String[] categoryKeys = {"paraben", "fragrance", "additive", "sulphate",
                "lanolin", "metal", "pore", "sensitive",
                "organic", "cruelty"};
        String[] categoryNames = {"parabens", "fragrances", "additives", "sulfates",
                "lanolin", "metals", "pore-clogging ingredients",
                "sensitive skin irritants", "non-organic ingredients",
                "animal-tested ingredients"};

        for (String ingredient : allIngredients) {
            String lowerIngredient = ingredient.toLowerCase();
            for (int i = 0; i < this.allAllergens.size() && i < this.userAllergens.size(); i++) {
                if (this.userAllergens.get(i).equals("1")) {
                    for (String allergen : this.allAllergens.get(i)) {
                        if (lowerIngredient.contains(allergen)) {
                            foundAllergens = true;
                            String warning = "You specified you are allergic to " + categoryNames[i] +
                                    " and this product contains '" + ingredient +
                                    "'. Therefore, it is dangerous for you to use this product.";
                            detectedAllergens.add(ingredient + "|" + warning);
                            break;
                        }
                    }
                }
            }
        }

        if (foundAllergens) {
            returnList.add(detectedAllergens);
            returnList.add(new ArrayList<>(Arrays.asList(
                    "Warning: This product contains ingredients that match your specified allergens (" +
                            userAllergenCategories + ").")));
        } else {
            returnList.add(new ArrayList<>());
            returnList.add(new ArrayList<>(Arrays.asList(
                    "No allergens detected based on your specified sensitivities (" +
                            userAllergenCategories + ").")));
        }

        return returnList;
    }



    public ArrayList<String> checkPore(ArrayList<String> ingredients) {
        ArrayList<String> returnList = new ArrayList<>();
        if (this.userAllergens.size() > 6 && this.userAllergens.get(6).equals("1")) {
            ArrayList<String> allIngredients = this.processInput(ingredients);
            for (String ingredient : allIngredients) {
                for (String poreIngredient : this.allPore) {
                    if (ingredient.contains(poreIngredient)) {
                        returnList.add(ingredient);
                        break;
                    }
                }
            }
            if (!returnList.isEmpty()) {
                returnList.add(0, "Pore-clogging ingredients found:");
            }
        }
        return returnList;
    }

    public ArrayList<String> checkSensitive(ArrayList<String> ingredients) {
        ArrayList<String> returnList = new ArrayList<>();
        if (this.userAllergens.size() > 7 && this.userAllergens.get(7).equals("1")) {
            ArrayList<String> allIngredients = this.processInput(ingredients);
            for (String ingredient : allIngredients) {
                for (String sensitiveIngredient : this.allSensitive) {
                    if (ingredient.contains(sensitiveIngredient)) {
                        returnList.add(ingredient);
                        break;
                    }
                }
            }
            if (!returnList.isEmpty()) {
                returnList.add(0, "Ingredients that may irritate sensitive skin:");
            }
        }
        return returnList;
    }

    public ArrayList<String> checkOrganic(ArrayList<String> ingredients) {
        ArrayList<String> returnList = new ArrayList<>();
        if (this.userAllergens.size() > 8 && this.userAllergens.get(8).equals("1")) {
            ArrayList<String> allIngredients = this.processInput(ingredients);
            for (String ingredient : allIngredients) {
                for (String nonOrganic : this.allOrganic) {
                    if (ingredient.contains(nonOrganic)) {
                        returnList.add(ingredient);
                        break;
                    }
                }
            }
            if (!returnList.isEmpty()) {
                returnList.add(0, "Non-organic/synthetic ingredients found:");
            }
        }
        return returnList;
    }

    public ArrayList<String> checkCruelty(ArrayList<String> ingredients) {
        ArrayList<String> returnList = new ArrayList<>();
        if (this.userAllergens.size() > 9 && this.userAllergens.get(9).equals("1")) {
            ArrayList<String> allIngredients = this.processInput(ingredients);
            for (String ingredient : allIngredients) {
                for (String crueltyIngredient : this.allCruelty) {
                    if (ingredient.contains(crueltyIngredient)) {
                        returnList.add(ingredient);
                        break;
                    }
                }
            }
            if (!returnList.isEmpty()) {
                returnList.add(0, "Potential animal testing/cruelty ingredients found:");
            }
        }
        return returnList;
    }

    public int getUserBmr(int mass, int age, int height) {
        double menBmr = 66.473 + (13.7516 * mass) + (5.0033 * height) - (6.755 * age);
        double womenBmr = 655.0955 + (9.5634 * mass) + (1.8496 * height) + (4.6756 * age);
        return (int) ((menBmr + womenBmr) / 2);
    }

    public ArrayList<String> checkNutrition(ArrayList<String> nutritionFactsInput, String mass, String age, String height) {
        ArrayList<String> nutritionFacts = this.processInput(nutritionFactsInput);
        ArrayList<String> returnList = new ArrayList<>();
        StringBuilder returnString = new StringBuilder();

        try {
            int userMass = Integer.parseInt(mass);
            int userAge = Integer.parseInt(age);
            int userHeight = Integer.parseInt(height);

            if (nutritionFacts.contains("calories")) {
                try {
                    int calories = Integer.parseInt(nutritionFacts.get(nutritionFacts.indexOf("calories") + 1));
                    int userBmr = getUserBmr(userMass, userAge, userHeight);
                    int percent_cal = (int) ((calories * 100) / userBmr);

                    if (calories > userBmr) {
                        returnList.add("The calorie count in this food is over the daily recommended minimum for you!");
                        returnString.append("The calorie count in this food is over the daily recommended minimum for you!");
                    } else if (calories == userBmr) {
                        returnList.add("The calorie count in this food is at the daily recommended minimum for you!");
                        returnString.append("The calorie count in this food is at the daily recommended minimum for you!");
                    } else {
                        returnList.add("The calorie count in this food is " + percent_cal + "% of the daily recommended minimum for you");
                        returnString.append("The calorie count in this food is ").append(percent_cal).append("% of the daily recommended minimum for you");
                    }
                } catch (Exception e) {
                    returnList.add("Calorie related data could not be calculated");
                    returnString.append("Calorie related data could not be calculated");
                }
            }

            if (nutritionFacts.contains("sodium")) {
                try {
                    int sodium_mass = Integer.parseInt(nutritionFacts.get(nutritionFacts.indexOf("sodium") + 1));
                    int percent_sodium = (int) ((sodium_mass * 100) / 2300);

                    if (percent_sodium > 100) {
                        returnList.add("The sodium content in this food is over the daily recommended limit of 2300 milligrams!");
                        returnString.append(" The sodium content in this food is over the daily recommended limit of 2300 milligrams!");
                    } else if (percent_sodium == 100) {
                        returnList.add("The sodium content in this food is at the daily recommended limit of 2300 milligrams!");
                        returnString.append(" The sodium content in this food is at the daily recommended limit of 2300 milligrams!");
                    } else {
                        returnList.add("The sodium content in this food is " + percent_sodium + "% of the daily recommended limit of 2300 milligrams");
                        returnString.append(" The sodium content in this food is ").append(percent_sodium).append("% of the daily recommended limit of 2300 milligrams");
                    }
                } catch (Exception e) {
                    returnList.add("Sodium related data could not be calculated");
                    returnString.append(" Sodium related data could not be calculated");
                }
            }

        } catch (Exception e) {
            returnList.add("The weight, age and/or height for the user is/are invalid");
            returnString.append("The weight, age or height for the user is invalid");
        }

        if (returnString.length() > 0) {
            returnList.add(returnString.toString());
        }
        return returnList;
    }

    public void setUserPreferences(String input) {
        this.userAllergens.clear();
        Log.i("TextParser", "Setting user preferences: " + input);
        for (int i = 0; i < input.length() && i < 10; i++) {
            this.userAllergens.add(Character.toString(input.charAt(i)));
        }
    }
}