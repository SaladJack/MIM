package com.saladjack.im.ui.message;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import scut.saladjack.core.bean.FriendBean;

import com.saladjack.im.R;
import com.saladjack.im.app.Constant;
import com.saladjack.im.ui.base.BaseFragment;
import com.saladjack.im.ui.chat.ChatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by saladjack on 17/1/27.
 */

public class MessageFragment extends BaseFragment implements MessageView {

    private RecyclerView friendRv;
    private MessageAdapter adapter;
    private List<FriendBean> friendList = new ArrayList<>();
    private BroadcastReceiver mMessagReceiver;
    private MessageIPrensenter presenter;

    @Nullable @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.friend_fragment,container,false);
        presenter = new MessagePresenter(this);
        friendRv = (RecyclerView) view.findViewById(R.id.friend_rv);
        friendRv.setLayoutManager(new LinearLayoutManager(getContext()));

        int id = Constant.USER_ID;

        friendList.add(createFriend(id,"Self",""));
        adapter = new MessageAdapter(friendList);
        presenter.queryFriendWithLatestContent();
        adapter.setOnItemClickListener(userBean -> {
            ChatActivity.open(getContext(),userBean);
        });
        friendRv.setAdapter(adapter);
        mMessagReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                abortBroadcast();
                Bundle bundle = intent.getBundleExtra("bundle");
                int friendId = bundle.getInt("friendId");
                String content = bundle.getString("content");
                int contentType = bundle.getInt("contentType");
                presenter.insertMessageToDbAndQueryFriend(friendId,content);
            }
        };

        return view;
    }

    @Override public void onResume() {
        super.onResume();
        IntentFilter filterMessage = new IntentFilter("chat");
        filterMessage.setPriority(60);
        getActivity().registerReceiver(mMessagReceiver,filterMessage);
    }

    @Override public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mMessagReceiver);
    }

    private FriendBean createFriend(int id, String name, String latestContent){
        FriendBean friend = new FriendBean();
        friend.setId(id);
        friend.setName(name);
        friend.setLatestContent(latestContent);
        return friend;
    }

    public void refresh(int friendId,String latestContent){
        presenter.insertMessageToDbAndQueryFriend(friendId,latestContent);
    }

    @Override public void onQueryFriendSuccess(FriendBean friendBean) {
        adapter.addItem(friendBean);
    }

    @Override public void onQueryFriendFail() {
        showToast(R.string.query_friend_fail);
    }

    @Override public void onQueryFriendWithLatestContentFinish(List<FriendBean> friendBeen) {
        adapter.reset(friendBeen);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
