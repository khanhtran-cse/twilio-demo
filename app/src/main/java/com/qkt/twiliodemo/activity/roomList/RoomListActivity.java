package com.qkt.twiliodemo.activity.roomList;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.qkt.twiliodemo.R;
import com.qkt.twiliodemo.activity.learningRoom.LearningRoomActivity;
import com.qkt.twiliodemo.other.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RoomListActivity extends AppCompatActivity {
    private final int REQUEST_LEARNING_ROOM_CODE = 0;

    @BindView(R.id.rb_user1)
    RadioButton mRbUser1;
    @BindView(R.id.rb_user2)
    RadioButton mRbUser2;
    @BindView(R.id.rb_user3)
    RadioButton mRbUser3;
    @BindView(R.id.rb_user4)
    RadioButton mRbUser4;
    @BindView(R.id.rb_view_only)
    RadioButton mRbViewOnly;
    @BindView(R.id.et_room_name)
    EditText mEtRoomName;

    private String[] mAccessTokens;
    private String mAccessToken;
    private String mRoomName;
    private boolean mViewOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);
        ButterKnife.bind(this);
        mAccessTokens = getResources().getStringArray(R.array.access_token);
        init();
    }

    @OnClick(R.id.btn_connect_to_room)
    public void connectToRoom(View view) {
        if (!mEtRoomName.getText().toString().trim().equals("")) {
            mRoomName = mEtRoomName.getText().toString();
            connectToRoom();
        } else {
            Toast.makeText(this, R.string.error_invalid_room_name, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick({R.id.rb_user1, R.id.rb_user2, R.id.rb_user3, R.id.rb_user4,R.id.rb_view_only})
    public void selectUser(View view) {
        switch (view.getId()){
            case R.id.rb_user1:
                mAccessToken = mAccessTokens[0];
                mViewOnly = false;
                break;
            case R.id.rb_user2:
                mViewOnly = false;
                mAccessToken = mAccessTokens[1];
                break;
            case R.id.rb_user3:
                mViewOnly = false;
                mAccessToken = mAccessTokens[2];
                break;
            case R.id.rb_view_only:
                mViewOnly = true;
                mAccessToken = mAccessTokens[4];
                break;
            default:
                mViewOnly = false;
                mAccessToken = mAccessTokens[3];
                break;
        }
    }

    private void init() {
        String[] names = getResources().getStringArray(R.array.user_name);
        mRbUser1.setText(names[0]);
        mRbUser2.setText(names[1]);
        mRbUser3.setText(names[2]);
        mRbUser4.setText(names[3]);
        mRbViewOnly.setText(names[4]);
    }

    private void connectToRoom() {
        Intent intent = new Intent(this, LearningRoomActivity.class);
        intent.putExtra(Constant.ROOM_NAME, mRoomName);
        intent.putExtra(Constant.ACCESS_TOKEN, mAccessToken);
        intent.putExtra(Constant.VIEW_ONLY,mViewOnly);
        startActivityForResult(intent, REQUEST_LEARNING_ROOM_CODE);
    }
}
