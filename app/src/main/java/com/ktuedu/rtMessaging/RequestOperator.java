package com.ktuedu.rtMessaging;

import android.util.Log;

import com.ktuedu.rtMessaging.Models.ModelPost;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RequestOperator extends Thread {

    public interface RequestOperatorListener {
        void success(List<ModelPost> publication);

        void failed(int responseCode);
    }

    private RequestOperatorListener listener;
    private int responseCode;

    public void setListener(RequestOperatorListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        super.run();
        try {
            List<ModelPost> publication = request();
            if (publication != null) {
                success(publication);
            } else {
                failed(responseCode);
            }
        } catch (IOException e) {
            failed(-1);
        } catch (JSONException e) {
            failed(-2);
        }

    }

    private List<ModelPost> request() throws IOException, JSONException {
        URL obj = new URL("http://jsonplaceholder.typicode.com/posts/");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-type", "application/json");

        responseCode = con.getResponseCode();

        InputStreamReader streamReader;

        if (responseCode == 200) {
            streamReader = new InputStreamReader(con.getInputStream());
        } else {
            streamReader = new InputStreamReader(con.getErrorStream());
        }

        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        if (responseCode == 200) {
            return parsingJsonObject(response.toString());
        } else {
            return null;
        }
    }

    public List<ModelPost> parsingJsonObject(String response) throws JSONException {

        JSONArray jsonArray= new JSONArray(response);
        List<ModelPost> list = new ArrayList<ModelPost>();

        for(int i=0; i<jsonArray.length(); i++) {
            ModelPost post = new ModelPost();
            post.setId(jsonArray.getJSONObject(i).getInt("id"));
            post.setUserId(jsonArray.getJSONObject(i).getInt("userId"));
            post.setTitle(jsonArray.getJSONObject(i).getString("title"));
            post.setBodyText(jsonArray.getJSONObject(i).getString("body"));

            //Log.e("JSON", "Data: "+post.getId());
            list.add(post);
        }

        return list;
    }

    private void failed(int code) {
        if (listener != null)
            listener.failed(code);
    }

    private void success(List<ModelPost> post) {
        if (listener != null)
            listener.success(post);
    }

}