package lk.jiat.blockbuster.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.PrefThemeSelectorBinding;

public class ThemeSelectorPreference extends Preference {

    public ThemeSelectorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pref_theme_selector);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        PrefThemeSelectorBinding binding =
                PrefThemeSelectorBinding.bind(holder.itemView);

        CardView lightCard = binding.lightCard;
        CardView darkCard = binding.darkCard;

        View lightGlow = binding.lightGlow;
        View darkGlow = binding.darkGlow;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        String current = prefs.getString("theme_mode", "light");

        if ("light".equals(current)) {
            highlightSelection(lightCard, darkCard, lightGlow, darkGlow);
        } else {
            highlightSelection(darkCard, lightCard, darkGlow, lightGlow);
        }

        lightCard.setOnClickListener(v -> {
            prefs.edit().putString("theme_mode", "light").apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            highlightSelection(lightCard, darkCard, lightGlow, darkGlow);
        });

        darkCard.setOnClickListener(v -> {
            prefs.edit().putString("theme_mode", "dark").apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            highlightSelection(darkCard, lightCard, darkGlow, lightGlow);
        });
    }

    private void highlightSelection(CardView selected, CardView unselected, View selectedGlow, View unselectedGlow) {
        selected.setCardElevation(18f);
        unselected.setCardElevation(6f);

        selected.setAlpha(1f);
        unselected.setAlpha(0.7f);

        selected.setCardBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.md_theme_secondaryContainer));
        unselected.setCardBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.md_theme_surfaceContainerHigh));

        selectedGlow.setVisibility(View.VISIBLE);
        unselectedGlow.setVisibility(View.GONE);

        // Optional: add scale effect
        selected.setScaleX(1.05f);
        selected.setScaleY(1.05f);
        unselected.setScaleX(1f);
        unselected.setScaleY(1f);
    }
}