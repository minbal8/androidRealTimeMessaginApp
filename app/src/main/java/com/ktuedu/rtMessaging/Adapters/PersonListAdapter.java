package com.ktuedu.rtMessaging.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ktuedu.rtMessaging.R;
import com.ktuedu.rtMessaging.Models.User;

import java.util.List;

public class PersonListAdapter extends ArrayAdapter {

    public PersonListAdapter(Context context, List<User> resources) {
        super(context, R.layout.item_design, resources);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View v = convertView;

        if(v==null){
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.item_design,null);
        }

        TextView name = v.findViewById(R.id.listItemTitle);
        ImageView image = v.findViewById(R.id.listItemImage);

        User user = (User)getItem(position);

        name.setText(user.Name);

        if(user.Photo.equals("none")) {
            image.setImageResource(R.drawable.index);
        }
        else {
            Glide.with(getContext()).load(user.Photo).into(image);
        }
        return v;
    }


}
