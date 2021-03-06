package rvrc.geq1917.g34.android.diningmania;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stepstone.apprating.AppRatingDialog;
import com.stepstone.apprating.listener.RatingDialogListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static rvrc.geq1917.g34.android.diningmania.Home.user;
import static rvrc.geq1917.g34.android.diningmania.Home.studentId;
import static rvrc.geq1917.g34.android.diningmania.MatchingResult.POINT_AMOUNT;

public class ShowTransaction extends AppCompatActivity implements RatingDialogListener {

    private static String TAG = "ShowTransaction";

    private DatabaseHelper mySQDatabase;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reviews;
    private static FirebaseAuth mAuth;
    private static DatabaseReference mDatabase;
    private static DatabaseReference currentUserRef;
    private static String currUserId;

    private String date;
    private String meal;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_transaction);

        ListView lv_records = findViewById(R.id.lv_transactions);
        mySQDatabase = new DatabaseHelper(this);
        firebaseDatabase = FirebaseDatabase.getInstance();

        ArrayList<String> recordList = new ArrayList<>();
        Cursor data = mySQDatabase.getListContents(DatabaseHelper.TABLE_TRANSACTIONS);

        if(data.getCount() == 0) {
            Toast.makeText(this, "Nothing to show.", Toast.LENGTH_LONG);
        } else {
            while (data.moveToNext()) {
                recordList.add(data.getString(0) + "   " + data.getString(1)
                        + "\n" + data.getString(2));
            }
            ListAdapter listAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, recordList);
            lv_records.setAdapter(listAdapter);

            lv_records.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView textView = (TextView) view;
                    date = textView.getText().toString().split("\n")[0].substring(0,10);
                    meal = textView.getText().toString().split("\n")[1];
                    reviews = firebaseDatabase.getReference("Dinner Reviews").child(date).child(meal)
                            .child(studentId);
                    reviews.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()) {
                                showReviewDialog(meal, date);
                                addPoint();
                            } else {
                                showInfoDialog();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            });
        }
    }

    private void showReviewDialog(String meal, String date) {
        new AppRatingDialog.Builder()
                .setPositiveButtonText("Submit")
                .setNegativeButtonText("Cancel")
                .setNumberOfStars(5)
                .setDefaultRating(0)
                .setTitle("Give feedback for " + meal)
                .setDescription("We will appreciate it if you could give detailed feedback")
                .setDescriptionTextColor(android.R.color.black)
                .setHint("Write your comments here. e.g. The fish is salty; " +
                        "Need more sources for the noodles...")
                .setHintTextColor(android.R.color.darker_gray)
                .setCommentBackgroundColor(R.color.color1)
                .setWindowAnimation(R.style.RatingDialogFadeAnim)
                .create(this)
                .show();
    }

    public void showInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Feedback exists");
        builder.setMessage("\nYou have already given feedback for this meal!");
        builder.setNeutralButton("Okay",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onPositiveButtonClicked(int value, @NotNull String comment) {
        reviews.child(Integer.toString(value)).setValue(comment);
        mySQDatabase.addReview(date, meal, comment);
        Toast.makeText(this,"Thanks for you comments! One point has been credited to " +
                "you!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNegativeButtonClicked() {
    }

    public void addPoint(){
        mAuth = FirebaseAuth.getInstance();
        currUserId = mAuth.getCurrentUser().getUid();
        mDatabase= FirebaseDatabase.getInstance().getReference();
        currentUserRef = mDatabase.child("Users").child(currUserId);
        user.earnPoint(POINT_AMOUNT);
        currentUserRef.setValue(user);
    }
}
