package com.tangrun.mschat.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.*;
import com.example.mschat.R;
import com.example.mschat.databinding.MsCallWindowBinding;
import com.tangrun.mschat.MSManager;
import com.tangrun.mschat.enums.RoomType;
import com.tangrun.mschat.model.BuddyModel;
import com.tangrun.mschat.model.UIRoomStore;
import org.jetbrains.annotations.NotNull;

public class CallWindowService extends LifecycleService {

    UIRoomStore uiRoomStore;
    MsCallWindowBinding binding;


    Observer<BuddyModel> mineHardwareStateObserver = new Observer<BuddyModel>() {
        @Override
        public void onChanged(BuddyModel buddyModel) {
            buddyModel.disabledCam.observe(CallWindowService.this, aBoolean -> {
                ImageViewCompat.setImageTintList(binding.msIvCameraState, ResourcesCompat.getColorStateList(getResources(),
                        aBoolean == Boolean.FALSE ? R.color.ms_chat_green : R.color.ms_chat_gray, getTheme()));
            });
            buddyModel.disabledMic.observe(CallWindowService.this, aBoolean -> {
                ImageViewCompat.setImageTintList(binding.msIvMicrophoneState, ResourcesCompat.getColorStateList(getResources(),
                        aBoolean == Boolean.FALSE ? R.color.ms_chat_green : R.color.ms_chat_gray, getTheme()));
            });
        }
    };

    Observer<BuddyModel> mineVideoTrackObserver = new Observer<BuddyModel>() {
        @Override
        public void onChanged(BuddyModel buddyModel) {
            buddyModel.videoTrack.observe(CallWindowService.this, videoTrack -> {
                binding.msVRender.bind(CallWindowService.this, videoTrack);
            });
        }
    };

    WindowViewDragManager windowViewDragManager;

