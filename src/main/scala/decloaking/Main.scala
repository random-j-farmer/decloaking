package decloaking

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.CanvasRenderingContext2D

import scala.annotation.tailrec
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object Main {

  val velocity = input(id := "dcl-velocity", `type` := "number", value := "4500").render
  val distance = input(id := "dcl-distance", `type` := "number", value := "15000").render
  val angle = input(id := "dcl-angle", `type` := "number", step := "any", value := "0").render
  val attackerRadius = input(id := "dcl-attacker-radius", `type` := "number", value := "30").render
  val victimRadius = input(id := "dcl-victim-radius", `type` := "number", value := "350").render

  var params = newParams()

  var mouseDown = false

  val mainForm = form(cls := "pure-form pure-form-stacked",
    onsubmit := onFormSubmit _,
    fieldset(
      legend("Decloaking parameters"),
      div(cls := "pure-control-group",
        label(`for` := "dcl-velocity", "Velocity"),
        velocity
      ),
      div(cls := "pure-control-group",
        label(`for` := "dcl-distance", "Distance"),
        distance
      ),
      div(cls := "pure-control-group",
        label(`for` := "dcl-angle", "Angle"),
        angle
      ),
      div(cls := "pure-control-group",
        label(`for` := "dcl-attacker-radius", "Decloaker Radius"),
        attackerRadius
      ),
      div(cls := "pure-control-group",
        label(`for` := "dcl-victim-radius", "Victim Radius"),
        victimRadius
      ),
      div(cls := "pure-controls",
        button(cls := "pure-button pure-button-primary", `type` := "submit", "Render")
      )
    )
  ).render

  val mainCanvas = canvas(style := "margin: 10px;",
    onmousedown := onMouseDown _,
    onmousemove := onMouseMove _,
    onmouseup := onMouseUp _
  ).render

  @JSExport
  def main(mainDiv: html.Div): Unit = {
    // println(s"Main.main: $mainDiv")
    mainDiv.innerHTML = ""
    mainDiv.appendChild(div(cls := "pure-g",
      div(cls := "pure-u-1-4", mainForm),
      div(cls := "pure-u-3-4", mainCanvas)).render)
    // println(s"canvans w: ${mainCanvas.width}, h: ${mainCanvas.height}")
    drawCanvas()
    dom.window.onresize = onResize _
  }

  def onFormSubmit(ev: dom.Event): Unit = {
    ev.preventDefault()
    ev.stopPropagation()
    params = newParams()
    // println("set new params", params)
    drawCanvas()
  }

  def onMouseDown(ev: dom.Event): Unit = {
    mouseDown = true
  }

  def onMouseUp(ev: dom.Event): Unit = {
    mouseDown = false
  }

  def onMouseMove(ev: dom.MouseEvent): Unit = {
    if (mouseDown) {
      ev.preventDefault()
      ev.stopPropagation()
      // println(s"onMouseMove: x=${ev.clientX} y=${ev.clientY}")
      val x = ev.clientX - mainCanvas.offsetLeft
      val y = ev.clientY - mainCanvas.offsetTop - mainCanvas.height / 2d
      val rad = -math.atan(y / math.max(1e-6, x))
      angle.value = math.toDegrees(rad).toString
      params = newParams()
      drawCanvas()
    }
  }

  def onResize(ev: dom.UIEvent) = {
    resize()
    drawCanvas()
  }

  def newParams(): Params = {
    Params(velocity.value.toDouble, distance.value.toDouble,
      math.toRadians(angle.value.toDouble),
      attackerRadius.value.toDouble, victimRadius.value.toDouble)
  }

  def resize(): Unit = {
    // canvas height has to be set in javascript
    val parentDiv = mainCanvas.parentElement.asInstanceOf[html.Div]
    parentDiv.style.height = parentDiv.parentElement.clientHeight.toString + "px"
    mainCanvas.height = parentDiv.clientHeight - 20
    mainCanvas.width = parentDiv.clientWidth - 20
  }

  def drawCanvas(): Unit = {
    resize()

    val ctx = mainCanvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
    val w = mainCanvas.width.toDouble
    val h = mainCanvas.height.toDouble

    val r = new Renderer(ctx, params, w, h)
    r.render()
  }

}

