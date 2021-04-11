package medicalcare.com.medicalcare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

    }

    public void MainMenuClick(View view) {
        Intent i  = new Intent(this, MainMenuActivity.class);
        startActivity(i);
    }
}
