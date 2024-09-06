package com.litron.litronremote;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BantuanActivity extends AppCompatActivity {
    @BindView(R.id.tvVersion)
    TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bantuan);
        ButterKnife.bind(this);

        tvVersion.setText(String.format("v %s", BuildConfig.VERSION_NAME));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.stay, R.anim.slide_down);
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down);
    }
}
