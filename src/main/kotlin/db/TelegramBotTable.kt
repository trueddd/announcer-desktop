package db

object TelegramBotTable : BaseTable("telegram_users") {

    val id = integer("id").autoIncrement()
    val token = text("token").default("")
    val channelId = text("channel_id").default("")
    val channelTitle = text("channel_title").default("")

    override val primaryKey = PrimaryKey(id)
}
