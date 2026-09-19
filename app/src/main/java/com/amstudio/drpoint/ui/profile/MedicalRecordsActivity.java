package com.amstudio.drpoint.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.BuildConfig;
import com.amstudio.drpoint.R;
import com.amstudio.drpoint.adapter.MedicalRecordAdapter;
import com.amstudio.drpoint.databinding.ActivityMedicalRecordsBinding;
import com.amstudio.drpoint.databinding.BottomSheetAddMedicalRecordBinding;
import com.amstudio.drpoint.databinding.DialogViewMedicalRecordBinding;
import com.amstudio.drpoint.model.MedicalRecord;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.util.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicalRecordsActivity extends AppCompatActivity {

    private ActivityMedicalRecordsBinding binding;
    private MedicalRecordAdapter adapter;
    private int currentTabPosition = 0;

    private List<MedicalRecord> allFetchedRecords = new ArrayList<>();
    private Uri selectedFileUri = null;
    private String selectedFileType = "image/jpeg";
    private BottomSheetAddMedicalRecordBinding currentAddBinding;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> pdfLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicalRecordsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBack.setOnClickListener(v -> finish());

        setupLaunchers();
        setupRecyclerView();
        setupTabLayout();

        binding.swipeRefresh.setOnRefreshListener(this::loadMedicalRecordsFromSupabase);
        binding.btnAddRecord.setOnClickListener(v -> openAddRecordBottomSheet());

        loadMedicalRecordsFromSupabase();
    }

    private void setupLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedFileUri = result.getData().getData();
                        selectedFileType = getContentResolver().getType(selectedFileUri);
                        if (selectedFileType == null) selectedFileType = "image/jpeg";
                        updateFileSelectedStatus("🖼 Gallery image attached");
                    }
                }
        );

        pdfLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedFileUri = result.getData().getData();
                        selectedFileType = "application/pdf";
                        updateFileSelectedStatus("📄 PDF Document attached");
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (result.getData().getData() != null) {
                            selectedFileUri = result.getData().getData();
                        }
                        selectedFileType = "image/jpeg";
                        updateFileSelectedStatus("📷 Camera image captured");
                    }
                }
        );
    }

    private void updateFileSelectedStatus(String message) {
        if (currentAddBinding != null) {
            currentAddBinding.tvFileSelectedStatus.setText(message);
            currentAddBinding.tvFileSelectedStatus.setTextColor(getResources().getColor(R.color.primary, getTheme()));
        }
    }

    private void setupRecyclerView() {
        binding.rvMedicalRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicalRecordAdapter(new MedicalRecordAdapter.OnRecordActionListener() {
            @Override
            public void onItemClick(MedicalRecord record) {
                openViewRecordDialog(record);
            }

            @Override
            public void onDeleteClick(MedicalRecord record) {
                confirmAndDeleteRecord(record);
            }
        });
        binding.rvMedicalRecords.setAdapter(adapter);
    }

    private void setupTabLayout() {
        binding.tabLayoutRecords.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                filterAndDisplayRecords(currentTabPosition);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadMedicalRecordsFromSupabase() {
        binding.swipeRefresh.setRefreshing(true);
        String userId = PreferenceManager.getInstance(this).getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            binding.swipeRefresh.setRefreshing(false);
            allFetchedRecords = getSampleRecords();
            filterAndDisplayRecords(currentTabPosition);
            return;
        }

        SupabaseClient.getMedicalRecordService().getMedicalRecordsForPatient("eq." + userId)
                .enqueue(new Callback<List<MedicalRecord>>() {
                    @Override
                    public void onResponse(Call<List<MedicalRecord>> call, Response<List<MedicalRecord>> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        List<MedicalRecord> list = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null) {
                            list.addAll(response.body());
                        }
                        if (list.isEmpty()) {
                            list.addAll(getSampleRecords());
                        }
                        allFetchedRecords = list;
                        filterAndDisplayRecords(currentTabPosition);
                    }

                    @Override
                    public void onFailure(Call<List<MedicalRecord>> call, Throwable t) {
                        binding.swipeRefresh.setRefreshing(false);
                        allFetchedRecords = getSampleRecords();
                        filterAndDisplayRecords(currentTabPosition);
                    }
                });
    }

    private void filterAndDisplayRecords(int tabPosition) {
        List<MedicalRecord> filtered = new ArrayList<>();

        for (MedicalRecord rec : allFetchedRecords) {
            if (tabPosition == 0) {
                // All Records
                filtered.add(rec);
            } else if (tabPosition == 1) {
                // Prescriptions
                if (rec.isDoctorGenerated() || rec.getRecordType().toLowerCase().contains("prescription")) {
                    filtered.add(rec);
                }
            } else if (tabPosition == 2) {
                // My Uploads
                if (!rec.isDoctorGenerated()) {
                    filtered.add(rec);
                }
            }
        }

        if (filtered.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvMedicalRecords.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvMedicalRecords.setVisibility(View.VISIBLE);
        }

        adapter.submitList(filtered);
    }

    private void openAddRecordBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        currentAddBinding = BottomSheetAddMedicalRecordBinding.inflate(getLayoutInflater());
        dialog.setContentView(currentAddBinding.getRoot());

        selectedFileUri = null;
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        currentAddBinding.etRecordDate.setText(todayStr);

        currentAddBinding.btnSourceCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        currentAddBinding.btnSourceGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        currentAddBinding.btnSourcePdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfLauncher.launch(Intent.createChooser(intent, "Select PDF Document"));
        });

        currentAddBinding.btnCancelAdd.setOnClickListener(v -> dialog.dismiss());

        currentAddBinding.btnSaveRecord.setOnClickListener(v -> {
            String title = currentAddBinding.etRecordTitle.getText().toString().trim();
            String recordType = currentAddBinding.etRecordType.getText().toString().trim();
            String docName = currentAddBinding.etDoctorName.getText().toString().trim();
            String clinicName = currentAddBinding.etClinicName.getText().toString().trim();
            String recordDate = currentAddBinding.etRecordDate.getText().toString().trim();
            String notes = currentAddBinding.etNotes.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Please enter a title for the record.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(recordType)) {
                recordType = "prescription";
            }

            currentAddBinding.btnSaveRecord.setEnabled(false);
            currentAddBinding.btnSaveRecord.setText("Saving & Uploading...");

            uploadAndSaveRecord(dialog, title, recordType, docName, clinicName, recordDate, notes);
        });

        dialog.show();
    }

    private void uploadAndSaveRecord(
            BottomSheetDialog dialog,
            String title,
            String recordType,
            String doctorName,
            String clinicName,
            String recordDate,
            String notes
    ) {
        String userId = PreferenceManager.getInstance(this).getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            userId = "usr_patient_default";
        }

        String recordId = "rec_" + UUID.randomUUID().toString().substring(0, 8);
        String fileName = (selectedFileType.contains("pdf") ? "record.pdf" : "record.jpg");
        String storagePath = userId + "/" + recordId + "/" + fileName;

        final String finalUserId = userId;
        final String finalRecordType = recordType;

        if (selectedFileUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();

                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(selectedFileType));

                SupabaseClient.getMedicalRecordService().uploadStorageFile(storagePath, selectedFileType, requestBody)
                        .enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                saveRecordMetadata(dialog, finalUserId, recordId, title, finalRecordType, doctorName, clinicName, recordDate, storagePath, notes);
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                saveRecordMetadata(dialog, finalUserId, recordId, title, finalRecordType, doctorName, clinicName, recordDate, storagePath, notes);
                            }
                        });
            } catch (Exception e) {
                saveRecordMetadata(dialog, finalUserId, recordId, title, finalRecordType, doctorName, clinicName, recordDate, storagePath, notes);
            }
        } else {
            saveRecordMetadata(dialog, finalUserId, recordId, title, finalRecordType, doctorName, clinicName, recordDate, storagePath, notes);
        }
    }

    private void saveRecordMetadata(
            BottomSheetDialog dialog,
            String userId,
            String recordId,
            String title,
            String recordType,
            String doctorName,
            String clinicName,
            String recordDate,
            String filePath,
            String notes
    ) {
        MedicalRecord newRecord = new MedicalRecord(
                recordId,
                userId,
                recordType,
                title,
                doctorName,
                clinicName,
                recordDate,
                filePath,
                notes
        );

        SupabaseClient.getMedicalRecordService().createMedicalRecord("return=representation", newRecord)
                .enqueue(new Callback<List<MedicalRecord>>() {
                    @Override
                    public void onResponse(Call<List<MedicalRecord>> call, Response<List<MedicalRecord>> response) {
                        Toast.makeText(MedicalRecordsActivity.this, "Medical Record saved successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadMedicalRecordsFromSupabase();
                    }

                    @Override
                    public void onFailure(Call<List<MedicalRecord>> call, Throwable t) {
                        allFetchedRecords.add(0, newRecord);
                        filterAndDisplayRecords(currentTabPosition);
                        Toast.makeText(MedicalRecordsActivity.this, "Medical Record saved locally.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }

    private void openViewRecordDialog(MedicalRecord record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogViewMedicalRecordBinding viewBinding = DialogViewMedicalRecordBinding.inflate(getLayoutInflater());
        builder.setView(viewBinding.getRoot());

        AlertDialog dialog = builder.create();

        viewBinding.tvViewRecordTitle.setText(record.getTitle());
        viewBinding.tvViewTypeText.setText(record.getUserFriendlyType());
        viewBinding.tvViewDoctorClinic.setText(record.getDoctorName() + " • " + record.getClinicName());
        viewBinding.tvViewRecordDate.setText("Date: " + (record.getRecordDate() != null ? record.getRecordDate() : "Recent"));

        if (record.isDoctorGenerated()) {
            viewBinding.cardDoctorPrescriptionDetails.setVisibility(View.VISIBLE);
            viewBinding.btnDeleteRecord.setVisibility(View.GONE); // Doctor-generated prescriptions cannot be deleted by patient

            viewBinding.tvViewDiagnosis.setText("Diagnosis: " + (record.getDiagnosis() != null ? record.getDiagnosis() : "Clinical Consultation"));
            viewBinding.tvViewMedicines.setText("Medicines: " + (record.getMedicines() != null ? record.getMedicines() : "Prescribed Vitamin & Care Supplements"));
            viewBinding.tvViewAdvice.setText("Advice: " + (record.getAdvice() != null ? record.getAdvice() : "Follow healthy lifestyle & stay hydrated."));
            viewBinding.tvViewFollowUp.setText("Follow-up: " + (record.getFollowUpDate() != null ? record.getFollowUpDate() : "As needed"));
        } else {
            viewBinding.cardDoctorPrescriptionDetails.setVisibility(View.GONE);
            viewBinding.btnDeleteRecord.setVisibility(View.VISIBLE);
        }

        if (record.getNotes() != null && !record.getNotes().trim().isEmpty()) {
            viewBinding.tvViewNotes.setVisibility(View.VISIBLE);
            viewBinding.tvViewNotes.setText("Notes: " + record.getNotes());
        } else {
            viewBinding.tvViewNotes.setVisibility(View.GONE);
        }

        if (record.isPdf()) {
            viewBinding.llPdfPreviewContainer.setVisibility(View.VISIBLE);
            viewBinding.ivRecordImagePreview.setVisibility(View.GONE);
            viewBinding.tvPdfName.setText(record.getTitle() + ".pdf");
            viewBinding.btnOpenPdf.setOnClickListener(v -> Toast.makeText(this, "Opening PDF document...", Toast.LENGTH_SHORT).show());
        } else {
            viewBinding.llPdfPreviewContainer.setVisibility(View.GONE);
            viewBinding.ivRecordImagePreview.setVisibility(View.VISIBLE);

            Object imageSource = (record.getFileUrl() != null && !record.getFileUrl().isEmpty())
                    ? record.getFileUrl()
                    : R.drawable.ic_file;

            Glide.with(this)
                    .load(imageSource)
                    .placeholder(R.drawable.ic_file)
                    .error(R.drawable.ic_file)
                    .into(viewBinding.ivRecordImagePreview);
        }

        // Fetch secure signed URL for private storage attachments
        if (record.getFilePath() != null && !record.getFilePath().trim().isEmpty()) {
            Map<String, Object> signBody = new HashMap<>();
            signBody.put("expiresIn", 3600);

            SupabaseClient.getMedicalRecordService().getSignedUrl(record.getFilePath(), signBody)
                    .enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            String signedUrl = null;
                            if (response.isSuccessful() && response.body() != null) {
                                Object rawPath = response.body().get("signedURL");
                                if (rawPath != null) {
                                    String relative = rawPath.toString();
                                    if (relative.startsWith("/")) {
                                        signedUrl = BuildConfig.SUPABASE_URL + "storage/v1" + relative;
                                    } else {
                                        signedUrl = BuildConfig.SUPABASE_URL + "storage/v1/" + relative;
                                    }
                                }
                            }

                            if (signedUrl == null) {
                                signedUrl = BuildConfig.SUPABASE_URL + "storage/v1/object/authenticated/medical-records/" + record.getFilePath();
                            }

                            final String finalUrl = signedUrl;

                            if (record.isPdf()) {
                                viewBinding.btnOpenPdf.setOnClickListener(v -> {
                                    try {
                                        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
                                        pdfIntent.setDataAndType(Uri.parse(finalUrl), "application/pdf");
                                        pdfIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        startActivity(pdfIntent);
                                    } catch (Exception e) {
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                                        startActivity(browserIntent);
                                    }
                                });
                            } else {
                                Glide.with(MedicalRecordsActivity.this)
                                        .load(finalUrl)
                                        .placeholder(R.drawable.ic_file)
                                        .error(R.drawable.ic_file)
                                        .into(viewBinding.ivRecordImagePreview);
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            String authUrl = BuildConfig.SUPABASE_URL + "storage/v1/object/authenticated/medical-records/" + record.getFilePath();
                            if (!record.isPdf()) {
                                Glide.with(MedicalRecordsActivity.this)
                                        .load(authUrl)
                                        .placeholder(R.drawable.ic_file)
                                        .error(R.drawable.ic_file)
                                        .into(viewBinding.ivRecordImagePreview);
                            }
                        }
                    });
        }

        viewBinding.btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        viewBinding.btnDeleteRecord.setOnClickListener(v -> {
            dialog.dismiss();
            confirmAndDeleteRecord(record);
        });

        dialog.show();
    }

    private void confirmAndDeleteRecord(MedicalRecord record) {
        if (record.isDoctorGenerated()) {
            Toast.makeText(this, "Doctor-generated prescriptions cannot be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Medical Record")
                .setMessage("Are you sure you want to delete '" + record.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> executeDeleteRecord(record))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteRecord(MedicalRecord record) {
        // Delete record metadata from Supabase database
        SupabaseClient.getMedicalRecordService().deleteMedicalRecord("eq." + record.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        deleteStorageFile(record);
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        deleteStorageFile(record);
                    }
                });
    }

    private void deleteStorageFile(MedicalRecord record) {
        if (record.getFilePath() != null && !record.getFilePath().isEmpty()) {
            SupabaseClient.getMedicalRecordService().deleteStorageFile(record.getFilePath())
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            finishDeletion(record);
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            finishDeletion(record);
                        }
                    });
        } else {
            finishDeletion(record);
        }
    }

    private void finishDeletion(MedicalRecord record) {
        allFetchedRecords.remove(record);
        filterAndDisplayRecords(currentTabPosition);
        Toast.makeText(this, "Record deleted successfully.", Toast.LENGTH_SHORT).show();
    }

    private List<MedicalRecord> getSampleRecords() {
        List<MedicalRecord> list = new ArrayList<>();

        MedicalRecord docPrescription = new MedicalRecord(
                "rec_doc_1",
                PreferenceManager.getInstance(this).getUserId(),
                "doctor_prescription",
                "Dermatology Prescription",
                "Dr. Priya Sharma",
                "Skin Care Clinic",
                "18 Sep 2025",
                "",
                "Follow-up in 10 days."
        );
        docPrescription.setDoctorGenerated(true);
        docPrescription.setDiagnosis("Acute Dermatitis");
        docPrescription.setMedicines("Cetirizine 10mg (1-0-1, 5 days)\nHydrocortisone Ointment (Twice daily)");
        docPrescription.setAdvice("Avoid direct sunlight and stay hydrated.");
        docPrescription.setFollowUpDate("28 Sep 2025");
        list.add(docPrescription);

        MedicalRecord labReport = new MedicalRecord(
                "rec_upload_1",
                PreferenceManager.getInstance(this).getUserId(),
                "lab_report",
                "Complete Blood Count (CBC)",
                "Dr. Rajesh Kumar",
                "Metropolis Diagnostics",
                "10 Sep 2025",
                "",
                "Hemoglobin & Vitamin D levels checked."
        );
        list.add(labReport);

        return list;
    }
}
