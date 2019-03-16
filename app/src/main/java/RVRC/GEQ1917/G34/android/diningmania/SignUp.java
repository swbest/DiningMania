package RVRC.GEQ1917.G34.android.diningmania;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.regex.Pattern;

public class SignUp extends AppCompatActivity {

    private EditText et_email;
    private EditText et_password;
    private Button b_signUp;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        et_email = findViewById(R.id.signUp_tv_user_email);
        et_password = findViewById(R.id.signUp_tv_password);
        b_signUp = findViewById(R.id.signUp_b_sign_up);
        firebaseAuth = FirebaseAuth.getInstance();
        final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference usersRefer = firebaseDatabase.getReference("Users");

        b_signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidEmail(et_email.getText().toString())
                        && et_password.getText().toString().length() > 7) {
                    final ProgressDialog progressDialog = new ProgressDialog(SignUp.this);
                    progressDialog.setMessage("Registering...");
                    progressDialog.show();

                    (firebaseAuth.createUserWithEmailAndPassword(et_email.getText().toString(),
                            et_password.getText().toString())).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressDialog.dismiss();
                            if(task.isSuccessful()) {
                                Toast.makeText(SignUp.this, "Registration successful!",
                                        Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(SignUp.this, Home.class);
                                startActivity(intent);
                            }
                        }
                    });
                } else {
                    Toast.makeText(SignUp.this, "Please enter correct email address " +
                            "and Matrix number", Toast.LENGTH_LONG);
                }
            }
        });


    }

    private static boolean isValidEmail(String email)
    {
        final String EMAIL_REGEX = "^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]" +
                "+(\\.[\\w]+)*(\\.[a-z]{2,})$";
        Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
        if (email == null)
            return false;

        return pattern.matcher(email).matches();
    }
}
