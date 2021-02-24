package com.codepath.apps.restclienttemplate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class TimelineActivity extends AppCompatActivity {

    public static final String TAG = "TimelineActivity";
    TwitterClient twitterClient;
    RecyclerView rvTweets;
    List<Tweet> tweets;
    TweetsAdapter tweetsAdapter;
    SwipeRefreshLayout swipeContainer;
    EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        twitterClient = TwitterApplication.getRestClient(this);

        swipeContainer = findViewById(R.id.swipeContainer);
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        // Set our refresh listener
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "Fetching new data!");
                populateHomeTimeline();
            }
        });

        // Find the recycler view
        rvTweets = findViewById(R.id.rvTweets);

        // Initialize the list of tweets and adapter
        tweets = new ArrayList<>();
        tweetsAdapter = new TweetsAdapter(this, tweets);

        // Recycler View Setup: Layout Manager and the adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(layoutManager);
        rvTweets.setAdapter(tweetsAdapter);

        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.i(TAG, "onLoadMore" + page);
                loadMoreData();
            }
        };

        // Adds the scroll listener to RecyclerView
        rvTweets.addOnScrollListener(scrollListener);

        populateHomeTimeline();
    }

    // This is where we will make another API call to get the next page of tweets and add the objects to our current list of tweets
    private void loadMoreData() {
        // 1. Send an API request to retrieve appropriate paginated data
        twitterClient.getNextPageOfTweets(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess for loadMoreData" + json.toString());
                // 2. Deserialize and construct new model objects from the API response
                JSONArray jsonArray = json.jsonArray;
                try {
                    // 3. Append the new data objects to the existing set of items inside the array of items
                    final List<Tweet> tweetsList = Tweet.fromJSONArray(jsonArray);
                    tweetsAdapter.addAll(tweetsList);

                    // 4. Notify the adapter of the new items made with `notifyItemRangeInserted()
                    // Skipped
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure for loadMoreData" + response, throwable);

            }
        }, tweets.get(tweets.size() - 1).id);

    }

    private void populateHomeTimeline() {
        twitterClient.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess" + json.toString());
                JSONArray jsonArray = json.jsonArray;
                try {
                    tweetsAdapter.clear();
                    tweetsAdapter.addAll(Tweet.fromJSONArray(jsonArray));
                    //tweets.addAll(Tweet.fromJSONArray(jsonArray));
                    tweetsAdapter.notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                } catch (JSONException e) {
                    Log.e(TAG, "Json Exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure" + response, throwable);
            }
        });
    }
}