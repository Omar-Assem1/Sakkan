# Google Sign-In Setup

This guide records the Google Cloud configuration needed to try SCRUM-56 locally. The application code is implemented, but real Google sign-in requires a Google OAuth web client ID and client secret.

## Cost

Creating an OAuth client for this Google sign-in flow should not incur a Google per-login charge. The app requests only the identity scopes `openid`, `profile`, and `email`; it does not use a paid Google API or Google Identity Platform. Google Identity Platform is a separate product with its own pricing: [Identity Platform pricing](https://cloud.google.com/identity-platform/pricing).

You may have separate costs for hosting, a domain, or other Google Cloud products. Do not enable unrelated APIs or services for this sign-in setup.

## 1. Configure Google Auth Platform

1. Open the [Google Cloud Console](https://console.cloud.google.com/) and select or create a project.
2. Open **Google Auth Platform → Branding** and provide the app name, support email, and contact email.
3. Under **Audience**, choose the appropriate audience. For local testing with regular Google accounts, use **External** and keep the app in **Testing**.
4. Add the Google account(s) you will use for testing if the console presents a test-user list. Google documents the current audience/test-user behavior in its [Manage App Audience guide](https://support.google.com/cloud/answer/15549945?hl=en).
5. Keep the requested scopes limited to `openid`, `profile`, and `email` for this feature.

## 2. Create a Web OAuth client

1. In Google Auth Platform, open **Clients** and select **Create client**.
2. Choose **Web application**.
3. Add the local frontend origin under **Authorized JavaScript origins**:

   ```text
   http://localhost:4200
   ```

4. Add the backend callback under **Authorized redirect URIs**. The usual URI with this repository's Angular development proxy is:

   ```text
   http://localhost:8080/login/oauth2/code/google
   ```

   OAuth redirect URIs must match exactly, including scheme, host, port, and path. Confirm the actual URI emitted by your running app before relying on this value: visit `http://localhost:8080/oauth2/authorization/google` and inspect the `redirect_uri` parameter in the `Location` response. Register that exact URI in Google Cloud. The Google [web-server OAuth guide](https://developers.google.com/identity/protocols/oauth2/web-server?hl=en) describes this exact-match requirement.

5. Create the client and copy its **Client ID** and **Client secret**. Keep the secret private.

For a deployed environment, use its HTTPS frontend origin and the externally reachable HTTPS backend callback URI. Register that exact production callback separately in the OAuth client.

## 3. Put credentials in the local environment

From the repository root, set the values in `.env` (create the file if it does not exist):

```dotenv
GOOGLE_CLIENT_ID=replace-with-client-id
GOOGLE_CLIENT_SECRET=replace-with-client-secret
FRONTEND_URL=http://localhost:4200
```

Do not put real credentials in this Markdown file, source code, screenshots, commits, or chat. Do not commit `.env`; commit only safe placeholders in `.env.example`.

The Docker Compose configuration reads `.env` from the repository root. Recreate the API container after changing credentials:

```bash
cd ~/Sakkan
docker compose up -d --force-recreate api
docker compose logs -f api
```

If running the backend directly rather than through Docker, export the same variables in the shell before starting Spring Boot.

## 4. Try the flow

1. Start the backend and frontend; open `http://localhost:4200`.
2. Select **Continue with Google** and sign in with an allowed Google account.
3. On first sign-in, choose a Sakan username. Returning users should go straight into Sakan.
4. Confirm the frontend receives a Sakan session and can load the authenticated account.

If Google reports `redirect_uri_mismatch`, compare the `redirect_uri` in the Google authorization request with the **Authorized redirect URIs** list. They must be identical. If the app reports a Google sign-in error, check the API logs and verify the client ID/secret and frontend URL.

## References

- [Create Google OAuth credentials](https://developers.google.com/workspace/guides/create-credentials)
- [OAuth 2.0 for Web Server Applications](https://developers.google.com/identity/protocols/oauth2/web-server?hl=en)
- [Manage app audience and testing status](https://support.google.com/cloud/answer/15549945?hl=en)
- [Google Identity Platform pricing](https://cloud.google.com/identity-platform/pricing)
