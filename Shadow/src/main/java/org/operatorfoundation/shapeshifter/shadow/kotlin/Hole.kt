package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.net.Socket
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Hole {
    fun startHole(timeoutDelay: Int, socket: Socket) {
        var currentTimeInSeconds = Calendar.getInstance().timeInMillis / 1000
        val endTime = currentTimeInSeconds + timeoutDelay
        val scheduler = Executors.newScheduledThreadPool(1)

        startPacketDelayTimer(endTime, socket, scheduler)
    }

    fun startPacketDelayTimer(mainTimer: Long, socket: Socket, scheduler: ScheduledExecutorService) {
        val currentTimeInSeconds = Calendar.getInstance().timeInMillis / 1000 // convert from milliseconds to seconds
        val packetTimerMax = 5
        val packetTimerMin = 1
        val packetSizeMax = 1440 - 16 // max TCP size without encryption overhead
        val packetSizeMin = 1
        var countdownStarter = betweenRNG(packetTimerMax, packetTimerMin)

        if (mainTimer - currentTimeInSeconds > 0) {
            val runnable = Runnable() {
                fun run() {
                    val packetSize = betweenRNG(packetSizeMax, packetSizeMin)
                    var packet = ByteArray(packetSize)
                    val random = java.security.SecureRandom()
                    random.nextBytes(packet)
                    socket.outputStream.write(packet)
                    startPacketDelayTimer(mainTimer, socket, scheduler)
                }
            }
            scheduler.schedule(runnable, countdownStarter.toLong(), TimeUnit.SECONDS)
        } else {
            scheduler.shutdown()
            socket.close()
        }
    }

    // generate a random number between the two numbers
    fun betweenRNG(maxNumber: Int, minNumber: Int): Int {
        // if we want maxNumber to be inclusive, add one
        // minNumber is inclusive
        val r = java.security.SecureRandom()
        return r.nextInt(maxNumber - minNumber) + minNumber
    }
}