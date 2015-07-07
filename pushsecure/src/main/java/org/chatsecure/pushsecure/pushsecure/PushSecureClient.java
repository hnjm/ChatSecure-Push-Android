package org.chatsecure.pushsecure.pushsecure;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.chatsecure.pushsecure.pushsecure.response.CreateAccountResponse;
import org.chatsecure.pushsecure.pushsecure.response.CreateDeviceResponse;
import org.chatsecure.pushsecure.pushsecure.response.CreateTokenResponse;
import org.chatsecure.pushsecure.pushsecure.response.SendMessageResponse;
import org.chatsecure.pushsecure.pushsecure.response.typeadapter.DjangoDateTypeAdapter;

import java.util.Date;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;
import timber.log.Timber;

/**
 * An API client for the ChatSecure Push Server
 * Created by davidbrodsky on 6/23/15.
 */
public class PushSecureClient {

    private PushSecureApi api;
    private String token;
    private String registrationId;

    public PushSecureClient(@NonNull Context context) {

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DjangoDateTypeAdapter())
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://chatsecure-push.herokuapp.com")//"http://192.168.1.27:8000")
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(request -> {
                    if (token != null) request.addHeader("Authorization", "Token " + token);
                })
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        api = restAdapter.create(PushSecureApi.class);

        // Load persisted items
        SharedPreferences storage = context.getSharedPreferences("pushSecureService", Context.MODE_PRIVATE);

        token = storage.getString("token", null);
        registrationId = storage.getString("registrationId", null);
    }

    public Observable<CreateAccountResponse> createAccount(@Nullable String email,
                                                           @NonNull String username,
                                                           @NonNull String password) {

        return api.createAccount(email, username, password)
                .doOnNext(response -> {
                    Timber.d("Created account with token ", response.token);
                    token = response.token;
                });
    }

    public Observable<CreateDeviceResponse> createDevice(@Nullable String name,
                                                         @NonNull String gcmRegistrationId,
                                                         @Nullable String gcmDeviceId) {

        return api.createDevice(name, gcmRegistrationId, gcmDeviceId)
                .doOnNext(createDeviceResponse -> registrationId = createDeviceResponse.registrationId);
    }

    public Observable<CreateTokenResponse> createToken(@Nullable String name) {
        if (registrationId == null)
            return Observable.error(new IllegalStateException("You must register this device" +
                    " before creating tokens. Did you call createDevice(...)?"));

        return api.createToken(name, registrationId);
    }

    public Observable<SendMessageResponse> sendMessage(@NonNull String recipientToken,
                                                       @Nullable String data) {

        return api.sendMessage(recipientToken, data);
    }
}