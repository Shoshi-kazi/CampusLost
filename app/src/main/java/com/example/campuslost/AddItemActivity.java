
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddItemActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;

    EditText etTitle, etDescription, etLocation, etType, etContactNumber;
    Button btnSaveItem, btnChooseImage, btnTakePhoto;
    ImageView ivSelectedImage;

    DatabaseReference databaseReference;
    FirebaseAuth auth;

    Uri selectedImageUri;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final String cloudName = "kns9m6z7";
    private final String uploadPreset = "campuslost_unsigned";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        etTitle = findViewById(R.id.etItemTitle);
        etDescription = findViewById(R.id.etItemDescription);
        etLocation = findViewById(R.id.etItemLocation);
        etType = findViewById(R.id.etItemType);
        etContactNumber = findViewById(R.id.etContactNumber);

        btnSaveItem = findViewById(R.id.btnSaveItem);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);

        auth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase
                .getInstance("https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        btnChooseImage.setOnClickListener(v -> openGallery());
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnSaveItem.setOnClickListener(v -> saveItem());
    }

    // Open Gallery
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(
                Intent.createChooser(intent, "Select Item Image"),
                PICK_IMAGE_REQUEST
        );
    }

    // Open phone Camera
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this,
                    "No camera app found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Receive image from Gallery or Camera
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == PICK_IMAGE_REQUEST && data.getData() != null) {

            selectedImageUri = data.getData();
            ivSelectedImage.setImageURI(selectedImageUri);

        } else if (requestCode == CAMERA_REQUEST) {

            Bundle extras = data.getExtras();

            if (extras != null) {
                Bitmap photo = (Bitmap) extras.get("data");

                if (photo != null) {
                    try {
                        File imageFile = new File(
                                getCacheDir(),
                                "campuslost_camera_" + System.currentTimeMillis() + ".jpg"
                        );

                        FileOutputStream outputStream =
                                new FileOutputStream(imageFile);

                        photo.compress(
                                Bitmap.CompressFormat.JPEG,
                                90,
                                outputStream
                        );

                        outputStream.flush();
                        outputStream.close();

                        selectedImageUri = Uri.fromFile(imageFile);
                        ivSelectedImage.setImageBitmap(photo);

                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Could not save camera photo",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // Validate and save item
    private void saveItem() {

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String type = etType.getText().toString().trim();
        String contactNumber =
                etContactNumber.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        if (!type.equalsIgnoreCase("Lost")
                && !type.equalsIgnoreCase("Found")) {
            etType.setError("Enter Lost or Found");
            etType.requestFocus();
            return;
        }

        if (contactNumber.isEmpty()) {
            etContactNumber.setError("Contact number is required");
            etContactNumber.requestFocus();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveItem.setEnabled(false);
        btnChooseImage.setEnabled(false);
        btnTakePhoto.setEnabled(false);

        if (selectedImageUri != null) {

            uploadImageToCloudinary(
                    selectedImageUri,
                    new UploadCallback() {

                        @Override
                        public void onSuccess(String imageUrl) {
                            runOnUiThread(() ->
                                    saveItemToFirebase(
                                            title,
                                            description,
                                            location,
                                            type,
                                            imageUrl,
                                            contactNumber
                                    )
                            );
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> {
                                enableButtons();

                                Toast.makeText(
                                        AddItemActivity.this,
                                        "Image upload failed: " + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
            );

        } else {
            saveItemToFirebase(
                    title,
                    description,
                    location,
                    type,
                    "",
                    contactNumber
            );
        }
    }

    // Enable buttons after saving or an error
    private void enableButtons() {
        btnSaveItem.setEnabled(true);
        btnChooseImage.setEnabled(true);
        btnTakePhoto.setEnabled(true);
    }

    // Save item details to Firebase
    private void saveItemToFirebase(
            String title,
            String description,
            String location,
            String type,
            String imageUrl,
            String contactNumber) {

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            enableButtons();
            Toast.makeText(this,
                    "Please login again",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        DatabaseReference userReference = FirebaseDatabase
                .getInstance("https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(userId);

        userReference.child("name").get()
                .addOnSuccessListener(snapshot -> {

                    String posterName = snapshot.getValue(String.class);

                    if (posterName == null || posterName.trim().isEmpty()) {
                        posterName = "CampusLost User";
                    }

                    String itemId = databaseReference.push().getKey();

                    if (itemId == null) {
                        enableButtons();
                        Toast.makeText(this,
                                "Unable to create item ID",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long timestamp = System.currentTimeMillis();

                    LostItem item = new LostItem(
                            itemId,
                            title,
                            description,
                            location,
                            type,
                            userId,
                            timestamp,
                            imageUrl,
                            posterName,
                            contactNumber
                    );

                    databaseReference.child(itemId)
                            .setValue(item)
                            .addOnCompleteListener(task -> {

                                enableButtons();

                                if (task.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Item saved successfully!",
                                            Toast.LENGTH_SHORT).show();
                                    finish();

                                } else {
                                    String errorMessage =
                                            task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "Unknown error";

                                    Toast.makeText(this,
                                            "Firebase save failed: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(error -> {
                    enableButtons();

                    Toast.makeText(this,
                            "Could not load your profile: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

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
                String boundary = "----CampusLost" + UUID.randomUUID();

                URL url = new URL(
                        "https://api.cloudinary.com/v1_1/"
                                + cloudName + "/image/upload"
                );

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);

                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                DataOutputStream output =
                        new DataOutputStream(connection.getOutputStream());

                writeTextField(
                        output,
                        boundary,
                        "upload_preset",
                        uploadPreset
                );

                String mimeType = getContentResolver().getType(imageUri);

                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }

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
                        getContentResolver().openInputStream(imageUri);

                if (input == null) {
                    throw new Exception("Cannot read selected image");
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

                int responseCode = connection.getResponseCode();

                InputStream responseStream;

                if (responseCode >= 200 && responseCode < 300) {
                    responseStream = connection.getInputStream();
                } else {
                    responseStream = connection.getErrorStream();
                }

                if (responseStream == null) {
                    throw new Exception("Empty response from Cloudinary");
                }

                BufferedReader reader = new BufferedReader(
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

                    callback.onSuccess(json.getString("secure_url"));

                } else {

                    JSONObject json =
                            new JSONObject(response.toString());

                    String errorMessage = "HTTP " + responseCode;

                    if (json.has("error")) {
                        errorMessage = json.getJSONObject("error")
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
