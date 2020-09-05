package hex.engine

import com.badlogic.gdx.*
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import hex.engine.map.LevelMap
import hex.engine.map.MapReader
import hex.engine.map.PixI


open class BaseScreen : Screen, InputProcessor {
    val mainStage = Stage()
    val uiStage = Stage()

    init {
//        val camera = OrthographicCamera(Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
//        camera.setToOrtho(true, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
//        mainStage.viewport.camera = camera
    }

    override fun hide() {
        val im = Gdx.input.inputProcessor as InputMultiplexer
        im.removeProcessor(this)
        im.removeProcessor(uiStage)
        im.removeProcessor(mainStage)
    }

    override fun show() {
        val im = Gdx.input.inputProcessor as InputMultiplexer
        im.addProcessor(this)
        im.addProcessor(uiStage)
        im.addProcessor(mainStage)
    }

    override fun render(dt: Float) {
        mainStage.act(dt);
        uiStage.act(dt);

        // clear the screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // draw the graphics
        mainStage.draw();
        uiStage.draw()
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun dispose() {
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }
}

class WorldScreen(worldMapAsset: String) : BaseScreen() {

    val levelMap: LevelMap
    val highlight: WhitePx

    var lmbPressed: Vector2? = null
    var rmbPressed: Vector2? = null

    init {
        levelMap = MapReader.readMap(worldMapAsset)
        levelMap.terrain.forEach { oddQ, hex -> TileActor(hex.tile.texture, hex.pixF.x, hex.pixF.y, hex.tile.textureWidth.toFloat(), hex.tile.textureHeight.toFloat(), this.mainStage) }
        highlight = WhitePx(mainStage)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Buttons.LEFT) {
            lmbPressed = Vector2(screenX.toFloat(), screenY.toFloat())
        }
        if (button == Buttons.RIGHT) {
            rmbPressed = Vector2(screenX.toFloat(), screenY.toFloat())
        }
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        lmbPressed = null
        rmbPressed = null
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        rmbPressed?.let {
            val moveVector = Vector2((screenX.toFloat() - it.x) / 4.0f, (screenY.toFloat() - it.y) / 4.0f)
            rmbPressed = Vector2(screenX.toFloat(), screenY.toFloat())

            mainStage.camera.position.x = mainStage.camera.position.x - moveVector.x
            mainStage.camera.position.y = mainStage.camera.position.y + moveVector.y

            mainStage.camera.update()
        }

        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        val vec = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        this.mainStage.camera.unproject(vec)

        val worldPx = PixI(vec.x.toInt(), vec.y.toInt())
        levelMap.worldToHex[worldPx]?.let {
            // hex
            highlight.x = it.pixF.x + (levelMap.tileWidth.toFloat() / 2f)
            highlight.y = it.pixF.y + (levelMap.tileHeight.toFloat() / 2f)
        }

        return super.mouseMoved(screenX, screenY)
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.ESCAPE -> Gdx.app.exit()
        }

        return super.keyDown(keycode)
    }
}


class TileActor : Actor {

    constructor(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, s: Stage) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        this.texture = texture
        s.addActor(this)
    }

    var texture: TextureRegion

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(texture,
                x, y, originX, originY,
                width, height, scaleX, scaleY, rotation);
        super.draw(batch, parentAlpha)
    }
}

class WhitePx(s: Stage) : Actor() {
    val texture: TextureRegion

    init {
        this.x = 0f
        this.y = 0f
        texture = TextureHelper.loadTexture("white-px.png")
        width = 1f
        height = 1f
        s.addActor(this)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(texture,
                x, y, originX, originY,
                width, height, scaleX, scaleY, rotation);
        super.draw(batch, parentAlpha)
    }
}

