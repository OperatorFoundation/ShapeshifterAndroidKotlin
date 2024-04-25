### The Operator Foundation

[Operator](https://operatorfoundation.org) makes usable tools to help people around the world with censorship, security, and privacy.

### Shapeshifter

The Shapeshifter project provides network protocol shapeshifting technology
(also sometimes referred to as obfuscation). The purpose of this technology is
to change the characteristics of network traffic so that it is not identified
and subsequently blocked by network filtering devices.

There are two components to Shapeshifter: transports and the dispatcher. Each
transport provide different approach to shapeshifting. ShapeshifterAndroidKotlin is provided as a 
Kotlin library which can be integrated directly into Android applications.

If you are a tool developer working in the Kotlin programming language for Android, then you
are in the right place. If you are a tool developer working in other languages we have 
several other tools available to you:

- A Go transports library that can be used directly in your application:
[shapeshifter-transports](https://github.com/OperatorFoundation/shapeshifter-transports)

- A Swift transport library that can be used directly in your iOS and macOS applications:
[ShadowSwift](https://github.com/OperatorFoundation/ShadowSwift.git)

- A Java transports library that can be used directly in your Android application (currently supports Shadow):
[ShapeshifterAndroidJava](https://github.com/OperatorFoundation/ShapeshifterAndroidJava)

If you want a end user that is trying to circumvent filtering on your network or
you are a developer that wants to add pluggable transports to an existing tool
that is not written in the Kotlin programming language, then you probably want the
dispatcher. Please note that familiarity with executing programs on the command
line is necessary to use this tool.
<https://github.com/OperatorFoundation/shapeshifter-dispatcher>

If you are looking for a complete, easy-to-use VPN that incorporates
shapeshifting technology and has a graphical user interface, consider
[Moonbounce](https://github.com/OperatorFoundation/Moonbounce), an application for macOS which incorporates shapeshifting without
the need to write code or use the command line.

### Shapeshifter Transports

Shapeshifter Transports is a suite of pluggable transports implemented in a variety of langauges. This repository 
is the Shapeshifter implementation in Kotlin for Android applications. 

If you are looking for a tool which you can install and
use from the command line, take a look at [shapeshifter-dispatcher](https://github.com/OperatorFoundation/shapeshifter-dispatcher.git) instead.

ShapeshifterAndroidKotlin implements the Pluggable Transports 3.0 specification available here:
<https://github.com/Pluggable-Transports/Pluggable-Transports-spec/tree/main/releases/PTSpecV3.0> 

The purpose of the transport library is to provide a set of different
transports. Each transport implements a different method of shapeshifting
network traffic. The goal is for application traffic to be sent over the network
in a shapeshifted form that bypasses network filtering, allowing
the application to work on networks where it would otherwise be blocked or
heavily throttled.

# ShapeshifterAndroidKotlin

ShapeshifterAndroidKotlin is a native Android library that contains an implementation of the Shadow transport. Shadow is a wrapper for Shadowsocks that makes it available as a Pluggable Transport. Shadowsocks is a fast, free, and open-source encrypted proxy project, used to circumvent Internet censorship by utilizing a simple, but effective encryption and a shared password. 
 

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

1) Create a shadow config, putting the password and cipher name.  If you're using DarkStar, put the Server's Persistent Public Key in place of the password.
```
val config = ShadowConfig(password, cipherName)
```

2) Make a Shadow Socket with the config, the host, and the port.
```
val shadowSocket = ShadowSocket(config, host, port)
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
