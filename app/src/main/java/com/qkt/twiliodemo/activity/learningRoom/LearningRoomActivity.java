package com.qkt.twiliodemo.activity.learningRoom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.qkt.twiliodemo.R;
import com.qkt.twiliodemo.other.Constant;
import com.twilio.video.AudioTrack;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Participant;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LearningRoomActivity extends AppCompatActivity {
    private final String TAG = getClass().getName();
    private final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 0;

    @BindView(R.id.vv_teacher)
    VideoView mVvTeacher;
    @BindView(R.id.vv_my_self)
    VideoView mVvMySelf;
    @BindView(R.id.tv_connect_status)
    TextView mTvVideoStatus;

    private String mAccessToken;
    private String mRoomName;
    private Room mRoom;
    private LocalAudioTrack mLocalAudioTrack;
    private LocalVideoTrack mLocalVideoTrack;
    private boolean mViewOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_room);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (!intent.hasExtra(Constant.ROOM_NAME) || !intent.hasExtra(Constant.ACCESS_TOKEN) ||
                !intent.hasExtra(Constant.VIEW_ONLY)) {
            Toast.makeText(this, R.string.error_no_data, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAccessToken = intent.getStringExtra(Constant.ACCESS_TOKEN);
        mRoomName = intent.getStringExtra(Constant.ROOM_NAME);
        mViewOnly = intent.getBooleanExtra(Constant.VIEW_ONLY,false);
        Log.i(TAG, "Room name is " + mRoomName + " and access token is " + mAccessToken);
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            connectToRoom();
        }
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
                finish();
            } else{
                connectToRoom();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(mRoom != null){
            mRoom.disconnect();
        }
        destroyLocalMedia();
        super.onDestroy();
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCameraAndMicrophone() {
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

    private void createLocalMedia() {
        // Create an audio track
        boolean enable = true;
        mLocalAudioTrack = LocalAudioTrack.create(this, enable);

        // A video track requires an implementation of VideoCapturer
        CameraCapturer cameraCapturer = new CameraCapturer(this,
                CameraCapturer.CameraSource.FRONT_CAMERA);

        // Create a video track
        mLocalVideoTrack = LocalVideoTrack.create(this, enable, cameraCapturer);
    }

    private void destroyLocalMedia(){
        if(mLocalAudioTrack != null){
            mLocalAudioTrack.release();
            mLocalAudioTrack = null;
        }
        if(mLocalVideoTrack != null){
            mLocalVideoTrack.release();
            mLocalVideoTrack = null;
        }
    }

    private void connectToRoom() {
        ConnectOptions options;
        if(mViewOnly){
            options = new ConnectOptions.Builder(mAccessToken)
                    .roomName(mRoomName)
                    .build();
            mVvMySelf.setVisibility(View.GONE);
        } else{
            createLocalMedia();
            List<LocalAudioTrack> list = new ArrayList<LocalAudioTrack>();
            list.add(mLocalAudioTrack);
            List<LocalVideoTrack> videos = new ArrayList<>();
            videos.add(mLocalVideoTrack);
            options= new ConnectOptions.Builder(mAccessToken)
                    .roomName(mRoomName)
                    .audioTracks(list)
                    .videoTracks(videos)
                    .build();
            mLocalVideoTrack.addRenderer(mVvMySelf);
        }

        mRoom = Video.connect(this, options,getRoomListener());
    }

    private Room.Listener getRoomListener(){
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                Log.i(TAG, "Connect room success\nCurrent participant in room: " + room.getParticipants().size());
                for(Participant participant:room.getParticipants()){
                    if(participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)){
                        addTeacherToRoom(participant);
                    } else{
                        addStudentToRoom(participant);
                    }
                }
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                Toast.makeText(LearningRoomActivity.this,
                        R.string.error_connect_failure,
                        Toast.LENGTH_LONG).show();
                Log.i(TAG, "Connect room error - " + e.getMessage());
                e.printStackTrace();
                destroyLocalMedia();
                finish();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                Log.i(TAG,"Disconnect");
                destroyLocalMedia();
            }

            @Override
            public void onParticipantConnected(Room room, Participant participant) {
                Log.i(TAG, "New participant connect");
                if(participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)){
                    addTeacherToRoom(participant);
                } else{
                    addStudentToRoom(participant);
                }
            }

            @Override
            public void onParticipantDisconnected(Room room, Participant participant) {
                if(participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)){
                    removeTeacherFromRoom(participant);
                } else{
                    removeStudentFromRoom(participant);
                }
            }

            @Override
            public void onRecordingStarted(Room room) {
                Log.i(TAG,"RecodingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                Log.i(TAG,"RecodingStopped");
            }
        };
    }

    private void addTeacherToRoom(Participant participant){
        Log.i(TAG,"Add teacher to room");
        if (participant.getVideoTracks().size() > 0) {
            addVideo(participant.getVideoTracks().get(0));
        }
        participant.setListener(new Participant.Listener() {
            @Override
            public void onAudioTrackAdded(Participant participant, AudioTrack audioTrack) {
                Log.i(TAG, "AudioTrackAdded");
            }

            @Override
            public void onAudioTrackRemoved(Participant participant, AudioTrack audioTrack) {
                Log.i(TAG, "AudioTrackRemoved");
            }

            @Override
            public void onVideoTrackAdded(Participant participant, VideoTrack videoTrack) {
                Log.i(TAG, "VideoTrackAdded");
                addVideo(videoTrack);
            }

            @Override
            public void onVideoTrackRemoved(Participant participant, VideoTrack videoTrack) {
                Log.i(TAG, "VideoTrackRemoved");
                removeVideo(videoTrack);
            }

            @Override
            public void onAudioTrackEnabled(Participant participant, AudioTrack audioTrack) {

            }

            @Override
            public void onAudioTrackDisabled(Participant participant, AudioTrack audioTrack) {

            }

            @Override
            public void onVideoTrackEnabled(Participant participant, VideoTrack videoTrack) {

            }

            @Override
            public void onVideoTrackDisabled(Participant participant, VideoTrack videoTrack) {

            }
        });
    }

    private void addStudentToRoom(Participant participant){
        Log.i(TAG,"Add student to room");
        participant.setListener(new Participant.Listener() {
            @Override
            public void onAudioTrackAdded(Participant participant, AudioTrack audioTrack) {
            }

            @Override
            public void onAudioTrackRemoved(Participant participant, AudioTrack audioTrack) {

            }

            @Override
            public void onVideoTrackAdded(Participant participant, VideoTrack videoTrack) {

            }

            @Override
            public void onVideoTrackRemoved(Participant participant, VideoTrack videoTrack) {

            }

            @Override
            public void onAudioTrackEnabled(Participant participant, AudioTrack audioTrack) {

            }

            @Override
            public void onAudioTrackDisabled(Participant participant, AudioTrack audioTrack) {

            }

            @Override
            public void onVideoTrackEnabled(Participant participant, VideoTrack videoTrack) {

            }

            @Override
            public void onVideoTrackDisabled(Participant participant, VideoTrack videoTrack) {

            }
        });
    }

    private void removeTeacherFromRoom(Participant participant){
        if(participant.getVideoTracks().size()>0) {
            removeVideo(participant.getVideoTracks().get(0));
        }
    }

    private void removeStudentFromRoom(Participant participant){

    }

    private void addVideo(VideoTrack videoTrack){
        mVvTeacher.setMirror(false);
        videoTrack.addRenderer(mVvTeacher);
        mTvVideoStatus.setVisibility(View.GONE);
    }

    private void removeVideo(VideoTrack videoTrack){
        videoTrack.removeRenderer(mVvTeacher);
        mTvVideoStatus.setVisibility(View.VISIBLE);
    }
}
