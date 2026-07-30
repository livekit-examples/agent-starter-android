<img src="./.github/assets/app-icon.png" alt="Voice Assistant App Icon" width="100" height="100">

# Android Agent Starter

This is a starter template for [LiveKit Agents](https://docs.livekit.io/agents/overview/) that provides a simple voice interface using the [LiveKit Android SDK](https://github.com/livekit/client-sdk-android).

This template is free for you to use or modify as you see fit.

## Getting started

The easiest way to get this app running is with the [LiveKit CLI](https://docs.livekit.io/home/cli/cli-setup/). Run the following command to automatically clone this template and connect it to LiveKit Cloud:

```bash
lk app create --template agent-starter-android
```

Build and run the app in Android Studio.

> [!NOTE]
> To set up without the LiveKit CLI, clone the repository via git.

## Connect to your agent

To switch from the default agent to your own, you first need a LiveKit agent to speak with. For a no code setup, use the [Agent Builder](https://docs.livekit.io/agents/start/builder/). For more customization, try our starter agent for [Python](https://github.com/livekit-examples/agent-starter-python), [Node.js](https://github.com/livekit-examples/agent-starter-node), or [create your own from scratch](https://docs.livekit.io/agents/start/voice-ai/).

Second, you need a token server. For development, the easiest option is the sandbox token server: enable it from your project's Options on the Settings page in LiveKit Cloud and copy the sandboxId.

Then edit `TokenExt.kt`:

```
const val sandboxID = "your sandbox id"
```

## Token generation

In a production environment, you will be responsible for developing a solution to [generate tokens for your users](https://docs.livekit.io/home/server/generating-tokens/) which is integrated with your authentication solution. You should disable the token server and modify `TokenExt.kt` to use your own token server.

## Contributing

This template is open source and we welcome contributions! Please open a PR or issue through GitHub, and don't forget to join us in the [LiveKit Community Slack](https://livekit.io/join-slack)!
