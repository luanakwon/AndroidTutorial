package com.example.recyclerviewexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView contactsRecView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactsRecView = findViewById(R.id.contactsRCView);

        ArrayList<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact("Elon","ElonPersonal@gmail.com",getResources().getString(R.string.img_doge)));
        contacts.add(new Contact("Como","Como123@gmail.com",getResources().getString(R.string.img_babyShark)));
        contacts.add(new Contact("llamas","llamas456@gmail.com",getResources().getString(R.string.img_burger)));
        contacts.add(new Contact("hablar","hablar789@gmail.com",getResources().getString(R.string.img_beach)));
        contacts.add(new Contact("coreano","uncoreano@gmail.com",getResources().getString(R.string.img_firework)));

        ContactsRecViewAdapter adapter = new ContactsRecViewAdapter(this);
        adapter.setContacts(contacts);

        contactsRecView.setAdapter(adapter);
        contactsRecView.setLayoutManager(new LinearLayoutManager(this));
        //contactsRecView.setLayoutManager(new LinearLayoutManager(this,  LinearLayoutManager.HORIZONTAL, false));
        //contactsRecView.setLayoutManager(new GridLayoutManager(this,2));
    }
}