# Twilio Video SDK

Twilio Video SDK is custom SDK for integrating Twilio E2E Video Call feature with
managing [foreground service](https://developer.android.com/guide/components/foreground-services)

# Feature

This Video SDK contain all feature that:

1. Handle Incoming Call notification for Android 10+
2. Pip mode for Android Oreo and above while leaving call screen
3. Floating widget for below android oreo while leaving call screen
4. Handle multiple audio device switch
5. Switching camera easy and smooth way
6. Control audio & video turn off and on
7. Showing participant audio, video and network state
8. Showing image attachment during ongoing call.

# Using Technology

1. Twilio Video [SDK](https://www.twilio.com/docs/video)
2. Twilio [Audio Switch](https://github.com/twilio/audioswitch)
3. [ViewModel](https://developer.android.com/topic/libraries/architecture/livedata)
   & [LiveData](https://developer.android.com/topic/libraries/architecture/viewmodel) for lifecycle aware
4. [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for Dependency Injection

# Install

<hr>

First add `hilt` dependency on your project with follow
this [guideline](https://developer.android.com/training/dependency-injection/hilt-android)

To install twilio sdk on you android project add this `implementation` on you `app/build.gradle` file

```
implementation 'com.github.mainuddinm55:twilio-vc-sdk:1.0.15'
```

then, add this config on your `app/build.gradle` inside `android` config

```
hilt {
    enableExperimentalClasspathAggregation = true
}
```

# Usages

First initialize sdk with `TwilioSdk.init(context)` on your application class

```
@HiltAndroidApp
class YourApp :Application() {
    override fun onCreate() {
        super.onCreate()
        TwilioSdk.initSdk(this)
    }
}
```

then, start service from your activity or notification service class to start call.

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
