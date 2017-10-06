package com.qkt.twiliodemo.activity.learningRoom;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.qkt.twiliodemo.R;
import com.qkt.twiliodemo.other.Constant;
import com.twilio.video.AudioTrack;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Participant;
import com.twilio.video.Room;
import com.twilio.video.RoomState;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoTrack;

import java.util.Collections;

/**
 * Created by qkt on 05/10/2017.
 */

public class LearningRoomPresenter {
    private final String TAG = getClass().getName();

    private Context mContext;
    private LearningRoomInterface.View mView;
    private String mRoomName;
    private String mAccessToken;
    private LocalAudioTrack mLocalAudioTrack;
    private LocalVideoTrack mLocalVideoTrack;
    private boolean mViewOnly;
    private LocalParticipant mLocalParticipant;
    private Room mRoom;

    public LearningRoomPresenter(Context context, LearningRoomInterface.View view,
                                 String roomName, String accessToken, boolean viewOnly) {
        mContext = context;
        mView = view;
        mAccessToken = accessToken;
        mRoomName = roomName;
        mViewOnly = viewOnly;
    }

    /**
     * Create the media resources such as audio and video
     */
    private void createLocalMedia() {
        // Create an audio track
        boolean enable = true;
        mLocalAudioTrack = LocalAudioTrack.create(mContext, enable);

        // A video track requires an implementation of VideoCapturer
        CameraCapturer cameraCapturer = new CameraCapturer(mContext,
                CameraCapturer.CameraSource.FRONT_CAMERA);

        // Create a video track
        mLocalVideoTrack = LocalVideoTrack.create(mContext, enable, cameraCapturer);
    }

    /**
     * Destroy the resource media.
     */
    private void destroyLocalMedia() {
        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.release();
            mLocalAudioTrack = null;
        }
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.release();
            mLocalVideoTrack = null;
        }
    }

    private Room.Listener getRoomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                mLocalParticipant = room.getLocalParticipant();
                Log.i(TAG, "Connect room success\nCurrent participant in room: " + room.getParticipants().size());
                for (Participant participant : room.getParticipants()) {
                    Log.i(TAG, "Already in room: " + participant.getIdentity());
                    addParticipantToRoom(participant);
                }
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                mView.configureAudio(false);
                mView.showMessage(R.string.error_connect_failure);
                Log.i(TAG, "Connect room error - " + e.getMessage());
                e.printStackTrace();
                destroyLocalMedia();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                Log.i(TAG, "Disconnect");
                mView.configureAudio(false);
                mLocalParticipant = null;
                destroyLocalMedia();
            }

            @Override
            public void onParticipantConnected(Room room, Participant participant) {
                Log.i(TAG, "New participant connect: " + participant.getIdentity());
                addParticipantToRoom(participant);
            }

            @Override
            public void onParticipantDisconnected(Room room, Participant participant) {
                Log.i(TAG, "Participant disconnect: " + participant.getIdentity());
                removeParticipantFromRoom(participant);
            }

            @Override
            public void onRecordingStarted(Room room) {
                Log.i(TAG, "RecodingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                Log.i(TAG, "RecodingStopped");
            }
        };
    }

    /**
     * Call when a participant join the room.
     * Track the event which occurs from this participant.
     * @param participant
     */
    private void addParticipantToRoom(Participant participant) {
        if (participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)) {
            Log.i(TAG, "Teacher " + participant.getIdentity() + " joined room");
            if (participant.getVideoTracks().size() > 0) {
                mView.showTeacherVideo(participant.getVideoTracks().get(0));
            }
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
                if (participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)) {
                    mView.showTeacherVideo(videoTrack);
                }
            }

            @Override
            public void onVideoTrackRemoved(Participant participant, VideoTrack videoTrack) {
                Log.i(TAG, "VideoTrackRemoved");
                if (participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)) {
                    mView.hideTeacherVideo(videoTrack);
                }
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

    /**
     * Call when a participant leaves the room.
     * @param participant
     */
    private void removeParticipantFromRoom(Participant participant) {
        if (participant.getIdentity().contains(Constant.TEACHER_PREFIX_NAME)) {
            mView.hideTeacherVideo(null);
        }
    }

    public void connectToRoom() {
        mView.configureAudio(true);
        ConnectOptions options;
        if (mViewOnly) {
            options = new ConnectOptions.Builder(mAccessToken)
                    .roomName(mRoomName)
                    .build();
            mView.hideMyThumbnail(null);
        } else {
            createLocalMedia();
            options = new ConnectOptions.Builder(mAccessToken)
                    .roomName(mRoomName)
                    .audioTracks(Collections.singletonList(mLocalAudioTrack))
                    .videoTracks(Collections.singletonList(mLocalVideoTrack))
                    .build();
            mView.showMyThumbnail(mLocalVideoTrack);
        }

        mRoom = Video.connect(mContext, options, getRoomListener());
    }

    /**
     * Disconnect and destroy all resources
     */
    public void destroy() {
        if (mRoom != null && mRoom.getState() != RoomState.DISCONNECTED) {
            mRoom.disconnect();
        }
        destroyLocalMedia();
    }

    /**
     * If the local video track was released when the app was put in the background, recreate.
     */
    public void resume() {
        if (mLocalVideoTrack == null || mLocalAudioTrack == null) {
            createLocalMedia();
            mView.showMyThumbnail(mLocalVideoTrack);

            /*
             * If connected to a Room then share the local video track.
             */
            if (mLocalParticipant != null) {
                mLocalParticipant.addVideoTrack(mLocalVideoTrack);
                mLocalParticipant.addAudioTrack(mLocalAudioTrack);
            }
        }
    }

    /**
     * Release the local video track before going in the background. This ensures that the
     * camera can be used by other applications while this app is in the background.
     */
    public void pause() {
        if (mLocalVideoTrack != null) {
            /*
             * If this local video track is being shared in a Room, remove from local
             * participant before releasing the video track. Participants will be notified that
             * the track has been removed.
             */
            if (mLocalParticipant != null) {
                mLocalParticipant.removeVideoTrack(mLocalVideoTrack);
            }
        }

        if (mLocalAudioTrack != null) {
            /*
             * If this local audio track is being shared in a Room, remove from local
             * participant before releasing the audio track. Participants will be notified that
             * the track has been removed.
             */
            if (mLocalParticipant != null) {
                mLocalParticipant.removeAudioTrack(mLocalAudioTrack);
            }
        }

        destroyLocalMedia();
    }

    /**
     * Enable/disable the local audio track. The results of this operation are
     * signaled to other Participants in the same Room. When an audio track is
     * disabled, the audio is muted.
     */
    public void disableAudio(boolean enable) {

        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.enable(enable);
        }
    }

    /**
     * Enable/disable the local video track
     */
    public void disableVideo(boolean enable) {
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.enable(enable);
        }
    }
}
