package com.amstudio.drpoint.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.adapter.ProfileMenuAdapter;
import com.amstudio.drpoint.databinding.FragmentProfileBinding;
import com.amstudio.drpoint.model.MenuItem;
import com.amstudio.drpoint.ui.auth.LoginActivity;
import com.amstudio.drpoint.ui.main.MainActivity;
import com.amstudio.drpoint.util.DummyDataProvider;
import com.amstudio.drpoint.util.PreferenceManager;

import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferenceManager prefManager = PreferenceManager.getInstance(requireContext());
        binding.tvUserName.setText(prefManager.getUserName());
        binding.tvUserEmail.setText(prefManager.getUserEmail());

        binding.ivSettings.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Opening Settings...", Toast.LENGTH_SHORT).show()
        );

        binding.tvEditProfile.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        binding.rvProfileMenu.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<MenuItem> menuItems = DummyDataProvider.getProfileMenuItems();
        ProfileMenuAdapter adapter = new ProfileMenuAdapter(item -> {
            String title = item.getTitle();
            if ("Logout".equalsIgnoreCase(title)) {
                prefManager.clearSession();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            } else if ("My Appointments".equalsIgnoreCase(title)) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(MainActivity.TAB_APPOINTMENTS);
                }
            } else {
                Toast.makeText(requireContext(), title + " clicked", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvProfileMenu.setAdapter(adapter);
        adapter.submitList(menuItems);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
