package com.ktuedu.rtMessaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.ktuedu.rtMessaging.Adapters.MessageAdapter;
import com.ktuedu.rtMessaging.Models.localMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatActivity extends AppCompatActivity {


    FirebaseFirestore database;

    String userID;
    String otherID;
    String userNameString;


    ImageView userImage;
    TextView userName;

    EditText messageText;
    ImageButton sendMessageBtn;
    MessageAdapter messageAdapter;
    RecyclerView recyclerView;
    List<localMessage> localMessageList;
    List<localMessage> fromLocalMessageList;
    List<localMessage> toLocalMessageList;


    Context context;
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        if (intent.hasExtra("userId")) {
            otherID = intent.getStringExtra("userId");
        } else {
            finish();
        }
        if (intent.hasExtra("userName")) {
            userNameString = intent.getStringExtra("userName");
        }
        else{
            userNameString = "none";
        }
        fromLocalMessageList = new ArrayList<>();
        toLocalMessageList = new ArrayList<>();
        localMessageList = new ArrayList<>();
        context = getApplicationContext();
        database = FirebaseFirestore.getInstance();
        userImage = findViewById(R.id.chat_user_image);
        userName = findViewById(R.id.chat_username);
        getUserData();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        messageText = findViewById(R.id.chat_message_text);
        sendMessageBtn = findViewById(R.id.chat_send_button);
        sendMessageBtn.setOnClickListener(sendMessageClickListener);

        recyclerView = findViewById(R.id.chat_messages_recycler);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);

        readMessages();

    }


    private void getUserData() {
        DocumentReference docRef = database.collection("users").document(otherID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        Map<String, Object> map = document.getData();
                        Log.d("User", document.getId() + " => " + map);

                        Map<String, Object> userMap = (Map<String, Object>) map.get(document.getId());
                        if (userMap != null) {
                            String name = userMap.get("username").toString();
                            String photo = userMap.get("imageUrl").toString();

                            if (photo.equals("none")) {
                                userImage.setImageResource(R.drawable.index);
                            } else {
                                Glide.with(context).load(photo).into(userImage);
                            }
                            userName.setText(name);
                        }
                    }
                } else {
                    Log.d("CHAT", "get failed with ", task.getException());
                }
            }
        });


    }

    View.OnClickListener sendMessageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            sendMessage();
        }
    };

    private void sendMessage() {

        FirebaseFirestore database = FirebaseFirestore.getInstance(); //Initialize cloud firestore

        String messageValue = messageText.getText().toString();

        HashMap<String, String> message = new HashMap<>();
        message.put("from", userID);
        message.put("to", otherID);
        message.put("date", formatter.format(new Date()));
        message.put("text", messageValue);

        database.collection("messages").document().set(message); //add message to Firestore

        messageText.setText("");

        createNotification(messageValue);

    }

    private String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private String serverKey =
            "key=" + "AAAAadur8ho:APA91bG0wsLjFDST1YF7a0YcIUvd_LpbwD2Mlm9uMBUT0ZrPuFdthS5ZwwyJXNLgiFQB-Q23iASpSg_VLCd0w29QIByLRV901_7SNrbiqEXuqgaSyHXw2W1AWThx__oQg-burfACeNM9";
    private String contentType = "application/json";

    public void createNotification(String text) {


        if(text.length()>30){
            text= text.subSequence(0,30)+"...";
        }

        String topic = "/topics/"+otherID; //topic has to match what the receiver subscribed to
        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put("title", userNameString);
            notificationBody.put("message", text) ;  //Enter your notification message
            notificationBody.put("otherID", userID) ;  //Enter your notification message
            notification.put("to", topic);
            notification.put("data", notificationBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendNotification(notification);
    }


        private void sendNotification(JSONObject notification) {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i("RESPONSE", "onResponse: " + response.toString());

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i("RESPONSE", "onErrorResponse: Didn't work");
                        }
                    }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", serverKey);
                    params.put("Content-Type", contentType);
                    return params;
                }
            };
            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void updateMessageRecyclerView(){
        localMessageList = new ArrayList<>();
        localMessageList.addAll(fromLocalMessageList);
        localMessageList.addAll(toLocalMessageList);
        Collections.sort(localMessageList);

        messageAdapter = new MessageAdapter(context, localMessageList);
        recyclerView.setAdapter(messageAdapter);
    }

    private void readMessages() {

        database.collection("messages")
                .whereEqualTo("from", userID)
                .whereEqualTo("to", otherID)
                .addSnapshotListener(fromListener);

        database.collection("messages")
                .whereEqualTo("from", otherID)
                .whereEqualTo("to", userID)
                .addSnapshotListener(toListener);

        updateMessageRecyclerView();
    }

    EventListener<QuerySnapshot> fromListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value,
                            @Nullable FirebaseFirestoreException e) {
            if (e != null) {
                Log.w("Read", "Listen failed.", e);
                return;
            } else {
                fromLocalMessageList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    if (doc.get("text") != null) {
                        fromLocalMessageList.add(parseMessage(doc));
                    }
                }
                updateMessageRecyclerView();
            }
        }
    };

    EventListener<QuerySnapshot> toListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value,
                            @Nullable FirebaseFirestoreException e) {
            if (e != null) {
                Log.w("Read", "Listen failed.", e);
                return;
            } else {

                toLocalMessageList.clear();// gets all messages so we need to clear
                for (QueryDocumentSnapshot doc : value) {
                    if (doc.get("text") != null) {
                        toLocalMessageList.add(parseMessage(doc));
                    }
                }
                updateMessageRecyclerView();
            }
        }
    };


    private localMessage parseMessage(QueryDocumentSnapshot doc){
        localMessage m = new localMessage();
        m.setFromId((String) doc.get("from"));
        m.setText((String) doc.get("text"));
        m.setToId((String) doc.get("to"));
        m.setDate((String) doc.get("date"));

        return  m;
    }

}

//same for toListener
