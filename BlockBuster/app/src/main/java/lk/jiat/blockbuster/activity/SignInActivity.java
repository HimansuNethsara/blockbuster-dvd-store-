package lk.jiat.blockbuster.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.jiat.blockbuster.databinding.ActivitySignInBinding;
import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        binding.signinLinkSignup.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signinBtnSignin.setOnClickListener(view -> {

            String email = binding.signinInputEmail.getText().toString().trim();
            String password = String.valueOf(binding.signinInputPassword.getText()).trim();

            if (email.isEmpty()) {
                binding.signinInputEmail.setError("Email is required");
                binding.signinInputEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.signinInputEmail.setError("Enter valid email");
                binding.signinInputEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                binding.signinInputPassword.setError("Password is required");
                binding.signinInputPassword.requestFocus();
                return;
            }

            binding.signinBtnSignin.setEnabled(false);

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        binding.signinBtnSignin.setEnabled(true);
                        if (task.isSuccessful()) {
                            updateUI(firebaseAuth.getCurrentUser());
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            MotionToast.Companion.createColorToast(
                                    SignInActivity.this,
                                    "Login Failed",
                                    errorMessage != null ? errorMessage : "Authentication failed",
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    Typeface.DEFAULT
                            );
                        }
                    });
        });

    }

    private void updateUI(FirebaseUser user) {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}