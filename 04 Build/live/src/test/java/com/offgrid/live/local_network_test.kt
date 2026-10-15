package com.offgrid.live

import android.content.pm.PackageManager
import java.io.File
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Phase 0.4: the two-phone link must work with no CHANGE_WIFI_MULTICAST_STATE and must always
 * have a typed-address path when Wi-Fi discovery is blocked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
// Robolectric would otherwise install Conscrypt as the top JCA provider for the whole test JVM,
// which breaks the plain JUnit X25519 handshake tests that run after this class.
@ConscryptMode(ConscryptMode.Mode.OFF)
class LocalNetworkTest {
    @Test
    fun discoveryStartsWithoutMulticastPermission() {
        val context: android.content.Context = RuntimeEnvironment.getApplication()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toList().orEmpty()
        assertFalse(requested.contains("android.permission.CHANGE_WIFI_MULTICAST_STATE"))
        assertFalse(requested.contains("android.permission.ACCESS_WIFI_STATE"))

        var failure: String? = null
        val discovery = RemoteServiceDiscovery(context) { failure = it }
        discovery.discover { }
        val manager = context.getSystemService(android.net.nsd.NsdManager::class.java)
        val listeners = shadowOf(manager).getDiscoveryListeners(RemoteServiceDiscovery.SERVICE_TYPE)
        assertEquals(1, listeners?.size)
        assertNull(failure)
        discovery.stopDiscovery()
        assertTrue(shadowOf(manager).getDiscoveryListeners(RemoteServiceDiscovery.SERVICE_TYPE).isNullOrEmpty())
    }

    @Test
    fun discoverySourceHasNoMulticastLock() {
        val source = File("src/main/java/com/offgrid/live/LocalNetwork.kt").readText()
        assertFalse(source.contains("MulticastLock"))
        assertFalse(source.contains("WifiManager"))
        assertTrue(source.contains("manager.discoverServices(SERVICE_TYPE"))
    }

    @Test
    fun manualAddressAcceptsPrivateAddressWithPort() {
        val address = ManualAddress.parse(" 192.168.1.5:9100 ")
        assertEquals("192.168.1.5", address.address.hostAddress)
        assertEquals(9100, address.port)
    }

    @Test
    fun manualAddressFillsInTheDefaultPort() {
        val address = ManualAddress.parse("10.0.0.7")
        assertEquals("10.0.0.7", address.address.hostAddress)
        assertEquals(CameraRemoteServer.DEFAULT_PORT, address.port)
        assertEquals("10.0.0.7:9000", ManualAddress.describe(address.address, address.port))
    }

    @Test
    fun manualAddressRefusesPublicAddresses() {
        assertRefused("8.8.8.8:9000", "private")
    }

    @Test
    fun manualAddressRefusesNamesAndGarbage() {
        assertRefused("camera.local:9000", "four numbers")
        assertRefused("", "Type the camera phone")
        assertRefused("192.168.1", "four numbers")
        assertRefused("192.168.1.999", "0 to 255")
    }

    @Test
    fun manualAddressRefusesBadPorts() {
        assertRefused("192.168.1.5:0", "port")
        assertRefused("192.168.1.5:70000", "port")
        assertRefused("192.168.1.5:abc", "port")
    }

    @Test
    fun manualAddressNeverResolvesNames() {
        val address = ManualAddress.parse("172.16.4.9")
        assertEquals(InetAddress.getByAddress(byteArrayOf(172.toByte(), 16, 4, 9)), address.address)
        assertFalse(address.isUnresolved)
    }

    private fun assertRefused(text: String, messagePart: String) {
        try {
            ManualAddress.parse(text)
            fail("Expected $text to be refused")
        } catch (error: IllegalArgumentException) {
            assertTrue(
                "Message for '$text' should mention '$messagePart' but was: ${error.message}",
                error.message.orEmpty().contains(messagePart, ignoreCase = true),
            )
        }
    }
}
