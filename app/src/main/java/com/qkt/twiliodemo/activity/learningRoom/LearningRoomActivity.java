package com.qkt.twiliodemo.activity.learningRoom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.qkt.twiliodemo.R;
import com.qkt.twiliodemo.other.Constant;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LearningRoomActivity extends AppCompatActivity
        implements LearningRoomInterface.View {
    private final String TAG = getClass().getName();
    private final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 0;

    @BindView(R.id.vv_teacher)
    VideoView mVvTeacher;
    @BindView(R.id.vv_my_self)
    VideoView mVvMySelf;
    @BindView(R.id.tv_video_status)
    TextView mTvVideoStatus;

    private LearningRoomPresenter mPresenter;
    private AudioManager mAudioManager;
    private int mPreviousAudioMode;
    private boolean mPreviousMicrophoneMute;

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


        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        String accessToken = intent.getStringExtra(Constant.ACCESS_TOKEN);
        String roomName = intent.getStringExtra(Constant.ROOM_NAME);
        boolean viewOnly = intent.getBooleanExtra(Constant.VIEW_ONLY, false);
        mPresenter = new LearningRoomPresenter(this, this, roomName, accessToken, viewOnly);
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            mPresenter.connectToRoom();
        }
    }

    @Override
    protected void onPause() {
        mPresenter.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.resume();
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
            } else {
                mPresenter.connectToRoom();
            }
        }
    }

    @Override
    protected void onDestroy() {
        mPresenter.destroy();
        super.onDestroy();
    }

    /*
     * Implements LearningRoomInterface.View
     *
     */

    @Override
    public void hideMyThumbnail(VideoTrack videoTrack) {
        if (videoTrack != null) {
            videoTrack.removeRenderer(mVvMySelf);
        }
        mVvMySelf.setVisibility(View.GONE);
    }

    @Override
    public void showMyThumbnail(VideoTrack videoTrack) {
        if (videoTrack != null) {
            videoTrack.addRenderer(mVvMySelf);
            mVvMySelf.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showTeacherVideo(VideoTrack videoTrack) {
        if (videoTrack != null) {
            mVvTeacher.setMirror(false);
            videoTrack.addRenderer(mVvTeacher);
            mTvVideoStatus.setVisibility(View.GONE);
            mVvMySelf.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideTeacherVideo(VideoTrack videoTrack) {
        if (videoTrack != null) {
            videoTrack.removeRenderer(mVvTeacher);
        }
        mTvVideoStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public void showMessage(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
    }

    @Override
    public void configureAudio(boolean enable) {
        if (enable) {
            mPreviousAudioMode = mAudioManager.getMode();
            // Request audio focus before making any device switch
            requestAudioFocus();
            /*
             * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
             * to be in this mode when playout and/or recording starts for the best
             * possible VoIP performance. Some devices have difficulties with
             * speaker mode if this is not set.
             */
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            /*
             * Always disable microphone mute during a WebRTC call.
             */
            mPreviousMicrophoneMute = mAudioManager.isMicrophoneMute();
            mAudioManager.setMicrophoneMute(false);
        } else {
            mAudioManager.setMode(mPreviousAudioMode);
            mAudioManager.abandonAudioFocus(null);
            mAudioManager.setMicrophoneMute(mPreviousMicrophoneMute);
        }
    }

    /*
     * Other method
     */
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

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            AudioFocusRequest focusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(
                                    new AudioManager.OnAudioFocusChangeListener() {
                                        @Override
                                        public void onAudioFocusChange(int i) { }
                                    })
                            .build();
            mAudioManager.requestAudioFocus(focusRequest);
        } else {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }
}
