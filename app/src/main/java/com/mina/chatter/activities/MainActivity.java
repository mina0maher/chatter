package com.mina.chatter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.transition.Fade;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mina.chatter.R;
import com.mina.chatter.adapters.RecentConversionsAdapter;
import com.mina.chatter.databinding.ActivityMainBinding;
import com.mina.chatter.fragments.PictureFragment;
import com.mina.chatter.listeners.ConversionListener;
import com.mina.chatter.listeners.PictureListener;
import com.mina.chatter.models.ChatMessage;
import com.mina.chatter.models.User;
import com.mina.chatter.utilities.Constants;
import com.mina.chatter.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener , PictureListener {
    private ActivityMainBinding binding ;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversionsAdapter conversionsAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Chatter);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("mainActivity");
        modifyTransition();
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversions();
    }
    public void modifyTransition(){
        Fade fade = new Fade();
        fade.excludeTarget(getActionBarView(),true);
        fade.excludeTarget(android.R.id.statusBarBackground,true);
        fade.excludeTarget(android.R.id.navigationBarBackground,true);
        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);
    }
    public View getActionBarView() {
        Window window = getWindow();
        View v = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", "android");
        return v.findViewById(resId);
    }

    private void init(){
        conversations = new ArrayList<>();
        conversionsAdapter = new RecentConversionsAdapter(conversations,this,this);
        binding.conversionsRecyclerView.setAdapter(conversionsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(),UsersActivity.class));
        });
        binding.imageProfile.setOnClickListener(v -> {
            byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            PictureFragment pictureFragment = new PictureFragment(bitmap);
            pictureFragment.show(getSupportFragmentManager(),null);
        });
    }
    private void loadUserDetails(){
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), android.util.Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    private void showToast (String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void listenConversions(){
        database.collection(Constants.KEY_COLLECTION_CONVERSIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

    }

    private final EventListener<QuerySnapshot>eventListener = (value, error) -> {
      if(error != null){
          return;
      }
      if(value != null){
          for(DocumentChange documentChange:value.getDocumentChanges()){
              if(documentChange.getType() == DocumentChange.Type.ADDED){
                  String senderId =documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                  String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                  ChatMessage chatMessage = new ChatMessage();
                  chatMessage.senderId = senderId;
                  chatMessage.receiverId = receiverId;
                  if(preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                      chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                      chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                      chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                  }else{
                      chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                      chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                      chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                  }
                  chatMessage.message  = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                  chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                  conversations.add(chatMessage);
              }else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                  for(int i = 0 ; i< conversations.size();i++){
                      String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                      String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                      if(conversations.get(i).senderId.equals(senderId)&&conversations.get(i).receiverId.equals(receiverId)){
                          conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                          conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                          break;
                      }
                  }
              }
          }
          Collections.sort(conversations,(obj1,obj2)-> obj2.dateObject.compareTo(obj1.dateObject));
          conversionsAdapter.notifyDataSetChanged();
          binding.conversionsRecyclerView.smoothScrollToPosition(0);
          binding.conversionsRecyclerView.setVisibility(View.VISIBLE);
          binding.progressBar.setVisibility(View.GONE);
      }
    };

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN,token);
        FirebaseFirestore database =FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }
    private void signOut (){
        showToast("Singing out ...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String,Object> updates =new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversionClicked(User user, TextView textView) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this,textView, ViewCompat.getTransitionName(textView));
        startActivity(intent,options.toBundle());
    }

    @Override
    public void onPictureClicked(Bitmap bitmap) {
        PictureFragment pictureFragment=new PictureFragment(bitmap);
        pictureFragment.show(getSupportFragmentManager(),null);
    }
}