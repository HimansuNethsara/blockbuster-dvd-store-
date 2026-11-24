package lk.jiat.blockbuster.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.activity.SignInActivity;
import lk.jiat.blockbuster.adapter.ActorAdapter;
import lk.jiat.blockbuster.adapter.DirectorAdapter;
import lk.jiat.blockbuster.adapter.RelatedMovieAdapter;
import lk.jiat.blockbuster.databinding.FragmentSingleMovieBinding;
import lk.jiat.blockbuster.model.Actor;
import lk.jiat.blockbuster.model.CartItem;
import lk.jiat.blockbuster.model.Director;
import lk.jiat.blockbuster.model.Dvd;
import lk.jiat.blockbuster.model.Movie;
import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class SingleMovieFragment extends Fragment {

    private FragmentSingleMovieBinding binding;
    private String movieId;
    private FirebaseFirestore db;
    private String dvdId;
    private int quantity = 1;
    private int avbQuantity;
    private List<Movie> relatedMovies = new ArrayList<>();
    private RelatedMovieAdapter relatedAdapter;
    private String selectedGenreId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            movieId = getArguments().getString("movieId");
            selectedGenreId = getArguments().getString("genreId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSingleMovieBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().findViewById(R.id.bottom_navigation_view)
                .setVisibility(View.GONE);

        setupTabs();

        getActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    }
                });

        setupRecyclerViews();
        loadMovieData();
        setupAddToCartButton();
        binding.btnBuyDvd.setOnClickListener(v -> selectTab(3));

    }

    private void setupRecyclerViews() {

        binding.directorRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.castRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.relatedRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        relatedAdapter = new RelatedMovieAdapter(relatedMovies);
        binding.relatedRecyclerView.setAdapter(relatedAdapter);
    }

    private void loadMovieData() {

        db.collection("movies")
                .whereEqualTo("movieId", movieId)
                .get()
                .addOnSuccessListener(qds -> {

                    if (qds.isEmpty()) return;

                    Movie movie = qds.getDocuments()
                            .get(0)
                            .toObject(Movie.class);

                    if (movie == null) return;

                    binding.movieTitle.setText(movie.getTitle());
                    binding.castMovieTitle.setText(movie.getTitle());
                    binding.movieOverview.setText(movie.getDescription());

                    binding.movieYear.setText(String.valueOf(movie.getReleaseYear()));
                    binding.movieReleaseYear.setText(String.valueOf(movie.getReleaseYear()));

                    binding.imdbRating.setText(String.valueOf(movie.getImdbRating()));
                    binding.rtRating.setText(String.valueOf(movie.getRtRating()));

                    binding.movieRuntime.setText(movie.getRuntime() + " min");

                    loadMovieImage(movie);

                    loadGenres(movie.getGenreIds());
                    loadDirectors(movie.getDirectorIds());
                    loadActors(movie.getActorIds());
                    loadMpaaRating(movie.getMpaaRatingId());
                    renderPlotKeywords(movie.getPlotKeywordIds());

                    loadDvdDetails(movie.getMovieId());
                    loadRelatedMovies(movie);
                    setupTrailerButton(movie.getTrailerUrl());



                });
    }

    private void loadMovieImage(Movie movie) {

        if (movie.getImages() == null || movie.getImages().isEmpty()) return;

        String imagePath = movie.getImages().get(0);

        FirebaseStorage.getInstance()
                .getReference()
                .child(imagePath)
                .getDownloadUrl()
                .addOnSuccessListener(uri ->
                        Glide.with(requireContext())
                                .load(uri)
                                .into(binding.movieBanner)
                );
    }

    private void loadGenres(List<String> genreIds) {

        if (genreIds == null || genreIds.isEmpty()) return;

        StringBuilder genreNames = new StringBuilder();

        for (String genreId : genreIds) {

            db.collection("genres")
                    .whereEqualTo("genreId", genreId)
                    .get()
                    .addOnSuccessListener(gs -> {

                        if (!gs.isEmpty()) {

                            String name = gs.getDocuments()
                                    .get(0)
                                    .getString("name");

                            if (name == null) return;

                            if (genreNames.length() > 0) genreNames.append(", ");
                            genreNames.append(name);

                            binding.movieGenres.setText(genreNames.toString());
                        }
                    });
        }
    }

    private void loadDirectors(List<String> directorIds) {

        List<Director> directors = new ArrayList<>();
        DirectorAdapter adapter = new DirectorAdapter(directors);

        binding.directorRecyclerView.setAdapter(adapter);

        for (String id : directorIds) {

            db.collection("directors")
                    .document(id)
                    .get()
                    .addOnSuccessListener(doc -> {

                        Director d = doc.toObject(Director.class);

                        if (d != null) {
                            directors.add(d);
                            adapter.notifyItemInserted(directors.size() - 1);
                        }
                    });
        }
    }

    private void loadActors(List<String> actorIds) {

        List<Actor> actors = new ArrayList<>();
        ActorAdapter adapter = new ActorAdapter(actors);

        binding.castRecyclerView.setAdapter(adapter);

        for (String id : actorIds) {

            db.collection("actors")
                    .document(id)
                    .get()
                    .addOnSuccessListener(doc -> {

                        Actor a = doc.toObject(Actor.class);

                        if (a != null) {
                            actors.add(a);
                            adapter.notifyItemInserted(actors.size() - 1);
                        }
                    });
        }
    }

    private void loadMpaaRating(String ratingId) {

        if (ratingId == null || ratingId.isEmpty()) return;

        db.collection("mpaa_ratings")
                .whereEqualTo("ratingId", ratingId)
                .get()
                .addOnSuccessListener(ratingSnap -> {

                    if (ratingSnap.isEmpty()) return;

                    String rating = ratingSnap.getDocuments()
                            .get(0)
                            .getString("value");

                    if (rating != null) {
                        binding.movieMpaaRating.setText(rating);
                    }
                });
    }

    private void renderPlotKeywords(List<String> keywordIds) {

        FlexboxLayout container = binding.keywordsContainer;
        container.removeAllViews();

        if (keywordIds == null || keywordIds.isEmpty()) {
            return;
        }

        for (String keywordId : keywordIds) {

            db.collection("plot_keywords")
                    .document(keywordId)
                    .get()
                    .addOnSuccessListener(doc -> {

                        if (doc.exists()) {

                            String keyword = doc.getString("keyword");

                            if (keyword == null) return;

                            TextView tv = new TextView(requireContext());
                            tv.setText(keyword);

                            tv.setTextColor(Color.parseColor("#CCFF00"));
                            tv.setTextSize(12);
                            tv.setBackgroundResource(R.drawable.keyword_bg);

                            int paddingH = (int) (10 * getResources().getDisplayMetrics().density);
                            int paddingV = (int) (4 * getResources().getDisplayMetrics().density);
                            tv.setPadding(paddingH, paddingV, paddingH, paddingV);

                            FlexboxLayout.LayoutParams params =
                                    new FlexboxLayout.LayoutParams(
                                            FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                            FlexboxLayout.LayoutParams.WRAP_CONTENT
                                    );

                            params.setMargins(0, 0, 16, 8);
                            tv.setLayoutParams(params);

                            container.addView(tv);
                        }
                    });
        }
    }

    private void setupTrailerButton(String trailerUrl) {

        if (trailerUrl == null || trailerUrl.isEmpty()) return;

        binding.btnWatchTrailer.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl));
            startActivity(intent);
        });
    }

    private void loadDvdDetails(String movieId) {

        db.collection("dvds")
                .whereEqualTo("movieId", movieId)
                .get()
                .addOnSuccessListener(qds -> {

                    if (qds.isEmpty()) return;

                    Dvd dvd = qds.getDocuments()
                            .get(0)
                            .toObject(Dvd.class);

                    if (dvd == null) return;

                    dvdId = dvd.getDvdId();
                    avbQuantity = dvd.getStockCount();

                    double price = dvd.getPrice();

                    binding.dvdPrice.setText("Rs " + price);
                    binding.tvTotalPrice.setText("Rs " + price);

                    binding.dvdStockCount.setText(String.valueOf(avbQuantity));

                    checkStockStatus(dvd);
                    setupQuantityControls(price);
                });
    }

    private void checkStockStatus(Dvd dvd) {

        if (dvd.getStockCount() <= 0) {

            binding.dvdStockStatus.setText(getString(R.string.single_movie_out_stock));
            binding.dvdStockStatus.setTextColor(Color.RED);

            disablePurchase();

            return;
        }

        db.collection("status")
                .whereEqualTo("statusId", dvd.getStatusId())
                .get()
                .addOnSuccessListener(qds -> {

                    if (!qds.isEmpty()) {

                        String status = qds.getDocuments()
                                .get(0)
                                .getString("value");

                        if ("Pending".equalsIgnoreCase(status)) {

                            binding.dvdStockStatus.setText(getString(R.string.single_movie_out_stock));
                            binding.dvdStockStatus.setTextColor(Color.RED);

                            disablePurchase();

                        } else {

                            binding.dvdStockStatus.setText(getString(R.string.single_movie_in_stock));
                            binding.dvdStockStatus.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }
                });
    }

    private void disablePurchase() {

        binding.btnIncreaseQty.setEnabled(false);
        binding.btnDecreaseQty.setEnabled(false);
        binding.btnAddToCart.setEnabled(false);

    }

    private void setupQuantityControls(double price) {

        quantity = 1;
        binding.tvQuantity.setText(String.valueOf(quantity));

        binding.btnDecreaseQty.setOnClickListener(v -> {

            if (quantity > 1) {
                quantity--;
                binding.tvQuantity.setText(String.valueOf(quantity));
                updateTotal(price);
            }
        });

        binding.btnIncreaseQty.setOnClickListener(v -> {

            if (quantity < avbQuantity) {
                quantity++;
                binding.tvQuantity.setText(String.valueOf(quantity));
                updateTotal(price);
            } else {
                binding.tvMaxStockWarning.setText(getString(R.string.max_stock_reached));
                binding.tvMaxStockWarning.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateTotal(double price) {

        double total = price * quantity;
        binding.tvTotalPrice.setText("Rs " + total);

        if (quantity < avbQuantity) {
            binding.tvMaxStockWarning.setVisibility(View.GONE);
        }
    }

    private void loadRelatedMovies(Movie currentMovie) {

        if (selectedGenreId == null) return;

        relatedMovies.clear();

        db.collection("movies")
                .whereArrayContains("genreIds", selectedGenreId)
                .get()
                .addOnSuccessListener(qds -> {

                    for (var doc : qds.getDocuments()) {

                        Movie movie = doc.toObject(Movie.class);

                        if (movie == null) continue;

                        if (!movie.getMovieId().equals(currentMovie.getMovieId())) {
                            relatedMovies.add(movie);
                        }
                    }

                    relatedAdapter.notifyDataSetChanged();
                });
    }

    private void setupAddToCartButton() {

        binding.btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void addToCart() {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {

            MotionToast.Companion.createColorToast(
                    requireActivity(),
                    "Login Required",
                    "Please sign in to add items to cart",
                    MotionToastStyle.WARNING,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.SHORT_DURATION,
                    Typeface.DEFAULT
            );

            startActivity(new Intent(getActivity(), SignInActivity.class));
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        if (dvdId == null) {
            Toast.makeText(getContext(), getString(R.string.please_wait), Toast.LENGTH_SHORT).show();
            return;
        }

        CartItem item = createCartItem();

        db.collection("users")
                .document(uid)
                .collection("cart")
                .document(dvdId)
                .set(item)
                .addOnSuccessListener(unused ->
                        MotionToast.Companion.createColorToast(
                                requireActivity(),
                                "Success",
                                "Added to cart!",
                                MotionToastStyle.SUCCESS,
                                MotionToast.GRAVITY_BOTTOM,
                                MotionToast.SHORT_DURATION,
                                Typeface.DEFAULT
                        )
                )
                .addOnFailureListener(e ->
                        MotionToast.Companion.createColorToast(
                                requireActivity(),
                                "Error",
                                "Failed to add to cart",
                                MotionToastStyle.ERROR,
                                MotionToast.GRAVITY_BOTTOM,
                                MotionToast.SHORT_DURATION,
                                Typeface.DEFAULT
                        )
                );
    }

    private CartItem createCartItem() {

        CartItem item = new CartItem();
        item.setDvdId(dvdId);
        item.setQuantity(quantity);

        return item;
    }

    private void setupTabs() {

        binding.tabOverview.setOnClickListener(v -> selectTab(0));
        binding.tabCast.setOnClickListener(v -> selectTab(1));
        binding.tabRelated.setOnClickListener(v -> selectTab(2));
        binding.tabBuyDvd.setOnClickListener(v -> selectTab(3));

        selectTab(0);
    }

    private void selectTab(int index) {

        binding.overviewContent.setVisibility(View.GONE);
        binding.castContent.setVisibility(View.GONE);
        binding.relatedContent.setVisibility(View.GONE);
        binding.buyDvdContent.setVisibility(View.GONE);

        binding.indicatorOverview.setVisibility(View.INVISIBLE);
        binding.indicatorCast.setVisibility(View.INVISIBLE);
        binding.indicatorRelated.setVisibility(View.INVISIBLE);
        binding.indicatorBuy.setVisibility(View.INVISIBLE);

        resetTabColors();

        switch (index) {
            case 0:
                binding.overviewContent.setVisibility(View.VISIBLE);
                binding.indicatorOverview.setVisibility(View.VISIBLE);
                binding.tabOverviewTitle.setTextColor(
                        requireContext().getColor(R.color.highlight_yellow_color)
                );
                break;

            case 1:
                binding.castContent.setVisibility(View.VISIBLE);
                binding.indicatorCast.setVisibility(View.VISIBLE);
                binding.tabCastTitle.setTextColor(
                        requireContext().getColor(R.color.highlight_yellow_color)
                );
                break;

            case 2:
                binding.relatedContent.setVisibility(View.VISIBLE);
                binding.indicatorRelated.setVisibility(View.VISIBLE);
                binding.tabRelatedTitle.setTextColor(
                        requireContext().getColor(R.color.highlight_yellow_color)
                );
                break;

            case 3:
                binding.buyDvdContent.setVisibility(View.VISIBLE);
                binding.indicatorBuy.setVisibility(View.VISIBLE);
                binding.tabBuyTitle.setTextColor(
                        requireContext().getColor(R.color.highlight_yellow_color)
                );
                break;
        }
    }

    private void resetTabColors() {

        int white = requireContext().getColor(android.R.color.white);

        binding.tabOverviewTitle.setTextColor(white);
        binding.tabCastTitle.setTextColor(white);
        binding.tabRelatedTitle.setTextColor(white);
        binding.tabBuyTitle.setTextColor(white);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);
    }
}