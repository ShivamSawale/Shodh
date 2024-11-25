package com.example.shodh;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;

public class MapTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_type);

        // Set up buttons for different map types
        Button normalButton = findViewById(R.id.btn_normal);
        Button satelliteButton = findViewById(R.id.btn_satellite);
        Button terrainButton = findViewById(R.id.btn_terrain);
        Button hybridButton = findViewById(R.id.btn_hybrid);

        // Set up onClick listeners for each button
        normalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnMapTypeResult(GoogleMap.MAP_TYPE_NORMAL);
            }
        });

        satelliteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnMapTypeResult(GoogleMap.MAP_TYPE_SATELLITE);
            }
        });

        terrainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnMapTypeResult(GoogleMap.MAP_TYPE_TERRAIN);
            }
        });

        hybridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnMapTypeResult(GoogleMap.MAP_TYPE_HYBRID);
            }
        });
    }

    private void returnMapTypeResult(int mapType) {
        // Return the selected map type to HomeActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedMapType", mapType);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
