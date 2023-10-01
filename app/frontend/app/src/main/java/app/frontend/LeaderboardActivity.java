package app.frontend.app.src.main.java.app.frontend;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import app.R;
import app.backend.modules.Leaderboard;
import app.frontend.adapters.LeaderboardAdapter;

public class LeaderboardActivity extends AppCompatActivity {

    TextView leaderboardTitleTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leaderboard_activity);

        leaderboardTitleTextView = (TextView) findViewById(R.id.leaderboardTitleTextView);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        Leaderboard leaderboard = (Leaderboard) bundle.get("leaderboard");

        leaderboardTitleTextView.setText(leaderboard.getFileName().replace(".gpx", ""));

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.leaderboardActivityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new LeaderboardAdapter(this, leaderboard));
    }
}