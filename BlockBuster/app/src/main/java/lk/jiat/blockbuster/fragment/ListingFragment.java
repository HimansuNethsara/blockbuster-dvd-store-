package lk.jiat.blockbuster.fragment;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.ListingAdapter;
import lk.jiat.blockbuster.databinding.FragmentListingBinding;
import lk.jiat.blockbuster.model.Movie;

public class ListingFragment extends Fragment {

    private FragmentListingBinding binding;
    private ListingAdapter adapter;
    private String genreId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            genreId = getArguments().getString("genreId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        binding.recyclerViewListing.setLayoutManager(new GridLayoutManager(getContext(), 2));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("movies")
                .whereArrayContains("genreIds", genreId)
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(ds -> {
                    if (!ds.isEmpty()) {
                        List<Movie> movies = ds.toObjects(Movie.class);
                        adapter = new ListingAdapter(movies, movie -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("movieId", movie.getMovieId());
                            bundle.putString("genreId", genreId);

                            SingleMovieFragment singleMovieFragment = new SingleMovieFragment();
                            singleMovieFragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, singleMovieFragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        binding.recyclerViewListing.setAdapter(adapter);

                        if (movies.isEmpty()) {
                            Log.d("ListingFragment", "No movies found for genreId: " + genreId);
                        }
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Firestore", "Error:"+e.getMessage());
                    }
                });

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

    }
}
