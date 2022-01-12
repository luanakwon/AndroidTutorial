package com.example.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onClkBtn(View view){
        TextView txtFname = findViewById(R.id.txtFname);
        TextView txtLname = findViewById(R.id.txtLname);
        TextView txtEmail = findViewById(R.id.txtEmail);


        EditText edtTxtFstName = findViewById(R.id.edtTxtFstName);
        String fstNameTxt = edtTxtFstName.getText().toString();

        EditText edtTxtLstName = findViewById(R.id.edtTxtLstName);
        String lstNameTxt = edtTxtLstName.getText().toString();

        EditText edtTxtEmail = findViewById(R.id.edtTxtEmail);
        String emailTxt = edtTxtEmail.getText().toString();

        txtFname.setText(fstNameTxt);
        txtLname.setText(lstNameTxt);
        txtEmail.setText(emailTxt);
    }
}