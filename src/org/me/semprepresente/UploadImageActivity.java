/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.me.semprepresente;

import android.app.*;
import android.graphics.*;
import android.os.Bundle;
import android.util.*;
import java.io.*;
import java.util.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;

/**
 *
 * @author Ricardo
 */
public class UploadImageActivity extends Activity {

    InputStream is;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(), R.drawable.lena);

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmapOrg.compress(Bitmap.CompressFormat.JPEG, 90, bao);

        byte [] ba = bao.toByteArray();
        String ba1=Base64.encodeToString(ba, Base64.DEFAULT);

        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("image",ba1));

        try{
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new
            HttpPost("http://10.0.2.2:80/android/base.php");
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        }catch(Exception e){
            Log.e("log_tag", "Error in http connection "+e.toString());
        }
    }
}