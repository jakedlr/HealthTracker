# Local dates use the device's current time zone

Food and sleep times are stored as instants with no time zone, and are turned into local dates using whatever zone the phone is in now. Weight is stored as a plain date. If the phone changes time zone (for example after travel), past food logs and sleeps can move to a different day, while weigh-ins stay put. We accept this because the app has one user on one phone who rarely changes zone far enough to matter, and storing a zone with every entry would add a schema migration and complexity everywhere dates are derived.
