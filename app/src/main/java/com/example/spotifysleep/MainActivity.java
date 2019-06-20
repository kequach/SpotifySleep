package com.example.spotifysleep;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class MainActivity extends AppCompatActivity {
    private static final String CLIENT_ID = "f3d32cc698f74ec6a22d05d86411f31f";
    private static final String REDIRECT_URI = "spotifysleep://callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> playerState;
    private Button button;
    private EditText mEdit;
    // Request code will be used to verify if result comes from the login activity. Can be set to any integer.
    private static final int REQUEST_CODE = 1337;
    private Track curTrack;
    private Handler mHandler;
    private int numSongs;
    private int counter;

    private Track firstTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        mEdit = findViewById(R.id.editText);
        mHandler = new Handler();
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (curTrack.name != firstTrack.name) {
                counter++;
            }
            System.out.println("Current song is number " + counter + " from a total of " + numSongs + " songs");
            if (counter  == numSongs) {
                //sleep?
                System.out.println("Last song");
                try {
                    Thread.sleep(curTrack.duration);
                } catch (InterruptedException e) {

                } finally {
                    mSpotifyAppRemote.getPlayerApi().pause();
                    playerState.cancel();
                }
            } else if (counter < numSongs) {
                System.out.println("FMADELUXE2");
                mHandler.postDelayed(mRunnable, curTrack.duration);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-modify-playback-state", "app-remote-control"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        System.out.println("YEE ACTIVITY RESULT");

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    System.out.println("YEE TOKEN 2");
                    ConnectionParams connectionParams =
                            new ConnectionParams.Builder(CLIENT_ID)
                                    .setRedirectUri(REDIRECT_URI)
                                    .build();
                    SpotifyAppRemote.connect(MainActivity.this, connectionParams,
                            new Connector.ConnectionListener() {

                                @Override
                                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                    mSpotifyAppRemote = spotifyAppRemote;
                                    Log.d("MainActivity", "Connected! Yay!");
                                    getCurrentTrack();

                                    // Now you can start interacting with App Remote
                                    button.setOnClickListener(new View.OnClickListener() {

                                        @Override
                                        public void onClick(final View v) {
                                            firstTrack = curTrack;
                                            System.out.println("CLICK");
                                            counter = 1;
                                            numSongs = Integer.parseInt(mEdit.getText().toString());
                                            PlayerApi playerApi = mSpotifyAppRemote.getPlayerApi();
                                            playerApi.seekTo(0);
                                            playerApi.resume();
                                            startRepeatingTask();
                                        }


                                    });


                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    Log.e("MainActivity", throwable.getMessage(), throwable);

                                    // Something went wrong when attempting to connect! Handle errors here
                                }
                            });

                    break;

                // Auth flow returned an error
                case ERROR:
                    System.err.println("YEE ERROR");
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }


    }

    private void startRepeatingTask() {
        mRunnable.run();
    }

    private void getCurrentTrack() {
        playerState = mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState();

        playerState.setEventCallback(playerState -> {
            curTrack = playerState.track;
            System.out.println(curTrack.name + " FMA2");
            if (curTrack != null) {
                Log.d("MainActivity", curTrack.name + " by " + curTrack.artist.name);
            }
        });
    }
}