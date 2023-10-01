package app.frontend.app.src.main.java.app.frontend.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import app.R;
import app.backend.modules.Leaderboard;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardViewHolder> {

    Context context;
    Leaderboard leaderboard;

    public LeaderboardAdapter(Context context, Leaderboard leaderboard) {
        this.context = context;
        this.leaderboard = leaderboard;
    }


    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LeaderboardViewHolder(LayoutInflater.from(context).inflate(R.layout.leaderboard_view, parent, false));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        holder.userPosition.setText(String.format("%d", position+1));
        holder.userName.setText(String.format("%s", leaderboard.getUserNames().get(position)));
        holder.userTime.setText(String.format("%.2f min", (double)leaderboard.getUserTimes().get(position).getTotalTime()/1000/60));
    }

    @Override
    public int getItemCount() {
        return leaderboard.size();
    }
}


class LeaderboardViewHolder extends RecyclerView.ViewHolder {

    TextView userName, userTime, userPosition;

    public LeaderboardViewHolder(View itemView) {
        super(itemView);
        userName = itemView.findViewById(R.id.userName);
        userTime = itemView.findViewById(R.id.userTime);
        userPosition = itemView.findViewById(R.id.userPosition);
    }
}
