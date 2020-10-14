package org.operatorfoundation.shapeshifter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.textclassifier.TextLinks;

import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig;
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocketFactory;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SampleOkHttpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_ok_http);


    }

    private void init ()
    {
        String host = "myshadowhost.org";
        int port = 8989;

        ShadowConfig sConfig = new ShadowConfig("secret","password");
        OkHttpClient client = new OkHttpClient.Builder()
                .socketFactory(new ShadowSocketFactory(sConfig, host, port)).build();

        Request request = new Request.Builder().url("https://foo.com").build();

        try {
            client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}