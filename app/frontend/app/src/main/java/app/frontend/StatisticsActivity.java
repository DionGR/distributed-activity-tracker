package app.frontend.app.src.main.java.app.frontend;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import app.R;
import app.frontend.adapters.ViewPagerAdapter;


public class StatisticsActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;

    TabLayout tablayout;
    ViewPager2 viewPager2;
    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics_activity);

        tablayout = findViewById(R.id.statsTabLayout);
        viewPager2 = findViewById(R.id.view_pager);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(viewPagerAdapter);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tablayout.getTabAt(position).select();
            }
        });


        bottomNavigationView.setSelectedItemId(R.id.statisticsBtn);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (item.getItemId() == R.id.homeBtn) {
                Intent switchToHomeActivity = new Intent(this, HomeActivity.class);
                startActivity(switchToHomeActivity);

            } else if (itemId == R.id.exitBtn) {
                Intent switchToLoginActivity = new Intent(this, LoginActivity.class);
                startActivity(switchToLoginActivity);
            }
            return true;
        });


    }

}