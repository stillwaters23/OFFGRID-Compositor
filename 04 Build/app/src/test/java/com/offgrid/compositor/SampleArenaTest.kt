package com.offgrid.compositor

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleArenaTest {
    private fun bytes(size: Int, value: Int) = ByteBuffer.wrap(ByteArray(size) { value.toByte() })

    private fun SampleArena.firstByte(piece: SampleArena.Piece) = read(piece).get(0).toInt()

    @Test
    fun keepsPiecesInOrderAndReadsThemBack() {
        val arena = SampleArena(100, direct = false)
        arena.add(bytes(30, 1), 30, 10, 0)
        arena.add(bytes(30, 2), 30, 20, 0)
        val pieces = arena.snapshot()
        assertEquals(listOf(1, 2), pieces.map { arena.firstByte(it) })
        assertEquals(30, arena.read(pieces[0]).remaining())
    }

    @Test
    fun letsGoOfTheOldestWhenFullAndNeverGrows() {
        val arena = SampleArena(100, direct = false)
        for (i in 1..10) assertTrue(arena.add(bytes(30, i), 30, i * 10L, 0))
        val pieces = arena.snapshot()
        assertTrue(pieces.size <= 3)
        assertEquals(10, arena.firstByte(pieces.last()))
        assertEquals(listOf(8, 9, 10), pieces.map { arena.firstByte(it) })
        assertTrue(pieces.sumOf { it.size } <= arena.capacity)
    }

    @Test
    fun survivingPiecesAreNeverOverwritten() {
        val arena = SampleArena(100, direct = false)
        for (i in 1..25) arena.add(bytes(7 + i % 5, i), 7 + i % 5, i * 10L, 0)
        for (piece in arena.snapshot()) {
            val data = arena.read(piece)
            val first = data.get(0)
            for (n in 0 until piece.size) assertEquals(first, data.get(n))
        }
    }

    @Test
    fun countsOnlyPiecesLostToSpace() {
        val arena = SampleArena(100, direct = false)
        for (i in 1..5) arena.add(bytes(30, i), 30, i * 10L, 0)
        assertEquals(2L, arena.overwritten)
        arena.dropBefore(1_000)
        assertEquals(2L, arena.overwritten)
    }

    @Test
    fun refusesAPieceBiggerThanTheArena() {
        assertFalse(SampleArena(10, direct = false).add(bytes(20, 1), 20, 0, 0))
    }

    @Test
    fun dropsOldPiecesByTimeButKeepsTheNewest() {
        val arena = SampleArena(100, direct = false)
        for (i in 1..3) arena.add(bytes(10, i), 10, i * 100L, 0)
        arena.dropBefore(10_000)
        assertEquals(1, arena.size)
        assertEquals(300L, arena.newestTimeUs)
    }
}
