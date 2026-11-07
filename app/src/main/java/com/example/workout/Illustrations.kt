package com.example.workout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple stick-figure demos. Each move has two poses that the figure moves between.
 * Angles are in degrees: 0 points down, 90 forward (right), 180 up. The hip is the origin and the
 * ground is y = 0, with y growing downward. Generated from a pose script, so the numbers are not meant
 * to be edited by hand.
 */
class FigPose(
    val view: Int, val torso: Float, val head: Float, val hipX: Float, val hipY: Float,
    val arms: FloatArray, val legs: FloatArray, val feet: FloatArray
)

class Fig(
    val a: FigPose, val b: FigPose, val prop: Int,
    val x0: Float, val x1: Float, val y0: Float, val y1: Float
)

fun hasFigure(name: String): Boolean = Figures.containsKey(name)

private const val TORSO = 1.0f
private const val HEAD_R = 0.2f
private const val UA = 0.55f
private const val FA = 0.5f
private const val TH = 0.85f
private const val SH = 0.85f
private const val FOOT = 0.3f
private const val SW = 0.28f
private const val HW = 0.15f

private fun dirX(a: Float) = sin(a * PI.toFloat() / 180f)
private fun dirY(a: Float) = cos(a * PI.toFloat() / 180f)

/** Moves between two angles the short way round. */
private fun mixAngle(a: Float, b: Float, t: Float): Float {
    var diff = (b - a) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return a + diff * t
}

private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

private class Pts(val map: Map<String, Offset>) {
    operator fun get(k: String) = map.getValue(k)
}

private fun joints(p: FigPose, t: Float, q: FigPose): Pts {
    val torso = mixAngle(p.torso, q.torso, t)
    val head = mixAngle(p.head, q.head, t)
    val hip = Offset(mix(p.hipX, q.hipX, t), mix(p.hipY, q.hipY, t))
    val neck = Offset(hip.x + dirX(torso) * TORSO, hip.y + dirY(torso) * TORSO)
    val m = HashMap<String, Offset>()
    m["H"] = hip
    m["N"] = neck
    val hd = Offset(neck.x + dirX(torso + head) * (HEAD_R + 0.06f), neck.y + dirY(torso + head) * (HEAD_R + 0.06f))
    m["head"] = hd
    val front = p.view == 1
    for (i in 0..1) {
        val sgn = if (i == 0) 1f else -1f
        val flip = if (front && i == 1) -1f else 1f
        val so = if (front) sgn * SW else 0f
        val ho = if (front) sgn * HW else 0f
        val s = Offset(neck.x + so, neck.y)
        val ua = mixAngle(p.arms[i * 2], q.arms[i * 2], t)
        val fa = mixAngle(p.arms[i * 2 + 1], q.arms[i * 2 + 1], t)
        val e = Offset(s.x + flip * dirX(ua) * UA, s.y + dirY(ua) * UA)
        val w = Offset(e.x + flip * dirX(fa) * FA, e.y + dirY(fa) * FA)
        val hp = Offset(hip.x + ho, hip.y)
        val th = mixAngle(p.legs[i * 2], q.legs[i * 2], t)
        val sh = mixAngle(p.legs[i * 2 + 1], q.legs[i * 2 + 1], t)
        val ft = mixAngle(p.feet[i], q.feet[i], t)
        val k = Offset(hp.x + flip * dirX(th) * TH, hp.y + dirY(th) * TH)
        val an = Offset(k.x + flip * dirX(sh) * SH, k.y + dirY(sh) * SH)
        val to = Offset(an.x + flip * dirX(ft) * FOOT, an.y + dirY(ft) * FOOT)
        m["S$i"] = s; m["E$i"] = e; m["W$i"] = w
        m["HP$i"] = hp; m["K$i"] = k; m["A$i"] = an; m["T$i"] = to
    }
    return Pts(m)
}