class Renderer(ctx: CanvasRenderingContext2D, p: Params, w: Double, h: Double) {

  val totalMeters = 2d * p.attackerRadius + p.distance + 2d * (2000d + p.victimRadius)

  val onePixel = totalMeters / w


  val probability: Int = {
    val sin = math.sin(p.angle)
    val cos = math.cos(p.angle)

    val xVictim = p.distance + 2000d + p.victimRadius
    val decloakDistance = p.attackerRadius + 2000d + p.victimRadius
    val decloakSquared = decloakDistance * decloakDistance

    val start = p.distance - p.attackerRadius
    val end = p.distance + 2d*(2000d + p.victimRadius)
    val delta = p.velocity / 100d

    @tailrec
    def isDecloak(pos: Double, endPos: Double): Boolean = {
      if (pos < endPos) {
        val x = (cos * pos) - xVictim
        val y = sin * pos
        if (x*x + y*y < decloakSquared) {
          true
        } else {
          isDecloak(pos + delta, endPos)
        }
      } else {
        false
      }
    }

    val decloaks = for {
      i <- 0 until 100
      startPos = start + i*delta
      endPos = end + i*delta
    } yield isDecloak(startPos, endPos)

    println("decloaks", decloaks)

    (decloaks.count(x => x) * 100d/ decloaks.size).toInt
  }

  def victimColor: String = {
    if (probability > 50) {
      // p=100 green (00FF00) to 50 yellow (808000)
      val green = probability * 255 / 100
      val red = (100 - probability) * 255 / 100
      f"#$red%02x$green%02x00"
    } else {
      // p=50 yellow (808000) to p=0 red (FF0000)
      val green = probability * 255 / 100
      val red = (100 - probability) * 255 / 100
      f"#$red%02x$green%02x00"
    }
  }

  def render(): Unit = {

    ctx.save()

    // cartesian coordinates, origin at left/half-height == center of attacker
    ctx.translate(0, h / 2d)
    val scaleFac = w / totalMeters
    // println("scale factor", scaleFac)
    ctx.scale(scaleFac, -scaleFac)
    ctx.translate(p.attackerRadius, 0d)

    ctx.save()
    try {
      ctx.translate(p.distance + p.victimRadius + 2000d, 0d)
      renderVictim()
    } finally {
      ctx.restore()
    }

    ctx.rotate(p.angle)
    renderDistance()
    renderShip("black", p.attackerRadius)

    for (x <- 0d until(p.distance + 2d * (2000d + p.victimRadius), p.velocity)) {
      ctx.translate(x, 0d)
      renderShip("black", p.attackerRadius)
      ctx.translate(-x, 0d)
    }

    ctx.restore()

  }

  def renderShip(style: String, r: Double): Unit = {
    ctx.fillStyle = style
    ctx.beginPath()
    ctx.arc(0d, 0d, math.max(r, onePixel), 0d, 2 * math.Pi)
    ctx.closePath()
    ctx.fill()
  }

  def renderVictim(): Unit = {
    // println("renderVictim", probability, victimColor)
    ctx.fillStyle = victimColor

    ctx.beginPath()
    ctx.arc(0d, 0d, math.max(p.victimRadius + 2000d, onePixel), 0d, 2 * Math.PI)
    ctx.closePath()
    ctx.fill()

    renderShip("yellow", p.victimRadius)

    // save restore not necessary, is restored anyway after renderVictim
    ctx.scale(onePixel, -onePixel)
    val probText = s"$probability%"
    val m = ctx.measureText(probText)
    ctx.font = "16px serif"
    ctx.fillStyle = "black"
    ctx.fillText(probText, -m.width/2d, 8)
  }

  def renderDistance(): Unit = {
    ctx.strokeStyle = "yellow"
    ctx.lineWidth = onePixel
    ctx.beginPath()
    ctx.moveTo(0d, 0d)
    ctx.lineTo(p.distance, 0d)
    ctx.closePath()
    ctx.stroke()
  }

}


case class Params(velocity: Double, distance: Double, angle: Double, attackerRadius: Double, victimRadius: Double)
