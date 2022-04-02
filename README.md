##Desktop application for broadcasting messages to several media like Discord and Telegram. [![CI](https://github.com/trueddd/announcer-desktop/actions/workflows/main.yml/badge.svg?branch=master)](https://github.com/trueddd/announcer-desktop/actions/workflows/main.yml)

Actual release could be found [here](https://github.com/trueddd/announcer-desktop/releases). Now there are only 2 available integrations: Discord and Telegram.

####NOTE! Application listens and sends new messages from provided sources **ONLY** when it is launched.

####Setup

- Telegram
  1. Create a telegram bot via [@BotFather](https://t.me/botfather).
  2. Get your bot token and enter it into the input field under Telegram logo on the Announcer app.
  3. Add bot to your Telegram channel as admin.
  4. Click `Start` button on the app and wait bot to connect.
  5. To remember your Telegram channel, click `Refresh` button and pin any message in your channel. After that app should show your channel name in field above the `Refresh` button.
  6. Now app is ready to work with your Telegram channel and will remember channel after app restart.
- Discord
  1. Register new Discord application [here](https://discord.com/developers/applications).
  2. In the `Bot` tab get token and paste it into input field under Discord logo on the Announcer app.
  3. Invite bot to your Discord guild (server) and make sure it has enough permissions to view channels, messages and send messages into desired channel.
  4. Click `Start` button on the app and wait bot to connect.
  5. Select your guild and channel under `Token` field.
  6. Now app is ready to work with your Discord guild.

####To-do:
- [ ] #4
- [ ] #3
- [ ] #2
- [ ] #1
