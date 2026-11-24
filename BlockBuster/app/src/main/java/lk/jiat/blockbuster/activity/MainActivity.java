package lk.jiat.blockbuster.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.blockbuster.R;
import lk.jiat.blockbuster.adapter.MovieSearchAdapter;
import lk.jiat.blockbuster.databinding.ActivityMainBinding;
import lk.jiat.blockbuster.databinding.SideNavHeaderBinding;
import lk.jiat.blockbuster.fragment.AboutFragment;
import lk.jiat.blockbuster.fragment.CartFragment;
import lk.jiat.blockbuster.fragment.GenreFragment;
import lk.jiat.blockbuster.fragment.HomeFragment;
import lk.jiat.blockbuster.fragment.MapFragment;
import lk.jiat.blockbuster.fragment.MessageFragment;
import lk.jiat.blockbuster.fragment.OrdersFragment;
import lk.jiat.blockbuster.fragment.ProfileFragment;
import lk.jiat.blockbuster.fragment.SettingsFragment;
import lk.jiat.blockbuster.fragment.SingleMovieFragment;
import lk.jiat.blockbuster.fragment.WishlistFragment;
import lk.jiat.blockbuster.model.Movie;
import lk.jiat.blockbuster.model.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth.AuthStateListener authStateListener;
    private AutoCompleteTextView searchInput;
    private MovieSearchAdapter searchAdapter;
    private List<Movie> movieList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyTheme();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.sideNavigationView.getHeaderView(0);
        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;

        searchInput = findViewById(R.id.text_input_search);

        //toggle action
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Close drawer or exit on back and even for other fragments
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    } else {
                        finish();
                    }
                }
            }
        });

        // Badge example for cart
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.bottom_nav_cart);
        badge.setVisible(true);
        badge.setNumber(3);
        badge.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_onErrorContainer));
        badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.md_theme_onError));

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setupSearch();
        loadMoviesForSearch();

        authStateListener = firebaseAuth -> {
            refreshDrawerHeader();
            updateNavItemsBasedOnLogin();
        };

    }

    private void applyTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme_mode", "light");

        if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public void refreshDrawerHeader() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            sideNavHeaderBinding.headerUserName.setText(R.string.user_not_logged_in);
            sideNavHeaderBinding.headerUserEmail.setText("");
            sideNavHeaderBinding.headerProfilePic.setImageResource(R.drawable.contacts_product_24px);
            return;
        }

        firebaseFirestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(ds -> {
                    if (ds.exists()) {
                        User user = ds.toObject(User.class);

                        if (user == null) {
                            sideNavHeaderBinding.headerUserName.setText(R.string.user_not_logged_in);
                            sideNavHeaderBinding.headerUserEmail.setText("");
                            sideNavHeaderBinding.headerProfilePic.setImageResource(R.drawable.contacts_product_24px);
                            return;
                        }

                        sideNavHeaderBinding.headerUserName.setText(
                                user.getFullName() != null ? user.getFullName() : getString(R.string.user_not_logged_in)
                        );

                        sideNavHeaderBinding.headerUserEmail.setText(
                                user.getEmail() != null ? user.getEmail() : ""
                        );

                        String profilePic = user.getProfilePicUrl();

                        if (profilePic != null && !profilePic.isEmpty()) {

                            if (isDestroyed() || isFinishing()) return;

                            if (sideNavHeaderBinding != null &&
                                    sideNavHeaderBinding.headerProfilePic != null &&
                                    sideNavHeaderBinding.headerProfilePic.getContext() != null) {

                                Glide.with(getApplicationContext())
                                        .load(profilePic)
                                        .circleCrop()
                                        .into(sideNavHeaderBinding.headerProfilePic);
                            }

                        } else {
                            if (sideNavHeaderBinding != null) {
                                sideNavHeaderBinding.headerProfilePic.setImageResource(R.drawable.contacts_product_24px);
                            }
                        }

                    } else {
                        Log.e("Firestore", "Document does not exist");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error: " + e.getMessage()));
    }

    private void updateNavItemsBasedOnLogin() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        boolean loggedIn = currentUser != null;

        navigationView.getMenu().findItem(R.id.side_nav_login).setVisible(!loggedIn);
        navigationView.getMenu().findItem(R.id.side_nav_profile).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.side_nav_orders).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.side_nav_wishlist).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.side_nav_cart).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.side_nav_message).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.side_nav_logout).setVisible(loggedIn);

        bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setEnabled(loggedIn);
        bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile).setEnabled(loggedIn);

        int alpha = loggedIn ? 255 : 130;
        MenuItem cartItem = bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart);
        MenuItem profileItem = bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile);

        if (cartItem != null && cartItem.getIcon() != null) {
            cartItem.getIcon().setAlpha(alpha);
        }

        if (profileItem != null && profileItem.getIcon() != null) {
            profileItem.getIcon().setAlpha(alpha);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        Menu navMenu = navigationView.getMenu();
        Menu bottomNavMenu = bottomNavigationView.getMenu();

        // Uncheck all items
        for (int i = 0; i < navMenu.size(); i++) {
            navMenu.getItem(i).setChecked(false);
        }
        for (int i = 0; i < bottomNavMenu.size(); i++) {
            bottomNavMenu.getItem(i).setChecked(false);
        }

        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home){

            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);

        }else if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile){

            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, SignInActivity.class));
                return true;
            }

            loadFragment(new ProfileFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_profile).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile).setChecked(true);

        }else if (itemId == R.id.side_nav_orders) {

            loadFragment(new OrdersFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_orders).setChecked(true);

        }else if (itemId == R.id.side_nav_wishlist) {

            loadFragment(new WishlistFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_wishlist).setChecked(true);

        }else if (itemId == R.id.side_nav_cart || itemId == R.id.bottom_nav_cart) {

            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, SignInActivity.class));
                return true;
            }

            loadFragment(new CartFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_cart).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setChecked(true);

        }else if (itemId == R.id.side_nav_message) {

            loadFragment(new MessageFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_message).setChecked(true);

        } else if (itemId == R.id.side_nav_settings) {

            loadFragment(new SettingsFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_settings).setChecked(true);

        } else if (itemId == R.id.side_nav_about) {

            loadFragment(new AboutFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_about).setChecked(true);

        } else if (itemId == R.id.side_nav_map) {

            loadFragment(new MapFragment(), true);
            navigationView.getMenu().findItem(R.id.side_nav_map).setChecked(true);

        }else if (itemId == R.id.bottom_nav_category) {

            loadFragment(new GenreFragment(), true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_category).setChecked(true);

        }else if (itemId == R.id.side_nav_login) {

            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);

        }else if (itemId == R.id.side_nav_logout) {

            firebaseAuth.signOut();

            loadFragment(new HomeFragment());

            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);

        }

        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
        }

        return true;
    }

    private void setupSearch() {

        searchAdapter = new MovieSearchAdapter(this, movieList);
        searchAdapter.setShowQuickAction(true);

        searchInput.setAdapter(searchAdapter);
        searchInput.setThreshold(2);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnItemClickListener((parent, view, position, id) -> {

            Movie selectedMovie = (Movie) parent.getItemAtPosition(position);

            openMovieDetails(selectedMovie);

            searchInput.setText("");
        });

        searchAdapter.setOnQuickActionClickListener(movie -> {
            // TODO: add to cart logic
        });
    }

    private void loadMoviesForSearch() {

        firebaseFirestore.collection("movies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    movieList.clear();

                    for (var doc : queryDocumentSnapshots) {
                        Movie movie = doc.toObject(Movie.class);
                        movieList.add(movie);
                    }

                    searchAdapter.updateMovies(movieList);
                });
    }

    private void openMovieDetails(Movie movie) {

        Bundle bundle = new Bundle();
        bundle.putString("movieId", movie.getMovieId());

        Fragment fragment = new SingleMovieFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private void loadFragment(Fragment fragment) {
        loadFragment(fragment, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

}