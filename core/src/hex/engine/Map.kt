package hex.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object MapReader {

    fun readMap(mapAsset: String): LevelMap {
        val json = JsonReader()
        val base = json.parse(Gdx.files.internal(mapAsset))

        val tileWidth = base.getInt("tilewidth", 32)
        val tileHeight = base.getInt("tileheight", 32)
        val hexSideLength = base.getInt("hexsidelength", 16)
        val tileSets = base.get("tilesets").asIterable().map { TileSetRef(it.getInt("firstgid"), it.getString("source")) }
        val layers = parseTileLayers(base.get("layers").asIterable())

        return LevelMap(tileWidth, tileHeight, hexSideLength, layers, tileSets)
    }

    private fun parseTileLayers(jsonLayers: Iterable<JsonValue>): Map<String, Layer> {
        val layers = HashMap<String, Layer>()
        jsonLayers.filter { layer -> layer.getString("type") == "tilelayer" }
                .forEach { layer ->
                    val data = layer.get("data").asIntArray()
                    val width = layer.getInt("width")
                    val height = layer.getInt("height")

                    layers.put(layer.getString("name", "main"), Layer(data, width, height))
                }
        return layers
    }
}

class Layer(val data: IntArray, val width: Int, val height: Int)

class TileSetRef(val firstGid: Int, val source: String)

class LevelMap(val tileWidth: Int = 32,
               val tileHeight: Int = 32,
               val hexSideLength: Int = 16,
               val layers: Map<String, Layer> = HashMap(),
               val tileSets: List<TileSetRef> = ArrayList()) {

    val terrain = HashMap<OddQ, Hex>()
    val worldToHex = HashMap<PixI, Hex>()
    val oddQToPixF = HashMap<OddQ, PixF>()

    init {
        val gidTileMap = HashMap<Int, Tile>()
        // read tileSets
        tileSets.forEach {
            gidTileMap.putAll(TileSetReader.readTileSet(it.firstGid, it.source))
        }

        // init terain
        layers["terrain"]?.let {
            it.data.forEachIndexed { idx: Int, gid: Int ->
                val oddQ = OddQ(idx % it.width, idx / it.width)
                val odd = oddQ.col % 2 == 0
                val pix = PixF((oddQ.col * (tileWidth - hexSideLength / 2)).toFloat(), -(oddQ.row * tileHeight + if (!odd) tileHeight / 2 else 0).toFloat())
                oddQToPixF[oddQ] = pix
                gidTileMap[gid]?.let { tile ->
                    val hex = Hex(oddQ, pix, tile)
                    terrain[oddQ] = hex

                    // mapping world coordinates to hex
                    hex.myPixels(hexSideLength, tileWidth, tileHeight).forEach {
                        worldToHex[it] = hex
                    }
                }
            }
        }
    }
}

data class OddQ(val col: Int, val row: Int) {
    companion object {
        private val directions = arrayOf(
                arrayOf(PixI(+1, 0), PixI(+1, -1), PixI(0, -1), PixI(-1, -1), PixI(-1, 0), PixI(0, +1)),
                arrayOf(PixI(+1, +1), PixI(+1, 0), PixI(0, -1), PixI(-1, 0), PixI(-1, +1), PixI(0, +1))
        )
    }

    fun neighbor(direction: Int): OddQ {
        var parity = col and 1
        var dir = directions[parity][direction]
        return OddQ(col + dir.x, row + dir.y)
    }

    fun possibleMovement(terrain: Map<OddQ, Hex>, range: Int): Set<OddQ> {
        val visited = HashSet<OddQ>()
        visited.add(this)

        var fringes = ArrayList<ArrayList<OddQ>>() // array of arrays of hexes
        fringes.add(arrayListOf(this))

        for (k in 1..range) {
            fringes.add(ArrayList())
            fringes[k - 1].forEach {
                for (dir in 0..5) {
                    var neighbor = it.neighbor(dir)
                    var hex = terrain[neighbor]
                    if (hex != null) {
                        if (!visited.contains(neighbor)) { // and not blocked:
                            visited.add(neighbor)
                            fringes[k].add(neighbor)
                        }
                    }
                }
            }
        }
        return visited
    }

