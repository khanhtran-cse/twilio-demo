package com.qkt.twiliodemo.activity.learningRoom;

import com.twilio.video.Participant;
import com.twilio.video.Room;

/**
 * Created by qkt on 05/10/2017.
 */

public class LearningRoomModel {
    private LearningRoomInterface.Presenter mPresenter;

    public LearningRoomModel(LearningRoomInterface.Presenter presenter){
        mPresenter = presenter;
    }

    /**
     * Check a user has permission to join room or not.
     * A user has permission to join room if the user has subscribe in Section Activity
     * @param roomName
     */
    public void checkPermission(String roomName){

    }

    /**
     * Connects to a room and has full permission
     * @param roomName
     */
    public void joinRoom(String roomName, Room.Listener listener){

    }

    /**
     * Connect to a room in view only mode
     * @param roomName
     */
    public void joinRoomAsGuest(String roomName, Room.Listener listener){

    }
}
