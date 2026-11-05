
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddItemActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 100;

    EditText etTitle, etDescription, etLocation, etType;
    Button btnSaveItem, btnChooseImage;
    ImageView ivSelectedImage;

    DatabaseReference databaseReference;
    FirebaseAuth auth;

    Uri selectedImageUri;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // Replace with your actual Cloudinary details
    private final String cloudName = "campuslost_unsigned";
    private final String uploadPreset = "campuslost_unsigned";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        // Connect XML views
        etTitle = findViewById(R.id.etItemTitle);
        etDescription = findViewById(R.id.etItemDescription);
        etLocation = findViewById(R.id.etItemLocation);
        etType = findViewById(R.id.etItemType);

        btnSaveItem = findViewById(R.id.btnSaveItem);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);

        // Firebase setup
        auth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase
                .getInstance("https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        // Open Gallery
        btnChooseImage.setOnClickListener(v -> openGallery());

        // Save item
        btnSaveItem.setOnClickListener(v -> saveItem());
    }

    // Open phone Gallery
    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(
                Intent.createChooser(intent, "Select Item Image"),
                PICK_IMAGE_REQUEST
        );
    }

    // Receive selected image
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            selectedImageUri = data.getData();

            // Show image preview
            ivSelectedImage.setImageURI(selectedImageUri);
        }
    }

    // Validate and save item
    private void saveItem() {

        String title = etTitle.getText().toString().trim();
        String description =
                etDescription.getText().toString().trim();
        String location =
                etLocation.getText().toString().trim();
        String type = etType.getText().toString().trim();

        // Validate title
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        // Validate description
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        // Validate location
        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        // Validate item type
        if (!type.equalsIgnoreCase("Lost")
                && !type.equalsIgnoreCase("Found")) {

            etType.setError("Enter Lost or Found");
            etType.requestFocus();
            return;
        }

        // Check logged-in user
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable buttons during saving
        btnSaveItem.setEnabled(false);
        btnChooseImage.setEnabled(false);

        // If image is selected, upload it first
        if (selectedImageUri != null) {

            uploadImageToCloudinary(
                    selectedImageUri,
                    new UploadCallback() {

                        @Override
                        public void onSuccess(String imageUrl) {

                            runOnUiThread(() -> {

                                saveItemToFirebase(
                                        title,
                                        description,
                                        location,
                                        type,
                                        imageUrl
                                );
                            });
                        }

                        @Override
                        public void onFailure(String error) {

                            runOnUiThread(() -> {

                                btnSaveItem.setEnabled(true);
                                btnChooseImage.setEnabled(true);

                                Toast.makeText(AddItemActivity.this,
                                        "Image upload failed: " + error,
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    });

        } else {

            // Save without image
            saveItemToFirebase(
                    title,
                    description,
                    location,
                    type,
                    ""
            );
        }
    }

    // Save item details and image URL to Firebase
    private void saveItemToFirebase(
            String title,
            String description,
            String location,
            String type,
            String imageUrl) {

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            btnSaveItem.setEnabled(true);
            btnChooseImage.setEnabled(true);
            Toast.makeText(this,
                    "Please login again",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Generate item ID
        String itemId = databaseReference.push().getKey();

        if (itemId == null) {

            btnSaveItem.setEnabled(true);
            btnChooseImage.setEnabled(true);

            Toast.makeText(this,
                    "Unable to create item ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        // Create LostItem object
        LostItem item = new LostItem(
                itemId,
                title,
                description,
                location,
                type,
                userId,
                timestamp,
                imageUrl
        );

        // Save to Firebase
        databaseReference.child(itemId)
                .setValue(item)
                .addOnCompleteListener(task -> {

                    btnSaveItem.setEnabled(true);
                    btnChooseImage.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Item saved successfully!",
                                Toast.LENGTH_SHORT).show();

                        finish();

                    } else {

                        Exception error = task.getException();

                        String errorMessage = error != null
                                ? error.getMessage()
                                : "Unknown error";

                        Toast.makeText(this,
                                "Firebase save failed: "
                                        + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Upload callback
    private interface UploadCallback {

        void onSuccess(String imageUrl);

        void onFailure(String error);
    }

    // Upload image to Cloudinary
    private void uploadImageToCloudinary(
            Uri imageUri,
            UploadCallback callback) {

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String boundary =
                        "----CampusLost" + UUID.randomUUID();

                URL url = new URL(
                        "https://api.cloudinary.com/v1_1/"
                                + cloudName + "/image/upload"
                );

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);

                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                DataOutputStream output =
                        new DataOutputStream(
                                connection.getOutputStream()
                        );

                // Send upload preset
                writeTextField(
                        output,
                        boundary,
                        "upload_preset",
                        uploadPreset
                );

                // Get image MIME type
                String mimeType =
                        getContentResolver().getType(imageUri);

                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }

                // Send image file
                output.writeBytes("--" + boundary + "\r\n");

                output.writeBytes(
                        "Content-Disposition: form-data; "
                                + "name=\"file\"; "
                                + "filename=\"campuslost_image\"\r\n"
                );

                output.writeBytes(
                        "Content-Type: " + mimeType + "\r\n\r\n"
                );

                InputStream input =
                        getContentResolver()
                                .openInputStream(imageUri);

                if (input == null) {
                    throw new Exception(
                            "Cannot read selected image"
                    );
                }

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                input.close();

                output.writeBytes("\r\n");
                output.writeBytes("--" + boundary + "--\r\n");

                output.flush();
                output.close();

                // Read Cloudinary response
                int responseCode = connection.getResponseCode();

                InputStream responseStream;

                if (responseCode >= 200 && responseCode < 300) {
                    responseStream = connection.getInputStream();
                } else {
                    responseStream = connection.getErrorStream();
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(responseStream)
                        );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode >= 200 && responseCode < 300) {

                    JSONObject json =
                            new JSONObject(response.toString());

                    String secureUrl =
                            json.getString("secure_url");

                    callback.onSuccess(secureUrl);

                } else {

                    JSONObject json =
                            new JSONObject(response.toString());

                    String errorMessage = "HTTP " + responseCode;

                    if (json.has("error")) {
                        errorMessage = json
                                .getJSONObject("error")
                                .optString("message", errorMessage);
                    }

                    callback.onFailure(errorMessage);
                }

            } catch (Exception e) {

                callback.onFailure(
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Unknown upload error"
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Helper method for sending text fields
    private void writeTextField(
            DataOutputStream output,
            String boundary,
            String name,
            String value) throws Exception {

        output.writeBytes("--" + boundary + "\r\n");

        output.writeBytes(
                "Content-Disposition: form-data; "
                        + "name=\"" + name + "\"\r\n\r\n"
        );

        output.writeBytes(value + "\r\n");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
