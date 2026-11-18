package campus.organiser

import campus.activities.*
import campus.auth.actor
import campus.auth.requireRole
import campus.http.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.http.ContentType
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private fun qrPng(url: String): ByteArray {
    val bits = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 256, 256)
    val image = BufferedImage(bits.width, bits.height, BufferedImage.TYPE_BYTE_BINARY)
    for (y in 0 until bits.height) for (x in 0 until bits.width) image.setRGB(
        x,
        y,
        if (bits[x, y]) 0x000000 else 0xffffff,
    )
    return ByteArrayOutputStream().use { out ->
        ImageIO.write(image, "png", out)
        out.toByteArray()
    }
}

fun Route.organiserRoutes(api: Api) =
    route("/organiser") {
        get("clubs") { api.role(call, "organiser") { organiserClubs(it) } }
        route("activities") {
            get { api.role(call, "organiser") { organiserActivities(it) } }
            post {
                val body = call.payload()
                api.role(call, "organiser") { createDraft(it, body) }
            }
            route("{id}") {
                get { api.role(call, "organiser") { manageView(managed(it, call.param("id"))) } }
                put {
                    val body = call.payload()
                    api.role(call, "organiser") { editDraft(it, call.param("id"), body) }
                }
                post("submit") {
                    api.role(call, "organiser") { actor ->
                        val a = managed(actor, call.param("id"))
                        submitReview(actor, a, a.fields, false)
                        manageView(activity(a.id))
                    }
                }
                post("edits") {
                    val body = call.payload()
                    api.role(call, "organiser") { actor ->
                        val a = managed(actor, call.param("id"))
                        submitReview(actor, a, a.fields.with(formFields(body)), true)
                        manageView(activity(a.id))
                    }
                }
                get("participants") {
                    api.role(call, "organiser") { participants(it, call.param("id")) }
                }
                post("cancel") {
                    val body = call.payload()
                    api.role(call, "organiser") {
                        cancelActivity(it, call.param("id"), body.text("reason"))
                    }
                }
                get("qr.png") {
                    val session = call.request.cookies[sessionCookie]
                    val token =
                        api.transaction {
                            val a =
                                managed(actor(session).requireRole("organiser"), call.param("id"))
                            a.qr ?: missing()
                        }
                    call.respondBytes(
                        qrPng(api.publicUrl + "/register/" + token),
                        ContentType.Image.PNG,
                    )
                }
            }
        }
    }
