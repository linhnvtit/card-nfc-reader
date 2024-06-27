# Card NFC Reader
- Card NFC Reader is a lightweight and easy-to-use library for reading smart card information using NFC technology. It supports retrieving card brand, card number, and expiration date from compatible smart cards.
- Card NFC Reader now supports Mastercard and Visa, with plans to include other card schemes in upcoming versions.

## Gradle
```groovy
dependencies {
    implementation 'com.github.linhnvtit:card-nfc-reader:1.0.1'
}
```
```groovy
repositories {
    maven { url = uri("https://jitpack.io") }
}
```

## Requirement
Following instructions from [Android docs](https://developer.android.com/develop/connectivity/nfc)
#### Mandatory:
 * Enable NFC on your device
 * Add ```<uses-permission android:name="android.permission.NFC" />``` in Manifest.xml 

## Usage

### Prepare to receive data 
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

