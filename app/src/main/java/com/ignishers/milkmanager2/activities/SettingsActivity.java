package com.ignishers.milkmanager2.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.utils.CsvExportManager;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private Button btnExportCsv;
    private LinearLayout progressOverlay;
    private TextView tvProgressMsg;

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
        btnExportCsv    = findViewById(R.id.btnExportCsv);
        progressOverlay = findViewById(R.id.progressOverlay);
        tvProgressMsg   = findViewById(R.id.tvProgressMsg);

        // Version info
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) findViewById(R.id.tvVersion)).setText("Version " + versionName);
        } catch (Exception ignored) {}

        // Buttons
        btnExportCsv.setOnClickListener(v -> exportDataToCsv());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void exportDataToCsv() {
        showProgress("Generating CSV files...");
        CsvExportManager.exportDatabaseToCsv(this, new CsvExportManager.ExportCallback() {
            @Override
            public void onSuccess(ArrayList<Uri> fileUris) {
                hideProgress();
                
                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.setType("text/csv");
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                startActivity(Intent.createChooser(intent, "Save CSV Backup via"));
            }

            @Override
            public void onFailure(String error) {
                hideProgress();
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Export Failed")
                        .setMessage(error)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void showProgress(String msg) {
        tvProgressMsg.setText(msg);
        progressOverlay.setVisibility(View.VISIBLE);
        btnExportCsv.setEnabled(false);
    }

    private void hideProgress() {
        progressOverlay.setVisibility(View.GONE);
        btnExportCsv.setEnabled(true);
    }
}
