package com.ignishers.milkmanager2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.ignishers.milkmanager2.DAO.DailyTransactionDAO;
import com.ignishers.milkmanager2.views.SimpleBarView;
import com.ignishers.milkmanager2.views.SimplePieView;
import com.google.android.material.tabs.TabLayout;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesReportActivity extends AppCompatActivity {

    private DailyTransactionDAO dao;
    private TextView tvTotalMilk, tvTotalRevenue, tvTotalCollected, tvTotalDue;
    private RecyclerView rvMonthlyReport;
    private Spinner spinnerYear;
    private MonthlyReportAdapter adapter;

    private static final int CREATE_PDF_REQUEST_CODE = 42;

    private View layoutOverview, layoutTrends, layoutCustomers;
    private SimpleBarView chartYearlyTrend, chartMonthlyTrend;
    private SimplePieView chartCustomerPie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_sales_report);

            dao = new DailyTransactionDAO(this);

            // Toolbar
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());

            // Bind Views
            tvTotalMilk = findViewById(R.id.tvTotalMilkLifetime);
            tvTotalRevenue = findViewById(R.id.tvTotalRevenueLifetime);
            tvTotalCollected = findViewById(R.id.tvTotalCollectedLifetime);
            tvTotalDue = findViewById(R.id.tvTotalDueLifetime);
            
            rvMonthlyReport = findViewById(R.id.rvMonthlyReport);
            spinnerYear = findViewById(R.id.spinnerReportYear);

            layoutOverview = findViewById(R.id.layoutOverview);
            layoutTrends = findViewById(R.id.layoutTrends);
            layoutCustomers = findViewById(R.id.layoutCustomers);
            
            chartYearlyTrend = findViewById(R.id.chartYearlyTrend);
            chartMonthlyTrend = findViewById(R.id.chartMonthlyTrend);
            chartCustomerPie = findViewById(R.id.chartCustomerPie);

            // Setup RecyclerView
            rvMonthlyReport.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MonthlyReportAdapter();
            rvMonthlyReport.setAdapter(adapter);

            // Setup Tabs
            TabLayout tabLayout = findViewById(R.id.tabLayout);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateTabVisibility(tab.getPosition());
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });

            // Load Data
            loadLifetimeStats();
            setupYearSpinner();
            loadGraphs();
            
            // PDF Button
            findViewById(R.id.fabDownloadPdf).setOnClickListener(v -> createPdf());
            
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Error opening report: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void updateTabVisibility(int position) {
        layoutOverview.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        layoutTrends.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        layoutCustomers.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }
    
    private void loadGraphs() {
        // 1. Customer Pie
        List<DailyTransactionDAO.CustomerSalesItem> customers = dao.getCustomerSalesRanking(5);
        List<SimplePieView.PieItem> pieData = new ArrayList<>();
        for (DailyTransactionDAO.CustomerSalesItem c : customers) {
            String name = c.customerName != null ? c.customerName : "Unknown";
            pieData.add(new SimplePieView.PieItem(name, (float) c.totalSpent));
        }
        chartCustomerPie.setData(pieData);
        
        // 2. Yearly Trend Bar
        List<DailyTransactionDAO.YearlyReportItem> yearly = dao.getYearlyRevenueTrend(5);
        List<SimpleBarView.BarItem> barData = new ArrayList<>();
        for (DailyTransactionDAO.YearlyReportItem y : yearly) {
            barData.add(new SimpleBarView.BarItem(String.valueOf(y.year), (int) y.totalRevenue));
        }
        chartYearlyTrend.setData(barData);
        
        // 3. Monthly Trend Bar (For Current Year)
        // Re-use monthly breakdown logic
        int currentYear = LocalDate.now().getYear();
        List<DailyTransactionDAO.MonthlyReportItem> monthly = dao.getMonthlyBreakdown(currentYear);
        List<SimpleBarView.BarItem> monthBarData = new ArrayList<>();
        String[] shortMonths = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (DailyTransactionDAO.MonthlyReportItem m : monthly) {
            if (m.month >= 1 && m.month <= 12) {
                monthBarData.add(new SimpleBarView.BarItem(shortMonths[m.month], (int) m.amount));
            }
        }
        chartMonthlyTrend.setData(monthBarData);
    }

    private void loadLifetimeStats() {
        DailyTransactionDAO.ReportSummary summary = dao.getLifetimeSummary();
        tvTotalMilk.setText(String.format(Locale.getDefault(), "%.1f L", summary.totalMilk));
        tvTotalRevenue.setText(String.format(Locale.getDefault(), "₹ %.0f", summary.totalRevenue));
        tvTotalCollected.setText(String.format(Locale.getDefault(), "₹ %.0f", summary.totalCollected));
        tvTotalDue.setText(String.format(Locale.getDefault(), "₹ %.0f", summary.totalDue));
    }

    private void setupYearSpinner() {
        // Simple 10 year range ending at current year
        int currentYear = LocalDate.now().getYear();
        List<String> years = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            years.add(String.valueOf(currentYear - i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(adapter);

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedYear = Integer.parseInt(years.get(position));
                loadMonthlyReport(selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadMonthlyReport(int year) {
        List<DailyTransactionDAO.MonthlyReportItem> list = dao.getMonthlyBreakdown(year);
        adapter.submitList(list);
    }
    
    // ----------------------------------------------------------------------
    // PDF Logic
    // ----------------------------------------------------------------------
    private void createPdf() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "Milk_Sales_Report.pdf");
        startActivityForResult(intent, CREATE_PDF_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_PDF_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                writePdfToUri(data.getData());
            }
        }
    }

    private void writePdfToUri(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                
                // Create PDF Document
                PdfDocument document = new PdfDocument();
                
                // Page Info
                // A4 size in points (approx 595 x 842)
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                
                // Start Page
                PdfDocument.Page page = document.startPage(pageInfo);
                
                // Draw logic (Simple Screenshot of the scroll view content for now, or custom draw)
                // For simplicity and effectiveness, let's draw the View content
                View content = findViewById(android.R.id.content);
                content.draw(page.getCanvas());
                
                document.finishPage(page);
                
                // Write
                document.writeTo(fileOutputStream);
                document.close();
                fileOutputStream.close();
                pfd.close();
                
                Toast.makeText(this, "PDF Saved Successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    // ----------------------------------------------------------------------
    // Adapter
    // ----------------------------------------------------------------------
    private static class MonthlyReportAdapter extends RecyclerView.Adapter<MonthlyReportAdapter.ViewHolder> {
        
        private final List<DailyTransactionDAO.MonthlyReportItem> items = new ArrayList<>();
        private final String[] monthNames = {"", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

        public void submitList(List<DailyTransactionDAO.MonthlyReportItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_month, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DailyTransactionDAO.MonthlyReportItem item = items.get(position);
            
            String name = (item.month >= 1 && item.month <= 12) ? monthNames[item.month] : "Unknown";
            holder.tvName.setText(name);
            holder.tvMilk.setText(String.format(Locale.getDefault(), "%.1f L", item.milk));
            holder.tvAmount.setText(String.format(Locale.getDefault(), "₹ %.0f", item.amount));
            holder.tvCollected.setText(String.format(Locale.getDefault(), "₹ %.0f", item.collected));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvMilk, tvAmount, tvCollected;
            public ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvMonthName);
                tvMilk = itemView.findViewById(R.id.tvMonthMilk);
                tvAmount = itemView.findViewById(R.id.tvMonthAmount);
                tvCollected = itemView.findViewById(R.id.tvMonthCollected);
            }
        }
    }
}
