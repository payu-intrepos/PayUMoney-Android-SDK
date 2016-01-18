package payumoney.payu.com.payumoneysdkapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;


public class MyActivity extends Activity {


    EditText amt = null, txnid = null, phone = null, pinfo = null, fname = null, email = null, surl = null, furl = null, mid = null;
    Button pay = null;

    HashMap<String, String> params = new HashMap<>();
    public final int RESULT_FAILED = 90;
    public final int RESULT_BACK = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        amt = (EditText) findViewById(R.id.amount);
        txnid = (EditText) findViewById(R.id.txnid);
        mid = (EditText) findViewById(R.id.merchant_id);
        phone = (EditText) findViewById(R.id.phone);
        pinfo = (EditText) findViewById(R.id.pinfo);
        fname = (EditText) findViewById(R.id.fname);
        email = (EditText) findViewById(R.id.email);
        surl = (EditText) findViewById(R.id.surl);
        furl = (EditText) findViewById(R.id.furl);
        pay = (Button) findViewById(R.id.pay);
        if (SdkConstants.WALLET_SDK) {
            findViewById(R.id.history).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.history).setVisibility(View.GONE);
        }
    }

    /*public void getTransactionHistory(View view) {

        if (params == null) {
            params = new HashMap<String, String>();
        } else {
            params.clear();
        }

        params.put(SdkConstants.KEY, "1LCS8b");
        params.put(SdkConstants.SALT, "4z5sUWFC");
        params.put(SdkConstants.MERCHANT_ID, "4827862");
        params.put(SdkConstants.CLIENT_ID, "180553");

        params.put(SdkConstants.MERCHANT_TXNID, "0nf7");

        params.put(SdkConstants.SURL, "https://mobiletest.payumoney.com/mobileapp/payumoney/success.php");
        params.put(SdkConstants.FURL, "https://mobiletest.payumoney.com/mobileapp/payumoney/failure.php");
        params.put(SdkConstants.PRODUCT_INFO, "productInfo");
        params.put(SdkConstants.FIRSTNAME, "mobile");
        params.put(SdkConstants.EMAIL, "govind.bajpai@yopmail.com");
        params.put(SdkConstants.PHONE, "9873179584");
        params.put(SdkConstants.AMOUNT, amt.getText().toString());

        params.put("udf1", "");
        params.put("udf2", "");
        params.put("udf3", "");
        params.put("udf4", "");
        params.put("udf5", "");
        params.put(SdkConstants.IS_HISTORY_CALL, "");

        if (SdkSession.getInstance(this) == null) {
            SdkSession.startPaymentProcess(this, params);
        } else {
            SdkSession.createNewInstance(this);
        }

        SdkSession.startPaymentProcess(this, params);

    }*/

    public void makePayment(View view) {
        if (amt.getText().toString().equals("") || (Double.parseDouble(amt.getText().toString()) == 0.0)) {
            Toast.makeText(getApplicationContext(), "no amount specified", Toast.LENGTH_LONG).show();
        } else if (Double.parseDouble(amt.getText().toString()) > 1000000.00) {
            Toast.makeText(getApplicationContext(), "Amount exceeding the limit : 1000000.00 ", Toast.LENGTH_LONG).show();
        } else {
            if (SdkSession.getInstance(this) == null) {
                SdkSession.startPaymentProcess(this, params);
            } else {
                SdkSession.createNewInstance(this);
            }
            /*String hashSequence = "Vw997n" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "4womTBoq";
            params.put("key", "Vw997n");
            params.put("MerchantId", "4825269");*/
            String hashSequence = "mdyCKV" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "Je7q3652";
            params.put(SdkConstants.KEY, "mdyCKV");
            params.put(SdkConstants.MERCHANT_ID, "4914106");
            String hash = hashCal(hashSequence);
            Log.i("hash", hash);
            params.put(SdkConstants.TXNID, "0nf7");// debug
            params.put("SURL", "https://mobiletest.payumoney.com/mobileapp/payumoney/success.php");
            params.put("FURL", "https://mobiletest.payumoney.com/mobileapp/payumoney/failure.php");
            params.put(SdkConstants.PRODUCT_INFO, "product_name");
            params.put(SdkConstants.FIRSTNAME, "piyush");
            params.put(SdkConstants.EMAIL, "piyush.jain@payu.in");
            params.put(SdkConstants.PHONE, "8882434664");
            params.put(SdkConstants.AMOUNT, amt.getText().toString());
            params.put("hash", hash);
            params.put("udf1", "");
            params.put("udf2", "");
            params.put("udf3", "");
            params.put("udf4", "");
            params.put("udf5", "");
            SdkSession.startPaymentProcess(this, params);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if(data!=null) {
        if (requestCode == SdkSession.PAYMENT_SUCCESS) {
            if (resultCode == RESULT_OK) {
                Log.i("app_activity", "success");

                if (data != null && data.hasExtra(SdkConstants.IS_HISTORY_CALL)) {
                    return;
                } else {
                    //Log.i(SdkConstants.PAYMENT_ID, data.getStringExtra(SdkConstants.PAYMENT_ID));
                    Intent intent = new Intent(this, paymentSuccess.class);
                    intent.putExtra(SdkConstants.RESULT, "success");
                    intent.putExtra(SdkConstants.PAYMENT_ID, data.getStringExtra(SdkConstants.PAYMENT_ID));
                    startActivity(intent);
                }
                // finish();
            }

            if (resultCode == RESULT_CANCELED) {
                Log.i("app_activity", "failure");

                Intent intent = new Intent(this, paymentSuccess.class);
                intent.putExtra(SdkConstants.RESULT, "cancelled");
                startActivity(intent);

                //Write your code if there's no result
            }

            if (resultCode == RESULT_FAILED) {
                Log.i("app_activity", "failure");

                if (data != null) {
                    if (data.getStringExtra(SdkConstants.RESULT).equals("cancel")) {

                    } else {
                        Intent intent = new Intent(this, paymentSuccess.class);
                        intent.putExtra(SdkConstants.RESULT, "failure");
                        startActivity(intent);
                    }
                }
                //Write your code if there's no result
            }

            if (resultCode == RESULT_BACK) {
                Log.i("app_activity", "User returned without login");


                Toast.makeText(getApplicationContext(), "User returned without login", Toast.LENGTH_LONG).show();
                /*Intent intent = new Intent(this, paymentSuccess.class);
                intent.putExtra(SdkConstants.RESULT, "cancelled");
                startActivity(intent);*/

                //Write your code if there's no result
            }
        }
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*int id = item.getItemId();
        if (id == R.id.enable)
            SdkSession.getInstance(getApplicationContext()).enableOneClickTransaction("1");
        else if (id == R.id.disable)
            SdkSession.getInstance(getApplicationContext()).enableOneClickTransaction("0");*/
        return super.onOptionsItemSelected(item);

    }

    public static String hashCal(String str) {
        byte[] hashseq = str.getBytes();
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("SHA-512");
            algorithm.reset();
            algorithm.update(hashseq);
            byte messageDigest[] = algorithm.digest();
            for (byte aMessageDigest : messageDigest) {
                String hex = Integer.toHexString(0xFF & aMessageDigest);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
        } catch (NoSuchAlgorithmException ignored) {
        }
        return hexString.toString();
    }
}