    @Override
    public void onCreate() {
        super.onCreate();
        uiRoomStore = MSManager.getCurrent();
        if (uiRoomStore == null) {
            stopSelf();
            return;
        }
        binding = MsCallWindowBinding.inflate(LayoutInflater.from(this));
        uiRoomStore.showActivity.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == Boolean.TRUE) {
                    stopSelf();
                }
            }
        });

        // 左边麦克 摄像头状态只有在多人音视频才有
        if (uiRoomStore.roomType == RoomType.MultiCall && !uiRoomStore.audioOnly) {
            binding.msLlHardwareState.setVisibility(View.VISIBLE);
            uiRoomStore.mine.observe(this, mineHardwareStateObserver);
        } else {
            binding.msLlHardwareState.setVisibility(View.GONE);
        }
        // 音/视频窗口大小
        ViewGroup.LayoutParams layoutParams = binding.msVRender.getLayoutParams();
        layoutParams.width = dp2px(uiRoomStore.audioOnly ? 60 : 80);
        layoutParams.height = dp2px(uiRoomStore.audioOnly ? 60 : 120);
        if (uiRoomStore.audioOnly) {
            binding.msVRender.init(CallWindowService.this);
            uiRoomStore.mine.observe(this, mineVideoTrackObserver);
        }
        // 通话
        uiRoomStore.localConversationState.observe(this, conversationState -> {
            uiRoomStore.callTime.removeObservers(this);
            String text = null;
            int tintId = 0;
            int imgId = 0;
            switch (conversationState) {
                case New: {
                    tintId = R.color.ms_chat_green;
                    imgId = R.drawable.ic_baseline_call_end_24;
                    text = "";
                    break;
                }
                case Invited: {
                    tintId = R.color.ms_chat_green;
                    imgId = R.drawable.ic_baseline_call_end_24;
                    text = "待接听";
                    break;
                }
                case Joined: {
                    tintId = R.color.ms_chat_white;
                    imgId = R.drawable.ic_baseline_call_24;
                    text = "通话中";
                    uiRoomStore.callTime.observe(this, s -> {
                        binding.msTvCallTip.setText(s);
                    });
                    break;
                }
                case Left:
                case InviteBusy:
                case InviteTimeout:
                case InviteReject: {
                    tintId = R.color.ms_chat_red;
                    imgId = R.drawable.ic_baseline_call_end_24;
                    text = "通话已结束";
                    break;
                }
            }
            setCallInfoTintColor(tintId);
            binding.msIvCallType.setImageResource(imgId);
            binding.msTvCallTip.setText(text);
        });

        windowViewDragManager = new WindowViewDragManager(this, binding.getRoot()) {
            @Override
            public void onWindowScrolling(int viewX, int viewY, int width, int height, int screenWidth, int screenHeight) {
                binding.getRoot().setBackgroundResource(R.drawable.ms_bg_window_all_corner);
            }

            @Override
            public void onWindowScrollEnd(int viewX, int viewY, int width, int height, int screenWidth, int screenHeight) {
                boolean left = viewX + width / 2 < screenWidth / 2;
                setWindowPositionToClosestBord(left);
                binding.getRoot().setBackgroundResource(left ? R.drawable.ms_bg_window_right_corner : R.drawable.ms_bg_window_left_corner);
            }

            @Override
            public void onWindowViewClick() {
                MSManager.openCallActivity();
            }
        };
        getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull @NotNull LifecycleOwner source, @NonNull @NotNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    windowViewDragManager.showView();
                } else if (event == Lifecycle.Event.ON_DESTROY) {
                    windowViewDragManager.removeView();
                }
            }
        });

    }

    void setImgTint(ImageView view, int colorId) {
        ImageViewCompat.setImageTintList(view, ResourcesCompat.getColorStateList(getResources(), colorId, null));
    }

    void setCallInfoTintColor(int colorId) {
        setImgTint(binding.msIvCallType, colorId);
        binding.msTvCallTip.setTextColor(ResourcesCompat.getColor(getResources(), colorId, getTheme()));
    }


    static abstract class WindowViewDragManager implements View.OnTouchListener {
        private Context context;
        private int screenWidth;
        private int screenHeight;
        private int statusBarHeight;
        private boolean showView;
        private View view;
        private WindowManager windowManager;
        private WindowManager.LayoutParams layoutParams;
        private GestureDetector gestureDetector;

        public WindowViewDragManager(Context context, View view) {
            this.context = context;
            this.view = view;
            init();
        }

        public abstract void onWindowScrolling(int viewX, int viewY, int width, int height, int screenWidth, int screenHeight);

        public abstract void onWindowScrollEnd(int viewX, int viewY, int width, int height, int screenWidth, int screenHeight);

        public abstract void onWindowViewClick();


        private void init() {
            screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            {
                Resources resources = context.getResources();
                int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
                statusBarHeight = resources.getDimensionPixelSize(resourceId);
            }
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            layoutParams = new WindowManager.LayoutParams();
            int type;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            layoutParams.type = type;
            layoutParams.flags =
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.y = layoutParams.x = screenWidth / 10;
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return false;
                }

                @Override
                public void onShowPress(MotionEvent e) {
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    onWindowViewClick();
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    layoutParams.x = (int) (e2.getRawX() - (width / 2));
                    layoutParams.y = (int) (e2.getRawY() - (height / 2)) - statusBarHeight;
                    //Log.d(TAG, "onScroll: window params xy= " + layoutParams.x + "," + layoutParams.y + " root view wh= " + width + "," + height + " event raw xy= " + e2.getRawX() + "," + e2.getRawY());
                    windowManager.updateViewLayout(view, layoutParams);
                    onWindowScrolling(layoutParams.x, layoutParams.y, width, height, screenWidth, screenHeight);
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    performWindowScrollEnd();
                    return true;
                }
            };
            gestureDetector = new GestureDetector(context, gestureListener);
            gestureDetector.setIsLongpressEnabled(false);
        }

        private void showView() {
            if (showView) return;
            showView = true;
            view.setOnTouchListener(this);
            windowManager.addView(view, layoutParams);
        }

        public void removeView() {
            if (!showView) return;
            showView = false;
            view.setOnTouchListener(null);
            windowManager.removeView(view);
        }

        private void performWindowScrollEnd() {
            int width = view.getWidth();
            int height = view.getHeight();
            onWindowScrollEnd(layoutParams.x, layoutParams.y, width, height, screenWidth, screenHeight);
        }

        public void setWindowPositionToClosestBord(boolean left) {
//            layoutParams.x = getViewCenterX() < screenWidth / 2 ? 0 : screenWidth;
            layoutParams.x = left ? 0 : screenWidth;
            windowManager.updateViewLayout(view, layoutParams);
        }


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!showView) {
                return v.onTouchEvent(event);
            }
            boolean b = gestureDetector.onTouchEvent(event);
            //解决慢速滑动时 不回调onFling的问题
            if (event.getAction() == MotionEvent.ACTION_UP && !b) {
                performWindowScrollEnd();
                return true;
            }
            return b;
        }
    }


    public static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
