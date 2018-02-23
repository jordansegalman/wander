package com.example.kyle.wander;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Delete extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete);
    }

    public void delete(View view){
        //TODO: delete account

        startActivity(new Intent(Delete.this, Login.class));
    }

    public void back(View view){
        startActivity(new Intent(Delete.this, Settings.class));
    }
}
