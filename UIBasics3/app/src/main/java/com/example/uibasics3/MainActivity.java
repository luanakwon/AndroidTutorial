package com.example.uibasics3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private CheckBox checkBoxHarry, checkBoxMatrix, checkBoxJoker;
    private RadioGroup rgMaritalStatus;
    private ProgressBar progressCircle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBoxHarry = findViewById(R.id.checkboxHarry);
        checkBoxMatrix = findViewById(R.id.checkboxMatrix);
        checkBoxJoker = findViewById(R.id.checkboxJoker);
        rgMaritalStatus = findViewById(R.id.rgMaritalStatus);
        progressCircle = findViewById(R.id.progressCircle);
        progressBar = findViewById(R.id.progressBar);

        // Thread for progress bar
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<10;i++){
                    progressBar.incrementProgressBy(10);
                    SystemClock.sleep(500);
                }
            }
        });
        thread.start();

        // initial toast msg for radio button group
        int checkedRB = rgMaritalStatus.getCheckedRadioButtonId();
        switch (checkedRB){
            case R.id.rbMarried:
                Toast.makeText(MainActivity.this, "Married", Toast.LENGTH_SHORT).show();
                break;
            case R.id.rbSingle:
                Toast.makeText(MainActivity.this, "Single", Toast.LENGTH_SHORT).show();
                break;
            case R.id.rbInRel:
                Toast.makeText(MainActivity.this, "In a Relationship", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        // toast msg for change in radio button group
        rgMaritalStatus.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.rbMarried:
                        Toast.makeText(MainActivity.this, "Married", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.rbSingle:
                        Toast.makeText(MainActivity.this, "Single", Toast.LENGTH_SHORT).show();
                        progressCircle.setVisibility(View.VISIBLE);
                        break;
                    case R.id.rbInRel:
                        Toast.makeText(MainActivity.this, "In a Relationship", Toast.LENGTH_SHORT).show();
                        progressCircle.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        });

        // initial toast msg for checkbox harry
        if (checkBoxHarry.isChecked()) {
            Toast.makeText(MainActivity.this, "You have watched Harry Potter, Yay", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "You NEED to watch Harry Potter", Toast.LENGTH_SHORT).show();
        }
        // toast msg for change in checkbox harry
        checkBoxHarry.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Toast.makeText(MainActivity.this, "You have watched Harry Potter, Yay", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "You NEED to watch Harry Potter", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}