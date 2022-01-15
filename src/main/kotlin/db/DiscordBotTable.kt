package db

object DiscordBotTable : BaseTable("discord_users") {

    val id = integer("id").autoIncrement()
    val token = text("token").default("")
    val guildId = text("guildId").nullable()
    val guildName = text("guildName").nullable()
    val channelId = text("channelId").nullable()
    val channelName = text("channelName").nullable()

    override val primaryKey = PrimaryKey(id)
}
