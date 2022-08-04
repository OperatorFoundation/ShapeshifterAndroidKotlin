package org.operatorfoundation.shapeshifter.kotlin.shadow

fun ByteArray.toHexString() = asUByteArray().joinToString("") {
    it.toString(16).padStart(2, '0')
}