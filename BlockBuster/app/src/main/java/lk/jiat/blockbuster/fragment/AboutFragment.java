package lk.jiat.blockbuster.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import lk.jiat.blockbuster.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupClickListeners();
        setupVersionInfo();
    }

    private void setupClickListeners() {

        binding.contactEmail.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:blockbusterforyou1@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "BlockBuster Support Inquiry");

            try {
                startActivity(Intent.createChooser(emailIntent, "Send email"));
            } catch (Exception e) {
                showToast("No email app found");
            }
        });

        binding.contactPhone.setOnClickListener(v -> {
            String phoneNumber = binding.contactPhoneNumber.getText().toString();

            if (phoneNumber.isEmpty()) {
                showToast("Phone number not available");
                return;
            }

            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + phoneNumber));

            try {
                startActivity(dialIntent);
            } catch (Exception e) {
                showToast("Cannot open dialer");
            }
        });

        binding.socialFacebook.setOnClickListener(v -> showComingSoon("Facebook"));
        binding.socialInstagram.setOnClickListener(v -> showComingSoon("Instagram"));
        binding.socialTwitter.setOnClickListener(v -> showComingSoon("Twitter"));
        binding.socialYoutube.setOnClickListener(v -> showComingSoon("YouTube"));
    }

    private void setupVersionInfo() {
        try {
            String versionName = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;

            binding.appVersion.setText("Version " + versionName);

        } catch (Exception e) {
            binding.appVersion.setText("Version 1.0.0");
        }
    }

    private void showComingSoon(String platform) {
        if (!isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Coming Soon")
                .setMessage(platform + " page will be available in a future update.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), message,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}