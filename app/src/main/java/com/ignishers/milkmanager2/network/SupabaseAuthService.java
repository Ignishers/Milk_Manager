package com.ignishers.milkmanager2.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SupabaseAuthService {
    
    // Represents a row in our Supabase "sellers" table
    class SellerRecord {
        public String seller_id;
        public String password_hash;
    }

    @GET("rest/v1/sellers")
    Call<List<SellerRecord>> getSeller(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Query("seller_id") String sellerId,
            @Query("select") String selectFields
    );
}
