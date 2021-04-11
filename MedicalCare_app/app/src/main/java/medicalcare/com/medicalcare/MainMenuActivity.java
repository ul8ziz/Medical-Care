package medicalcare.com.medicalcare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainMenuActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main_menu);


        }

    public void terminalClick(View view) {
            Intent i = new Intent(this,TerminalActivity.class);
            startActivity(i);
    }

    public void heartClick(View view) {
        Intent i = new Intent(this,GraphActivity .class);
        startActivity(i);
    }
}
