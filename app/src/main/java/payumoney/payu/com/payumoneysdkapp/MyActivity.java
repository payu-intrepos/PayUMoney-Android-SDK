package payumoney.payu.com.payumoneysdkapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.payUMoney.sdk.PayUmoneySdkInitilizer;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class MyActivity extends Activity {


    EditText amt = null, txnid = null, phone = null, pinfo = null, fname = null, email = null, surl = null, furl = null, mid = null, udf1 = null, udf2 = null, udf3 = null, udf4 = null, udf5 = null;
    Button pay = null;

    public static final String TAG = "PayUMoneySDK Sample";


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
        udf1 = (EditText) findViewById(R.id.udf1);
        udf2 = (EditText) findViewById(R.id.udf2);
        udf3 = (EditText) findViewById(R.id.udf3);
        udf4 = (EditText) findViewById(R.id.udf4);
        udf5 = (EditText) findViewById(R.id.udf5);
        furl = (EditText) findViewById(R.id.furl);
        pay = (Button) findViewById(R.id.pay);
    }

    private boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void makePayment(View view) {
        Double amount =0.0;
        String txnId = "0nf7" + System.currentTimeMillis();
        if(isDouble(amt.getText().toString())){
            amount = Double.parseDouble(amt.getText().toString());
        }else{
            Toast.makeText(getApplicationContext(), "Enter correct amount", Toast.LENGTH_LONG).show();
            return ;
        }
        if (amount <= 0.0) {
            Toast.makeText(getApplicationContext(), "Enter Some amount", Toast.LENGTH_LONG).show();
        } else if (amount > 1000000.00) {
            Toast.makeText(getApplicationContext(), "Amount exceeding the limit : 1000000.00 ", Toast.LENGTH_LONG).show();
        } else {

            PayUmoneySdkInitilizer.PaymentParam.Builder builder = new PayUmoneySdkInitilizer.PaymentParam.Builder(  );

            if(amt != null && amt.getText().toString().isEmpty()) {
                builder.setAmount(10);
            } else {
                builder.setAmount(amount);// debug
            }

            if(txnid != null && txnid.getText().toString().isEmpty()) {
                //builder.setTnxId(UUID.randomUUID().toString());

                builder.setTnxId(txnId);
            } else {
                builder.setTnxId(txnid.getText().toString());
            }

            if(mid != null && mid.getText().toString().isEmpty()) {
                builder.setMerchantId("4744443"); //
            } else {
                builder.setMerchantId(mid.getText().toString());// debug
            }

            if(phone != null && phone.getText().toString().isEmpty()) {
                builder.setPhone("8882434664");
            } else {
                builder.setPhone(phone.getText().toString());// debug
            }

            if(pinfo != null && pinfo.getText().toString().isEmpty()) {
                builder.setProductName("product_name");
            } else {
                builder.setProductName(pinfo.getText().toString());// debug
            }

            if(fname != null && fname.getText().toString().isEmpty()) {
                builder.setFirstName("piyush");
            } else {
                builder.setFirstName(fname.getText().toString());// debug
            }

            if(email != null && email.getText().toString().isEmpty()) {
                builder.setEmail("piyush.jain@payu.in");
            } else {
                builder.setEmail(email.getText().toString());// debug
            }

            if(surl != null && surl.getText().toString().isEmpty()) {
                builder.setsUrl("https://mobiletest.payumoney.com/mobileapp/payumoney/success.php");
            } else {
                builder.setsUrl(surl.getText().toString());// debug
            }

            if(furl != null && furl.getText().toString().isEmpty()) {
                builder.setfUrl("https://mobiletest.payumoney.com/mobileapp/payumoney/failure.php");
            } else {
                builder.setfUrl(furl.getText().toString());// debug
            }

            if(udf1 != null && udf1.getText().toString().isEmpty()) {
                builder.setUdf1("");
            } else {
                builder.setUdf1(udf1.getText().toString());// debug
            }

            if(udf2 != null && udf2.getText().toString().isEmpty()) {
                builder.setUdf2("");
            } else {
                builder.setUdf2(udf2.getText().toString());// debug
            }

            if(udf3 != null && udf3.getText().toString().isEmpty()) {
                builder.setUdf3("");
            } else {
                builder.setUdf3(udf3.getText().toString());// debug
            }

            if(udf4 != null && udf4.getText().toString().isEmpty()) {
                builder.setUdf4("");
            } else {
                builder.setUdf4(udf4.getText().toString());// debug
            }

            if(udf5 != null && udf5.getText().toString().isEmpty()) {
                builder.setUdf5("");
            } else {
                builder.setUdf5(udf5.getText().toString());// debug
            }

            /*builder.setKey("mdyCKV");// pp4
            builder.setMerchantId("4914106");*/
            builder.setIsDebug(true);
            //builder.setKey("510kFd");// mobileDev Key
            /*builder.setKey("FCstqb");// mobileTest Key
            builder.setMerchantId("4827834");// Debug Merchant ID*/

            /*builder.setKey("279bckLH");// mobileTest Key
            builder.setMerchantId("4828325");// Debug Merchant ID*/

            /*builder.setKey("UxQ7ajsf");// mobileTest Key
            builder.setMerchantId("4828544");// Debug Merchant ID*/

            /*builder.setKey("hL7HHs");//  Key
            builder.setMerchantId("4828901");// Debug Merchant ID*/

            /*builder.setKey("tPJM2e");// mobileTest Key
            builder.setMerchantId("4824899");// Debug Merchant ID*/

            /*builder.setKey("mdyCKV");
            builder.setMerchantId("4914106");*/

            /*builder.setKey("O50ARA");// pp4
            builder.setMerchantId("4825489");*/

            /*builder.setKey("GJ048r");// PP21
            builder.setMerchantId("4826176");*/

            /*builder.setKey("40747T");// mobileTest Key
            builder.setMerchantId("396132");// Debug Merchant ID*/

            builder.setKey("YbZq8n");// mobileTest Key
            builder.setMerchantId("393592");// Debug Merchant ID

            PayUmoneySdkInitilizer.PaymentParam paymentParam = builder.build();

            /*
             server side call required to calculate hash with the help of <salt>
             <salt> is already shared along with merchant <key>
             serverCalculatedHash =sha512(key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|<salt>)

             (e.g.)

             sha512(FCstqb|0nf7|10.0|product_name|piyush|piyush.jain@payu.in||||||MBgjYaFG)

             9f1ce50ba8995e970a23c33e665a990e648df8de3baf64a33e19815acd402275617a16041e421cfa10b7532369f5f12725c7fcf69e8d10da64c59087008590fc

            */

            // Recommended
            calculateServerSideHashAndInitiatePayment(paymentParam);


            /*String hashSequence = "mdyCKV" + "|" + txnId + "|" + amt.getText().toString() + "|" + "product_name" + "|" + "piyush" + "|"
                    + "piyush.jain@payu.in" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "" + "|" + "Je7q3652";
            String hash = hashCal(hashSequence);
            paymentParam.setMerchantHash(hash);
            PayUmoneySdkInitilizer.startPaymentActivityForResult(MyActivity.this, paymentParam);*/

           /*
            testing purpose

            String serverCalculatedHash="9f1ce50ba8995e970a23c33e665a990e648df8de3baf64a33e19815acd402275617a16041e421cfa10b7532369f5f12725c7fcf69e8d10da64c59087008590fc";
            paymentParam.setMerchantHash(serverCalculatedHash);
            PayUmoneySdkInitilizer.startPaymentActivityForResult(this, paymentParam);

            */

        }
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

    private void calculateServerSideHashAndInitiatePayment(final PayUmoneySdkInitilizer.PaymentParam paymentParam) {

        // Replace your server side hash generator API URL
        //String url = "https://mobiletest.payumoney.com/payment/op/calculateHashForTest";
        //String url = "http://pp0.payumoney.com/payment/op/calculateHashForTest";
        //String url = "http://pp10.payumoney.com/payment/op/calculateHashForTest";
        //String url = "http://pp4.payumoney.com/payment/op/calculateHashForTest";
        //String url = "http://pp41.payumoney.com/payment/op/calculateHashForTest";
        String url = "http://pp42.payumoney.com/payment/op/calculateHashForTest";
        //String url = "http://pp21.payumoney.com/payment/op/calculateHashForTest";

        Toast.makeText(this,"Please wait... Generating hash from server ... ",Toast.LENGTH_LONG).show();
        StringRequest jsonObjectRequest = new StringRequest(Request.Method.POST,url,new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONObject jsonObject = new JSONObject(response);

                    if(jsonObject.has(SdkConstants.STATUS)) {
                        String status = jsonObject.optString(SdkConstants.STATUS);
                        if(status != null ||  status.equals("1")) {

                            String hash =  jsonObject.getString(SdkConstants.RESULT);
                            Log.i("app_activity", "Server calculated Hash :  " + hash);

                            paymentParam.setMerchantHash(hash);

                            PayUmoneySdkInitilizer.startPaymentActivityForResult(MyActivity.this, paymentParam);
                        }
                        else
                        {
                            Toast.makeText(MyActivity.this,
                                    jsonObject.getString(SdkConstants.RESULT),
                                    Toast.LENGTH_SHORT).show();
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

                if (error instanceof NoConnectionError) {
                    Toast.makeText(MyActivity.this,
                            MyActivity.this.getString(R.string.connect_to_internet),
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MyActivity.this,
                            error.getMessage(),
                            Toast.LENGTH_SHORT).show();

                }

            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return paymentParam.getParams();
            }
        };
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PayUmoneySdkInitilizer.PAYU_SDK_PAYMENT_REQUEST_CODE) {

            /*if(data != null && data.hasExtra("result")){
              String responsePayUmoney = data.getStringExtra("result");
                if(SdkHelper.checkForValidString(responsePayUmoney))
                    showDialogMessage(responsePayUmoney);
            } else {
                showDialogMessage("Unable to get Status of Payment");
            }*/


            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Success - Payment ID : " + data.getStringExtra(SdkConstants.PAYMENT_ID));
                String paymentId = data.getStringExtra(SdkConstants.PAYMENT_ID);
                showDialogMessage( "Payment Success Id : " + paymentId);
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "failure");
                showDialogMessage("cancelled");
            }else if (resultCode == PayUmoneySdkInitilizer.RESULT_FAILED) {
                Log.i("app_activity", "failure");

                if (data != null) {
                    if (data.getStringExtra(SdkConstants.RESULT).equals("cancel")) {

                    } else {
                        showDialogMessage("failure");
                    }
                }
                //Write your code if there's no result
            }

            else if (resultCode == PayUmoneySdkInitilizer.RESULT_BACK) {
                Log.i(TAG, "User returned without login");
                showDialogMessage( "User returned without login");
            }
        }

    }

    private void showDialogMessage(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(TAG);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();

    }
}
