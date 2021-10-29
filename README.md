# ShapeshifterAndroidKotlin

Shadowsocks is a simple, but effective and popular network traffic obfuscation tool that uses basic encryption with a shared password. shadow is a wrapper for Shadowsocks that makes it available as a Pluggable Transport.

## Setting up dependencies

1) add the following at the end of repositories in your PROJECT's build.gradle:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2) add the dependency in your MODULE's build.gradle:
```
dependencies {
        // Be sure to replace tag with the most recent version
        implementation 'com.github.OperatorFoundation:ShapeshifterAndroidKotlin:TAG'

        // Later releases of bouncycastle may not work with ShapeshifterAndroidKotlin
        implementation 'org.bouncycastle:bcpkix-jdk15on:1.58'

        // libsodium is only necessary if you plan on using ChaCha20
        implementation 'com.github.joshjdevl.libsodiumjni:libsodium-jni-aar:2.0.2'
        testImplementation 'com.github.joshjdevl.libsodiumjni:libsodium-jni-aar:2.0.2'
        androidTestImplementation 'com.github.joshjdevl.libsodiumjni:libsodium-jni-aar:2.0.2'
}
```

3) Make sure the min SDK in your build.gradle is 21 or higher in each project/app related build.gradle

4) If you plan on supporting ChaCha20, add the following to your AndroidManifest.xml:
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
   xmlns:tools="http://schemas.android.com/tools"
   package="org.YourPackagesName">

   <uses-permission android:name="android.permission.INTERNET" />
   <application
       tools:replace="android:allowBackup"
       ...
   </application>
   ...
</manifest>
```

## Using the Library
1) Create a shadow config, putting the password and cipher name.  If you're using DarkStar, put the Server's Persistent Public Key in place of the password.
```
val config = ShadowConfig("Password", "CipherName")
```

2) Make a Shadow Socket with the config, the host, and the port.
```
val shadowSocket = ShadowSocket(config, "host", port)
```

3) Get the output stream and write some bytes.
```
shadowSocket.outputStream.write(textBytes)
```

4) Flush the output stream.
```
shadowSocket.outputStream.flush()
```

5) Get the input stream and read some bytes into an empty buffer.
```
shadowSocket.inputStream.read(buffer)
```