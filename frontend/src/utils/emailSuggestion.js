/**
 * Catches the common way people mistype an email domain.
 *
 * Nothing here proves an address exists — only sending to it does that. What it
 * does catch is "gmai.com" for "gmail.com", which the browser's own validation
 * happily accepts because the format is perfectly legal.
 */

const COMMON_DOMAINS = [
    "gmail.com",
    "yahoo.com",
    "hotmail.com",
    "outlook.com",
    "icloud.com",
    "protonmail.com",
    "live.com",
    "aol.com",
];

/** Damerau-style distance, so a swapped pair counts as one mistake. */
const distance = (a, b) => {
    const d = Array.from({ length: a.length + 1 }, (_, i) =>
        Array.from({ length: b.length + 1 }, (_, j) => (i === 0 ? j : j === 0 ? i : 0))
    );

    for (let i = 1; i <= a.length; i++) {
        for (let j = 1; j <= b.length; j++) {
            const cost = a[i - 1] === b[j - 1] ? 0 : 1;
            d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);

            if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1]) {
                d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
            }
        }
    }
    return d[a.length][b.length];
};

/**
 * Returns a corrected address when the domain looks like a near-miss of a
 * common one, or null when there is nothing to suggest.
 */
export const suggestEmail = (email) => {
    if (!email || !email.includes("@")) return null;

    const [local, domain] = email.toLowerCase().split("@");
    if (!local || !domain) return null;

    // Already a known domain: nothing to say.
    if (COMMON_DOMAINS.includes(domain)) return null;

    for (const candidate of COMMON_DOMAINS) {
        if (distance(domain, candidate) <= 2) {
            return `${local}@${candidate}`;
        }
    }
    return null;
};
