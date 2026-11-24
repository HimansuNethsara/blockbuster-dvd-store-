package lk.jiat.blockbuster.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;


import lk.jiat.blockbuster.databinding.ActivitySplashBinding;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Lock splash to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.splashLogo.setVisibility(View.VISIBLE);
        binding.lottieAnimation.setVisibility(View.GONE);
        binding.splashProgressBar.setVisibility(View.INVISIBLE);

        // After 1 second → show Lottie + progress
        binding.splashLogo.postDelayed(() -> {
            binding.splashLogo.setVisibility(View.GONE);
            binding.lottieAnimation.setVisibility(View.VISIBLE);
            binding.lottieAnimation.playAnimation();
            binding.splashProgressBar.setVisibility(View.VISIBLE);
        }, 1000);

        // After 3 seconds → go to Main
        binding.splashLogo.postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3000);
    }

}