package com.payUMoney.sdk.walledSdk;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.payUMoney.sdk.utils.SdkHelper;

/**
 * Created by viswash on 27/8/15.
 */
public class SdkBaseFragment extends Fragment {

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if(SdkHelper.isValidClick()) {
            super.startActivityForResult(intent, requestCode);
        }
    }
}
