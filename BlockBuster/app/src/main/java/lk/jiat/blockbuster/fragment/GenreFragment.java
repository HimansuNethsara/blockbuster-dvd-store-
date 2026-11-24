package lk.jiat.blockbuster.fragment;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.GenreAdapter;
import lk.jiat.blockbuster.databinding.FragmentGenreBinding;
import lk.jiat.blockbuster.model.Genre;
import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class GenreFragment extends Fragment {


    private FragmentGenreBinding binding;
    private GenreAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentGenreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewGenre.setLayoutManager(new GridLayoutManager(getContext(), 2));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("genres").get()
                .addOnCompleteListener(task -> {

                    if (!isAdded() || binding == null) return;

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Genre> genres = task.getResult().toObjects(Genre.class);
                        adapter = new GenreAdapter(genres, genre -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("genreId", genre.getGenreId());

                            ListingFragment fragment = new ListingFragment();
                            fragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();

                        });
                        binding.recyclerViewGenre.setAdapter(adapter);
                    }else {
                        Log.e("GenreFragment", "Failed to load genres");

                        if (isAdded()) {
                            MotionToast.Companion.createColorToast(
                                    requireActivity(),
                                    "Error",
                                    "Failed to load genres",
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    Typeface.DEFAULT
                            );
                        }
                    }
                });

    }
}