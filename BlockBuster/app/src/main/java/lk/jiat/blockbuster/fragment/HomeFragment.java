package lk.jiat.blockbuster.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.MovieAdapter;
import lk.jiat.blockbuster.databinding.FragmentHomeBinding;
import lk.jiat.blockbuster.model.Dvd;
import lk.jiat.blockbuster.model.Movie;
import lk.jiat.blockbuster.model.Order;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore db;
    private MovieAdapter featuredAdapter;
    private MovieAdapter topSellingAdapter;
    private MovieAdapter newArrivalsAdapter;
    private MovieAdapter recommendedAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerViews();
        setupClickListeners();
        loadData();
    }

    private void setupRecyclerViews() {
        // Featured RecyclerView
        binding.featuredRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        featuredAdapter = new MovieAdapter(new ArrayList<>(), this::navigateToMovieDetails);
        binding.featuredRecyclerView.setAdapter(featuredAdapter);

        // Top Selling RecyclerView
        binding.topSellingRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        topSellingAdapter = new MovieAdapter(new ArrayList<>(), this::navigateToMovieDetails);
        binding.topSellingRecyclerView.setAdapter(topSellingAdapter);

        // New Arrivals RecyclerView
        binding.newArrivalsRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        newArrivalsAdapter = new MovieAdapter(new ArrayList<>(), this::navigateToMovieDetails);
        binding.newArrivalsRecyclerView.setAdapter(newArrivalsAdapter);

        // Recommended RecyclerView
        binding.recommendedRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recommendedAdapter = new MovieAdapter(new ArrayList<>(), this::navigateToMovieDetails);
        binding.recommendedRecyclerView.setAdapter(recommendedAdapter);

        binding.featuredRecyclerView.setHasFixedSize(true);
        binding.topSellingRecyclerView.setHasFixedSize(true);
        binding.newArrivalsRecyclerView.setHasFixedSize(true);
        binding.recommendedRecyclerView.setHasFixedSize(true);
    }

    private void setupClickListeners() {
        binding.btnShopNow.setOnClickListener(v -> navigateToAllMovies());

        binding.featuredSeeAll.setOnClickListener(v -> navigateToAllMovies());

        binding.topSellingSeeAll.setOnClickListener(v -> navigateToAllMovies());

        binding.newArrivalsSeeAll.setOnClickListener(v -> navigateToAllMovies());

        binding.recommendedSeeAll.setOnClickListener(v -> navigateToAllMovies());
    }

    private void loadData() {
        loadFeaturedMovies();
        loadTopSellingMovies();
        loadNewArrivals();
        loadRecommendedMovies();
    }

    private void loadFeaturedMovies() {
        db.collection("movies")
                .orderBy("imdbRating", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Movie movie = document.toObject(Movie.class);
                        if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
                            movie.setMovieId(document.getId());
                        }
                        movies.add(movie);
                    }
                    featuredAdapter.updateMovies(movies);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load featured movies", Toast.LENGTH_SHORT).show());
    }

    private void loadTopSellingMovies() {
        // Get all orders and count DVD purchases
        db.collection("orders")
                .get()
                .addOnSuccessListener(orderSnapshots -> {

                    // Count occurrences of each dvdId
                    Map<String, Integer> dvdSalesCount = new HashMap<>();

                    for (QueryDocumentSnapshot orderDoc : orderSnapshots) {
                        Order order = orderDoc.toObject(Order.class);

                        if (order.getOrderItems() != null) {
                            for (Order.OrderItem item : order.getOrderItems()) {
                                String dvdId = item.getDvdId();
                                int quantity = item.getQuantity();
                                dvdSalesCount.put(dvdId, dvdSalesCount.getOrDefault(dvdId, 0) + quantity);
                            }
                        }
                    }

                    if (dvdSalesCount.isEmpty()) {
                        // If no orders, load fallback movies
                        loadFallbackTopSelling();
                        return;
                    }

                    // Sort dvdIds by sales count
                    List<String> topDvdIds = new ArrayList<>(dvdSalesCount.keySet());
                    topDvdIds.sort((a, b) -> dvdSalesCount.get(b).compareTo(dvdSalesCount.get(a)));

                    // Get top 10 DVD IDs
                    List<String> top10DvdIds = topDvdIds.subList(0, Math.min(10, topDvdIds.size()));

                    db.collection("dvds")
                            .whereIn("dvdId", top10DvdIds)
                            .get()
                            .addOnSuccessListener(dvdSnapshots -> {

                                List<String> movieIds = new ArrayList<>();

                                for (QueryDocumentSnapshot dvdDoc : dvdSnapshots) {
                                    if (top10DvdIds.contains(dvdDoc.getId())) {
                                        Dvd dvd = dvdDoc.toObject(Dvd.class);

                                        if (dvd != null && dvd.getMovieId() != null) {
                                            movieIds.add(dvd.getMovieId());
                                        }
                                    }
                                }

                                if (movieIds.isEmpty()) {
                                    loadFallbackTopSelling();
                                    return;
                                }

                                db.collection("movies")
                                        .whereIn("movieId", movieIds)
                                        .limit(10)
                                        .get()
                                        .addOnSuccessListener(movieSnapshots -> {
                                            List<Movie> movies = new ArrayList<>();

                                            for (QueryDocumentSnapshot movieDoc : movieSnapshots) {
                                                if (movieIds.contains(movieDoc.getId())) {
                                                    Movie movie = movieDoc.toObject(Movie.class);

                                                    if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
                                                        movie.setMovieId(movieDoc.getId());
                                                    }

                                                    movies.add(movie);
                                                }
                                            }

                                            if (movies.isEmpty()) {
                                                loadFallbackTopSelling();
                                            } else {
                                                topSellingAdapter.updateMovies(movies);
                                            }
                                        })
                                        .addOnFailureListener(e -> loadFallbackTopSelling());
                            })
                            .addOnFailureListener(e -> loadFallbackTopSelling());
                })
                .addOnFailureListener(e -> loadFallbackTopSelling());
    }

    private void loadFallbackTopSelling() {
        // Fallback: load movies by IMDb rating
        db.collection("movies")
                .orderBy("imdbRating", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Movie movie = document.toObject(Movie.class);
                        movie.setMovieId(document.getId());
                        movies.add(movie);
                    }
                    topSellingAdapter.updateMovies(movies);
                });
    }

    private void loadNewArrivals() {
        db.collection("movies")
                .orderBy("releaseYear", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Movie movie = document.toObject(Movie.class);
                        if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
                            movie.setMovieId(document.getId());
                        }
                        movies.add(movie);
                    }
                    newArrivalsAdapter.updateMovies(movies);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load new arrivals", Toast.LENGTH_SHORT).show());
    }

    private void loadRecommendedMovies() {
        db.collection("movies")
                .orderBy("rtRating", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Movie movie = document.toObject(Movie.class);
                        if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
                            movie.setMovieId(document.getId());
                        }
                        movies.add(movie);
                    }
                    recommendedAdapter.updateMovies(movies);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load recommendations", Toast.LENGTH_SHORT).show());
    }

    private void navigateToMovieDetails(Movie movie) {
        Bundle bundle = new Bundle();
        bundle.putString("movieId", movie.getMovieId());

        SingleMovieFragment fragment = new SingleMovieFragment();
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToAllMovies() {
        ListingFragment fragment = new ListingFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}