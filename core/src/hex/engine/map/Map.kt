package hex.engine.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

object MapReader {

    fun readMap(mapAsset: String): LevelMap {
        val json = JsonReader()
        val base = json.parse(Gdx.files.internal(mapAsset))

        val tileHeight = base.getInt("tileheight", 32)
        val hexSideLength = base.getInt("hexsidelength", 16)
        val tileSets = base.get("tilesets").asIterable().map { TileSetRef(it.getLong("firstgid"), it.getString("source")) }
        val layers = parseTileLayers(base.get("layers").asIterable())

        return LevelMap(tileHeight, hexSideLength, layers, tileSets)
    }

    private fun parseTileLayers(jsonLayers: Iterable<JsonValue>): List<Layer> {
        val layers = ArrayList<Layer>()
        jsonLayers.filter { layer -> layer.getString("type") == "tilelayer" }
                .forEach { layer ->
                    val data = layer.get("data").asIntArray()
                    val width = layer.getInt("width")
                    val height = layer.getInt("height")

                    layers.add(Layer(data, width, height))
                }
        return layers
    }
}

class LevelMap(val tileHeight: Int = 32,
               val hexSideLength: Int = 16,
               val layers: List<Layer> = ArrayList(),
               val tileSets: List<TileSetRef> = ArrayList()) {

    // read tile-sets

    // build map
    // * choose coordinate type

}

class Layer(val data: IntArray, val width: Int, val height: Int)

class TileSetRef(val firstGid: Long, val source: String)


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
                tileMap[gid + initGid] = Tile(textureRegion, tiles.getOrDefault(gid, TileType("none")))
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

class Tile(val textureRegion: TextureRegion, val type: TileType)
