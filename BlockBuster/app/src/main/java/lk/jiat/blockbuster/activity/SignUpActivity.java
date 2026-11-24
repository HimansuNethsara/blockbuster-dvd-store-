package lk.jiat.blockbuster.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.databinding.ActivitySignUpBinding;
import lk.jiat.blockbuster.model.User;
import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        binding.signupLinkSignin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signupBtnSignup.setOnClickListener(view -> {

            String firstName = String.valueOf(binding.signupInputFirstname.getText()).trim();
            String lastName = String.valueOf(binding.signupInputLastname.getText()).trim();
            String email = String.valueOf(binding.signupInputEmail.getText()).trim();
            String password = String.valueOf(binding.signupInputPassword.getText()).trim();
            String retypePassword = String.valueOf(binding.signupInputRetypePassword.getText()).trim();

            if (firstName.isEmpty()) {
                binding.signupInputFirstname.setError("First name is required");
                binding.signupInputFirstname.requestFocus();
                return;
            }

            if (lastName.isEmpty()) {
                binding.signupInputLastname.setError("Last name is required");
                binding.signupInputLastname.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                binding.signupInputEmail.setError("Email is required");
                binding.signupInputEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.signupInputEmail.setError("Enter valid email");
                binding.signupInputEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                binding.signupInputPassword.setError("Password is required");
                binding.signupInputPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                binding.signupInputPassword.setError("Password must be at least 6 characters");
                binding.signupInputPassword.requestFocus();
                return;
            }

            if (!retypePassword.equals(password)) {
                binding.signupInputRetypePassword.setError("Password and retype password must be the same");
                binding.signupInputRetypePassword.requestFocus();
                return;
            }

            binding.signupBtnSignup.setEnabled(false);

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        binding.signupBtnSignup.setEnabled(true);

                        if (task.isSuccessful()) {
                            if (task.getResult() != null && task.getResult().getUser() != null) {
                                String uid = task.getResult().getUser().getUid();

                                String registeredDate = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());

                                User user = User.builder()
                                        .uid(uid)
                                        .firstName(firstName)
                                        .lastName(lastName)
                                        .email(email)
                                        .registeredDate(registeredDate)
                                        .build();

                                firebaseFirestore.collection("users").document(uid)
                                        .set(user)
                                        .addOnSuccessListener(unused -> {
                                            MotionToast.Companion.createColorToast(
                                                    SignUpActivity.this,
                                                    "Account Created",
                                                    "Account created successfully",
                                                    MotionToastStyle.SUCCESS,
                                                    MotionToast.GRAVITY_BOTTOM,
                                                    MotionToast.SHORT_DURATION,
                                                    Typeface.DEFAULT
                                            );
                                            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> MotionToast.Companion.createColorToast(
                                                SignUpActivity.this,
                                                "Firestore Error",
                                                "Failed to save user data: " + e.getMessage(),
                                                MotionToastStyle.ERROR,
                                                MotionToast.GRAVITY_BOTTOM,
                                                MotionToast.LONG_DURATION,
                                                Typeface.DEFAULT
                                        ));

                            } else {
                                MotionToast.Companion.createColorToast(
                                        SignUpActivity.this,
                                        "Registration Error",
                                        "Unable to get user ID",
                                        MotionToastStyle.ERROR,
                                        MotionToast.GRAVITY_BOTTOM,
                                        MotionToast.LONG_DURATION,
                                        Typeface.DEFAULT
                                );
                            }


                        } else {
                            String errorMessage = "Registration failed";
                            if (task.getException() != null && task.getException().getMessage() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            MotionToast.Companion.createColorToast(
                                    SignUpActivity.this,
                                    "Registration Failed",
                                    errorMessage,
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.LONG_DURATION,
                                    Typeface.DEFAULT
                            );
                        }
                    });


        });

        //privacy and terms of service load
        TextView tvPolicy = binding.signupPolicyText;
        String fullText = getString(R.string.signup_policy_text);
        String[] clickableTexts = {"Terms of Service", "Privacy Policy"};
        loadPolicy(tvPolicy, fullText, clickableTexts);
    }

    private void loadPolicy(TextView textView, String fullText, String[] clickableTexts) {
        SpannableString spannable = new SpannableString(fullText);

        for (String word : clickableTexts) {
            int start = fullText.indexOf(word);
            if (start == -1) continue;
            int end = start + word.length();

            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (word.equals("Terms of Service")) {
                        showTermsDialog();
                    } else if (word.equals("Privacy Policy")) {
                        showPrivacyDialog();
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(ContextCompat.getColor(SignUpActivity.this, R.color.app_primary_fixed));
                    ds.setUnderlineText(false);
                    ds.setFakeBoldText(true);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showTermsDialog() {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_terms_of_service, null);

        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        view.findViewById(R.id.btn_close_terms).setOnClickListener(v -> dialog.dismiss());
    }

    private void showPrivacyDialog() {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_privacy_policy, null);

        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        view.findViewById(R.id.btn_close_privacy).setOnClickListener(v -> dialog.dismiss());
    }

}