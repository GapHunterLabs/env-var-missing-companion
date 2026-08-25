// DATABASE_URL: declared in .env.example -- should NOT be flagged.
const dbUrl = process.env.DATABASE_URL;

// PORT: declared, has an explicit default -- should show as a weaker warning, not suppressed.
const port = process.env.PORT || 3000;

// STRIPE_SECRET_KEY: used but never declared anywhere -- should be flagged with a quick-fix.
const stripeKey = process.env.STRIPE_SECRET_KEY;
