package rvrc.geq1917.g34.android.diningmania;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static rvrc.geq1917.g34.android.diningmania.Login.filename;
import static rvrc.geq1917.g34.android.diningmania.Utility.formatDate;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DatePickerDialog.OnDateSetListener {


    private static final Date beginDate = new GregorianCalendar(2019, Calendar.APRIL, 1).getTime();
    private static final Date endDate = new GregorianCalendar(2019, Calendar.APRIL, 6).getTime();
    private TextView tv_dUsedCredit;
    private TextView tv_dLeftCredit;
    private TextView tv_leftPoint;
    private Button b_calendar;
    private RecyclerView recyclerMenu;
    private RecyclerView.LayoutManager layoutManager;
    private Date chosenDate;
    public DatabaseHelper mySQDatabase;

    protected static String studentId;
    protected static User user;
    protected static FirebaseAuth mAuth;
    protected static DatabaseReference mDatabase;
    protected static DatabaseReference userRef;
    protected static DatabaseReference dinnerChoice;
    protected static String currUserId;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //App-use functions
        View headerView = navigationView.getHeaderView(0);
        tv_dUsedCredit = headerView.findViewById(R.id.nav_tv_dUsedCredit);
        tv_dLeftCredit = headerView.findViewById(R.id.nav_tv_dLeftCredit);
        tv_leftPoint = headerView.findViewById(R.id.nav_tv_leftPoint);
        b_calendar = findViewById(R.id.home_b_calender);
        b_calendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment calendar = new CalendarFragment();
                calendar.show(getSupportFragmentManager(), "date pick");
            }
        } );
        //lv_transactionList = headerView.findViewById(R.id.transaction_lv_container);
        //default date: today
        chosenDate = Calendar.getInstance().getTime();
        mySQDatabase = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        currUserId = mAuth.getCurrentUser().getUid();
        mDatabase= FirebaseDatabase.getInstance().getReference();
        userRef = mDatabase.child("Users");
        sp = getSharedPreferences(filename, Context.MODE_PRIVATE);
        dinnerChoice = mDatabase.child("Dinner Choice");

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(currUserId)){
                    studentId = Login.stuId;
                    user = new User(studentId);
                    userRef.child(currUserId).setValue(user);
                } else {
                    user = dataSnapshot.child(currUserId).getValue(User.class);
                    studentId = user.getStudentId();
                    updateUI();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        //load menu
        recyclerMenu = findViewById(R.id.recycler_menu);
        recyclerMenu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerMenu.setLayoutManager(layoutManager);
        loadMenu(formatDate(chosenDate));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar chosenCalendar = Calendar.getInstance();
        chosenCalendar.set(Calendar.YEAR, year);
        chosenCalendar.set(Calendar.MONTH,month);
        chosenCalendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);
        chosenDate = chosenCalendar.getTime();
        SimpleDateFormat specialDateFormat = new SimpleDateFormat("dd MMM YYYY");
        b_calendar.setText(specialDateFormat.format(chosenDate));
        loadMenu(formatDate(chosenDate));
    }


    private void loadMenu(final String date) {
        FirebaseRecyclerAdapter<FoodChoice, MenuHolder> adapter = new FirebaseRecyclerAdapter<FoodChoice, MenuHolder>
                (FoodChoice.class, R.layout.menu_items, MenuHolder.class, dinnerChoice) {
            @Override
            protected void populateViewHolder(MenuHolder viewHolder, FoodChoice model, int position) {
                viewHolder.tv_choice.setText(model.getChoice());
                String menuContent = model.getDaily_menu().get(date);
                if(menuContent != null) {
                    viewHolder.tv_content.setText(textFormat(menuContent));
                } else {
                    viewHolder.tv_content.setText("");
                }

                final FoodChoice clickItem = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        if (canAcceptChange()) {
                            if (chosenDate.after(beginDate) && chosenDate.before(endDate)) {
                                showConfirmationDialog(date, clickItem.getChoice());
                            } else {
                                showInfoDialog("The chosen date is beyond this trial",
                                        "Please select a date between 1st April and 5th April");
                            }

                        } else {
                            showInfoDialog("Food has already been prepared :(",
                                    "Please select your meal choices at least one day in advance.");
                        }
                    }
                });
            }
        };
        recyclerMenu.setAdapter(adapter);
    }

    private String textFormat(String rawString) {
        return rawString.replaceAll("%","\n");
    }

    private boolean canAcceptChange() {
        Calendar dueDate = Calendar.getInstance();
        dueDate.add(Calendar.HOUR_OF_DAY, +23);
        return chosenDate.after(dueDate.getTime());
    }

    private void addLocalDate(String date, String indication) {
        boolean insertDate = mySQDatabase.addRecord(date, indication);
        if(insertDate) {
            makeToast("Choice was successfully recorded!");
        } else {
            makeToast("Something went wrong. Please try again.");
        }
    }

    public void showConfirmationDialog(final String date, final String choice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
        builder.setCancelable(true);
        builder.setTitle("Are you sure you want to select " + choice + " ?");
        builder.setMessage("You can change your choice up to one day before the actual date.");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        user.addDinnerIndication(date, choice);
                        addLocalDate(date, choice);
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showInfoDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(msg);
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
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;

        switch (item.getItemId()){
            case R.id.nav_scan:
                boolean hasScanned = false;
                Cursor data = mySQDatabase.getListContents(DatabaseHelper.TABLE_TRANSACTIONS);
                while (data.moveToNext()) {
                    if(data.getString(0).equals(formatDate(Calendar.getInstance().getTime()))) {
                        String date = data.getString(0);
                        hasScanned = true;
                    }
                }
                if (hasScanned) {
                    showInfoDialog("Unable to scan",
                            "\nYou have already scanned for today's meal");
                } else {
                    startActivity(new Intent(this, Scan.class));
                }
                break;
            case R.id.nav_transaction:
                startActivity(new Intent(this, ShowTransaction.class));
                break;
            case R.id.nav_selection_records:
                startActivity(new Intent(this, SelectionRecords.class));
                break;
            case R.id.nav_feedback:
                startActivity(new Intent(this, Feedback.class));
                break;
            case R.id.nav_logout:
                mAuth.signOut();
                sp.edit().putBoolean("isLoggedIn",false).apply();
                intent = new Intent(this,Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;
            default:

        }


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateUI(){
        tv_dUsedCredit.setText(String.format("%d",user.getDUsedCredit()));
        tv_dLeftCredit.setText(String.format("%d",user.getDLeftCredit()));
        tv_leftPoint.setText(String.format("%d",user.getLeftPoint()));
    }

    public static class MenuHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public TextView tv_choice;
        public TextView tv_content;

        private ItemClickListener itemClickListener;

        public MenuHolder(@NonNull View itemView) {
            super(itemView);
            tv_choice = itemView.findViewById(R.id.tv_food_choice);
            tv_content = itemView.findViewById(R.id.tv_food_content);
            itemView.setOnClickListener(this);
        }

        public void setItemClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onClick(view, getAdapterPosition(), false);
        }
    }

    private void makeToast(String msg) {
        Toast.makeText(Home.this, msg, Toast.LENGTH_LONG).show();
    }

}
