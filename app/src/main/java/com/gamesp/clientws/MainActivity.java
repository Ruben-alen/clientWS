package com.gamesp.clientws;

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

import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
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
    private WebSocketClient mWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Sending commands...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // Send commands
                sendMessage();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Connect with esp8266 default IP
     */
    private void connectWebSocket() {
        URI uri;
        JSONObject outMsg;

        try {
            uri = new URI("ws://192.168.4.1:81");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        Map<String, String> headers = new HashMap<>();
        mWebSocketClient = new WebSocketClient(uri,new Draft_17(),headers,0) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.d("robota", "Websocket Opened");
                // send message when open socket
                mWebSocketClient.send("ClientAndroid");
            }


            @Override
            public void onMessage(final String onMsg) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("robota", "Message ON");
                        // put msg at textview
                        TextView textView = (TextView)findViewById(R.id.position_label);
                        textView.setText(onMsg);
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

    private void sendMessage() {
        TextView textView = (TextView)findViewById(R.id.commands_edit);
        String sendMsg =textView.getText().toString();

        //JSON msg
        JSONObject client=new JSONObject();
        try {
            client.put("msg",sendMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mWebSocketClient.send(client.toString());
        textView.setText(client.toString());
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
                    textView = (TextView) rootView.findViewById(R.id.commands_label);
                    textView.setText(R.string.commands);
                    break;
                // "MAP"
                case 2:
                    rootView = inflater.inflate(R.layout.fragment_map, container, false);
                    textView = (TextView) rootView.findViewById(R.id.position_label);
                    textView.setText(R.string.position);
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
