-- Seed data: 6 fictional support articles for the "Nexus Platform" SaaS product.
-- The embedding column is left NULL here.
-- In mock mode, MockRagService returns dummy responses without reading embeddings.
-- In live mode, the knowledge service populates embeddings via the OpenAI API on first use.

INSERT INTO knowledge_articles (id, title, content, created_at, updated_at) VALUES

(gen_random_uuid(),
 'How to reset your Nexus Platform password',
 'To reset your password, navigate to the login page and click "Forgot password?". Enter the email address associated with your account and click "Send reset link". Check your inbox for an email from noreply@nexusplatform.io — it may take up to 2 minutes to arrive. Click the link in the email, which is valid for 30 minutes. Enter your new password twice and click "Save". If you do not receive the email, check your spam folder. If the problem persists, contact support with your registered email address.',
 NOW(), NOW()),

(gen_random_uuid(),
 'How to invite team members to your workspace',
 'Workspace owners and admins can invite new members. Go to Settings → Team → Invite Members. Enter one or more email addresses separated by commas and choose a role: Viewer, Editor, or Admin. Click "Send Invitations". Invitees receive an email with an activation link valid for 48 hours. If the link expires, resend it from the Pending Invitations section. Free plan workspaces are limited to 3 members. Upgrade to Pro to invite unlimited team members.',
 NOW(), NOW()),

(gen_random_uuid(),
 'Understanding Nexus Platform subscription plans',
 'Nexus Platform offers three plans. Free: up to 3 users, 5 projects, 1 GB storage, community support only. Pro ($29/month per workspace): unlimited users, unlimited projects, 50 GB storage, priority email support, and custom domains. Enterprise (custom pricing): everything in Pro plus SSO/SAML, audit logs, a dedicated account manager, 99.99% uptime SLA, and on-premise deployment options. You can upgrade at any time from Settings → Billing. Downgrades take effect at the end of the current billing cycle.',
 NOW(), NOW()),

(gen_random_uuid(),
 'How to export your project data',
 'You can export all project data at any time. Open the project, click the three-dot menu in the top-right corner, and select "Export Project". Choose a format: JSON (full fidelity, recommended for backups) or CSV (compatible with spreadsheet tools, flattens nested structures). Large exports are processed asynchronously — you will receive an email with a download link when ready. Download links expire after 24 hours. Workspace Admins can export all projects at once from Settings → Data → Bulk Export.',
 NOW(), NOW()),

(gen_random_uuid(),
 'Troubleshooting failed API integrations',
 'If your API integration is returning errors, follow these steps. 1) Verify your API key is active: go to Settings → API Keys and check its status. Rotate the key if it is expired. 2) Check the request format: ensure the Content-Type header is application/json and the request body matches the schema in the API docs. 3) Check rate limits: Free plan allows 100 requests/minute, Pro allows 1000/minute. A 429 response means you have exceeded the limit — implement exponential back-off in your client. 4) Check the status page at status.nexusplatform.io for ongoing incidents. 5) If the issue persists, open a support ticket and include the full request and response payload.',
 NOW(), NOW()),

(gen_random_uuid(),
 'How to enable two-factor authentication',
 'Two-factor authentication (2FA) adds a second layer of security to your account. To enable it, go to Settings → Security → Two-Factor Authentication and click "Enable 2FA". Scan the QR code with an authenticator app such as Google Authenticator, Authy, or 1Password. Enter the 6-digit code from your app to confirm setup. Save your backup codes in a secure location — they are the only way to recover access if you lose your device. Once 2FA is enabled, you will be prompted for a code on every login. Workspace Admins can enforce 2FA for all members from Settings → Security → Enforce 2FA.',
 NOW(), NOW());
