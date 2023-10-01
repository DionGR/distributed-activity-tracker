package app.frontend.app.src.main.java.app.frontend.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.R;
import app.backend.modules.Leaderboard;
import app.frontend.LeaderboardActivity;

public class SegmentAdapter extends RecyclerView.Adapter<SegmentViewHolder>{

    Context context;
    List<Leaderboard> segmentLeaderboards;

    public SegmentAdapter(Context context, List<Leaderboard> segmentLeaderboards) {
        this.context = context;
        this.segmentLeaderboards = segmentLeaderboards;
    }

    @NonNull
    @Override
    public SegmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SegmentViewHolder(LayoutInflater.from(context).inflate(R.layout.segment_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SegmentViewHolder holder, int position) {
        int currentPos = position;
        holder.segmentLeaderboardBtn.setText(segmentLeaderboards.get(currentPos).getFileName().replace(".gpx", ""));
        holder.segmentLeaderboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, LeaderboardActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("leaderboard", segmentLeaderboards.get(currentPos));
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return segmentLeaderboards.size();
    }
}


class SegmentViewHolder extends RecyclerView.ViewHolder {

    Button segmentLeaderboardBtn;
    public SegmentViewHolder(View itemView) {
        super(itemView);
        segmentLeaderboardBtn = itemView.findViewById(R.id.segmentLeaderboardBtn);
    }
}