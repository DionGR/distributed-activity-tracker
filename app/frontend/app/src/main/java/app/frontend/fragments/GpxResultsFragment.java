package app.frontend.app.src.main.java.app.frontend.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import app.R;
import app.backend.modules.GPXResult;
import app.backend.AppBackend;
import app.frontend.adapters.GPXResultAdapter;

public class GpxResultsFragment extends Fragment {

    AppBackend appBackend;

    @SuppressLint("DefaultLocale")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_gpx_results, container, false);
        appBackend = AppBackend.getInstance();

        List<GPXResult> GPXResults = appBackend.getGpxResults();
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.gpxResultsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new GPXResultAdapter(getActivity(), GPXResults));

        return rootView;
    }
}