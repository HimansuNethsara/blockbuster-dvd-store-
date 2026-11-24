package lk.jiat.blockbuster.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import lk.jiat.blockbuster.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
    }
}