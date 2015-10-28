package payumoney.payu.com.payumoneysdkapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.SdkConstants;

/**
 * Created by piyush on 12/11/14.
 */
public class paymentSuccess extends Activity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.success);

        TextView textView = (TextView)findViewById(R.id.status);
        String status = getIntent().getStringExtra(SdkConstants.RESULT);
        Toast.makeText(getApplicationContext(),status,Toast.LENGTH_LONG).show();
        if(status.equals("success"))
            textView.setText("Congratz!!Your payment is successful");
        else
            textView.setText("Payment failed");

    }



}
