package org.operatorfoundation.shadow

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

internal class ShadowTest
{
    @Test
    fun testShadowConfig()
    {
        val shadowConfig = ShadowConfig("notanactualkey", "darkstar", "127.0.0.1", 1111)
        val shadowConfigJSON = Json.encodeToString(shadowConfig)
        println(shadowConfigJSON)

        val shadowConfigDecoded = Json.decodeFromString<ShadowConfig>(shadowConfigJSON)
        assert(shadowConfig.serverPublicKey == shadowConfigDecoded.serverPublicKey)
    }
}