package com.mina.chatter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.transition.Fade;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mina.chatter.R;
import com.mina.chatter.adapters.UsersAdapter;
import com.mina.chatter.databinding.ActivityUsersBinding;
import com.mina.chatter.fragments.PictureFragment;
import com.mina.chatter.listeners.PictureListener;
import com.mina.chatter.listeners.UserListener;
import com.mina.chatter.models.User;
import com.mina.chatter.utilities.Constants;
import com.mina.chatter.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener , PictureListener {
    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Chatter);
        binding=ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("userActivity");
        modifyTransition();
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
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
    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                   loading(false);
                   String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                   if(task.isSuccessful() && task.getResult()!=null){
                       List<User> users = new ArrayList<>();
                       for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                           if(currentUserId.equals(queryDocumentSnapshot.getId())){
                               continue;
                           }
                           User user  = new User();
                           user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                           user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                           user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                           user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                           user.id = queryDocumentSnapshot.getId();
                           users.add(user);
                       }
                       if(users.size() > 0 ){
                           UsersAdapter usersAdapter = new UsersAdapter(users,this,this);
                           binding.userRecyclerView.setAdapter(usersAdapter);
                           binding.userRecyclerView.setVisibility(View.VISIBLE);
                       }else{
                           showErrorMessage();
                       }
                   }else{
                       showErrorMessage();
                   }
                });
    }
    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading (Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user, TextView textView) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(UsersActivity.this,textView, ViewCompat.getTransitionName(textView));
        startActivity(intent,options.toBundle());
        finish();
    }

    @Override
    public void onPictureClicked(Bitmap bitmap) {
        PictureFragment pictureFragment = new PictureFragment(bitmap);
        pictureFragment.show(getSupportFragmentManager(),null);
    }
}