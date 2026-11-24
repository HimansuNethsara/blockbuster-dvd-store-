package lk.jiat.blockbuster.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.net.Uri;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;
import android.graphics.Typeface;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.activity.MainActivity;
import lk.jiat.blockbuster.databinding.FragmentProfileBinding;
import lk.jiat.blockbuster.model.User;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        loadUserDetails();
        setupEditActions();

        return binding.getRoot();
    }

    private void loadUserDetails() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {

            firebaseFirestore.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(ds -> {

                        if (!isAdded() || binding == null) return;

                        if (ds.exists()) {

                            User user = ds.toObject(User.class);
                            if (user == null) return;

                            String fullName = user.getFirstName() + " " + user.getLastName();

                            binding.profileName.setText(fullName);
                            binding.profileEmail.setText(user.getEmail());

                            binding.profileFirstname.setText(user.getFirstName());
                            binding.profileLastname.setText(user.getLastName());
                            binding.profileEmailInfo.setText(user.getEmail());

                            if (user.getRegisteredDate() != null && !user.getRegisteredDate().isEmpty()) {
                                binding.profileMemberSince.setText(
                                        getString(R.string.profile_member_since, user.getRegisteredDate())
                                );
                            }

                            if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {

                                Glide.with(binding.profileImage.getContext())
                                        .load(user.getProfilePicUrl())
                                        .circleCrop()
                                        .into(binding.profileImage);

                            } else {

                                binding.profileImage.setImageResource(R.drawable.contacts_product_24px);

                            }
                        }
                    });
        }
    }

    private void setupEditActions() {

        binding.profileEditProfileOption.setOnClickListener(v -> enableEditMode());
        binding.profileEditName.setOnClickListener(v -> enableEditMode());
        binding.profileSaveBtn.setOnClickListener(v -> saveProfile());
        binding.profileUpdatePasswordBtn.setOnClickListener(v -> updatePassword());

        binding.profileEditIcon.setOnClickListener(v -> showImagePickerOptions());
    }

    private void enableEditMode() {

        binding.profileSaveBtn.setVisibility(View.VISIBLE);

        binding.profileFirstnameDisplay.setVisibility(View.GONE);
        binding.profileFirstnameEdit.setVisibility(View.VISIBLE);

        binding.profileLastnameDisplay.setVisibility(View.GONE);
        binding.profileLastnameEdit.setVisibility(View.VISIBLE);

        binding.profileFirstnameEdit.setText(binding.profileFirstname.getText());
        binding.profileLastnameEdit.setText(binding.profileLastname.getText());
    }

    private void saveProfile() {

        String firstName = binding.profileFirstnameEdit.getText().toString().trim();
        String lastName = binding.profileLastnameEdit.getText().toString().trim();

        if (firstName.isEmpty()) {
            binding.profileFirstnameEdit.setError("First name is required");
            binding.profileFirstnameEdit.requestFocus();
            return;
        }

        if (firstName.length() < 2) {
            binding.profileFirstnameEdit.setError("First name must be at least 2 characters");
            binding.profileFirstnameEdit.requestFocus();
            return;
        }

        if (firstName.length() > 30) {
            binding.profileFirstnameEdit.setError("First name cannot exceed 30 characters");
            binding.profileFirstnameEdit.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            binding.profileLastnameEdit.setError("Last name is required");
            binding.profileLastnameEdit.requestFocus();
            return;
        }

        if (lastName.length() < 2) {
            binding.profileLastnameEdit.setError("Last name must be at least 2 characters");
            binding.profileLastnameEdit.requestFocus();
            return;
        }

        if (lastName.length() > 30) {
            binding.profileLastnameEdit.setError("Last name cannot exceed 30 characters");
            binding.profileLastnameEdit.requestFocus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { return; }
        String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(
                        "firstName", firstName,
                        "lastName", lastName,
                        "fullName", firstName + " " + lastName
                )
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        MotionToast.Companion.createColorToast(
                                requireActivity(),
                                "Success",
                                "Profile updated successfully",
                                MotionToastStyle.SUCCESS,
                                MotionToast.GRAVITY_BOTTOM,
                                MotionToast.SHORT_DURATION,
                                Typeface.DEFAULT
                        );
                    }

                    binding.profileFirstname.setText(firstName);
                    binding.profileLastname.setText(lastName);
                    binding.profileName.setText(firstName + " " + lastName);

                    binding.profileFirstnameEdit.setVisibility(View.GONE);
                    binding.profileLastnameEdit.setVisibility(View.GONE);
                    binding.profileFirstnameDisplay.setVisibility(View.VISIBLE);
                    binding.profileLastnameDisplay.setVisibility(View.VISIBLE);

                    binding.profileSaveBtn.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updatePassword() {

        String currentPassword = Objects.requireNonNullElse(binding.profileCurrentPassword.getText(), "").toString().trim();
        String newPassword = Objects.requireNonNullElse(binding.profileNewPassword.getText(), "").toString().trim();
        String confirmPassword = Objects.requireNonNullElse(binding.profileConfirmPassword.getText(), "").toString().trim();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        binding.profileUpdatePasswordBtn.setEnabled(false);

        if (user == null) return;

        if (currentPassword.isEmpty()) {
            binding.profileCurrentPassword.setError("Enter current password");
            binding.profileCurrentPassword.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            binding.profileNewPassword.setError("Enter new password");
            binding.profileNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            binding.profileNewPassword.setError("Password must be at least 6 characters");
            binding.profileNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.profileConfirmPassword.setError("Passwords do not match");
            binding.profileConfirmPassword.requestFocus();
            return;
        }

        if (newPassword.equals(currentPassword)) {
            binding.profileNewPassword.setError("New password must be different from current password");
            binding.profileNewPassword.requestFocus();
            return;
        }

        // Re-authenticate user
        String email = user.getEmail();
        if (email == null) {
            MotionToast.Companion.createColorToast(
                    requireActivity(),
                    "Error",
                    "Email not found. Cannot update password.",
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.SHORT_DURATION,
                    Typeface.DEFAULT
            );
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        user.reauthenticate(credential).addOnSuccessListener(unused -> user.updatePassword(newPassword).addOnSuccessListener(unused2 -> {

            if (isAdded()) {

                MotionToast.Companion.createColorToast(
                        requireActivity(),
                        "Success",
                        "Password updated successfully",
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.SHORT_DURATION,
                        Typeface.DEFAULT
                );
            }

            binding.profileCurrentPassword.setText("");
            binding.profileNewPassword.setText("");
            binding.profileConfirmPassword.setText("");
            binding.profileUpdatePasswordBtn.setEnabled(true);

        }).addOnFailureListener(e -> MotionToast.Companion.createColorToast(
                requireActivity(),
                "Error",
                "Password update failed",
                MotionToastStyle.ERROR,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.SHORT_DURATION,
                Typeface.DEFAULT
        ))).addOnFailureListener(e -> {

            binding.profileCurrentPassword.setError("Current password incorrect");
            binding.profileCurrentPassword.requestFocus();

        });
    }

    private void showImagePickerOptions() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(
                R.layout.bottom_sheet_image_picker,
                (ViewGroup) getView(),
                false
        );
        bottomSheetDialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btn_camera).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        });

        sheetView.findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(galleryIntent);
        });

        sheetView.findViewById(R.id.btn_remove).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            removeProfileImage();
        });

        bottomSheetDialog.show();
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    Bundle extras = result.getData().getExtras();
                    assert extras != null;
                    Bitmap imageBitmap = (Bitmap) extras.get("data");

                    if (imageBitmap == null) return;
                    Uri imageUri = getImageUriFromBitmap(imageBitmap);

                    Glide.with(this)
                            .load(imageUri)
                            .circleCrop()
                            .into(binding.profileImage);

                    uploadImageToFirebase(imageUri);

                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    Uri imageUri = result.getData().getData();

                    Glide.with(this)
                            .load(imageUri)
                            .circleCrop()
                            .into(binding.profileImage);

                    uploadImageToFirebase(imageUri);
                }
            }
    );

    private void uploadImageToFirebase(Uri imageUri) {

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference profileImageRef = storageRef.child(
                "profile/profile_" + uid + "/profile.jpg"
        );

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                    String imageUrl = uri.toString();

                    saveImageUrlToFirestore(imageUrl);

                    if (binding != null) {
                        Glide.with(binding.profileImage.getContext())
                                .load(imageUrl)
                                .skipMemoryCache(true)
                                .circleCrop()
                                .into(binding.profileImage);
                    }

                }))
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        MotionToast.Companion.createColorToast(
                                requireActivity(),
                                "Upload Failed",
                                "Image upload failed",
                                MotionToastStyle.ERROR,
                                MotionToast.GRAVITY_BOTTOM,
                                MotionToast.SHORT_DURATION,
                                Typeface.DEFAULT
                        );
                    }
                });
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(
                requireActivity().getContentResolver(),
                bitmap,
                "ProfilePic",
                null
        );

        return Uri.parse(path);
    }

    private void saveImageUrlToFirestore(String imageUrl) {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            firebaseFirestore.collection("users")
                    .document(user.getUid())
                    .update("profilePicUrl", imageUrl)
                    .addOnSuccessListener(unused -> {

                        if (isAdded()) {
                            MotionToast.Companion.createColorToast(
                                    requireActivity(),
                                    "Success",
                                    "Profile image updated",
                                    MotionToastStyle.SUCCESS,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    Typeface.DEFAULT
                            );
                        }

                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshDrawerHeader();
                        }

                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void removeProfileImage() {

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        StorageReference profileImageRef = firebaseStorage.getReference()
                .child("profile/profile_" + uid + "/profile.jpg");

        profileImageRef.delete().addOnSuccessListener(unused -> {

            firebaseFirestore.collection("users")
                    .document(uid)
                    .update("profilePicUrl", "");

            binding.profileImage.setImageResource(R.drawable.contacts_product_24px);

            if (binding != null) {

                MotionToast.Companion.createColorToast(
                        requireActivity(),
                        "Removed",
                        "Profile image removed",
                        MotionToastStyle.DELETE,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.SHORT_DURATION,
                        Typeface.DEFAULT
                );

            }

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshDrawerHeader();
            }

        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to remove image", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}