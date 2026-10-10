package com.amstudio.drpoint.ui.main;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ActivityMainBinding;
import com.amstudio.drpoint.ui.appointments.AppointmentsFragment;
import com.amstudio.drpoint.ui.explore.ExploreFragment;
import com.amstudio.drpoint.ui.home.HomeFragment;
import com.amstudio.drpoint.ui.profile.ProfileFragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SELECT_TAB = "extra_select_tab";
    public static final int TAB_HOME = 0;
    public static final int TAB_EXPLORE = 1;
    public static final int TAB_APPOINTMENTS = 2;
    public static final int TAB_PROFILE = 3;

    private static final String TAG_HOME = "TAG_HOME";
    private static final String TAG_EXPLORE = "TAG_EXPLORE";
    private static final String TAG_APPOINTMENTS = "TAG_APPOINTMENTS";
    private static final String TAG_PROFILE = "TAG_PROFILE";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBackNavigation();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                switchFragment(TAG_HOME);
                return true;
            } else if (itemId == R.id.navigation_explore) {
                switchFragment(TAG_EXPLORE);
                return true;
            } else if (itemId == R.id.navigation_appointments) {
                switchFragment(TAG_APPOINTMENTS);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                switchFragment(TAG_PROFILE);
                return true;
            }
            return false;
        });

        binding.bottomNavigation.setOnItemReselectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                switchFragment(TAG_HOME);
            } else if (itemId == R.id.navigation_explore) {
                switchFragment(TAG_EXPLORE);
            } else if (itemId == R.id.navigation_appointments) {
                switchFragment(TAG_APPOINTMENTS);
            } else if (itemId == R.id.navigation_profile) {
                switchFragment(TAG_PROFILE);
            }
        });

        if (savedInstanceState == null) {
            int selectTab = getIntent().getIntExtra(EXTRA_SELECT_TAB, TAB_HOME);
            selectTab(selectTab);
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding == null) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    return;
                }

                int selectedItemId = binding.bottomNavigation.getSelectedItemId();

                if (selectedItemId == R.id.navigation_profile) {
                    selectTab(TAB_APPOINTMENTS);
                } else if (selectedItemId == R.id.navigation_appointments) {
                    selectTab(TAB_EXPLORE);
                } else if (selectedItemId == R.id.navigation_explore) {
                    selectTab(TAB_HOME);
                } else {
                    // Currently on Home tab: exit app / default back behavior
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void switchFragment(String targetTag) {
        if (isFinishing() || isDestroyed()) return;

        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFrag = getActiveFragment(fm);

        if (currentFrag != null && Objects.equals(targetTag, currentFrag.getTag())) {
            return; // Already showing this fragment
        }

        FragmentTransaction transaction = fm.beginTransaction();

        Fragment targetFrag = fm.findFragmentByTag(targetTag);
        if (targetFrag == null) {
            switch (targetTag) {
                case TAG_EXPLORE:
                    targetFrag = new ExploreFragment();
                    break;
                case TAG_APPOINTMENTS:
                    targetFrag = new AppointmentsFragment();
                    break;
                case TAG_PROFILE:
                    targetFrag = new ProfileFragment();
                    break;
                case TAG_HOME:
                default:
                    targetFrag = new HomeFragment();
                    break;
            }
            transaction.add(R.id.fragment_container, targetFrag, targetTag);
        }

        Fragment homeFrag = fm.findFragmentByTag(TAG_HOME);
        Fragment exploreFrag = fm.findFragmentByTag(TAG_EXPLORE);
        Fragment apptsFrag = fm.findFragmentByTag(TAG_APPOINTMENTS);
        Fragment profileFrag = fm.findFragmentByTag(TAG_PROFILE);

        Fragment[] fragments = {homeFrag, exploreFrag, apptsFrag, profileFrag};
        for (Fragment f : fragments) {
            if (f != null && f != targetFrag && !f.isHidden()) {
                transaction.hide(f);
                transaction.setMaxLifecycle(f, Lifecycle.State.STARTED);
            }
        }

        transaction.show(targetFrag);
        transaction.setMaxLifecycle(targetFrag, Lifecycle.State.RESUMED);

        transaction.commitAllowingStateLoss();
    }

    private Fragment getActiveFragment(FragmentManager fm) {
        Fragment homeFrag = fm.findFragmentByTag(TAG_HOME);
        if (homeFrag != null && homeFrag.isVisible()) return homeFrag;

        Fragment exploreFrag = fm.findFragmentByTag(TAG_EXPLORE);
        if (exploreFrag != null && exploreFrag.isVisible()) return exploreFrag;

        Fragment apptsFrag = fm.findFragmentByTag(TAG_APPOINTMENTS);
        if (apptsFrag != null && apptsFrag.isVisible()) return apptsFrag;

        Fragment profileFrag = fm.findFragmentByTag(TAG_PROFILE);
        if (profileFrag != null && profileFrag.isVisible()) return profileFrag;

        return null;
    }

    public void selectTab(int tabIndex) {
        if (binding == null) return;

        int targetItemId;
        String targetTag;

        switch (tabIndex) {
            case TAB_EXPLORE:
                targetItemId = R.id.navigation_explore;
                targetTag = TAG_EXPLORE;
                break;
            case TAB_APPOINTMENTS:
                targetItemId = R.id.navigation_appointments;
                targetTag = TAG_APPOINTMENTS;
                break;
            case TAB_PROFILE:
                targetItemId = R.id.navigation_profile;
                targetTag = TAG_PROFILE;
                break;
            case TAB_HOME:
            default:
                targetItemId = R.id.navigation_home;
                targetTag = TAG_HOME;
                break;
        }

        // 1) Explicitly switch to target fragment to guarantee initial load on startup
        switchFragment(targetTag);

        // 2) Update bottom navigation selected item if different
        if (binding.bottomNavigation.getSelectedItemId() != targetItemId) {
            binding.bottomNavigation.setSelectedItemId(targetItemId);
        }
    }
}
