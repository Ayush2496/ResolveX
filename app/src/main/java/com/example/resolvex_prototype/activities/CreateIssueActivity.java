package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.receivers.IssueStatusReceiver;
import com.example.resolvex_prototype.utils.DropdownHelper;
import com.example.resolvex_prototype.utils.ImageUtils;
import com.example.resolvex_prototype.utils.IssueOptions;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.example.resolvex_prototype.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class CreateIssueActivity extends AppCompatActivity {

    private static final String STATE_IMAGE_PATH = "selected_image_path";
    private static final String STATE_CAMERA_PATH = "pending_camera_path";

    private TextInputEditText etTitle, etLocation, etDescription;
    private MaterialAutoCompleteTextView actCategory, actBuilding;
    private RadioGroup rgPriority;
    private MaterialButton btnTakePhoto, btnPickGallery, btnSubmitIssue;
    private ImageView ivImagePreview;
    private CheckBox cbConfirm;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    /** The photo that will be saved with the issue ("" when there is none). */
    private String selectedImagePath = "";
    /** The file the camera app is currently writing into. */
    private String pendingCameraPath = "";

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"STUDENT".equalsIgnoreCase(sessionManager.getRole())) {
            startActivity(new Intent(CreateIssueActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_create_issue);

        etTitle = findViewById(R.id.etTitle);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        actCategory = findViewById(R.id.actCategory);
        actBuilding = findViewById(R.id.actBuilding);
        rgPriority = findViewById(R.id.rgPriority);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnSubmitIssue = findViewById(R.id.btnSubmitIssue);
        ivImagePreview = findViewById(R.id.ivImagePreview);
        cbConfirm = findViewById(R.id.cbConfirm);

        ((TextView) findViewById(R.id.tvTopTitle)).setText("Report an issue");
        findViewById(R.id.tvTopSub).setVisibility(View.GONE);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        UiUtils.applySystemBarSpace(this);

        DropdownHelper.setup(this, actCategory, IssueOptions.CATEGORIES, -1, null);
        DropdownHelper.setup(this, actBuilding, IssueOptions.BUILDINGS, -1, null);
        setupImageLaunchers();

        // The screen is rebuilt when the phone rotates; get the photo back
        if (savedInstanceState != null) {
            pendingCameraPath = savedInstanceState.getString(STATE_CAMERA_PATH, "");
            selectedImagePath = savedInstanceState.getString(STATE_IMAGE_PATH, "");
            if (!selectedImagePath.isEmpty()) {
                ImageUtils.loadImageToView(selectedImagePath, ivImagePreview);
            }
        }

        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnPickGallery.setOnClickListener(v -> pickGallery());
        btnSubmitIssue.setOnClickListener(v -> handleSubmitIssue());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE_PATH, selectedImagePath);
        outState.putString(STATE_CAMERA_PATH, pendingCameraPath);
    }

    private void setupImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    File photo = new File(pendingCameraPath);
                    if (result.getResultCode() == RESULT_OK && photo.exists() && photo.length() > 0) {
                        selectedImagePath = pendingCameraPath;
                        ImageUtils.loadImageToView(selectedImagePath, ivImagePreview);
                    } else if (photo.exists() && photo.length() == 0) {
                        photo.delete(); // the user cancelled, remove the empty file
                    }
                    pendingCameraPath = "";
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        if (selectedUri != null) {
                            String savedPath = ImageUtils.saveImageToInternalStorage(this, selectedUri);
                            if (savedPath.isEmpty()) {
                                Toast.makeText(this, "Could not read that image", Toast.LENGTH_SHORT).show();
                            } else {
                                selectedImagePath = savedPath;
                                ImageUtils.loadImageToView(selectedImagePath, ivImagePreview);
                            }
                        }
                    }
                }
        );
    }

    /**
     * The phone's camera app takes the picture and saves it into a file we provide, so the photo
     * is full size. The camera app needs no CAMERA permission from us because it is a separate app.
     */
    private void takePhoto() {
        try {
            File photoFile = ImageUtils.createImageFile(this);
            if (photoFile == null) {
                Toast.makeText(this, "Could not prepare a file for the photo", Toast.LENGTH_SHORT).show();
                return;
            }

            pendingCameraPath = photoFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No camera available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void pickGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No gallery app available", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isOneOf(String value, String[] options) {
        for (String option : options) {
            if (option.equals(value)) return true;
        }
        return false;
    }

    private void handleSubmitIssue() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String room = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String building = actBuilding.getText() != null ? actBuilding.getText().toString().trim() : "";
        String category = actCategory.getText() != null ? actCategory.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (!ValidationUtils.isNotEmpty(title)) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (!isOneOf(category, IssueOptions.CATEGORIES)) {
            actCategory.setError("Please select a category");
            actCategory.requestFocus();
            return;
        }

        if (!isOneOf(building, IssueOptions.BUILDINGS)) {
            actBuilding.setError("Please select the campus area");
            actBuilding.requestFocus();
            return;
        }

        if (description.length() < 10) {
            etDescription.setError("Please describe the problem (at least 10 characters)");
            etDescription.requestFocus();
            return;
        }

        if (!cbConfirm.isChecked()) {
            Toast.makeText(this, "Please confirm the report details", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = "Medium";
        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        if (selectedPriorityId == R.id.rbLow) {
            priority = "Low";
        } else if (selectedPriorityId == R.id.rbHigh) {
            priority = "High";
        }

        // "Block A" + "room 204" is saved as "Block A - Room 204"
        String location = IssueOptions.buildLocation(building, room);

        Issue issue = new Issue();
        issue.setUserId(sessionManager.getUserId());
        issue.setTitle(title);
        issue.setLocation(location);
        issue.setCategory(category);
        issue.setPriority(priority);
        issue.setDescription(description);
        issue.setImagePath(selectedImagePath);

        long issueId = dbHelper.insertIssue(issue);
        if (issueId != -1) {
            Toast.makeText(this, "Issue reported successfully!", Toast.LENGTH_LONG).show();

            Intent broadcastIntent = new Intent(IssueStatusReceiver.ACTION_STATUS_CHANGED);
            broadcastIntent.setClass(this, IssueStatusReceiver.class);
            broadcastIntent.putExtra(IssueStatusReceiver.EXTRA_ISSUE_ID, (int) issueId);
            broadcastIntent.putExtra(IssueStatusReceiver.EXTRA_TITLE, "New Issue Submitted");
            broadcastIntent.putExtra(IssueStatusReceiver.EXTRA_MESSAGE,
                    "Issue " + IssueStatusManager.formatIssueId((int) issueId) + " reported successfully.");
            sendBroadcast(broadcastIntent);

            finish();
        } else {
            Toast.makeText(this, "Failed to submit issue. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
