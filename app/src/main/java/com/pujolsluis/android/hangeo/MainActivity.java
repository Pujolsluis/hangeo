package com.pujolsluis.android.hangeo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Context context = this;
    private DrawerLayout mDrawerLayout;
    private FloatingActionButton mCreatePlanButton;
    private static final int WELCOME_SCREEN_RESPONSE = 1;
    private static final int CREATE_PLAN_RESPONSE = 2;
    private FirebaseUser mUser;
    private String mUserID;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mProfileDatabaseReference;
    private String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mProfileDatabaseReference = mFirebaseDatabase.getReference().child("userProfiles");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        navigationView.setCheckedItem(R.id.nav_plans);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        if (viewPager != null) {
            setupViewPager(viewPager);
        }

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        mCreatePlanButton = (FloatingActionButton) findViewById(R.id.fab_create_plan);

        mCreatePlanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PlanCreationActivity.class);
                intent.putExtra("mUserID", mUser.getUid());
                startActivityForResult(intent, CREATE_PLAN_RESPONSE);
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    onSignedInInitialize(user);

                }else{
                    onSignoutCleanUp();
                    //user is signed out
                    Intent intent = new Intent(context, WelcomeScreenActivity.class);
                    startActivityForResult(intent,WELCOME_SCREEN_RESPONSE);
                }
            }
        };
    }

    private void onSignoutCleanUp() {
        mUser = null;
    }

    private void onSignedInInitialize(FirebaseUser user) {
        mUser = user;
        mUserID = user.getUid();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.string.menu_settings:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Adding Fragments to the ViewPager
    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(new PlanListFragment(), "My Plans");
        adapter.addFragment(new PlanListFragment(), "Recommended Plans");
        viewPager.setAdapter(adapter);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()){
                            case R.id.nav_home:
                                Intent intent = new Intent(context, MapsActivity.class);
                                mDrawerLayout.closeDrawer(GravityCompat.START);
                                context.startActivity(intent);
                                return true;
                            case R.id.nav_sign_out:
                                if(mFirebaseAuth != null){
                                    //sign out
                                    mFirebaseAuth.signOut();
                                    mDrawerLayout.closeDrawer(GravityCompat.START);
                                }
                                return true;

                        }
                        return true;
                    }
                });
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == WELCOME_SCREEN_RESPONSE){
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();

                mProfileDatabaseReference.child(mUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Get user information
                                UserProfile tempProfile = dataSnapshot.getValue(UserProfile.class);
                                if(tempProfile == null) {

                                    tempProfile = new UserProfile();
                                    tempProfile.setmFirstName(mUser.getDisplayName());


                                    Map<String, Object> profileValues = tempProfile.toMap();

                                    Map<String, Object> childUpdates = new HashMap<>();
                                    childUpdates.put(mUserID, profileValues);
                                    mProfileDatabaseReference.updateChildren(childUpdates);

//                                // Create new comment object
//                                String commentText = mCommentField.getText().toString();
//                                Comment comment = new Comment(uid, authorName, commentText);
//
//                                // Push the comment, it will appear in the list
//                                mCommentsReference.push().setValue(comment);
//
//                                // Clear the field
//                                mCommentField.setText(null);
                                }else{
                                    Toast.makeText(context, "Profile is already created", Toast.LENGTH_SHORT).show();
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(LOG_TAG, databaseError.toString());
                            }
                        });

            }else if(resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }

        }else if(requestCode == CREATE_PLAN_RESPONSE){
            if(resultCode == RESULT_OK){
                Toast.makeText(context, "Your Plan was created", Toast.LENGTH_SHORT).show();
            }else{

            }
        }
    }

    @Override
    protected void onResume() {
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        super.onStart();
    }

    @Override
    protected void onPause() {
        if(mFirebaseAuth != null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        super.onStop();
    }


}


