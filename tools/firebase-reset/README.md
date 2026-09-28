# Development Data Reset Tooling

This directory contains development-only tooling to perform a complete reset of test/user data for the **KannadaSpeechAssistant** project.

> [!CAUTION]
> **DEVELOPMENT ONLY tool**. Do NOT bundle or execute this in production environments.

## Prerequisites
- Node.js v18+ installed
- Firebase Admin SDK (already configured in `package.json`)
- Firebase credentials (`GOOGLE_APPLICATION_CREDENTIALS` environment variable set OR `serviceAccountKey.json` placed in this directory).

## Usage

To run complete reset (Auth + Firestore):
```bash
node reset-all.js --confirm=DELETE_ALL_DEVELOPMENT_DATA
```

To run Auth reset only:
```bash
node reset-auth.js --confirm=DELETE_ALL_DEVELOPMENT_DATA
```

To run Firestore reset only:
```bash
node reset-firestore.js --confirm=DELETE_ALL_DEVELOPMENT_DATA
```

## Safety Features
- **Strict Confirmation Phrase**: Execution is blocked unless `--confirm=DELETE_ALL_DEVELOPMENT_DATA` is passed.
- **Service Account Isolation**: Credentials are never included in APK or committed to source repositories.
