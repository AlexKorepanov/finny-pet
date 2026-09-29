package ru.finny.petgame.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.ui.components.LeafPhase
import ru.finny.petgame.ui.components.leafRustle

class LeafRustleTest {

    @Test
    fun coverRustleSwellsThenSettles() {
        val curve = leafRustle(LeafPhase.COVER, 45)
        println("COVER  " + curve.joinToString(" ") { "%.2f".format(it) })
        val peak = curve.indices.maxBy { curve[it] }
        assertTrue("пока листья выше экрана, тихо", curve[0] < 0.05f)
        assertTrue("пик — когда листья влетают в кадр", peak in curve.size / 3 until curve.size * 3 / 4)
        assertTrue("к концу листья почти легли", curve.last() < 0.2f)
    }

    @Test
    fun revealRustleFadesAsLeavesLeave() {
        val curve = leafRustle(LeafPhase.REVEAL, 80)
        println("REVEAL " + curve.joinToString(" ") { "%.2f".format(it) })
        assertTrue("листья шелестят, пока разлетаются", curve.count { it > 0.3f } > 10)
        assertTrue("улетевшие листья не вибрируют", curve.takeLast(5).all { it < 0.1f })
    }
}
