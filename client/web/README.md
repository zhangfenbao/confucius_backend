# Open Sesame Web App

This is a Next.js app implementation to interact with the Open Sesame REST APIs and RTVI.

## Get started

1. Install dependencies:

```bash
npm ci
```

2. Configure environment variables:

```bash
cp env.example .env.local
```

- `SESAME_BASE_URL` points to the Open Sesame webapp / REST API.
- `SESAME_USER_TOKEN` a valid (non revokved / expired) user token. See [authentication](../../docs/authentication.md).

3. Start dev server

```bash
npm run dev
```

By default this will start the Next.js server on Port 3000.
You can optionally provide a custom port:

```bash
PORT=3002 npm run dev
```

## More configuraion

### Use Modal deploy
`/client/web/.env.local`:

```
BACKEND_URL=https://YOUR_DEPLOY_NAME--sesame-api.modal.run
```