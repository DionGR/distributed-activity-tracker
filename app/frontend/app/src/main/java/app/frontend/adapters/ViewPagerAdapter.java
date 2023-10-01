package app.frontend.app.src.main.java.app.frontend.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import app.frontend.fragments.GpxResultsFragment;
import app.frontend.fragments.GpxStatisticsFragment;
import app.frontend.fragments.SegmentStatisticsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(FragmentActivity fragmentActivity){
        super(fragmentActivity);
    }

    @Override
    public Fragment createFragment(int position){
        switch (position){
            case 0:
                return new GpxResultsFragment();
            case 1:
                return new GpxStatisticsFragment();
            case 2:
                return new SegmentStatisticsFragment();
            default:
                return new GpxResultsFragment();
        }
    }


    @Override
    public int getItemCount() {
        return 3;
    }
}
