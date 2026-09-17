# Custom server setup

Custom server mode sends audio and text directly to an OpenAI-compatible server. It is separate from the built-in cloud mode and must be selected explicitly.

## Defaults file

Create `custom-openai.json` in the app's local profile directory:

- macOS: `~/Library/Application Support/AI Dictation/`
- iOS: the app's Application Support directory
- Android: the app's files directory
- Windows: `%APPDATA%\\AIDictation\\`

The file contains defaults only. A value saved in Settings wins for that field; API keys are intentionally never read from this file.

```json
{
  "version": 1,
  "transcription": {
    "baseUrl": "https://server.example/v1",
    "model": "whisper-1",
    "realtimeUrl": "wss://server.example/v1/realtime?intent=transcription",
    "realtimeModel": "gpt-4o-transcribe"
  },
  "cleanup": {
    "baseUrl": "https://server.example/v1",
    "model": "gpt-4o-mini"
  }
}
```

The app derives `/audio/transcriptions` and `/chat/completions` from the two base URLs. Remote servers must use HTTPS/WSS. HTTP/WS is accepted only for `localhost`, `127.0.0.1`, or `::1`.

## Required compatibility

- Batch transcription: OpenAI `POST /audio/transcriptions`, returning JSON with a non-empty `text` field.
- Cleanup: OpenAI `POST /chat/completions`, returning `choices[0].message.content`.
- Realtime: OpenAI Realtime WebSocket transcription events, including audio append/commit and ordered completed transcript events.

If realtime cannot finish, the app uses the finalized persisted recording with the configured batch endpoint. Cleanup remains optional: a cleanup failure keeps the complete raw transcript.
