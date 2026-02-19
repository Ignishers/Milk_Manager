package com.ignishers.milkmanager2.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.ignishers.milkmanager2.model.Customer;
import com.ignishers.milkmanager2.model.DailyTransaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PDFGenerator {

    private Context context;
    private Customer customer;
    private List<DailyTransaction> transactions;
    private double initialOpeningBalance;
    private double headerTotalDue;

    // A4 Size in points (1/72 inch)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 30;
    
    // Formatting
    private Paint titlePaint, headerPaint, subHeaderPaint, textPaint, smallTextPaint, linePaint, watermarkPaint, amountPaint;
    private DateTimeFormatter monthHeaderFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    
    // Column Configuration
    private static final int COL_DATE = MARGIN;
    private static final int COL_MORN = MARGIN + 60;
    private static final int COL_EVE = MARGIN + 160;
    private static final int COL_EXTRA = MARGIN + 260;
    private static final int COL_PAY = MARGIN + 360;

    public PDFGenerator(Context context, Customer customer, List<DailyTransaction> transactions, double initialOpeningBalance, double headerTotalDue) {
        this.context = context;
        this.customer = customer;
        this.transactions = transactions;
        this.initialOpeningBalance = initialOpeningBalance;
        this.headerTotalDue = headerTotalDue;
        initPaints();
    }

    private void initPaints() {
        titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        headerPaint = new Paint();
        headerPaint.setColor(Color.BLACK);
        headerPaint.setTextSize(14);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        subHeaderPaint = new Paint();
        subHeaderPaint.setColor(Color.DKGRAY);
        subHeaderPaint.setTextSize(12);
        subHeaderPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(10);
        
        amountPaint = new Paint();
        amountPaint.setColor(Color.BLACK);
        amountPaint.setTextSize(10);
        amountPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        smallTextPaint = new Paint();
        smallTextPaint.setColor(Color.DKGRAY);
        smallTextPaint.setTextSize(9);

        linePaint = new Paint();
        linePaint.setColor(Color.LTGRAY);
        linePaint.setStrokeWidth(1);

        watermarkPaint = new Paint();
        watermarkPaint.setColor(Color.LTGRAY);
        watermarkPaint.setTextSize(40);
        watermarkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        watermarkPaint.setTextAlign(Paint.Align.CENTER);
        watermarkPaint.setAlpha(80); 
    }

    public File generate(String fileName) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();

        // 1. Pre-calculate Opening Balances for each month
        // We need a sorted copy to calculate rolling balance accurately
        List<DailyTransaction> sortedTrans = new ArrayList<>(transactions);
        sortedTrans.sort(Comparator.comparing(DailyTransaction::getDate));

        Map<String, List<DailyTransaction>> sortedGroup = new LinkedHashMap<>();
        for (DailyTransaction t : sortedTrans) {
            String monthKey = t.getDate().substring(0, 7);
            if (!sortedGroup.containsKey(monthKey)) {
                sortedGroup.put(monthKey, new ArrayList<>());
            }
            sortedGroup.get(monthKey).add(t);
        }

        Map<String, Double> monthStartBalances = new HashMap<>();
        double currentBal = initialOpeningBalance;
        
        for (Map.Entry<String, List<DailyTransaction>> entry : sortedGroup.entrySet()) {
            monthStartBalances.put(entry.getKey(), currentBal);
            
            // Calculate change for this month
            double bill = 0, pay = 0;
            for (DailyTransaction t : entry.getValue()) {
                 String s = t.getSession();
                 // Normalize session checks
                 if (s.startsWith("Payment")) pay += t.getAmount();
                 else bill += t.getAmount();
            }
            currentBal += (bill - pay);
        }

        // 2. Group Transactions for Display (Use original order to support LIFO)
        Map<String, List<DailyTransaction>> displayGroup = new LinkedHashMap<>();
        for (DailyTransaction t : transactions) {
            String monthKey = t.getDate().substring(0, 7);
            if (!displayGroup.containsKey(monthKey)) {
                displayGroup.put(monthKey, new ArrayList<>());
            }
            displayGroup.get(monthKey).add(t);
        }

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int y = MARGIN;
        int pageNumber = 1;

        // --- PAGE 1 MAIN HEADER ---
        y = drawMainHeader(canvas, y);
        y += 20;

        // Iterate Display Groups
        boolean isFirstMonth = true;
        for (Map.Entry<String, List<DailyTransaction>> entry : displayGroup.entrySet()) {
            String monthKey = entry.getKey();
            List<DailyTransaction> monthTrans = entry.getValue();
            
            // Start new page for new month (except first month)
            if (!isFirstMonth) {
                drawWatermark(canvas);
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                pageNumber++;
                y = MARGIN;
            }
            isFirstMonth = false;
            
            LocalDate monthDate = LocalDate.parse(monthKey + "-01");
            String monthTitle = monthDate.format(monthHeaderFormatter);

            // Calculate Totals for this month
            double sumMorn = 0, sumEve = 0, sumExtra = 0, sumPay = 0;
            double sumMornQty = 0, sumEveQty = 0, sumExtraQty = 0;
            
            for (DailyTransaction t : monthTrans) {
                 String s = t.getSession();
                 double amt = t.getAmount();
                 double qty = t.getQuantity();
                 
                 if (s.equalsIgnoreCase("Morning")) {
                     sumMorn += amt;
                     sumMornQty += qty;
                 } else if (s.equalsIgnoreCase("Evening")) {
                     sumEve += amt;
                     sumEveQty += qty;
                 } else if (s.startsWith("Payment")) {
                     sumPay += amt;
                 } else {
                     sumExtra += amt;
                     sumExtraQty += qty;
                 }
            }

            // Draw Month Header
            canvas.drawText(monthTitle, MARGIN, y, headerPaint);
            y += 20;

            // Draw Previous Dues from Pre-calculated Map
            Double startBal = monthStartBalances.getOrDefault(monthKey, 0.0);
            String prevDueText = String.format("Previous Dues: %.2f", startBal);
            Paint leftAlignSub = new Paint(subHeaderPaint);
            leftAlignSub.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(prevDueText, MARGIN, y, leftAlignSub);
            y += 15;
            
            // Draw Payment Received
            String payRecText = String.format("Payment Received: %.2f", sumPay);
            canvas.drawText(payRecText, MARGIN, y, leftAlignSub);
            y += 25;

            // Draw Table Header
            drawTableHeader(canvas, y);
            y += 5; 
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
            y += 15;

            // Group by Day
            Map<String, List<DailyTransaction>> dayGroup = new LinkedHashMap<>();
            for (DailyTransaction t : monthTrans) {
                if (!dayGroup.containsKey(t.getDate())) dayGroup.put(t.getDate(), new ArrayList<>());
                dayGroup.get(t.getDate()).add(t);
            }

            // Iterate Days
            for (Map.Entry<String, List<DailyTransaction>> dayEntry : dayGroup.entrySet()) {
                 String dateStr = dayEntry.getKey(); 
                 List<DailyTransaction> dayTrans = dayEntry.getValue();
                 
                 String mornQ="", mornA="", eveQ="", eveA="", extraQ="", extraA="", payA="", payNote="";
                 
                 // Accumulators for aggregation
                 double sumMornQ = 0, sumMornA = 0;
                 double sumEveQ = 0, sumEveA = 0;
                 double sumExtraQ = 0, sumExtraA = 0;
                 double sumPayA = 0;
                 java.util.List<String> payModes = new java.util.ArrayList<>();

                 // Aggregate transactions for the day
                 for (DailyTransaction t : dayTrans) {
                     String s = t.getSession();
                     double amt = t.getAmount();
                     double qty = t.getQuantity();
                     
                     if (s.equalsIgnoreCase("Morning")) {
                         sumMornQ += qty;
                         sumMornA += amt;
                     } else if (s.equalsIgnoreCase("Evening")) {
                         sumEveQ += qty;
                         sumEveA += amt;
                     } else if (s.startsWith("Payment")) {
                         sumPayA += amt;
                         String pm = t.getPaymentMode();
                         if (pm != null && !pm.isEmpty() && !payModes.contains(pm)) {
                             payModes.add(pm);
                         }
                     } else {
                         sumExtraQ += qty;
                         sumExtraA += amt;
                     }
                 }

                 // Format aggregated values
                 if (sumMornQ > 0 || sumMornA > 0) {
                     mornQ = formatMilk(sumMornQ);
                     mornA = String.format("%.2f", sumMornA);
                 }
                 if (sumEveQ > 0 || sumEveA > 0) {
                     eveQ = formatMilk(sumEveQ);
                     eveA = String.format("%.2f", sumEveA);
                 }
                 if (sumExtraQ > 0 || sumExtraA > 0) {
                     extraQ = formatMilk(sumExtraQ);
                     extraA = String.format("%.2f", sumExtraA);
                 }
                 if (sumPayA > 0) {
                     payA = String.format("- %.2f", sumPayA);
                     if (!payModes.isEmpty()) {
                         StringBuilder sb = new StringBuilder("(");
                         for (int i = 0; i < payModes.size(); i++) {
                             sb.append(payModes.get(i));
                             if (i < payModes.size() - 1) sb.append(", ");
                         }
                         sb.append(")");
                         payNote = sb.toString();
                     }
                 }
                 
                 // Check Page Break
                 if (y + 35 > PAGE_HEIGHT - MARGIN) {
                     drawWatermark(canvas);
                     document.finishPage(page);
                     page = document.startPage(pageInfo);
                     canvas = page.getCanvas();
                     pageNumber++;
                     y = MARGIN;
                     
                     canvas.drawText(monthTitle + " (Continued)", MARGIN, y, headerPaint);
                     y += 25;
                     drawTableHeader(canvas, y);
                     y += 5;
                     canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
                     y += 15;
                 }
                 
                 // Draw Row
                 String dayNum = dateStr.substring(8); 
                 canvas.drawText(dayNum, COL_DATE, y, textPaint);
                 
                 if (!mornQ.isEmpty()) {
                     canvas.drawText(mornQ, COL_MORN, y, textPaint);
                     canvas.drawText(mornA, COL_MORN, y + 12, amountPaint);
                 }
                 if (!eveQ.isEmpty()) {
                     canvas.drawText(eveQ, COL_EVE, y, textPaint);
                     canvas.drawText(eveA, COL_EVE, y + 12, amountPaint);
                 }
                 if (!extraQ.isEmpty()) {
                     canvas.drawText(extraQ, COL_EXTRA, y, textPaint);
                     canvas.drawText(extraA, COL_EXTRA, y + 12, amountPaint);
                 }
                 if (!payA.isEmpty()) {
                     canvas.drawText(payA, COL_PAY, y + 6, amountPaint);
                     if (!payNote.isEmpty()) canvas.drawText(payNote, COL_PAY, y + 18, smallTextPaint); 
                 }
                 
                 y += 10; 
                 y += 8; 
                 canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint); 
                 y += 12; 
            }
            
            // --- MONTH FOOTER ---
            y-=11;
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
            y+=2;
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
            y+=1;
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
            y += 12;
            
            if (y + 100 > PAGE_HEIGHT - MARGIN) {
                 drawWatermark(canvas);
                 document.finishPage(page);
                 page = document.startPage(pageInfo);
                 canvas = page.getCanvas();
                 pageNumber++;
                 y = MARGIN;
            }
            
            canvas.drawText("Total", COL_DATE, y, amountPaint);
            
            // Quantity checks need to be robust
            if (sumMorn > 0 || sumMornQty > 0) {
                canvas.drawText(formatMilk(sumMornQty), COL_MORN, y, textPaint);
                canvas.drawText(String.format("%.2f", sumMorn), COL_MORN, y + 12, amountPaint);
            }
            if (sumEve > 0 || sumEveQty > 0) {
                canvas.drawText(formatMilk(sumEveQty), COL_EVE, y, textPaint);
                canvas.drawText(String.format("%.2f", sumEve), COL_EVE, y + 12, amountPaint);
            }
            if (sumExtra > 0 || sumExtraQty > 0) {
                canvas.drawText(formatMilk(sumExtraQty), COL_EXTRA, y, textPaint);
                canvas.drawText(String.format("%.2f", sumExtra), COL_EXTRA, y + 12, amountPaint);
            }
            if (sumPay > 0) {
                canvas.drawText(String.format("%.2f", sumPay), COL_PAY, y + 12, amountPaint);
            }
            
            y += 35;
            
            double thisMonthBill = sumMorn + sumEve + sumExtra;
            double thisMonthMilk = sumMornQty + sumEveQty + sumExtraQty;
            
            Paint leftAlignSummary = new Paint(subHeaderPaint);
            leftAlignSummary.setTextAlign(Paint.Align.LEFT);
            
            canvas.drawText("This Month Milk: " + formatMilk(thisMonthMilk), MARGIN, y, leftAlignSummary);
            y += 20;
            canvas.drawText("Total Rs: " + String.format("%.2f", thisMonthBill), MARGIN, y, leftAlignSummary);
            
            y += 40; 
        }

        drawWatermark(canvas);
        document.finishPage(page);

        // Save
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, fileName);
        
        int counter = 1;
        String baseName = fileName.endsWith(".pdf") ? fileName.substring(0, fileName.length() - 4) : fileName;
        
        while (file.exists()) {
            file = new File(downloadsDir, baseName + "(" + counter + ").pdf");
            counter++;
        }

        try {
            document.writeTo(new FileOutputStream(file));
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            document.close();
        }
    }
    
    private int drawMainHeader(Canvas canvas, int y) {
        canvas.drawText("Milk Manager 2", PAGE_WIDTH / 2, y + 20, titlePaint);
        y += 60;
        
        canvas.drawText("Customer: " + customer.name, MARGIN, y, textPaint);
        y += 15;
        canvas.drawText("Mobile: " + customer.mobile, MARGIN, y, textPaint);
        y += 15;
        String addr = (customer.address == null || customer.address.isEmpty()) ? "N/A" : customer.address;
        canvas.drawText("Address: " + addr, MARGIN, y, textPaint);
        
        // Use passed headerTotalDue
        String dueStr = String.format("Total Due: %.2f", headerTotalDue);
        Paint boldP = new Paint(textPaint);
        boldP.setTypeface(Typeface.DEFAULT_BOLD);
        boldP.setTextSize(14);
        boldP.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(dueStr, PAGE_WIDTH - MARGIN, y, boldP);
        
        y += 20;
        Paint thick = new Paint(linePaint);
        thick.setStrokeWidth(2);
        thick.setColor(Color.DKGRAY);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, thick);
        
        return y;
    }
    
    private void drawTableHeader(Canvas canvas, int y) {
        canvas.drawText("Date", COL_DATE, y, subHeaderPaint);
        canvas.drawText("Morning", COL_MORN, y, subHeaderPaint);
        canvas.drawText("Evening", COL_EVE, y, subHeaderPaint);
        canvas.drawText("Extra", COL_EXTRA, y, subHeaderPaint);
        canvas.drawText("Payment", COL_PAY, y, subHeaderPaint);
    }
    
    private void drawWatermark(Canvas canvas) {
        canvas.drawText("IGNISHERS", PAGE_WIDTH / 2, PAGE_HEIGHT - 30, watermarkPaint);
    }
    
    private String formatMilk(double qty) {
        if (qty >= 1.0) {
            return String.format("%.1f L", qty);
        } else {
            return String.format("%d ml", (int)(qty * 1000));
        }
    }
}
