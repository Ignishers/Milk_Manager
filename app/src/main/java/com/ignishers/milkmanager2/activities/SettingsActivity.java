package com.ignishers.milkmanager2.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.utils.GoogleDriveHelper;

/**
 * Settings screen — currently hosts Google Drive Backup and Restore.
 *
 * <p><b>OAuth flow:</b>
 * <ol>
 *   <li>User taps Sign In → Google chooser opens.</li>
 *   <li>On success, account is cached; Backup and Restore buttons become enabled.</li>
 *   <li>Backup → uploads {@code milky_mist_db} to Drive folder.</li>
 *   <li>Restore → downloads it back and replaces local DB; app must restart.</li>
 * </ol>
 * </p>
 */
public class SettingsActivity extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private GoogleSignInAccount signedInAccount;
    private Drive driveService;

    private Button btnSignIn, btnBackup, btnRestore;
    private TextView tvAccountStatus, tvLastBackup;
    private LinearLayout progressOverlay;
    private TextView tvProgressMsg;

    // ── Activity-result launcher for Google Sign-In ──
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() != null) {
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                    .addOnSuccessListener(account -> onSignInSuccess(account))
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                "Sign-in failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Views
        btnSignIn       = findViewById(R.id.btnGoogleSignIn);
        btnBackup       = findViewById(R.id.btnBackup);
        btnRestore      = findViewById(R.id.btnRestore);
        tvAccountStatus = findViewById(R.id.tvGoogleAccountStatus);
        tvLastBackup    = findViewById(R.id.tvLastBackup);
        progressOverlay = findViewById(R.id.progressOverlay);
        tvProgressMsg   = findViewById(R.id.tvProgressMsg);

        // Version info
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) findViewById(R.id.tvVersion)).setText("Version " + versionName);
        } catch (Exception ignored) {}

        // Configure Google Sign-In with Drive.File scope
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check if already signed in
        GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (lastAccount != null &&
                GoogleSignIn.hasPermissions(lastAccount, new Scope(DriveScopes.DRIVE_FILE))) {
            onSignInSuccess(lastAccount);
        }

        // Buttons
        btnSignIn.setOnClickListener(v -> launchSignIn());
        btnBackup.setOnClickListener(v -> confirmBackup());
        btnRestore.setOnClickListener(v -> confirmRestore());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ──────────────────────────────────────────────
    //  Sign-In
    // ──────────────────────────────────────────────

    private void launchSignIn() {
        // Silent sign-in first; if it fails, show chooser
        googleSignInClient.silentSignIn()
                .addOnSuccessListener(account -> onSignInSuccess(account))
                .addOnFailureListener(e ->
                        signInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void onSignInSuccess(GoogleSignInAccount account) {
        signedInAccount  = account;
        driveService     = GoogleDriveHelper.buildDriveService(this, account);

        String email = account.getEmail() != null ? account.getEmail() : account.getDisplayName();
        tvAccountStatus.setText("Signed in as: " + email);
        btnSignIn.setText("Switch Account");
        btnBackup.setEnabled(true);
        btnRestore.setEnabled(true);
    }

    // ──────────────────────────────────────────────
    //  Backup
    // ──────────────────────────────────────────────

    private void confirmBackup() {
        new AlertDialog.Builder(this)
                .setTitle("Backup to Google Drive")
                .setMessage("Upload your current data to Google Drive?\n\nAny existing backup will be replaced.")
                .setPositiveButton("Backup", (d, w) -> doBackup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doBackup() {
        showProgress("Backing up to Google Drive…");
        GoogleDriveHelper.backup(this, driveService, new GoogleDriveHelper.DriveCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    hideProgress();
                    tvLastBackup.setText("Last backup: just now");
                    showSuccessDialog("Backup Complete", message);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showErrorDialog("Backup Failed", error);
                });
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Restore
    // ──────────────────────────────────────────────

    private void confirmRestore() {
        new AlertDialog.Builder(this)
                .setTitle("⚠ Restore from Google Drive")
                .setMessage("This will REPLACE all current data with the backup.\n\n" +
                            "The app will need to restart after restore.\n\nContinue?")
                .setPositiveButton("Restore", (d, w) -> doRestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doRestore() {
        showProgress("Restoring from Google Drive…");
        GoogleDriveHelper.restore(this, driveService, new GoogleDriveHelper.DriveCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    hideProgress();
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("Restore Complete")
                            .setMessage(message)
                            .setPositiveButton("Restart Now", (d, w) -> restartApp())
                            .setCancelable(false)
                            .show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    hideProgress();
                    showErrorDialog("Restore Failed", error);
                });
            }
        });
    }

    /** Kill and relaunch so the restored DB is loaded fresh. */
    private void restartApp() {
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    // ──────────────────────────────────────────────
    //  UI helpers
    // ──────────────────────────────────────────────

    private void showProgress(String msg) {
        tvProgressMsg.setText(msg);
        progressOverlay.setVisibility(View.VISIBLE);
        btnBackup.setEnabled(false);
        btnRestore.setEnabled(false);
    }

    private void hideProgress() {
        progressOverlay.setVisibility(View.GONE);
        btnBackup.setEnabled(true);
        btnRestore.setEnabled(true);
    }

    private void showSuccessDialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle("✓ " + title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle("✗ " + title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}
