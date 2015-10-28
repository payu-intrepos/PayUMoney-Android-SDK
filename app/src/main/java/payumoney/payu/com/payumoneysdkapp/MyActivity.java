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
    }

    public void makePayment(View view) {
        if (amt.getText().toString().equals("") || (Double.parseDouble(amt.getText().toString()) == 0.0)) {
            Toast.makeText(getApplicationContext(), "no amount specified", Toast.LENGTH_LONG).show();
        }
        else if (Double.parseDouble(amt.getText().toString()) > 1000000.00) {
            Toast.makeText(getApplicationContext(), "Amount exceeding the limit : 1000000.00 ", Toast.LENGTH_LONG).show();
        }
        else {
            if (SdkSession.getInstance(this) == null) {
                SdkSession.startPaymentProcess(this, params);
            } else {
                SdkSession.createNewInstance(this);
            }
            //Mukesh PP5
            /*String hashSequence = "O50ARA" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "ftUjjzPp";
            params.put("key", "O50ARA");
            params.put("MerchantId", "4825489");*/
            //MGL mobiletest
            /*String hashSequence = "pkVknR" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "wOQkPTPA";
            params.put("key", "pkVknR");
            params.put("MerchantId", "7454");*/
            //govind_mobiletest
            String hashSequence = "1LCS8b" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "4z5sUWFC";
            params.put("key", "1LCS8b");
            params.put("MerchantId", "4827862");
            //mukesh_test
            /*String hashSequence = "Vw997n" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "4womTBoq";
            params.put("key", "Vw997n");
            params.put("MerchantId", "4825269");*/
            //mukesh_mobiletest
            /*String hashSequence = "h044J7" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "UMkRRTtZ";
            params.put("key", "h044J7");
            params.put("merchantId", "4959136")*/
            /*LIVE MUKESH*/
            /*String hashSequence = "h044J7" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "UMkRRTtZ";
            params.put("key", "h044J7");
            params.put("MerchantId", "4959136");*/
            //Eva Live
            /*String hashSequence = "UZkVYg" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "IB659gi9";
            params.put("key", "UZkVYg");
            params.put("MerchantId", "5072952");*/
            //ApPower
           // params.put("MerchantId", "4824738");
            //ApPower
            // params.put("key","ngCXUJ");
            //ApPower
          // String hashSequence = "ngCXUJ" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
            //      + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "O7ZBSgci";
            //live parameters
            //   String hashSequence = "h044J7" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
            // + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "UMkRRTtZ";
            //debug
           //String hashSequence = "dTaf8u" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
             //      + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "yov4hU3x";
            //debug karan
            //   String hashSequence = "twknxo" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
            //   + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "5EbrpgnJ";
            //debug
            //   String hashSequence = "ZSi1em" + "|" + "0nf7" + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
            // + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "TxtKl6BQ";

            String hash = hashCal(hashSequence);
            Log.i("hash", hash);
            //  params.put("TxnId", "0nf7" + System.currentTimeMillis());
            params.put("TxnId", "0nf7");// debug
            //Test
           // params.put("MerchantId","4825052");
            //Test Karan MID
            // params.put("MerchantId","4927751");
            //PP4
           //  params.put("MerchantId","4825489");
            //PP5
            //params.put("MerchantId","4827143");
            //live merchant key //
            //  params.put("MerchantId","4959136");
            //  params.put("MerchantId","4959136");//live merchant
            params.put("SURL", "https://mobiletest.payumoney.com/mobileapp/payumoney/success.php");
            params.put("FURL", "https://mobiletest.payumoney.com/mobileapp/payumoney/failure.php");
            params.put("ProductInfo", "product_name");
            params.put("firstName", "piyush");
            params.put("Email", "piyush.jain@payu.in");
            params.put("Phone", "8882434664");
            params.put("Amount", amt.getText().toString());
            params.put("hash", hash);
            // params.put("PayUMoneyApp","Recharge");
            //test key
            // params.put("key", "dTaf8u");
            //Karantestkey
            // params.put("key", "twknxo");
            //pp4 key
            //  params.put("key", "r7aQlH");
            //live key
            // params.put("key","h044J7");
            params.put("udf1", "");
            params.put("udf2", "");
            params.put("udf3", "");
            params.put("udf4", "");
            params.put("udf5", "");
            //params.put("TxnId", txnid.getText().toString());
            // params.put("MerchantId",mid.getText().toString());
            //params.put("surl",surl.getText().toString());
            //params.put("furl",furl.getText().toString());
            //params.put("productinfo",pinfo.getText().toString());
            // params.put("firstName",fname.getText().toString());
            //params.put("Email", email.getText().toString());
            //params.put("Phone",phone.getText().toString());
            //params.put("amt",amt.getText().toString());
            SdkSession.startPaymentProcess(this, params);
            //System.out.print(s);
            //Log.i("mess1", params);
        }
    }
////

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if(data!=null) {
        if (requestCode == SdkSession.PAYMENT_SUCCESS) {
            if (resultCode == RESULT_OK) {
                Log.i("app_activity", "success");
                Log.i("paymentID", data.getStringExtra("paymentId"));
                Intent intent = new Intent(this, paymentSuccess.class);
                intent.putExtra(SdkConstants.RESULT, "success");
                intent.putExtra(SdkConstants.PAYMENT_ID, data.getStringExtra("paymentId"));
                startActivity(intent);
                // finish();
            }

            if (resultCode == RESULT_CANCELED) {
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
        int id = item.getItemId();
        if(id == R.id.enable)
            SdkSession.getInstance(getApplicationContext()).enableCvvLessTransaction("1");
        else if(id == R.id.disable)
            SdkSession.getInstance(getApplicationContext()).enableCvvLessTransaction("0");
        return  super.onOptionsItemSelected(item);

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
