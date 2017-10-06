package com.qkt.twiliodemo.activity.learningRoom;

import com.twilio.video.VideoTrack;

/**
 * Created by qkt on 05/10/2017.
 */

public interface LearningRoomInterface {
    interface Presenter{

    }

    interface View{
        void hideMyThumbnail(VideoTrack videoTrack);
        void showMyThumbnail(VideoTrack videoTrack);
        void showTeacherVideo(VideoTrack videoTrack);
        void hideTeacherVideo(VideoTrack videoTrack);
        void showMessage(int messageId);
        void configureAudio(boolean enable);
    }
}
