NFC Reader is a lighweight android nfc smart card reader library. NFC Reader will allow you to get basic data from smart card(VISA, Mastercard for now) such as card's brand, number, expire date, holder name

# Gradle
```groovy
dependencies {
    implementation 'com.github.linhnvt.nfcreader.1-.01-.01'
}
```
```groovy
repositories {
    ....
    maven { url = uri("https://jitpack.io") }
}
```

# Requirement
Following instructions from [Android docs](https://developer.android.com/develop/connectivity/nfc)
#### Mandatory:
 * Enable NFC on your device
 * Add ```<uses-permission android:name="android.permission.NFC" />``` in Manifest.xml 

# Let's read smart card's data

## Prepare to receive data 
* #### Inherit your MainActivity with NfcReaderActivity
NfcReaderActivity will handle all the NFC Configurations needed for you.
```kotlin
class MainActivity : NfcReaderActivity() { }
```

If you want to custom, then you need to handle it manually 
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
        NfcReader.init(this@NfcReaderActivity)
        NfcReader.handleNewIntent(intent)
    }
}

override fun onPause() {
    super.onPause()
    NfcReader.disableNfcScanning(this@NfcReaderActivity)
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    NfcReader.handleNewIntent(intent)
}
```

* #### Enable NFC on foreground if you only allow it on foreground and not declare in Manifest.xml
```kotlin 
NfcReader.enableNfcScanning(this@MainActivity)
```

* #### Get card's data state
```kotlin
 NfcReader.getSourceStateFlow()
```
A state flow that will reflect the state of card's data fetching process.

