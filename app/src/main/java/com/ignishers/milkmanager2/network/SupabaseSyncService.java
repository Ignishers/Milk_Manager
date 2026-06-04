package com.ignishers.milkmanager2.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseSyncService {

    @POST("rest/v1/customers")
    Call<Void> upsertCustomers(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Header("Prefer") String prefer,
            @Body List<Object> customers
    );

    @POST("rest/v1/milk_transactions")
    Call<Void> upsertTransactions(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Header("Prefer") String prefer,
            @Body List<Object> transactions
    );

    @POST("rest/v1/route_groups")
    Call<Void> upsertRoutes(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Header("Prefer") String prefer,
            @Body List<Object> routes
    );

    @POST("rest/v1/payments")
    Call<Void> upsertPayments(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Header("Prefer") String prefer,
            @Body List<Object> payments
    );

    @POST("rest/v1/milk_prices")
    Call<Void> upsertPrices(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Header("Prefer") String prefer,
            @Body List<Object> prices
    );
}
