package app.frontend.app.src.main.java.app.frontend;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import app.R;
import app.backend.AppBackend;


public class LoginActivity extends AppCompatActivity {

    EditText inputUsername;
    Button loginBtn;
    AppBackend appBackend;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        loginBtn = (Button) findViewById(R.id.loginBtn);
        inputUsername = (EditText) findViewById(R.id.inputUsername);
        appBackend = AppBackend.getInstance();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appBackend.clearLeaderboards();
                String username = inputUsername.getText().toString();
                appBackend.setUserID(username);

                Intent switchToMainActivity = new Intent(view.getContext(), HomeActivity.class);
                startActivity(switchToMainActivity);
            }
        });

    }

    @Override
    public void onBackPressed() {
        // do nothing
    }
}