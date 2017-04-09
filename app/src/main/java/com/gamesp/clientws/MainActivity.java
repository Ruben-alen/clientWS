package com.gamesp.clientws;

import android.content.pm.ActivityInfo;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    // client websocket
    private WebSocketClient mWebSocketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        /**
         * delete commnads, images and text view
         */
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Delete", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                TextView textView = (TextView) findViewById(R.id.commands_edit);
                String commandTotal = textView.getText().toString();
               // delete commands
                textView.setText("");
                // clear de linear layout
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.totalCommands);
                linearLayout.removeAllViews();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(getApplicationContext(),
                    "Connecting...", Toast.LENGTH_SHORT).show();
            connectWebSocket();

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Connect with esp8266 default IP
     */
    private void connectWebSocket() {
        URI uri = null;

        try {
            uri = new URI("ws://192.168.4.1:81");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Map<String, String> headers = new HashMap<>();
        mWebSocketClient = new WebSocketClient(uri,new Draft_17(),headers,0) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.w("robota", "Websocket Opened");
                // send message 'STOP/SLEEP' when open socket
                sendMessage("S");
            }

            @Override
            public void onMessage(final String onMsg) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.w("robota", "Message ON:"+onMsg);
                        //JSON onMsg
                        try {
                            JSONObject client=new JSONObject(onMsg);
                            if(client.has("idRobota")) {
                                TextView textView = (TextView) findViewById(R.id.id_content);
                                textView.setText(client.getString("idRobota"));
                            }
                            if(client.has("state")) {
                                TextView textView = (TextView)findViewById(R.id.state_content);
                                textView.setText(client.getString("state"));
                            }
                            if(client.has("mov")) {
                                TextView textView = (TextView)findViewById(R.id.position_content);
                                String executing = client.getString("mov")+":"+client.getInt("X")+":"+client.getInt("Y")+":"+client.getString("compass");
                                textView.setText(executing);
                            }

                        } catch (JSONException e) {
                            Log.e("robota",e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.d("robota", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.d("robota", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

    /**
     * Send a JSON object with commands
     * @param sendMsg commands to Robota
     * @return true if send ok
     */
    private boolean sendMessage(String sendMsg) {
        //JSON msg
        JSONObject client=new JSONObject();
        try {
            client.put("commands",sendMsg);
        } catch (JSONException e) {
            Log.e("robota",e.getMessage());
            return false;
        }
        // only send if object was created
        if (null == mWebSocketClient)
            return false;
        else {
            Log.w("robota", client.toString());
            mWebSocketClient.send(client.toString());
            return true;
        }
    }

    /**
     * Create a String with the command and call sendMessage when click send button
     * @param view command buttons
     */
    public void addCommand(View view) {
        Toast.makeText(getApplicationContext(),
                "Click "+view.getContentDescription(), Toast.LENGTH_SHORT).show();

        // Add commands
        TextView textView = (TextView) findViewById(R.id.commands_edit);
        String commandTotal = textView.getText().toString();
        textView.setText(view.getContentDescription()+commandTotal);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.totalCommands);
        ImageView imageCommand = new ImageView(this);
        LinearLayout.LayoutParams viewParamsCenter = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imageCommand.setLayoutParams(viewParamsCenter);
        switch (view.getContentDescription().charAt(0)) {
            case 'F':
                imageCommand.setImageResource(R.drawable.f);
                break;
            case 'B':
                imageCommand.setImageResource(R.drawable.b);
                break;
            case 'L':
                imageCommand.setImageResource(R.drawable.l);
                break;
            case 'R':
                imageCommand.setImageResource(R.drawable.r);
                break;
        }

        linearLayout.addView(imageCommand);

    }

    /**
     * Send a complete commands
     * @param view send button
     */
    public void sendCommand(View view) {
        TextView textView = (TextView) findViewById(R.id.commands_edit);
        String commandTotal = textView.getText().toString();
        // Send
        sendMessage(commandTotal);
        textView.setText("");
        // clear de linear layout
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.totalCommands);
        linearLayout.removeAllViews();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            TextView textView;
            // each section different fragment_layout
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                // "COMMANDS"
                case 1:
                    rootView = inflater.inflate(R.layout.fragment_commands, container, false);
                    break;
                // "MAP"
                case 2:
                    rootView = inflater.inflate(R.layout.fragment_map, container, false);
                    break;

                // "OTHERS"
                case 3:
                    rootView = inflater.inflate(R.layout.fragment_main, container, false);
                    textView = (TextView) rootView.findViewById(R.id.section_label);
                    textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
                    break;

            }

            return rootView;

        }


    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "COMMANDS";
                case 1:
                    return "MAP";
                case 2:
                    return "OTHERS";
            }
            return null;
        }
    }
}
