/*
	MIT License

	Copyright (c) 2020 Operator Foundation

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
*/

package org.operatorfoundation.shadow

import kotlinx.serialization.Serializable
import android.util.Log
import kotlinx.serialization.Transient
import java.lang.IllegalArgumentException

// ShadowConfig is a class that implements the arguments necessary for a Shadowsocks connection.
@Serializable
class ShadowConfig(val password: String, val cipherName: String, val serverIP: String? = null, val port: Int? = null)
{
    /**
     * serverIP and port properties were added so that JSON config files can be brought into line with iOS, macOS, and Linux (Swift)
     * These are being left as optionals defaulting to null to minimize any impact this change will have until we are able to work on normalizing configs across all platforms
     */

    @Transient lateinit var cipherMode: CipherMode

    init {
        var maybeMode: CipherMode? = null

        try
        {
            maybeMode = when (cipherName)
            {
                "DarkStar" -> CipherMode.DarkStar
                else -> null
            }
        }
        catch (error: IllegalArgumentException)
        {
            Log.e("ShadowConfig", "Invalid cipherMode in the config: $cipherName")
        }

        requireNotNull(maybeMode)
        cipherMode = maybeMode
    }
}
