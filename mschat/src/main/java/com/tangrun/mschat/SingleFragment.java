package com.tangrun.mschat;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 9:36
 */
public class SingleFragment extends Fragment {
    public static SingleFragment newInstance() {

        Bundle args = new Bundle();

        SingleFragment fragment = new SingleFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
