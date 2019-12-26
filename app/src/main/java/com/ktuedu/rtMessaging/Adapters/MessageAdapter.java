package com.ktuedu.rtMessaging.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ktuedu.rtMessaging.Models.localMessage;
import com.ktuedu.rtMessaging.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final  int MSG_TYPE_LEFT = 0;
    public static final  int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<localMessage> localMessageList;

    public MessageAdapter(Context c, List<localMessage> list){
        context = c;
        localMessageList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.message_item_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.message_item_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position){
        localMessage localMessage = localMessageList.get(position);
        holder.message.setText(localMessage.getText());
        Log.i("OnBindViewHolder","Text: "+ localMessage.getText());
    }

    @Override
    public int getItemCount(){
        return localMessageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView message;
        public ViewHolder(View view){
            super(view);
            message = view.findViewById(R.id.chat_message);
        }
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user.getUid().equals(localMessageList.get(position).getFromId())){
            return  MSG_TYPE_RIGHT;
        }
        else{
            return MSG_TYPE_LEFT;
        }

    }
}
