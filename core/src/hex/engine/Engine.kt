package hex.engine

import com.badlogic.gdx.*
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage

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

    val controller: Controller

    var lmbPressed: PixI? = null
    var rmbPressed: PixI? = null

    init {
        controller = Controller(this, worldMapAsset)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Buttons.LEFT) {
            lmbPressed = PixI(screenX, screenY)
        }
        if (button == Buttons.RIGHT) {
            rmbPressed = PixI(screenX, screenY)
        }
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        lmbPressed = null
        rmbPressed = null

        val worldPx = screenToWorld(screenX, screenY)
        controller.mouseClick(worldPx, button == Buttons.LEFT)
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (lmbPressed != null) {
            val moveVector = PixI((screenX - lmbPressed!!.x), (screenY - lmbPressed!!.y))
            lmbPressed = PixI(screenX, screenY)
            controller.mouseDragged(moveVector, true)
        } else if (rmbPressed != null) {
            val moveVector = PixI((screenX - rmbPressed!!.x), (screenY - rmbPressed!!.y))
            rmbPressed = PixI(screenX, screenY)
            controller.mouseDragged(moveVector, false)
        }

        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        controller.mouseOver(screenToWorld(screenX, screenY))
        return super.mouseMoved(screenX, screenY)
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.ESCAPE -> Gdx.app.exit()
        }

        return super.keyDown(keycode)
    }

    private fun screenToWorld(screenX: Int, screenY: Int): PixI {
        val vec = this.mainStage.camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        return PixI(vec.x.toInt(), vec.y.toInt())
    }
}

abstract class BaseActor(val texture: TextureRegion, s: Stage) : Actor() {
    init {
        this.x = 0f
        this.y = 0f
        s.addActor(this)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.draw(texture,
                x, y, originX, originY,
                width, height, scaleX, scaleY, rotation);
        super.draw(batch, parentAlpha)
    }
}

class TileActor(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, s: Stage) : BaseActor(texture, s) {
    init {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}

class Highlight(s: Stage) : BaseActor(TextureHelper.loadTexture("highlight.png"), s) {
    var hex: Hex? = null
    init {
        width = 32f
        height = 30f
        isVisible = false
    }
}

class UnitHighlight(s: Stage) : BaseActor(TextureHelper.loadTexture("unit-highlight.png"), s) {
    var unit: Unit? = null
    init {
        width = 32f
        height = 30f
        isVisible = false
    }
}

class Unit(val map: LevelMap, position: OddQ, s: Stage) : BaseActor(TextureHelper.loadTexture("unit.png"), s) {
    var oddQ: OddQ = position
        set(newPosition) {
            field = newPosition
            x = map.oddQToPixF[newPosition]!!.x
            y = map.oddQToPixF[newPosition]!!.y
        }

    init {
        width = 32f
        height = 30f
        x = map.oddQToPixF[position]!!.x
        y = map.oddQToPixF[position]!!.y
    }
}

class Controller(val baseScreen: BaseScreen, worldMapAsset: String) {

    val levelMap: LevelMap
    val highlight: Highlight
    val unitHighlight: UnitHighlight
    val units = ArrayList<Unit>()

    init {
        levelMap = MapReader.readMap(worldMapAsset)
        levelMap.terrain.forEach { (oddQ, hex) -> TileActor(hex.tile.texture, hex.pixF.x, hex.pixF.y, hex.tile.textureWidth.toFloat(), hex.tile.textureHeight.toFloat(), baseScreen.mainStage) }
        highlight = Highlight(baseScreen.mainStage)
        unitHighlight = UnitHighlight(baseScreen.mainStage)
        units.add(Unit(this.levelMap, OddQ(1, 1), baseScreen.mainStage))
    }

    fun mouseOver(worldPx: PixI) {
        this.highlight.isVisible = false
        highlight.hex = null
        levelMap.worldToHex[worldPx]?.let {
            highlight.hex = it
            highlight.x = it.pixF.x
            highlight.y = it.pixF.y
            this.highlight.isVisible = true
        }
    }

    fun mouseClick(worldPx: PixI, lmb: Boolean) {
        levelMap.worldToHex[worldPx]?.let { hex ->
            // get unit
            val unit = units.firstOrNull { it.oddQ == hex.oddQ }
            if (unit != null) {
                unitHighlight.unit = unit
                unitHighlight.x = levelMap.oddQToPixF[unit.oddQ]!!.x
                unitHighlight.y = levelMap.oddQToPixF[unit.oddQ]!!.y
                unitHighlight.isVisible = true
            } else {
                unitHighlight.isVisible = false
                unitHighlight.unit = null
            }
        }
    }

    fun mouseDragged(moveVector: PixI, lmb: Boolean) {
        if (!lmb) { // rmb
            baseScreen.mainStage.camera.position.x = baseScreen.mainStage.camera.position.x - moveVector.x
            baseScreen.mainStage.camera.position.y = baseScreen.mainStage.camera.position.y + moveVector.y

            baseScreen.mainStage.camera.update()
        }
    }
}