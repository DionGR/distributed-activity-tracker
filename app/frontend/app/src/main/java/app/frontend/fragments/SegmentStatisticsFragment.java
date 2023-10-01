package app.frontend.app.src.main.java.app.frontend.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

import app.R;
import app.backend.modules.Leaderboard;
import app.backend.AppBackend;
import app.frontend.adapters.SegmentAdapter;

public class SegmentStatisticsFragment extends Fragment {

    AppBackend appBackend;
    Button requestSegmentStatisticsBtn;

    List<Leaderboard> leaderboards;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_segment_statistics, container, false);
        appBackend = AppBackend.getInstance();
        requestSegmentStatisticsBtn = (Button) rootView.findViewById(R.id.requestSegmentStatisticsBtn);
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.segmentStatisticsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        requestSegmentStatisticsBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                @SuppressLint("StaticFieldLeak")
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        appBackend.requestSegmentStatistics();
                        return null;
                    }

                    protected void onPostExecute(Void v) {
                        leaderboards = appBackend.getLeaderboards();
                        //Log.e("leaderboardsSize", String.valueOf(leaderboards.size()));
                        recyclerView.setAdapter(new SegmentAdapter(getActivity(), leaderboards));
                    }
                };
                task.execute();
            }
        });

        requestSegmentStatisticsBtn.callOnClick();

        return rootView;
    }

}