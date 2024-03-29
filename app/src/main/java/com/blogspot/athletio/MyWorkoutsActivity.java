package com.blogspot.athletio;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Vector;

import adapters.WorkoutAdapter;
import general.Workout;
import services.FirebaseUploadService;
import stepdetector.StepDetector;
import storage.SharedPrefData;

public class MyWorkoutsActivity extends AppCompatActivity {
    DatabaseReference mDatabase,mWorkoutkeyDatabase;
    FirebaseAuth mAuth;
    Vector<String> workoutKeys=new Vector<String>();
    List<Workout> workouts=new Vector<Workout>();
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_workouts);
        mAuth=FirebaseAuth.getInstance();
        mWorkoutkeyDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid()).child("workouts");
        mDatabase=FirebaseDatabase.getInstance().getReference().child("workouts");

        setupUI();

       recyclerView = (RecyclerView) findViewById(R.id.my_workout_layout_card_list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);


        ///Retrieves workouts from online database
        mWorkoutkeyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                for(DataSnapshot d : dataSnapshot.getChildren()) {
                    workoutKeys.add(d.getValue().toString());
                }
                for (final String key:workoutKeys){
                    mDatabase.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshotw) {

                            if(dataSnapshotw.getValue()!=null)
                            {
                                Workout workout=new Workout(dataSnapshotw.getValue().toString());
                                workout.key=key;
                                workouts.add(workout);
                                updateUI();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }


        });

    }


    void setupUI(){

    }

    void updateUI(){

        recyclerView.setAdapter(new WorkoutAdapter(workouts));

    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_home:
                startActivity(new Intent(this,MainActivity.class));
                finish();
                return true;
            case R.id.menu_track_workout:
                startActivity(new Intent(this, TrackWorkoutMenuActivity.class));
                finish();
                return true;
            case R.id.menu_online_workout:
                startActivity(new Intent(this, OnlineWorkoutActivity.class));
                finish();
                return true;
            case R.id.menu_my_workouts:
                startActivity(new Intent(this, MyWorkoutsActivity.class));
                finish();
                return true;
            case R.id.menu_excersices:
                startActivity(new Intent(this, ExercisesActivity.class));
                finish();
                return true;
            case R.id.menu_social:
                startActivity(new Intent(this, SocialMainActivity.class));
                finish();
                return true;
            case R.id.menu_events:
                startActivity(new Intent(this, EventsActivity.class));
                finish();
                return true;
            case R.id.menu_event_reminder:
                startActivity(new Intent(this, ShowEventRemindersActivity.class));
                finish();
                return true;
            case R.id.menu_create_event:
                startActivity(new Intent(this, CreateEventActivity.class));
                finish();
                return true;
            case R.id.menu_nearby_place:
                startActivity(new Intent(this, MapsActivity.class));
                finish();
                return true;
            case R.id.menu_chat_bot:
                startActivity(new Intent(this, ChatBotMain.class));
                finish();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            case R.id.menu_signout:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    void signOut(){
        SharedPrefData sharedPrefData=new SharedPrefData(this);
        sharedPrefData.clear();
        Intent intent=new Intent(this, FirebaseUploadService.class);
        stopService(intent);

        Intent intent2=new Intent(this, StepDetector.class);
        stopService(intent2);
        FirebaseAuth.getInstance().signOut();
    }

}
