package com.ktuedu.rtMessaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ktuedu.rtMessaging.Adapters.PersonListAdapter;
import com.ktuedu.rtMessaging.Models.User;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    Context context;
    EditText filterText;
    ArrayList<User> listOfUsers;
    ArrayList<User> listOfPersonsFiltered;
    ListView listView;
    ListAdapter listAdapter;
    String userName;

    ImageButton popUpMenu;

    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        checkLogIn();
        context = this;
        listOfUsers = new ArrayList<>();
        listView = findViewById(R.id.recyclerView);
        filterText = findViewById(R.id.search_editText);

        popUpMenu = findViewById(R.id.pop_up_menu);
        popUpMenu.setOnClickListener(popUpMenuListener);

        filterText.addTextChangedListener(watcher);
        listOfPersonsFiltered = listOfUsers;
        listView.setOnItemClickListener(onItemClickListener);

        FirebaseMessaging.getInstance().setAutoInitEnabled(true);


        getUserList();
        updateListView();
        registerNotification();
    }

    View.OnClickListener popUpMenuListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //Creating the instance of PopupMenu
            PopupMenu popup = new PopupMenu(MainActivity.this, popUpMenu);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.pop_up_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    //Toast.makeText(MainActivity.this,"You Clicked : " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    if(item.getItemId() == R.id.log_out){
                        LogOut();
                    }
                    if(item.getItemId() == R.id.temporary_functions){
                        runTempFuncActivity();
                    }
                    if(item.getItemId() == R.id.update_account_info){
                        runUpdateActivity();
                    }


                    return true;
                }
            });

            popup.show();//showing popup menu
        }
    };


    private void registerNotification(){
        if(user != null) {
            FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + user.getUid())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            String msg = "Subscribed";
                            if (!task.isSuccessful()) {
                                msg = "Failed";
                            }
                            Log.e("RegisterNotification", msg);
                        }
                    });
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        getUserList();
        updateListView();

    }

    private void checkLogIn(){
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Start sign in/sign up activity
            Intent runIntent = new Intent(this, LoginActivity.class);
            startActivity(runIntent);
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
        }
    }

    private void getUserList() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            listOfUsers.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (!document.getId().equals(user.getUid())) {
                                    Map<String, Object> map = document.getData();
                                    Log.d("User", document.getId() + " => " + map);

                                    Map<String, Object> userMap = (Map<String, Object>) map.get(document.getId());
                                    if (userMap != null) {
                                        String name = userMap.get("username").toString();
                                        String img = userMap.get("imageUrl").toString();


                                        User user = new User(document.getId(),name, img);
                                        listOfUsers.add(user);
                                    }
                                }else{
                                    Map<String, Object> map = document.getData();
                                    Map<String, Object> userMap = (Map<String, Object>) map.get(document.getId());
                                    if (userMap != null) {
                                        userName = userMap.get("username").toString();
                                    }
                                }
                            }
                            updateListView();
                        } else {
                            Log.e("Users", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void LogOut(){

        FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/" + user.getUid());
        FirebaseAuth.getInstance().signOut();
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence s, int i0, int i1, int i2) {
            if (s.toString().length() > 0 && s.toString().length() < 2) {
                listOfPersonsFiltered = new ArrayList<>();
                for (int i = 0; i < listOfUsers.size(); i++) {
                    User p = listOfUsers.get(i);
                    if (p.Name.toLowerCase().startsWith(s.toString().toLowerCase())) {
                        listOfPersonsFiltered.add(p);
                    }
                }
            } else {
                listOfPersonsFiltered = listOfUsers;
            }
            updateListView();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.e("OnItemClick", "Clicked on: " +listOfPersonsFiltered.get(i).Name);
            startChatActivity(listOfPersonsFiltered.get(i));

        }
    };

    private void startChatActivity(User u) {
        Intent runIntent = new Intent(this, ChatActivity.class);
        runIntent.putExtra("userId",u.getID());
        runIntent.putExtra("userName",userName);

        startActivity(runIntent);
    }

    private void runTempFuncActivity() {
        Intent runIntent = new Intent(this, tempFunctions.class);
        startActivity(runIntent);
    }

    private void runUpdateActivity(){
        Intent runIntent = new Intent(this, updateAccountInfo.class);
        startActivity(runIntent);
    }


    public void updateListView() {
        listAdapter = new PersonListAdapter(context, listOfPersonsFiltered);
        listView.setAdapter(listAdapter);
    }
}
