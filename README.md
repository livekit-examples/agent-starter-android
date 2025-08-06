<img src="./.github/assets/app-icon.png" alt="Voice Assistant App Icon" width="100" height="100">

# Android Agent Starter

This is a starter template for [LiveKit Agents](https://docs.livekit.io/agents/overview/) that provides a simple voice interface using the [LiveKit Android SDK](https://github.com/livekit/client-sdk-android).

This template is free for you to use or modify as you see fit.

## Getting started

The easiest way to get this app running is with the [Sandbox for LiveKit Cloud](https://cloud.livekit.io/projects/p_/sandbox) and the [LiveKit CLI](https://docs.livekit.io/home/cli/cli-setup/).

First, create a new [Sandbox Token Server](https://cloud.livekit.io/projects/p_mytc7vpzfkt/sandbox/templates/token-server) for your LiveKit Cloud project.

Then, run the following command to automatically clone this template and connect it to LiveKit Cloud:

```bash
lk app create --template agent-starter-android --sandbox <token_server_sandbox_id>
```

Build and run the app in Android Studio.

You'll also need an agent to speak with. Try our starter agent for [Python](https://github.com/livekit-examples/agent-starter-python), [Node.js](https://github.com/livekit-examples/agent-starter-node), or [create your own from scratch](https://docs.livekit.io/agents/start/voice-ai/).

> [!NOTE]
> To setup without the LiveKit CLI, clone the repository and edit the `TokenExt.kt` file to add either a `sandboxID` (if using a [Sandbox Token Server](https://cloud.livekit.io/projects/p_/sandbox/templates/token-server)), or a [manually generated](#token-generation) URL and token.

## Token generation

In a production environment, you will be responsible for developing a solution to [generate tokens for your users](https://docs.livekit.io/home/server/generating-tokens/) which is integrated with your authentication solution. You should disable your sandbox token server and modify `TokenExt.kt` to use your own token server.

## Contributing

This template is open source and we welcome contributions! Please open a PR or issue through GitHub, and don't forget to join us in the [LiveKit Community Slack](https://livekit.io/join-slack)!
