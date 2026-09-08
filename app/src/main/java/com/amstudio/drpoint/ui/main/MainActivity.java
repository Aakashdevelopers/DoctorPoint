package com.amstudio.drpoint.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ActivityMainBinding;
import com.amstudio.drpoint.ui.appointments.AppointmentsFragment;
import com.amstudio.drpoint.ui.explore.ExploreFragment;
import com.amstudio.drpoint.ui.home.HomeFragment;
import com.amstudio.drpoint.ui.profile.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SELECT_TAB = "extra_select_tab";
    public static final int TAB_HOME = 0;
    public static final int TAB_EXPLORE = 1;
    public static final int TAB_APPOINTMENTS = 2;
    public static final int TAB_PROFILE = 3;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_explore) {
                selectedFragment = new ExploreFragment();
            } else if (itemId == R.id.navigation_appointments) {
                selectedFragment = new AppointmentsFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        int selectTab = getIntent().getIntExtra(EXTRA_SELECT_TAB, TAB_HOME);
        selectTab(selectTab);
    }

    public void selectTab(int tabIndex) {
        if (binding == null) return;

        if (tabIndex == TAB_EXPLORE) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_explore);
        } else if (tabIndex == TAB_APPOINTMENTS) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_appointments);
        } else if (tabIndex == TAB_PROFILE) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile);
        } else {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }
    }
}