/** Looping demo of one move. Draws nothing when the move has no figure. */
@Composable
fun ExerciseFigure(name: String, modifier: Modifier = Modifier) {
    val fig = Figures[name] ?: return
    val transition = rememberInfiniteTransition(label = "figure")
    val raw by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "progress"
    )
    // Ease the ends so the figure pauses briefly at each pose.
    val t = raw * raw * (3f - 2f * raw)
    val near = MaterialTheme.colorScheme.onBackground
    val far = near.copy(alpha = 0.45f)
    val prop = Color(0xFFA06020)
    val w = fig.x1 - fig.x0
    val h = fig.y1 - fig.y0
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val scale = minOf(size.width / w, size.height / h)
        val ox = (size.width - w * scale) / 2f - fig.x0 * scale
        val oy = (size.height - h * scale) / 2f - fig.y0 * scale
        fun pt(o: Offset) = Offset(ox + o.x * scale, oy + o.y * scale)
        val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        val line = 2.6.dp.toPx()

        drawLine(Color.Gray.copy(alpha = 0.5f), pt(Offset(fig.x0, 0f)), pt(Offset(fig.x1, 0f)), 1.dp.toPx())
        val base = joints(fig.a, 0f, fig.a)
        when (fig.prop) {
            2 -> {
                val a = base["T0"]
                drawLine(prop, pt(Offset(a.x - 0.5f, a.y)), pt(Offset(a.x + 0.3f, a.y)), line, StrokeCap.Round)
                drawLine(prop, pt(Offset(a.x - 0.45f, a.y)), pt(Offset(a.x - 0.45f, 0f)), line, StrokeCap.Round)
            }
            3 -> {
                val hp = base["H"]
                drawLine(prop, pt(Offset(hp.x - 0.45f, hp.y)), pt(Offset(hp.x + 0.25f, hp.y)), line, StrokeCap.Round)
                drawLine(prop, pt(Offset(hp.x - 0.4f, hp.y)), pt(Offset(hp.x - 0.4f, 0f)), line, StrokeCap.Round)
            }
            4 -> {
                val hp = base["H"]
                drawLine(prop, pt(Offset(hp.x - 0.2f, 0f)), pt(Offset(hp.x - 0.2f, hp.y - 1.0f)), line, StrokeCap.Round)
            }
        }
        val j = joints(fig.a, t, fig.b)
        fun seg(a: String, b: String, c: Color) = drawLine(c, pt(j[a]), pt(j[b]), line, StrokeCap.Round)
        // Far limbs first so the near ones sit on top.
        for (i in listOf(1, 0)) {
            val c = if (i == 1) far else near
            seg("S$i", "E$i", c); seg("E$i", "W$i", c)
            seg("HP$i", "K$i", c); seg("K$i", "A$i", c); seg("A$i", "T$i", c)
        }
        seg("H", "N", near)
        if (fig.a.view == 1) {
            seg("S0", "S1", near)
            seg("HP0", "HP1", near)
        }
        drawCircle(near, HEAD_R * scale, pt(j["head"]), style = stroke)
    }
}

