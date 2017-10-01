package com.blazeey.nearbycamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.blazeey.nearbycamera.Adapter.DiscoverAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DiscoverAdapter.DiscoverInterface {

    public static final String TAG = "NearbyCamera";
    public static final int REQUEST_CODE=999;

    @BindView(R.id.show_devices)
    RecyclerView showDevices;
    @BindView(R.id.advertise)
    FloatingActionButton advertise;
    @BindView(R.id.discover)
    FloatingActionButton discover;
    @BindView(R.id.send_button)
    FloatingActionButton sendButton;

    private Activity activity;
    private Context context;
    private DiscoverAdapter.DiscoverInterface discoverInterface;

    /**
     * These permissions are required before connecting to Nearby Connections. Only {@link
     * Manifest.permission#ACCESS_COARSE_LOCATION} is considered dangerous, so the others should be
     * granted just by having them in our AndroidManfiest.xml
     */
    private static final String[] REQUIRED_PERMISSIONS =

            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    /**
     * We'll talk to Nearby Connections through the GoogleApiClient.
     */
    private GoogleApiClient googleApiClient,googleApiClientSend;

    /**
     * The devices we've discovered near us.
     */
    private final Map<String, EndPoint> mDiscoveredEndpoints = new HashMap<>();
    private List<EndPoint> endPoints = new ArrayList<>();

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * <p> #acceptConnection(Endpoint)}.
     */
    private final Map<String, EndPoint> mPendingConnections = new HashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * <p>
     * there will only be one entry in this map.
     */
    private final Map<String, EndPoint> mEstablishedConnections = new HashMap<>();

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * <p>
     * device.
     */
    private boolean mIsConnecting = false;

    /**
     * True if we are discovering.
     */
    private boolean mIsDiscovering = false;

    /**
     * True if we are advertising.
     */
    private boolean mIsAdvertising = false;

    private DiscoverAdapter discoverAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        activity = this;
        context = this;
        discoverInterface = this;

        createGoogleApi();

        endPoints = getDiscoveredList();
        discoverAdapter = new DiscoverAdapter(this, endPoints, this);
        showDevices.setAdapter(discoverAdapter);


        advertise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAdvertising();
            }
        });
        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovering();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
