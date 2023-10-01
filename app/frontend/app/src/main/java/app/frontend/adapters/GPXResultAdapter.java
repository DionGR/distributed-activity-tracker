package app.frontend.app.src.main.java.app.frontend.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.R;
import app.backend.modules.GPXResult;

public class GPXResultAdapter extends RecyclerView.Adapter<GPXResultViewHolder> {

    Context context;
    List<GPXResult> GPXResults;

    public GPXResultAdapter(Context context, List<GPXResult> GPXResults) {
        this.context = context;
        this.GPXResults = GPXResults;
    }

    @NonNull
    @Override
    public GPXResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GPXResultViewHolder(LayoutInflater.from(context).inflate(R.layout.gpxresult_view, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull GPXResultViewHolder holder, int position) {
        holder.itemDate.setText(GPXResults.get(position).getDate());
        holder.itemDistance.setText(GPXResults.get(position).getDistance());
        holder.itemTime.setText(GPXResults.get(position).getTime());
        holder.itemSpeed.setText(GPXResults.get(position).getSpeed());
        holder.itemElevation.setText(GPXResults.get(position).getElevation());
        holder.itemImage.setImageResource(GPXResults.get(position).getImage());
    }

    @Override
    public int getItemCount() {
        return GPXResults.size();
    }

}

class GPXResultViewHolder extends RecyclerView.ViewHolder {

    ImageView itemImage;
    TextView itemDate, itemDistance, itemSpeed, itemTime, itemElevation;

    public GPXResultViewHolder(View itemView) {
        super(itemView);
        itemImage = itemView.findViewById(R.id.itemImage);
        itemDate = itemView.findViewById(R.id.itemDate);
        itemDistance = itemView.findViewById(R.id.itemDistance);
        itemSpeed = itemView.findViewById(R.id.itemSpeed);
        itemTime = itemView.findViewById(R.id.itemTime);
        itemElevation = itemView.findViewById(R.id.itemElevation);

    }
}

