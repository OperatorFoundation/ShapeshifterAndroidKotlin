ShapeshifterAndroidKotlin dependency setup:

    1) add the following at the end of repositories in your PROJECT's build.gradle:
	    allprojects {
	    	repositories {
	    		...
	    		maven { url 'https://jitpack.io' }
	    	}
	    }

	2) add the dependency in your MODULE's build.gradle:
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

    3) Make sure the min SDK in your build.gradle is 29 in each project/app related build.gradle

    4) If you plan on supporting ChaCha20, add the following to your AndroidManifest.xml:
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