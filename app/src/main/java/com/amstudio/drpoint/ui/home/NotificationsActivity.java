package com.amstudio.drpoint.ui.home;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amstudio.drpoint.adapter.NotificationAdapter;
import com.amstudio.drpoint.databinding.ActivityNotificationsBinding;
import com.amstudio.drpoint.model.NotificationItem;
import com.amstudio.drpoint.network.SupabaseClient;
import com.amstudio.drpoint.util.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvMarkAllRead.setOnClickListener(v -> markAllAsRead());

        setupRecyclerView();

        binding.swipeRefresh.setOnRefreshListener(this::loadNotificationsFromSupabase);

        loadNotificationsFromSupabase();
    }

    private void setupRecyclerView() {
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notification -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                adapter.notifyDataSetChanged();
                markNotificationReadInSupabase(notification.getId());
            }
        });
        binding.rvNotifications.setAdapter(adapter);
    }

    private void loadNotificationsFromSupabase() {
        binding.swipeRefresh.setRefreshing(true);
        String userId = PreferenceManager.getInstance(this).getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            binding.swipeRefresh.setRefreshing(false);
            notificationList = getSampleNotifications();
            displayNotifications();
            return;
        }

        SupabaseClient.getNotificationService().getNotificationsForPatient("eq." + userId)
                .enqueue(new Callback<List<NotificationItem>>() {
                    @Override
                    public void onResponse(Call<List<NotificationItem>> call, Response<List<NotificationItem>> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        List<NotificationItem> list = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null) {
                            list.addAll(response.body());
                        }
                        if (list.isEmpty()) {
                            list.addAll(getSampleNotifications());
                        }
                        notificationList = list;
                        displayNotifications();
                    }

                    @Override
                    public void onFailure(Call<List<NotificationItem>> call, Throwable t) {
                        binding.swipeRefresh.setRefreshing(false);
                        notificationList = getSampleNotifications();
                        displayNotifications();
                    }
                });
    }

    private void displayNotifications() {
        if (notificationList.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvNotifications.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvNotifications.setVisibility(View.VISIBLE);
        }
        adapter.submitList(new ArrayList<>(notificationList));
    }

    private void markNotificationReadInSupabase(String notificationId) {
        if (notificationId == null || notificationId.startsWith("notif_sample")) return;

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("is_read", true);

        SupabaseClient.getNotificationService().markNotificationAsRead("eq." + notificationId, updateMap)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {}

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    private void markAllAsRead() {
        for (NotificationItem item : notificationList) {
            item.setRead(true);
            markNotificationReadInSupabase(item.getId());
        }
        displayNotifications();
        Toast.makeText(this, "All notifications marked as read.", Toast.LENGTH_SHORT).show();
    }

    private List<NotificationItem> getSampleNotifications() {
        List<NotificationItem> list = new ArrayList<>();

        list.add(new NotificationItem(
                "notif_sample_1",
                PreferenceManager.getInstance(this).getUserId(),
                "Appointment Confirmed",
                "Your appointment with Dr. Priya Sharma is confirmed for Tomorrow at 10:30 AM.",
                "appointment_confirmed",
                false,
                "10 mins ago"
        ));

        list.add(new NotificationItem(
                "notif_sample_2",
                PreferenceManager.getInstance(this).getUserId(),
                "Prescription Available",
                "Dr. Priya Sharma generated a new prescription for your consultation.",
                "prescription_available",
                false,
                "2 hours ago"
        ));

        list.add(new NotificationItem(
                "notif_sample_3",
                PreferenceManager.getInstance(this).getUserId(),
                "Follow-up Reminder",
                "Reminder: Follow-up visit scheduled with Skin Care Clinic on 28 Sep.",
                "follow_up_reminder",
                true,
                "1 day ago"
        ));

        return list;
    }
}
