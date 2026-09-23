package com.amstudio.drpoint.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.databinding.ItemSpecialityBinding;
import com.amstudio.drpoint.model.Speciality;
import com.bumptech.glide.Glide;

import java.util.Objects;

public class SpecialitiesAdapter extends ListAdapter<Speciality, SpecialitiesAdapter.ViewHolder> {

    public interface OnSpecialityClickListener {
        void onSpecialityClick(Speciality speciality);
    }

    private static final DiffUtil.ItemCallback<Speciality> DIFF_CALLBACK = new DiffUtil.ItemCallback<Speciality>() {
        @Override
        public boolean areItemsTheSame(@NonNull Speciality oldItem, @NonNull Speciality newItem) {
            return oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Speciality oldItem, @NonNull Speciality newItem) {
            return oldItem.getIconRes() == newItem.getIconRes() && Objects.equals(oldItem.getIconUrl(), newItem.getIconUrl());
        }
    };

    private final OnSpecialityClickListener listener;

    public SpecialitiesAdapter(OnSpecialityClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSpecialityBinding binding = ItemSpecialityBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSpecialityBinding binding;

        ViewHolder(@NonNull ItemSpecialityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Speciality speciality, OnSpecialityClickListener listener) {
            Context context = itemView.getContext();
            binding.tvSpecialityName.setText(speciality.getName());

            // Adjust layout width dynamically based on parent LayoutManager
            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                if (isParentHorizontalLinear(itemView)) {
                    lp.width = dpToPx(context, 92);
                } else {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                itemView.setLayoutParams(lp);
            }

            String imageUrl = getPhotoUrlForSpeciality(speciality);

            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.banner_1)
                    .error(R.drawable.banner_1)
                    .into(binding.ivSpecialityImage);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSpecialityClick(speciality);
                }
            });
        }

        private static boolean isParentHorizontalLinear(View view) {
            if (view.getParent() instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) view.getParent();
                RecyclerView.LayoutManager lm = rv.getLayoutManager();
                if (lm instanceof LinearLayoutManager && !(lm instanceof GridLayoutManager)) {
                    return ((LinearLayoutManager) lm).getOrientation() == LinearLayoutManager.HORIZONTAL;
                }
            }
            return false;
        }

        private static int dpToPx(Context context, float dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }

    private static String getPhotoUrlForSpeciality(Speciality speciality) {
        if (speciality != null && speciality.getIconUrl() != null && !speciality.getIconUrl().trim().isEmpty()) {
            String url = speciality.getIconUrl().trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            } else if (url.startsWith("//")) {
                return "https:" + url;
            }
        }

        String name = (speciality != null && speciality.getName() != null) ? speciality.getName().toLowerCase().trim() : "";

        if (name.contains("general") || name.contains("physician") || name.contains("fever")) {
            return "https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("women") || name.contains("gynaec") || name.contains("maternity")) {
            return "https://images.unsplash.com/photo-1594824813566-78a931a2935e?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("skin") || name.contains("derma") || name.contains("hair")) {
            return "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("dent") || name.contains("teeth")) {
            return "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("eye") || name.contains("optom") || name.contains("vision")) {
            return "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("ent") || name.contains("ear") || name.contains("nose") || name.contains("throat")) {
            return "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("child") || name.contains("pediatr") || name.contains("baby")) {
            return "https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("heart") || name.contains("cardio")) {
            return "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?auto=format&fit=crop&q=80&w=400";
        } else if (name.contains("mental") || name.contains("psych") || name.contains("mind")) {
            return "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400";
        }

        String[] fallbacks = {
            "https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=400",
            "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400",
            "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?auto=format&fit=crop&q=80&w=400",
            "https://images.unsplash.com/photo-1594824813566-78a931a2935e?auto=format&fit=crop&q=80&w=400"
        };
        int idx = Math.abs(name.hashCode()) % fallbacks.length;
        return fallbacks[idx];
    }
}
