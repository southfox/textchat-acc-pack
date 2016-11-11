package com.tokbox.android.textchatsample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tokbox.android.accpack.OneToOneCommunication;
import com.tokbox.android.accpack.textchat.ChatMessage;
import com.tokbox.android.accpack.textchat.TextChatFragment;
import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import com.tokbox.android.textchatsample.config.OpenTokConfig;
import com.tokbox.android.textchatsample.ui.PreviewCameraFragment;
import com.tokbox.android.textchatsample.ui.PreviewControlFragment;
import com.tokbox.android.textchatsample.ui.RemoteControlFragment;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OneToOneCommunication.Listener, PreviewControlFragment.PreviewControlCallbacks,
        RemoteControlFragment.RemoteControlCallbacks, PreviewCameraFragment.PreviewCameraCallbacks, TextChatFragment.TextChatListener{

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    private final int permsRequestCode = 200;

    //OpenTok calls
    private OneToOneCommunication mComm;

    private RelativeLayout mPreviewViewContainer;
    private RelativeLayout mRemoteViewContainer;
    private RelativeLayout mAudioOnlyView;
    private RelativeLayout mLocalAudioOnlyView;
    private RelativeLayout.LayoutParams layoutParamsPreview;
    private FrameLayout mTextChatContainer;
    private RelativeLayout mCameraFragmentContainer;
    private RelativeLayout mActionBarContainer;

    private TextView mAlert;
    private ImageView mAudioOnlyImage;

    //UI control bars fragments
    private PreviewControlFragment mPreviewFragment;
    private RemoteControlFragment mRemoteFragment;
    private PreviewCameraFragment mCameraFragment;
    private FragmentTransaction mFragmentTransaction;

    //TextChat fragment
    private TextChatFragment mTextChatFragment;

    //Dialog
    ProgressDialog mProgressDialog;

    private OTKAnalyticsData mAnalyticsData;
    private OTKAnalytics mAnalytics;

    private boolean mAudioPermission = false;
    private boolean mVideoPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);

        //Init the analytics logging for internal use
        String source = this.getPackageName();

        SharedPreferences prefs = this.getSharedPreferences("opentok", Context.MODE_PRIVATE);
        String guidVSol = prefs.getString("guidVSol", null);
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString();
            prefs.edit().putString("guidVSol", guidVSol).commit();
        }

        setContentView(R.layout.activity_main);

        mPreviewViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mRemoteViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
        mAlert = (TextView) findViewById(R.id.quality_warning);
        mAudioOnlyView = (RelativeLayout) findViewById(R.id.audioOnlyView);
        mLocalAudioOnlyView = (RelativeLayout) findViewById(R.id.localAudioOnlyView);
        mTextChatContainer = (FrameLayout) findViewById(R.id.textchat_fragment_container);
        mCameraFragmentContainer = (RelativeLayout) findViewById(R.id.camera_preview_fragment_container);
        mActionBarContainer = (RelativeLayout) findViewById(R.id.actionbar_preview_fragment_container);

        //request Marshmallow camera permission
        if (ContextCompat.checkSelfPermission(this,permissions[1]) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,permissions[0]) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, permsRequestCode);
            }
        }
        else {
            mVideoPermission = true;
            mAudioPermission = true;
        }

        //init 1to1 communication object
        mComm = new OneToOneCommunication(MainActivity.this, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN, OpenTokConfig.API_KEY);
        //set listener to receive the communication events, and add UI to these events
        mComm.setListener(this);
        mComm.init();

        //init controls fragments
        if (savedInstanceState == null) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            initCameraFragment(); //to swap camera
            initPreviewFragment(); //to enable/disable local media
            initRemoteFragment(); //to enable/disable remote media
            initTextChatFragment(); //to send/receive text-messages
            mFragmentTransaction.commitAllowingStateLoss();
        }

        //show connecting dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setMessage("Connecting...");
        mProgressDialog.show();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mComm != null) {
            mComm.reloadViews(); //reload the local preview and the remote views
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mComm.destroy();
    }

    @Override
    public void onRequestPermissionsResult(final int permsRequestCode, final String[] permissions,
                                           int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                mVideoPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                mAudioPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;


                if ( !mVideoPermission || !mAudioPermission ){
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(getResources().getString(R.string.permissions_denied_title));
                    builder.setMessage(getResources().getString(R.string.alert_permissions_denied));
                    builder.setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("RE-TRY", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(permissions, permsRequestCode);
                            }
                        }
                    });
                    builder.show();
                }

                break;
        }
    }

    //Get OneToOneCommunicator object
    public OneToOneCommunication getComm() {
        return mComm;
    }

    public void showRemoteControlBar(View v) {
        if (mRemoteFragment != null && mComm.isRemote()) {
            mRemoteFragment.show();
        }
    }

    //Video local button event
    @Override
    public void onDisableLocalVideo(boolean video) {
        if (mComm != null) {
            mComm.enableLocalMedia(OneToOneCommunication.MediaType.VIDEO, video);

            if (mComm.isRemote()) {
                if (!video) {
                    mAudioOnlyImage = new ImageView(this);
                    mAudioOnlyImage.setImageResource(R.drawable.avatar);
                    mAudioOnlyImage.setBackgroundResource(R.drawable.bckg_audio_only);
                    mPreviewViewContainer.addView(mAudioOnlyImage, layoutParamsPreview);
                } else {
                    mPreviewViewContainer.removeView(mAudioOnlyImage);
                }
            } else {
                if (!video) {
                    mLocalAudioOnlyView.setVisibility(View.VISIBLE);
                    mPreviewViewContainer.addView(mLocalAudioOnlyView);
                } else {
                    mLocalAudioOnlyView.setVisibility(View.GONE);
                    mPreviewViewContainer.removeView(mLocalAudioOnlyView);
                }
            }
        }
    }

    //Call button event
    @Override
    public void onCall() {
        if (mComm != null && mComm.isStarted()) {
            mComm.end();
            cleanViewsAndControls();
        } else {
            mComm.start();
            if (mPreviewFragment != null) {
                mPreviewFragment.setEnabled(true);
            }
        }
    }

    //TextChat button event
    @Override
    public void onTextChat() {
        if (mTextChatContainer.getVisibility() == View.VISIBLE){
            mTextChatContainer.setVisibility(View.GONE);
            showAVCall(true);
        }
        else {
            showAVCall(false);
            mTextChatContainer.setVisibility(View.VISIBLE);
        }
    }

    //Audio remote button event
    @Override
    public void onDisableRemoteAudio(boolean audio) {
        if (mComm != null) {
            mComm.enableRemoteMedia(OneToOneCommunication.MediaType.AUDIO, audio);
        }
    }

    //Video remote button event
    @Override
    public void onDisableRemoteVideo(boolean video) {
        if (mComm != null) {
            mComm.enableRemoteMedia(OneToOneCommunication.MediaType.VIDEO, video);
        }
    }

    //Camera control button event
    @Override
    public void onCameraSwap() {
        if (mComm != null) {
            mComm.swapCamera();
        }
    }

    //OneToOneCommunicator listener events
    @Override
    public void onInitialized() {
        mProgressDialog.dismiss();
        if ( mTextChatFragment != null ) {
            try {
                //Init TextChat values
                mTextChatFragment.setMaxTextLength(140);
                mTextChatFragment.setSenderAlias("Tokboxer");
                mTextChatFragment.setListener(this);
            }catch(Exception e){
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    //OneToOneCommunication callbacks
    @Override
    public void onError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        mComm.end(); //end communication
        mProgressDialog.dismiss();
        cleanViewsAndControls(); //restart views
    }

    @Override
    public void onQualityWarning(boolean warning) {
        if (warning) { //quality warning
            mAlert.setBackgroundResource(R.color.quality_warning);
            mAlert.setTextColor(this.getResources().getColor(R.color.warning_text));
        } else { //quality alert
            mAlert.setBackgroundResource(R.color.quality_alert);
            mAlert.setTextColor(this.getResources().getColor(R.color.white));
        }
        mAlert.bringToFront();
        mAlert.setVisibility(View.VISIBLE);
        mAlert.postDelayed(new Runnable() {
            public void run() {
                mAlert.setVisibility(View.GONE);
            }
        }, 7000);
    }

    @Override
    public void onAudioOnly(boolean enabled) {
        if (enabled) {
            mAudioOnlyView.setVisibility(View.VISIBLE);
        }
        else {
            mAudioOnlyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPreviewReady(View preview) {
        mPreviewViewContainer.removeAllViews();
        if (preview != null) {
            layoutParamsPreview = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

             if (mComm.isRemote()) {
                layoutParamsPreview.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                        RelativeLayout.TRUE);
                layoutParamsPreview.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                        RelativeLayout.TRUE);
                layoutParamsPreview.width = (int) getResources().getDimension(R.dimen.preview_width);
                layoutParamsPreview.height = (int) getResources().getDimension(R.dimen.preview_height);
                layoutParamsPreview.rightMargin = (int) getResources().getDimension(R.dimen.preview_rightMargin);
                layoutParamsPreview.bottomMargin = (int) getResources().getDimension(R.dimen.preview_bottomMargin);
            }
            mPreviewViewContainer.addView(preview, layoutParamsPreview);
            if (!mComm.getLocalVideo()){
                onDisableLocalVideo(false);
            }
        }
    }

    @Override
    public void onRemoteViewReady(View remoteView) {
        //update preview when a new participant joined to the communication
        if (mPreviewViewContainer.getChildCount() > 0) {
            onPreviewReady(mPreviewViewContainer.getChildAt(0)); //main preview view
        }
        if (!mComm.isRemote()) {
            //clear views
            onAudioOnly(false);
            mRemoteViewContainer.removeView(remoteView);
            mRemoteViewContainer.setClickable(false);
        }
        else {
            //show remote view
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    this.getResources().getDisplayMetrics().widthPixels, this.getResources()
                    .getDisplayMetrics().heightPixels);
            mRemoteViewContainer.removeView(remoteView);
            mRemoteViewContainer.addView(remoteView, layoutParams);
            mRemoteViewContainer.setClickable(true);
        }
    }

    @Override
    public void onReconnecting() {
        Log.i(LOG_TAG, "The session is reconnecting.");
        Toast.makeText(this, R.string.reconnecting, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReconnected() {
        Log.i(LOG_TAG, "The session reconnected.");
        Toast.makeText(this, R.string.reconnected, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCameraChanged(int newCameraId) {
        Log.i(LOG_TAG, "The camera changed. New camera id is: "+newCameraId);
    }

    //TextChat Fragment listener events
    @Override
    public void onNewSentMessage(ChatMessage message) {
        Log.i(LOG_TAG, "New sent message");
    }

    @Override
    public void onNewReceivedMessage(ChatMessage message) {
        Log.i(LOG_TAG, "New received message");
    }

    @Override
    public void onTextChatError(String error) {
        Log.i(LOG_TAG, "Error on text chat "+error);
    }

    @Override
    public void onClosed() {
        Log.i(LOG_TAG, "OnClosed text-chat");
        mTextChatContainer.setVisibility(View.GONE);
        showAVCall(true);
        restartTextChatLayout(true);
    }

    @Override
    public void onRestarted() {
        Log.i(LOG_TAG, "OnRestarted text-chat");
    }

    //Private methods
    private void initPreviewFragment() {
        mPreviewFragment = new PreviewControlFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.actionbar_preview_fragment_container, mPreviewFragment).commit();
    }

    private void initRemoteFragment() {
        mRemoteFragment = new RemoteControlFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.actionbar_remote_fragment_container, mRemoteFragment).commit();
    }

    private void initCameraFragment() {
        mCameraFragment = new PreviewCameraFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.camera_preview_fragment_container, mCameraFragment).commit();
    }

    private void initTextChatFragment(){
        mTextChatFragment = TextChatFragment.newInstance(mComm.getSession(), OpenTokConfig.API_KEY);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.textchat_fragment_container, mTextChatFragment).commit();

    }

    //Audio local button event
    @Override
    public void onDisableLocalAudio(boolean audio) {
        if (mComm != null) {
            mComm.enableLocalMedia(OneToOneCommunication.MediaType.AUDIO, audio);
        }
    }

    //cleans views and controls
    private void cleanViewsAndControls() {
        if ( mPreviewFragment != null )
            mPreviewFragment.restart();
        if ( mRemoteFragment != null )
            mRemoteFragment.restart();
        if (mTextChatFragment != null ){
            restartTextChatLayout(true);
            mTextChatFragment.restart();
            mTextChatContainer.setVisibility(View.GONE);
        }
    }

    private void showAVCall(boolean show){
        if(show) {
            mActionBarContainer.setVisibility(View.VISIBLE);
            mPreviewViewContainer.setVisibility(View.VISIBLE);
            mRemoteViewContainer.setVisibility(View.VISIBLE);
            mCameraFragmentContainer.setVisibility(View.VISIBLE);
        }
        else {
            mActionBarContainer.setVisibility(View.GONE);
            mPreviewViewContainer.setVisibility(View.GONE);
            mRemoteViewContainer.setVisibility(View.GONE);
            mCameraFragmentContainer.setVisibility(View.GONE);
        }
    }

    private void restartTextChatLayout(boolean restart) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTextChatContainer.getLayoutParams();

        if (restart) {
            //restart to the original size
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        } else {
            //go to the minimized size
            params.height = dpToPx(40);
            params.addRule(RelativeLayout.ABOVE, R.id.actionbar_preview_fragment_container);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        }
        mTextChatContainer.setLayoutParams(params);
     }

    /**
     * Converts dp to real pixels, according to the screen density.
     *
     * @param dp A number of density-independent pixels.
     * @return The equivalent number of real pixels.
     */
    private int dpToPx(int dp) {
        double screenDensity = this.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }

    private void addLogEvent(String action, String variation){
        if ( mAnalytics!= null ) {
            mAnalytics.logEvent(action, variation);
        }
    }
}
