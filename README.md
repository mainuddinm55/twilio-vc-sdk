# Twilio Video SDK

Twilio Video SDK is custom SDK for integrating Twilio E2E Video Call feature with
managing [foreground service](https://developer.android.com/guide/components/foreground-services)

# Install

<hr>

To install twilio sdk on you android project add this `implementation` on you `build.gradle` file

```
implementation 'com.github.mainuddinm55:twilio-vc-sdk:1.0.15'
```

# Usages

<hr>

```
 
 VideoCallService.startService(
    this,
    CallOptions(
        "${twilio_token_url}",
        room,
        remoteUniqueIdentity,
        actionData, //action data is releted about the call
        remoteUserName,
        emptyList(), //attachment that showing in ongoing call
        UserType.RECEIVER //user type CALLER or RECEIVER
    ),
    VideoCallListenerReceiver()
)

```

To receive call related information through `BroadcastReceiver` like `VideoCallListenerReceiver()`

To register your `BroadcastReceiver` first register on your `AndroidManifest` with that `intent-filter`

`ACTION_VIDEO_CALL_CALLBACK` intent filter for call related information
`ACTION_CALL_DATA` intent filter for call log related information

```
<receiver
    android:name=".VideoCallListenerReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="ACTION_VIDEO_CALL_CALLBACK" />
    </intent-filter>
    <intent-filter>
        <action android:name="ACTION_CALL_DATA" />
    </intent-filter>
</receiver>
```

### `ACTION_VIDEO_CALL_CALLBACK` `intent-filter`

<hr>

Handled type are:

```
private fun handleVideoCallAction(context: Context, intent: Intent) {

    if (intent.action == TwilioSdk.ACTION_CALLBACK) {
        val type = intent.getStringExtra(TwilioSdk.EXTRA_TYPE)
        val msg = intent.getStringExtra(TwilioSdk.EXTRA_MSG)
        val callOptionJson = intent.getStringExtra(TwilioSdk.EXTRA_CALL_OPTIONS)

        Timber.d("handleVideoCallAction $type $msg $callOptionJson")
        when(type) {
            TwilioSdk.TYPE_REJECT -> {
                if (callOptionJson != null) {
                    val callOption: CallOptions =  Gson().fromJson(callOptionJson, CallOptions::class.java)
                    sendNotification(context, type, callOption)
                }
                CallingInfoWorker.setCallingInfoToServer(context)
            }
            TwilioSdk.TYPE_RINGING -> {

            }
            TwilioSdk.TYPE_MISSED_CALL -> {

            }
            TwilioSdk.TYPE_INCOMING -> {

            }
            TwilioSdk.TYPE_ACCEPT -> {

            }
            TwilioSdk.TYPE_END -> {
                CallingInfoWorker.setCallingInfoToServer(context)
            }
            TwilioSdk.TYPE_FAILED -> {

            }
            TwilioSdk.TYPE_CONNECTING -> {

            }
            TwilioSdk.TYPE_CONNECTED -> {

            }
            TwilioSdk.TYPE_RECONNECTING -> {

            }
            TwilioSdk.TYPE_RECONNECTED -> {

            }
        }
    }
}
```

### `ACTION_CALL_DATA` `intent-filter`

<hr>

Handled type are:

```

    private fun handleVideoCallLog(context: Context, intent: Intent) {
        if (intent.action == TwilioSdk.ACTION_CALL_DATA) {
            val dataKey = intent.getStringExtra(TwilioSdk.EXTRA_CALL_DATA_KEY)
            val callOptionJson = intent.getStringExtra(TwilioSdk.EXTRA_CALL_OPTIONS)
            val callOption: CallOptions =  Gson().fromJson(callOptionJson, CallOptions::class.java)

            Timber.d("handleVideoCallLog $dataKey $callOptionJson ${intent.extras?.bundleToString()}")

            intent.extras?.let {
                val hashmap = bundleToHashmap(it)
                hashmap.remove(TwilioSdk.EXTRA_CALL_DATA_KEY)
                hashmap.remove(TwilioSdk.EXTRA_CALL_OPTIONS)
                val callingInfo = CallingInfo(callOption.actionData?.toIntOrNull() ?: 0, "${dataKey}user", Gson().toJson(hashmap))
                //Handle log related data
                CallingInfoWorker.setLocalAction(context, callingInfo)
            }
        }
    }

    private fun bundleToHashmap(bundle: Bundle): HashMap<String, String> {
        val haspMap: HashMap<String, String> = hashMapOf()
        bundle.keySet().iterator().forEach { key ->
            haspMap[key] = bundle.getString(key) ?: ""
        }
        return haspMap
    }
```

# Preview

<hr>
