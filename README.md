# PageWeaver Java SDK

Official Java client for the [PageWeaver](https://pageweaver.io) PDF generation API. Uses the JDK's built-in HTTP client; Gson for JSON. Java 11+.

## Install

```xml
<dependency>
  <groupId>io.pageweaver</groupId>
  <artifactId>pageweaver</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick start

```java
import io.pageweaver.PageWeaver;
import io.pageweaver.WaitOptions;
import java.util.Map;

PageWeaver pw = new PageWeaver("pk_live_...");
// Point at a local stack in dev: new PageWeaver("pk_test_...", "http://localhost:4000")

// Create a document and wait for it to finish rendering.
Map<String, Object> doc = pw.documents().createAndWait(
    Map.of("templateId", "tmpl_invoice",
           "payload", Map.of("number", "INV-001", "total", 4200)),
    new WaitOptions());
System.out.println(doc.get("status")); // "done"

// Download the finished PDF (resolves the signed URL for you).
byte[] pdf = pw.documents().download((String) doc.get("id"));
```

The client is organized into **resources**, each reached via an accessor that returns a cached instance:
`documents()`, `templates()` (with `templates().proposals()`), `schemas()`, `usage()`, `comments()`,
`reviews()`, `shareLinks()`, `environments()`, and `deployments()`. Object responses come back as
`Map<String, Object>` and array responses as `List<Object>` (parsed via Gson).

## Documents

```java
// Fire-and-poll yourself.
Map<String, Object> created = pw.documents().create(Map.of("html", "<h1>Hello</h1>"));
Map<String, Object> result = pw.documents().get((String) created.get("id"));

// Idempotent create.
pw.documents().create(body, "my-idempotency-key");

// Synchronous create (server holds the response open until the render finishes).
SyncResult sync = pw.documents().createSync(body, /* pdf */ true);
if ("pdf".equals(sync.kind)) {
    Files.write(Path.of("invoice.pdf"), sync.pdf);
} else if ("pending".equals(sync.kind)) {
    pw.documents().waitFor(sync.id, new WaitOptions()); // deadline elapsed; poll
} else { // "document"
    System.out.println(sync.document.get("id"));
}

// History, integrity, replay, pages.
Map<String, Object> page = pw.documents().list(Map.of("status", "done", "limit", 50));
List<Object> everything = pw.documents().listAll(Map.of("status", "failed"));
Map<String, Object> proof = pw.documents().verify(id);
Map<String, Object> again = pw.documents().regenerate(id);
List<Object> pages = pw.documents().pages(id);

// Download-protected documents: pass the password (no API key sent).
byte[] bytes = pw.documents().download(id, "the-download-password");
```

`waitFor` / `createAndWait` accept a `WaitOptions` (defaults: `intervalMs=1000`, `maxIntervalMs=5000`,
`backoff=1.5`, `timeoutMs=60000`, `throwOnFailure=true`). On timeout they throw
`PageWeaverTimeoutException`; on a failed document they throw `PageWeaverDocumentFailedException`
(unless `throwOnFailure(false)`).

## Templates, proposals, and schemas

```java
List<Object> templates = pw.templates().list();
Map<String, Object> tmpl = pw.templates().get("tmpl_invoice");
List<Object> versions = pw.templates().versions("tmpl_invoice");
Map<String, Object> v = pw.templates().version("tmpl_invoice", 3, "source");

// Proposals: the PR analog for template changes (deploy-scoped key).
Map<String, Object> proposal = pw.templates().proposals().open("tmpl_invoice", Map.of("fromDraft", true));
pw.templates().proposals().approve("tmpl_invoice", (String) proposal.get("id"), Map.of());
pw.templates().proposals().promote("tmpl_invoice", (String) proposal.get("id"));

List<Object> schemas = pw.schemas().list();
Map<String, Object> schema = pw.schemas().get("sch_invoice", 2);
Map<String, Object> nodes = pw.schemas().version("sch_invoice", 2, "nodes");
```

## Reviews, comments, and share links

```java
Map<String, Object> review = pw.reviews().create(Map.of("documentId", id));
pw.reviews().addParticipant((String) review.get("id"), Map.of("externalEmail", "boss@acme.test"));
pw.reviews().approve((String) review.get("id"), Map.of());

Map<String, Object> thread = pw.comments().create(Map.of("documentId", id, "body", "Fix the total"));
pw.comments().reply((String) thread.get("id"), Map.of("body", "Done"));
pw.comments().resolve((String) thread.get("id"));

Map<String, Object> link = pw.shareLinks().create(Map.of("documentId", id, "capabilities", List.of("view")));
pw.shareLinks().disable((String) link.get("id"));
```

## Environments and deployments

```java
List<Object> envs = pw.environments().list();
pw.environments().setPin("production", "tmpl_invoice", 5);
pw.environments().promote("production", Map.of("fromEnvironment", "staging"));
pw.environments().rollback("production");

Map<String, Object> plan = pw.deployments().plan(Map.of("manifest", manifestYaml, "environment", "production"));
pw.deployments().apply((String) plan.get("id"));
```

## Usage

```java
Map<String, Object> usage = pw.usage().get();
```

## Verifying webhooks

PageWeaver signs each delivery with `HMAC-SHA256` over the raw body, in the `X-PageWeaver-Signature`
header as `sha256=<hex>`. Verify it before trusting the payload:

```java
import io.pageweaver.Webhooks;

String signature = request.getHeader(Webhooks.SIGNATURE_HEADER);
if (!Webhooks.verifySignature(secret, rawBody, signature)) {
    // reject
}

// Or verify + parse in one step (throws PageWeaverWebhookSignatureException on mismatch):
Map<String, Object> event = Webhooks.verifyWebhook(secret, rawBody, signature);
```

Pass the **exact raw request body bytes** (as a UTF-8 string) — re-serializing a parsed object can change
the bytes and break the signature.

## Errors

Non-2xx responses throw `PageWeaverException` (with `getStatusCode()` and `getBody()`). The subclasses
`PageWeaverTimeoutException`, `PageWeaverDocumentFailedException`, and
`PageWeaverWebhookSignatureException` all extend it, so catching `PageWeaverException` handles everything.

## Releasing

Publishing to Maven Central requires a Sonatype Central account and GPG signing keys, so no release workflow is wired yet. `mvn -B verify` builds and tests locally / in CI.

## License

MIT
