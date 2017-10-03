package com.qkt.twiliodemo.activity.learningRoom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.VideoView;

import com.qkt.twiliodemo.R;
import com.qkt.twiliodemo.other.Constant;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Participant;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class LearningRoomActivity extends AppCompatActivity {
    private final String TAG = getClass().getName();
    private final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 0;

    @BindView(R.id.vv_teacher)
    VideoView mVvTeacher;
    @BindView(R.id.vv_my_self)
    VideoView mVvMySelf;

    private String mAccessToken;
    private String mRoomName;
    private Room mRoom;
    LocalAudioTrack mLocalAudioTrack;
    LocalVideoTrack mLocalVideoTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_room);
        Intent intent = getIntent();

        if(!intent.hasExtra(Constant.ROOM_NAME) || !intent.hasExtra(Constant.ACCESS_TOKEN)){
            Toast.makeText(this,R.string.error_no_data,Toast.LENGTH_LONG).show();;
            finish();
            return;
        }

        mAccessToken = intent.getStringExtra(Constant.ACCESS_TOKEN);
        mRoomName = intent.getStringExtra(Constant.ROOM_NAME);
        Log.i(TAG,"Room name is " + mRoomName + " and access token is " + mAccessToken);
        if(!checkPermissionForCameraAndMicrophone()){
            requestPermissionForCameraAndMicrophone();
        }

        createLocalMedia();
        connectToRoom();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            boolean cameraAndMicPermissionGranted = true;

            for (int grantResult : grantResults) {
                cameraAndMicPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }

            if (!cameraAndMicPermissionGranted) {
                Toast.makeText(this,
                        R.string.error_permission_need,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkPermissionForCameraAndMicrophone(){
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    R.string.error_permission_need,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void createLocalMedia(){
        // Create an audio track
        boolean enable = true;
        mLocalAudioTrack = LocalAudioTrack.create(this, enable);

        // A video track requires an implementation of VideoCapturer
        CameraCapturer cameraCapturer = new CameraCapturer(this,
                CameraCapturer.CameraSource.FRONT_CAMERA);

        // Create a video track
        mLocalVideoTrack = LocalVideoTrack.create(this, enable, cameraCapturer);
    }

    private void connectToRoom(){
        List<LocalAudioTrack> list = new ArrayList<LocalAudioTrack>();
        list.add(mLocalAudioTrack);
        List<LocalVideoTrack> videos = new ArrayList<>();
        videos.add(mLocalVideoTrack);
        ConnectOptions options = new ConnectOptions.Builder(mAccessToken)
                .roomName(mRoomName)
                .audioTracks(list)
                .videoTracks(videos)
                .build();
        mRoom = Video.connect(this, options, new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                Log.i(TAG,"Connect room success");
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                Log.i(TAG,"Connect room error");
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {

            }

            @Override
            public void onParticipantConnected(Room room, Participant participant) {
                Log.i(TAG,"New participant connect");
            }

            @Override
            public void onParticipantDisconnected(Room room, Participant participant) {

            }

            @Override
            public void onRecordingStarted(Room room) {

            }

            @Override
            public void onRecordingStopped(Room room) {

            }
        });
        Log.i(TAG,"Current participant in room: " + mRoom.getParticipants().size());
    }
}
