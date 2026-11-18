package campus.db

import campus.http.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.Clock
import java.time.Instant
import kotlinx.serialization.json.*

class Database(private val file: String, private val clock: Clock = Clock.systemUTC()) {
    init {
        Class.forName("org.sqlite.JDBC")
        transaction {
            for (name in listOf("schema.sql", "seed.sql")) {
                val script =
                    Database::class.java.getResourceAsStream("/" + name)!!.bufferedReader().use {
                        it.readText()
                    }
                // A blank line separates statements, including triggers containing semicolons.
                script.trim().split(Regex("\\r?\\n\\s*\\r?\\n")).forEach { sql ->
                    connection.createStatement().use { it.execute(sql) }
                }
            }
        }
    }

    fun <T> transaction(block: Tx.() -> T): T =
        DriverManager.getConnection("jdbc:sqlite:" + file).use { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute("PRAGMA foreign_keys=ON")
                stmt.execute("PRAGMA busy_timeout=10000")
                stmt.execute("BEGIN IMMEDIATE")
                try {
                    val result = Tx(connection, clock.instant()).block()
                    stmt.execute("COMMIT")
                    result
                } catch (failure: Throwable) {
                    stmt.execute("ROLLBACK")
                    throw failure
                }
            }
        }
}

class Tx(val connection: Connection, val now: Instant) {
    val time
        get() = timestamp(now)

    private fun statement(sql: String, args: Array<out Any?>): PreparedStatement =
        connection.prepareStatement(sql).apply {
            args.forEachIndexed { i, v -> setObject(i + 1, v) }
        }

    fun exec(sql: String, vararg args: Any?): Int = statement(sql, args).use { it.executeUpdate() }

    fun rows(sql: String, vararg args: Any?): List<JsonObject> =
        statement(sql, args).use { stmt ->
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(
                        buildJsonObject {
                            for (i in 1..rs.metaData.columnCount) {
                                val value = rs.getObject(i)
                                val name = rs.metaData.getColumnLabel(i)
                                when (value) {
                                    null -> put(name, JsonNull)
                                    is Number -> put(name, JsonPrimitive(value))
                                    else -> put(name, value.toString())
                                }
                            }
                        }
                    )
                }
            }
        }

    fun one(sql: String, vararg args: Any?) = rows(sql, *args).firstOrNull()
}
