package com.amstudio.drpoint.ui.profile;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amstudio.drpoint.databinding.BottomSheetCustomerCareBinding;
import com.amstudio.drpoint.model.CustomerCareSettings;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.util.ToastUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerCareBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "CustomerCareBottomSheet";

    private BottomSheetCustomerCareBinding binding;

    public static CustomerCareBottomSheetFragment newInstance() {
        return new CustomerCareBottomSheetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCustomerCareBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding == null) return;

        binding.ivCloseCc.setOnClickListener(v -> dismiss());
        binding.btnCcRetry.setOnClickListener(v -> loadCustomerCareSettings());

        loadCustomerCareSettings();
    }

    private void loadCustomerCareSettings() {
        if (binding == null) return;

        // Show loading state
        binding.llCcLoading.setVisibility(View.VISIBLE);
        binding.llCcError.setVisibility(View.GONE);
        binding.llCcEmpty.setVisibility(View.GONE);
        binding.llCcContent.setVisibility(View.GONE);

        SupabaseClient.getSettingsService().getCustomerCareSettings().enqueue(new Callback<List<CustomerCareSettings>>() {
            @Override
            public void onResponse(@NonNull Call<List<CustomerCareSettings>> call, @NonNull Response<List<CustomerCareSettings>> response) {
                if (binding == null || !isAdded()) return;

                binding.llCcLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    CustomerCareSettings settings = response.body().get(0);
                    displaySettings(settings);
                } else {
                    showErrorState("Unable to load customer care details.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CustomerCareSettings>> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) return;

                binding.llCcLoading.setVisibility(View.GONE);
                showErrorState("Unable to load customer care details.");
            }
        });
    }

    private void showErrorState(String message) {
        if (binding == null) return;
        binding.llCcError.setVisibility(View.VISIBLE);
        binding.tvCcErrorMsg.setText(message != null ? message : "Unable to load customer care details.");
        binding.llCcLoading.setVisibility(View.GONE);
        binding.llCcEmpty.setVisibility(View.GONE);
        binding.llCcContent.setVisibility(View.GONE);
    }

    private void displaySettings(CustomerCareSettings settings) {
        if (binding == null || settings == null) {
            showErrorState("Customer care details unavailable.");
            return;
        }

        String email = settings.getEmail() != null ? settings.getEmail().trim() : "";
        String phone1 = settings.getPhone1() != null ? settings.getPhone1().trim() : "";
        String phone2 = settings.getPhone2() != null ? settings.getPhone2().trim() : "";

        boolean hasEmail = !email.isEmpty();
        boolean hasPhone1 = !phone1.isEmpty();
        boolean hasPhone2 = !phone2.isEmpty();

        if (!hasEmail && !hasPhone1 && !hasPhone2) {
            binding.llCcEmpty.setVisibility(View.VISIBLE);
            binding.llCcContent.setVisibility(View.GONE);
            return;
        }

        binding.llCcContent.setVisibility(View.VISIBLE);

        // Email Support Card
        if (hasEmail) {
            binding.cardCcEmail.setVisibility(View.VISIBLE);
            binding.tvCcEmailValue.setText(email);
            binding.btnCcEmail.setOnClickListener(v -> openEmailClient(email));
        } else {
            binding.cardCcEmail.setVisibility(View.GONE);
        }

        // Phone 1 Card
        if (hasPhone1) {
            binding.cardCcPhone1.setVisibility(View.VISIBLE);
            binding.tvCcPhone1Value.setText(phone1);
            binding.btnCcPhone1.setOnClickListener(v -> openDialer(phone1));
        } else {
            binding.cardCcPhone1.setVisibility(View.GONE);
        }

        // Phone 2 Card
        if (hasPhone2) {
            binding.cardCcPhone2.setVisibility(View.VISIBLE);
            binding.tvCcPhone2Value.setText(phone2);
            binding.btnCcPhone2.setOnClickListener(v -> openDialer(phone2));
        } else {
            binding.cardCcPhone2.setVisibility(View.GONE);
        }
    }

    private void openEmailClient(String email) {
        Context context = getContext();
        if (context == null || email == null || email.isEmpty()) return;

        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + Uri.encode(email)));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Customer Support Inquiry - DoctorPoint");
            startActivity(Intent.createChooser(intent, "Send email using..."));
        } catch (ActivityNotFoundException e) {
            ToastUtils.showError(context, "No email app found");
        } catch (Exception e) {
            ToastUtils.showError(context, "No email app found");
        }
    }

    private void openDialer(String phoneNumber) {
        Context context = getContext();
        if (context == null || phoneNumber == null || phoneNumber.isEmpty()) return;

        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + Uri.encode(phoneNumber)));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            ToastUtils.showError(context, "No phone dialer app found");
        } catch (Exception e) {
            ToastUtils.showError(context, "Could not open dialer");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