    fun path(terrain: Map<OddQ, Hex>, to: OddQ): List<OddQ> {
        val frontier: PriorityQueue<Pair<Int, OddQ>> = PriorityQueue(kotlin.Comparator { o1, o2 -> o1.first.compareTo(o2.first) })
        frontier.add(Pair(0, this))
        val cameFrom = HashMap<OddQ, OddQ?>()
        cameFrom[this] = null
        val costSoFar = HashMap<OddQ, Int>()
        costSoFar[this] = 0

        while (!frontier.isEmpty()) {
            val current = frontier.poll().second
            if (current == to) break
            for (dir in 0..5) {
                var neighbor = current.neighbor(dir)
                var hex = terrain[neighbor]
                if (hex != null) { // and is accessible
                    val newCost = costSoFar[current]!! + hex.cost
                    if (!costSoFar.containsKey(neighbor) || newCost < costSoFar[neighbor]!!) {
                        costSoFar[neighbor] = newCost
                        frontier.offer(Pair(newCost + heuristic(to, neighbor), neighbor))
                        cameFrom[neighbor] = current
                    }
                }
            }
        }

        // construct path
        var a: OddQ? = to
        val path = LinkedList<OddQ>()
        path.add(a!!)
        while (cameFrom[a] != null) {
            path.addFirst(cameFrom[a])
            a = cameFrom[a]
        }
        return path
    }

    fun heuristic(a: OddQ, b: OddQ): Int {
        return Math.abs(a.col - b.col) + Math.abs(a.row - b.row)
    }

    fun toCube(): Cube {
        var x = col
        var z = row - (col - (col and 1)) / 2
        var y = -x - z

        return Cube(x, y, z)
    }
}

data class Cube(val x: Int, val y: Int, val z: Int) {

    fun toOddQ(): OddQ {
        var col = x
        var row = z + (x - (x and 1)) / 2
        return OddQ(col, row)
    }
}

class Hex(val oddQ: OddQ, val pixF: PixF, val tile: Tile) {

    var pixels = ArrayList<PixI>()
    val cost: Int = 1

    fun myPixels(topEdgeLen: Int, width: Int, height: Int): List<PixI> {
        val hexPixels = ArrayList<PixI>()

        val steps = height / 2
        var startx = (width - topEdgeLen).toFloat() / 2.0f
        val inc = startx / steps.toFloat()
        for (y in 0 until steps) {
            val len = topEdgeLen + (2.0f * inc * y.toFloat()).toInt()
            startx -= inc
            for (x in 0 until len) {
                hexPixels.add(PixI(pixF.x.toInt() + x + startx.toInt(), pixF.y.toInt() + y))
            }
        }

        startx = 0f
        for (y in steps until height) {
            val len = width - (2.0f * inc * (y - steps).toFloat()).toInt()
            startx += inc
            for (x in 0 until len) {
                hexPixels.add(PixI(pixF.x.toInt() + x + startx.toInt(), pixF.y.toInt() + y))
            }
        }

        pixels = hexPixels
        return hexPixels
    }
}

data class PixF(val x: Float, val y: Float)

data class PixI(val x: Int, val y: Int)

object TileSetReader {

    fun readTileSet(initGid: Int, textureAsset: String): Map<Int, Tile> {
        val tileMap = HashMap<Int, Tile>()

        val json = JsonReader()
        val base = json.parse(Gdx.files.internal(textureAsset))

        val imageAsset = base.getString("image")
        val cols = base.getInt("columns")
        val tileWidth = base.getInt("tilewidth")
        val tileHeight = base.getInt("tileheight")

        val tiles = parseTiles(base.get("tiles").asIterable())

        val texture = Texture(Gdx.files.internal(imageAsset))
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val temp = TextureRegion.split(texture, tileWidth, tileHeight)

        temp.forEachIndexed { row, arrayOfTextureRegions ->
            arrayOfTextureRegions.forEachIndexed { col, textureRegion ->
                val gid = row * cols + col
                tileMap[gid + initGid] = Tile(textureRegion, tileWidth, tileHeight, tiles.getOrDefault(gid, TileType("none")))
            }
        }

        return tileMap
    }

    private fun parseTiles(tilesIt: Iterable<JsonValue>): Map<Int, TileType> {
        val tileTypeMap = HashMap<Int, TileType>()
        tilesIt.forEach { tileTypeMap[it.getInt("id")] = TileType(it.getString("type"), parseTileProperties(it.get("properties"))) }
        return tileTypeMap
    }

    private fun parseTileProperties(propertiesJson: JsonValue?): List<TileProperty> {
        val properties = ArrayList<TileProperty>()
        propertiesJson?.let {
            it.asIterable().forEach {
                properties.add(TileProperty(it.getString("name"), it.getString("type"), it.getString("value")))
            }
        }

        return properties
    }
}

class TileType(val type: String, properties: List<TileProperty> = ArrayList())

class TileProperty(val name: String, val type: String, val value: String)

class Tile(val texture: TextureRegion, val textureWidth: Int, val textureHeight: Int, val type: TileType)
