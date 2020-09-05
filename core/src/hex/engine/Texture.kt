package hex.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

object TextureHelper {
    fun loadTexture(path: String) = TextureRegion(Texture(Gdx.files.internal(path)))
}