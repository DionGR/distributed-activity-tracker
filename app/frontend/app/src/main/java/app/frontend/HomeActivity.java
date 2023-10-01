package app.frontend.app.src.main.java.app.frontend;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;
import android.widget.PopupMenu;

import app.R;
import app.backend.AppBackend;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;

import java.util.Random;

import android.graphics.BitmapFactory;
import android.widget.TextView;
import android.widget.Toast;

import app.frontend.LoginActivity;
import app.frontend.StatisticsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class HomeActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private static final int PICK_GPX_FILE_REQUEST_CODE = 100;
    private static final int PICK_SEGMENT_FILE_REQUEST_CODE = 200;
    AppBackend appBackend;

    TextView welcomeUser;

    FloatingActionButton uploadFloatBtn;

    BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        appBackend = AppBackend.getInstance();
        welcomeUser = (TextView) findViewById(R.id.welcomeUser);
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigationView);
        uploadFloatBtn = (FloatingActionButton) findViewById(R.id.fab);

        welcomeUser.setText("Welcome, " + appBackend.getUserID() + "!");

        uploadFloatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.homeBtn);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (item.getItemId() == R.id.statisticsBtn) {
                Intent switchToStatisticsActivity = new Intent(this, StatisticsActivity.class);
                startActivity(switchToStatisticsActivity);

            } else if (itemId == R.id.exitBtn) {
                Intent switchToLoginActivity = new Intent(this, LoginActivity.class);
                startActivity(switchToLoginActivity);
            }
            item.setChecked(true);
            return true;
        });

    }

    private void ChooseFileFromDevice(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_GPX_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            String filePath = selectedFileUri.getPath();

            // Do something with the file path
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, Boolean> taskForGPXUpload = new AsyncTask<String, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(String... strings) {
                    String gpxPath = strings[0];
                    return appBackend.uploadGPX(gpxPath);
                }

                @SuppressLint("DefaultLocale")
                @Override
                protected void onPostExecute(Boolean success) {
                    if (success)
                        createNotification();
                    else
                        Toast.makeText(HomeActivity.this, "Unreachable server!", Toast.LENGTH_LONG).show();//
                }
            };

            taskForGPXUpload.execute(filePath);
        }

        else if(requestCode == PICK_SEGMENT_FILE_REQUEST_CODE && resultCode == RESULT_OK && data !=null){
            Uri selectedFileUri = data.getData();
            String filePath = selectedFileUri.getPath();

            // Do something with the file path
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, Boolean> taskForSegmentUpload = new AsyncTask<String, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(String... strings) {
                    String gpxPath = strings[0];
                    return appBackend.uploadSegment(gpxPath);
                }

                @SuppressLint("DefaultLocale")
                @Override
                protected void onPostExecute(Boolean success) {
                    if (success)
                        Toast.makeText(HomeActivity.this, "Segment uploaded!", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(HomeActivity.this, "Unreachable server!", Toast.LENGTH_LONG).show();
                }
            };

            taskForSegmentUpload.execute(filePath);
        }
    }

    public void showPopup(View v){
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.upload_popup_menu);
        popup.show();
    }
    @Override
    public boolean onMenuItemClick(MenuItem item){

        int itemId = item.getItemId();
        if (item.getItemId() == R.id.uploadGPXBtn) {
            ChooseFileFromDevice(PICK_GPX_FILE_REQUEST_CODE);

        } else if (itemId == R.id.uploadSegmentBtn) {
            ChooseFileFromDevice(PICK_SEGMENT_FILE_REQUEST_CODE);
        }
        return true;
    }

    private void createNotification() {
        String id = "my_channel_id_01";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = manager.getNotificationChannel(id);
            if (channel == null) {
                channel = new NotificationChannel(id, "Channel Title", NotificationManager.IMPORTANCE_HIGH);
                //config nofication channel
                channel.setDescription("[Channel description]");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{100, 1000, 200, 340});
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(channel);
            }
        }
        Intent notificationIntent = new Intent(this, StatisticsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, id)
                .setSmallIcon(R.drawable.img_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.gpx_img))
//                .setStyle(new NotificationCompat.BigPictureStyle()
//                        .bigPicture(BitmapFactory.decodeResource(getResources(), R.drawable.img2))
//                        .bigLargeIcon(null))
                .setContentTitle("Your GPX results are ready")
                .setContentText("Tap to view your statistics.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{100, 1000, 200, 340})
                .setAutoCancel(true)
                .setTicker("Notification");
        builder.setContentIntent(contentIntent);
        NotificationManagerCompat m = NotificationManagerCompat.from(getApplicationContext());
        //id to generate new notification in list notifications menu
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        m.notify(new Random().nextInt(), builder.build());

    }

}

