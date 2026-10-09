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

- **Factory / factory method:** `NotifierFactory#createStrategy()` creates the
  notification renderer instead of callers constructing it directly. In plain
  terms, the caller asks a factory for "something that can render a
  notification" instead of knowing the concrete class name.
- **Strategy:** `NotificationStrategy` is the strategy interface, and
  `EmailNotificationStrategy` is the concrete strategy used to render a
  `NotificationMessage` into text. In plain terms, the formatting algorithm is
  put behind an interface so the system could swap "email formatting" for
  another rendering rule.
- **Observer / publisher-subscriber:** `NotificationHub` is the publisher,
  `NotificationSubscriber` is the subscriber interface, and `OutboxSubscriber`
  is the concrete subscriber registered by the hub. In plain terms, the hub
  publishes one rendered message and every subscriber gets notified without the
  hub needing to know what each subscriber does with it.

### The problem each one solves

- **Factory / factory method:** This is the right call if notification renderer
  construction has a real decision to make, such as choosing email, SMS, or
  Slack from configuration or environment.
- **Strategy:** This is the right call if the system must support multiple
  rendering algorithms for the same `NotificationMessage`, such as email text,
  SMS text, and webhook JSON.
- **Observer / publisher-subscriber:** This is the right call if one published
  notification must fan out to multiple independent actions, such as writing to
  the outbox, sending email, recording metrics, and notifying an audit log.

### Which of those problems exist here

- **Factory / factory method:** No. `NotifierFactory#createStrategy()` always
  returns `new EmailNotificationStrategy()`, and `NotificationHub` immediately
  calls that factory in its constructor, so there is no runtime construction
  choice in this codebase.
- **Strategy:** No. There is only one implementation,
  `EmailNotificationStrategy`, and no caller passes in or selects another
  `NotificationStrategy`; the tested output is one fixed email-like string.
- **Observer / publisher-subscriber:** Mostly no. `NotificationHub` has a
  `subscribe` method and loops over subscribers in `publish`, but its
  constructor always registers exactly one `OutboxSubscriber`, and
  `NotificationHubTest#hubDeliversToItsOneSubscriber` asserts that count is 1.

### The simpler structure

**Your proposal.** Replace the pattern stack with `NotificationHub`,
`NotificationMessage`, and `Outbox`. `NotificationHub#publish` can render the
one supported format directly and append it to the outbox:
`"To: " + message.recipient() + " | Subject: " + message.subject() + " | "
+ message.body()`. That removes `NotifierFactory`, `NotificationStrategy`,
`EmailNotificationStrategy`, `NotificationSubscriber`, and
`OutboxSubscriber`.

**What stays the same.** `NotificationHubTest#publishedMessageLandsInTheOutboxFullyRendered`
must still produce exactly `To: ada@rooms.example.edu | Subject: Booking confirmed
| Room Willow Room from 2026-10-05T09:00 to 2026-10-05T10:00`.
`NotificationHubTest#aConfirmationFromTheWorkflowReachesTheOutbox` must still
show a workflow confirmation landing in the outbox with that same rendered
format.

**What you would keep, if anything.** I would keep no interface today.
`NotificationMessage` is still useful as the value passed by workflow code, and
`Outbox` is still useful as the tested storage object, but neither needs an
interface while there is only one rendering format and one destination.

### What would bring each layer back

- **Factory / factory method:** Bring it back if notification setup starts
  choosing a renderer from configuration, such as `NOTIFY_CHANNEL=email|sms|slack`,
  or if renderer construction needs credentials and environment-specific setup.
- **Strategy:** Bring it back if the product must render the same
  `NotificationMessage` into multiple formats, such as email headers, SMS text,
  and webhook JSON, while keeping workflow code independent of those formats.
- **Observer / publisher-subscriber:** Bring it back if publishing one booking
  event must trigger several independent destinations, such as outbox storage,
  an audit log, metrics, and a real mail sender.

**Misuse or anti-pattern?** This is pattern misuse, not an anti-pattern. The
patterns are recognizable and implemented in ordinary ways, but the current
requirements are too small for them; calling it an anti-pattern would imply the
patterns are harmful by nature, when the real issue is using them before the
code has the problems they solve.

---

## Milestone 3: The missing pattern

Read `pricing/`. Not coded, one sentence.

**The pattern.** Chain of Responsibility fits `PriceCalculator`: the problem is
ordered pricing rules, where each rule adjusts the running price before handing
it to the next rule.

**Would you apply it today?** No; today there are only four fixed rules in one
published order, so a rule chain would add more structure than the requirements
need.
