package com.tangrun.mschat.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 9:36
 */
public class SingleCallRoomFragment extends Fragment {
    public static SingleCallRoomFragment newInstance() {

        Bundle args = new Bundle();

        SingleCallRoomFragment fragment = new SingleCallRoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