private val Figures: Map<String, Fig> = mapOf(
    "Standard Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Military Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Wide Fly Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Diamond Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Slow-Motion 3-in-1 Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Two-Twitch Speed Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Side-to-Side Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Decline Push-Ups" to Fig(FigPose(0,90.593f,0f,0f,-0.99f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(272.558f,268.627f,272.541f,268.61f),floatArrayOf(55f,55f)),
        FigPose(0,75.587f,0f,0f,-0.549f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(257.552f,253.621f,257.96f,254.029f),floatArrayOf(55f,55f)),
        2,-2.149f,1.71f,-1.453f,0.08f),
    "Plange Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(328.805f,333.812f,328.805f,333.812f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(250.622f,344.792f,250.622f,344.792f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.529f,0.08f),
    "Clap or Plyo Push-Ups" to Fig(FigPose(0,92.716f,0f,0f,-0.253f,floatArrayOf(277.567f,62.926f,277.567f,62.926f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        FigPose(0,115.866f,0f,0f,-0.913f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(297.831f,293.901f,297.115f,293.185f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.913f,0.08f),
    "One-Arm Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,320.156f,342.828f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.853f,floatArrayOf(277.567f,62.926f,320.156f,342.828f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "One-Arm Balance Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,320.156f,342.828f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,92.716f,0f,0f,-0.853f,floatArrayOf(277.567f,62.926f,320.156f,342.828f),floatArrayOf(274.681f,270.75f,274.603f,270.673f),floatArrayOf(55f,55f)),
        0,-2.147f,1.709f,-1.53f,0.08f),
    "Scapular Push-Ups" to Fig(FigPose(0,107.857f,0f,0f,-0.693f,floatArrayOf(343.101f,18.648f,343.101f,18.648f),floatArrayOf(289.822f,285.892f,289.32f,285.389f),floatArrayOf(55f,55f)),
        FigPose(0,103.994f,0f,0f,-0.583f,floatArrayOf(322.182f,38.644f,322.182f,38.644f),floatArrayOf(285.96f,282.029f,285.563f,281.633f),floatArrayOf(55f,55f)),
        0,-2.101f,1.673f,-1.53f,0.08f),
    "Half-Squat Pulses" to Fig(FigPose(0,150f,0f,0f,-1.3f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(55.073f,343.114f,49.742f,332.029f),floatArrayOf(90f,90f)),
        FigPose(0,150f,0f,0f,-1.15f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(64.784f,337.957f,58.455f,326.075f),floatArrayOf(90f,90f)),
        0,-0.45f,1.9f,-2.841f,0.08f),
    "Alternating Shoulder Presses" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(29.413f,176.572f,160.452f,195.549f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(160.452f,195.549f,29.413f,176.572f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.53f,0.83f,-4.13f,0.08f),
    "Deep Swimmer's Presses" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(342.108f,110.292f,335.669f,107.596f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(160.452f,195.549f,164.281f,199.756f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.677f,0.83f,-4.13f,0.08f),
    "Upright Rows" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.175f,32.681f,341.27f,30.879f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(100.915f,320.201f,96.293f,316.102f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.627f,0.997f,-3.39f,0.08f),
    "Seated Two-Angle Shoulder Flys" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(351.208f,24.087f,22.477f,349.598f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(70.452f,105.549f,103.823f,68.726f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.73f,1.73f,-3.39f,0.08f),
    "In & Out Straight-Arm Shoulder Flys" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(351.208f,24.087f,22.477f,349.598f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(70.452f,105.549f,103.823f,68.726f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.73f,1.73f,-3.39f,0.08f),
    "In & Out Shoulder Flys" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(351.208f,24.087f,22.477f,349.598f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(70.452f,105.549f,103.823f,68.726f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.73f,1.73f,-3.39f,0.08f),
    "Scarecrows" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(90f,180f,90f,180f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(90f,10f,90f,10f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.367f,1.367f,-3.63f,0.08f),
    "Y-Presses" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(95f,175f,95f,175f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(155f,155f,155f,155f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.321f,1.321f,-4.082f,0.08f),
    "Weighted Circles" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(70f,70f,70f,70f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(115f,115f,115f,115f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.717f,1.717f,-3.574f,0.08f),
    "Pour Flys" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(90f,90f,90f,90f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(70f,40f,70f,40f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.78f,1.78f,-3.39f,0.08f),
    "Dumbbell Cross-Body Blows" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(342.155f,123.167f,334.772f,119.675f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(68.259f,101.973f,334.772f,119.675f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.684f,1.45f,-3.39f,0.08f),
    "In & Out Bicep Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Standing Bicep Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Hammer Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Congdon Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Twenty-Ones" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "In-Out Hammer Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "One-Arm Cross-Body Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "One-Arm Corkscrew Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Curl-Up/Hammer Downs" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Strip-Set Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.625f,0.87f,-3.39f,0.08f),
    "Congdon Locomotives" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(26.993f,160.079f,344.896f,20.281f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,18.09f,157.763f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.593f,0.87f,-3.39f,0.08f),
    "Static Arm Curls" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(54.949f,91.819f,344.896f,20.281f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(54.949f,91.819f,24.34f,157.254f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        0,-0.593f,1.4f,-3.39f,0.08f),
    "Full Supination Concentration Curls" to Fig(FigPose(0,140f,0f,0f,-0.85f,floatArrayOf(336.047f,26.525f,336.047f,26.525f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        FigPose(0,140f,0f,0f,-0.85f,floatArrayOf(353.707f,133.898f,336.047f,26.525f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        3,-0.45f,1.6f,-2.265f,0.08f),
    "One-Arm Concentration Curls" to Fig(FigPose(0,140f,0f,0f,-0.85f,floatArrayOf(336.047f,26.525f,336.047f,26.525f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        FigPose(0,140f,0f,0f,-0.85f,floatArrayOf(353.707f,133.898f,336.047f,26.525f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        3,-0.45f,1.6f,-2.265f,0.08f),
    "Two-Arm Triceps Kickback" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(327.558f,29.347f,324.814f,25.712f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(259.68f,293.394f,263.65f,294.94f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.743f,1.35f,-2.791f,0.08f),
    "Flip-Grip Twist Triceps Kickbacks" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(327.558f,29.347f,324.814f,25.712f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(259.68f,293.394f,263.65f,294.94f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.743f,1.35f,-2.791f,0.08f),
    "Front-to-Back Tricep Extensions" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(327.558f,29.347f,324.814f,25.712f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(259.68f,293.394f,263.65f,294.94f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.743f,1.35f,-2.791f,0.08f),
    "Side Tri-Rises" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(327.558f,29.347f,324.814f,25.712f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(259.68f,293.394f,263.65f,294.94f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.743f,1.35f,-2.791f,0.08f),
    "Wide Bent-Over Rows" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,340.452f,15.549f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,287.772f,48.375f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.35f,-2.791f,0.08f),
    "Switch-Grip Bent-Over Rows" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,340.452f,15.549f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,287.772f,48.375f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.35f,-2.791f,0.08f),
    "Reverse Grip Bent-Over Rows" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,340.452f,15.549f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,287.772f,48.375f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.35f,-2.791f,0.08f),
    "Close-Grip Underhand Rows" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,340.452f,15.549f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,287.772f,48.375f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.35f,-2.791f,0.08f),
    "Max Rep Dumbbell Rows" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,340.452f,15.549f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,287.772f,48.375f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.35f,-2.791f,0.08f),
    "Lawnmowers" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,349.716f,44.137f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,349.716f,44.137f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.407f,-2.791f,0.08f),
    "Elbows-Out Lawnmowers" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,349.716f,44.137f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,349.716f,44.137f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.407f,-2.791f,0.08f),
    "One-Arm Rows" to Fig(FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(348.226f,22.61f,349.716f,44.137f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        FigPose(0,135f,0f,0f,-1.45f,floatArrayOf(292.549f,54.677f,349.716f,44.137f),floatArrayOf(45.099f,359.86f,43.98f,350.503f),floatArrayOf(90f,90f)),
        0,-0.45f,1.407f,-2.791f,0.08f),
    "Seated Bent-Over Back Flys" to Fig(FigPose(0,125f,0f,0f,-0.85f,floatArrayOf(349.68f,23.394f,346.177f,21.274f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        FigPose(0,125f,0f,0f,-0.85f,floatArrayOf(304.418f,11.972f,303.487f,6.743f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        3,-0.45f,1.6f,-2.023f,0.08f),
    "Balanced Lunges" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.2f,floatArrayOf(343.101f,18.648f,340.452f,15.549f),floatArrayOf(76.159f,354.916f,6.131f,278.447f),floatArrayOf(90f,40f)),
        0,-1.2f,1.5f,-3.39f,0.08f),
    "Step Back Lunge" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.2f,floatArrayOf(343.101f,18.648f,340.452f,15.549f),floatArrayOf(76.159f,354.916f,6.131f,278.447f),floatArrayOf(90f,40f)),
        0,-1.2f,1.5f,-3.39f,0.08f),
    "Three-Way Lunge" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.2f,floatArrayOf(343.101f,18.648f,340.452f,15.549f),floatArrayOf(76.159f,354.916f,6.131f,278.447f),floatArrayOf(90f,40f)),
        0,-1.2f,1.5f,-3.39f,0.08f),
    "Sneaky Lunge" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.2f,floatArrayOf(343.101f,18.648f,340.452f,15.549f),floatArrayOf(76.159f,354.916f,6.131f,278.447f),floatArrayOf(90f,40f)),
        0,-1.2f,1.5f,-3.39f,0.08f),
    "Toe-Roll Iso Lunge" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(344.896f,20.281f,341.459f,16.845f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.2f,floatArrayOf(343.101f,18.648f,340.452f,15.549f),floatArrayOf(76.159f,354.916f,6.131f,278.447f),floatArrayOf(90f,40f)),
        0,-1.2f,1.5f,-3.39f,0.08f),
    "Alternating Side Lunge" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(337.495f,38.393f,35.186f,334.288f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.15f,floatArrayOf(337.495f,38.393f,35.186f,334.288f),floatArrayOf(69.332f,0.317f,344.138f,66.982f),floatArrayOf(90f,90f)),
        0,-1.45f,1.7f,-3.39f,0.08f),
    "Reverse Lunges with Reach" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(160.452f,195.549f,164.281f,199.756f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.2f,floatArrayOf(160.452f,195.549f,164.281f,199.756f),floatArrayOf(76.159f,354.916f,6.131f,278.447f),floatArrayOf(90f,40f)),
        0,-1.2f,1.5f,-4.13f,0.08f),
    "Calf-Raise Squats" to Fig(FigPose(0,145f,0f,0f,-1f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(77.65f,344.278f,72.489f,331.114f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.892f,floatArrayOf(54.814f,115.712f,47.128f,116.641f),floatArrayOf(11.883f,356.288f,6.927f,349.663f),floatArrayOf(45f,45f)),
        0,-0.5f,1.974f,-3.602f,0.08f),
    "Slow Bodyweight Squats" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,145f,0f,0f,-1f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(77.65f,344.278f,72.489f,331.114f),floatArrayOf(90f,90f)),
        0,-0.53f,1.974f,-3.39f,0.08f),
    "Chair Salutations" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(349.533f,88.951f,344.22f,87.624f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,145f,0f,0f,-1f,floatArrayOf(349.533f,88.951f,344.22f,87.624f),floatArrayOf(77.65f,344.278f,72.489f,331.114f),floatArrayOf(90f,90f)),
        0,-0.6f,1.424f,-3.39f,0.08f),
    "Wall Squats" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(346.177f,21.274f,340.452f,15.549f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-0.85f,floatArrayOf(21.74f,96.994f,15.917f,93.315f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        4,-0.634f,1.6f,-3.39f,0.08f),
    "Single-Leg Wall Squat" to Fig(FigPose(0,180f,0f,0f,-0.85f,floatArrayOf(21.74f,96.994f,15.917f,93.315f),floatArrayOf(90f,0f,89.901f,356.628f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-0.85f,floatArrayOf(21.74f,96.994f,15.917f,93.315f),floatArrayOf(90f,0f,109.75f,70.25f),floatArrayOf(90f,90f)),
        4,-0.45f,2.35f,-2.56f,0.08f),
    "Deadlift Squats" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(346.177f,21.274f,340.452f,15.549f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,112f,0f,0f,-1.15f,floatArrayOf(349.68f,23.394f,346.177f,21.274f),floatArrayOf(67.823f,347.283f,64.784f,337.957f),floatArrayOf(90f,90f)),
        0,-0.634f,1.618f,-3.39f,0.08f),
    "Super Skaters" to Fig(FigPose(0,150f,0f,0f,-1.45f,floatArrayOf(265.068f,354.026f,27.911f,99.907f),floatArrayOf(33.386f,330.564f,342.889f,269.166f),floatArrayOf(90f,40f)),
        FigPose(0,150f,0f,0f,-1.45f,floatArrayOf(27.911f,99.907f,265.068f,354.026f),floatArrayOf(356.534f,266.733f,39.84f,339.725f),floatArrayOf(40f,90f)),
        0,-1.55f,1.7f,-2.991f,0.08f),
    "Groucho Walk" to Fig(FigPose(0,150f,0f,0f,-0.7f,floatArrayOf(345.39f,42.645f,341.32f,40.716f),floatArrayOf(99.116f,10.9f,69.704f,298.467f),floatArrayOf(90f,90f)),
        FigPose(0,150f,0f,0f,-0.7f,floatArrayOf(345.39f,42.645f,341.32f,40.716f),floatArrayOf(69.704f,298.467f,99.116f,10.9f),floatArrayOf(90f,90f)),
        0,-0.45f,1.75f,-2.241f,0.08f),
    "Calf Raises" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(349.68f,23.394f,338.259f,11.973f),floatArrayOf(11.094f,354.359f,5.641f,348.906f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.892f,floatArrayOf(349.68f,23.394f,338.259f,11.973f),floatArrayOf(11.883f,356.288f,6.927f,349.663f),floatArrayOf(45f,45f)),
        0,-0.654f,0.83f,-3.602f,0.08f),
    "80/20 Siebers-Speed Squats" to Fig(FigPose(0,145f,0f,0f,-1f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(77.65f,344.278f,72.489f,331.114f),floatArrayOf(90f,90f)),
        FigPose(0,145f,0f,0f,-1.1f,floatArrayOf(63.193f,113.334f,54.814f,115.712f),floatArrayOf(71.129f,346.092f,44.97f,297.969f),floatArrayOf(90f,90f)),
        0,-0.6f,1.974f,-2.582f,0.08f),
    "Easy Jumping Jacks" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(353.65f,24.94f,23.412f,352.121f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.58f,floatArrayOf(140.156f,162.828f,161.734f,139.063f),floatArrayOf(26.984f,14.604f,14.604f,26.984f),floatArrayOf(90f,90f)),
        0,-1.5f,1.5f,-3.93f,0.08f),
    "Arm Circles" to Fig(FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(93.573f,115.559f,114.499f,92.513f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        FigPose(1,180f,0f,0f,-1.68f,floatArrayOf(67.474f,79.713f,79.128f,66.889f),floatArrayOf(8.797f,351.203f,351.203f,8.797f),floatArrayOf(90f,90f)),
        0,-1.73f,1.73f,-3.39f,0.08f),
    "Leg Swings" to Fig(FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(57.558f,119.347f,49.784f,120.68f),floatArrayOf(80.131f,11.027f,8.797f,351.203f),floatArrayOf(90f,90f)),
        FigPose(0,180f,0f,0f,-1.68f,floatArrayOf(57.558f,119.347f,49.784f,120.68f),floatArrayOf(356.588f,272.124f,8.797f,351.203f),floatArrayOf(90f,90f)),
        0,-1.35f,1.75f,-3.39f,0.08f)
)
