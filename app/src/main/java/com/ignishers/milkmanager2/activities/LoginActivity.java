package com.ignishers.milkmanager2.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.managers.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        if (sessionManager.isLoggedIn()) {
            // Already logged in
            etEmail.setText(sessionManager.getEmail());
            etEmail.setEnabled(false);
            etPassword.setVisibility(View.GONE);
            btnLogin.setText("Logout");
            btnLogin.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            btnLogin.setOnClickListener(v -> performLogout());
        } else {
            btnLogin.setOnClickListener(v -> performLogin());
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both Email/Seller ID and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Retrofit setup
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(com.ignishers.milkmanager2.network.SupabaseConfig.SUPABASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();

        com.ignishers.milkmanager2.network.SupabaseAuthService service = retrofit.create(com.ignishers.milkmanager2.network.SupabaseAuthService.class);
        
        String authHeader = "Bearer " + com.ignishers.milkmanager2.network.SupabaseConfig.SUPABASE_KEY;
        String select = "seller_id,password_hash";

        service.getSeller(com.ignishers.milkmanager2.network.SupabaseConfig.SUPABASE_KEY, authHeader, "eq." + email, select)
                .enqueue(new retrofit2.Callback<java.util.List<com.ignishers.milkmanager2.network.SupabaseAuthService.SellerRecord>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.List<com.ignishers.milkmanager2.network.SupabaseAuthService.SellerRecord>> call, retrofit2.Response<java.util.List<com.ignishers.milkmanager2.network.SupabaseAuthService.SellerRecord>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            com.ignishers.milkmanager2.network.SupabaseAuthService.SellerRecord seller = response.body().get(0);
                            
                            // BCrypt Verification
                            try {
                                if (org.mindrot.jbcrypt.BCrypt.checkpw(password, seller.password_hash)) {
                                    sessionManager.createLoginSession(seller.seller_id, seller.seller_id);
                                    Toast.makeText(LoginActivity.this, "Successfully logged in! Sync enabled.", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(LoginActivity.this, "Hash Verification Error", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Seller ID not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<java.util.List<com.ignishers.milkmanager2.network.SupabaseAuthService.SellerRecord>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performLogout() {
        sessionManager.logout();
        Toast.makeText(this, "Logged out. Cloud Sync disabled.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
