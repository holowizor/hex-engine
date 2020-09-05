package hex.engine

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer

object HexPreviewGame : Game() {

    // BAD DESIGN
    private var worldScreen: WorldScreen? = null

    override fun create() {
        Gdx.input.inputProcessor = InputMultiplexer()

        // BAD DESIGN
        worldScreen = WorldScreen("hex-sample.json")

        world()
    }

    fun world() {
        screen = HexPreviewGame.worldScreen
        screen.show()
    }

    override fun dispose() {
        screen.dispose()
    }
}