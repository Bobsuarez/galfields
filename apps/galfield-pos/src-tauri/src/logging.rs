//! One shared, sequential trace log so every meaningful checkpoint across
//! the backend — entering a command, a validation passing, a request going
//! out — gets a numbered line in the terminal (`[0001] ...`, `[0002] ...`)
//! in the exact order it happened, tagged with the module/function it came
//! from. When something goes wrong, point at a specific number instead of
//! describing "the thing that happens after sync starts" — the number,
//! the tag, and the message together say which file/function to open.
//!
//! Not a replacement for the request/response bodies `http_client.rs`
//! prints — this is for the steps *around* those calls (validation,
//! branching, DB writes) that have no HTTP call to log by themselves.

use std::sync::atomic::{AtomicU64, Ordering};

static COUNTER: AtomicU64 = AtomicU64::new(1);

/// `location` should be `module::function`, e.g. `"sync::sync_products"` —
/// that's what you grep the terminal for for to find every step of one flow.
pub fn step(location: &str, message: impl std::fmt::Display) {
    let n = COUNTER.fetch_add(1, Ordering::Relaxed);
    println!("[{n:04}] {location} - {message}");
}
