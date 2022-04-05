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
        // Be sure to replace TAG with the most recent version
        implementation 'com.github.OperatorFoundation:ShapeshifterAndroidKotlin:TAG'

        // Later releases of bouncycastle may not work with ShapeshifterAndroidKotlin
        implementation 'org.bouncycastle:bcpkix-jdk15on:1.58'

        implementation 'com.google.code.gson:gson:2.8.2'
        implementation 'com.google.guava:guava:31.0.1-android'

}
```

3) Make sure the min SDK in your build.gradle is 21 or higher in each project/app related build.gradle


## Using the Library
1) Create the Bloom Filter
```
val bloomFilter = Bloom()
```

2) Load the Bloom Filter from the path given (include the file name)
```
bloomFilter.load(fileName)
```   

3) Create a shadow config, putting the password and cipher name.  If you're using DarkStar, put the Server's Persistent Public Key in place of the password.
```
val config = ShadowConfig(password, cipherName)
```

4) Make a Shadow Socket with the config, the host, and the port.
```
val shadowSocket = ShadowSocket(config, host, port)
```

5) Get the output stream and write some bytes.
```
shadowSocket.outputStream.write(textBytes)
```

6) Flush the output stream.
```
shadowSocket.outputStream.flush()
```

7) Get the input stream and read some bytes into an empty buffer.
```
shadowSocket.inputStream.read(buffer)
```

8) Save the Bloom Filter to the path given at the end of the session (include file name)
```
bloomFilter.save(fileName)
```