//                startActivityForResult(intent,REQUEST_CODE);
                Uri uri = Uri.parse("content://com.android.providers.media.documents/document/image%3A41490");
                try {
                    send(getFileFromUri(uri),getDiscoveredEndpoints());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_CODE:
                try {
                    if (resultCode == RESULT_OK && data != null) {
                        Uri uri = data.getData();
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                        Payload filePayload = Payload.fromFile(pfd);
                        String payloadFilenameMessage = filePayload.getId() + ":" + uri.getLastPathSegment();

                        logV(uri.toString());
//                        send(filePayload,getDiscoveredEndpoints());

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

                break;
        }
    }

    public void createGoogleApi() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.CONNECTIONS_API)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .build();
        }
    }

    @Override
    public void onClick(EndPoint endPoint) {
        connectToEndpoint(endPoint);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasPermissions(this, REQUIRED_PERMISSIONS)) {
            googleApiClient.connect();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_REQUIRED_PERMISSIONS:
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "Requests not granted", Toast.LENGTH_SHORT).show();
                    }
                }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(googleApiClient.isConnected())
            googleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!googleApiClient.isConnected()||googleApiClient.isConnecting())
        {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v("CONNECTION : ", "Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v("CONNECTION : ", "Connection Suspended " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v("CONNECTION : ", "Connection Failed Status : " + connectionResult.getErrorMessage());
    }

    /**
     * =============================================== CONNECTION CALLBACK METHODS =============================================
     */

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.v("ENDPOINT", "Endpoint ID : " + endpointId + "ConnectionInfo : " + connectionInfo.getEndpointName());

            EndPoint endPoint = new EndPoint(endpointId, connectionInfo.getEndpointName());
            mPendingConnections.put(endpointId, endPoint);
            acceptConnection(endPoint);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    logV("Connected Status OK");
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    logV("Connected Status Rejected");
                    break;
                case ConnectionsStatusCodes.STATUS_BLUETOOTH_ERROR:
                    logV(connectionResolution.getStatus().getStatusMessage());
                    break;
                default:
//                    logV(connectionResolution.getStatus().getStatusMessage());
                    break;
            }

        }

        @Override
        public void onDisconnected(String endpointId) {

        }
    };

    protected void acceptConnection(final EndPoint endpoint) {
        Nearby.Connections.acceptConnection(googleApiClient, endpoint.getEndpointId(), mPayloadCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(
                                            String.format(
                                                    "acceptConnection failed. %s", MainActivity.toString(status)));
                                } else {
                                    Toast.makeText(activity, "Connection accepted : " + endpoint.getEndPointName(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
    }

    /**
     * Rejects a connection request.
     */
    protected void rejectConnection(EndPoint endpoint) {
        Nearby.Connections.rejectConnection(googleApiClient, endpoint.getEndpointId())
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(
                                            String.format(
                                                    "rejectConnection failed. %s", MainActivity.toString(status)));
                                }
                            }
                        });
    }

    /** -X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-*/

    /**
     * =============================================== ADVERTISING METHODS =============================================
     */

    protected void startAdvertising() {
        mIsAdvertising = true;
        Nearby.Connections.startAdvertising(
                googleApiClient,
                null,
                getServiceId(),
                connectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    logV("Now advertising endpoint " + result.getLocalEndpointName());
                                    mIsAdvertising = true;
                                    onAdvertisingStarted();
                                } else {
                                    mIsAdvertising = false;
                                    logW(
                                            String.format(
                                                    "Advertising failed. Received status %s.",
                                                    MainActivity.toString(result.getStatus())));

                                    if (result.getStatus().getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) {
                                        stopAdvertising();
                                        Toast.makeText(MainActivity.this, "Stopped Advertising", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
    }

    protected void stopAdvertising() {
        mIsAdvertising = false;
        Nearby.Connections.stopAdvertising(googleApiClient);
    }

    private void onAdvertisingFailed() {
        Toast.makeText(this, "Advertising Failed", Toast.LENGTH_SHORT).show();
    }

    private void onAdvertisingStarted() {
        Toast.makeText(this, "Started Advertising", Toast.LENGTH_SHORT).show();
    }

    protected boolean isAdvertising() {
        return mIsAdvertising;
    }


    /** -X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-*/

    /**
     * =============================================== DISCOVERING METHODS =============================================
     */

    protected void startDiscovering() {
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        Nearby.Connections.startDiscovery(
                googleApiClient,
                getServiceId(),
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        logD(
                                String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));

                        if (getServiceId().equals(info.getServiceId())) {
                            EndPoint endpoint = new EndPoint(endpointId, info.getEndpointName());
                            mDiscoveredEndpoints.put(endpointId, endpoint);
                            endPoints = getDiscoveredList();
                            DiscoverAdapter adapter = new DiscoverAdapter(context, endPoints, discoverInterface);
                            showDevices.setAdapter(adapter);
                            connectToEndpoint(endpoint);
                            onEndpointDiscovered(endpoint);
                        }
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        logD(String.format("onEndpointLost(endpointId=%s)", endpointId));
                    }
                },
                new DiscoveryOptions(STRATEGY))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    onDiscoveryStarted();
                                } else {
                                    mIsDiscovering = false;
                                    logW(
                                            String.format(
                                                    "Discovering failed. Received status %s.",
                                                    MainActivity.toString(status)));
                                    if (status.getStatusCode() == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) {
                                        Nearby.Connections.stopDiscovery(googleApiClient);
                                        Toast.makeText(MainActivity.this, "Discovery Stopped", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
    }

    protected void connectToEndpoint(final EndPoint endpoint) {

        logV("Sending a connection request to endpoint " + endpoint);

        mIsConnecting = true;

        // Ask to connect
        Nearby.Connections.requestConnection(
                googleApiClient, null, endpoint.getEndpointId(), connectionLifecycleCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(
                                            String.format(
                                                    "requestConnection failed. %s", MainActivity.toString(status)));
//                                    logV(status.getStatusMessage());
                                    mIsConnecting = false;
                                    onConnectionFailed(endpoint);
                                } else {
                                    Toast.makeText(MainActivity.this, "Connected to Endpoint : " + endpoint.getEndPointName(), Toast.LENGTH_SHORT).show();
                                    logV("Connected to Endpoint : " + endpoint.getEndPointName());

                                }
                            }
                        });
    }

    private void onDiscoveryFailed() {
        Toast.makeText(this, "Discovery Failed", Toast.LENGTH_SHORT).show();
    }

    private void onDiscoveryStarted() {
        Toast.makeText(this, "Discovery Started", Toast.LENGTH_SHORT).show();
    }

    private void onEndpointDiscovered(EndPoint endpoint) {
        Toast.makeText(this, "EndPoint Discovered : " + endpoint.getEndPointName(), Toast.LENGTH_SHORT).show();
    }

    protected void onConnectionFailed(EndPoint endpoint) {
        Toast.makeText(this, "Connection Failed : " + endpoint.getEndPointName(), Toast.LENGTH_SHORT).show();
    }

    /**-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-*/

    /**
     * ==================================================SEND PAYLOAD==================================================
     */

    private final PayloadCallback mPayloadCallback = new PayloadCallback() {

        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            logD(String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
            onReceive(mEstablishedConnections.get(endpointId), payload);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            logD(
                    String.format(
                            "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
        }
    };

    private void send(Payload payload, Set<String> endpoints) {

        Nearby.Connections.sendPayload(googleApiClient, new ArrayList<>(endpoints), payload)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(
                                            String.format(
                                                    "sendUnreliablePayload failed. %s",
                                                    MainActivity.toString(status)));
                                } else {
                                    Toast.makeText(MainActivity.this, "Payload Sent", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
    }

    protected void onReceive(EndPoint endpoint, Payload payload) {
        Payload.File file = payload.asFile();
        Toast.makeText(this, "Payload Received Size : "+file.getSize(), Toast.LENGTH_SHORT).show();
    }

    /**
     * -X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-X-
     */

    public String getServiceId() {
        return "com.blazeey.NearbyCamera";
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public List<EndPoint> getDiscoveredList() {

        List<EndPoint> endPointList = new ArrayList<>();

        for (String key : mDiscoveredEndpoints.keySet()) {
            endPointList.add(mDiscoveredEndpoints.get(key));
        }

        return endPointList;
    }

    public Payload getFileFromUri(Uri uri) throws FileNotFoundException {

        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        return Payload.fromFile(pfd);
    }

    protected Set<String> getDiscoveredEndpoints() {
        return mDiscoveredEndpoints.keySet();
    }

    @CallSuper
    protected void logV(String msg) {
        Log.v(TAG, msg);
    }

    @CallSuper
    protected void logD(String msg) {
        Log.d(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg) {
        Log.w(TAG, msg);
    }

    @CallSuper
    protected void logE(String msg, Throwable e) {
        Log.e(TAG, msg, e);
    }

    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

}
