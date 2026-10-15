package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.adapters.HistoryAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.models.IssueHistory;
import com.example.resolvex_prototype.models.IssueVerification;
import com.example.resolvex_prototype.utils.ImageUtils;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IssueDetailsActivity extends AppCompatActivity {

    private static final String STATE_RESOLUTION_PHOTO = "resolution_photo_path";

    private LinearLayout llProgress;
    private TextView tvDetailTitle, tvDetailCategory, tvDetailLocation, tvDetailReporter, tvDetailDescription;
    private TextView tvStaffName, tvResolutionNotes, tvReopenReason, tvReopenComment, tvVerifyTitle, tvVerifyQuestion;
    private TextView chipPriority, chipStatus;
    private ImageView ivIssueImage, ivResolutionPreview, ivResolutionImageDisplay;

    private MaterialButton btnAssignStaff, btnStartWork, btnResolutionPhoto, btnMarkResolved;
    private MaterialButton btnVerifyYes, btnVerifyNo, btnSubmitReopen, btnSubmitFeedback;
    private View llPriorityPicker, llFeedbackForm;
    private TextView chipPrioLow, chipPrioMedium, chipPrioHigh;
    private RatingBar ratingBar;
    private TextInputEditText etFeedbackComment;
    private View cardAssignedStaff, cardStaffActions, cardResolutionDetails, cardVerification, cardReopenDetails;
    private LinearLayout llResolutionForm, llReopenForm, llVerificationButtons;
    private TextInputEditText etResolutionNotes, etReopenComment;
    private RadioGroup rgReopenReason;
    private RecyclerView rvHistory;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private int issueId = 0;
    private Issue currentIssue;
    private String resolutionImagePath = "";

    private ActivityResultLauncher<Intent> resolutionCameraLauncher;
    private ActivityResultLauncher<Intent> assignLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(IssueDetailsActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_issue_details);

        issueId = getIntent().getIntExtra("issue_id", 0);

        llProgress = findViewById(R.id.llProgress);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailReporter = findViewById(R.id.tvDetailReporter);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvStaffName = findViewById(R.id.tvStaffName);
        tvResolutionNotes = findViewById(R.id.tvResolutionNotes);
        tvReopenReason = findViewById(R.id.tvReopenReason);
        tvReopenComment = findViewById(R.id.tvReopenComment);
        tvVerifyTitle = findViewById(R.id.tvVerifyTitle);
        tvVerifyQuestion = findViewById(R.id.tvVerifyQuestion);

        chipPriority = findViewById(R.id.chipPriority);
        chipStatus = findViewById(R.id.chipStatus);

        ivIssueImage = findViewById(R.id.ivIssueImage);
        ivResolutionPreview = findViewById(R.id.ivResolutionPreview);
        ivResolutionImageDisplay = findViewById(R.id.ivResolutionImageDisplay);

        btnAssignStaff = findViewById(R.id.btnAssignStaff);
        llPriorityPicker = findViewById(R.id.llPriorityPicker);
        chipPrioLow = findViewById(R.id.chipPrioLow);
        chipPrioMedium = findViewById(R.id.chipPrioMedium);
        chipPrioHigh = findViewById(R.id.chipPrioHigh);
        btnStartWork = findViewById(R.id.btnStartWork);
        btnResolutionPhoto = findViewById(R.id.btnResolutionPhoto);
        btnMarkResolved = findViewById(R.id.btnMarkResolved);
        btnVerifyYes = findViewById(R.id.btnVerifyYes);
        btnVerifyNo = findViewById(R.id.btnVerifyNo);
        btnSubmitReopen = findViewById(R.id.btnSubmitReopen);
        llFeedbackForm = findViewById(R.id.llFeedbackForm);
        ratingBar = findViewById(R.id.ratingBar);
        etFeedbackComment = findViewById(R.id.etFeedbackComment);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);

        cardAssignedStaff = findViewById(R.id.cardAssignedStaff);
        cardStaffActions = findViewById(R.id.cardStaffActions);
        cardResolutionDetails = findViewById(R.id.cardResolutionDetails);
        cardVerification = findViewById(R.id.cardVerification);
        cardReopenDetails = findViewById(R.id.cardReopenDetails);

        llResolutionForm = findViewById(R.id.llResolutionForm);
        llReopenForm = findViewById(R.id.llReopenForm);
        llVerificationButtons = findViewById(R.id.llVerificationButtons);

        etResolutionNotes = findViewById(R.id.etResolutionNotes);
        etReopenComment = findViewById(R.id.etReopenComment);
        rgReopenReason = findViewById(R.id.rgReopenReason);
        rvHistory = findViewById(R.id.rvHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTopTitle)).setText("Issue detail");
        UiUtils.applySystemBarSpace(this);

        // The screen is rebuilt when the phone rotates; keep the resolution photo the staff member just took
        if (savedInstanceState != null) {
            resolutionImagePath = savedInstanceState.getString(STATE_RESOLUTION_PHOTO, "");
        }

        setupResolutionCameraLauncher();
        // the assign screen reports back so this screen refreshes
        assignLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadIssueDetails();
                    }
                });
        loadIssueDetails();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_RESOLUTION_PHOTO, resolutionImagePath);
    }

    private void setupResolutionCameraLauncher() {
        resolutionCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    File photo = new File(resolutionImagePath);
                    if (result.getResultCode() == RESULT_OK && photo.exists() && photo.length() > 0) {
                        ImageUtils.loadImageToView(resolutionImagePath, ivResolutionPreview);
                    } else {
                        // cancelled: do not keep a path to a photo that does not exist
                        if (photo.exists()) {
                            photo.delete();
                        }
                        resolutionImagePath = "";
                        ivResolutionPreview.setVisibility(View.GONE);
                    }
                }
        );
    }

    private boolean hasResolutionPhoto() {
        if (resolutionImagePath.isEmpty()) return false;
        File photo = new File(resolutionImagePath);
        return photo.exists() && photo.length() > 0;
    }

    private void loadIssueDetails() {
        currentIssue = dbHelper.getIssueById(issueId);

        if (currentIssue == null) {
            Toast.makeText(this, "Issue not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Privacy checks: a student can only open their own issues, staff only the issues assigned to them
        String userRole = sessionManager.getRole();
        int currentUserId = sessionManager.getUserId();
        if ("STUDENT".equalsIgnoreCase(userRole) && currentIssue.getUserId() != currentUserId) {
            Toast.makeText(this, "Unauthorized access to this issue.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if ("STAFF".equalsIgnoreCase(userRole) && currentIssue.getAssignedStaffId() != currentUserId) {
            Toast.makeText(this, "This issue is not assigned to you.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTopSub)).setText(
                IssueStatusManager.formatIssueId(currentIssue.getIssueId()) + " \u00B7 " + currentIssue.getCategory());
        tvDetailTitle.setText(currentIssue.getTitle());
        tvDetailCategory.setText(currentIssue.getCategory());
        tvDetailLocation.setText("Location: " + currentIssue.getLocation());
        tvDetailReporter.setText("Reported by: " + currentIssue.getStudentName() + " on " + currentIssue.getCreatedAt());
        tvDetailDescription.setText(currentIssue.getDescription());

        String statusKey = currentIssue.getStatus();
        UiUtils.bindPriority(chipPriority, currentIssue.getPriority());
        UiUtils.bindStatus(chipStatus, statusKey);
        UiUtils.buildProgressBar(llProgress, statusKey);

        ImageUtils.loadImageToView(currentIssue.getImagePath(), ivIssueImage);

        // Assigned Staff Name
        if (currentIssue.getAssignedStaffId() > 0 && currentIssue.getAssignedStaffName() != null) {
            tvStaffName.setText(currentIssue.getAssignedStaffName());
        } else {
            tvStaffName.setText("Unassigned");
        }

        // The admin can tap the reporter or the assigned staff member to see their details
        boolean isAdminViewer = "ADMIN".equalsIgnoreCase(userRole);
        makePersonLink(tvDetailReporter, isAdminViewer, currentIssue.getUserId());
        makePersonLink(tvStaffName, isAdminViewer && currentIssue.getAssignedStaffId() > 0, currentIssue.getAssignedStaffId());

        // The student's reopen reason is useful while staff are working on it again, not after a new resolution
        IssueVerification latestVerification = dbHelper.getLatestVerificationForIssue(issueId);
        boolean reworkInProgress = IssueStatusManager.REOPENED.equals(statusKey) || IssueStatusManager.IN_PROGRESS.equals(statusKey);
        if (latestVerification != null && "REOPENED".equalsIgnoreCase(latestVerification.getResult()) && reworkInProgress) {
            tvReopenReason.setText("Reason: " + (latestVerification.getReason() != null ? latestVerification.getReason() : "Reopened by student"));
            tvReopenComment.setText("Comment: " + (latestVerification.getComment() != null && !latestVerification.getComment().isEmpty() ? latestVerification.getComment() : "No additional details provided."));
            cardReopenDetails.setVisibility(View.VISIBLE);
        } else {
            cardReopenDetails.setVisibility(View.GONE);
        }

        setupRoleBasedActions();
        loadTimelineHistory();
    }

    /** Turns a text line into a link (orange) that opens the person's details, or back into plain text. */
    private void makePersonLink(TextView view, boolean enabled, int personId) {
        if (enabled) {
            view.setTextColor(getColor(R.color.rx_accent));
            view.setOnClickListener(v -> {
                Intent intent = new Intent(IssueDetailsActivity.this, UserDetailActivity.class);
                intent.putExtra(UserDetailActivity.EXTRA_USER_ID, personId);
                startActivity(intent);
            });
        } else {
            view.setOnClickListener(null);
            view.setClickable(false);
        }
    }

    private void setupRoleBasedActions() {
        String userRole = sessionManager.getRole();
        int currentUserId = sessionManager.getUserId();
        String status = currentIssue.getStatus();

        btnAssignStaff.setVisibility(View.GONE);
        llPriorityPicker.setVisibility(View.GONE);
        llFeedbackForm.setVisibility(View.GONE);
        cardStaffActions.setVisibility(View.GONE);
        llResolutionForm.setVisibility(View.GONE);
        cardVerification.setVisibility(View.GONE);
        cardResolutionDetails.setVisibility(View.GONE);

        // 1. ADMIN ACTIONS
        if ("ADMIN".equals(userRole)) {
            boolean canAssign = IssueStatusManager.SUBMITTED.equals(status);
            boolean canReassign = currentIssue.getAssignedStaffId() > 0 && (IssueStatusManager.ASSIGNED.equals(status)
                    || IssueStatusManager.IN_PROGRESS.equals(status) || IssueStatusManager.REOPENED.equals(status));
            if (canAssign || canReassign) {
                btnAssignStaff.setText(canAssign ? "Assign staff" : "Reassign staff");
                btnAssignStaff.setVisibility(View.VISIBLE);
                btnAssignStaff.setOnClickListener(v -> {
                    Intent intent = new Intent(IssueDetailsActivity.this, AssignStaffActivity.class);
                    intent.putExtra(AssignStaffActivity.EXTRA_ISSUE_ID, issueId);
                    assignLauncher.launch(intent);
                });
            }

            if (!IssueStatusManager.CLOSED.equals(status)) {
                llPriorityPicker.setVisibility(View.VISIBLE);
                bindPriorityChip(chipPrioLow, "Low");
                bindPriorityChip(chipPrioMedium, "Medium");
                bindPriorityChip(chipPrioHigh, "High");
            }
        }

        // 2. STAFF ACTIONS
        if ("STAFF".equals(userRole)) {
            if (currentIssue.getAssignedStaffId() == currentUserId) {
                cardStaffActions.setVisibility(View.VISIBLE);

                if (IssueStatusManager.ASSIGNED.equals(status) || IssueStatusManager.REOPENED.equals(status)) {
                    btnStartWork.setVisibility(View.VISIBLE);
                    llResolutionForm.setVisibility(View.GONE);

                    btnStartWork.setOnClickListener(v -> {
                        boolean updated = dbHelper.updateIssueStatus(issueId, IssueStatusManager.IN_PROGRESS, currentUserId, "Staff started work on issue");
                        if (updated) {
                            Toast.makeText(this, "Status updated to In Progress", Toast.LENGTH_SHORT).show();
                            loadIssueDetails();
                        } else {
                            Toast.makeText(this, "Invalid transition", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if (IssueStatusManager.IN_PROGRESS.equals(status)) {
                    btnStartWork.setVisibility(View.GONE);
                    llResolutionForm.setVisibility(View.VISIBLE);

                    if (hasResolutionPhoto()) {
                        ImageUtils.loadImageToView(resolutionImagePath, ivResolutionPreview);
                    }

                    btnResolutionPhoto.setOnClickListener(v -> takeResolutionPhoto());

                    btnMarkResolved.setOnClickListener(v -> {
                        String notes = etResolutionNotes.getText() != null ? etResolutionNotes.getText().toString().trim() : "";
                        if (notes.isEmpty()) {
                            etResolutionNotes.setError("Please describe resolution actions taken");
                            return;
                        }

                        String photoToSave = hasResolutionPhoto() ? resolutionImagePath : "";
                        boolean success = dbHelper.addResolution(issueId, notes, photoToSave, currentUserId);
                        if (success) {
                            resolutionImagePath = "";
                            ivResolutionPreview.setVisibility(View.GONE);
                            Toast.makeText(this, "Resolution submitted. Awaiting student verification.", Toast.LENGTH_LONG).show();
                            loadIssueDetails();
                        } else {
                            Toast.makeText(this, "Failed to mark as resolved", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        // 3. STUDENT ACTIONS (Verification & Feedback)
        if ("STUDENT".equals(userRole) && currentIssue.getUserId() == currentUserId) {
            if (IssueStatusManager.RESOLVED.equals(status)) {
                tvVerifyTitle.setText("Staff marked this as resolved");
                tvVerifyQuestion.setText("Was the issue actually fixed?");
                cardVerification.setVisibility(View.VISIBLE);
                llVerificationButtons.setVisibility(View.VISIBLE);
                llReopenForm.setVisibility(View.GONE);

                btnVerifyYes.setOnClickListener(v -> {
                    boolean success = dbHelper.verifyIssue(issueId, currentUserId);
                    if (success) {
                        Toast.makeText(this, "Resolution verified! Please provide quick feedback to close.", Toast.LENGTH_LONG).show();
                        // The screen now shows the feedback form for this verified issue
                        loadIssueDetails();
                    } else {
                        Toast.makeText(this, "Verification error", Toast.LENGTH_SHORT).show();
                    }
                });

                btnVerifyNo.setOnClickListener(v -> {
                    llVerificationButtons.setVisibility(View.GONE);
                    llReopenForm.setVisibility(View.VISIBLE);
                });

                btnSubmitReopen.setOnClickListener(v -> {
                    int selectedId = rgReopenReason.getCheckedRadioButtonId();
                    RadioButton rb = findViewById(selectedId);
                    String reason = rb != null ? rb.getText().toString() : "Unresolved";
                    String comment = etReopenComment.getText() != null ? etReopenComment.getText().toString().trim() : "";

                    boolean success = dbHelper.reopenIssue(issueId, currentUserId, reason, comment);
                    if (success) {
                        Toast.makeText(this, "Issue reopened. Assigned staff notified.", Toast.LENGTH_LONG).show();
                        loadIssueDetails();
                    } else {
                        Toast.makeText(this, "Failed to reopen issue", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (IssueStatusManager.VERIFIED.equals(status)) {
                // Verified, but the feedback step was not finished: offer it again so the issue can be closed
                tvVerifyTitle.setText("Almost done: your feedback closes this issue");
                tvVerifyQuestion.setText("How did we do?");
                cardVerification.setVisibility(View.VISIBLE);
                llVerificationButtons.setVisibility(View.GONE);
                llReopenForm.setVisibility(View.GONE);
                llFeedbackForm.setVisibility(View.VISIBLE);
                btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
            }
        }

        // Display Resolution Details Output card if resolution exists
        if (currentIssue.getResolutionNotes() != null && !currentIssue.getResolutionNotes().isEmpty()) {
            tvResolutionNotes.setText(currentIssue.getResolutionNotes());
            ImageUtils.loadImageToView(currentIssue.getResolutionImagePath(), ivResolutionImageDisplay);
            cardResolutionDetails.setVisibility(View.VISIBLE);
        }
    }

    /** The camera app saves a full-size photo into this file (through the FileProvider). */
    private void takeResolutionPhoto() {
        try {
            File photoFile = ImageUtils.createImageFile(this);
            if (photoFile == null) {
                Toast.makeText(this, "Could not prepare a file for the photo", Toast.LENGTH_SHORT).show();
                return;
            }

            resolutionImagePath = photoFile.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            resolutionCameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            resolutionImagePath = "";
            Toast.makeText(this, "No camera available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** One of the Low / Medium / High chips: selected when it is the current priority, tap to change it. */
    private void bindPriorityChip(TextView chip, String priority) {
        chip.setSelected(priority.equalsIgnoreCase(currentIssue.getPriority()));
        chip.setOnClickListener(v -> {
            if (priority.equalsIgnoreCase(currentIssue.getPriority())) return;

            boolean changed = dbHelper.updateIssuePriority(issueId, priority, sessionManager.getUserId());
            if (changed) {
                Toast.makeText(this, "Priority changed to " + priority, Toast.LENGTH_SHORT).show();
                loadIssueDetails();
            } else {
                Toast.makeText(this, "Could not change the priority", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Sending the feedback closes the issue (it must be verified first). */
    private void submitFeedback() {
        int rating = Math.round(ratingBar.getRating());
        if (rating < 1) {
            Toast.makeText(this, "Please choose a star rating", Toast.LENGTH_SHORT).show();
            return;
        }
        String comment = etFeedbackComment.getText() != null ? etFeedbackComment.getText().toString().trim() : "";

        boolean closed = dbHelper.closeIssueAfterFeedback(issueId, sessionManager.getUserId(), rating, comment);
        if (closed) {
            Toast.makeText(this, "Thank you! Issue closed successfully.", Toast.LENGTH_SHORT).show();
            loadIssueDetails();
        } else {
            Toast.makeText(this, "Error closing issue", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTimelineHistory() {
        // newest first, like the timeline in the design
        List<IssueHistory> historyList = new ArrayList<>(dbHelper.getIssueHistory(issueId));
        Collections.reverse(historyList);
        if (!historyList.isEmpty()) {
            HistoryAdapter adapter = new HistoryAdapter(this, historyList);
            rvHistory.setAdapter(adapter);
            rvHistory.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.GONE);
        }
    }
}
