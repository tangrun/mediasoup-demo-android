package com.tangrun.mschat;

import com.tangrun.mslib.utils.JsonUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;

import static org.apache.http.conn.ssl.SSLSocketFactory.SSL;

public class MSApi  {
    private static final String TAG = "MS_Api";

    private final String host;
    private final String port;

    public MSApi(String host, String port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public void roomExists(String id, ApiCallback<Boolean> apiCallback) {
        if (apiCallback == null) return;
        Observable.create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<Boolean> emitter) throws Exception {
                        Request.Builder builder = new Request.Builder();
                        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl() + "/roomExists").newBuilder();
                        urlBuilder.addQueryParameter("roomId", id);
                        builder.url(urlBuilder.build());
                        String request = request(builder.build());
                        JSONObject jsonObject = JsonUtils.toJsonObject(request);
                        int code = jsonObject.optInt("code");
                        if (code == 0) {
                            emitter.onNext(true);
                        } else {
                            emitter.onNext(false);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Boolean>() {
                    @Override
                    public void onNext(@NonNull Boolean s) {
                        apiCallback.onSuccess(s);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dispose();
                        apiCallback.onFail(e);
                    }

                    @Override
                    public void onComplete() {
                        dispose();
                    }
                });
    }

    public void busy(String roomId, String userId, ApiCallback<Object> apiCallback) {
        if (apiCallback == null) return;
        Observable.create(new ObservableOnSubscribe<Object>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<Object> emitter) throws Exception {
                        Request.Builder builder = new Request.Builder();
                        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl() + "/busy").newBuilder();
                        urlBuilder.addQueryParameter("roomId", roomId)
                                .addQueryParameter("peerId", userId);
                        builder.url(urlBuilder.build());
                        String request = request(builder.build());
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Object>() {
                    @Override
                    public void onNext(@NonNull Object s) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dispose();
                        apiCallback.onFail(e);
                    }

                    @Override
                    public void onComplete() {
                        dispose();
                        apiCallback.onSuccess(null);
                    }
                });
    }

    public void createRoom(String id, ApiCallback<String> apiCallback) {
        if (apiCallback == null) return;
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Exception {
                        Request.Builder builder = new Request.Builder();
                        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl() + "/debug_createRoom").newBuilder();
                        urlBuilder.addQueryParameter("roomId", id);
                        builder.url(urlBuilder.build());
                        String request = request(builder.build());
                        JSONObject jsonObject = JsonUtils.toJsonObject(request);
                        int code = jsonObject.optInt("code");
                        if (code == 0) {
                            emitter.onNext(id);
                        } else {
                            if (jsonObject.optString("msg", "").contains("已存在")) {
                                emitter.onNext(id);
                            } else {
                                emitter.onError(new Exception(jsonObject.optString("message")));
                            }
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<String>() {
                    @Override
                    public void onNext(@NonNull String s) {
                        apiCallback.onSuccess(s);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dispose();
                        apiCallback.onFail(e);
                    }

                    @Override
                    public void onComplete() {
                        dispose();
                    }
                });
    }

    public void createRoom(ApiCallback<String> apiCallback) {
        if (apiCallback == null) return;
        Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Exception {
                        Request.Builder builder = new Request.Builder();
                        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl()+ "/createRoom").newBuilder();
                        builder.url(urlBuilder.build());
                        String request = request(builder.build());
                        JSONObject jsonObject = JsonUtils.toJsonObject(request);
                        int code = jsonObject.optInt("code");
                        if (code == 0) {
                            emitter.onNext(jsonObject.optJSONObject("data").optString("roomId"));
                        } else {
                            emitter.onError(new Exception(jsonObject.optString("message")));
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<String>() {
                    @Override
                    public void onNext(@NonNull String s) {
                        apiCallback.onSuccess(s);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dispose();
                        apiCallback.onFail(e);
                    }

                    @Override
                    public void onComplete() {
                        dispose();
                    }
                });
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts =
                    new TrustManager[]{
                            new X509TrustManager() {

                                @Override
                                public void checkClientTrusted(
                                        java.security.cert.X509Certificate[] chain, String authType)
                                        throws CertificateException {
                                }

                                @Override
                                public void checkServerTrusted(
                                        java.security.cert.X509Certificate[] chain, String authType)
                                        throws CertificateException {
                                }

                                // Called reflectively by X509TrustManagerExtensions.
                                public void checkServerTrusted(
                                        java.security.cert.X509Certificate[] chain, String authType, String host) {
                                }

                                @Override
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new java.security.cert.X509Certificate[]{};
                                }
                            }
                    };

            final SSLContext sslContext = SSLContext.getInstance(SSL);
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            HttpLoggingInterceptor httpLoggingInterceptor =
                    new HttpLoggingInterceptor(s -> Logger.d(TAG, s));
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient.Builder builder =
                    new OkHttpClient.Builder()
                            .addInterceptor(httpLoggingInterceptor)
                            .retryOnConnectionFailure(true);
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);

            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String request(Request request) throws IOException {
        OkHttpClient httpClient = getUnsafeOkHttpClient();
        Response response = httpClient.newCall(request).execute();
        return response.body().string();
    }

    private String getUrl() {
        StringBuilder stringBuilder = new StringBuilder()
                .append("https://")
                .append(host);
        if (port != null && port.trim().length() > 0) {
            stringBuilder.append(":")
                    .append(port);
        }
        return stringBuilder.toString();
    }
}
