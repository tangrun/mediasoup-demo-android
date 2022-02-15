package com.tangrun.mschat;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 15:37
 */
public class BindAdapter {

    @BindingAdapter({"visible"})
    public static void visible(View view, boolean b) {
        view.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter({"videoTrack"})
    public static void videoTrack(SurfaceViewRenderer renderer, VideoTrack track) {
        if (track != null) {
            track.addSink(renderer);
        }
    }

    @BindingAdapter(value = {"resId"})
    public static void loadImgUrl(AppCompatImageView view, int resId) {
        view.setImageResource(resId);
    }

    @BindingAdapter(value = {"url", "placeholder", "error"})
    public static void loadImgUrl(AppCompatImageView view, String url, Drawable placeholder, Drawable error) {
        RequestBuilder<Drawable> builder = Glide.with(view)
                .load(url);
        if (placeholder != null || error != null) {
            RequestOptions options = new RequestOptions();
            if (placeholder != null)
                options.placeholder(placeholder);
            if (error != null)
                options.error(error);
            builder = builder.apply(options);
        }
        builder.into(view);
    }
}
