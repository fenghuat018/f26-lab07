# REFACTOR.md

One section per milestone. Fill each one in as you go, in order.

Milestone 1 is written in two sittings, the pin before the refactor and the
rest after. A pin written afterwards is worth nothing, and a TA will ask.

Keep it short and specific. Point at methods, call sites, and test names.

---

## Milestone 1: Direct a refactor, characterization first

### The pin (write this section before you direct the refactor)

**The pin.** `src/test/java/edu/cmu/cs214/scheduling/workflow/BookingWorkflowTest.java`,
`recurringCancelFromMiddleReleasesCurrentAndFutureOccurrencesOnly`: `cancel`
on a recurring occurrence cancels that occurrence and later occurrences in the
same series, while leaving earlier occurrences active. It is green against the
shipped code, and I added a new test method rather than editing or deleting an
existing one.

**Why that one, and does a shipped test already cover it?** This is worth a
pin because the recurring cancel branch has a non-obvious policy boundary: it
does not just cancel one occurrence, and it also does not cancel the whole
series. The shipped `recurringCancelReleasesTheOccurrence` test comes close,
but it cancels the last occurrence, so it cannot show that future occurrences
are also cancelled or that earlier occurrences stay active. I checked the rest
of `BookingWorkflowTest` and no other shipped test asserts that middle-of-series
cascade.

**What a regeneration would do differently here.** Regenerating
`BookingWorkflow` from a one-line booking workflow description would have to
decide the recurring-cancel policy again. It would probably choose the simpler
"cancel only the selected occurrence" behavior unless the prompt explicitly
said cancellation should release the selected occurrence and every future
occurrence in the series.

### The directive

**The refactor and the exact directive.** Refactor chosen: extract a class per
booking type. Directive: "In Milestone 1, refactor `BookingWorkflow` by
extracting a class per booking type: regular, recurring, and blocked. Scope is
`src/main/java/edu/cmu/cs214/scheduling/workflow/` only; keep
`domain/`, `pricing/`, `notify/`, `reporting/`, `pom.xml`, and tests out of
bounds except for the already-committed characterization pin. The refactor must
remove the repeated booking-type conditional from all four public workflow
methods (`submit`, `cancel`, `priceOf`, and `describe`) without changing
observable behavior, notification text, rejection messages, prices, or store
writes." The boundary sits at `workflow/` because this is where the repeated
type policy lives; the other packages are collaborators and data contracts.

### The result

**The diff and the suite.** I am showing the TA two commits: first
`63ab380 Add recurring cancel characterization pin`, then the refactor commit
that extracts the booking-type workflow classes. The suite totals line after
the refactor is `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`: the shipped
35 tests plus my pin, all green.

**What did NOT change: behavior and files.** The checked behavior stayed the
same for submit accept/reject messages, overlap handling, recurring skip
counts, cancellation notifications, prices, descriptions, and the new pin for
middle-of-series recurring cancellation. The surprising behavior I preserved is
that recurring cancellation from the middle cancels that occurrence and future
occurrences while leaving earlier ones active. Files outside the directive did
not change: `git diff --name-only HEAD` before committing showed only
`workflow/` files and `REFACTOR.md`, and the tests stayed unchanged after the
pin commit. The agent did not reach into `domain/`, `pricing/`, `notify/`, or
`reporting/`.

**One thing the agent changed that you had to look at twice.** I checked the
recurring path line by line because it had two easy-to-break details: submit
uses the original inclusive overlap comparison for skipped occurrences, and
cancel keeps the original "selected occurrence and future occurrences" loop.
Those details are now in `RecurringBookingWorkflow`.

### The closing explanation

**Refactor or regenerate?** Refactor was the better call. Test coverage is
good enough to guard this move: the shipped tests cover regular, recurring, and
blocked submit/cancel/price/describe behavior, and my pin adds the missing
middle-of-series recurring cancellation rule. Code age argues against throwing
it away: the class has accumulated specific decisions, like recurring skip
messages, admin-only block cancellation, and the recurring cancellation
cascade. Spec quality is not high enough to regenerate from scratch; the
README describes capabilities, but it does not state details like exact
notification text, rejection strings, or whether recurring cancellation should
cancel one occurrence or the future tail of the series. Reach also argues for a
refactor: this class is the front door for writes, prices, descriptions, and
notifications, and other packages depend on its exact behavior. Regeneration
would be more likely to remake hidden decisions than preserve them.

**What would flip your answer.** I would choose regeneration if there were a
complete executable contract for `BookingWorkflow`: tests or specs covering all
messages, notification side effects, conflict boundaries, pricing calls, and
recurring cancellation policy, plus no external client depending on the current
quirks.

---

## Milestone 2: The pattern critique

Read `notify/`. It works and the outbox tests pass.

### The patterns present

List every design pattern you can name in that package. For each one, the class
or classes that carry it.

### The problem each one solves

For each pattern you listed, what would have to be true about the requirements
for that pattern to be the right call? One sentence each, not in terms of
"flexibility".

### Which of those problems exist here

For each pattern, does the problem it solves exist in this codebase? Point at
the code that settles it.

### The simpler structure

**Your proposal.** What replaces `notify/`. Sketch the classes and the one
method that matters.

**What stays the same.** The tested behavior it must still produce, named
precisely enough that a reader can check it against the shipped tests.

**What you would keep, if anything.** If you would keep one interface, say
which and why. "None of it" is a fine answer if you can defend it.

### What would bring each layer back

For at least two of the layers you would remove, what requirement, if it
arrived next sprint, would make that layer the right structure? Be specific
about the requirement, not about the pattern.

**Misuse or anti-pattern?** Say which this is and why the distinction matters.

---

## Milestone 3: The missing pattern

Read `pricing/`. Not coded, one sentence.

**The pattern.** Which one fits `PriceCalculator`, and the problem that makes
it fit. Name the problem.

**Would you apply it today?** Yes or no, one line, with the reason.
