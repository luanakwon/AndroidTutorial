package com.example.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private EditText edtTxtPhoneNo;
    private EditText edtTxtMsg;
    private List<Contacts> myContacts = new ArrayList<>();
    private int state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.textView = findViewById(R.id.textView);
        this.edtTxtPhoneNo = findViewById(R.id.edtTxtPhoneNo);
        this.edtTxtMsg = findViewById(R.id.edtTxtMsg);
        this.textView.setText("Hello User");
        this.setMainMenu();
    }

    public void setMainMenu(){
        this.textView.setText(
                "Hello User\n\n"+
                "1. Manage contacts\n\n"+
                "2. Messages\n\n"+
                "3. Quit \n\n");
        this.state = 0;
    }

    public void setManageContacts(){
        this.textView.setText(
                "===Manage Contacts===\n\n"+
                "1. Show all contacts\n\n"+
                "2. Add a new contact\n\n"+
                "3. Search for a contact\n\n"+
                "4. Delete a contact\n\n"+
                "5. Go back to the previous menu\n\n");
        this.edtTxtMsg.setHint("Name");
        this.state = 1;
    }

    public void setMessages(){
        this.textView.setText(
                "===Messages===\n\n"+
                "1. See the list of all messages\n\n"+
                "2. Send a new message\n\n"+
                "3. Go back to the previous menu\n\n");
        this.edtTxtMsg.setHint("Message");
        this.state = 2;
    }

    public void pseudoQuit(){
        this.textView.setText("Quit");
        this.state = 3;
    }

    public void onClkBtn1(View view) {
        if (this.state == 0) {
            this.setManageContacts();
        } else if (this.state == 1) {
            this.setManageContacts();
            this.textView.append(String.valueOf(this.myContacts.size()) + " contacts\n");
            for (Contacts c : this.myContacts) {
                this.textView.append(c.getPhoneNo() + "\n");
            }
        } else if (this.state == 2) {
            this.setMessages();
            this.textView.append(String.valueOf(this.myContacts.size()) + " dialogs\n");
            for (Contacts c : this.myContacts) {
                this.textView.append("Message with " + c.getName() + "\n");
                this.textView.append("\t" + c.getMsgDialog().get(c.getMsgDialog().size() - 1));
            }
        }
    }

    public void onClkBtn2(View view){
        if(this.state == 0) {
            this.setMessages();
        } else if(this.state == 1) {
            String phoneNo = this.edtTxtPhoneNo.getText().toString();
            String name = this.edtTxtMsg.getText().toString();
            this.myContacts.add(new Contacts(name, phoneNo));
        }
        //TODO if state ==2 send new message
    }
    public void onClkBtn3(View view){
        if(this.state == 0){
            this.pseudoQuit();
        }
        //TODO if state==1 search for a contact
        //TODO if state==2 go back to the previous menu
    }
    public void onClkBtn4(View view){
        //TODO if state==1 delete a contact
    }
    public void onClkBtn5(View view){
        //TODO if state==1 go back to the previous menu
    }
//    public void onClkBtn(View view){
//
//
//        TextView txtFname = findViewById(R.id.txtFname);
//        TextView txtLname = findViewById(R.id.txtLname);
//        TextView txtEmail = findViewById(R.id.txtEmail);
//
//
//        EditText edtTxtFstName = findViewById(R.id.edtTxtFstName);
//        String fstNameTxt = edtTxtFstName.getText().toString();
//
//        EditText edtTxtLstName = findViewById(R.id.edtTxtLstName);
//        String lstNameTxt = edtTxtLstName.getText().toString();
//
//        EditText edtTxtEmail = findViewById(R.id.edtTxtEmail);
//        String emailTxt = edtTxtEmail.getText().toString();
//
//        txtFname.setText(fstNameTxt);
//        txtLname.setText(lstNameTxt);
//        txtEmail.setText(emailTxt);
//        // terminal commit test 2
//    }
}